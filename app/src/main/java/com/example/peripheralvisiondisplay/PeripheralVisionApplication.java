package com.example.peripheralvisiondisplay;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.provider.Settings;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;


import androidx.annotation.Nullable;

public class PeripheralVisionApplication extends Application {

    public void onCreate() {
        super.onCreate();
        //add global application initialisation code here
        requestpermissions();
//        isNotificationServiceEnabled();
    }

    private void requestpermissions(){
        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

//    // Check if the user granted permission to access notifications
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
//            if (isNotificationServiceEnabled()) {
//                // Permission has been granted
//                // Perform necessary operations here
//            } else {
//                // Permission denied by the user
//                // Handle accordingly or request permission again
//            }
//        }
//    }

//    // Check if the notification listener service is enabled
//    public boolean isNotificationServiceEnabled() {
//        String pkgName = getPackageName();
//        final String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
//        return flat != null && flat.contains(pkgName);
//    }
}
