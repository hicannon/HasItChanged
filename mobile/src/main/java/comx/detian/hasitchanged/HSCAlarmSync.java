package comx.detian.hasitchanged;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class HSCAlarmSync extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("SYNC_STATUS", "Wakeup from alarm");
        HSCMain.requestSyncNow(context.getApplicationContext(), -1, false);
    }
}
