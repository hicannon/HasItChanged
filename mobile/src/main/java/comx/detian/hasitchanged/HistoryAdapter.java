package comx.detian.hasitchanged;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class HistoryAdapter extends BaseAdapter {
    Context context;

    Gson gson;
    TreeMap<Long, String> data;
    HashMap<String, Bitmap> favicons;
    HistoryAdapter (Context context, Cursor c){
        data = new TreeMap<Long, String>();
        favicons = new HashMap<String, Bitmap>();

        this.context = context;
        gson = new GsonBuilder().create();
        addAllFroomCurosr(c);
    }

    public void addAllFroomCurosr(Cursor cursor) {
        //Skip first dummy
        cursor.moveToNext();

        while (cursor.moveToNext()){
            String historyRaw = cursor.getString(DatabaseOH.COLUMNS.HISTORY.ordinal());
            String url = cursor.getString(DatabaseOH.COLUMNS.URL.ordinal());
            if (historyRaw==null || historyRaw.length()==0){
                continue;
            }else{
                Map<Long, String> history = gson.fromJson(historyRaw, DatabaseOH.historyType);
                for (long timeStamp : history.keySet()){
                    data.put(timeStamp, url+"\t"+ history.get(timeStamp));
                }
            }
            byte[] rawFavicon = cursor.getBlob(DatabaseOH.COLUMNS.FAVICON.ordinal());
            if (rawFavicon!=null && rawFavicon.length!=0) {
                favicons.put(url, BitmapFactory.decodeByteArray(rawFavicon, 0, rawFavicon.length));
            }
        }
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int i) {
        i = getCount()-i-1;
        //TODO store array instead of creating
        return data.values().toArray()[i];
    }

    public Object getKey(int i) {
        i = getCount()-i-1;
        //TODO store array instead of creating
        return data.keySet().toArray()[i];
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

        boolean changed = raw[1].charAt(0)=='C';

        if (favicons.containsKey(url)){
            favicon.setImageBitmap(favicons.get(url));
        }
        title.setText(url);
        description.setText(new Date((Long)getKey(i)).toString());

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
}
