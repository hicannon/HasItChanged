package comx.detian.hasitchanged;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;


public class HSCMain extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    protected static final String AUTHORITY = "comx.detian.hasitchanged.provider";
    //RFC 822 date format
    static final SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
    static final String PREFERENCE_PREFIX = "HSCPREFERENCE.";
    Account mAccount;
    ContentResolver mResolver;

    //in Milliseconds
    static long calculateTimeToSync(ArrayList<String> targetTimes, ArrayList<String> syncTimes) {
        if (BuildConfig.DEBUG && targetTimes.size()!=syncTimes.size()){
            throw new RuntimeException("TargetTimes and syncTimes size doesn't match "+targetTimes.size() + " vs " + syncTimes.size());
        }
        long minTimeToSync = Long.MAX_VALUE;
        for (int i = 0; i<targetTimes.size(); i++) {
            if (syncTimes.get(i)==null){ //this entry has never been synced
                minTimeToSync = 0;
                break;
            }
            long timeDifference = calcTimeDiff(syncTimes.get(i), targetTimes.get(i));
            if (timeDifference<minTimeToSync){
                minTimeToSync = timeDifference;
            }
        }
        return minTimeToSync;
    }

    /**
     *
     * @param lastSyncTime - DB stored GMT timestamp
     * @param targetT - User given time string for sync interval
     * @return time in mills of how far in the in future the next sync should be
     */
    static long calcTimeDiff(String lastSyncTime, String targetT) {
        long elapsedTime;
        String[] pieces = targetT.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
        long targetTime = 0;
        long temp = -1;
        for(int j=0; j< pieces.length; j++){
            //Start with number
            if (temp<0) {
                try {
                    temp = Long.parseLong(pieces[j]);
                }catch (NumberFormatException e){
                    //Ignore
                }
            }else{ //get units for number
                pieces[j] = pieces[j].trim().toLowerCase();
                //Grumble Grumble
                if (pieces[j].endsWith("s")){
                    pieces[j] = pieces[j].substring(0, pieces[j].length()-1);
                }
                long temp2 = temp;
                temp = -1;
                if (pieces[j].equals("milli")){
                    targetTime+=temp2;
                }else if (pieces[j].equals("sec")){
                    targetTime+=(temp2*1000);
                }else if (pieces[j].equals("min")){
                    targetTime+=(temp2*1000*60);
                }else if (pieces[j].equals("hour")){
                    targetTime+=(temp2*1000*60*60);
                }else if (pieces[j].equals("day")){
                    targetTime+=(temp2*1000*60*60*24);
                }else if (pieces[j].equals("week")){
                    targetTime+=(temp2*1000*60*60*24*7);
                }else{
                    //Not valid unit, continue trying to get valid num
                    temp = temp2;
                }
            }
        }
        try {
            elapsedTime = (new Date()).getTime() - df.parse(lastSyncTime).getTime();
        } catch (Exception e) {
            elapsedTime = targetTime;
        }

        long timeDifference = targetTime - elapsedTime;

        Log.d("CalcFuture", " Item has passed " + elapsedTime + " on its way to " + targetTime);
        Log.d("CalcFuture", "Item has " + timeDifference + " to go before sync");

        return timeDifference;
    }

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hscmain);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = "HSC?";//getTitle();

        Account[] accounts = ((AccountManager) this.getSystemService(ACCOUNT_SERVICE)).getAccountsByType("HSC.comx");
        if (accounts.length>=1){
            //account already made
            mAccount = accounts[0];
            assert(accounts.length==1);
        }else if (accounts.length==0){
            mAccount = CreateSyncAccount(this);
        }

        mResolver = getContentResolver();

        ContentResolver.setSyncAutomatically(mAccount, AUTHORITY, true);

        Bundle params = new Bundle();

        //ContentResolver.addPeriodicSync(mAccount, AUTHORITY, params, 120);

        df.setTimeZone(TimeZone.getTimeZone("GMT"));

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    public static Account CreateSyncAccount(Context context){
        Account out = new Account("Dummy", "HSC.comx");
        AccountManager am = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);

        if (am.addAccountExplicitly(out, null, null)){
            return out;
        }else{
            return null;
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position, long id) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, id==0?OverviewFragment.newInstance():SiteSettingsFragment.newInstance(id))
                .commit();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
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
