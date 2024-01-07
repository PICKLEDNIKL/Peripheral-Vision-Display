package com.example.peripheralvisiondisplay;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class BluetoothScanner {

    private final BluetoothLeScanner bluetoothLeScanner;
    private boolean scanning;
    private Handler handler = new Handler();
    private static final long SCAN_PERIOD = 10000; // Stops scanning after 10 seconds.
    private List<BluetoothDevice> deviceList;

    public BluetoothScanner(BluetoothAdapter bluetoothAdapter) {
        this.bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        this.deviceList = new ArrayList<>();
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

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            deviceList.add(result.getDevice());
            Log.d("BluetoothScanner", "Device: " + device.getName() + ", Address: " + device.getAddress());

        }
    };
}