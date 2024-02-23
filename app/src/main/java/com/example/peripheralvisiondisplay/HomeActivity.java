package com.example.peripheralvisiondisplay;

import static com.example.peripheralvisiondisplay.BluetoothActivity.REQUEST_CODE;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
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

public class HomeActivity extends AppCompatActivity {
    Button notificationServiceButton;
    Button locationServiceButton;
    BottomNavigationView bottomNavigationView;
    boolean toggleNotificationService = false;
    boolean toggleLocationService = false;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;

    private BluetoothLeService bluetoothService;
    private String deviceAddress = "D7:0B:99:6B:B6:D7";
    private boolean bound = false;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_SCAN_PERMISSION = 1;
    private static final String PREFS_NAME = "HomeActivityPrefs";
    private static final String PREFS_TOGGLE_LOCATION_SERVICE = "toggleLocationService";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        SharedPreferences sharedPref = getSharedPreferences("LedPreferences", Context.MODE_PRIVATE);
        boolean isFirstLaunch = sharedPref.getBoolean("isFirstLaunch", true);
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

        notificationServiceButton = findViewById(R.id.notificationServiceButton);
        notificationServiceButton.setOnClickListener(view -> toggleNotificationService());

        locationServiceButton = findViewById(R.id.locationServiceButton);
        locationServiceButton.setOnClickListener(view -> toggleLocationService());

        bottomNavigationView = findViewById(R.id.bottom_menu);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Intent intent;
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                intent = new Intent(HomeActivity.this, HomeActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.map) {
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



        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        if (!isServiceRunning(BluetoothLeService.class)) {
            Intent serviceIntent = new Intent(this, BluetoothLeService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
                Log.d("homeactivity", "onCreate: startforegroundservice for ble");
            } else {
                startService(serviceIntent);
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_CODE);
        }

    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
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
                //TODO: MAYBE MAKE IT SO IT STARTS THE SERVICE HERE OF SOMETHING TO MAKE IT WORK FIRST TIME.
                if (!bluetoothService.initialize()) {
                    Log.e("serviceconnected", "Unable to initialize Bluetooth");
                    finish();
                }
                bound = true;
//                bluetoothService.connect(deviceAddress);
                Log.e("serviceconnected", "conencted to bluetooth service");
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
            Log.d("HomeActivity", "Connect request result=" + result);
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
        // Save the state of toggleLocationService in SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREFS_TOGGLE_LOCATION_SERVICE, toggleLocationService);
        editor.apply();
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