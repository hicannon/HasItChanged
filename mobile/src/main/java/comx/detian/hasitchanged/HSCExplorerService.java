package comx.detian.hasitchanged;

import android.app.Service;
import android.content.Intent;
import android.content.SyncAdapterType;
import android.os.IBinder;

public class HSCExplorerService extends Service {
    private static HSCSyncAdapter mSA = null;
    private static final Object mSALock = new Object();

    public HSCExplorerService() {
        synchronized (mSALock) {
            if (mSA == null) {
                mSA = new HSCSyncAdapter(getApplicationContext(), false);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mSA.getSyncAdapterBinder();
    }
}
