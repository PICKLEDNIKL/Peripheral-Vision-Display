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
import android.util.Log;
import android.location.LocationRequest.Builder;

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

public class LocationForegroundService extends Service {

    final String channelID = "locationforegroundchannelid";
    final int notificationID = 2;

    public static final String START_ACTION = "com.example.peripheralvisiondisplay.START_FOREGROUND_SERVICE";
    public static final String STOP_ACTION = "com.example.peripheralvisiondisplay.STOP_FOREGROUND_SERVICE";

    private boolean isServiceRunning = false;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        if (intent != null && intent.getAction() != null) {
            SharedPreferences sharedPref = getSharedPreferences("LocationPreferences", Context.MODE_PRIVATE);
            boolean isButtonOn = sharedPref.getBoolean("isButtonOn", false);

            if (intent.getAction().equals(START_ACTION) && isButtonOn) {
                startService();
                Notification notification = new NotificationCompat.Builder(this, channelID)
                        .setContentTitle("Location Foreground Service")
                        .setContentText("Foreground service is running")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setOngoing(true)
                        .build();
//                Notification notification = notificationBuilder.build();
                startForeground(notificationID, notification);
            } else if (intent.getAction().equals(STOP_ACTION) || !isButtonOn) {
                stopService();
            } else if (intent.getAction().equals("com.example.peripheralvisiondisplay.NEW_NOTIFICATION")){
                handleNewNotification(intent);
            }
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                channelID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_HIGH
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }



    private void startService() {
        if (!isServiceRunning) {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        return;
                    }
                    for (Location location : locationResult.getLocations()) {
                        String locationText = "Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude();
                        updateNotification(locationText);

                        // Create an Intent to broadcast the location
                        Intent intent = new Intent("LocationUpdates");
                        intent.putExtra("Latitude", location.getLatitude());
                        intent.putExtra("Longitude", location.getLongitude());

                        // Broadcast the Intent
                        sendBroadcast(intent);
                    }
                }
            };

            isServiceRunning = true;

            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                    .setWaitForAccurateLocation(true)
                    .setMinUpdateIntervalMillis(500)
                    .setMaxUpdateDelayMillis(1000)
                    .build();

            //removing this code stops everything from working
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            }
        }
    }

    private void stopService() {
        if (isServiceRunning) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            stopForeground(true);
            stopSelf();
            isServiceRunning = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceRunning = false;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // Stop the service when the app is swiped off from the recent apps list
        stopService();
    }

    private void handleNewNotification(Intent intent) {
        Log.d("ForegroundService", "handlenewnotification works");
        // Retrieve notification data from the intent

        String notificationText = intent.getStringExtra("notificationText");

        // Update the notification content with the latest listened notification text
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelID)
                        .setContentTitle("Location Foreground Service")
                        .setContentText(notificationText) // Set the latest notification text here
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        Notification notification = notificationBuilder.build();
        notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;

        startForeground(notificationID, notification);
    }

    private void updateNotification(String content) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelID)
                .setContentTitle("Location Foreground Service")
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true);

        Notification notification = notificationBuilder.build();
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(notificationID, notification);
        }
    }


}