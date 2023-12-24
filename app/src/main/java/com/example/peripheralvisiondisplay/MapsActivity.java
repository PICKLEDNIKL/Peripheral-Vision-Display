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


import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.example.peripheralvisiondisplay.databinding.ActivityMapsBinding;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.util.Arrays;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private static final int REQUEST_LOCATION_PERMISSION = 1001;

    EditText destinationEditText;
    Button searchButton;

    double currentLatitude;
    double currentLongitude;

    private LatLng selectedPlace;
    private String apikey = BuildConfig.apiKey;

    private AutocompleteSupportFragment autocompleteFragment;

    private boolean isCameraMoved = false;

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

//        destinationEditText = findViewById(R.id.destinationEditText);
        searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(view -> searchForDestination());

        // Initialize the SDK
        Places.initialize(getApplicationContext(), apikey);

        // Create a new Places client instance
        PlacesClient placesClient = Places.createClient(this);

        // Initialize the AutocompleteSupportFragment
        autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);


        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        autocompleteFragment.setCountry("UK");

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // Handle the selected Place
                selectedPlace = place.getLatLng();
                Log.i("onplaceselect", "Place: " + place.getName() + ", " + place.getId());
            }

            @Override
            public void onError(Status status) {
                // Handle the error
                Log.i("onerror", "An error occurred: " + status);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // check location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationRequest locationRequest = createLocationRequest();
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
            if (isCameraMoved == false)
            {
                Toast.makeText(this, "Retrieving current location", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Handle location permission denied
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }


    private LocationRequest createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();

        locationRequest.setInterval(6000); // Set the desired interval for active location updates, in milliseconds.
        locationRequest.setFastestInterval(3000); // Set the fastest rate for active location updates, in milliseconds.
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // Set the priority of the request.
        return locationRequest;
    }

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }
            for (Location location : locationResult.getLocations()) {
                // Update UI with location data
                currentLatitude = location.getLatitude();
                currentLongitude = location.getLongitude();
            }

            mMap.clear();

            LatLng currentLatLng = new LatLng(currentLatitude, currentLongitude);
            mMap.addMarker(new MarkerOptions().position(currentLatLng)
                    .title("Current Location"));

            if (!isCameraMoved) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                isCameraMoved = true;
            }

            double biasDistance = 0.01; // This is the distance from the center to the edges of the rectangle in degrees. Adjust as needed.
            LatLng southwest = new LatLng(currentLatitude - biasDistance, currentLongitude - biasDistance);
            LatLng northeast = new LatLng(currentLatitude + biasDistance, currentLongitude + biasDistance);
            RectangularBounds bounds = RectangularBounds.newInstance(southwest, northeast);
            autocompleteFragment.setLocationBias(bounds);
            Log.i("bounds", "onCreate: " + bounds.toString());

            Log.i("TAG", "onLocationResult: " + currentLatitude + " " + currentLongitude);
        }
    };

    private void searchForDestination() {
        if (selectedPlace == null) {
            Toast.makeText(this, "Please select a place first", Toast.LENGTH_SHORT).show();
            return;
        }

//        String destination = destinationEditText.getText().toString();

        // Construct the URL for the Google Maps Directions API
        Log.i("eas", "searchForDestination: API_KEY = " + apikey);
        String url = "https://maps.googleapis.com/maps/api/directions/json" +
                "?destination=" + selectedPlace.latitude + "," + selectedPlace.longitude +
                "&mode=walking" +
                "&origin=" + currentLatitude + "," + currentLongitude +
                "&key=" + apikey;

        // Execute the AsyncTask to perform the API request
        new DirectionsTask(mMap).execute(url);
    }
}
