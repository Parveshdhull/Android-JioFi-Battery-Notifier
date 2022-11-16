package espero.jiofibatterynotifier.Services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import espero.jiofibatterynotifier.Activites.MainActivity;
import espero.jiofibatterynotifier.Classes.SharedPrefMain;
import espero.jiofibatterynotifier.R;

public class JioFiService extends Service {

    private static final String TAG_FOREGROUND_SERVICE = "mainActivity";

    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";

    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";


    public JioFiService() {
    }

    private Thread thread;
    private SharedPrefMain sharedPrefMain;
    private RequestQueue queue;
    private String pattern_string = "(id=\"batterylevel\"\\s*value=\")(\\d*)(%\")";
    private Pattern pattern;

    @Override
    public void onDestroy() {
        try {
            if (mp != null) {
                mp.stop();
                mp = null;
            }
            if(thread != null && thread.isAlive()){
                thread.interrupt();
                thread = null;
            }
            if(queue != null){
                queue.cancelAll(TAG_FOREGROUND_SERVICE);
                queue.stop();
                queue = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mp = MediaPlayer.create(this, R.raw.low);
        mp.setLooping(false);
        mp.setVolume(0.5f, 0.5f);
        sharedPrefMain = new SharedPrefMain(this);
        Log.d(TAG_FOREGROUND_SERVICE, "My foreground service onCreate().");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        WifiReceiver broadcastReceiver = new WifiReceiver();
        registerReceiver(broadcastReceiver, intentFilter);
        queue = Volley.newRequestQueue(this);
        pattern = Pattern.compile(pattern_string);
        thread = new Thread(runnable);
        thread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null)
                switch (action) {
                    case ACTION_START_FOREGROUND_SERVICE:
                        startForegroundService();
                        if (checkWifiOnAndConnected()) {
                            wifiChanged(true);
                        }
                        Toast.makeText(getApplicationContext(), "JioFi Battery Notifier Started.", Toast.LENGTH_LONG).show();
                        break;
                    case ACTION_STOP_FOREGROUND_SERVICE:
                        stopForegroundService();
                        Toast.makeText(getApplicationContext(), "JioFi Battery Notifier Stopped.", Toast.LENGTH_LONG).show();
                        break;
                }
        }
        return super.onStartCommand(intent, flags, startId);
    }

 String channelId = "JioFi Battery Notifier";
    /* Used to build and start foreground service. */
    private void startForegroundService() {

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "JioFI Battery Notifier",
                    NotificationManager.IMPORTANCE_HIGH);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
            Notification notification = getNotification("JioFi Battery Notifier Running", R.drawable.icon, false);
            notificationManager.notify(236, notification);
            startForeground(236, notification);
        } else {
            Notification notification = getNotification("JioFi Battery Notifier Running", R.drawable.icon, false);
            startForeground(236, notification);
        }
    }


    private void stopForegroundService() {
        Log.d(TAG_FOREGROUND_SERVICE, "Stop foreground service.");
        stopForeground(true);
        stopSelf();
    }

    private boolean checkWifiOnAndConnected() {
        WifiManager wifiMgr = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

        if (wifiMgr.isWifiEnabled()) { // Wi-Fi adapter is ON

// Disabling extra check of wifi connectivity because,
// newer version of devices always showing not connected,
// without location permission
// Bug: https://issuetracker.google.com/issues/136021574

//            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
//
//            if (wifiInfo.getNetworkId() == -1) {
//                return false; // Not connected to an access point
//            }
            return true; // Connected to an access point
        } else {
            return false; // Wi-Fi adapter is OFF
        }
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            while (thread != null) {
                if (thread.isInterrupted()) {
                    thread = null;
                    break;
                } else {

                    int interval = getInterval();

                    if(queue != null){
                        sendRequest();
                    }

                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };


    private void sendRequest(){
        String url = "http://" + sharedPrefMain.getString("gateway");

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    Matcher m = pattern.matcher(response);
                    String percentage = "50";
                    boolean charging = true;
                    if (m.find( )) {
                        percentage = m.group(2);
                        if(response.toLowerCase().contains("discharging")){
                            charging = false;
                        }
                        updateBatteryData(percentage, charging);
                    }else{
                        updateNotification("JioFi Not Connected", R.drawable.icon, false);
                    }

                }, error -> {
            updateNotification("JioFi Not Connected", R.drawable.icon, false);
//            Log.d(TAG_FOREGROUND_SERVICE, "URL" + url);
//            Log.d(TAG_FOREGROUND_SERVICE, "Error Service:" + error.getMessage());
        });

        stringRequest.setTag(TAG_FOREGROUND_SERVICE);

        queue.add(stringRequest);
    }

    int icons[] = {R.drawable.b0,R.drawable.b1,R.drawable.b2,R.drawable.b3,R.drawable.b4,R.drawable.b5,R.drawable.b6,R.drawable.b7,R.drawable.b8,R.drawable.b9,R.drawable.b10,R.drawable.b11,R.drawable.b12,R.drawable.b13,R.drawable.b14,R.drawable.b15,R.drawable.b16,R.drawable.b17,R.drawable.b18,R.drawable.b19,R.drawable.b20,R.drawable.b21,R.drawable.b22,R.drawable.b23,R.drawable.b24,R.drawable.b25,R.drawable.b26,R.drawable.b27,R.drawable.b28,R.drawable.b29,R.drawable.b30,R.drawable.b31,R.drawable.b32,R.drawable.b33,R.drawable.b34,R.drawable.b35,R.drawable.b36,R.drawable.b37,R.drawable.b38,R.drawable.b39,R.drawable.b40,R.drawable.b41,R.drawable.b42,R.drawable.b43,R.drawable.b44,R.drawable.b45,R.drawable.b46,R.drawable.b47,R.drawable.b48,R.drawable.b49,R.drawable.b50,R.drawable.b51,R.drawable.b52,R.drawable.b53,R.drawable.b54,R.drawable.b55,R.drawable.b56,R.drawable.b57,R.drawable.b58,R.drawable.b59,R.drawable.b60,R.drawable.b61,R.drawable.b62,R.drawable.b63,R.drawable.b64,R.drawable.b65,R.drawable.b66,R.drawable.b67,R.drawable.b68,R.drawable.b69,R.drawable.b70,R.drawable.b71,R.drawable.b72,R.drawable.b73,R.drawable.b74,R.drawable.b75,R.drawable.b76,R.drawable.b77,R.drawable.b78,R.drawable.b79,R.drawable.b80,R.drawable.b81,R.drawable.b82,R.drawable.b83,R.drawable.b84,R.drawable.b85,R.drawable.b86,R.drawable.b87,R.drawable.b88,R.drawable.b89,R.drawable.b90,R.drawable.b91,R.drawable.b92,R.drawable.b93,R.drawable.b94,R.drawable.b95,R.drawable.b96,R.drawable.b97,R.drawable.b98,R.drawable.b99,R.drawable.b100};
    private void updateBatteryData(String percentage, boolean charging){

        int level = getLevel();
        int percentage_int = 0;

        try {
            percentage_int = Integer.parseInt(percentage);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        if(percentage_int < 0){
            percentage_int = 0;
        }else if(percentage_int > 100){
            percentage_int = 100;
        }
        boolean playSound = false;
        int icon = icons[percentage_int];

        String msg = "";
        if(charging){
            msg = "Charging - " + percentage + "%";
        }else{
            msg = "Discharging - " + percentage + "%";
        }
        if(charging){
            sharedPrefMain.setInt("low_msg", 100);
            int high_msg = sharedPrefMain.getInt("high_msg");
            if(percentage_int == 100 && percentage_int >= high_msg){
                playSound = true;
                sharedPrefMain.setInt("high_msg", 200);
            }
        }else{
            sharedPrefMain.setInt("high_msg", 0);
            int low_msg = sharedPrefMain.getInt("low_msg");
            if(percentage_int <= level && percentage_int <= low_msg){
                playSound = true;
                sharedPrefMain.setInt("low_msg", percentage_int/2);
            }
        }
        updateNotification(msg, icon, playSound);

    }
    private int getLevel(){
        int level = 0;
        switch (sharedPrefMain.getString("low")){
            case "10 %":
                level = 10;
                break;
            case "20 %":
                level = 20;
                break;
            case "30 %":
                level = 30;
                break;
            case "40 %":
                level = 40;
                break;
            case "50 %":
                level = 50;
                break;
            case "60 %":
                level = 60;
                break;
            default:
                level = 30;
                break;
        }
        return level;
    }
    private int getInterval(){
        int interval = 0;
        switch (sharedPrefMain.getString("interval")){
            case "5 Sec":
                interval = 5 * 1000;
                break;
            case "10 Sec":
                interval = 10 * 1000;
                break;
            case "30 Sec":
                interval = 30 * 1000;
                break;
            case "60 Sec":
                interval = 60 * 1000;
                break;
            case "2 Min":
                interval = 120 * 1000;
                break;
            case "3 Min":
                interval = 180 * 1000;
                break;
            case "5 Min":
                interval = 300 * 1000;
                break;
            case "10 Min":
                interval = 600 * 1000;
                break;
            default:
                interval = 60 * 1000;
                break;
        }
        return interval;
    }
    private Notification getNotification(String msg, int small_icon, boolean playSound){
        sharedPrefMain.setString("msg", msg);
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent, 0);

        String channelId = getString(R.string.app_name);
        Bitmap largeIconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.icon2);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(small_icon)
                        .setLargeIcon(largeIconBitmap)
                        .setContentTitle(msg)
                        .setColor(getResources().getColor(R.color.colorPrimary))
                        .setContentIntent(pendingIntent).setOnlyAlertOnce(true);
        if(sharedPrefMain.getBoolean("alerts") && playSound &&  ((System.currentTimeMillis() - sharedPrefMain.getLong("notif")) > (1000 * 60 * 5))){
            sharedPrefMain.setLong("notif", System.currentTimeMillis());
            alert();
//            showAlert(msg);
        }


        return notificationBuilder.build();
    }
    private MediaPlayer mp;

    private void alert() {
        if (sharedPrefMain.getBoolean("vibrate")) {
            Vibrator vibrator;
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(500);
        }
        if (sharedPrefMain.getBoolean("sound")) {
            try {
                if (mp != null) {
                    mp.start();
                }
            } catch (Exception e) {
            }
        }
    }

    private void updateNotification(String msg, int icon, boolean playSOund){
        if(!checkWifiOnAndConnected()){
            msg = "JioFi Not Connected";
            icon = R.drawable.icon;
            playSOund = false;
        }
        Notification notification = getNotification(msg, icon, playSOund);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "JioFI Battery Notifier",
                    NotificationManager.IMPORTANCE_HIGH);
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(channel);
            }

        }
        mNotificationManager.notify(236, notification);
    }

    private void showAlert(String msg){
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("dialog", true);
        intent.putExtra("msg", msg);
        startActivity(intent);
    }
    private String gateway;

    private void updateGateway() {
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        int count = 0;
        while (true) {
            count ++;
            if(count > 30){
                break;
            }
            int gateway_int = wifiManager.getDhcpInfo().gateway;
            if (gateway_int == 0) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                gateway = String.format("%d.%d.%d.%d", (gateway_int & 0xff), (gateway_int >> 8 & 0xff), (gateway_int >> 16 & 0xff), (gateway_int >> 24 & 0xff));
                Log.d(TAG_FOREGROUND_SERVICE, "Gateway" + gateway);
                sharedPrefMain.setString("gateway", gateway);
                break;
            }
        }
    }

    private void wifiChanged(Boolean connected) {
        if (connected) {
            updateNotification("Loading...", R.drawable.icon, false);
            updateGateway();

//            if(thread == null) {
//                thread = new Thread(runnable);
//                thread.start();
//            }
        }
        if(queue != null){
            sendRequest();
        }
//        else {
//            if (thread != null && thread.isAlive()) {
//                updateNotification("JioFi Not Connected", R.drawable.icon, false);
//                thread.interrupt();
//                thread = null;
//            }
//        }
    }

    public class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();
            if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
                    wifiChanged(true);
                } else {
                    wifiChanged(false);
                }
            }

        }
    }
}