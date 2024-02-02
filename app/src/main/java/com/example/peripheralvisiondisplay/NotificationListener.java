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

import java.util.HashMap;
import java.util.List;

public class NotificationListener extends NotificationListenerService {

    private HashMap<String, String> lastSentNotifications = new HashMap<>();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        // Get the shared preferences
        SharedPreferences sharedPref = getSharedPreferences("NotificationPreferences", Context.MODE_PRIVATE);
        boolean isButtonOn = sharedPref.getBoolean("isButtonOn", false);

        if (isButtonOn) {
            // Forward the notification data to your foreground service
            String packageName = sbn.getPackageName();
            //todo: figure out what to do with notifications that dont have tickertext
            if (sbn.getNotification().tickerText != null) {
                String notificationText = sbn.getNotification().tickerText.toString();

                // Check if the new notification is the same as the last sent notification
                if (notificationText.equals(lastSentNotifications.get(packageName))) {
                    // Ignore the new notification
                    return;
                }

                // Update the last sent notification for the application
                lastSentNotifications.put(packageName, notificationText);



                Intent serviceIntent = new Intent(this, NotificationForegroundService.class);
                serviceIntent.setAction("com.example.peripheralvisiondisplay.NEW_NOTIFICATION");
                serviceIntent.putExtra("packageName", packageName);
                serviceIntent.putExtra("notificationText", notificationText);

                // Send a broadcast with the notification text
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction("com.example.peripheralvisiondisplay.NEW_NOTIFICATION");
                broadcastIntent.putExtra("notificationText", notificationText);
                sendBroadcast(broadcastIntent);

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