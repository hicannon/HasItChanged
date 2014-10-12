package comx.detian.hasitchanged;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

public class HSCProvider extends ContentProvider{

    private SQLiteOpenHelper sqloh;

    private SQLiteDatabase db;

    /*private static final UriMatcher mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static{
        mUriMatcher.addURI("comx.detian.hasitchanged.HSCProvider", );
    }*/

    @Override
    public boolean onCreate() {
        sqloh = new DatabaseOH(getContext());

        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor out = sqloh.getReadableDatabase().query("HSC", projection, selection, selectionArgs, null, null, sortOrder);
        Log.d("PROVIDER: query has "+out.getCount(), uri.toString());
        return out;
    }

    @Override
    public String getType(Uri uri) {
        return "text/plain";
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        long id = sqloh.getWritableDatabase().insert("HSC", null, contentValues);
        return ContentUris.withAppendedId(DatabaseOH.getBaseURI(), id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.d("PROVIDER: delete", uri.toString());
        return sqloh.getWritableDatabase().delete("HSC", selection, selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        Log.d("PROVIDER: update", uri.toString());
        return sqloh.getWritableDatabase().update("HSC", contentValues, selection, selectionArgs);
    }
}
