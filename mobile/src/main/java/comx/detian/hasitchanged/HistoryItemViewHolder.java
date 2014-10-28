package comx.detian.hasitchanged;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class HistoryItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    public TextView mTitle, mDescription, mStatus;
    public ImageView mIcon;
    public View mV;
    public String id;

    public HistoryItemViewHolder(View v, TextView title, TextView description, TextView status, ImageView icon) {
        super(v);
        mV = v;
        mV.setOnClickListener(this);
        mTitle = title;
        mDescription = description;
        mStatus = status;
        mIcon = icon;
    }

    @Override
    public void onClick(View v) {
        //TODO this will eventually be past versions
        Intent browerIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + mTitle.getText().toString()));
        v.getContext().startActivity(browerIntent);
    }
}
