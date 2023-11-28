package com.example.peripheralvisiondisplay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class ForegroundService extends Service {

    final String channelID = "foregroundchannelid";
    final int notificationID = 1;

//    Intent activityIntent = null;
//    enum State
//    {
//        START, STOP
//    }

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
            }
        }
        return START_NOT_STICKY;
    }
//        //service viewer in logcat
//        new Thread(
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        while (true) {
//                            Log.e("Service", "Service is running...");
//                            try {
//                                Thread.sleep(2000);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//                }
//        ).start();

//        NotificationChannel channel;
//        Notification.Builder notification = null;
//
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//            channel = new NotificationChannel(channelID, channelID, NotificationManager.IMPORTANCE_HIGH);
//
////            getSystemService(NotificationManager.class).createNotificationChannel(channel);
//            notification = new Notification.Builder(this, channelID)
//            .setContentText("Service is running")
//            .setContentTitle("Service enabled")
//            .setSmallIcon(R.drawable.ic_launcher_background)
//            .setOngoing(true)
//            .setCategory(Notification.CATEGORY_NAVIGATION);
//        }
//        startForeground(notificationID, notification.build());
//        //send heavy work to background thread
////        return super.onStartCommand(intent, flags, startID);
//        return START_STICKY;
//    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                channelID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_HIGH
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private void startService() {
        if (!isServiceRunning) {
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelID)
                    .setContentTitle("Foreground Service")
                    .setContentText("Foreground service is running")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setOngoing(true);

            Notification notification = notificationBuilder.build();
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
}




