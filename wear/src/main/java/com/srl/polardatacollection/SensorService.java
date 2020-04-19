package com.srl.polardatacollection;

import android.content.Context;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import androidx.annotation.NonNull;

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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class SensorService extends Service implements SensorEventListener {

    //This can't be set below 10ms due to Android/hardware limitations. Use 9 to get more accurate 10ms intervals
    final short POLL_FREQUENCY = 3000; //in milliseconds
    final short SAVE_FREQUENCY = 3000;

    public static String ACTIVITY = "com.srl.polardatacollection.ACTIVITY_WEAR";
    public static String FILENAME = "com.srl.polardatacollection.FILENAME_WEAR";

    public static StitchAppClient client =
            Stitch.initializeDefaultAppClient("careassiststitchapp-owlqs");

    public static RemoteMongoClient mongoClient =
            client.getServiceClient(RemoteMongoClient.factory, "mongodb-atlas");

    public static RemoteMongoCollection<Document> coll =
            mongoClient.getDatabase("patients").getCollection("data");

    private long lastUpdate = -1;
    long curTime;

    private SensorManager sensorManager = null;

    Sensor sensor;
    Sensor accelerometer;
    //Sensor gyroscope;
    //Sensor gravity;
    Sensor heartRate;

    float[] accelerometerMatrix = new float[3];
    //float[] gyroscopeMatrix = new float[3];
   // float[] gravityMatrix = new float[3];
    float[] heartrateMatrix = new float[1];
    ArrayList<String[]> saveData = new ArrayList<>();
//    float[] rotationMatrix = new float[9];
    String filename = "raw_data.csv";
    String activity = "Nothing";

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(MainActivity.TYPE_ACCELEROMETER);
        heartRate = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (!intent.getStringExtra(FILENAME).isEmpty()) {
            filename = intent.getStringExtra(FILENAME);
        }

        activity = intent.getStringExtra(ACTIVITY);

        System.out.println("Starting service");
        registerListener();
        return START_STICKY;
    }

    private void registerListener() {
        sensorManager.registerListener(this, accelerometer, 50000);
        sensorManager.registerListener(this, heartRate, SensorManager.SENSOR_DELAY_NORMAL);
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

        //SensorManager.getRotationMatrix(rotationMatrix, null, gravityMatrix, magneticMatrix);

        //This if statement is because whether to use elapsedRealtimeNanos() or nanoTime() to convert the sensor event time to epoch time depends on the specific device
        if (Math.abs((event.timestamp - System.nanoTime())/1000000L) < Math.abs((event.timestamp - SystemClock.elapsedRealtimeNanos())/1000000L)) {
            curTime = System.currentTimeMillis() + (event.timestamp - System.nanoTime())/1000000L;
        } else {
            curTime = System.currentTimeMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos())/1000000L;
        }

        // only allow one update every POLL_FREQUENCY (convert from ms to nano for comparison).
        if((curTime - lastUpdate) > POLL_FREQUENCY) {

            lastUpdate = curTime;
//            System.out.println("Accelerometer:");
//            System.out.println(Float.toString(accelerometerMatrix[0]) + "," + Float.toString(accelerometerMatrix[1]) + "," + Float.toString(accelerometerMatrix[2])); // x, y, z
//            System.out.println("Gyroscope:");
//            System.out.println(Float.toString(gyroscopeMatrix[0]) + "," + Float.toString(gyroscopeMatrix[1]) + "," + Float.toString(gyroscopeMatrix[2])); // x, y, z
//            System.out.println("Gravity");
//            System.out.println(Float.toString(gravityMatrix[0]) + "," + Float.toString(gravityMatrix[1]) + "," + Float.toString(gravityMatrix[2])); // x, y, z

            float[] coordinates = new float[accelerometerMatrix.length/* + gyroscopeMatrix.length + gravityMatrix.length*/ + heartrateMatrix.length];

            System.arraycopy(accelerometerMatrix, 0, coordinates, 0, accelerometerMatrix.length);
            //System.arraycopy(gyroscopeMatrix, 0, coordinates, accelerometerMatrix.length, gyroscopeMatrix.length);
            //System.arraycopy(gravityMatrix, 0, coordinates, accelerometerMatrix.length + gyroscopeMatrix.length, gravityMatrix.length);


            String[] string_coordinates = new String[coordinates.length + 2];

            string_coordinates[0] = Long.toString(curTime);

            for (int n = 0; n < coordinates.length; n++){
                string_coordinates[n + 1] = String.valueOf(coordinates[n]);
            }

            string_coordinates[string_coordinates.length - 1] = activity;
            saveData.add(string_coordinates);
            if (saveData.size() > SAVE_FREQUENCY) {
                save_file(saveData, filename);
                saveData.clear();
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

                            insertDoc.put("patient_id", 1);
                            insertDoc.put("time", string_coordinates[0]);
                            insertDoc.put("heartrate", string_coordinates[4]);
                            insertDoc.put("accelerometerX", string_coordinates[1]);
                            insertDoc.put("accelerometerY", string_coordinates[2]);
                            insertDoc.put("accelerometerZ", string_coordinates[3]);
                            insertDoc.put("longitude", 0);
                            insertDoc.put("latitude", 0);
                            insertDoc.put("activity_type", string_coordinates[5]);
                            System.out.println("Sending update");
                            return coll.insertOne(insertDoc);

                            // SEND MESSAGE HERE
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
                        Log.d("STITCH", "Found docs: " + task.getResult().toString());
                        return;
                    }
                    Log.e("STITCH", "Error: " + task.getException().toString());
                    task.getException().printStackTrace();
                }
            });

        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void save_file(ArrayList<String[]> saveData, String filename) {

        try {
            File traceFile = new File(this.getExternalFilesDir(null), filename + ".csv");
            boolean fileExists = traceFile.exists();

            // Adds a line to the trace file
            BufferedWriter writer = new BufferedWriter(new FileWriter(traceFile, true));
            if (!fileExists){
                writer.write("Time,AccelerometerX,AccelerometerY,AccelerometerZ,Heartrate,Activity");
                writer.newLine();
            }
//            String linetowrite[] = new String[coordinates.length];
//            System.arraycopy(coordinates,0,linetowrite,0,coordinates.length);

            for (String[] sensorData : saveData) {
                for (int i = 0; i < sensorData.length; i++) {
                    writer.write(sensorData[i]);
                    if (i != sensorData.length - 1) {
                        writer.write(",");
                    }
                }
                writer.newLine();
            }

            writer.close();

            Intent mediaScannerIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri fileContentUri = Uri.fromFile(traceFile); // With 'permFile' being the File object
            mediaScannerIntent.setData(fileContentUri);
            this.sendBroadcast(mediaScannerIntent); // With 'this' being the context, e.g. the activity

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        System.out.println("Stopping service");
        save_file(saveData, filename);
        unregisterListener();
        stopSelf();
    }

}