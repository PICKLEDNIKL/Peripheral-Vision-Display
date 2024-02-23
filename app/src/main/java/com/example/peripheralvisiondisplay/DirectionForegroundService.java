package com.example.peripheralvisiondisplay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

//todo need to make it so that if the user has walked the wrong way, it can update the directions to accomodate the user.
//todo: also need to make it so that it tells the user if they are walking the wrong way. i may need to get the previous step information for them to get to the destination of that step before they can continue.
//todo: check if the user is following the path
public class DirectionForegroundService extends Service{

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
    private LatLng firstLatlng;
    private Location firstLocation;

    private int straightcount = 0;


    private String currentStepString;

    private LatLng[] latLngArray = new LatLng[2]; //index 0 = previous, index 1 = current location

    final String channelID = "directionforegroundchannelid";
    final int notificationID = 3;

    private String apikey = BuildConfig.apiKey;

    private BluetoothLeService mBluetoothLeService;

    private boolean firststraightqueue = false;

    // Create a Queue to store the last 5 location updates
    private Queue<Location> locationQueue = new LinkedList<>();

    @Override
    public void onCreate() {
        super.onCreate();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        IntentFilter filter = new IntentFilter("StepsData");
        registerReceiver(stepsDataReceiver, filter);

        IntentFilter filter2 = new IntentFilter("LocationUpdates");
        registerReceiver(locationUpdateReceiver, filter2);

        Intent bleintent = new Intent(this, BluetoothLeService.class);
        bindService(bleintent, serviceConnection, BIND_AUTO_CREATE);

    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BluetoothLeService.LocalBinder binder = (BluetoothLeService.LocalBinder) service;
            mBluetoothLeService = binder.getService();

            // Send the preferences to the Bluetooth device
            SharedPreferences ledsharedPref = getSharedPreferences("LedPreferences", Context.MODE_PRIVATE);
            mBluetoothLeService.sendSettingPref(ledsharedPref);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBluetoothLeService = null;
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Create a notification for this foreground service

        createNotificationChannel();
        // Create a notification for this foreground service
        Notification notification = new NotificationCompat.Builder(this, channelID)
                .setContentTitle("Direction Service")
                .setContentText("Service is running...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();

        // Call startForeground with the notification
        startForeground(notificationID, notification);
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
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        unregisterReceiver(stepsDataReceiver);
        unbindService(serviceConnection);
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

            currentLatLng = new LatLng(currentLatitude, currentLongitude);

            Location currentLocation = new Location("");
            currentLocation.setLatitude(currentLatitude);
            currentLocation.setLongitude(currentLongitude);

            if (currentStepEndLocation == null) {
                // currentStepEndLocation is not available, skip processing
                return;
            }
            Location stependlocation = new Location("");
            stependlocation.setLatitude(currentStepEndLocation.latitude);
            stependlocation.setLongitude(currentStepEndLocation.longitude);

            float movementThreshold = 5; // Set this to a value that makes sense for your application // this was set to 10 before

            // Check if the locationQueue is empty
            if (locationQueue.isEmpty()) {
                // This is the user's first location, add it to the queue
                locationQueue.add(currentLocation);
            } else {
                // Calculate the distance between the new location and the last location in the queue
                Location lastLocation = locationQueue.peek();
                float distanceMoved = lastLocation.distanceTo(currentLocation);

                // If the distance is greater than the movementThreshold, add the new location to the queue
                if (distanceMoved > movementThreshold) {
                    locationQueue.add(currentLocation);
                    Log.d("DirectionForegroundService", "onReceive: location added to queue");
//                    Toast.makeText(DirectionForegroundService.this, "location added to queue", Toast.LENGTH_SHORT).show();
                }
            }

            if (locationQueue.size() == 4) {
                float totalBearing = 0;
                Location[] locations = locationQueue.toArray(new Location[0]);
                for (int i = 0; i < locations.length - 1; i++) {
                    totalBearing += locations[i].bearingTo(locations[i + 1]);
                }
                float averageBearing = totalBearing / (locationQueue.size() - 1);

                // Calculate the bearing from the start location to the stependlocation
                float bearingFromStart = locations[0].bearingTo(stependlocation);

                // Check if the average bearing is within a range of 60 degrees from the bearing from start
                if (Math.abs(averageBearing - bearingFromStart) <= 60) {
                    straightcount++;
                    // The user is moving in the right direction, send a message to keep going straight
                    if (mBluetoothLeService != null) {
                        if (straightcount == 3 || !firststraightqueue){
                            mBluetoothLeService.queueMessage("STR");
                            Toast.makeText(DirectionForegroundService.this, "STR", Toast.LENGTH_SHORT).show();
                            straightcount = 0;
                            firststraightqueue = true;
                        }
                    } else {
                        if (straightcount == 3 || !firststraightqueue){
                            // Handle the situation when mBluetoothLeService is null
                            Log.e("DirectionForegroundService", "mBluetoothLeService is null for sending message STR");
                            Toast.makeText(DirectionForegroundService.this, "STR", Toast.LENGTH_SHORT).show();
                            straightcount = 0;
                            firststraightqueue = true;
                        }
                    }
                } else {
                    straightcount = 2;
                    // The user is not moving in the right direction, send a message to turn
                    if (mBluetoothLeService != null) {
                        mBluetoothLeService.queueMessage("TURN");
                        Toast.makeText(DirectionForegroundService.this, "TURN", Toast.LENGTH_SHORT).show();
                    } else {
                        // Handle the situation when mBluetoothLeService is null
                        Log.e("DirectionForegroundService", "mBluetoothLeService is null for sending message TURN");
                        Toast.makeText(DirectionForegroundService.this, "TURN", Toast.LENGTH_SHORT).show();
                    }
                }
                locationQueue.clear();
            }

            if (isStepFulfilled()) {
                straightcount = 0;
                firststraightqueue = false;
                locationQueue.clear();

                // Handle condition when the user has completed their journey and there are no step information left.
                currentStepString = getNextStep();
                if (currentStepString == null){
                    currentStepString = "You have reached your destination.";
                }
                mBluetoothLeService.sendDirectionInfo(currentStepString);

                currentStepEndLocation = getNextStepEndLocation();
                currentStepIndex++;
                createNotification("Step "+ currentStepIndex + " : " + currentStepString);
            }

//            if (isUserOffPath()) {
//
//                //TODO: NEED TO ADD THIS BACK AND MAKE IT WORK ASWELL
////                String url = "https://maps.googleapis.com/maps/api/directions/json" +
////                        "?destination=" + MapsActivity.selectedPlace.latitude + "," + MapsActivity.selectedPlace.longitude +
////                        "&mode=walking" +
////                        "&origin=" + currentLatitude + "," + currentLongitude +
////                        "&key=" + apikey;
////
////                Intent newdirectionintent = new Intent("RecalcPath");
////                newdirectionintent.putExtra("url", url);
////                sendBroadcast(newdirectionintent);
//
//                // Execute the AsyncTask to perform the API request
////                new DirectionsTask(DirectionForegroundService.this).execute(url);
//            }
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
//                mBluetoothLeService.sendDirectionInfo(currentStepString);
                if (mBluetoothLeService != null) {
                    mBluetoothLeService.sendDirectionInfo(currentStepString);
                } else {
                    // Handle the situation when mBluetoothLeService is null
                    // For example, you can log an error message
                    Log.e("DirectionForegroundService", "mBluetoothLeService is null for stepdata");
                }

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
//                Toast.makeText(this, "user distance > expected distance", Toast.LENGTH_SHORT).show();
                Log.d("directionforegroundservice", "isUserOffPath: true 10%");
                return true; // The user is off the path
            }

            // Check if the user is moving towards the end of the step
            if (latLngArray[0] != null) {
                double previousDistance = calculateDistance(latLngArray[0], currentStepEndLocation);
                if (currentDistance >= previousDistance * 1.05) {
//                    Toast.makeText(this, "user is moving off path", Toast.LENGTH_SHORT).show();
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

    private float calculateBearing(LatLng point1, LatLng point2) {
        double lat1 = Math.toRadians(point1.latitude);
        double lat2 = Math.toRadians(point2.latitude);
        double deltaLng = Math.toRadians(point2.longitude - point1.longitude);

        double y = Math.sin(deltaLng) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLng);

        return (float) ((Math.toDegrees(Math.atan2(y, x)) + 360) % 360);
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


