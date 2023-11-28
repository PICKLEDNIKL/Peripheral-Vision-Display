package com.example.peripheralvisiondisplay;

import android.service.notification.NotificationListenerService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class NotificationListener extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // Forward the notification data to your foreground service
        String packageName = sbn.getPackageName();
        //Testing if notification works or not
        Log.i("onnotificationposted","ID :" + sbn.getId() + "\t" + sbn.getNotification().tickerText +"\t" + sbn.getPackageName());
        if (sbn.getNotification().tickerText != null){
            String notificationText = sbn.getNotification().tickerText.toString();
            // Pass the notification data to your foreground service using Intent or other means
            // For example:
            Intent serviceIntent = new Intent(this, ForegroundService.class);
            serviceIntent.setAction("com.example.peripheralvisiondisplay.NEW_NOTIFICATION");
            serviceIntent.putExtra("packageName", packageName);
            serviceIntent.putExtra("notificationText", notificationText);
            startService(serviceIntent);
            Log.d("notificationlistener", "seen notification");
        }
        else{
            //if tickettext is null
            Log.d("notificationlistener", "notification doesnt have tickertext");
        }

    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Handle notification removal if needed
    }
}