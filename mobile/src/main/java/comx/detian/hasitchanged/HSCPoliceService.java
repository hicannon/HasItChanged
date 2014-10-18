package comx.detian.hasitchanged;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class HSCPoliceService extends Service {
    private HSCAuthenticator mA;

    public HSCPoliceService() {
        mA = new HSCAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mA.getIBinder();
    }
}
