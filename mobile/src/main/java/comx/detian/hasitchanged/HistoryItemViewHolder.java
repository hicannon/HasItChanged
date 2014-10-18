package comx.detian.hasitchanged;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class HistoryItemViewHolder extends RecyclerView.ViewHolder{
    public TextView mTitle, mDescription, mStatus;
    public ImageView mIcon;
    public View mV;

    public HistoryItemViewHolder(View v, TextView title, TextView description, TextView status, ImageView icon){
        super(v);
        mV = v;
        mTitle = title;
        mDescription = description;
        mStatus = status;
        mIcon = icon;
    }
}
