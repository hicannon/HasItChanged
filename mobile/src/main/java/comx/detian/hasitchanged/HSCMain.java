package comx.detian.hasitchanged;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.TimeZone;


public class HSCMain extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    protected static final String AUTHORITY = "comx.detian.hasitchanged.provider";
    //RFC 822 date format
    static final SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
    static final String PREFERENCE_PREFIX = "HSCPREFERENCE.";
    Account mAccount;
    ContentResolver mResolver;

    public static enum METHOD{
        SYNC, ALARM
    }

    public static enum TYPE{
        REPEATING, TIME
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
