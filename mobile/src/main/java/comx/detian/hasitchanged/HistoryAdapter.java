package comx.detian.hasitchanged;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dictiography.collections.IndexedTreeMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryItemViewHolder> implements View.OnClickListener {
    static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM - HH:mm:ss");
    boolean collapse = true;

    Cursor cursor;
    int cursorStart = -1;

    Context context;

    Gson gson;
    IndexedTreeMap<Long, String> data;
    HashMap<String, Bitmap> favicons;
    private boolean reverse;

    HistoryAdapter(Context context, Cursor c) {
        data = new IndexedTreeMap<Long, String>();
        favicons = new HashMap<String, Bitmap>();

        this.context = context;
        gson = new GsonBuilder().create();
        cursor = c;
        reverse = true;
        addAllFromCurosr(c);
        //TODO maybe not necessary
        notifyDataSetChanged();
    }

    void clear() {
        data.clear();
        //Probably don't need to clear favicons
    }

    synchronized void addAllFromCurosr(Cursor cursor) {
        //Normally the list will only grow, the only other case is clear all history
        int totalCount = 0;

        //If we're actually changing to a different cursor, close the old one
        if (this.cursor!=null && this.cursor!=cursor){
            this.cursor.close();
        }

        this.cursor = cursor;

        if (cursor==null){
            return;
        }

        cursorStart = cursor.getPosition();

        //Skip first dummy
        cursor.moveToNext();

        while (cursor.moveToNext()) {
            String historyRaw = cursor.getString(DatabaseOH.COLUMNS.HISTORY.ordinal());
            String url = cursor.getString(DatabaseOH.COLUMNS.URL.ordinal());
            if (historyRaw == null || historyRaw.length() == 0) {
                continue;
            } else {
                Map<Long, String> history = gson.fromJson(historyRaw, DatabaseOH.historyType);
                totalCount+=history.size();
                long firstTimeStamp = -1, lastTimeStamp = -1, previousKeyTimeStamp = -1;
                String lastStatus = "";
                int count = 0;
                for (long timeStamp : history.keySet()) {
                    if (collapse) {
                        if (lastStatus.length() == 0) {
                            lastStatus = history.get(timeStamp);
                            firstTimeStamp = timeStamp;
                        }
                        if (!lastStatus.equals(history.get(timeStamp))) {
                            String lastVal = data.get(lastTimeStamp);
                            String status = data.put(lastTimeStamp, url + "\t" + lastStatus + "\t" + firstTimeStamp + "\t" + count);

                            //Update UI if necessary
                            if (data.get(lastTimeStamp).equals(lastVal)) {
                                //Do nothing, unchanged
                            } else if (status == null) {
                                //not previously in map, check to see if this is actually an update to a collapsed entry
                                if (previousKeyTimeStamp != -1) {
                                    int previousIndex = data.keyIndex(previousKeyTimeStamp);
                                    data.remove(previousKeyTimeStamp);
                                    int newIndex = data.keyIndex(lastTimeStamp);
                                    if (previousIndex == newIndex) {
                                        //Log.d("HistoryAdapter", newIndex + " has changed");
                                        notifyItemChanged(mReverse(newIndex));
                                    } else {
                                        //Log.d("HistoryAdapter", previousIndex + " has been moved to " + newIndex);
                                        notifyItemMoved(mReverse(previousIndex), mReverse(newIndex));
                                    }
                                } else {
                                    //Newly added
                                    //Log.d("HistoryAdapter", data.keyIndex(lastTimeStamp) + " has been added");
                                    notifyItemInserted(mReverse(data.keyIndex(lastTimeStamp)));
                                }
                            } else {
                                //Log.d("HistoryAdapter", data.keyIndex(lastTimeStamp) + " has changed");
                                notifyItemChanged(mReverse(data.keyIndex(lastTimeStamp)));
                            }
                            firstTimeStamp = timeStamp;
                            count = 0;
                            previousKeyTimeStamp = -1;
                        }

                        if (data.containsKey(timeStamp)) {
                            previousKeyTimeStamp = timeStamp;
                        }

                        lastTimeStamp = timeStamp;
                        lastStatus = history.get(timeStamp);
                        count++;
                    } else {
                        String status = data.put(timeStamp, url + "\t" + history.get(timeStamp) + "\t" + 1234 + "\t" + 1);
                        if (status == null) {
                            notifyItemInserted(mReverse(data.keyIndex(timeStamp)));
                        }
                    }
                }

                //Finish up last interval, don't need to check since Map does it for us
                if (collapse) {
                    String lastVal = data.get(lastTimeStamp);
                    String status = data.put(lastTimeStamp, url + "\t" + lastStatus + "\t" + firstTimeStamp + "\t" + count);
                    //Update UI if necessary
                    if (data.get(lastTimeStamp).equals(lastVal)) {
                        //Do nothing, unchanged
                    } else if (status == null) {
                        //not previously in map, check to see if this is actually an update to a collapsed entry
                        if (previousKeyTimeStamp != -1) {
                            int previousIndex = data.keyIndex(previousKeyTimeStamp);
                            data.remove(previousKeyTimeStamp);
                            int newIndex = data.keyIndex(lastTimeStamp);
                            if (previousIndex == newIndex) {
                                //Log.d("HistoryAdapter-e", newIndex + " has changed");
                                notifyItemChanged(mReverse(newIndex));
                            } else {
                                //Log.d("HistoryAdapter-e", previousIndex + " has been moved to " + newIndex);
                                notifyItemMoved(mReverse(previousIndex), mReverse(newIndex));
                                notifyItemChanged(mReverse(newIndex));
                            }
                        } else {
                            //Newly added
                            //Log.d("HistoryAdapter-e", mReverse(data.keyIndex(lastTimeStamp)) + " has been added");
                            notifyItemInserted(mReverse(data.keyIndex(lastTimeStamp)));
                        }
                    }
                }
            }
            byte[] rawFavicon = cursor.getBlob(DatabaseOH.COLUMNS.FAVICON.ordinal());
            if (rawFavicon != null && rawFavicon.length != 0) {
                favicons.put(url, BitmapFactory.decodeByteArray(rawFavicon, 0, rawFavicon.length));
            }
        }

        //This is a clear all scenario
        if (totalCount==0){
            notifyItemRangeRemoved(0, data.size());
            clear();
        }
    }

    protected int mReverse(int i) {
        return reverse ? getItemCount() - i - 1 : i;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    //@Override
    public String getItem(int i) {
        return data.get(getKey(i));
    }

    public long getKey(int i) {
        return data.exactKey(mReverse(i));
    }

    @Override
    public HistoryItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.history_item, viewGroup, false);
        ImageView favicon = (ImageView) v.findViewById(R.id.history_icon);
        TextView status = (TextView) v.findViewById(R.id.history_status);
        TextView title = (TextView) v.findViewById(R.id.history_title);
        TextView description = (TextView) v.findViewById(R.id.history_description);

        title.setTypeface(HSCMain.getRobotoRegular(context));
        status.setTypeface(HSCMain.getRobotoRegular(context));
        description.setTypeface(HSCMain.getRobotoRegular(context));
        return new HistoryItemViewHolder(v, title, description, status, favicon);
    }

    @Override
    public void onBindViewHolder(HistoryItemViewHolder viewHolder, int i) {
        String[] raw = getItem(i).split("\t");
        String url = raw[0];
        String statusCode = raw[1].substring(1);
        Long firstTimeStamp = Long.parseLong(raw[2]);
        int count = Integer.parseInt(raw[3]);

        boolean changed = raw[1].charAt(0) == 'C';

        if (favicons.containsKey(url)) {
            viewHolder.mIcon.setImageBitmap(favicons.get(url));
        } else {
            viewHolder.mIcon.setImageResource(android.R.drawable.ic_menu_report_image);
        }
        viewHolder.mTitle.setText(url);
        viewHolder.mDescription.setText((count == 1 ? "" : count + "x from " + dateFormat.format(new Date(firstTimeStamp)) + " to ") + dateFormat.format(new Date(getKey(i))));

        switch (Integer.parseInt(statusCode)) {
            case 200:
                //status.setImageResource(R.drawable.status_ok);
                viewHolder.mStatus.setTextColor(Color.GREEN);
                break;
            case 304:
                //status.setImageResource(R.drawable.status_304);
                changed = false;
                viewHolder.mStatus.setTextColor(Color.BLACK);
                break;
            case SiteResponse.IOEXCEPTION:
                viewHolder.mStatus.setTextColor(Color.RED);
                statusCode = "DNS";
                break;
            case SiteResponse.MALFORMEDURL:
                viewHolder.mStatus.setTextColor(Color.RED);
                statusCode = "URL";
                break;
            default:
                break;
        }
        viewHolder.mStatus.setText(statusCode);

        if (changed) {
            viewHolder.mV.setBackgroundColor(Color.parseColor("#ffd180"));
        } else {
            viewHolder.mV.setBackgroundColor(Color.WHITE);
        }
    }

    @Override
    public long getItemId(int i) {
        return mReverse(i);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.history_button_collapse) {
            collapse = !collapse;
            clear();
            cursor.moveToPosition(cursorStart);
            addAllFromCurosr(cursor);
            notifyDataSetChanged();
        } else if (view.getId() == R.id.history_button_filter) {
            reverse = !reverse;
            for (int i = 0; i < getItemCount() / 2; i++) {
                notifyItemMoved(i, getItemCount() - i - 1);
                //Note not symmetric b/c view acts like queue; items are pushed up
                notifyItemMoved(getItemCount() - i - 2, i);
            }
        }
    }
}
