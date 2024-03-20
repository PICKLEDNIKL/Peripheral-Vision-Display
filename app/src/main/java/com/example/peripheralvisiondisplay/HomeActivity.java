package com.example.peripheralvisiondisplay;

import static com.example.peripheralvisiondisplay.BluetoothActivity.REQUEST_CODE;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.Manifest;
import android.widget.Toast;
import android.os.Process;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
/**
 * HomeActivity is the main activity of the application.
 * It has buttons to start and stop the notification and location services.
 * Theres also a bottom navigation bar to switch to other activities.
 */
public class HomeActivity extends AppCompatActivity {
    Button notificationServiceButton;
    Button locationServiceButton;
    BottomNavigationView bottomNavigationView;
    boolean toggleNotificationService = false;
    boolean toggleLocationService = false;

    private BluetoothLeService bluetoothLeService;
    private boolean bound = false;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_SCAN_PERMISSION = 1;
    private static final String PREFS_NAME = "HomeActivityPrefs";
    private static final String PREFS_TOGGLE_LOCATION_SERVICE = "toggleLocationService";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Get shared preferences for LED settings and check if it's the first launch of the app.
        SharedPreferences sharedPref = getSharedPreferences("LedPreferences", Context.MODE_PRIVATE);
        boolean isFirstLaunch = sharedPref.getBoolean("isFirstLaunch", true);
        // If it's the first launch, set the default LED colors.
        if (isFirstLaunch) {
            new Handler().postDelayed(() -> {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("isFirstLaunch", false);
                editor.putInt("notifColor", Color.YELLOW);
                editor.putInt("leftColor", Color.BLUE);
                editor.putInt("rightColor", Color.BLUE);
                editor.putInt("straightColor", Color.GREEN);
                editor.putInt("turnColor", Color.RED);
                editor.apply();
            }, 5000); // Delay of 5 seconds
        }

        // Initialize the notification service button and run the toggleNotificationService function when clicked.
        notificationServiceButton = findViewById(R.id.notificationServiceButton);
        notificationServiceButton.setOnClickListener(view -> toggleNotificationService());

        // Initialize the location service button and run the toggleLocationService function when clicked.
        locationServiceButton = findViewById(R.id.locationServiceButton);
        locationServiceButton.setOnClickListener(view -> toggleLocationService());

        // Initialize the bottom navigation bar and set the listener for the menu items.
        bottomNavigationView = findViewById(R.id.bottom_menu);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Intent intent;
            int itemId = item.getItemId();

            // Switch to the selected activity.
            if (itemId == R.id.home) {
                intent = new Intent(HomeActivity.this, HomeActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.map) {
                // Check if location permission is granted.
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    if (toggleLocationService) {
                        intent = new Intent(HomeActivity.this, MapsActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Please start the location service first", Toast.LENGTH_SHORT).show();
                    }
                }
                else
                {
                    Toast.makeText(this, "Please allow location permission to use this feature", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (itemId == R.id.bluetooth) {
                intent = new Intent(HomeActivity.this, BluetoothActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.settings) {
                intent = new Intent(HomeActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        // Check if the app has the BLUETOOTH_SCAN permission.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_BLUETOOTH_SCAN_PERMISSION);
            }
        }

        // Get the Bluetooth adapter and check if Bluetooth is supported.
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "This device doesn't support bluetooth", Toast.LENGTH_SHORT).show();
        }

        // Check if Bluetooth is enabled.
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Register the Bluetooth state receiver.
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStateReceiver, filter);

        // Hide the action bar.
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Check if the app has the BLUETOOTH_CONNECT permission.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_CODE);
        }

        // Bind to the BluetoothLeService.
        Intent intent = new Intent(this, BluetoothLeService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);

    }

    /**
     * BroadcastReceiver for Bluetooth state changes.
     * This receiver listens for the ACTION_STATE_CHANGED action for the bluetooth adapter.
     * When the Bluetooth state changes to STATE_OFF, it shows a toast message and sends
     * an intent to request the user to enable Bluetooth.
     */
    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the action from the intent.
            final String action = intent.getAction();
            // Create an intent to request the user to enable Bluetooth.
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            // Check if state of bluetooth adapter has changed.
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                // Get state of bluetooth adapter
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                // if the state is off, then make the user turn it back on.
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Toast.makeText(context, "Bluetooth turned off. Turn back on to use the Peripheral Vision Display Application", Toast.LENGTH_SHORT).show();
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        break;
                }
            }
        }
    };

    /**
     * ServiceConnection for managing the lifecycle of the BluetoothLeService.
     * This is used to receive callbacks stating if the bluetoothLeService is connected or disconnected.
     */
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        /**
         * Called when a connection to the Service has been gained.
         * If bound is true, this means that the BluetoothLeService is connected.
         * If bound is false, this means that the BluetoothLeService is disconnected.
         *
         * @param componentName The component name of the service that has been connected.
         * @param service The IBinder of the Service's communication channel.
         */
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            BluetoothLeService.LocalBinder binder = (BluetoothLeService.LocalBinder) service;
            bluetoothLeService = binder.getService();
            bound = true;
        }

        /**
         * Called when a connection to the Service has been lost.
         *
         * @param componentName The component name of the service that has been disconnected.
         */
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeService = null;
            bound = false;
        }
    };

    /**
     * This method is used to toggle the notification service on and off.
     * It checks if the notification listener permission is granted.
     * If not, it shows a dialog to explain why the app needs the permission and leads to the notification access settings for the user to turn them on manually.
     * If the permission is granted, it checks the current state of the notification service making sure its not already running.
     * Service is started and the notificationServiceButton text is changed to "Stop Notification Service".
     * When the user stops the service, the notificationServiceButton text changes to "Start Notification Service".
     * The current state of the notification service is saved in sharedpref for future reference.
     */
    private void toggleNotificationService()
    {
        // Check if notification listener permission is granted.
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            // Show dialog to explain why the app needs the permission.
            new AlertDialog.Builder(this)
                    .setTitle("Notification Listener Permission")
                    .setMessage("This app requires access to notifications for the notification reader to work.")
                    .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Redirect to notification access settings
                            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

            // If the notification permission is turned off when previously on, the service should be stopped.
            Intent notificationserviceIntent = new Intent(this, NotificationForegroundService.class);
            notificationserviceIntent.setAction(NotificationForegroundService.STOP_ACTION);
            stopService(notificationserviceIntent);
            notificationServiceButton.setText("Start Notification Service");
            toggleNotificationService = false;
        }
        else
        {
            // Start service.
            if (!toggleNotificationService) {

                Intent notificationserviceIntent = new Intent(this, NotificationForegroundService.class);
                notificationserviceIntent.setAction(NotificationForegroundService.START_ACTION);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(notificationserviceIntent);
                } else {
                    startService(notificationserviceIntent);
                }
                notificationServiceButton.setText("Stop Notification Service");
                toggleNotificationService = true;

                SharedPreferences sharedPref = getSharedPreferences("NotificationPreferences", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("isButtonOn", true);
                editor.apply();
            }
            // Stop service.
            else
            {
                Intent notificationserviceIntent = new Intent(this, NotificationForegroundService.class);
                notificationserviceIntent.setAction(NotificationForegroundService.STOP_ACTION);
                stopService(notificationserviceIntent);

                notificationServiceButton.setText("Start Notification Service");
                toggleNotificationService = false;

                SharedPreferences sharedPref = getSharedPreferences("NotificationPreferences", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("isButtonOn", false);
                editor.apply();
            }
        }
    }

    /**
     * This method is used to toggle the location service on and off.
     * It checks if the location permission is granted.
     * If not, it shows a dialog to explain why the app needs the permission and redirects the user to the application settings.
     * If the permission is granted, it checks the current state of the location service making sure its not already running.
     * Service is started and the locationServiceButton text is changed to "Stop Location Service".
     * When the user stops the service, the locationServiceButton text changes to "Start Location Service".
     * The current state of the service is saved in sharedpreferences for future reference.
     */
    private void toggleLocationService()
    {
        Context context = getApplicationContext();
        AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_FINE_LOCATION, Process.myUid(), context.getPackageName());

        // Check if location permission is granted.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || mode != AppOpsManager.MODE_ALLOWED) {
            // Show dialog to explain why the app needs the permission.
            new AlertDialog.Builder(this)
                    .setTitle("Location Permission")
                    .setMessage("This feature requires access to your location. LOCATION PERMISSIONS MUST BE ON 'ALLOW ONLY WHILE USING THE APP' AND MUST USE PRECISE LOCATION.")
                    .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Redirect to application settings.
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

            // If the notification permission is turned off when previously on, it should stop the service.
            Intent locationserviceIntent = new Intent(this, LocationForegroundService.class);
            locationserviceIntent.setAction(LocationForegroundService.STOP_ACTION);
            stopService(locationserviceIntent);
            locationServiceButton.setText("Start Location Service");
            toggleLocationService = false;

        }
        else
        {
            // Start service.
            if (!toggleLocationService) {
                Intent locationserviceIntent = new Intent(this, LocationForegroundService.class);
                locationserviceIntent.setAction(LocationForegroundService.START_ACTION);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(locationserviceIntent);
                } else {
                    startService(locationserviceIntent);
                }
                locationServiceButton.setText("Stop Location Service");
                toggleLocationService = true;

                SharedPreferences sharedPref = getSharedPreferences("LocationPreferences", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("isButtonOn", true);
                editor.apply();

            }
            // Stop service.
            else
            {
                Intent locationserviceIntent = new Intent(this, LocationForegroundService.class);
                locationserviceIntent.setAction(LocationForegroundService.STOP_ACTION);
                stopService(locationserviceIntent);
                locationServiceButton.setText("Start Location Service");
                toggleLocationService = false;

                SharedPreferences sharedPref = getSharedPreferences("LocationPreferences", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("isButtonOn", false);
                editor.apply();
            }
        }
        // Save the state of toggleLocationService in prefs
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREFS_TOGGLE_LOCATION_SERVICE, toggleLocationService);
        editor.apply();
    }

    /**
     * This method is called when the activity is destroyed.
     * It stops the notification and location services.
     * It unregisters the Bluetooth state receiver.
     * It unbinds the BluetoothLeService.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Save state of notification service as off in sharedPref.
        SharedPreferences sharedPref = getSharedPreferences("NotificationPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("isButtonOn", false);
        editor.apply();

        // Save state of location service as off in sharedPref.
        sharedPref = getSharedPreferences("LocationPreferences", Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        editor.putBoolean("isButtonOn", false);
        editor.apply();

        // Stop the location service.
        Intent locationserviceIntent = new Intent(this, LocationForegroundService.class);
        locationserviceIntent.setAction(LocationForegroundService.STOP_ACTION);
        stopService(locationserviceIntent);

        // Unregister the Bluetooth state receiver.
        unregisterReceiver(bluetoothStateReceiver);

        // If BluetoothLeService is bound, unbind it.
        if (bound) {
            unbindService(serviceConnection);
            bound = false;
        }
    }
}