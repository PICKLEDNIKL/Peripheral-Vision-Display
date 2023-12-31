package com.example.peripheralvisiondisplay;

import android.app.ActivityManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.List;

public class NotificationListener extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        // Get the shared preferences
        SharedPreferences sharedPref = getSharedPreferences("NotificationPreferences", Context.MODE_PRIVATE);
        boolean isButtonOn = sharedPref.getBoolean("isButtonOn", false);

        if (isButtonOn) {
            // Forward the notification data to your foreground service
            String packageName = sbn.getPackageName();
            if (sbn.getNotification().tickerText != null) {
                String notificationText = sbn.getNotification().tickerText.toString();
                Intent serviceIntent = new Intent(this, NotificationForegroundService.class);
                serviceIntent.setAction("com.example.peripheralvisiondisplay.NEW_NOTIFICATION");
                serviceIntent.putExtra("packageName", packageName);
                serviceIntent.putExtra("notificationText", notificationText);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                Log.d("notificationlistener", "seen notification");
            } else {
                Log.d("notificationlistener", "notification doesnt have tickertext");
            }
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // Stop the service when the app is swiped off from the recent apps list
        stopSelf();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Handle notification removal if needed
    }
}