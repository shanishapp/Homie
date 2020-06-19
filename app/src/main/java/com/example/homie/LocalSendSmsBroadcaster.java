package com.example.homie;

import android.app.Application;
import android.content.IntentFilter;

public class LocalSendSmsBroadcaster extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        LocalSendSmsBroadcastReceiver receiver = new LocalSendSmsBroadcastReceiver();
        registerReceiver(receiver, new IntentFilter(MainActivity.CHANNEL_ID));
    }
}
