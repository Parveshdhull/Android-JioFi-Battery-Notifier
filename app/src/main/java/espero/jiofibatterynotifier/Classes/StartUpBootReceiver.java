package espero.jiofibatterynotifier.Classes;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import espero.jiofibatterynotifier.Services.JioFiService;

public class StartUpBootReceiver extends BroadcastReceiver {
    private SharedPrefMain sharedPrefMain;

    @Override
    public void onReceive(Context context, Intent intent) {

        sharedPrefMain = new SharedPrefMain(context);
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if(!isMyServiceRunning(JioFiService.class, context) && sharedPrefMain.getBoolean("start")){
                Intent intent2 = new Intent(context, JioFiService.class);
                intent2.setAction(JioFiService.ACTION_START_FOREGROUND_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent2);
                } else {
                    context.startService(intent2);
                }
            }
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}