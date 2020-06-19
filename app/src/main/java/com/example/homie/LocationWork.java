package com.example.homie;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;

public class LocationWork extends Worker {

    public static final String PREVIOUS = "previous";

    private Context context;
    private Location lastLocation;
    private LocationCallback locationCallback = null;
    private Location homeLocation = null;

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
        String json = sp.getString(PREVIOUS, null);
        Gson gson = new Gson();
        lastLocation = gson.fromJson(json, Location.class);
        //set last known home location, if exists
        String latitude = sp.getString(MainActivity.HOME_LATITUDE, "");
        String longtitude = sp.getString(MainActivity.HOME_LONGITUDE, "");
        if (!latitude.equals("") && !longtitude.equals("")) {
            homeLocation = new Location(LocationManager.GPS_PROVIDER);
            homeLocation.setLatitude(Double.parseDouble(latitude));
            homeLocation.setLongitude(Double.parseDouble(longtitude));
        }


        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    Location location = locationResult.getLastLocation();
                    Log.d("shani", "location not null");
                    if (location.getAccuracy() <= 50.0) {
                        Log.d("shani", "adccuracy good");
                        if (lastLocation == null || location.distanceTo(lastLocation) >= 50) {
                            if (homeLocation != null) {
                                if (location.distanceTo(homeLocation) < 50) {
                                    Log.d("shani", "dammm youre home");
                                    Intent intent = new Intent();
                                    intent.setAction(MainActivity.BROADCAST_SMS);
                                    intent.putExtra(MainActivity.PHONE,sp.getString(MainActivity.PHONE,null));
                                    intent.putExtra(MainActivity.CONTENT, "Honey I'm home!");
                                    context.sendBroadcast(intent);
                                }
                            }
                        }
                        fusedLocationClient.removeLocationUpdates(locationCallback);
                    }
                }

            }
        };

    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("debugshani","creating listanable future");
        Context context = LocationWork.this.context;
        if(hasPerms(context)) {
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(10000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setFastestInterval(5000);
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());//TODO check what the format
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
//                        String content = sp.getString(MainActivity.CONTENT,null);
        return hasLocationPermission && hasSmsPermission && phoneNumber != null;
    }


}