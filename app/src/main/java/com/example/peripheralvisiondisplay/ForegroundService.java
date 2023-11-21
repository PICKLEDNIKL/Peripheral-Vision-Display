package com.example.peripheralvisiondisplay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class ForegroundService extends Service {

    final String channelID = "MyForegroundServiceChannel";
    final int notificationID = 1;

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            Log.e("Service", "Service is running...");
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
        ).start();

        NotificationChannel channel;
        Notification.Builder notification = null;


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel = new NotificationChannel(channelID, channelID, NotificationManager.IMPORTANCE_DEFAULT);

            getSystemService(NotificationManager.class).createNotificationChannel(channel);
            notification = new Notification.Builder(this, channelID);
            notification.setContentText("Service is running");
            notification.setContentTitle("Service enabled");
            notification.setSmallIcon(R.drawable.ic_launcher_background);

        }
        startForeground(notificationID, notification.build());
        return super.onStartCommand(intent, flags, startID);
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
