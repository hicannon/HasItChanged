package comx.detian.hasitchanged;


import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class NavigationDrawerFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Remember the position of the selected item.
     */
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";
    private static final String STATE_SELECTED_ID = "selected_navigation_drawer_id";
    /**
     * Per the design guidelines, you should show the drawer on launch until the user manually
     * expands it. This shared preference tracks this.
     */
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private NavigationDrawerCallbacks mCallbacks;

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private View mFragmentContainerView;

    private int mCurrentSelectedPosition = 0;
    private long mCurrentId = 0;
    private boolean mFromSavedInstanceState;
    private boolean mUserLearnedDrawer;

    SimpleCursorAdapter mAdapter;

    public NavigationDrawerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Read in the flag indicating whether or not the user has demonstrated awareness of the
        // drawer. See PREF_USER_LEARNED_DRAWER for details.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);

        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
            mCurrentId = savedInstanceState.getLong(STATE_SELECTED_ID);
            mFromSavedInstanceState = true;
        }

        // Select either the default item (0) or the last selected item.
        selectItem(mCurrentSelectedPosition, 0);
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);

        getLoaderManager().initLoader(0, null, this);
        //setEmptyText("Not Watching Any");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mDrawerListView = (ListView) inflater.inflate(
                R.layout.fragment_navigation_drawer, container, false);
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("NavigationDrawer: ", position + " " + id);
                selectItem(position, id);
            }
        });
        mAdapter = new SimpleCursorAdapter(getActionBar().getThemedContext(),
                R.layout.list_item_icon_text,
                null,
                new String[]{"URL", "FAVICON"},
                new int[]{R.id.siteURL, R.id.siteIcon}, 0);

        SimpleCursorAdapter.ViewBinder viewBinder = new SimpleCursorAdapter.ViewBinder(){

            @Override
            public boolean setViewValue(View view, Cursor c, int cIndex) {
                if (view instanceof ImageView){
                    ImageView imageView = (ImageView) view;
                    byte[] raw = c.getBlob(cIndex);
                    if (raw!=null)
                        imageView.setImageBitmap(BitmapFactory.decodeByteArray(raw, 0, raw.length));
                    else{
                        imageView.setImageResource(android.R.drawable.ic_menu_view);
                    }
                    return true;
                }else{
                    TextView textView = (TextView) view;
                    String text = c.getString(cIndex);
                    if (text.trim().length()==0){
                        text = "BLANK";
                    }
                    textView.setText(text);
                    return true;
                }
            }
        };
        mAdapter.setViewBinder(viewBinder);
        /*mDrawerListView.setAdapter(new ArrayAdapter<String>(
                getActionBar().getThemedContext(),
                android.R.layout.simple_list_item_activated_1,
                android.R.id.text1,
                new String[]{
                        getString(R.string.title_section1),
                        getString(R.string.title_section2),
                        getString(R.string.title_section3),
                }));*/
        mDrawerListView.setAdapter(mAdapter);
        mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);
        return mDrawerListView;
    }

    private Cursor getSitesCursor() {
        return getActivity().getContentResolver().query(DatabaseOH.getBaseURI(), new String[]{"_id", "URL", "FAVICON"}, null, null, null);
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                R.drawable.ic_drawer,             /* nav drawer image to replace 'Up' caret */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

                if (!mUserLearnedDrawer) {
                    // The user manually opened the drawer; store this flag to prevent auto-showing
                    // the navigation drawer automatically in the future.
                    mUserLearnedDrawer = true;
                    SharedPreferences sp = PreferenceManager
                            .getDefaultSharedPreferences(getActivity());
                    sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
                }

                getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
        // per the navigation drawer design guidelines.
        if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
            mDrawerLayout.openDrawer(mFragmentContainerView);
        }

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private void selectItem(int position, long id) {
        Log.d("NavigationDrawer: ", "select item "+ position + " " + id);

        //A listener that updates the URL displayed in the Navigation Drawer immediately if it is changed
        SharedPreferences targetPref = getActivity().getSharedPreferences(HSCMain.PREFERENCE_PREFIX+id, Context.MODE_MULTI_PROCESS);
        targetPref.registerOnSharedPreferenceChangeListener(this);

        //Remove/add the delete option depending on screen
        if (id==0 || mCurrentId==0){
            getActivity().invalidateOptionsMenu();
        }

        mCurrentSelectedPosition = position;
        mCurrentId = id;

        if (mDrawerListView != null) {
            mDrawerListView.setItemChecked(position, true);
        }
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
        if (mCallbacks != null) {
            mCallbacks.onNavigationDrawerItemSelected(position, id);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (NavigationDrawerCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
        outState.putLong(STATE_SELECTED_ID, mCurrentId);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // If the drawer is open, show the global app actions in the action bar. See also
        // showGlobalContextActionBar, which controls the top-left area of the action bar.
        if (mDrawerLayout != null && isDrawerOpen()) {
            inflater.inflate(R.menu.global, menu);
            showGlobalContextActionBar();
        }else{
            //if (menu.findItem(R.id.delete_site)!=null){
                if (mCurrentId==0){
                    menu.findItem(R.id.delete_site).setVisible(false);
                }else{
                    menu.findItem(R.id.delete_site).setVisible(true);
                }
            //}
        }
        super.onCreateOptionsMenu(menu, inflater);


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        if (item.getItemId() == R.id.manual_sync) {
            Toast.makeText(getActivity(), "Checking for Changes", Toast.LENGTH_SHORT).show();

            if (ContentResolver.isSyncPending(((HSCMain)getActivity()).mAccount, HSCMain.AUTHORITY) ||
                    ContentResolver.isSyncActive(((HSCMain)getActivity()).mAccount, HSCMain.AUTHORITY)){
                Log.d("SYNC: Manual", "Sync pending, cancelling");
                ContentResolver.cancelSync(((HSCMain)getActivity()).mAccount, HSCMain.AUTHORITY);
            }
            Bundle params = new Bundle();
            params.putBoolean(
                    ContentResolver.SYNC_EXTRAS_MANUAL, true);
            params.putBoolean(
                    ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
            ContentResolver.requestSync(((HSCMain)getActivity()).mAccount, HSCMain.AUTHORITY, params);
            //mAdapter.changeCursor(getSitesCursor());
            getLoaderManager().restartLoader(0, null, this);
            //mAdapter.notifyDataSetChanged();
            return true;
        }else if (item.getItemId() == R.id.add_site){
            ContentValues values = new ContentValues();
            values.put("URL", "");
            Uri uri = getActivity().getContentResolver().insert(DatabaseOH.getBaseURI(), values);
            mAdapter.changeCursor(getSitesCursor());
            mAdapter.notifyDataSetChanged();
            //TODO NavigationDrawer will not upddate properly if using LoadManager due to timing
            //getLoaderManager().restartLoader(0, null ,this);
            //mDrawerListView.postInvalidate();
            selectItem(mAdapter.getCount()-1, ContentUris.parseId(uri));

            return true;
        }else if (item.getItemId() == R.id.delete_site){
            Log.d("NavigationDrawer: ", "Delete");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final SharedPreferences targetPref = getActivity().getSharedPreferences(HSCMain.PREFERENCE_PREFIX+mCurrentId, Context.MODE_MULTI_PROCESS);
            builder.setTitle("Delete this URL?").setMessage(targetPref.getString("pref_site_url", null) +" will be deleted.");
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    getActivity().getContentResolver().delete(ContentUris.withAppendedId(DatabaseOH.getBaseURI(), mCurrentId), "_id=?", new String[]{mCurrentId+""});
                    targetPref.edit().clear().commit();
                    //TODO delete the file too
                    getLoaderManager().restartLoader(0, null, NavigationDrawerFragment.this);
                    //mAdapter.changeCursor(getSitesCursor());
                    //mAdapter.notifyDataSetChanged();
                    selectItem(0, 0); //Switch to overview
                }
            });

            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //do nothing
                }
            });

            AlertDialog dialog = builder.show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Per the navigation drawer design guidelines, updates the action bar to show the global app
     * 'context', rather than just what's in the current screen.
     */
    private void showGlobalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setTitle(R.string.app_name);
    }

    private ActionBar getActionBar() {
        return getActivity().getActionBar();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d("NavigationDrawer: ", "PreferenceChange triggered");
        if (key.equals("pref_site_url")){
            ContentValues updateValues = new ContentValues();
            updateValues.put("URL", sharedPreferences.getString("pref_site_url", null));
            getActivity().getContentResolver().update(ContentUris.withAppendedId(DatabaseOH.getBaseURI(), mCurrentId), updateValues, "_id=?", new String[]{""+mCurrentId});

            getLoaderManager().restartLoader(0, null, this);
            //mAdapter.changeCursor(getSitesCursor());
            //mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(getActivity(), DatabaseOH.getBaseURI(), new String[]{"_id", "URL", "FAVICON"}, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader loader) {
        mAdapter.swapCursor(null);
    }

    /**
     * Callbacks interface that all activities using this fragment must implement.
     */
    public static interface NavigationDrawerCallbacks {
        /**
         * Called when an item in the navigation drawer is selected.
         */
        void onNavigationDrawerItemSelected(int position, long id);
    }
}
