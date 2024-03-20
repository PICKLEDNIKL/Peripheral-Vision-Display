package com.example.peripheralvisiondisplay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.content.Context;
import android.content.Intent;
import android.service.notification.StatusBarNotification;
import java.util.HashMap;

/**
 * This class listens for notifications and forwards them to the NotificationForegroundService.
 */
public class NotificationListener extends NotificationListenerService {

    private HashMap<String, String> lastSentNotifications = new HashMap<>();

    /**
     * This method is called when a new notification is posted.
     * It forwards the notification data to the NotificationForegroundService.
     *
     * @param sbn The StatusBarNotification holding posted notification data.
     */
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        // Get the shared preference to see if the button to start notification service is on.
        SharedPreferences sharedPref = getSharedPreferences("NotificationPreferences", Context.MODE_PRIVATE);
        boolean isButtonOn = sharedPref.getBoolean("isButtonOn", false);

        // Get the selected importance level from shared preferences
        String selectedImportanceLevel = sharedPref.getString("selectedImportanceLevel", "High");

        // Convert the importance level from settings to the corresponding NotificationManager importance level
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

        // If the notification service is on, process the notification
        if (isButtonOn) {
            // Get the package name and channel ID of the notification
            String packageName = sbn.getPackageName();
            String channelID = sbn.getNotification().extras.getString(Notification.EXTRA_CHANNEL_ID);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            // Only handle notifications that have a ticker text
            if (sbn.getNotification().tickerText != null) {
                String notificationText = sbn.getNotification().tickerText.toString();

                // Ignore notification if its the same as the previous received notification
                if (notificationText.equals(lastSentNotifications.get(packageName))) {
                    return;
                }

                // Update the last sent notification for the application
                lastSentNotifications.put(packageName, notificationText);

                // If the notification manager is not null and the channel ID is not null, get the notification channel
                if (notificationManager != null && channelID != null) {
                    NotificationChannel channel = notificationManager.getNotificationChannel(channelID);

                    // Check the importance level of the notification.
                    if (channel.getImportance() < selectedImportance) {
                        // If the importance level is less than HIGH, stop processing the notification.
                        return;
                    }
                }

                // Create an Intent to start the NotificationForegroundService
                Intent serviceIntent = new Intent(this, NotificationForegroundService.class);
                serviceIntent.setAction("com.example.peripheralvisiondisplay.NEW_NOTIFICATION");
                serviceIntent.putExtra("packageName", packageName);
                serviceIntent.putExtra("notificationText", notificationText);

                // Send a broadcast with the notification text
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction("com.example.peripheralvisiondisplay.NEW_NOTIFICATION");
                broadcastIntent.putExtra("notificationText", notificationText);
                sendBroadcast(broadcastIntent);

                // Start the NotificationForegroundService as a foreground service if the device is running Android Oreo or higher
                startForegroundService(serviceIntent);
            }
        }
    }

    /**
     * This method is called when the app is swiped off from the recent apps list.
     *
     * @param rootIntent root intent of the application being swiped off.
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    /**
     * This method is called when a notification is removed.
     * Can be handled in the future if needed.
     *
     * @param sbn The StatusBarNotification holding removed notification data.
     */
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }
}