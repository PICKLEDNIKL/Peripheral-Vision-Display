package com.example.peripheralvisiondisplay;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.HashSet;
import java.util.Set;

public class BluetoothActivity extends Activity {
    static final int REQUEST_CODE = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> mArrayAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private Set<BluetoothDevice> mDeviceSet = new HashSet<>();

    Button refreshButton;

    private BluetoothLeService bluetoothLeService;
    BottomNavigationView bottomNavigationView;
    private boolean bound = false;

    private static final String PREFS_NAME = "HomeActivityPrefs";
    private static final String PREFS_TOGGLE_LOCATION_SERVICE = "toggleLocationService";

    private TextView connectedDeviceTextView;
    private Button disconnectButton;

    // Manage service lifecycle.
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @SuppressLint("MissingPermission")
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            BluetoothLeService.LocalBinder binder = (BluetoothLeService.LocalBinder) service;
            bluetoothLeService = binder.getService();
            bound = true;

            // If a device is connected, update the TextView.
            if (bound) {
                BluetoothDevice device = bluetoothLeService.getConnectedDevice();
                if (device != null) {
                    connectedDeviceTextView.setText("Connected Device: " + device.getName() + "\n" + device.getAddress());
                } else {
                    connectedDeviceTextView.setText("No device connected");
                }
            }
        }

        // Handle unexpected disconnection from service.
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeService = null;
            bound = false;
        }
    };

    // Broadcast receiver for device discovery.
    private final BroadcastReceiver deviceDiscoveryReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                @SuppressLint("MissingPermission") String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                if (deviceName != null && !mDeviceSet.contains(device)) {
                    mDeviceSet.add(device);
                    mArrayAdapter.add(deviceName + "\n" + deviceHardwareAddress);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        // Check if the permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted, start BluetoothActivity
            Intent intent = new Intent(this, BluetoothActivity.class);
            startActivity(intent);
        } else {
            // Permission is not granted, request the permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, REQUEST_CODE);
        }

        refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(view -> refreshBluetoothDevices(view));

        connectedDeviceTextView = findViewById(R.id.connected_device);

        disconnectButton = findViewById(R.id.disconnect_button);
        // Disconnect from connected device if user presses disconnect button
        disconnectButton.setOnClickListener(view -> {
            if (bound) {
                bluetoothLeService.disconnect();
                connectedDeviceTextView.setText("No device connected");
            }
        });

        ListView listView = findViewById(R.id.bluetooth_devices);

        // Retrieve the state of toggleLocationService from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean toggleLocationService = prefs.getBoolean(PREFS_TOGGLE_LOCATION_SERVICE, false);

        // Handle bottom navigation menu
        bottomNavigationView = findViewById(R.id.bottom_menu);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Intent intent;
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                intent = new Intent(BluetoothActivity.this, HomeActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.map) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    if (toggleLocationService) {
                        intent = new Intent(BluetoothActivity.this, MapsActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Please start the location service first", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Please allow location permission to use this feature", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (itemId == R.id.bluetooth) {
                intent = new Intent(BluetoothActivity.this, BluetoothActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.settings) {
                intent = new Intent(BluetoothActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the device contains the substring "CIRCUITPY", set background colour to light green.
        mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                String deviceName = getItem(position);
                if (deviceName != null && deviceName.contains("CIRCUITPY")) {
                    view.setBackgroundColor(Color.parseColor("#CCFFCC"));
                } else {
                    view.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                }

                return view;
            }
        };
        listView.setAdapter(mArrayAdapter);

        mPairedDevices = mBluetoothAdapter.getBondedDevices();
        if (mPairedDevices.size() > 0) {
            for (BluetoothDevice device : mPairedDevices) {
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }

        // Connect to the device the user clicks on
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);
                if (bound) {
                    bluetoothLeService.connect(address);
                    BluetoothDevice device = bluetoothLeService.getConnectedDevice();
                    Toast.makeText(BluetoothActivity.this, "Connecting to " + info, Toast.LENGTH_SHORT).show();
                    connectedDeviceTextView.setText("Connected Device: " + device.getName() + "\n" + device.getAddress());
                }
            }
        });


        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(deviceDiscoveryReceiver, filter);

        // Start discovery
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();

        // Start discovery.
        startDiscovery();

        // Start foreground service for Bluetooth connection if not already running.
        if (!isServiceRunning(BluetoothLeService.class)) {
            Intent serviceIntent = new Intent(this, BluetoothLeService.class);
            startForegroundService(serviceIntent);
        }
    }

    // Check if the service is running.
    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to the service.
        Intent intent = new Intent(this, BluetoothLeService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // If permission is granted, start BluetoothActivity
                Intent intent = new Intent(this, BluetoothActivity.class);
                startActivity(intent);
            } else {
                // Permission denied. Disable the functionality that depends on this permission.
                Toast.makeText(this, "Permission denied to connect to Bluetooth devices", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startDiscovery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
    }

    public void refreshBluetoothDevices(View view) {
        // Cancel the current discovery process if it's already running.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        // Clear the list of Bluetooth devices
        mDeviceSet.clear();
        mArrayAdapter.clear();

        // Start a new discovery process after a small delay
        new Handler().postDelayed(new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                mBluetoothAdapter.startDiscovery();
            }
        }, 500); // delay for half a second to allow the previous discovery process to cancel
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the broadcast receiver and unbind from the service.
        if (bound) {
            unbindService(serviceConnection);
            unregisterReceiver(deviceDiscoveryReceiver);
            bound = false;
        }
    }
}