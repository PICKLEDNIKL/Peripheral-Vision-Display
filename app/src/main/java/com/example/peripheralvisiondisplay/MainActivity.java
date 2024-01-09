package com.example.peripheralvisiondisplay;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.location.LocationRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.Manifest;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AppOpsManager;
import android.content.Context;
import android.os.Process;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
//TODO: MAKE IT SO THAT WHEN I LOOK FOR THE DEVICE, I DO IT BY NAME AND BY MAC ADDRESS. HOPEFULLY TX ON CIRCUIT PYTHON IS CORRECT?
    // TODO: MIGHT NEED TO ADD ANOTHER LAYOUT FOR BLUETOOTH CONNECTION WHICH WILL INCLUDE SEARCHING FOR THE DEVICE AND THEN CONNECTING TO IT. BUTTONS ARE NEEDED AS THE WAY I AM DOING IT NOW DOESNT ACCOUNT FOR THE DEVICE ALREADY HAVING BLUETOOTH ON AND SO DOESNT SCAN FOR DEVICES UNTIL TURNED OFF AND BACK ON.
    private static final int REQUEST_BLUETOOTH_SCAN_PERMISSION = 1;
    Button notificationServiceButton;
    Button locationServiceButton;
    //    Button stopServiceButton;
    Button switchToMapsActivityButton;
    //    TextView statusText;
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    int locationPermissionCount = 0;
    boolean toggleNotificationService = false;
    boolean toggleLocationService = false;
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;

    private BluetoothLeService bluetoothService;
    private String deviceAddress;
    private boolean bound = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        notificationServiceButton = findViewById(R.id.notificationServiceButton);
        notificationServiceButton.setOnClickListener(view -> toggleNotificationService());

        locationServiceButton = findViewById(R.id.locationServiceButton);
        locationServiceButton.setOnClickListener(view -> toggleLocationService());

        switchToMapsActivityButton = findViewById(R.id.switchToMapsActivityButton);
        switchToMapsActivityButton.setOnClickListener(view -> switchToMapsActivity());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_BLUETOOTH_SCAN_PERMISSION);
            }
        }

        bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "This device doesn't support bluetooth", Toast.LENGTH_SHORT).show();
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStateReceiver, filter);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
        startService(gattServiceIntent);


    }

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Toast.makeText(context, "Bluetooth turned off. Turn back on to use the Peripheral Vision Display Application", Toast.LENGTH_SHORT).show();
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        break;
                    case BluetoothAdapter.STATE_ON:
//                        Toast.makeText(context, "Bluetooth turned on", Toast.LENGTH_SHORT).show();
                        BluetoothScanner bluetoothScanner = new BluetoothScanner(bluetoothAdapter);
                        bluetoothScanner.scanLeDevice();
                        break;
                }
            }
        }
    };

    // Code to manage Service lifecycle.
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothService = ((BluetoothLeService.LocalBinder) service).getService();
            if (bluetoothService != null) {
                if (!bluetoothService.initialize()) {
                    Log.e("serviceconnected", "Unable to initialize Bluetooth");
                    finish();
                }
                bound = true;
                bluetoothService.connect(deviceAddress);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothService = null;
            // TODO: 07/01/2024 check if bounds are necessary and remove from here and from destroy if not.
            bound = false;
        }
    };

    // LISTEN FOR UPDATES IN ACTIVITY
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                // Update your UI here to reflect that the device is connected
                Toast.makeText(context, "device is connected", Toast.LENGTH_SHORT).show();

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                // Update your UI here to reflect that the device is disconnected
                Toast.makeText(context, "device is disconnected", Toast.LENGTH_SHORT).show();

            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        if (bluetoothService != null) {
            final boolean result = bluetoothService.connect(deviceAddress);
            Log.d("MainActivity", "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(gattUpdateReceiver);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        return intentFilter;
    }
















    private void toggleNotificationService()
    {
        // Check if notification listener permission is granted
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            // Show dialog to explain why the app needs the permission
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


            // if the notification permission is turned off when previously on, it should stop the service.
            Intent notificationserviceIntent = new Intent(this, NotificationForegroundService.class);
            notificationserviceIntent.setAction(NotificationForegroundService.STOP_ACTION);
            stopService(notificationserviceIntent);
            notificationServiceButton.setText("Start Notification Service");
            toggleNotificationService = false;
        }
        else
        {
            //start service
            if (!toggleNotificationService) {

                Log.d("functionstarted", "service starts");
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
            //stop service
            else
            {
                Log.d("functionstarted","service stops");
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

    private void toggleLocationService()
    {
        Context context = getApplicationContext();
        AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_FINE_LOCATION, Process.myUid(), context.getPackageName());

        // Check if location permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || mode != AppOpsManager.MODE_ALLOWED) {
            // Show dialog to explain why the app needs the permission
            new AlertDialog.Builder(this)
                    .setTitle("Location Permission")
                    .setMessage("This feature requires access to your location. LOCATION PERMISSIONS MUST BE ON 'ALLOW ONLY WHILE USING THE APP' AND MUST USE PRECISE LOCATION.")
                    .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Redirect to application settings
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

            // if the notification permission is turned off when previously on, it should stop the service.
            Intent locationserviceIntent = new Intent(this, LocationForegroundService.class);
            locationserviceIntent.setAction(LocationForegroundService.STOP_ACTION);
            stopService(locationserviceIntent);
            locationServiceButton.setText("Start Location Service");
            toggleLocationService = false;

        }
        else
        {
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
    }

    private void switchToMapsActivity()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(this, MapsActivity.class);
            startActivity(intent);
        }
        else
        {
            Toast.makeText(this, "Please allow location permission to use this feature", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SharedPreferences sharedPref = getSharedPreferences("NotificationPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("isButtonOn", false);
        editor.apply();

        sharedPref = getSharedPreferences("LocationPreferences", Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        editor.putBoolean("isButtonOn", false);
        editor.apply();

        Intent locationserviceIntent = new Intent(this, LocationForegroundService.class);
        locationserviceIntent.setAction(LocationForegroundService.STOP_ACTION);
        stopService(locationserviceIntent);

        unregisterReceiver(bluetoothStateReceiver);

        if (bound) {
            unbindService(serviceConnection);
            bound = false;
        }
    }
}