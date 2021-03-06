package comx.detian.hasitchanged;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;


public class SiteSettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String SITE_ID = "param_site_id";

    private long siteId;

    private SharedPreferences pref;

    public SiteSettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment SiteSettingsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SiteSettingsFragment newInstance(long param1) {
        SiteSettingsFragment fragment = new SiteSettingsFragment();
        Bundle args = new Bundle();
        args.putLong(SITE_ID, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            siteId = getArguments().getLong(SITE_ID);
        }

        Log.d("SiteSettings:", "Loading " + HSCMain.PREFERENCE_PREFIX + siteId);
        this.getPreferenceManager().setSharedPreferencesName(HSCMain.PREFERENCE_PREFIX + siteId);
        addPreferencesFromResource(R.xml.site_preferences);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.site_preferences, false);

        pref = getPreferenceManager().getSharedPreferences();
        pref.registerOnSharedPreferenceChangeListener(this);
        findPreference("pref_site_url").setSummary(pref.getString("pref_site_url", "EX: google.com"));
        findPreference("pref_site_protocol").setSummary(pref.getString("pref_site_protocol", "EX: HTTP"));
        findPreference("pref_site_sync_method").setSummary(pref.getString("pref_site_sync_method", "EX: SYNC"));
        findPreference("pref_site_sync_allow_inexact").setEnabled(!pref.getString("pref_site_sync_method", null).equals("sync"));
    }

    @Override
    public void onPause() {
        super.onPause();

        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        saveSite(pref);
    }

    private void saveSite(SharedPreferences pref) {
        ContentValues updateValues = new ContentValues();
        updateValues.put("URL", pref.getString("pref_site_url", null));
        getActivity().getContentResolver().update(ContentUris.withAppendedId(DatabaseOH.getBaseURI(), siteId), updateValues, "_id=?", new String[]{"" + siteId});
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d("SiteSettings: ", "PreferenceChange triggered");
        if (key.equals("pref_site_url")) {
            String targetUrl = sharedPreferences.getString(key, "EX: google.com");
            if (targetUrl.contains("://")){
                targetUrl = targetUrl.substring(targetUrl.indexOf("://")+3);
                sharedPreferences.edit().putString("pref_site_url", targetUrl).apply();
            }
            findPreference(key).setSummary(targetUrl);
        } else if (key.equals("pref_site_protocol")) {
            findPreference(key).setSummary(sharedPreferences.getString(key, "EX: HTTP"));
        } else if (key.equals("pref_site_sync_method")) {
            findPreference(key).setSummary(sharedPreferences.getString(key, "EX: SYNC"));
            (findPreference("pref_site_sync_allow_inexact")).setEnabled(!sharedPreferences.getString(key, "sync").equals("sync"));
            if (sharedPreferences.getString("key", "sync").equals("sync") && !sharedPreferences.getBoolean("pref_site_sync_allow_inexact", false)) {
                sharedPreferences.edit().putBoolean("pref_site_sync_allow_inexact", true).apply();
                ((CheckBoxPreference) findPreference("pref_site_sync_allow_inexact")).setChecked(true);
            } else {
                //Changing allow_inexact triggers this method again,so only call updateNextSyncTime here otherwise
                HSCMain.updateNextSyncTime(getActivity());
            }
        } else if (key.equals("pref_site_sync_time_elapsed")) {
            //if (sharedPreferences.getString("pref_site_sync_type", "elapsed_time").equals("elapsed_time")) {
            HSCMain.updateNextSyncTime(getActivity());
            //}
        } else if (key.equals("pref_site_sync_allow_inexact")) {
            HSCMain.updateNextSyncTime(getActivity());
        } else if (key.equals("pref_site_notification_sound")) {
            //TODO this will never get triggered because this preference uses a new intent, during which this listener is unregistered
            String current = sharedPreferences.getString(key, "");
            current = current.length() == 0 ? "Silent" : current;
            findPreference(key).setSummary(current);
        }
    }
}