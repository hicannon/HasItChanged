package comx.detian.hasitchanged;

import android.content.Context;
import android.database.Cursor;
import android.widget.SimpleCursorAdapter;

public class HSCCursorAdapter extends SimpleCursorAdapter {

    public HSCCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }
}
