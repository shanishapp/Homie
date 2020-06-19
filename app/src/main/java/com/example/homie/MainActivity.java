package com.example.homie;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Debug;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1546;
    private static final String REQUESTING_UPDATES = "requestingUpdates";
    public static final String HOME_LONGITUDE = "homeLongitude";
    public static final String HOME_LATITUDE = "homeLatitude";
    private static final String LONGTITUDE = "longtitude";
    private static final String LATITUDE = "latitude";
    private static final String ACCURACY = "accuracy";
    public static final String PARAMS = "params";
    public static final String PHONE = "phoneNumber";
    public static final String CONTENT = "msgContent";
    public static final String BROADCAST_SMS = "brSms";
    public static final String CHANNEL_ID = "channelIdForNonBroadcastReceiver" ;
    public static final int NOTIFICATION_ID = 123;

    private Location lastLocation;
    private Location homeLocation;
    private Boolean requestingLocationUpdates = false;
    private SharedPreferences sp = null;
    private TextView currentLongitude;
    private TextView currentLatitude;
    private TextView currentAccuracy;
    private String homeLongitude;
    private String homeLatitude;
    private LocationManager locationManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        initFromSp();
        validateHomeLocation();
    }

    private void validateHomeLocation() {
        if (!homeLatitude.equals("")) {
            findViewById(R.id.clear_home_location).setVisibility(View.VISIBLE);
            TextView homeLongtitudeTextView = findViewById(R.id.home_longtitude_content);
            homeLongtitudeTextView.setText(homeLongitude);
            TextView homeLatitudeTextView = findViewById(R.id.home_latitude_content);
            homeLatitudeTextView.setText(homeLatitude);
        } else {
            homeLocation = null;
        }
    }

    private void initFromSp() {
        currentLongitude = findViewById(R.id.longtitude_content);
        currentLatitude = findViewById(R.id.latitude_content);
        currentAccuracy = findViewById(R.id.accuracy_content);

        sp = this.getSharedPreferences(PARAMS, MODE_PRIVATE);

        currentLongitude.setText(sp.getString(LONGTITUDE,"no location"));
        currentLatitude.setText(sp.getString(LATITUDE,"no location"));
        currentAccuracy.setText(sp.getString(ACCURACY,"no location"));
        homeLatitude = sp.getString(HOME_LATITUDE, "");
        homeLongitude = sp.getString(HOME_LONGITUDE, "");
    }


    LocationListener locationListenerGPS=new LocationListener() {
        @Override
        public void onLocationChanged(android.location.Location location) {
            if (location != null) {
                lastLocation = location;
                currentLongitude.setText(new DecimalFormat("##.#######").format(lastLocation.getLongitude()));
                currentLatitude.setText(new DecimalFormat("##.#######").format(lastLocation.getLatitude()));
                currentAccuracy.setText(new DecimalFormat("##.##").format(lastLocation.getAccuracy()));
                if ( location.getAccuracy()<=50.0) {
                    findViewById(R.id.set_home_location).setVisibility(View.VISIBLE);
                }
                else {
                    findViewById(R.id.set_home_location).setVisibility(View.INVISIBLE);
                }
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {
            buildAlertMessageNoGps();
        }
    };


    public void startLocate(View view) {

        boolean hasPermission =
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)==
                        PackageManager.PERMISSION_GRANTED;
        if(hasPermission) {
            //check if gps enabled
            if ( !locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
                buildAlertMessageNoGps();
            }
            requestingLocationUpdates = true;
            change_button();
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    2000,
                    1, locationListenerGPS);
        } else {
            ActivityCompat.requestPermissions(this,
                                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                                REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for(int i = 0; i < grantResults.length; i++) {
            int res = grantResults[i];
            String perm = permissions[i];
            if(res == PackageManager.PERMISSION_GRANTED){

                if (perm == Manifest.permission.ACCESS_FINE_LOCATION) {
                    startLocate(null);
                } else { // perm == Manifest.permission.SEMD_SMS
                    showSetNumberDialog(null);
                }

            } else {
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, perm)){
                    //show ui explain why that location needed
                    // maybe show before ask for permission
                    showExplanation(perm); // TODO make general !!
                }
            }
        }
    }

    private void showExplanation(String perm) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (perm.equals(Manifest.permission.ACCESS_FINE_LOCATION))
            builder.setMessage("We need to have Location permission to startTrackLocation you in real time");
        else
            builder.setMessage("We need to have SMS permissions to send sms");
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void stopTrackingLocation(){
        requestingLocationUpdates = false;
        //locationManager.removeUpdates(locationListenerGPS);
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
        requestingLocationUpdates = sp.getBoolean(REQUESTING_UPDATES, false);
        if (requestingLocationUpdates) {
            startLocate(null);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        sp.edit().putBoolean(REQUESTING_UPDATES,requestingLocationUpdates)
                .putString(HOME_LONGITUDE,homeLongitude)
                .putString(HOME_LATITUDE,homeLatitude)
                .putString(LONGTITUDE, currentLongitude.getText().toString())
                .putString(LATITUDE,currentLatitude.getText().toString())
                .putString(ACCURACY,currentAccuracy.getText().toString()).apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sp.edit().putBoolean(REQUESTING_UPDATES,requestingLocationUpdates)
                .putString(HOME_LONGITUDE,homeLongitude)
                .putString(HOME_LATITUDE,homeLatitude)
                .putString(LONGTITUDE, currentLongitude.getText().toString())
                .putString(LATITUDE,currentLatitude.getText().toString())
                .putString(ACCURACY,currentAccuracy.getText().toString()).apply();
//        stopTrackingLocation();
    }

    public void setHomeLocation(View view) {
        homeLocation = lastLocation;
        homeLongitude = new DecimalFormat("##.#######").format(homeLocation.getLongitude());
        homeLatitude =new DecimalFormat("##.#######").format(homeLocation.getLatitude());
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

    public void showSetNumberDialog(View view) {
        boolean hasPermission = true;
        if(view != null) {
            hasPermission =
                    ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.SEND_SMS)==
                            PackageManager.PERMISSION_GRANTED;

        }
        if(hasPermission) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            LayoutInflater inflater = this.getLayoutInflater();
            final View dialogView = inflater.inflate(R.layout.dialog_set_sms_phone_number,null);
            Button set_button = dialogView.findViewById(R.id.sendPhoneNumber);
            Button cancel_button = dialogView.findViewById(R.id.cancelSetNumber);
            Button delete_button = dialogView.findViewById(R.id.deletePhoneNumber);
            final EditText phoneNumber = dialogView.findViewById(R.id.phoneTextView);
            dialogBuilder.setView(dialogView);
            final AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            set_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sp.edit().putString(PHONE,phoneNumber.getText().toString()).apply();
                    alertDialog.cancel();
                }
            });

            cancel_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alertDialog.cancel();
                }
            });

            delete_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sp.edit().remove(PHONE).apply();
                    alertDialog.cancel();
                }
            });
            alertDialog.show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    REQUEST_CODE);
        }
    }

    public void testSms(View view) {
        boolean hasSmsPermission = this.checkSelfPermission(
                Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
        if(hasSmsPermission) {
            Intent intent = new Intent();
            intent.setAction(BROADCAST_SMS);
            intent.putExtra(PHONE,sp.getString(PHONE,null));
            intent.putExtra(CONTENT,"Honey I'm Sending a Test Message!");
            sendBroadcast(intent);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    REQUEST_CODE);
        }
    }


}
