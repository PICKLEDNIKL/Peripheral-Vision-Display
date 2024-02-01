package com.example.peripheralvisiondisplay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class NotificationForegroundService extends Service {

    final String channelID = "notificationforegroundchannelid";
    final int notificationID = 1;

    public static final String START_ACTION = "com.example.peripheralvisiondisplay.START_FOREGROUND_SERVICE";
    public static final String STOP_ACTION = "com.example.peripheralvisiondisplay.STOP_FOREGROUND_SERVICE";

    private boolean isServiceRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(START_ACTION)) {
                startService();
            } else if (intent.getAction().equals(STOP_ACTION)) {
                stopService();
            } else if (intent.getAction().equals("com.example.peripheralvisiondisplay.NEW_NOTIFICATION")){
                handleNewNotification(intent);
            }
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                channelID,
                "Notification Foreground Service Channel",
                NotificationManager.IMPORTANCE_HIGH
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private void startService() {
        if (!isServiceRunning) {

            Notification notification = new NotificationCompat.Builder(this, channelID)
                    .setContentTitle("Notification Foreground Service")
                    .setContentText("Foreground service is running")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setOngoing(true)
                    .build();

            startForeground(notificationID, notification);
            isServiceRunning = true;
        }
    }

    private void stopService() {
        if (isServiceRunning) {
            stopForeground(true);
            stopSelf();
            isServiceRunning = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceRunning = false;
//        stopForeground(true);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // Stop the service when the app is swiped off from the recent apps list
        stopService();
    }

    private void handleNewNotification(Intent intent) {
        Log.d("ForegroundService", "handlenewnotification works");
        // Retrieve notification data from the intent

        String notificationText = intent.getStringExtra("notificationText");

        // Update the notification content with the latest listened notification text
        Notification notification = new NotificationCompat.Builder(this, channelID)
                        .setContentTitle("Notification Foreground Service")
                        .setContentText(notificationText) // Set the latest notification text here
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setOngoing(true)
                        .build();

//        Notification notification = notificationBuilder.build();
//        notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;

        startForeground(notificationID, notification);
    }


}




