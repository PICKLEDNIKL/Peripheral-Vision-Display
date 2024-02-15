package com.example.peripheralvisiondisplay;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;

public class NotificationListener extends NotificationListenerService {

    private HashMap<String, String> lastSentNotifications = new HashMap<>();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        // Get the shared preferences
        SharedPreferences sharedPref = getSharedPreferences("NotificationPreferences", Context.MODE_PRIVATE);
        boolean isButtonOn = sharedPref.getBoolean("isButtonOn", false);

//        SharedPreferences sharedPref = getSharedPreferences("NotificationPreferences", Context.MODE_PRIVATE);
        String selectedImportanceLevel = sharedPref.getString("selectedImportanceLevel", "High");

        // Convert the selected importance level to an integer
        int selectedImportance;
        switch (selectedImportanceLevel) {
            case "Urgent":
                selectedImportance = NotificationManager.IMPORTANCE_HIGH;
                break;
            case "High":
                selectedImportance = NotificationManager.IMPORTANCE_DEFAULT;
                break;
            case "Medium":
                selectedImportance = NotificationManager.IMPORTANCE_LOW;
                break;
            case "Low":
                selectedImportance = NotificationManager.IMPORTANCE_MIN;
                break;
            case "None":
                selectedImportance = NotificationManager.IMPORTANCE_NONE;
                break;
            default:
                selectedImportance = NotificationManager.IMPORTANCE_LOW;
                break;
        }

        if (isButtonOn) {
            // Forward the notification data to your foreground service
            String packageName = sbn.getPackageName();
            String channelID = sbn.getNotification().extras.getString(Notification.EXTRA_CHANNEL_ID);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

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


                if (notificationManager != null && channelID != null) {
                    // Get the notification channel
                    NotificationChannel channel = notificationManager.getNotificationChannel(channelID);

                    // Check the importance level of the notification
                    if (channel.getImportance() < selectedImportance) {
                        // If the importance level is less than HIGH, stop processing the notification
                        return;
                    }
                }

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