package com.example.peripheralvisiondisplay;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.Manifest;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

    Button startServiceButton;
    Button stopServiceButton;
    Button switchToMapsActivityButton;
    TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startServiceButton = findViewById(R.id.startServiceButton);
        startServiceButton.setOnClickListener(view -> startNotificationService());

        stopServiceButton = findViewById(R.id.stopServiceButton);
        stopServiceButton.setOnClickListener(view -> stopNotificationService());

        switchToMapsActivityButton = findViewById(R.id.switchToMapsActivityButton);
        switchToMapsActivityButton.setOnClickListener(view -> switchToMapsActivity());
    }

    private void startNotificationService()
    {
        Log.d("functionstarted", "service starts");
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        serviceIntent.setAction(ForegroundService.START_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        }
        else{
            startService(serviceIntent);
        }
        updateTextView("Status: Service started");
//        isMyServiceRunning(ForegroundService.class);
    }

    private void stopNotificationService()
    {
        Log.d("functionstarted","service stops");
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        serviceIntent.setAction(ForegroundService.STOP_ACTION);
        stopService(serviceIntent);
        updateTextView("Status: Service stopped");
    }

//    private boolean isMyServiceRunning(Class<?> serviceClass) {
//        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//            if (serviceClass.getName().equals(service.service.getClassName())) {
//                return true;
//            }
//        }
//        return false;
//    }

    private void updateTextView(String toThis) {
        TextView textView = (TextView) findViewById(R.id.statusText);
        textView.setText(toThis);
    }

    private void switchToMapsActivity()
    {
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }
}