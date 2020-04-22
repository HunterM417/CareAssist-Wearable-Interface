package com.srl.polardatacollection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.core.auth.StitchUser;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoCollection;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoDatabase;
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteInsertOneResult;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteUpdateOptions;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteUpdateResult;

import org.bson.Document;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Thread.sleep;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    public static final String TAG  = "MainActivity";

    //Set the specific sensors to be used throughout the app
    public final static short TYPE_ACCELEROMETER = Sensor.TYPE_ACCELEROMETER;
    //public final static short TYPE_GYROSCOPE = Sensor.TYPE_GYROSCOPE;
    //public final static short TYPE_GRAVITY = Sensor.TYPE_GRAVITY;
    //public final static short TYPE_MAGNETIC = Sensor.TYPE_MAGNETIC_FIELD;

    public static String ACTIVITY = "com.srl.polardatacollection.ACTIVITY_PHONE";
    public static String FILENAME = "com.srl.polardatacollection.FILENAME_PHONE";
    public static final String START_ACTIVITY_PATH = "/start/MainActivity";
    public static final String STOP_ACTIVITY_PATH = "/stop/MainActivity";

    private static boolean POLAR = false;
    private Intent intentSensing;

    private TextView activitySelected;// = "Nothing";
    private GoogleApiClient mGoogleApiClient;
    private FusedLocationProviderClient fusedLocationClient;
    private BroadcastReceiver locationReceiver;
    private TextView lonTextView;
    private TextView latTextView;



    public static StitchAppClient client =
            Stitch.initializeDefaultAppClient("careassiststitchapp-owlqs");

    public static RemoteMongoClient mongoClient =
            client.getServiceClient(RemoteMongoClient.factory, "mongodb-atlas");

    public static RemoteMongoCollection<Document> coll =
            mongoClient.getDatabase("patients").getCollection("data");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        latTextView = findViewById(R.id.LatLocation);
        lonTextView= findViewById(R.id.LongLocation);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        init();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    public void init() {

        intentSensing = new Intent(this, SensorService.class);

        //*************
        //***Sensors***
        //*************
        final CardView polarSelect = findViewById(R.id.polarSelect);

        View.OnClickListener sensorClick = new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                CheckBox tempClickedCheckbox = findViewById(R.id.polarCheckbox);
                CardView tempClickedLayout = findViewById(R.id.polarSelect);

                switch(v.getId()){
                    case R.id.polarSelect:
                        tempClickedCheckbox = findViewById(R.id.polarCheckbox);
                        tempClickedLayout = findViewById(R.id.polarSelect);
                        break;
                }

                final CheckBox clickedCheckbox = tempClickedCheckbox;
                final CardView clickedLayout = tempClickedLayout;

                if (clickedCheckbox.isChecked()) {

                    clickedCheckbox.setChecked(false);
                    clickedLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));

                    if (activitySelected != null) {

                        CardView activityCard = (CardView) activitySelected.getParent().getParent();

                        activityCard.setBackgroundColor(Color.parseColor("#FFFFFF"));
                        activitySelected = null;
                    }

                    if (clickedCheckbox.getId() == R.id.polarCheckbox) {
                        stopPolar();
                    }
                } else {
                    clickedCheckbox.setChecked(true);
                    clickedLayout.setBackgroundColor(Color.parseColor("#00e676"));

                    AlertDialog.Builder filenameBuilder = new AlertDialog.Builder(MainActivity.this);

                    @SuppressLint("Inflateparams")
                    final View dialogView = MainActivity.this.getLayoutInflater().inflate(R.layout.filename_prompt, null);

                    final EditText filename = dialogView.findViewById(R.id.activityPerformed);
                    final ToggleButton phoneLocBtn = dialogView.findViewById(R.id.phoneLocationBtn);
                    if (clickedCheckbox.getId() == R.id.polarCheckbox) {
                        phoneLocBtn.setTextOn("Right");
                        phoneLocBtn.setTextOff("Left");
                    } else {
                        phoneLocBtn.setTextOn("Label");
                        phoneLocBtn.setTextOff("Pocket");
                    }
                    filenameBuilder.setView(dialogView)
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    TextView otherActivity = findViewById(R.id.otherActivityLabel);
                                    otherActivity.setVisibility(View.INVISIBLE);
                                    if (filename.getText().toString().equals("")) {
                                        TextView error = findViewById(R.id.error);
                                        error.setText(R.string.app_name_prompt);
                                        error.setVisibility(View.VISIBLE);
                                        clickedCheckbox.toggle();
                                        clickedLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
                                    } else {
                                        TextView error = findViewById(R.id.error);
                                        error.setVisibility(View.INVISIBLE);
                                        String dataFile = filename.getText().toString();

                                        if (clickedCheckbox.getId() == R.id.polarCheckbox) {
                                            if (phoneLocBtn.isChecked()) {
                                                dataFile += "_right_labels";
                                            } else {
                                                dataFile += "_left_pocket";
                                            }
                                        } else {
                                            if (phoneLocBtn.isChecked()) {
                                                dataFile += "_labels";
                                            } else {
                                                dataFile += "_pocket";
                                            }
                                        }

                                        if (clickedCheckbox.getId() == R.id.polarCheckbox) {
                                            startPolar(dataFile, ""); //(activitySelected.getText().toString(), dataFile);
                                        } else {
                                            startSensing(dataFile, "");
                                        }
                                    }

                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    TextView error = findViewById(R.id.error);
                                    error.setText(R.string.app_name_prompt);
                                    error.setVisibility(View.INVISIBLE);

                                    clickedCheckbox.toggle();
                                    clickedLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));

                                    dialog.dismiss();
                                }
                            });

                    filenameBuilder.setCancelable(false);
                    filenameBuilder.create().show();

                }
            }
        };

        polarSelect.setOnClickListener(sensorClick);

        // Location
        final CardView locationSelect = findViewById(R.id.locationSelect);

        View.OnClickListener locationClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getLastLocation();
            }
        };
        locationSelect.setOnClickListener(locationClick);


        //****************
        //***Activities***
        //****************
        final CardView otherSelect = findViewById(R.id.otherSelect);

        final List<CardView> activities = new ArrayList<>(Arrays.asList(otherSelect));

        View.OnClickListener activityClick = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                for (CardView card : activities) {
                    if (card.getId() == v.getId()){
                        card.setBackgroundColor(Color.parseColor("#00e676"));
                        activitySelected = ((TextView) ((LinearLayout) card.getChildAt(0)).getChildAt(1));//.getText().toString();
                        final CheckBox polarCheckbox = findViewById(R.id.polarCheckbox);
                        if (activitySelected.getText().toString().equals("Other")) {
                            startSensing("", "");

                            AlertDialog.Builder otherActivityBuilder = new AlertDialog.Builder(MainActivity.this);

                            @SuppressLint("Inflateparams")
                            final View dialogView = MainActivity.this.getLayoutInflater().inflate(R.layout.other_activity, null);

                            final EditText actualActivity = dialogView.findViewById(R.id.activityPerformed);

                            otherActivityBuilder.setView(dialogView)
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int id) {
                                            TextView otherActivity = findViewById(R.id.otherActivityLabel);
                                            otherActivity.setVisibility(View.INVISIBLE);
                                            if (actualActivity.getText().toString().equals("")) {
                                                TextView error = findViewById(R.id.error);
                                                error.setText(R.string.select_activity_prompt);
                                                error.setVisibility(View.VISIBLE);
                                            } else {
                                                TextView error = findViewById(R.id.error);
                                                error.setVisibility(View.INVISIBLE);

                                                otherActivity.setText(actualActivity.getText().toString());
                                                otherActivity.setVisibility(View.VISIBLE);
                                                //activitySelected = actualActivity;
                                                if (polarCheckbox.isChecked()) {
                                                    startPolar("", actualActivity.getText().toString());
                                                }
                                            }

                                            dialog.dismiss();
                                        }
                                    })
                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int id) {
                                            TextView error = findViewById(R.id.error);
                                            error.setText(R.string.select_activity_prompt);
                                            error.setVisibility(View.INVISIBLE);

                                            CardView otherSelect = findViewById(R.id.otherSelect);

                                            otherSelect.setBackgroundColor(Color.parseColor("#FFFFFF"));
                                            dialog.dismiss();
                                        }
                                    });

                            otherActivityBuilder.setCancelable(false);
                            otherActivityBuilder.create().show();
                        } else {
                            if (polarCheckbox.isChecked()) {
                                startPolar("", activitySelected.getText().toString());
                            }
                        }
                    } else {
                        card.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    }
                }
            }
        };

        otherSelect.setOnClickListener(activityClick);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("GoogleAPi", "onConnected: " + bundle);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("GoogleApi", "onConnectionSuspended: " + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("GoogleApi", "onConnectionFailed: " + connectionResult);
    }
    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        Log.d("LOCATION", "location");
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                Log.d("LOCATION", "enable");
                fusedLocationClient.getLastLocation().addOnCompleteListener(
                        new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                Location location = task.getResult();
                                if (location == null) {
                                    latTextView.setText("0.0");
                                    lonTextView.setText("0.0");
                                    requestNewLocationData();
                                } else {
                                    latTextView.setText(location.getLatitude()+"");
                                    lonTextView.setText(location.getLongitude()+"");
                                    Log.d("LOCATION", ""+location.getLatitude());
                                    Log.d("LOCATION", ""+location.getLongitude());

                                }
                            }
                        }
                );
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            requestPermissions();
        }
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData(){
        Log.d("LOCATION", "new location");
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(0);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
        );

    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            latTextView.setText(mLastLocation.getLatitude()+"");
            lonTextView.setText(mLastLocation.getLongitude()+"");
        }
    };

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                44
        );
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 44) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            }
        }
    }

    private void startSensing(String filename, String otherActivity){
        Log.d(TAG, "Starting Sensing...");

        String activity = otherActivity;
        if (activitySelected == null){
            activity = "Nothing";
        } else if (otherActivity == ""){
            activity = activitySelected.getText().toString();
        }

        intentSensing.putExtra(ACTIVITY, activity);
        intentSensing.putExtra(FILENAME, filename);
        startService(intentSensing);

    }

    private void startPolar(String curFilename, String curActivity) {
        Log.d(TAG, "Starting Polar...");
        POLAR = true;

        if (activitySelected == null){
            curActivity = "Nothing";
        } else if (curActivity.equals("")){
            curActivity = activitySelected.getText().toString();
        }

        final String activity = curActivity;
        final String filename = curFilename;

        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                for (Node node : getConnectedNodesResult.getNodes()) {
                    sendMessage(node.getId(), START_ACTIVITY_PATH + "/" + filename + "/" + activity);
                }
            }
        });
    }

    private void sendMessage(String node, String message) {

        Wearable.MessageApi.sendMessage(mGoogleApiClient , node , message , new byte[0]).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                if (!sendMessageResult.getStatus().isSuccess()) {
                    Log.e("GoogleApi", "Failed to send message with status code: "
                            + sendMessageResult.getStatus().getStatusCode());
                }
            }
        });
    }

    private void stopPolar() {
        Log.d(TAG, "Stopping Polar...");
        POLAR = false;

        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                for (Node node : getConnectedNodesResult.getNodes()) {
                    sendMessage(node.getId(), STOP_ACTIVITY_PATH);
                }
            }
        });
    }

    private void stopSensing(){
        Log.d(TAG, "Stopping Sensing...");
        stopService(intentSensing);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        super.onNewIntent(intent);
    }
}

final class MongoLabSaveContact extends AsyncTask<Object, Void, Boolean> {
    @Override
    protected Boolean doInBackground(Object... params) {
        MyEntry entry = (MyEntry) params[0];
        Log.d("entry", ""+entry);

        try {
            SupportData sd = new SupportData();
            URL url = new URL(sd.buildEntriesSaveURL());

            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            connection.setRequestMethod("PUT");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type",
                    "application/json");
            connection.setRequestProperty("Accept", "application/json");

            OutputStreamWriter osw = new OutputStreamWriter(
                    connection.getOutputStream());

            osw.write(sd.createEntry(entry));
            osw.flush();
            osw.close();

            if(connection.getResponseCode() <205)
            {
                return true;
            }
            else
            {
                return false;
            }

        } catch (Exception e) {
            e.getMessage();
            Log.d("Got error", e.getMessage());
            return false;
        }
    }
}