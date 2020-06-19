package com.example.homie;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.util.Log;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class HomieApp extends Application {

    private BroadcastReceiver myReceiver;


    @Override
    public void onCreate() {
        super.onCreate();

        myReceiver = new LocalSendSmsBroadcastReceiver();
        registerReceiver(myReceiver,new IntentFilter(MainActivity.BROADCAST_SMS));
        startWorkManager();
    }

    private void startWorkManager() {
        Log.d("************hi**********","bye");
        PeriodicWorkRequest locationWork = new PeriodicWorkRequest.Builder(LocationWork.class,
                                                        15, TimeUnit.MINUTES).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("locationWork", ExistingPeriodicWorkPolicy.REPLACE,locationWork);
    }
}
