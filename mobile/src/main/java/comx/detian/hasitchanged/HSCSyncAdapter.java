package comx.detian.hasitchanged;

import android.accounts.Account;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import java.util.Objects;

public class HSCSyncAdapter extends AbstractThreadedSyncAdapter {
    //ContentResolver mCR;

    public HSCSyncAdapter(Context context, boolean autoInitialize){
        super(context, autoInitialize);

        /*if (context!=null)
            mCR = context.getContentResolver();
        else
            Log.d("SyncAdapter: Constructor", "Context is null");*/
    }

    public HSCSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs){
        super(context, autoInitialize, allowParallelSyncs);

        //mCR = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        //TODO
        Log.d("Sync: onPerform", "called");
        //Intent i = new Intent(SYNC_FINISHED);
        //sendBroadcast(i);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this.getContext())
                .setContentTitle("Sync finished")
                .setContentText("Hello World")
                .setSmallIcon(R.drawable.ic_launcher);

        Intent intent = new Intent(this.getContext(), HSCMain.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this.getContext());
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(HSCMain.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) this.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(42, mBuilder.build());
    }
}
