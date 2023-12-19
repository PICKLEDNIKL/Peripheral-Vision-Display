package com.example.peripheralvisiondisplay;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.peripheralvisiondisplay.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private static final int REQUEST_LOCATION_PERMISSION = 1001;

    EditText destinationEditText;
    Button searchButton;

    double currentLatitude;
    double currentLongitude;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        destinationEditText = findViewById(R.id.destinationEditText);
        searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(view -> searchForDestination());
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // check location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();

                        Log.i("mapsactivity", currentLatitude + " " + currentLongitude);


                        // marker for current location
                        LatLng currentLatLng = new LatLng(currentLatitude, currentLongitude);
                        mMap.addMarker(new MarkerOptions().position(currentLatLng)
                                .title("Current Location"));

                        // Move the camera to the user's current location
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));


                    } else {
                        // Handle location is null
                        Toast.makeText(this, "Unable to retrieve current location", Toast.LENGTH_SHORT).show();
                    }
                });
        } else {
            // Handle location permission denied
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void searchForDestination() {
        String destination = destinationEditText.getText().toString();

        // Construct the URL for the Google Maps Directions API
        String apikey = BuildConfig.apiKey;
        Log.i("eas", "searchForDestination: API_KEY = " + apikey);
        String url = "https://maps.googleapis.com/maps/api/directions/json" +
//                "?destination=" + destination +
                "?destination=" + "51.411624908447266" + "," + "-0.12337013334035873" +
                "&origin=" + currentLatitude + "," + currentLongitude +
                "&key=" + apikey;

        // Execute the AsyncTask to perform the API request
        new DirectionsTask(mMap).execute(url);
    }
}
