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
import android.widget.Toast;

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
//todo need to make it so that if the user has walked the wrong way, it can update the directions to accomodate the user.
//todo: also need to make it so that it tells the user if they are walking the wrong way. i may need to get the previous step information for them to get to the destination of that step before they can continue.
//todo: check if the user is following the path
public class DirectionForegroundService extends Service {

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private List<String> stepsList = new ArrayList<>();
    private List<LatLng> stepsEndLocationList = new ArrayList<>();
    private List<Integer> stepsDistanceList = new ArrayList<>();
    private double currentLatitude;
    private double currentLongitude;
    private int currentStepIndex = 0;
    private LatLng currentLatLng;
    private LatLng currentStepEndLocation;
    private String currentStepString;

    private LatLng[] latLngArray = new LatLng[2]; //index 0 = previous, index 1 = current location

    final String channelID = "directionforegroundchannelid";
    final int notificationID = 3;

    private String apikey = BuildConfig.apiKey;

    @Override
    public void onCreate() {
        super.onCreate();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        IntentFilter filter = new IntentFilter("StepsData");
        registerReceiver(stepsDataReceiver, filter);

        IntentFilter filter2 = new IntentFilter("LocationUpdates");
        registerReceiver(locationUpdateReceiver, filter2);
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
            currentLatitude = intent.getDoubleExtra("Latitude", 0);
            currentLongitude = intent.getDoubleExtra("Longitude", 0);

            // Update the LatLng array
            if (latLngArray[1] != null) {
                latLngArray[0] = latLngArray[1]; // Move the current LatLng to the previous LatLng
            }
            latLngArray[1] = new LatLng(currentLatitude, currentLongitude); // Update the current LatLng

            currentLatLng = new LatLng(currentLatitude, currentLongitude);
            Log.i("LOCATIONTAG", "directionforegroundlocationreceiver: " + currentLatitude + " " + currentLongitude);

            if (isStepFulfilled()) {

                // Handle condition when the user has completed their journey and there are no step information left.
                currentStepString = getNextStep();
                if (currentStepString == null){
                    currentStepString = "You have reached your destination.";
                }

                currentStepEndLocation = getNextStepEndLocation();
                currentStepIndex++;
                createNotification("Step "+ currentStepIndex + " : " + currentStepString);
            }

            if (isUserOffPath()) {

                String url = "https://maps.googleapis.com/maps/api/directions/json" +
                        "?destination=" + MapsActivity.selectedPlace.latitude + "," + MapsActivity.selectedPlace.longitude +
                        "&mode=walking" +
                        "&origin=" + currentLatitude + "," + currentLongitude +
                        "&key=" + apikey;

                Intent newdirectionintent = new Intent("RecalcPath");
                newdirectionintent.putExtra("url", url);
                sendBroadcast(newdirectionintent);

                // Execute the AsyncTask to perform the API request
//                new DirectionsTask(DirectionForegroundService.this).execute(url);
            }

            //todo: if the user is walking away from the direction = mention to the user to turn back. - use end location of current step.


        }
    };

    private BroadcastReceiver stepsDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("StepsData".equals(intent.getAction())) {
                stepsList = intent.getStringArrayListExtra("StepsList");
                stepsEndLocationList = intent.getParcelableArrayListExtra("StepsEndLocationList");
                stepsDistanceList = intent.getIntegerArrayListExtra("StepsDistanceList");

                Log.d("dfservice", "onReceive works");

                // reset the current step index if the user is getting new directions
                currentStepIndex = 0;

                currentStepString = stepsList.get(currentStepIndex);
                currentStepEndLocation = stepsEndLocationList.get(currentStepIndex);
                currentStepIndex++;
                createNotification("Step "+ currentStepIndex + " : " + currentStepString);

                // Print each step in the log
                for (String step : stepsList) {
                    Log.d("dfservice", step);
                }
                // Print each step in the log
                for (LatLng step : stepsEndLocationList) {
                    Log.d("dfservice", "Step latlng: " + step);
                }
                // Print each step in the log
                for (Integer step : stepsDistanceList) {
                    Log.d("dfservice", "Step distance: " + step);
                }

            }
        }
    };

    public String getNextStep() {
        if (currentStepIndex < stepsList.size()) {
            return stepsList.get(currentStepIndex);
        } else {
            return null; // No more steps
        }
    }

    public LatLng getNextStepEndLocation() {
        if (currentStepIndex < stepsEndLocationList.size()) {
            return stepsEndLocationList.get(currentStepIndex);
        } else {
            return null; // No more steps
        }
    }

    public boolean isStepFulfilled() {
        // Check if currentLatLng and currentStepEndLocation are not null
        if (currentLatLng != null && currentStepEndLocation != null) {
            // Calculate the distance between the user's location and the end location of the current step
            double distance = calculateDistance(currentLatLng, currentStepEndLocation);

            // Consider the step as fulfilled if the distance is less than a certain threshold
            // The threshold can be adjusted based on your requirements
            return distance < 15; // 15 meters
        } else {
            // If either currentLatLng or currentStepEndLocation is null, return false
            return false;
        }
    }

    public boolean isUserOffPath() {
        // Check if currentLatLng and currentStepEndLocation are not null
        if (latLngArray[1] != null && currentStepEndLocation != null) {
            // Calculate the distance between the user's location and the end location of the current step
            double currentDistance = calculateDistance(latLngArray[1], currentStepEndLocation);

            // Get the expected distance for the current step
            int expectedDistance = stepsDistanceList.get(currentStepIndex);

            // Check if the user's distance is significantly greater than the expected distance
            if (currentDistance > expectedDistance * 1.1) { // 10% tolerance
                Toast.makeText(this, "user distance > expected distance", Toast.LENGTH_SHORT).show();
                Log.d("directionforegroundservice", "isUserOffPath: true 10%");
                return true; // The user is off the path
            }

            // Check if the user is moving towards the end of the step
            if (latLngArray[0] != null) {
                double previousDistance = calculateDistance(latLngArray[0], currentStepEndLocation);
                if (currentDistance >= previousDistance * 1.05) {
                    Toast.makeText(this, "user is moving off path", Toast.LENGTH_SHORT).show();
                    Log.d("directionforegroundservice", "isUserOffPath: true 1.05%");
                    return true; // The user is not moving towards the end of the step
                }
            }
        }
        Log.d("directionforegroundservice", "isUserOffPath: false ");

        return false;
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


    private void createNotification(String message) {
        Notification notification = new NotificationCompat.Builder(this, channelID)
                .setContentTitle("Direction Service")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(notificationID, notification);
        }
    }

}


