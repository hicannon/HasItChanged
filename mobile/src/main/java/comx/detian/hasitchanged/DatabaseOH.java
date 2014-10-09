package comx.detian.hasitchanged;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import java.util.Calendar;

public class DatabaseOH extends SQLiteOpenHelper {
    private static final String DBNAME = "HSCdb";
    private static Uri baseURI;

    public DatabaseOH(Context context) {
        super(context, DBNAME, null, 11);
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
     * ID   URL     PROTOCOL    LUDATE      HASH     FAVICON        CONTENT        METHOD
     */
    public static enum COLUMNS{
        _id, URL, PROTOCOL, LUDATE, HASH, FAVICON, CONTENT, METHOD, TYPE, TIMEFRAME, EXACT, DATA
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE HSC ( _id INTEGER PRIMARY KEY AUTOINCREMENT, URL TEXT, PROTOCOL TEXT," +
                "LUDATE TEXT, HASH INTEGER, FAVICON BLOB, CONTENT BLOB, METHOD INTEGER, TYPE INTEGER, TIMEFRAME INTEGER, EXACT INTEGER, DATA BLOB)");

        db.execSQL("INSERT INTO HSC (URL, PROTOCOL, LUDATE, METHOD, TYPE) VALUES ('dd-wrt.com/site/index' , 'http', '"+ HSCMain.df.format(Calendar.getInstance().getTime()) +"', '"+ HSCMain.METHOD.SYNC.ordinal()+"' , '"+ HSCMain.TYPE.REPEATING.ordinal() + "')");
        db.execSQL("INSERT INTO HSC (URL, PROTOCOL, LUDATE) VALUES ('xkcd.com' , 'http', '"+ HSCMain.df.format(Calendar.getInstance().getTime()) +"')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("DBH: onUpgrade", oldVersion + " to " + newVersion);
        db.execSQL("DROP TABLE IF EXISTS HSC");
        onCreate(db);
    }
}
