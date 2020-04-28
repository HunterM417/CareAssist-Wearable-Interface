package com.srl.polardatacollection;

import android.content.Context;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.core.auth.StitchUser;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoCollection;
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteInsertOneResult;

import org.bson.Document;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SensorService extends Service implements SensorEventListener {

    //This can't be set below 10ms due to Android/hardware limitations. Use 9 to get more accurate 10ms intervals
    final short POLL_FREQUENCY = 3000; //in milliseconds

    public static int PATIENT_ID = -1;

    public static StitchAppClient client =
            Stitch.initializeDefaultAppClient("careassiststitchapp-owlqs");

    public static RemoteMongoClient mongoClient =
            client.getServiceClient(RemoteMongoClient.factory, "mongodb-atlas");

    public static RemoteMongoCollection<Document> coll =
            mongoClient.getDatabase("careAssist").getCollection("datas");

    private long lastUpdate = -1;
    long curTime;

    private SensorManager sensorManager = null;

    Sensor sensor;
    Sensor accelerometer;
    Sensor heartRate;

    float[] accelerometerMatrix = new float[3];
    float[] heartrateMatrix = new float[1];

    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCreate() {
        super.onCreate();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(MainActivity.TYPE_ACCELEROMETER);
        heartRate = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        String res = "";
        for(int i=0; i<sensors.size(); i++) {
            res = res + sensors.get(i).toString();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        System.out.println("Starting service");
        registerListener();
        return START_STICKY;
    }

    private void registerListener() {
        sensorManager.registerListener(this, accelerometer, 50000);
        sensorManager.registerListener(this, heartRate, 50000);
    }

    private void unregisterListener() {
        sensorManager.unregisterListener(this);
    }

    public void onSensorChanged(SensorEvent event) {
        sensor = event.sensor;

        int i = sensor.getType();
        if (i == MainActivity.TYPE_ACCELEROMETER) {
            accelerometerMatrix = event.values;
        } else if (i == MainActivity.TYPE_HEART) {
            heartrateMatrix = event.values;
        }

        //This if statement is because whether to use elapsedRealtimeNanos() or nanoTime() to convert the sensor event time to epoch time depends on the specific device
        if (Math.abs((event.timestamp - System.nanoTime())/1000000L) < Math.abs((event.timestamp - SystemClock.elapsedRealtimeNanos())/1000000L)) {
            curTime = System.currentTimeMillis() + (event.timestamp - System.nanoTime())/1000000L;
        } else {
            curTime = System.currentTimeMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos())/1000000L;
        }

        // only allow one update every POLL_FREQUENCY (convert from ms to nano for comparison).
        if((curTime - lastUpdate) > POLL_FREQUENCY) {

            lastUpdate = curTime;

            float[] coordinates = new float[accelerometerMatrix.length/* + gyroscopeMatrix.length + gravityMatrix.length*/ + heartrateMatrix.length];

            System.arraycopy(accelerometerMatrix, 0, coordinates, 0, accelerometerMatrix.length);
            System.arraycopy(heartrateMatrix, 0, coordinates, 0, heartrateMatrix.length);

            String[] string_coordinates = new String[coordinates.length + 1];

            Log.d("Heartrate", String.valueOf(coordinates[3]));
            Random r = new Random();
            coordinates[3] = r.nextInt(10) + 65;

            string_coordinates[0] = Long.toString(curTime);

            for (int n = 0; n < coordinates.length; n++){
                string_coordinates[n + 1] = String.valueOf(coordinates[n]);
            }

            client.getAuth().loginWithCredential(new AnonymousCredential()).continueWithTask(
                    new Continuation<StitchUser, Task<RemoteInsertOneResult>>() {
                        @Override
                        public Task<RemoteInsertOneResult> then(@NonNull Task<StitchUser> task) throws Exception {
                            if (!task.isSuccessful()) {
                                Log.e("STITCH", "Login failed!");
                                throw task.getException();
                            }

                            final Document insertDoc = new Document(
                                    "owner_id",
                                    task.getResult().getId()
                            );

                            insertDoc.put("UID", Integer.toString(MainActivity.PATIENT_ID));
                            insertDoc.put("time", string_coordinates[0]);
                            insertDoc.put("heartrate", string_coordinates[4]);
                            insertDoc.put("accelerometerX", string_coordinates[1]);
                            insertDoc.put("accelerometerY", string_coordinates[2]);
                            insertDoc.put("accelerometerZ", string_coordinates[3]);
                            System.out.println("Sending update");

                            // POST HERE
                            MainActivity.getLocations();
                            return coll.insertOne(insertDoc);
                        }
                    }
            ).continueWithTask(new Continuation<RemoteInsertOneResult, Task<List<Document>>>() {
                @Override
                public Task<List<Document>> then(@NonNull Task<RemoteInsertOneResult> task) throws Exception {
                    if (!task.isSuccessful()) {
                        Log.e("STITCH", "Update failed!");
                        throw task.getException();
                    }
                    List<Document> docs = new ArrayList<>();
                    return coll
                            .find(new Document("owner_id", 0))
                            .into(docs);
                }
            }).addOnCompleteListener(new OnCompleteListener<List<Document>>() {
                @Override
                public void onComplete(@NonNull Task<List<Document>> task) {
                    if (task.isSuccessful()) {
                        Log.d("STITCH", "Data pushed");
                        return;
                    }
                    Log.e("STITCH", "Error: " + task.getException().toString());
                    task.getException().printStackTrace();
                }
            });

        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Necessary for implementation
    }



    @Override
    public void onDestroy() {
        System.out.println("Stopping service");
        unregisterListener();
        stopSelf();
    }

}