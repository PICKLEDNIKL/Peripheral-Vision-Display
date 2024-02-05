package com.example.peripheralvisiondisplay;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class CalibrationActivity extends AppCompatActivity implements SensorEventListener {


    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] lastAccelerometer = new float[3];
    private float[] lastMagnetometer = new float[3];
    private boolean isAccelerometerSet = false;
    private boolean isMagnetometerSet = false;
    private float[] rotationMatrix = new float[9];
    private float[] orientation = new float[3];
    private float azimuthInDegrees = 0;
    private List<Float> readings = new ArrayList<>();
    private Vibrator vibrator;
    private MediaPlayer mediaPlayer;
    private TextView directionTextView;
    private TextView statusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mediaPlayer = MediaPlayer.create(this, R.raw.sound); // replace with your sound file
        directionTextView = findViewById(R.id.currentOrientation); // replace with your TextView id
        statusTextView = findViewById(R.id.status);

        // Start the sensor listeners here
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);


        Button calibrateButton = findViewById(R.id.calibrateButton);
        calibrateButton.setOnClickListener(view -> startCalibration());
    }

    private void startCalibration() {

        new CountDownTimer(10000, 1000) {

            public void onTick(long millisUntilFinished) {
                statusTextView.setText("Calibration status: Started\nTime remaining: " + millisUntilFinished / 1000 + " seconds");
            }

            public void onFinish() {
                sensorManager.unregisterListener(CalibrationActivity.this);
                float averageReading = 0;
                for (float reading : readings) {
                    averageReading += reading;
                }
                averageReading /= readings.size();
                SharedPreferences sharedPref = getSharedPreferences("MyApp", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putFloat("north", averageReading);
                editor.apply();

                statusTextView.setText("Calibration status: Completed");

                vibrator.vibrate(500);
                mediaPlayer.start();
            }
        }.start();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) {
            System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.length);
            isAccelerometerSet = true;
        } else if (event.sensor == magnetometer) {
            System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.length);
            isMagnetometerSet = true;
        }

        if (isAccelerometerSet && isMagnetometerSet) {
            SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer);
            SensorManager.getOrientation(rotationMatrix, orientation);
            float azimuthInRadians = orientation[0];
            azimuthInDegrees = (float)(Math.toDegrees(azimuthInRadians)+360)%360;
            readings.add(azimuthInDegrees);

            // Calculate the bearing
            float floatBearing = (azimuthInDegrees + 360) % 360;
            int bearing = Math.round(floatBearing);

            // Update the direction TextView
            int color;
            String orientationString = getDirectionFromDegrees(azimuthInDegrees);
            if (orientationString.equals("True North")) {
                color = Color.GREEN;
            } else {
                color = Color.RED;
            }
            directionTextView.setText("Current Orientation: " + orientationString + "\nBearing: " + bearing);
            directionTextView.setTextColor(color);
        }
    }

    private String getDirectionFromDegrees(float degrees) {
        if (degrees >= 355 || degrees < 5) {
            return "True North";
        } else if (degrees >= 345 || degrees < 15) {
            return "North";
        } else if (degrees >= 15 && degrees < 75) {
            return "North East";
        } else if (degrees >= 75 && degrees < 105) {
            return "East";
        } else if (degrees >= 105 && degrees < 165) {
            return "South East";
        } else if (degrees >= 165 && degrees < 195) {
            return "South";
        } else if (degrees >= 195 && degrees < 255) {
            return "South West";
        } else if (degrees >= 255 && degrees < 285) {
            return "West";
        } else if (degrees >= 285 && degrees < 345) {
            return "North West";
        }
        return "Unknown";
    }

//            // Update the direction TextView
//            if (azimuthInDegrees >= 345 || azimuthInDegrees < 15) {
//                directionTextView.setText("Current Orientation: North");
//            } else if (azimuthInDegrees >= 15 && azimuthInDegrees < 75) {
//                directionTextView.setText("Current Orientation: North East");
//            } else if (azimuthInDegrees >= 75 && azimuthInDegrees < 105) {
//                directionTextView.setText("Current Orientation: East");
//            } else if (azimuthInDegrees >= 105 && azimuthInDegrees < 165) {
//                directionTextView.setText("Current Orientation: South East");
//            } else if (azimuthInDegrees >= 165 && azimuthInDegrees < 195) {
//                directionTextView.setText("Current Orientation: South");
//            } else if (azimuthInDegrees >= 195 && azimuthInDegrees < 255) {
//                directionTextView.setText("Current Orientation: South West");
//            } else if (azimuthInDegrees >= 255 && azimuthInDegrees < 285) {
//                directionTextView.setText("Current Orientation: West");
//            } else if (azimuthInDegrees >= 285 && azimuthInDegrees < 345) {
//                directionTextView.setText("Current Orientation: North West");
//            }
//        }
//    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle changes in sensor accuracy
    }

}