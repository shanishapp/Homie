package com.example.homie;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;

public class LocationWork extends Worker {

    private static final String LAST_LOCATION = "previous";
    private Context context;
    private Location homeLocation = null;
    private boolean gotAccuracy;
    private FusedLocationProviderClient fusedLocationClient;
    private SharedPreferences sp;

    /**
     * @param appContext   The application {@link Context}
     * @param workerParams Parameters to setup the internal state of this worker
     */
    public LocationWork(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
        this.context = appContext;
        sp = context.getSharedPreferences(MainActivity.PARAMS, Context.MODE_PRIVATE);
        getHomeLocation();
    }

    private void getHomeLocation() {
        //set last known home location, if exists
        String latitude = sp.getString(MainActivity.HOME_LATITUDE, "");
        String longtitude = sp.getString(MainActivity.HOME_LONGITUDE, "");
        if (!latitude.equals("") && !longtitude.equals("")) {
            homeLocation = new Location(LocationManager.GPS_PROVIDER);
            homeLocation.setLatitude(Double.parseDouble(latitude));
            homeLocation.setLongitude(Double.parseDouble(longtitude));
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("debugshani","creating listanable future");
        //Context context = LocationWork.this.context;
        if(hasPerms(context)) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
            gotAccuracy = false;

            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(10000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            LocationCallback locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult != null) {
                        for (Location location: locationResult.getLocations()) {
                            updateLocation(location);
                        }

                    }
                }

                private void updateLocation(Location location) {
                    String json = sp.getString(LAST_LOCATION, "");
                    Gson gson = new Gson();
                    Location lastLocation = gson.fromJson(json, Location.class);
                    if (location.getAccuracy() < 50.0) {
                        if (gotAccuracy) {
                            fusedLocationClient.removeLocationUpdates(this);
                            gotAccuracy = false;
                        }
                        if (!json.equals("") && location.distanceTo(lastLocation) >= 50) {
                            if (homeLocation != null) {
                                if (location.distanceTo(homeLocation) < 50) {
                                    Intent intent = new Intent();
                                    intent.setAction(MainActivity.BROADCAST_SMS);
                                    intent.putExtra(MainActivity.PHONE, sp.getString(MainActivity.PHONE, null));
                                    intent.putExtra(MainActivity.CONTENT, "Honey I'm home!");
                                    LocationWork.this.context.sendBroadcast(intent);
                                }
                            }
                        }
                    }
                    String newLast = gson.toJson(location, Location.class);
                    sp.edit().putString(LAST_LOCATION, newLast).apply();
                }
            };
            Log.d("shani","set fusedLocationClient");
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
            gotAccuracy = true;
        }
        return Result.success();
    }

    private boolean hasPerms(Context context) {
        boolean hasLocationPermission = context.checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasSmsPermission = context.checkSelfPermission(
                Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
        String phoneNumber = sp.getString(MainActivity.PHONE,null);
        return hasLocationPermission && hasSmsPermission && phoneNumber != null;
    }
}