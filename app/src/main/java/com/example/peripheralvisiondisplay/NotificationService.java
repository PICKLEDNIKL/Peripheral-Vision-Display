//package com.example.peripheralvisiondisplay;
//
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.Service;
//import android.content.Context;
//import android.content.Intent;
//import android.os.Build;
//import android.os.IBinder;
//import android.util.Log;
//
//import androidx.core.app.NotificationCompat;
//
//public class NotificationService extends Service {
//    private final String channelId = "MyNotificationChannel";
//    private final int notificationId = 1;
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        Log.d("NotificationService", "service started");
//        createNotificationChannel();
//        showNotification();
//    }
//
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }
//
//    private void createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(
//                    channelId,
//                    "My Notification Channel",
//                    NotificationManager.IMPORTANCE_DEFAULT
//            );
//            NotificationManager notificationManager =
//                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//            notificationManager.createNotificationChannel(channel);
//        }
//    }
//
//    private void showNotification() {
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
//                .setContentTitle("Notification Title")
//                .setContentText("Notification Content")
//                .setSmallIcon(android.R.drawable.ic_dialog_info);
//
//        NotificationManager notificationManager =
//                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        notificationManager.notify(notificationId, builder.build());
//    }
//}
//
