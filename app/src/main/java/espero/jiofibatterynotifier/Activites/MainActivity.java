package espero.jiofibatterynotifier.Activites;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import espero.jiofibatterynotifier.Classes.SharedPrefMain;
import espero.jiofibatterynotifier.Classes.StartUpBootReceiver;
import espero.jiofibatterynotifier.R;
import espero.jiofibatterynotifier.Services.JioFiService;

public class MainActivity extends AppCompatActivity {

    private TextView mainText;
    private Spinner level, interval, alerts, sound, vibrate;
    private Button button;
    private SharedPrefMain sharedPrefMain;
    private String TAG = "mainActivity";
    private Thread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bundle extras = getIntent().getExtras();

        sharedPrefMain = new SharedPrefMain(this);
        if (extras != null) {
            mp = MediaPlayer.create(this, R.raw.low);
            mp.setLooping(false);
            mp.setVolume(0.5f, 0.5f);
            boolean dialog = extras.getBoolean("dialog");
            String msg = extras.getString("msg");

            if (dialog) {
                showAlert(msg);
            }
        }
        mainText = findViewById(R.id.main_text);
        button = findViewById(R.id.button);
        level = findViewById(R.id.level_spinner);
        interval = findViewById(R.id.interval_spinner);
        alerts = findViewById(R.id.alerts_spinner);
        sound = findViewById(R.id.sound_spinner);
        vibrate = findViewById(R.id.vibrate_spinner);



        if (sharedPrefMain.getBoolean("firstTime")) {
            sharedPrefMain.setBoolean("firstTime", false);
            sharedPrefMain.setString("interval", "5 Sec");
            sharedPrefMain.setString("low", "30 %");
            sharedPrefMain.setString("msg", "Stopped");
            sharedPrefMain.setInt("low_msg", 100);
            sharedPrefMain.setInt("high_msg", 0);
            sharedPrefMain.setBoolean("start", false);
        }
        createLayout();

        try {
            mainText.setBackgroundResource(R.drawable.rectangle_animation);
            AnimationDrawable frameAnimation2 = (AnimationDrawable) mainText.getBackground();
            frameAnimation2.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        interval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String item = parent.getAdapter().getItem(position).toString();
                Log.d(TAG, item);
                sharedPrefMain.setString("interval", item);
//                Toast.makeText(MainActivity.this, "Updated", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        level.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String item = parent.getAdapter().getItem(position).toString();
                Log.d(TAG, item);
                sharedPrefMain.setString("low", item);
//                Toast.makeText(MainActivity.this, "Updated", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        alerts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    sharedPrefMain.setBoolean("alerts", true);
                } else {
                    sharedPrefMain.setBoolean("alerts", false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        sound.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    sharedPrefMain.setBoolean("sound", true);
                } else {
                    sharedPrefMain.setBoolean("sound", false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        vibrate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    sharedPrefMain.setBoolean("vibrate", true);
                } else {
                    sharedPrefMain.setBoolean("vibrate", false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (button.getText().equals("Stop")) {
                    button.setText("Start");
                    stopService();
                } else {
                    button.setText("Stop");
                    startService();
                }
            }
        });


        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                while (true) {
                    if (myHandler != null)
                        myHandler.sendEmptyMessage(0);

                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        thread = new Thread(runnable);
        thread.start();

        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
            StartUpBootReceiver broadcastReceiver = new StartUpBootReceiver();
            registerReceiver(broadcastReceiver, intentFilter);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void showAlert(String msg) {
        alert();
        String title = "JioFi Battery Low";
        if (msg.contains("100")) {
            title = "JioFi Battery Full";
        }
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("Ok", null)
                .setCancelable(false)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert).create().show();
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
                Log.d(TAG, "Error:" + e.getMessage());
            }
        }
    }

    private final Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (mainText != null) {
                mainText.setText(sharedPrefMain.getString("msg"));
            }
        }
    };

    private void createLayout() {
        if (isMyServiceRunning(JioFiService.class)) {
            button.setText("Stop");
        } else {

            sharedPrefMain.setString("msg", "Stopped");
            button.setText("Start");
            if (sharedPrefMain.getBoolean("start")) {
                startService();
                button.setText("Stop");
            }
        }
        interval.setSelection(((ArrayAdapter<String>) interval.getAdapter()).getPosition(sharedPrefMain.getString("interval")));
        level.setSelection(((ArrayAdapter<String>) level.getAdapter()).getPosition(sharedPrefMain.getString("low")));
        if (sharedPrefMain.getBoolean("alerts")) {
            alerts.setSelection(0);
        } else {
            alerts.setSelection(1);
        }
        if (sharedPrefMain.getBoolean("sound")) {
            sound.setSelection(0);
        } else {
            sound.setSelection(1);
        }
        if (sharedPrefMain.getBoolean("vibrate")) {
            vibrate.setSelection(0);
        } else {
            vibrate.setSelection(1);
        }
    }

    private void startService() {
        requestWhitelist();
        sharedPrefMain.setBoolean("start", true);
        Intent intent = new Intent(MainActivity.this, JioFiService.class);
        intent.setAction(JioFiService.ACTION_START_FOREGROUND_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void stopService() {
        sharedPrefMain.setBoolean("start", false);
        sharedPrefMain.setString("msg", "Stopped");
        Intent intent = new Intent(MainActivity.this, JioFiService.class);
        intent.setAction(JioFiService.ACTION_STOP_FOREGROUND_SERVICE);
        startService(intent);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    public void openJioFiPanel(View view) {
        Log.d(TAG, "Clicked");
        String gateway = sharedPrefMain.getString("gateway");
        if (gateway.isEmpty()) {
            gateway = "http://192.168.225.1";
        } else {
            gateway = "http://" + gateway;
        }

        CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder();
        intentBuilder.setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        intentBuilder.setSecondaryToolbarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));

        try {
            CustomTabsIntent customTabsIntent = intentBuilder.build();
            customTabsIntent.intent.setPackage("com.android.chrome");
            customTabsIntent.launchUrl(this, Uri.parse(gateway));
        } catch (Exception e) {
            try {
                CustomTabsIntent customTabsIntent = intentBuilder.build();
                customTabsIntent.launchUrl(this, Uri.parse(gateway));
            } catch (Exception e1) {
                Log.d(TAG, "Error:" + e1.getMessage());
            }
        }

    }

    public void openPlayStore(View view) {
        startActivityForResult(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.buymeacoffee.com/parveshmonu")), 1);
    }

    private void requestWhitelist() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onStop() {

        try {
            if (mp != null) {
                mp.stop();
                mp = null;
            }
            if (thread != null) {
                thread.interrupt();
                thread = null;
            }
        } catch (Exception e) {

        }

        super.onStop();
    }
}
