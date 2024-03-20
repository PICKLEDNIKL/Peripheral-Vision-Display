package com.example.peripheralvisiondisplay;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * This service handles location updates in the foreground and shows a notification to the user.
 * FusedLocationProviderClient is used to get location updates.
 * The service starts and stops through intents by the location service button in HomeActivity.
 */
public class LocationForegroundService extends Service {

    final String channelID = "locationforegroundchannelid";
    final int notificationID = 2;

    public static final String START_ACTION = "com.example.peripheralvisiondisplay.START_FOREGROUND_SERVICE";
    public static final String STOP_ACTION = "com.example.peripheralvisiondisplay.STOP_FOREGROUND_SERVICE";

    private boolean isServiceRunning = false;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;

    /**
     * This method is called when the service is created.
     * It checks if the android version is oreo or higher.
     * If it is, it calls the method to create a notification channel.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        // Create a notification channel if the android version is oreo or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
    }

    /**
     * This method handles starting and stopping the service.
     * It gets the state of the location service button from sharedPref.
     *
     * @param intent The Intent from HomeActivity to start the service.
     * @param flags Flags for start request.
     * @param startID A unique ID for the request to start.
     * @return START_NOT_STICKY makes sure LocationForegroundService is always turned off when the app is restarted.
     */
    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        // Check if the intent is not null and if it has an action
        if (intent != null && intent.getAction() != null) {
            // Retrieve the state of the location service button from SharedPreferences
            SharedPreferences sharedPref = getSharedPreferences("LocationPreferences", Context.MODE_PRIVATE);
            boolean isButtonOn = sharedPref.getBoolean("isButtonOn", false);

            // If the action is to start the service and the location service button is on, start the service
            if (intent.getAction().equals(START_ACTION) && isButtonOn) {
                startService();
            }
            // If the action is to stop the service or the location service button is off, stop the service
            else if (intent.getAction().equals(STOP_ACTION) || !isButtonOn) {
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
     * This method requires and creates a notification channel for Android Oreo and higher.
     * It creates a new NotificationChannel with a specified ID, name, and importance level.
     * It then retrieves the system's NotificationManager and creates the notification channel.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        // Create a new NotificationChannel with an ID, name, and importance level.
        NotificationChannel channel = new NotificationChannel(
                channelID,
                "Location Foreground Service Channel",
                NotificationManager.IMPORTANCE_HIGH
        );
        // Retrieve the manager for notifications and create the notification channel.
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * This method starts the location service if not already running.
     * Creates a notification in the foreground to indicate the location service is running.
     * Creates a LocationRequest with settings for rate of location updates.
     * If location permission is granted, request location updates from the FusedLocationProviderClient.
     */
    private void startService() {
        // Check if the service is not already running.
        if (!isServiceRunning) {

            // Create a notification.
            Notification notification = new NotificationCompat.Builder(this, channelID)
                    .setContentTitle("Location Foreground Service")
                    .setContentText("Foreground service is running")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setOngoing(true)
                    .build();

            // Start the the notification in the foreground.
            startForeground(notificationID, notification);
            isServiceRunning = true;

            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

            // LocationCallback is used to get location updates.
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    // Check if the location result is not null
                    if (locationResult == null) {
                        return;
                    }
                    // Loop through locations in the location result
                    for (Location location : locationResult.getLocations()) {
                        String locationText = "Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude();

                        // Create an intent to broadcast the location
                        Intent intent = new Intent("LocationUpdates");
                        intent.putExtra("Latitude", location.getLatitude());
                        intent.putExtra("Longitude", location.getLongitude());

                        // Broadcast the Intent
                        sendBroadcast(intent);
                    }
                }
            };

            // Create a LocationRequest with specific parameters
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                    .setWaitForAccurateLocation(true)
                    .setMinUpdateIntervalMillis(500)
                    .setMaxUpdateDelayMillis(1000)
                    .build();

            // Check if location permission is granted
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // If it is, request location updates from the FusedLocationProviderClient
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            }
        }
    }

    /**
     * This method stops the location service if it is running.
     * Removes location updates from the FusedLocationProviderClient.
     * Stops the notification in the foreground.
     */
    private void stopService() {
        // Check if the service is running
        if (isServiceRunning) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            stopForeground(true);
            stopSelf();
            isServiceRunning = false;
        }
    }

    /**
     * This method is called when the service is destroyed.
     * It stops the service and removes location updates.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        stopForeground(true);
        isServiceRunning = false;
    }

    /**
     * This method is called when the service is removed from the recent apps list.
     * It stops the service.
     *
     * @param rootIntent The intent to remove the task.
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // Stop the service when the app is swiped off from the recent apps list
        stopService();
    }
}