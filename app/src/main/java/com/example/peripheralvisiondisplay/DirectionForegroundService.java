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
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * The DirectionForegroundService class extends the Service class.
 * It is used to provide directions and to check if the user is walking in the correct direction.
 * It runs in the foreground and shows a notification to the user.
 */
public class DirectionForegroundService extends Service{

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private List<String> stepsList = new ArrayList<>();
    private List<LatLng> stepsEndLocationList = new ArrayList<>();
    private double currentLatitude;
    private double currentLongitude;
    private int currentStepIndex = 0;
    private LatLng currentLatLng;
    private Location currentLocation;
    private LatLng currentStepEndLatlng;
    private Location currentStepEndLocation;
    private LatLng firstLatlng;
    private Location firstLocation;
    private int straightcount = 0;
    private int turncount = 0;
    private int straightdistcount = 0;
    private Location[] locationArray = new Location[8];
    private int locationIndex = 0;
    private String currentStepString;
    final String channelID = "directionforegroundchannelid";
    final int notificationID = 3;
    private BluetoothLeService mBluetoothLeService;
    private boolean firststraightqueue = false;
    private Queue<Location> locationQueue = new LinkedList<>();
    private boolean firstlocationasstep = false;
    private Location firstlocationstep;
    private Location currentStepStartLocation;
    private List<LatLng> stepsStartLocationList = new ArrayList<>();
    private float bearingFromStart;
    private Handler handler;
    private Runnable runnable;

    /**
     * This method is called when the service is created.
     * Initialises the FusedLocationProviderClient for location updates.
     * It registers broadcast receivers for receiving steps data and location updates.
     * It binds to the BluetoothLeService to send data to the Bluetooth device.
     * It also sets up a Handler and Runnable for sending a message to the Bluetooth device every 30 seconds to test battery life.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialise the FusedLocationProviderClient for location updates.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Register a BroadcastReceiver to receive steps data from the DirectionsTask
        IntentFilter filter = new IntentFilter("StepsData");
        registerReceiver(stepsDataReceiver, filter);

        // Register a BroadcastReceiver to receive location updates from the LocationService
        IntentFilter filter2 = new IntentFilter("LocationUpdates");
        registerReceiver(locationUpdateReceiver, filter2);

        // Bind to the BluetoothLeService to send alerts to the bluetooth device.
        Intent bleintent = new Intent(this, BluetoothLeService.class);
        bindService(bleintent, serviceConnection, BIND_AUTO_CREATE);

        // TODO: Remove this code after testing.
        // For battery testing to send a message every minute to shine all the LEDs.
        handler = new Handler(Looper.getMainLooper());
        runnable = new Runnable() {
            @Override
            public void run() {
                if (mBluetoothLeService != null) {
                    mBluetoothLeService.sendDirectionInfo("You have reached your destination");
                }
                handler.postDelayed(this, 30000); // Run every 30 seconds
            }
        };
        handler.post(runnable); // Start the Runnable when the service is created
    }

    /**
     * ServiceConnection for managing the lifecycle of the BluetoothLeService.
     * Used to receive callbacks stating if the BluetoothLeService is connected or disconnected.
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        /**
         * This method is called when a connection to the Service has been established.
         * It gets the BluetoothLeService instance and sends the LED preferences to the Bluetooth device.
         *
         * @param cn The component name of the service.
         * @param service The IBinder of the Service's communication channel.
         */
        @Override
        public void onServiceConnected(ComponentName cn, IBinder service) {

            BluetoothLeService.LocalBinder binder = (BluetoothLeService.LocalBinder) service;
            mBluetoothLeService = binder.getService();

            // Get the LED preferences from SharedPreferences and send it to the bluetooth device.
            SharedPreferences ledsharedPref = getSharedPreferences("LedPreferences", Context.MODE_PRIVATE);
            mBluetoothLeService.sendSettingPref(ledsharedPref);
        }

        /**
         * This method is called when connection to the service is lost.
         * It sets the BluetoothLeService instance to null to say there is no connected bluetooth device.
         *
         * @param cn The component name of the service which has lost connection.
         */
        @Override
        public void onServiceDisconnected(ComponentName cn) {
            mBluetoothLeService = null;
        }
    };

    /**
     * This method is called when the service is started.
     * It creates a notification for the foreground service.
     * It starts the notification in the foreground.
     *
     * @param intent The Intent to start the service.
     * @param flags Flags for start request.
     * @param startId A unique ID for the request to start.
     * @return START_STICKY makes sure the DirectionForegroundService always runs until explicitly told to stop.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Create a notification channel.
        createNotificationChannel();

        // Create a notification.
        Notification notification = new NotificationCompat.Builder(this, channelID)
                .setContentTitle("Direction Service")
                .setContentText("Service is running...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();

        // Start notification in the foreground.
        startForeground(notificationID, notification);

        return START_STICKY;
    }

    /**
     * This method creates a notification channel for the DirectionForegroundService.
     * It creates a new NotificationChannel with a channelID, name, and importance level.
     * It then retrieves the system's NotificationManager and creates the channel.
     */
    private void createNotificationChannel() {
        // Define the name and description of the channel
        CharSequence name = "Direction Service Channel";
        String description = "Channel for Direction Service";

        // Create a new NotificationChannel with the channelID, name, and importance level.
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(channelID, name, importance);
        channel.setDescription(description);

        // Get system's NotificationManager
        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        // Make sure that the notificationManager is available before creating the channel.
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * This method is called when the service is destroyed.
     * Remove location updates and unregister broadcast receivers.
     * Unbinds BluetoothLeService.
     * Stops the Runnable for battery testing.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        // Check if the locationCallback exists before removing location updates
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        // Unregister step data receiver and unbind BluetoothLeService.
        unregisterReceiver(stepsDataReceiver);
        unbindService(serviceConnection);

        // TODO: Remove this code after testing.
        // For battery testing to stop it when the service is destroyed
        handler.removeCallbacks(runnable);
    }

    /**
     * This method is called to say location service cannot be bound
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * This BroadcastReceiver is used to receive location updates from the LocationService.
     * It calculates the distance between the user's location and the end location of the current step.
     * It checks if the user is walking in the correct direction based on the bearing.
     * It sends a message to the Bluetooth device to keep going straight or to turn.
     */
    private BroadcastReceiver locationUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the latitude and longitude from the Intent
            currentLatitude = intent.getDoubleExtra("Latitude", 0);
            currentLongitude = intent.getDoubleExtra("Longitude", 0);

            // Create a LatLng object from the latitude and longitude
            currentLatLng = new LatLng(currentLatitude, currentLongitude);

            // Create a Location using current user latitude and longitude.
            currentLocation = new Location("");
            currentLocation.setLatitude(currentLatitude);
            currentLocation.setLongitude(currentLongitude);

            // If currentStepEndLocation is null, skip.
            if (currentStepEndLatlng == null) {
                return;
            }

            // The movement threshold to determine if the user is standing still.
            float movementThreshold = 5; // 5 meters

            // If the locationQueue is empty, this is the user's first location
            if (locationQueue.isEmpty()) {
                if (!firstlocationasstep){
                    firstlocationstep = currentLocation;
                    firstlocationasstep = true;
                }
                // This is the user's first location, add it to the queue
                locationQueue.add(currentLocation);
                locationArray[locationIndex] = currentLocation;
                locationIndex++;

            } else {
                // Calculate distance between the current location and the previous location.
                Location lastLocation = locationQueue.peek();
                float distanceMoved = lastLocation.distanceTo(currentLocation);

                // If the distance is more than the movementThreshold of using standing still, add the current location to the queue
                if (distanceMoved > movementThreshold) {
                    locationQueue.add(currentLocation);
                    locationArray[locationIndex] = currentLocation;
                    locationIndex++;

                    // Reset locationIndex when the the index reaches the length of locationArray
                    if (locationIndex >= locationArray.length-1) {
                        locationIndex = 0;
                    }
                }

                // Previous code for checking if the user is walking in the right direction based on distance.
//                if (locationArray[0] != null && locationArray[7] != null) {
//                    float distanceLtoS = locationArray[0].distanceTo(stependlocation);
//                    float distanceCtoS = currentLocation.distanceTo(stependlocation);
//                    Log.d("TAG", "onReceive: distanceLtoS: " + distanceLtoS);
//                    Log.d("TAG", "onReceive: distanceCtoS: " + distanceCtoS);
//                    if (distanceCtoS > distanceLtoS) {
//                        turncount++;
//                        if (turncount >= 4) {
//                            if (mBluetoothLeService != null) {
//                                mBluetoothLeService.queueMessage("TURN");
//                                Toast.makeText(DirectionForegroundService.this, "TURN", Toast.LENGTH_SHORT).show();
//                                turncount = 0;
//                                straightdistcount = 0;
//                            } else {
//                                Toast.makeText(DirectionForegroundService.this, "TURN", Toast.LENGTH_SHORT).show();
//                                turncount = 0;
//                                straightdistcount = 0;
//                            }
//                            locationArray = new Location[8];
//                            locationIndex = 0;
//                        }
//                    }
//                    else{
//                        straightdistcount++;
//                        if (straightdistcount >= 4){
//                            if (mBluetoothLeService != null) {
//                                mBluetoothLeService.queueMessage("STR");
//                                Toast.makeText(DirectionForegroundService.this, "STR", Toast.LENGTH_SHORT).show();
//                                turncount = 0;
//                                straightdistcount = 0;
//                            } else {
//                                Toast.makeText(DirectionForegroundService.this, "STR", Toast.LENGTH_SHORT).show();
//                                turncount = 0;
//                                straightdistcount = 0;
//                            }
//                            locationArray = new Location[8];
//                            locationIndex = 0;
//                        }
//                    }
//                }
            }

            // If the location queue is filled, process calculating the users movement and send a corresponding alert to the bluetooth device.
            if (locationQueue.size() >= 8) {
                // Initialize a variable to store the total bearing.
                float totalBearing = 0;

                // Convert the location queue to an array of same size.
                Location[] locations = locationQueue.toArray(new Location[8]);

                // Iterate through to get each location pair.
                for (int i = 0; i < locations.length - 1; i++) {
                    // Calculate the bearing between the location pairs and add them up.
                    totalBearing += locations[i].bearingTo(locations[i + 1]);
                }
                // Calculate average bearing the user is walking in.
                float averageBearing = totalBearing / (locationQueue.size());


                // Check if the currentStepStartLocation is null, set it as the first location.
                if (currentStepStartLocation == null) {
                    currentStepStartLocation = new Location("");
                    currentStepStartLocation = firstLocation;
                }

                // Calculate the bearing from the current step start to current step destination.
                bearingFromStart = currentStepStartLocation.bearingTo(currentStepEndLocation);

                // Check if the avg bearing of the user is less than 90 degrees from the step destination which means the user is walking in the right direction.
                if (Math.abs(averageBearing - bearingFromStart) <= 90) {
                    straightcount++;
                    // The user is moving in the right direction, send a message to keep going straight
                    if (mBluetoothLeService != null) {
                        mBluetoothLeService.queueMessage("STR");
                    }
                    Toast.makeText(DirectionForegroundService.this, "Walk Straight", Toast.LENGTH_SHORT).show();
                    straightcount = 0;
                    firststraightqueue = true;

                // Check if the avg bearing of the user is larger than 90 degrees from the step destination which means the user is walking in the wrong direction.
                } else if (Math.abs(averageBearing - bearingFromStart) > 90) {

                    // The user is not moving in the right direction, send a message to turn
                    if (mBluetoothLeService != null) {
                        mBluetoothLeService.queueMessage("TURN");
                    }
                    Toast.makeText(DirectionForegroundService.this, "Wrong Way", Toast.LENGTH_SHORT).show();
                    straightcount = 0;
                }
                locationQueue.clear();
            }

            // Check if the user has reached the end location of the current step. If so, move to the next step and update data accordingly.
            if (isStepFulfilled()) {
                straightcount = 0;
                turncount = 0;
                straightdistcount = 0;
                firststraightqueue = false;
                locationQueue.clear();

                // Check if there are more steps left and update the current step data accordingly.
                if (currentStepIndex < stepsList.size()) {
                    currentStepString = stepsList.get(currentStepIndex);
                    currentStepEndLocation = new Location("");
                    currentStepEndLocation.setLatitude(stepsEndLocationList.get(currentStepIndex).latitude);
                    currentStepEndLocation.setLongitude(stepsEndLocationList.get(currentStepIndex).longitude);
                    currentStepStartLocation = new Location("");
                    currentStepStartLocation.setLatitude(stepsStartLocationList.get(currentStepIndex).latitude);
                    currentStepStartLocation.setLongitude(stepsStartLocationList.get(currentStepIndex).longitude);
                    currentStepEndLatlng = stepsEndLocationList.get(currentStepIndex);
                    currentStepIndex++;
                    createNotification("Step "+ currentStepIndex + ": " + currentStepString);

                // No more steps left, handle end of navigation
                } else {
                    currentStepString = "You have reached your destination.";
                    Toast.makeText(DirectionForegroundService.this, "You have reached your destination.", Toast.LENGTH_SHORT).show();
                    createNotification("You have reached your destination.");
                    currentStepEndLatlng = null;
                }

                // Send the current step string to the Bluetooth device
                if (mBluetoothLeService != null) {
                    mBluetoothLeService.sendDirectionInfo(currentStepString);
                } else {
                    // Handle the situation when mBluetoothLeService is null
                    Log.e("DirectionForegroundService", "mBluetoothLeService is null");
                }

                // Reset the location array and index
                locationArray = new Location[8];
                locationIndex = 0;
            }
        }
    };

    /**
     * This BroadcastReceiver is used to receive steps data from the DirectionsTask.
     * It updates the current step data and sends the current step string to the Bluetooth device.
     * It also creates a notification for the current step.
     */
    private BroadcastReceiver stepsDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("StepsData".equals(intent.getAction())) {
                // Get the steps data for navigation.
                stepsList = intent.getStringArrayListExtra("StepsList");
                stepsEndLocationList = intent.getParcelableArrayListExtra("StepsEndLocationList");
                firstLatlng = intent.getParcelableExtra("FirstLatLng");

                // Clear the start locations list
                stepsStartLocationList.clear();

                // Add the user's start location from when the API request was handled as the first start step location.
                stepsStartLocationList.add(firstLatlng);

                // Add the step end locations to the start locations list which will now be used as the beginning
                stepsStartLocationList.addAll(stepsEndLocationList);

                // reset the current step index if the user is getting new directions
                currentStepIndex = 0;

                // Get the first step instruction.
                currentStepString = stepsList.get(currentStepIndex);

                // If there is a connected bluetooth device, send the first step instruction to the device.
                if (mBluetoothLeService != null) {
                    mBluetoothLeService.sendDirectionInfo(currentStepString);
                } else {
                    Log.e("DirectionForegroundService", "mBluetoothLeService is null");
                }

                // Set the start location for the current step
                currentStepStartLocation = new Location("");
                currentStepStartLocation.setLatitude(stepsStartLocationList.get(currentStepIndex).latitude);
                currentStepStartLocation.setLongitude(stepsStartLocationList.get(currentStepIndex).longitude);

                // Set the end location for the current step
                currentStepEndLocation = new Location("");
                currentStepEndLocation.setLatitude(stepsEndLocationList.get(currentStepIndex).latitude);
                currentStepEndLocation.setLongitude(stepsEndLocationList.get(currentStepIndex).longitude);

                // Set the LatLng for the end location of the current step
                currentStepEndLatlng = stepsEndLocationList.get(currentStepIndex);
                currentStepIndex++;
                createNotification("Step " + currentStepIndex + ": " + currentStepString);
            }
        }
    };

    /**
     * This method checks if the user has reached the end location of the current step.
     * It calculates the distance between the user's location and the end location of the current step.
     * If the distance is less than a certain threshold, the step is considered fulfilled.
     *
     * @return true if the user has reached the end location of the current step, false otherwise.
     */
    public boolean isStepFulfilled() {

        // Check if currentLatLng and currentStepEndLocation are not null
        if (currentLatLng != null && currentStepEndLatlng != null) {

            // Calculate the distance between the user's location and the end location of the current step
            float distance = currentLocation.distanceTo(currentStepEndLocation);

            // Consider the step as fulfilled if the distance is less than a certain threshold
            return distance < 11; // 11 meters

        // If currentLatLng or currentStepEndLocation is null, return false
        } else {
            return false;
        }
    }

    /**
     * This method creates a notification for the DirectionForegroundService.
     * It then gets the system's notification manager and posts the notification.
     *
     * @param message The message displayed in the notification.
     */
    private void createNotification(String message) {

        Notification notification = new NotificationCompat.Builder(this, channelID)
                .setContentTitle("Direction Service")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();

        // Check if there is a notification manager before displaying the notification
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(notificationID, notification);
        }
    }
}