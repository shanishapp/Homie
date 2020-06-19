package com.example.homie;

import android.Manifest;
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
import androidx.work.WorkerParameters;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;

public class LocationWork extends ListenableWorker {

    public static final String PREVIOUS = "previous";

    private LocationManager locationManager;
    private Context context;
    private CallbackToFutureAdapter.Completer<Result> callback;
    private Location lastLocation;
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
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        sp = context.getSharedPreferences(MainActivity.PARAMS, Context.MODE_PRIVATE);
        String json = sp.getString(PREVIOUS,null);
        Gson gson = new Gson();
        lastLocation = gson.fromJson(json,Location.class);
        //set last known home location, if exists
        String latitude = sp.getString(MainActivity.HOME_LATITUDE,"");
        String longtitude = sp.getString(MainActivity.HOME_LONGITUDE,"");
        if ( !latitude.equals("") && !longtitude.equals("")) {
            homeLocation = new Location(LocationManager.GPS_PROVIDER);
            homeLocation.setLatitude(Double.parseDouble(latitude));
            homeLocation.setLongitude(Double.parseDouble(longtitude));
        }
    }

    @Override
    public ListenableFuture<Result> startWork() {

        ListenableFuture<Result> future = CallbackToFutureAdapter.getFuture(
                new CallbackToFutureAdapter.Resolver<Result>() {
                    @Nullable
                    @Override
                    public Object attachCompleter(@NonNull CallbackToFutureAdapter.Completer<Result> callback) throws Exception {
                        LocationWork.this.callback = callback;
                        Log.d("debugshani","creating listanable future");
                        Context context = LocationWork.this.context;
                        boolean hasLocationPermission = context.checkSelfPermission(
                                Manifest.permission.ACCESS_FINE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED;
                        boolean hasSmsPermission = context.checkSelfPermission(
                                Manifest.permission.SEND_SMS)
                                == PackageManager.PERMISSION_GRANTED;
                        String phoneNumber = sp.getString(MainActivity.PHONE,null);
//                        String content = sp.getString(MainActivity.CONTENT,null);
                        if (hasLocationPermission && hasSmsPermission && phoneNumber != null) {
                            LocationRequest locationRequest = new LocationRequest();
                            locationRequest.setInterval(5);
                            fusedLocationClient.requestLocationUpdates(locationRequest,
                                    locationCallback,
                                    Looper.getMainLooper());//TODO check what the format
                        }

                        callback.set(Result.success());
                        return null;
                    }
                }
        );
        return future;
    }


}