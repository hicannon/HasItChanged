package comx.detian.hasitchanged;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseOH extends SQLiteOpenHelper {
    private static final String DBNAME = "HSCdb";

    public DatabaseOH(Context context) {
        super(context, DBNAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE HSC (ID INTEGER PRIMARY KEY, URL TEXT, LUDATE TEXT, CHANGED INTEGER, DATA BLOB)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {

    }
}
