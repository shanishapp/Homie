package com.example.homie;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SmsManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

public class LocalSendSmsBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (action != null && !action.equals(MainActivity.BROADCAST_SMS)) {
            return;
        }

        boolean hasPermission =
                ActivityCompat.checkSelfPermission(context,
                        Manifest.permission.SEND_SMS) ==
                        PackageManager.PERMISSION_GRANTED;
        if (hasPermission) {
            String phoneNum = intent.getStringExtra(MainActivity.PHONE);
            String content = intent.getStringExtra(MainActivity.CONTENT);

            if (phoneNum == null || content == null) {
                // error to log
                return;
            }

            SmsManager smgr = SmsManager.getDefault();
            smgr.sendTextMessage(phoneNum, null, content, null, null);
            String textContent = "sending sms to " + phoneNum + ": " + content;
            fireNotification(textContent,context);
        } else {
            //error to log
        }

    }
    private void fireNotification(String content, Context context){
        createChannelIfNotExists(content,context);
        Notification builder = new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
        .setContentText(content)
        .setSmallIcon(R.drawable.massages_icon)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT).build();
        NotificationManager notifictionManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notifictionManager.notify(MainActivity.NOTIFICATION_ID, builder);
    }

    private void createChannelIfNotExists(String description, Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "sendSmsOnReceive";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(MainActivity.CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
