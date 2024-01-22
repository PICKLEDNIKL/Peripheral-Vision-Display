package com.example.peripheralvisiondisplay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.RectangularBounds;

import java.util.ArrayList;
import java.util.List;

public class DirectionForegroundService extends Service {

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private List<String> stepsList = new ArrayList<>();
    private List<LatLng> stepsEndLocationList = new ArrayList<>();
    private int currentStepIndex = 0;
    private LatLng currentStepEndLocation;

    final String channelID = "directionforegroundchannelid";
    final int notificationID = 3;

    @Override
    public void onCreate() {
        super.onCreate();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    if (isStepFulfilled(userLocation)) {
                        String nextStep = getNextStep();
                        // Handle the next step...
                    }
                }
            }
        };
        registerReceiver(stepsDataReceiver, new IntentFilter("StepsData"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Create a notification for this foreground service

        // Request location updates
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
////            fusedLocationClient.requestLocationUpdates(createLocationRequest(), locationCallback, Looper.getMainLooper());
//        }

        createNotificationChannel();
        // Create a notification for this foreground service
        Notification notification = new NotificationCompat.Builder(this, channelID)
                .setContentTitle("Direction Service")
                .setContentText("Service is running...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();

        // Call startForeground with the notification
        startForeground(notificationID, notification);

        // Request location updates
        // ...

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Direction Service Channel";
            String description = "Channel for Direction Service";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
        unregisterReceiver(stepsDataReceiver);
    }

//    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private BroadcastReceiver locationUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the latitude and longitude from the Intent
            double currentLatitude = intent.getDoubleExtra("Latitude", 0);
            double currentLongitude = intent.getDoubleExtra("Longitude", 0);

            // Update the map's location
            LatLng currentLatLng = new LatLng(currentLatitude, currentLongitude);

            Log.i("TAG", "directionforegroundlocationreceiver: " + currentLatitude + " " + currentLongitude);

        }
    };

    private BroadcastReceiver stepsDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("StepsData".equals(intent.getAction())) {
                stepsList = intent.getStringArrayListExtra("StepsList");
                Log.d("dfservice", "onReceive works");
                // Now you have the steps data in stepsList. You can use it as needed.

                // Print each step in the log
                for (String step : stepsList) {
                    Log.d("dfservice", "Step: " + step);
                }
            }
            if ("StepsEndLocationData".equals(intent.getAction())) {
                stepsEndLocationList = intent.getParcelableArrayListExtra("StepsEndLocationList");
                Log.d("dfservice", "onReceive works");
                // Now you have the steps data in stepsList. You can use it as needed.

                // Print each step in the log
                for (LatLng step : stepsEndLocationList) {
                    Log.d("dfservice", "Step: " + step);
                }
            }
        }
    };

    public String getNextStep() {
        if (currentStepIndex < stepsList.size()) {
            return stepsList.get(currentStepIndex++);
        } else {
            return null; // No more steps
        }
    }

    public boolean isStepFulfilled(LatLng userLocation) {
        // Calculate the distance between the user's location and the end location of the current step
        double distance = calculateDistance(userLocation, currentStepEndLocation);

        // Consider the step as fulfilled if the distance is less than a certain threshold
        // The threshold can be adjusted based on your requirements
        return distance < 10; // 10 meters
    }

    public double calculateDistance(LatLng point1, LatLng point2) {
        double earthRadius = 6371; // Radius of the earth in km
        double latDiff = Math.toRadians(point2.latitude - point1.latitude);
        double lngDiff = Math.toRadians(point2.longitude - point1.longitude);
        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2)
                + Math.cos(Math.toRadians(point1.latitude)) * Math.cos(Math.toRadians(point2.latitude))
                * Math.sin(lngDiff / 2) * Math.sin(lngDiff / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = earthRadius * c; // Convert to meters
        return distance * 1000;
    }

}
