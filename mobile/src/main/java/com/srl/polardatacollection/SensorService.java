package com.srl.polardatacollection;


import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.SystemClock;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class SensorService extends Service implements SensorEventListener {

    //This can't be set below 10ms due to Android/hardware limitations. Use 9 to get more accurate 10ms intervals
    final short POLL_FREQUENCY = 3000; //in milliseconds

    public static String ACTIVITY = "com.srl.polardatacollection.ACTIVITY_PHONE";
    public static String FILENAME = "com.srl.polardatacollection.FILENAME_PHONE";

    private long lastUpdate = -1;
    long curTime;

    private SensorManager sensorManager = null;

    Sensor sensor;
    Sensor accelerometer;
    //Sensor gyroscope;
    //Sensor gravity;
    //Sensor magnetic;

    float[] accelerometerMatrix = new float[3];
    //float[] gyroscopeMatrix = new float[3];
    //float[] gravityMatrix = new float[3];
    //float[] magneticMatrix = new float[3];
    //float[] rotationMatrix = new float[9];
    String filename = "raw_data.csv";
    String activity = "Nothing";

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(MainActivity.TYPE_ACCELEROMETER);
        //gyroscope = sensorManager.getDefaultSensor(MainActivity.TYPE_GYROSCOPE);
        //gravity = sensorManager.getDefaultSensor(MainActivity.TYPE_GRAVITY);
        //magnetic = sensorManager.getDefaultSensor(MainActivity.TYPE_MAGNETIC);
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
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        //sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        //sensorManager.registerListener(this, gravity, SensorManager.SENSOR_DELAY_FASTEST);
        //sensorManager.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void unregisterListener() {
        sensorManager.unregisterListener(this);
    }

    public void onSensorChanged(SensorEvent event) {
        sensor = event.sensor;

        int i = sensor.getType();
        if (i == MainActivity.TYPE_ACCELEROMETER) {
            accelerometerMatrix = event.values;
        } /*else if (i == MainActivity.TYPE_GYROSCOPE) {
            gyroscopeMatrix = event.values;
        } else if (i == MainActivity.TYPE_GRAVITY) {
            gravityMatrix = event.values;
        } else if (i == MainActivity.TYPE_MAGNETIC) {
            magneticMatrix = event.values;
        }*/

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
//            System.out.println("Magnetic");
//            System.out.println(Float.toString(magneticMatrix[0]) + "," + Float.toString(magneticMatrix[1]) + "," + Float.toString(magneticMatrix[2])); // x, y, z

            float[] coordinates = new float[accelerometerMatrix.length /*+ gyroscopeMatrix.length + gravityMatrix.length + magneticMatrix.length*/];

            System.arraycopy(accelerometerMatrix, 0, coordinates, 0, accelerometerMatrix.length);
            //System.arraycopy(gyroscopeMatrix, 0, coordinates, accelerometerMatrix.length, gyroscopeMatrix.length);
            //System.arraycopy(gravityMatrix, 0, coordinates, accelerometerMatrix.length + gyroscopeMatrix.length, gravityMatrix.length);
            //System.arraycopy(magneticMatrix, 0, coordinates, accelerometerMatrix.length + gyroscopeMatrix.length + gravityMatrix.length, magneticMatrix.length);


            String[] string_coordinates = new String[coordinates.length + 2];

            string_coordinates[0] = Long.toString(curTime);

            for (int n = 0; n < coordinates.length; n++){
                string_coordinates[n + 1] = String.valueOf(coordinates[n]);
            }

            string_coordinates[string_coordinates.length - 1] = activity;
            save_file(string_coordinates, filename);
            post_to_db(string_coordinates);
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void save_file(String[] coordinates, String filename) {

        try {
            File traceFile = new File(this.getExternalFilesDir(null), filename + ".csv");
            boolean fileExists = traceFile.exists();

            // Adds a line to the trace file
            BufferedWriter writer = new BufferedWriter(new FileWriter(traceFile, true));
            if (!fileExists){
                writer.write("Time,AccelerometerX,AccelerometerY,AccelerometerZ,Activity");
                writer.newLine();
            }
//            String linetowrite[] = new String[coordinates.length];
//            System.arraycopy(coordinates,0,linetowrite,0,coordinates.length);

            for (int i = 0; i < coordinates.length; i++) {
                writer.write(coordinates[i]);
                if (i != coordinates.length-1) {
                    writer.write(",");
                }
            }

            writer.newLine();
            writer.close();

            Intent mediaScannerIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri fileContentUri = Uri.fromFile(traceFile); // With 'permFile' being the File object
            mediaScannerIntent.setData(fileContentUri);
            this.sendBroadcast(mediaScannerIntent); // With 'this' being the context, e.g. the activity

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void post_to_db(String[] coordinates) {
        //CODE TO POST DATA TO DB
    }

    @Override
    public void onDestroy() {
        System.out.println("Stopping service");
        unregisterListener();
        stopSelf();
    }

}
