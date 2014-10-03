package comx.detian.hasitchanged;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

public class HSCSyncAdapter extends AbstractThreadedSyncAdapter {
    ContentResolver mCR;

    public HSCSyncAdapter(Context context, boolean autoInitialize){
        super(context, autoInitialize);

        mCR = context.getContentResolver();
    }
    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {

    }
}
