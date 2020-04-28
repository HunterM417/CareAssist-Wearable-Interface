package com.srl.polardatacollection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    public static final String TAG  = "MainActivity";

    public static int PATIENT_ID = -1;
    public static final String START_ACTIVITY_PATH = "/start/MainActivity";
    public static final String STOP_ACTIVITY_PATH = "/stop/MainActivity";

    private static boolean POLAR = false;

    private GoogleApiClient mGoogleApiClient;
    private FusedLocationProviderClient fusedLocationClient;
    public static TextView lonTextView;
    public static TextView latTextView;
    private boolean locRunning = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        latTextView = findViewById(R.id.LatLocation);
        lonTextView= findViewById(R.id.LongLocation);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getLastLocation();
        init();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    public void init() {


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

                    if (clickedCheckbox.getId() == R.id.polarCheckbox) {
                        stopPolar();
                    }
                } else {
                    clickedCheckbox.setChecked(true);
                    clickedLayout.setBackgroundColor(Color.parseColor("#00e676"));

                    AlertDialog.Builder filenameBuilder = new AlertDialog.Builder(MainActivity.this);

                    @SuppressLint("Inflateparams")
                    final View dialogView = MainActivity.this.getLayoutInflater().inflate(R.layout.filename_prompt, null);

                    final EditText patient_id_text = dialogView.findViewById(R.id.activityPerformed);

                    filenameBuilder.setView(dialogView)
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    TextView otherActivity = findViewById(R.id.otherActivityLabel);
                                    otherActivity.setVisibility(View.INVISIBLE);
                                    if (patient_id_text.getText().toString().equals("")) {
                                        TextView error = findViewById(R.id.error);
                                        error.setText(R.string.app_id_prompt);
                                        error.setVisibility(View.VISIBLE);
                                        clickedCheckbox.toggle();
                                        clickedLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
                                    } else if (!isInteger(patient_id_text.getText().toString())) {
                                        TextView error = findViewById(R.id.error);
                                        error.setText(R.string.not_integer_prompt);
                                        error.setVisibility(View.VISIBLE);
                                        clickedCheckbox.toggle();
                                        clickedLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
                                    } else {
                                        TextView error = findViewById(R.id.error);
                                        error.setVisibility(View.INVISIBLE);
                                        String dataFile = patient_id_text.getText().toString();
                                        PATIENT_ID = Integer.parseInt(patient_id_text.getText().toString());

                                        if (clickedCheckbox.getId() == R.id.polarCheckbox) {
                                            startPolar(dataFile); //(activitySelected.getText().toString(), dataFile);
                                        }
                                    }

                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    TextView error = findViewById(R.id.error);
                                    error.setText(R.string.app_id_prompt);
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
    public void getLastLocation() {
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
                                    Log.d("LOCATION", ""+location.getLatitude()+", "+location.getLongitude());

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

    private void startPolar(String curFilename) {
        Log.d(TAG, "Starting Polar...");
        POLAR = true;
        locRunning = true;
        collectLocationData();

        final String patient_id = curFilename;

        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                for (Node node : getConnectedNodesResult.getNodes()) {
                    sendMessage(node.getId(), START_ACTIVITY_PATH + "/" + patient_id);
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
        locRunning = false;

        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                for (Node node : getConnectedNodesResult.getNodes()) {
                    sendMessage(node.getId(), STOP_ACTIVITY_PATH);
                }
            }
        });
    }

    private void collectLocationData() {
        new Thread(new Runnable() {
            public void run() {
                // a potentially time consuming task
                while(locRunning) {
                    try {
                        getLastLocation();
                        Thread.sleep(3000);
                        Log.d("Sleep", "Success!!!");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private boolean isInteger( String input ) {
        try {
            Integer.parseInt( input );
            return true;
        }
        catch( NumberFormatException e ) {
            return false;
        }
    }
}


