package comx.detian.hasitchanged;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class HistoryAdapter extends BaseAdapter implements View.OnClickListener {
    static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy - HH:mm:SS");
    boolean collapse = true;

    Cursor cursor;
    int cursorStart = -1;

    Context context;

    Gson gson;
    TreeMap<Long, String> data;
    HashMap<String, Bitmap> favicons;

    private Object[] keys;
    private Object[] values;

    HistoryAdapter (Context context, Cursor c){
        data = new TreeMap<Long, String>();
        favicons = new HashMap<String, Bitmap>();

        this.context = context;
        gson = new GsonBuilder().create();
        cursor = c;
        addAllFromCurosr(c);
    }

    void clear(){
        data.clear();
        //Probably don't need to clear favicons
    }

    synchronized void addAllFromCurosr(Cursor cursor) {
        this.cursor = cursor;
        cursorStart = cursor.getPosition();

        //Skip first dummy
        cursor.moveToNext();

        while (cursor.moveToNext()){
            String historyRaw = cursor.getString(DatabaseOH.COLUMNS.HISTORY.ordinal());
            String url = cursor.getString(DatabaseOH.COLUMNS.URL.ordinal());
            if (historyRaw==null || historyRaw.length()==0){
                continue;
            }else{
                Map<Long, String> history = gson.fromJson(historyRaw, DatabaseOH.historyType);
                long firstTimeStamp=-1, lastTimeStamp=-1;
                String lastStatus = "";
                int count = 0;
                for (long timeStamp : history.keySet()){
                    if (collapse) {
                        if (lastStatus.length() == 0) {
                            lastStatus = history.get(timeStamp);
                            firstTimeStamp = timeStamp;
                        }
                        if (!lastStatus.equals(history.get(timeStamp))) {
                            data.put(lastTimeStamp, url + "\t" + lastStatus + "\t" + firstTimeStamp + "\t" + count);
                            firstTimeStamp = timeStamp;
                            //lastStatus = history.get(timeStamp);
                            count = 0;
                            //continue;
                        }
                        lastTimeStamp = timeStamp;
                        lastStatus = history.get(timeStamp);
                        count++;
                    }else{
                        data.put(timeStamp, url+"\t"+ history.get(timeStamp) +"\t"+1234+"\t"+1);
                    }
                }
                if (collapse)
                    data.put(lastTimeStamp, url+"\t"+ lastStatus+"\t"+firstTimeStamp+"\t"+count);
            }
            byte[] rawFavicon = cursor.getBlob(DatabaseOH.COLUMNS.FAVICON.ordinal());
            if (rawFavicon!=null && rawFavicon.length!=0) {
                favicons.put(url, BitmapFactory.decodeByteArray(rawFavicon, 0, rawFavicon.length));
            }
        }

        keys = data.keySet().toArray();
        values = data.values().toArray();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int i) {
        i = getCount()-i-1;
        return values[i];
    }

    public Object getKey(int i) {
        i = getCount()-i-1;
        return keys[i];
    }

    @Override
    public long getItemId(int i) {
        i = getCount()-i-1;
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View out;
        if (view!=null){
            out = view;
        }
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        out = inflater.inflate(R.layout.history_item,viewGroup, false);
        ImageView favicon = (ImageView) out.findViewById(R.id.history_icon);
        TextView status = (TextView) out.findViewById(R.id.history_status);
        TextView title = (TextView) out.findViewById(R.id.history_title);
        TextView description = (TextView) out.findViewById(R.id.history_description);

        String[] raw = ((String)getItem(i)).split("\t");
        String url = raw[0];
        String statusCode = raw[1].substring(1);
        Long firstTimeStamp = Long.parseLong(raw[2]);
        int count = Integer.parseInt(raw[3]);

        boolean changed = raw[1].charAt(0)=='C';

        if (favicons.containsKey(url)){
            favicon.setImageBitmap(favicons.get(url));
        }
        title.setText(url);
        description.setText( (count==1 ? "" : count + "X from\n"+dateFormat.format(new Date(firstTimeStamp)) + " to ") + dateFormat.format(new Date((Long)getKey(i))));

        switch(Integer.parseInt(statusCode)){
            case 200:
                //status.setImageResource(R.drawable.status_ok);
                status.setTextColor(Color.GREEN);
                break;
            case 304:
                //status.setImageResource(R.drawable.status_304);
                changed = false;
                status.setTextColor(Color.BLACK);
                break;
            case SiteResponse.IOEXCEPTION:
                status.setTextColor(Color.RED);
                statusCode = "DNS";
                break;
            case SiteResponse.MALFORMEDURL:
                status.setTextColor(Color.RED);
                statusCode = "URL";
                break;
            default: break;
        }
        status.setText(statusCode);

        if (changed){
            out.setBackgroundColor(Color.parseColor("#dcbddf"));;
        }else{
            out.setBackgroundColor(Color.WHITE);
        }
        return out;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.history_button_collapse){
            collapse = !collapse;
            clear();
            cursor.moveToPosition(cursorStart);
            addAllFromCurosr(cursor);
            notifyDataSetChanged();
            System.out.println("Changed");
        }
    }
}
