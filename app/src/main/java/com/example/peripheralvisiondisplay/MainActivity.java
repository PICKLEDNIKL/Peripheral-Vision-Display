package com.example.peripheralvisiondisplay;

// MainActivity.java
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button startServiceButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        System.out.println("onCreate");

        startServiceButton = findViewById(R.id.startServiceButton);
        startServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startNotificationService();
            }
        });
    }

    private void startNotificationService() {
        Log.d("functionstarted", "service starts");
//        Intent serviceIntent = new Intent(this, NotificationService.class);
        Intent serviceIntent = new Intent(this, ForegroundService.class);
//        serviceIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
//        PendingIntent p = PendingIntent.getBroadcast(this,1,new Intent(this, ForegroundService.class), PendingIntent.FLAG_IMMUTABLE);
        startService(serviceIntent);
        Log.d("servicestarted", "service works");
    }
}