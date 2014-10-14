package comx.detian.hasitchanged;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.LinkedHashMap;

public class DatabaseOH extends SQLiteOpenHelper {
    public static final Type historyType = new TypeToken<LinkedHashMap<Long, String>>(){}.getType();
    private static final String DBNAME = "HSCdb";
    private static Uri baseURI;

    public DatabaseOH(Context context) {
        super(context, DBNAME, null, 2);
    }

    public static Uri getBaseURI(){
        if (baseURI==null){
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("content").authority(HSCMain.AUTHORITY);
            baseURI = builder.build();
        }
        return baseURI;
    }

    /**
     * 0    1       2           3           4           5               6           7
     * ID   URL     LUDATE      HASH     FAVICON        CONTENT        METHOD, TYPE, TIMEFRAME, EXACT, DATA
     */
    public static enum COLUMNS{
        _id, URL, LUDATE, HASH, ETAG, FAVICON, CONTENT, HISTORY
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE HSC ( _id INTEGER PRIMARY KEY AUTOINCREMENT, URL TEXT, LUDATE TEXT, HASH INTEGER, ETAG TEXT, FAVICON BLOB, CONTENT BLO0, HISTORY TEXT)");

        //db.execSQL("INSERT INTO HSC (URL, PROTOCOL, LUDATE, METHOD, TYPE) VALUES ('dd-wrt.com/site/index' , 'http', '"+ HSCMain.df.format(Calendar.getInstance().getTime()) +"', '"+ HSCMain.METHOD.SYNC.ordinal()+"' , '"+ HSCMain.TYPE.REPEATING.ordinal() + "')");
        db.execSQL("INSERT INTO HSC (_id, URL, LUDATE) VALUES (0, 'SUMMARY', '"+ HSCMain.df.format(new Date()) +"')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("DBH: onUpgrade", oldVersion + " to " + newVersion);
        db.execSQL("DROP TABLE IF EXISTS HSC");
        onCreate(db);
    }
}
