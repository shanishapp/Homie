package com.example.homie;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Debug;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1546 ;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location lastLocation;
    private Location homeLocation;
    private Boolean requestingLocationUpdates = false;
    private SharedPreferences sp = null;
    TextView currentLongitude;
    TextView currentLatitude;
    TextView currentAccuracy;
    String homeLongitude;
    String homeLatitude;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentLongitude = findViewById(R.id.longtitude_content);
        currentLatitude = findViewById(R.id.latitude_content);
        currentAccuracy = findViewById(R.id.accuracy_content);

        sp = this.getSharedPreferences("params",MODE_PRIVATE);
        homeLatitude = sp.getString("homeLatitude","");
        homeLongitude = sp.getString("homeLongitude","");
        if(! homeLatitude.equals("")) {
            findViewById(R.id.clear_home_location).setVisibility(View.VISIBLE);

            TextView homeLongtitudeTextView = findViewById(R.id.home_longtitude_content);
            homeLongtitudeTextView.setText(homeLongitude);
            TextView homeLatitudeTextView = findViewById(R.id.home_latitude_content);
            homeLatitudeTextView.setText(homeLatitude);
        } else {
            homeLocation = null;
        }
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Log.d("shani","in callback");
                    lastLocation = location;
                    currentLongitude.setText(String.valueOf(lastLocation.getLongitude()));
                    currentLatitude.setText(String.valueOf(lastLocation.getLatitude()));
                    currentAccuracy.setText(String.valueOf(lastLocation.getAccuracy()));
                    if ( location.getAccuracy()<=50.0) {
                        findViewById(R.id.set_home_location).setVisibility(View.VISIBLE);
                    }
                    else {
                        findViewById(R.id.set_home_location).setVisibility(View.INVISIBLE);
                    }
                }
            }
        };
        requestingLocationUpdates = sp.getBoolean("requestingUpdates",false);
        if (requestingLocationUpdates) {
            startLocate(null);
        }
        else {
            clearLocation();
            clearHomeLocation(null);
        }


    }

    public void startLocate(View view) {
        boolean hasPermission =
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)==
                        PackageManager.PERMISSION_GRANTED;
        if(hasPermission) {
            startTrackLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                                REQUEST_CODE);
        }

        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        //check if gps enabled
        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
        }

        //continue only if the user enabled gps
        if ( manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) )
        {
            startTrackLocation();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            startTrackLocation();
        } else {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                                        Manifest.permission.ACCESS_FINE_LOCATION)){
                //show ui explain why that location needed
                // maybe show before ask for permission
                showExplanation();
            }

        }
    }

    private void showExplanation() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("We need to have Location permission to startTrackLocation you in real time");
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void startTrackLocation()
    {
        requestingLocationUpdates = true;
        change_button();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            TextView latitute = findViewById(R.id.latitude_content);
                            latitute.setText(String.valueOf(location.getLatitude()));
                            TextView longtitute = findViewById(R.id.longtitude_content);
                            longtitute.setText(String.valueOf(location.getLongitude()));
                            TextView accuracy = findViewById(R.id.accuracy_content);
                            accuracy.setText(String.valueOf(location.getAccuracy()));
                        }
                    }
                });
        startLocationUpdates();//TODO needed ??
    }

    private void stopTrackingLocation(){
        requestingLocationUpdates = false;
        fusedLocationClient.removeLocationUpdates(locationCallback);
        change_button();
    }

    private void change_button()
    {
        if (!requestingLocationUpdates) {
            Button button = findViewById(R.id.locate_button);
            button.setText("Start tracking location");
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startLocate(v);
                }
            });
        }
        else {
            Button button = findViewById(R.id.locate_button);
            button.setText("Stop tracking");
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopTrackingLocation();
                }
            });
        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setMessage("We need to have Location enabled to startTrackLocation you in real time");
                        final AlertDialog alert = builder.create();
                        alert.show();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (requestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(5);
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        sp.edit().putBoolean("requestingUpdates",requestingLocationUpdates)
                .putString("homeLongitude",homeLongitude)
                .putString("homeLatitude",homeLatitude).apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        sp.edit().putBoolean("requestingUpdates",requestingLocationUpdates)
                .putString("homeLongitude",homeLongitude)
                .putString("homeLatitude",homeLatitude).apply();
    }

    public void setHomeLocation(View view) {
        homeLocation = lastLocation;
        homeLongitude = String.valueOf(homeLocation.getLongitude());
        homeLatitude = String.valueOf(homeLocation.getLatitude());
        TextView homeLongtitudeTextView = findViewById(R.id.home_longtitude_content);
        homeLongtitudeTextView.setText(homeLongitude);
        TextView homeLatitudeTextView = findViewById(R.id.home_latitude_content);
        homeLatitudeTextView.setText(homeLatitude);
        findViewById(R.id.clear_home_location).setVisibility(View.VISIBLE);
    }

    public void clearHomeLocation(View view) {
        homeLocation = null;
        TextView homeLongtitudeTextView = findViewById(R.id.home_longtitude_content);
        homeLongtitudeTextView.setText("no location");
        homeLongitude = "";
        TextView homeLatitudeTextView = findViewById(R.id.home_latitude_content);
        homeLatitudeTextView.setText("no location");
        homeLatitude = "";
        findViewById(R.id.clear_home_location).setVisibility(View.INVISIBLE);
    }

    private void clearLocation() {
        currentLongitude.setText("no location");
        currentLatitude.setText("no location");
        currentAccuracy.setText("no location");
    }
}
