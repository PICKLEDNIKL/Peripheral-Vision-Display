package com.example.peripheralvisiondisplay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.List;
import java.util.UUID;

public class BluetoothLeService extends Service {
    // TODO: MAKE SURE TO CHANGE ALL OF THE LOGCAT TO USE THIS AS THE TAG AND MAKE THIS IN OTHER CLASSES
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private String bluetoothDeviceAddress;

    private final IBinder binder = new LocalBinder();

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTED = 2;

    private int connectionState = STATE_DISCONNECTED;

    final String channelID = "bluetoothforegroundchannelid";
    public static final String START_ACTION = "com.example.peripheralvisiondisplay.START_FOREGROUND_SERVICE";
    public static final String STOP_ACTION = "com.example.peripheralvisiondisplay.STOP_FOREGROUND_SERVICE";

    //https://github.com/adafruit/bluetooth-low-energy#tx-characteristic---0x0003
//    public final static UUID SERVICE_UUID = UUID.fromString("ADAF0001-4369-7263-7569-74507974686E");
//    public final static UUID CHARACTERISTIC_UUID = UUID.fromString("ADAF0002-4369-7263-7569-74507974686E");
    public final static UUID SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public final static UUID CHARACTERISTIC_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");

    private int notificationcount = 0;

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // successfully connected to the GATT Server
                connectionState = STATE_CONNECTED;
                broadcastUpdate(ACTION_GATT_CONNECTED);
                Log.i(TAG, "Connected to GATT server.");
                // TODO: DISCOVER SERVICES IF I NEED TO???
                bluetoothGatt.discoverServices();
                printServiceAndCharacteristicUUIDs();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected from the GATT Server
                connectionState = STATE_DISCONNECTED;
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
                Log.i(TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                printServiceAndCharacteristicUUIDs();
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }
    };

    private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.peripheralvisiondisplay.NEW_NOTIFICATION".equals(intent.getAction())) {
                Log.d(TAG, "check if duped notifications");
                String notificationText = intent.getStringExtra("notificationText");
                notificationcount++;
                if (notificationcount == 3)
                    notificationcount = 1;
                //todo:
                sendMessage("NOTIF" + notificationcount);
            }
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        // Register the receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.peripheralvisiondisplay.NEW_NOTIFICATION");
        registerReceiver(notificationReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {

        Notification notification = new NotificationCompat.Builder(this, channelID)
                .setContentTitle("Bluetooth Service")
                .setContentText("Service is running...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
        // Make this service a foreground service
        startForeground(3, notification);

        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(START_ACTION)) {
                startService();
            } else if (intent.getAction().equals(STOP_ACTION)) {
                stopService();
            }
        }
        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "My Service Channel";
            String description = "Channel for My Service";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void startService() {
        // Code to execute when the service is started
        Log.i(TAG, "Service started");
    }

    private void stopService() {
        // Code to execute when the service is stopped
        Log.i(TAG, "Service stopped");
        stopSelf();
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    public boolean initialize() {
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }


    public boolean connect(final String address) {
        if (bluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // previously connected device.  Try to reconnect.
        if (bluetoothDeviceAddress != null && address.equals(bluetoothDeviceAddress)
                && bluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing bluetoothGatt for connection.");
            if (bluetoothGatt.connect()) {
                connectionState = STATE_CONNECTED;
                return true;
            } else {
                return false;
            }
        }

        try {
            final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            // connect to the GATT server on the device
            bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);
            return true;
        } catch (IllegalArgumentException exception) {
            Log.w(TAG, "Device not found with provided address.  Unable to connect.");
            return false;
        }

    }

    public void disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized.");
            return;
        }
        bluetoothGatt.disconnect();
    }



    public void printServiceAndCharacteristicUUIDs() {
        if (bluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return;
        }

        // Get the list of services
//        List<BluetoothGattService> services = bluetoothGatt.getServices();
//        bluetoothGatt.discoverServices();
        List<BluetoothGattService> services = bluetoothGatt.getServices();


        for (BluetoothGattService service : services) {
            Log.d(TAG, "printServiceAndCharacteristicUUIDs: printing services");
            // Print the service UUID
            Log.i(TAG, "Service UUID: " + service.getUuid().toString());

            // Get the list of characteristics for this service
            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();

            for (BluetoothGattCharacteristic characteristic : characteristics) {
                // Print the characteristic UUID
                Log.i(TAG, "Characteristic UUID: " + characteristic.getUuid().toString());
            }
        }
    }

    public void sendMessage(String message) {
        Log.d(TAG, "sendMessage: sending message");
        if (bluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return;
        }

        BluetoothGattService service = bluetoothGatt.getService(SERVICE_UUID);
        if (service == null) {
            Log.w(TAG, "Service not found");
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
        if (characteristic == null) {
            Log.w(TAG, "Characteristic not found");
            return;
        }

        // Check if the characteristic supports write
        if ((characteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) {
            Log.w(TAG, "Characteristic does not support write");
            return;
        }

        // Check if the message is too large
        if (message.getBytes().length > 20) {
            Log.w(TAG, "Message is too large");
            return;
        }

        characteristic.setValue(message);
        if (!bluetoothGatt.writeCharacteristic(characteristic)) {
            Log.w(TAG, "Failed to write characteristic");
        }
        Log.d(TAG, "sendMessage: sent message");
    }


    private void close() {
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // ... existing code ...

        // Unregister the receiver
        unregisterReceiver(notificationReceiver);
    }
}