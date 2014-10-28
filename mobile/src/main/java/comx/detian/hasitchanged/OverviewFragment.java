package comx.detian.hasitchanged;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * { OverviewFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link OverviewFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OverviewFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters

    //private OnFragmentInteractionListener mListener;

    private RecyclerView historyView;
    private HistoryAdapter historyAdapter;
    private BroadcastReceiver receiver;

    public OverviewFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment OverviewFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static OverviewFragment newInstance() {
        OverviewFragment fragment = new OverviewFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            //mParam1 = getArguments().getString(ARG_PARAM1);
            //mParam2 = getArguments().getString(ARG_PARAM2);
        }

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateContent();
            }
        };
        getActivity().registerReceiver(receiver, new IntentFilter("comx.detian.hasitchanged.SYNC_COMPLETE"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View out = inflater.inflate(R.layout.fragment_overview, container, false);
        historyView = (RecyclerView) out.findViewById(R.id.history);
        historyView.setHasFixedSize(true);
        historyView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        historyAdapter = new HistoryAdapter(getActivity(), getActivity().getContentResolver().query(DatabaseOH.getBaseURI(), null, null, null, null));
        historyView.setAdapter(historyAdapter);

        //TODO ability to tap on items
        //historyView.addOnItemTouchListener();

        out.findViewById(R.id.history_button_collapse).setOnClickListener(historyAdapter);
        out.findViewById(R.id.history_button_filter).setOnClickListener(historyAdapter);
        return out;
    }

    private void updateContent() {
        //historyAdapter.clear();
        historyAdapter.addAllFromCurosr(getActivity().getContentResolver().query(DatabaseOH.getBaseURI(), null, null, null, null));
        //historyAdapter.noti
        //historyAdapter.notifyDataSetChanged();
        //historyView.invalidate();
        historyView.scrollToPosition(historyAdapter.mReverse(historyAdapter.getItemCount() - 1));
        Toast.makeText(getActivity(), "Refreshed", Toast.LENGTH_SHORT).show();
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        /*if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }*/
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        /*try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }*/
    }

    @Override
    public void onDetach() {
        super.onDetach();
        //mListener = null;
        getActivity().unregisterReceiver(receiver);
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
    /*public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }*/

}
