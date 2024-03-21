package com.example.peripheralvisiondisplay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

/**
 * This service runs in the foreground to receive notification data from the smartphone.
 * It creates a notification to run in the foreground to service notification handling.
 * The service starts and stops through intents by the notification service button in HomeActivity.
 */
public class NotificationForegroundService extends Service {

    final String channelID = "notificationforegroundchannelid";
    final int notificationID = 1;

    public static final String START_ACTION = "com.example.peripheralvisiondisplay.START_FOREGROUND_SERVICE";
    public static final String STOP_ACTION = "com.example.peripheralvisiondisplay.STOP_FOREGROUND_SERVICE";

    private boolean isServiceRunning = false;

    /**
     * Called when the service is created to create a notification channel.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    /**
     * Called when the service is started.
     * Checks the intent action and starts or stops the service accordingly.
     * @param intent The Intent from HomeActivity to start the service.
     * @param flags Flags for start request.
     * @param startID A unique ID for the request to start.
     * @return START_NOT_STICKY makes sure LocationForegroundService is always turned off when the app is restarted.
     */
    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(START_ACTION)) {
                startService();
            } else if (intent.getAction().equals(STOP_ACTION)) {
                stopService();
            }
        }
        return START_NOT_STICKY;
    }

    /**
     * This method is called to say location service cannot be bound
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * It creates a new NotificationChannel with a specified ID, name, and importance level.
     * It retrieves the system's notification manager and creates the notification channel.
     */
    private void createNotificationChannel() {
        // Create a new NotificationChannel with an ID, name, and importance level.
        NotificationChannel channel = new NotificationChannel(
                channelID,
                "Notification Foreground Service Channel",
                NotificationManager.IMPORTANCE_HIGH
        );
        // Retrieve the system's notification manager and if it exists then and create the notification channel.
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * This method starts the NotificationForegroundService if it is not already running.
     * Creates a notification in the foreground to indicate the notification service is running.
     * .setOnGoing(true) makes sure the notification cannot be swiped away.
     */
    private void startService() {
        if (!isServiceRunning) {
            Notification notification = new NotificationCompat.Builder(this, channelID)
                    .setContentTitle("Notification Foreground Service")
                    .setContentText("Foreground service is running")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setOngoing(true)
                    .build();
            startForeground(notificationID, notification);
            isServiceRunning = true;
        }
    }

    /**
     * This method stops the NotificationForegroundService if it is running.
     * Removes service from the foreground, stops the service, and sets isServiceRunning to false.
     */
    private void stopService() {
        if (isServiceRunning) {
            stopForeground(true);
            stopSelf();
            isServiceRunning = false;
        }
    }

    /**
     * This method is called when the service is destroyed.
     * Sets isServiceRunning to false to indicate the service isn't running.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceRunning = false;
    }

    /**
     * This method is called when the service is removed from the recent apps list.
     * Service is stopped to prevent it from running in the background.
     *
     * @param rootIntent The intent that was used to launch the task that is being removed.
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // Stop the service when the app is swiped off from the recent apps list
        stopService();
    }
}