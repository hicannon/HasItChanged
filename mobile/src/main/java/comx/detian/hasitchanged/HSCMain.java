package comx.detian.hasitchanged;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Typeface;
import android.support.v7.app.ActionBar;
import android.app.AlarmManager;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;


public class HSCMain extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    protected static final String AUTHORITY = "comx.detian.hasitchanged.provider";
    //RFC 822 date format
    static final SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
    static final String PREFERENCE_PREFIX = "HSCPREFERENCE.";
    private static Account mAccount = null;
    private static PendingIntent exactSyncIntent = null;
    private static PendingIntent inexactSyncIntent = null;

    static {
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private static Bundle syncExtras = new Bundle();

    ContentResolver mResolver;
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    /**
     * Used to store the last screen title. For use in {@link /*#restoreActionBar()}.
     */
    private CharSequence mTitle;

    //TODO figure out why getBroadcast is returning different PendingIntents, current is workaround
    public static PendingIntent getExactSyncIntent(Context context) {
        if (exactSyncIntent == null) {
            exactSyncIntent = PendingIntent.getBroadcast(context, 1, new Intent(context, HSCAlarmSync.class), PendingIntent.FLAG_CANCEL_CURRENT);
        }
        return exactSyncIntent;
    }

    public static PendingIntent getInExactSyncIntent(Context context) {
        if (inexactSyncIntent == null) {
            inexactSyncIntent = PendingIntent.getBroadcast(context, 2, new Intent(context, HSCAlarmSync.class), PendingIntent.FLAG_CANCEL_CURRENT);
        }
        return inexactSyncIntent;
    }

    public static Account getAccount(Context context) {
        if (mAccount == null) {
            Account[] accounts = ((AccountManager) context.getSystemService(ACCOUNT_SERVICE)).getAccountsByType("HSC.comx");
            if (accounts.length >= 1) {
                //account already made
                mAccount = accounts[0];
                assert (accounts.length == 1);
            } else if (accounts.length == 0) {
                mAccount = CreateSyncAccount(context);
            }
        }
        return mAccount;
    }

    //in Milliseconds
    static long calculateTimeToTrigger(ArrayList<String> targetTimes, ArrayList<String> syncTimes) {
        if (BuildConfig.DEBUG && targetTimes.size() != syncTimes.size()) {
            throw new RuntimeException("TargetTimes and syncTimes size doesn't match " + targetTimes.size() + " vs " + syncTimes.size());
        }
        long minTimeToSync = Long.MAX_VALUE;
        for (int i = 0; i < targetTimes.size(); i++) {
            long timeDifference = calcTimeDiff(syncTimes.get(i), targetTimes.get(i));
            if (timeDifference < minTimeToSync) {
                minTimeToSync = timeDifference;
            }
        }
        return minTimeToSync;
    }

    /**
     * @param lastSyncTime - DB stored GMT timestamp
     * @param targetT      - User given time string for sync interval
     * @return time in mills of how far in the in future the next sync should be
     */
    static long calcTimeDiff(String lastSyncTime, String targetT) {
        if (targetT.toLowerCase().equals("never")){
            Log.d("CalcFuture", "Item should not be synced");
            return Long.MAX_VALUE;
        }
        if (lastSyncTime==null || lastSyncTime.toLowerCase().equals("never")){
            Log.d("CalcFuture", "Item has never been synced");
            return 0;
        }

        long elapsedTime;
        //This regex splits on on number/character boundary
        String[] pieces = targetT.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
        long targetTime = 0;
        long temp = -1;
        for (int j = 0; j < pieces.length; j++) {
            //Start with number
            if (temp < 0) {
                try {
                    temp = Long.parseLong(pieces[j]);
                } catch (NumberFormatException e) {
                    //Ignore
                }
            } else { //get units for number
                pieces[j] = pieces[j].trim().toLowerCase();
                //Grumble Grumble
                if (pieces[j].endsWith("s")) {
                    pieces[j] = pieces[j].substring(0, pieces[j].length() - 1);
                }
                long temp2 = temp;
                temp = -1;
                if (pieces[j].equals("milli")) {
                    targetTime += temp2;
                } else if (pieces[j].equals("sec")) {
                    targetTime += (temp2 * 1000);
                } else if (pieces[j].equals("min")) {
                    targetTime += (temp2 * 1000 * 60);
                } else if (pieces[j].equals("hour")) {
                    targetTime += (temp2 * 1000 * 60 * 60);
                } else if (pieces[j].equals("day")) {
                    targetTime += (temp2 * 1000 * 60 * 60 * 24);
                } else if (pieces[j].equals("week")) {
                    targetTime += (temp2 * 1000 * 60 * 60 * 24 * 7);
                } else {
                    //Not valid unit, continue trying to get valid num
                    temp = temp2;
                }
            }
        }
        assert (df.getTimeZone().equals(TimeZone.getTimeZone("GMT")));
        try {
            elapsedTime = (new Date()).getTime() - df.parse(lastSyncTime).getTime();
        } catch (Exception e) {
            elapsedTime = targetTime;
        }

        if (BuildConfig.DEBUG && elapsedTime < 0) {
            throw new RuntimeException("ElapsedTime" + lastSyncTime + " ::: " + df.format(new Date()));
        }


        long timeDifference = targetTime - elapsedTime;

        Log.d("CalcFuture", "Item has passed " + elapsedTime + " on its way to " + targetTime);
        Log.d("CalcFuture", "Item has " + timeDifference + " to go before sync");

        return timeDifference;
    }

    /**
     * Request a Sync to the content resolver
     *
     * @param context  the application context
     * @param idToSync 0 to force sync all, -1 to only sync those necessary; otherwise sync idToSync
     * @param notify   whether to display a toast
     */
    static void requestSyncNow(final Context context, long idToSync, boolean notify) {
        final Bundle params = new Bundle();
        params.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        params.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        if (idToSync == 0) {
            params.putBoolean("FORCE_SYNC_ALL", true);
            if (notify)
                Toast.makeText(context, "Checking All for Changes", Toast.LENGTH_SHORT).show();
        } else {
            params.putBoolean("FORCE_SYNC_" + idToSync, true);
            if (notify)
                Toast.makeText(context, "Checking this for Changes", Toast.LENGTH_SHORT).show();
        }
        //TODO consider using AsyncTask instead
        new Thread(new Runnable() {
            @Override
            public void run() {
                ContentProviderClient client = context.getContentResolver().acquireContentProviderClient(HSCMain.AUTHORITY);
                HSCSyncAdapter.performSyncNow(context, params, client);
                client.release();
            }
        }).start();
    }

    /**
     * Returns the next wakeup in milliseconds
     *
     * @param context
     * @param methodToCheck
     * @param inexact
     * @return
     */
    protected static long getNextSyncTime(Context context, String methodToCheck, boolean inexact) {
        Cursor cursor = context.getContentResolver().query(DatabaseOH.getBaseURI(), null, null, null, null);

        ArrayList<String> targetTimes = new ArrayList<String>();
        ArrayList<String> syncTimes = new ArrayList<String>();

        //Skip first dummy
        cursor.moveToNext();

        while (cursor.moveToNext()) {
            long siteId = cursor.getLong(DatabaseOH.COLUMNS._id.ordinal());

            SharedPreferences sp = context.getSharedPreferences(PREFERENCE_PREFIX + siteId, MODE_MULTI_PROCESS);
            if (sp.getString("pref_site_sync_method", "").equals(methodToCheck)
                    && sp.getString("pref_site_sync_type", "").equals("elapsed_time")
                    && sp.getBoolean("pref_site_sync_allow_inexact", true) == inexact) {
                if (sp.getString("pref_site_url", "").length() != 0
                        && !sp.getString("pref_site_sync_time_elapsed", "never").toLowerCase().contains("never")) {
                    targetTimes.add(sp.getString("pref_site_sync_time_elapsed", "never"));
                    //System.out.println(methodToCheck+ cursor.getString(DatabaseOH.COLUMNS.URL.ordinal()));
                    syncTimes.add(cursor.getString(DatabaseOH.COLUMNS.LUDATE.ordinal()));
                }
            }
        }
        cursor.close();

        long nextSync = calculateTimeToTrigger(targetTimes, syncTimes);
        Log.d("CalcFuture", methodToCheck + inexact + "Calculated sync for " + nextSync + " seconds in the future");
        nextSync = nextSync <= 0 ? 1 : nextSync;

        return nextSync;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected static void updateNextSyncTime(Context context) {
        long nextSyncTime = getNextSyncTime(context, "sync", true);
        if (nextSyncTime != Long.MAX_VALUE) {
            nextSyncTime /= 1000; //Sync needs to be in seconds
            //Prevent syncs from being too close together
            nextSyncTime = nextSyncTime < 60 ? 120 : nextSyncTime;
            ContentResolver.addPeriodicSync(getAccount(context), AUTHORITY, syncExtras, nextSyncTime);
            Log.d("SYNC_STATUS", "Setting sync for " + nextSyncTime + " sec\n");
        } else {
            ContentResolver.removePeriodicSync(getAccount(context), AUTHORITY, syncExtras);
        }

        //TODO check to make sure can schedule the same PendingIntent multiple times (same IntentSender?)
        //TODO handle per url with independent alarms
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(ALARM_SERVICE);

        //NOTE: Cancelling the alarm that has the same PendingIntent as the one that is about to be created seems to cancel that one too

        //PendingIntent syncIntent = PendingIntent.getBroadcast(context, 1, getSyncIntent(context), PendingIntent.FLAG_NO_CREATE);
        long nextExactAlarmTime = getNextSyncTime(context, "alarm", false);
        if (nextExactAlarmTime != Long.MAX_VALUE) {
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                alarmMgr.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + nextExactAlarmTime, getExactSyncIntent(context));
            } else {
                alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + nextExactAlarmTime, getExactSyncIntent(context));
            }
            Log.d("SYNC_STATUS", "Setting exact for " + nextExactAlarmTime + " millis\n");
        }else{
            alarmMgr.cancel(getExactSyncIntent(context));
        }

        //PendingIntent inexactSyncIntent = PendingIntent.getBroadcast(context, 2, getSyncIntent(context), PendingIntent.FLAG_NO_CREATE);
        long nextInexactAlarmTime = getNextSyncTime(context, "alarm", true);
        if (nextInexactAlarmTime != Long.MAX_VALUE) {
            alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + nextInexactAlarmTime, getInExactSyncIntent(context));
            Log.d("SYNC_STATUS", "Setting inexact for " + nextInexactAlarmTime + " millis\n");
        }else{
            alarmMgr.cancel(getInExactSyncIntent(context));
        }

        Log.d("SYNC_STATUS: ", (nextSyncTime == Long.MAX_VALUE ? "NEVER" : nextSyncTime * 1000) + " " + (nextExactAlarmTime == Long.MAX_VALUE ? "NEVER" : nextExactAlarmTime) + " " + (nextInexactAlarmTime == Long.MAX_VALUE ? "NEVER" : nextInexactAlarmTime));
    }

    public static Account CreateSyncAccount(Context context) {
        Account out = new Account("Dummy", "HSC.comx");
        AccountManager am = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);

        if (am.addAccountExplicitly(out, null, null)) {
            return out;
        } else {
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_hscmain);

        //Set up the actionbar
        Toolbar actionBar = (Toolbar) findViewById(R.id.actionBar);
        if (actionBar != null) {
            setSupportActionBar(actionBar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setElevation(2);
        } else {
            throw new RuntimeException();
        }

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = "HSC?";
        mResolver = getContentResolver();
        ContentResolver.setSyncAutomatically(getAccount(this), AUTHORITY, true);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        HSCMain.updateNextSyncTime(getApplicationContext());
    }

    @Override
    public void onBackPressed(){
        if (mNavigationDrawerFragment.getCurrentFragmentId()==0){
            super.onBackPressed();
        }else{
            mNavigationDrawerFragment.selectItem(0, 0);
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position, long id) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, id == 0 ? OverviewFragment.newInstance() : SiteSettingsFragment.newInstance(id))
                .commitAllowingStateLoss();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.hscmain, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}