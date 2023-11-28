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

import androidx.annotation.Nullable;

public class PeripheralVisionApplication extends Application {

    public void onCreate() {
        super.onCreate();
        //add global application initialisation code here
        requestpermissions();
//        isNotificationServiceEnabled();
    }

//    public boolean isNotificationServiceEnabled() {
//        String pkgName = getPackageName();
//        final String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
//        if (flat != null) {
//            return flat.contains(pkgName);
//        }
//        return false;
//    }

    private void requestpermissions(){
        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
