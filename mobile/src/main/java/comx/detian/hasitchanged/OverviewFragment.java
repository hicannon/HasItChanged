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
import android.widget.TextView;
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
    private RecyclerView historyView;
    private HistoryAdapter historyAdapter;
    private BroadcastReceiver receiver;
    private TextView emptyView;

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

        emptyView = (TextView) out.findViewById(R.id.history_empty);

        if (historyAdapter.getItemCount()==0){
            emptyView.setVisibility(View.VISIBLE);
            historyView.setVisibility(View.GONE);
        }else{
            emptyView.setVisibility(View.GONE);
            historyView.setVisibility(View.VISIBLE);
        }

        out.findViewById(R.id.history_button_collapse).setOnClickListener(historyAdapter);
        out.findViewById(R.id.history_button_filter).setOnClickListener(historyAdapter);
        return out;
    }

    private void updateContent() {
        historyAdapter.addAllFromCurosr(getActivity().getContentResolver().query(DatabaseOH.getBaseURI(), null, null, null, null));
        if (historyAdapter.getItemCount()==0){
            emptyView.setVisibility(View.VISIBLE);
            historyView.setVisibility(View.GONE);
        }else{
            emptyView.setVisibility(View.GONE);
            historyView.setVisibility(View.VISIBLE);
        }
        historyView.scrollToPosition(historyAdapter.mReverse(historyAdapter.getItemCount() - 1));
        if (isResumed() && isVisible())
            Toast.makeText(getActivity(), "Refreshed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        historyAdapter.addAllFromCurosr(null);
        getActivity().unregisterReceiver(receiver);
    }
}
