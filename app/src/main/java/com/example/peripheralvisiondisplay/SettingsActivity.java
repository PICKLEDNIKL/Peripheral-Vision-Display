package com.example.peripheralvisiondisplay;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.bottomnavigation.BottomNavigationView;


public class SettingsActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    Button saveChangesButton;
    Button resetButton;
    boolean toggleLocationService = false;

    private static final String PREFS_NAME = "HomeActivityPrefs";
    private static final String PREFS_TOGGLE_LOCATION_SERVICE = "toggleLocationService";

    private BluetoothLeService bluetoothLeService;

    Switch switchLedMovement;
    Button buttonNotif;
    Button buttonLeft;
    Button buttonRight;
    Button buttonStraight;
    Button buttonTurn;

    View colourNotif;
    View colourLeft;
    View colourRight;
    View colourStraight;
    View colourTurn;

    int selectedNotifColor;
    int selectedLeftColor;
    int selectedRightColor;
    int selectedStraightColor;
    int selectedTurnColor;

    private Handler handler = new Handler();


    SharedPreferences ledsharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Intent bleintent = new Intent(this, BluetoothLeService.class);
        bindService(bleintent, serviceConnection, BIND_AUTO_CREATE);

        // Get the shared preferences
        ledsharedPref = getSharedPreferences("LedPreferences", Context.MODE_PRIVATE);

        switchLedMovement = findViewById(R.id.switch_led_movement);

        buttonNotif = findViewById(R.id.notifbutton);
        buttonNotif.setOnClickListener(view -> setupColorButton(buttonNotif, colourNotif,"notifColor"));
        int notifColour = ledsharedPref.getInt("notifColor", Color.YELLOW);
        colourNotif = findViewById(R.id.notifcolour);
        colourNotif.setBackgroundColor(notifColour);

        buttonLeft = findViewById(R.id.leftbutton);
        buttonLeft.setOnClickListener(view -> setupColorButton(buttonLeft, colourLeft,"leftColor"));
        int leftColour = ledsharedPref.getInt("leftColor", Color.BLUE);
        colourLeft = findViewById(R.id.leftcolour);
        colourLeft.setBackgroundColor(leftColour);

        buttonRight = findViewById(R.id.rightbutton);
        buttonRight.setOnClickListener(view -> setupColorButton(buttonRight, colourRight,"rightColor"));
        int rightColour = ledsharedPref.getInt("rightColor", Color.BLUE);
        colourRight = findViewById(R.id.rightcolour);
        colourRight.setBackgroundColor(rightColour);

        buttonStraight = findViewById(R.id.straightbutton);
        buttonStraight.setOnClickListener(view -> setupColorButton(buttonStraight, colourStraight, "straightColor"));
        int straightColour = ledsharedPref.getInt("straightColor", Color.GREEN);
        colourStraight = findViewById(R.id.straightcolour);
        colourStraight.setBackgroundColor(straightColour);

        buttonTurn = findViewById(R.id.turnbutton);
        buttonTurn.setOnClickListener(view -> setupColorButton(buttonTurn, colourTurn, "turnColor"));
        int turnColour = ledsharedPref.getInt("turnColor", Color.RED);
        colourTurn = findViewById(R.id.turncolour);
        colourTurn.setBackgroundColor(turnColour);

        // Load the saved colors from shared preferences
        selectedNotifColor = ledsharedPref.getInt("notifColor", Color.YELLOW);
        selectedLeftColor = ledsharedPref.getInt("leftColor", Color.BLUE);
        selectedRightColor = ledsharedPref.getInt("rightColor", Color.BLUE);
        selectedStraightColor = ledsharedPref.getInt("straightColor", Color.GREEN);
        selectedTurnColor = ledsharedPref.getInt("turnColor", Color.RED);


        // Set the initial state of the switch
        boolean ledMovement = ledsharedPref.getBoolean("led_movement", true);
        switchLedMovement.setChecked(ledMovement);


        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean toggleLocationService = prefs.getBoolean(PREFS_TOGGLE_LOCATION_SERVICE, false);

        // Get the saved importance level from shared preferences
        SharedPreferences sharedPref = getSharedPreferences("NotificationPreferences", MODE_PRIVATE);
        String savedImportanceLevel = sharedPref.getString("selectedImportanceLevel", "Medium");

        Spinner spinner = findViewById(R.id.spinnerImportanceLevel);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.importance_levels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Set the Spinner to the saved importance level
        int spinnerPosition = adapter.getPosition(savedImportanceLevel);
        spinner.setSelection(spinnerPosition);

        // Set an OnClickListener for the "Save Changes" button
        saveChangesButton = findViewById(R.id.saveChangesButton);
        saveChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveChangesButton.setEnabled(false);
                // Save the current state of the Spinner to shared preferences
                String selectedImportanceLevel = spinner.getSelectedItem().toString();
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("selectedImportanceLevel", selectedImportanceLevel);
                editor.apply();

                editor = ledsharedPref.edit();

                // Save the selected colors to shared preferences
                editor.putInt("notifColor", selectedNotifColor);
                editor.putInt("leftColor", selectedLeftColor);
                editor.putInt("rightColor", selectedRightColor);
                editor.putInt("straightColor", selectedStraightColor);
                editor.putInt("turnColor", selectedTurnColor);
                editor.putBoolean("led_movement", switchLedMovement.isChecked());
                editor.apply();

                // Send the preferences to the Bluetooth device
                bluetoothLeService.sendSettingPref(ledsharedPref);

                Toast.makeText(SettingsActivity.this, "Changes saved", Toast.LENGTH_SHORT).show();

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        saveChangesButton.setEnabled(true);
                    }
                }, 2500); // 2.5 seconds
            }
        });

        // Find the reset button by its ID
        resetButton = findViewById(R.id.resetButton);
        // Set an OnClickListener for the reset button
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetButton.setEnabled(false);

                // Reset the shared preferences to their default values
                SharedPreferences.Editor editor = ledsharedPref.edit();
                editor.putInt("notifColor", Color.YELLOW);
                editor.putInt("leftColor", Color.BLUE);
                editor.putInt("rightColor", Color.BLUE);
                editor.putInt("straightColor", Color.GREEN);
                editor.putInt("turnColor", Color.RED);
                editor.putBoolean("led_movement", true);
                editor.apply();

                // Update the UI to reflect the changes
                colourNotif.setBackgroundColor(Color.YELLOW);
                colourLeft.setBackgroundColor(Color.BLUE);
                colourRight.setBackgroundColor(Color.BLUE);
                colourStraight.setBackgroundColor(Color.GREEN);
                colourTurn.setBackgroundColor(Color.RED);
                switchLedMovement.setChecked(true);

                // Reset the importance level to its default value
                String defaultImportanceLevel = "Medium"; // Replace with your default value
                int spinnerPosition = adapter.getPosition(defaultImportanceLevel);
                spinner.setSelection(spinnerPosition);

                // Save the default importance level to shared preferences
                editor = sharedPref.edit();
                editor.putString("selectedImportanceLevel", defaultImportanceLevel);
                editor.apply();

                // Send the preferences to the Bluetooth device
                bluetoothLeService.sendSettingPref(ledsharedPref);
                Toast.makeText(SettingsActivity.this, "Settings reset and saved", Toast.LENGTH_SHORT).show();

                // Enable the button after 5 seconds
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        resetButton.setEnabled(true);
                    }
                }, 2500); // 2.5 seconds
            }
        });



        bottomNavigationView = findViewById(R.id.bottom_menu);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Intent intent;
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                intent = new Intent(SettingsActivity.this, HomeActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.map) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    if (toggleLocationService) {
                        intent = new Intent(SettingsActivity.this, MapsActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Please start the location service first", Toast.LENGTH_SHORT).show();
                    }
                }
                else
                {
                    Toast.makeText(this, "Please allow location permission to use this feature", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (itemId == R.id.bluetooth) {
                intent = new Intent(SettingsActivity.this, BluetoothActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.settings) {
                intent = new Intent(SettingsActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BluetoothLeService.LocalBinder binder = (BluetoothLeService.LocalBinder) service;
            bluetoothLeService = binder.getService();

            // Send the preferences to the Bluetooth device
            SharedPreferences ledsharedPref = getSharedPreferences("LedPreferences", Context.MODE_PRIVATE);
            bluetoothLeService.sendSettingPref(ledsharedPref);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bluetoothLeService = null;
        }
    };

    //TODO: MAKE SURE THAT WHEN THE SAVE BUTTON IS PRESSED, IT ALSO SAVES THIS AND IT ALSO SENDS THIS TO THE MICROCONTROLLER WHEN I FIRST CONNECT TO IT. THIS NEEDS TO ALSO INCLUDE MOVEMENT OF LEDS
    private void setupColorButton(Button button, View view, String prefKey) {
        // Create a new dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose a color");

        // Add buttons for each color
        String[] colors = {"Red", "Purple", "Blue", "Green", "Yellow", "Orange"};
        builder.setItems(colors, (dialog, which) -> {
            int selectedColor;
            switch (which) {
                case 0:
                    selectedColor = Color.RED;
                    break;
                case 1:
                    selectedColor = Color.parseColor("#800080"); // Purple
                    break;
                case 2:
                    selectedColor = Color.BLUE;
                    break;
                case 3:
                    selectedColor = Color.GREEN;
                    break;
                case 4:
                    selectedColor = Color.YELLOW;
                    break;
                case 5:
                    selectedColor = Color.parseColor("#FFA500"); // Orange
                    break;
                default:
                    selectedColor = Color.YELLOW;
            }

            // Save the selected color to the corresponding class-level variable
            switch (prefKey) {
                case "notifColor":
                    selectedNotifColor = selectedColor;
                    break;
                case "leftColor":
                    selectedLeftColor = selectedColor;
                    break;
                case "rightColor":
                    selectedRightColor = selectedColor;
                    break;
                case "straightColor":
                    selectedStraightColor = selectedColor;
                    break;
                case "turnColor":
                    selectedTurnColor = selectedColor;
                    break;
            }

//            // Save the selected color in shared preferences
//            SharedPreferences.Editor editor = ledsharedPref.edit();
//            editor.putInt(prefKey, selectedColor);
//            editor.apply();

            // Change the background color of the view
            view.setBackgroundColor(selectedColor);
        });

        // Show the dialog
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ...

        unbindService(serviceConnection);
    }
}