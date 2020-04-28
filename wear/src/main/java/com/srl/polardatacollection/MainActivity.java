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

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.util.List;

public class MainActivity extends WearableActivity {

    public static final String TAG  = "MainActivity";

    private BroadcastReceiver finishReceiver;
    private BroadcastReceiver newActivityReceiver;
    private String[] newActivity;

    //Set the specific sensors to be used throughout the app
    public final static short TYPE_ACCELEROMETER = Sensor.TYPE_ACCELEROMETER;
    public final static short TYPE_HEART = Sensor.TYPE_HEART_RATE;

    public static int PATIENT_ID = -1;

    private static GoogleApiClient mGoogleApiClient;

    private Intent intentSensing;
    private PowerManager.WakeLock mWakeLock;
    String uid;
    String patient_id_string;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Google API for getting location from phone
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        // Enables Always-on
        setAmbientEnabled();

        newActivity = getIntent().getStringArrayExtra("NEW_ACTIVITY");
        uid = newActivity[0];
        patient_id_string = uid.split("_")[0];
        PATIENT_ID = Integer.parseInt(patient_id_string);
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
                    uid = newActivity[0];
                    startSensing(uid);

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

        startSensing(uid);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        newActivity = getIntent().getStringArrayExtra("NEW_ACTIVITY");
        String uid = newActivity[0];
        startSensing(uid);
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

    private void startSensing(String uid){
        Log.d(TAG, "Starting Sensing...");

        intentSensing.putExtra(Integer.toString(PATIENT_ID), uid);
        startService(intentSensing);

    }

    private void stopSensing(){
        Log.d(TAG, "Stopping Sensing...");
        stopService(intentSensing);
    }

    public static void getLocations() {
        if (mGoogleApiClient == null)
            return;

        final PendingResult<NodeApi.GetConnectedNodesResult> nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient);
        nodes.setResultCallback(result -> {
            final List<Node> nodes1 = result.getNodes();
            if (nodes1 != null) {
                for (int i = 0; i< nodes1.size(); i++) {
                    final Node node = nodes1.get(i);

                    Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), "/MESSAGE", null);
                }
            }
        });
    }
}
