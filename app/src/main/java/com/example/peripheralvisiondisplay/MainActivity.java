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

    Button startServiceButton;
    Button stopServiceButton;
    Button switchToMapsActivityButton;
    TextView statusText;
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    int locationPermissionCount = 0;

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
    }

    private void stopNotificationService()
    {
        Log.d("functionstarted","service stops");
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        serviceIntent.setAction(ForegroundService.STOP_ACTION);
        stopService(serviceIntent);
        updateTextView("Status: Service stopped");
    }

    private void updateTextView(String toThis) {
        TextView textView = (TextView) findViewById(R.id.statusText);
        textView.setText(toThis);
    }

    private void switchToMapsActivity()
    {
        // Check if location permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            locationPermissionCount++;
            if (locationPermissionCount > 2)
            {
                Toast.makeText(this, "Please grant location permission through app settings to use this feature.", Toast.LENGTH_LONG).show();
            }
        }else
        {
            Intent intent = new Intent(this, MapsActivity.class);
            startActivity(intent);
        }
    }
}