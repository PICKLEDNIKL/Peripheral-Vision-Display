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
import android.widget.Toast;

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
    private TextView accuracyTextView;

    private boolean sensorbool = true;

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
        accuracyTextView = findViewById(R.id.accuracy);

        // Start the sensor listeners here
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);


        Button calibrateButton = findViewById(R.id.calibrateButton);
        calibrateButton.setOnClickListener(view -> startCalibration());

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    private void startCalibration() {

        new CountDownTimer(10000, 1000) {

            public void onTick(long millisUntilFinished) {
                statusTextView.setText("Calibration status: Started\nTime remaining: " + millisUntilFinished / 1000 + " seconds");
            }

            public void onFinish() {
                sensorManager.unregisterListener(CalibrationActivity.this);
//                float averageReading = 0;
//                for (float reading : readings) {
//                    averageReading += reading;
//                }
//                averageReading /= readings.size();
                SharedPreferences sharedPref = getSharedPreferences("MyApp", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putFloat("north", azimuthInDegrees);
                editor.apply();

                statusTextView.setText("Calibration status: Completed");

                vibrator.vibrate(500);
                mediaPlayer.start();
            }
        }.start();
    }

    // sensorbool is used to half the number of sensor changes that are recognised. this should make it easier for the user to calibrate the device
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (sensorbool) {
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
                azimuthInDegrees = (float) (Math.toDegrees(azimuthInRadians) + 360) % 360;
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
        sensorbool = !sensorbool;
        // Store the azimuthInDegrees value
        SharedPreferences sharedPref = getSharedPreferences("MyApp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat("azimuthInDegrees", azimuthInDegrees);
        editor.apply();
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

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle changes in sensor accuracy
        switch (accuracy) {
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                accuracyTextView.setText("Sensor Accuracy: High");
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                accuracyTextView.setText("Sensor Accuracy: Medium");
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                accuracyTextView.setText("Sensor Accuracy: Low");
                break;
            case SensorManager.SENSOR_STATUS_UNRELIABLE:
                accuracyTextView.setText("Sensor Accuracy: Unreliable");
                break;
        }
    }
}