package com.example.peripheralvisiondisplay;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.Manifest;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    Button notificationServiceButton;
    Button locationServiceButton;
//    Button stopServiceButton;
    Button switchToMapsActivityButton;
//    TextView statusText;
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    int locationPermissionCount = 0;
    boolean toggleNotificationService = false;
    boolean toggleLocationService = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if notification listener permission is granted
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            // Show dialog to explain why the app needs the permission
            new AlertDialog.Builder(this)
                    .setTitle("Notification Listener Permission")
                    .setMessage("This app requires access to notifications for the notification reader to work.")
                    .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Redirect to notification access settings
                            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }


        notificationServiceButton = findViewById(R.id.notificationServiceButton);
        notificationServiceButton.setOnClickListener(view -> toggleNotificationService());

        locationServiceButton = findViewById(R.id.locationServiceButton);
        locationServiceButton.setOnClickListener(view -> toggleLocationService());

        switchToMapsActivityButton = findViewById(R.id.switchToMapsActivityButton);
        switchToMapsActivityButton.setOnClickListener(view -> switchToMapsActivity());
    }

    private void toggleNotificationService()
    {
        if (!toggleNotificationService) {

            Log.d("functionstarted", "service starts");
            Intent notificationserviceIntent = new Intent(this, NotificationForegroundService.class);
            notificationserviceIntent.setAction(NotificationForegroundService.START_ACTION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(notificationserviceIntent);
            } else {
                startService(notificationserviceIntent);
            }
//            updateTextView("Status: Service started");
            notificationServiceButton.setText("Stop Notification Service");
            toggleNotificationService = true;
        }
        else
        {
            Log.d("functionstarted","service stops");
            Intent notificationserviceIntent = new Intent(this, NotificationForegroundService.class);
            notificationserviceIntent.setAction(NotificationForegroundService.STOP_ACTION);
            stopService(notificationserviceIntent);
//            updateTextView("Status: Service stopped");
            notificationServiceButton.setText("Start Notification Service");
            toggleNotificationService = false;
        }
    }

    private void toggleLocationService()
    {
        if (!toggleLocationService) {
            Intent locationserviceIntent = new Intent(this, LocationForegroundService.class);
            locationserviceIntent.setAction(LocationForegroundService.START_ACTION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(locationserviceIntent);
            } else {
                startService(locationserviceIntent);
            }
            locationServiceButton.setText("Stop Location Service");
            toggleLocationService = true;
        }
        else
        {
            Intent locationserviceIntent = new Intent(this, LocationForegroundService.class);
            locationserviceIntent.setAction(LocationForegroundService.STOP_ACTION);
            stopService(locationserviceIntent);
            locationServiceButton.setText("Start Location Service");
            toggleLocationService = false;
        }
    }

    private void switchToMapsActivity()
    {
        if (locationPermissionCount < 2) {
            // Check if location permission is granted
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
                locationPermissionCount++;
            }
        }else
        {
            // Check if location permission is granted
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Show dialog to explain why the app needs the permission
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission")
                        .setMessage("This app requires access to your location to work properly.")
                        .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Redirect to application settings
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(this, MapsActivity.class);
            startActivity(intent);
        }
    }
}