package comx.detian.hasitchanged;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
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

import java.net.URL;

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
    SimpleCursorAdapter mAdapter;
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
    private boolean switchToLast = false;
    private BroadcastReceiver receiver;
    private MatrixCursor lastItem;

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

        lastItem = new MatrixCursor(new String[] { "_id", "URL", "LUDATE", "HASH", "ETAG", "FAVICON", "CONTENT", "HISTORY"});
        lastItem.addRow(new String[]{Integer.MAX_VALUE+"", "Add New", null, null, null, null, null, null});
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);

        getLoaderManager().initLoader(0, null, this);
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
        mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.list_item_icon_text,
                null,
                new String[]{"URL", "FAVICON"},
                new int[]{R.id.siteURL, R.id.siteIcon}, 0);

        SimpleCursorAdapter.ViewBinder viewBinder = new SimpleCursorAdapter.ViewBinder() {

            @Override
            public boolean setViewValue(View view, Cursor c, int cIndex) {
                if (view instanceof ImageView) {
                    ImageView imageView = (ImageView) view;
                    byte[] raw = c.getBlob(cIndex);
                    if (raw != null)
                        imageView.setImageBitmap(BitmapFactory.decodeByteArray(raw, 0, raw.length));
                    else {
                        switch (c.getInt(DatabaseOH.COLUMNS._id.ordinal())){
                            case 0: imageView.setImageResource(android.R.drawable.ic_menu_view); break;
                            case Integer.MAX_VALUE: imageView.setImageResource(android.R.drawable.ic_input_add); break;
                            default: imageView.setImageResource(android.R.drawable.ic_menu_report_image); break;
                        }
                    }
                    return true;
                } else {
                    TextView textView = (TextView) view;
                    String text = c.getString(cIndex);
                    if (text.trim().length() == 0) {
                        text = "BLANK";
                    }
                    textView.setText(text);
                    switch (c.getInt(DatabaseOH.COLUMNS._id.ordinal())){
                        case 0:
                            textView.setTextSize(20);
                            textView.setTypeface(HSCMain.getRobotoRegular(getActivity()), Typeface.BOLD); break;
                        case Integer.MAX_VALUE: textView.setTypeface(HSCMain.getRobotoRegular(getActivity()), Typeface.ITALIC); break;
                        default: textView.setTypeface(HSCMain.getRobotoRegular(getActivity()), Typeface.NORMAL); break;
                    }
                    return true;
                }
            }
        };
        mAdapter.setViewBinder(viewBinder);
        mDrawerListView.setAdapter(mAdapter);
        mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                getLoaderManager().restartLoader(0, null, NavigationDrawerFragment.this);
            }
        };
        getActivity().registerReceiver(receiver, new IntentFilter("comx.detian.hasitchanged.SYNC_COMPLETE"));

        return mDrawerListView;
    }

    /*private Cursor getSitesCursor() {
        return getActivity().getContentResolver().query(DatabaseOH.getBaseURI(), null, null, null, null);
    }*/

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
                (Toolbar) getActivity().findViewById(R.id.actionBar),             /* nav drawer image to replace 'Up' caret */
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

        //Create a new site if there is the intent to do so
        Intent intent = getActivity().getIntent();
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_SEND)) {
            if (intent.getType() != null && intent.getType().equals("text/plain")) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                System.out.println(sharedText);
                createNewEntry(sharedText);
                return;
            }
        }

        selectItem(mCurrentSelectedPosition, mCurrentId);
    }

    private void selectItem(int position, long id) {
        Log.d("NavigationDrawer: ", "select item " + position + " " + id);
        assert (mAdapter.getItemId(position) == id);

        //Dummy last item is add new
        if (position==mAdapter.getCount()-1){
            createNewEntry(null);
            return;
        }
        //A listener that updates the URL displayed in the Navigation Drawer immediately if it is changed
        SharedPreferences targetPref = getActivity().getSharedPreferences(HSCMain.PREFERENCE_PREFIX + id, Context.MODE_MULTI_PROCESS);
        targetPref.registerOnSharedPreferenceChangeListener(this);

        //Remove/add the delete option depending on screen
        if (id == 0 || mCurrentId == 0) {
            if (mCallbacks != null) {
                //Note this has the effect of calling onPrepareOptionsMenu() which does the actual removal
                mCallbacks.invalidateOptionsMenu();
            }
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
        getActivity().unregisterReceiver(receiver);
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
        } else {
            if (mCurrentId == 0) {
                menu.findItem(R.id.delete_site).setVisible(false);
            } else {
                menu.findItem(R.id.delete_site).setVisible(true);
            }
        }
        super.onCreateOptionsMenu(menu, inflater);


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        if (item.getItemId() == R.id.manual_sync) {
            HSCMain.requestSyncNow(getActivity(), mCurrentId, true);
            return true;
        } else if (item.getItemId() == R.id.add_site) {
            createNewEntry(null);
            return true;
        } else if (item.getItemId() == R.id.delete_site) {
            if (mCurrentId==0){
                return true;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final SharedPreferences targetPref = getActivity().getSharedPreferences(HSCMain.PREFERENCE_PREFIX + mCurrentId, Context.MODE_MULTI_PROCESS);
            builder.setTitle("Delete this URL?").setMessage(targetPref.getString("pref_site_url", null) + " will be deleted.");
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @SuppressLint("CommitPrefEdits")
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    getActivity().getContentResolver().delete(ContentUris.withAppendedId(DatabaseOH.getBaseURI(), mCurrentId), "_id=?", new String[]{mCurrentId + ""});
                    targetPref.edit().clear().commit();
                    //TODO delete the file too
                    getLoaderManager().restartLoader(0, null, NavigationDrawerFragment.this);
                    selectItem(0, 0); //Switch to overview
                    HSCMain.updateNextSyncTime(getActivity());
                }
            });

            builder.setNegativeButton(android.R.string.cancel, null);

            builder.show();
            return true;
        } else if (item.getItemId() == R.id.clear_history) {
            Log.d("NavigationDrawer: ", "Clear history");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final SharedPreferences targetPref = getActivity().getSharedPreferences(HSCMain.PREFERENCE_PREFIX + mCurrentId, Context.MODE_MULTI_PROCESS);
            if (mCurrentId==0)
                builder.setTitle("Clear history for all sites?").setMessage("All history will be deleted.");
            else
                builder.setTitle("Clear history for this URL?").setMessage(targetPref.getString("pref_site_url", null) + "'s history will be deleted.");
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ContentValues cv = new ContentValues();
                    cv.put("HISTORY", "");
                    if (mCurrentId==0){
                        for (int j=1; j<mAdapter.getCount(); j++){
                            getActivity().getContentResolver().update(ContentUris.withAppendedId(DatabaseOH.getBaseURI(), mAdapter.getItemId(j)), cv, "_id=?", new String[]{mAdapter.getItemId(j) + ""});
                        }
                        //Update the UI
                        //TODO change notification mechanism
                        Intent intent = new Intent("comx.detian.hasitchanged.SYNC_COMPLETE");
                        getActivity().sendBroadcast(intent);
                    }else {
                        getActivity().getContentResolver().update(ContentUris.withAppendedId(DatabaseOH.getBaseURI(), mCurrentId), cv, "_id=?", new String[]{mCurrentId + ""});
                    }
                }
            });

            builder.setNegativeButton(android.R.string.cancel, null);

            builder.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void createNewEntry(String url) {
        ContentValues values = new ContentValues();
        String address = "";

        if (url != null) {
            URL uri = null;

            try {
                uri = new URL(url);
                address = uri.getAuthority() + uri.getFile();
                //Todo protocol ass well
            } catch (Exception e) {
                address = url;
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("URL seems invalid").setMessage("Please double check the address.");
                builder.setPositiveButton("I will!", null);
                builder.show();
            }
        }

        values.put("URL", address);
        Uri uri = getActivity().getContentResolver().insert(DatabaseOH.getBaseURI(), values);
        switchToLast = true;
        getLoaderManager().restartLoader(0, null, this);

        SharedPreferences targetPref = getActivity().getSharedPreferences(HSCMain.PREFERENCE_PREFIX + ContentUris.parseId(uri), Context.MODE_MULTI_PROCESS);
        targetPref.edit().putString("pref_site_url", address).commit();

        //selectItem(mAdapter.getCount() - 1, ContentUris.parseId(uri));
    }

    /**
     * Per the navigation drawer design guidelines, updates the action bar to show the global app
     * 'context', rather than just what's in the current screen.
     */
    private void showGlobalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(R.string.app_name);
    }

    private ActionBar getActionBar() {
        return ((ActionBarActivity) getActivity()).getSupportActionBar();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d("NavigationDrawer: ", "PreferenceChange triggered");
        if (key.equals("pref_site_url")) {
            ContentValues updateValues = new ContentValues();
            updateValues.put("URL", sharedPreferences.getString("pref_site_url", ""));
            getActivity().getContentResolver().update(ContentUris.withAppendedId(DatabaseOH.getBaseURI(), mCurrentId), updateValues, "_id=?", new String[]{"" + mCurrentId});

            getLoaderManager().restartLoader(0, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        //return new CursorLoader(getActivity(), DatabaseOH.getBaseURI(), new String[]{"_id", "URL", "FAVICON"}, null, null, null);
        return new CursorLoader(getActivity(), DatabaseOH.getBaseURI(), null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        MergeCursor mergeCursor = new MergeCursor(new Cursor[]{data, lastItem});
        mAdapter.swapCursor(mergeCursor);
        //mAdapter.notifyDataSetChanged();
        if (switchToLast) {
            switchToLast = false;
            getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    selectItem(mAdapter.getCount() - 2, mAdapter.getItemId(mAdapter.getCount() - 2));

                }
            });
        }
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

        void invalidateOptionsMenu();
    }
}
