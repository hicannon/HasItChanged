package comx.detian.hasitchanged;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;


public class SiteSettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private long siteId;

    private SharedPreferences pref;
    //private String mParam2;

    //private OnFragmentInteractionListener mListener;

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
        args.putLong(ARG_PARAM1, param1);
        //args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }
    public SiteSettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            siteId = getArguments().getLong(ARG_PARAM1);
        }

        //Cursor cursor = getActivity().getContentResolver().query(DatabaseOH.getBaseURI(), null, null, null, null);
        Log.d("SiteSettings:", "Loading " + HSCMain.PREFERENCE_PREFIX + siteId);
        this.getPreferenceManager().setSharedPreferencesName(HSCMain.PREFERENCE_PREFIX+ siteId);
        addPreferencesFromResource(R.xml.site_preferences);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.site_preferences, false);

        pref = getPreferenceManager().getSharedPreferences();
        pref.registerOnSharedPreferenceChangeListener(this);
        findPreference("pref_site_url").setSummary(pref.getString("pref_site_url", "EX: google.com"));
        findPreference("pref_site_protocol").setSummary(pref.getString("pref_site_protocol", "EX: HTTP"));
        findPreference("pref_site_sync_method").setSummary(pref.getString("pref_site_sync_method", "EX: SYNC"));
    }

    @Override
    public void onPause(){
        super.onPause();

        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        saveSite(pref);
    }

    private void saveSite(SharedPreferences pref) {
        ContentValues updateValues = new ContentValues();
        updateValues.put("URL", pref.getString("pref_site_url", null));
        getActivity().getContentResolver().update(ContentUris.withAppendedId(DatabaseOH.getBaseURI(), siteId), updateValues, "_id=?", new String[]{""+siteId});
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d("SiteSettings: ", "PreferenceChange triggered");
        if (key.equals("pref_site_url")){
            findPreference(key).setSummary(sharedPreferences.getString(key, "EX: google.com"));
        }else if (key.equals("pref_site_protocol")){
            findPreference(key).setSummary(sharedPreferences.getString(key, "EX: HTTP"));
        }else if (key.equals("pref_site_sync_method")){
            findPreference(key).setSummary(sharedPreferences.getString(key, "EX: SYNC"));
        }
    }
    /*@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_site_settings, container, false);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
   /* public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }
    */

}
