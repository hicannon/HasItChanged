package comx.detian.hasitchanged;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import java.util.Calendar;

public class DatabaseOH extends SQLiteOpenHelper {
    private static final String DBNAME = "HSCdb";
    public static Uri baseURI;

    public DatabaseOH(Context context) {
        super(context, DBNAME, null, 5);

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("content").authority(HSCMain.AUTHORITY);
        baseURI = builder.build();
    }

    /**
     * 0    1       2           3           4           5               6
     * ID   URL     PROTOCOL    LUDATE      HASH     CONTENT        DATA
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE HSC (ID INTEGER PRIMARY KEY AUTOINCREMENT, URL TEXT, PROTOCOL TEXT, LUDATE TEXT, HASH INTEGER, CONTENT BLOB, DATA BLOB)");

        db.execSQL("INSERT INTO HSC (URL, PROTOCOL, LUDATE) VALUES ('google.com' , 'http', '"+ HSCMain.df.format(Calendar.getInstance().getTime()) +"')");
        db.execSQL("INSERT INTO HSC (URL, PROTOCOL, LUDATE) VALUES ('xkcd.com' , 'http', '"+ HSCMain.df.format(Calendar.getInstance().getTime()) +"')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("DBH: onUpgrade", oldVersion + " to " + newVersion);
        db.execSQL("DROP TABLE IF EXISTS HSC");
        onCreate(db);
    }
}
