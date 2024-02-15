package com.example.peripheralvisiondisplay;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    Button saveChangesButton;
    boolean toggleLocationService = false;

    private static final String PREFS_NAME = "HomeActivityPrefs";
    private static final String PREFS_TOGGLE_LOCATION_SERVICE = "toggleLocationService";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean toggleLocationService = prefs.getBoolean(PREFS_TOGGLE_LOCATION_SERVICE, false);

        // Get the saved importance level from shared preferences
        SharedPreferences sharedPref = getSharedPreferences("NotificationPreferences", MODE_PRIVATE);
        String savedImportanceLevel = sharedPref.getString("selectedImportanceLevel", "High");

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
                // Save the current state of the Spinner to shared preferences
                String selectedImportanceLevel = spinner.getSelectedItem().toString();
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("selectedImportanceLevel", selectedImportanceLevel);
                editor.apply();

                Toast.makeText(SettingsActivity.this, "Changes saved", Toast.LENGTH_SHORT).show();
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
}