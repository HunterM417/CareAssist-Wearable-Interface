package com.srl.polardatacollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;

import java.io.File;

public class MainActivity extends WearableActivity {

    public static final String TAG  = "MainActivity";

    private BroadcastReceiver finishReceiver;
    private BroadcastReceiver newActivityReceiver;
    private String[] newActivity;

    //Set the specific sensors to be used throughout the app
    public final static short TYPE_ACCELEROMETER = Sensor.TYPE_ACCELEROMETER;
    public final static short TYPE_GYROSCOPE = Sensor.TYPE_GYROSCOPE;
    public final static short TYPE_GRAVITY = Sensor.TYPE_GRAVITY;

    public static String ACTIVITY = "com.srl.polardatacollection.ACTIVITY_WEAR";
    public static String FILENAME = "com.srl.polardatacollection.FILENAME_WEAR";

    private Intent intentSensing;
    private PowerManager.WakeLock mWakeLock;
    String activity;
    String filename;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Enables Always-on
        setAmbientEnabled();

        newActivity = getIntent().getStringArrayExtra("NEW_ACTIVITY");
        filename = newActivity[0];
        activity = newActivity[1];
        finishReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null && action.equals("finish_activity")) {
                    finish();
                }
            }
        };

        newActivityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null && action.equals("new_activity")) {
                    newActivity = intent.getStringArrayExtra("NEW_ACTIVITY");
                    filename = newActivity[0];
                    activity = newActivity[1];
                    startSensing(filename, activity);

                }
            }
        };

        registerReceiver(finishReceiver, new IntentFilter("finish_activity"));
        registerReceiver(newActivityReceiver, new IntentFilter("new_activity"));
        intentSensing = new Intent(this, SensorService.class);

        if(mWakeLock == null){
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"wear:wakelock");
        }

        mWakeLock.acquire();

        File traceFile = new File(this.getExternalFilesDir(null), filename + ".csv");
        int fileNumber = 1;
        String testFilename = filename;
        while (traceFile.exists()) {
            testFilename = filename + fileNumber;
            traceFile = new File(this.getExternalFilesDir(null), testFilename + ".csv");
            fileNumber += 1;
        }
        filename = testFilename;
        System.out.println(filename);
        startSensing(filename, activity);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        newActivity = getIntent().getStringArrayExtra("NEW_ACTIVITY");
        filename = newActivity[0];
        activity = newActivity[1];
        startSensing(filename, activity);
    }

    public void onDestroy() {
        super.onDestroy();
        if(mWakeLock != null){
            mWakeLock.release();
        }
        unregisterReceiver(finishReceiver);
        unregisterReceiver(newActivityReceiver);
        stopSensing();
    }

    private void startSensing(String filename, String activity){
        Log.d(TAG, "Starting Sensing...");

        intentSensing.putExtra(ACTIVITY, activity);
        intentSensing.putExtra(FILENAME, filename);
        startService(intentSensing);

    }

    private void stopSensing(){
        Log.d(TAG, "Stopping Sensing...");
        stopService(intentSensing);
    }
}
