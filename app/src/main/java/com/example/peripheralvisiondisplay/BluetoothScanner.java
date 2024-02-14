package com.example.peripheralvisiondisplay;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.common.internal.constants.ListAppsActivityContract;

import java.util.ArrayList;
import java.util.List;

public class BluetoothScanner extends ListActivity {
    private final static String TAG = BluetoothScanner.class.getSimpleName();

    private final BluetoothLeScanner bluetoothLeScanner;

//    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean scanning;
    private Handler handler = new Handler();

    private static final long SCAN_PERIOD = 10000; // Stops scanning after 10 seconds.
    private static final int REQUEST_ENABLE_BT = 1;
    private static final String DEVICE_NAME = "CIRCUITPYb6d7";
    private static final String DEVICE_ADDRESS = "D7:0B:99:6B:B6:D7";

//    private List<BluetoothDevice> deviceList;

//    private BluetoothGatt bluetoothGatt;
//    private BluetoothGattCallback gattCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    public BluetoothScanner(BluetoothAdapter bluetoothAdapter) {
        this.bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
//        this.deviceList = new ArrayList<>();
//        this.gattCallback = gattCallback;
    }

    public void scanLeDevice() {
        if (!scanning) {
            // Stops scanning after a predefined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    bluetoothLeScanner.stopScan(leScanCallback);
                }
            }, SCAN_PERIOD);

            scanning = true;
            bluetoothLeScanner.startScan(leScanCallback);
        } else {
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

//    private ScanCallback leScanCallback = new ScanCallback() {
//        @Override
//        public void onScanResult(int callbackType, ScanResult result) {
//            super.onScanResult(callbackType, result);
//            BluetoothDevice device = result.getDevice();
//            deviceList.add(result.getDevice());
//            Log.d("BluetoothScanner", "Device: " + device.getName() + ", Address: " + device.getAddress());
//
//        }
//    };

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            if (DEVICE_NAME.equals(device.getName()) && DEVICE_ADDRESS.equals(device.getAddress())) {
                Log.d(TAG, "Found the target device, stopping scan.");
                Log.d(TAG, "Device: " + device.getName() + ", Address: " + device.getAddress());
//                bluetoothLeScanner.stopScan(leScanCallback);
//                scanning = false;
//                connectToDevice(device); // Connect to the device
            }
            else {
                Log.d(TAG, "not found target device");
            }
        }
    };

//    public void connectToDevice(BluetoothDevice device) {
//        bluetoothGatt = device.connectGatt(null, false, gattCallback);
//    }
}