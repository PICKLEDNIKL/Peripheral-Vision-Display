package com.example.peripheralvisiondisplay;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
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


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener {

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
//    private boolean isFirstLocationUpdate = true;


    //just added
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] lastAccelerometer = new float[3];
    private float[] lastMagnetometer = new float[3];
    private boolean isAccelerometerSet = false;
    private boolean isMagnetometerSet = false;
    private float[] rotationMatrix = new float[9];
    private float[] orientation = new float[3];

    private static final float ALPHA = 0.20f; // if ALPHA = 1 OR 0, no filter applies.


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

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // check location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            LocationRequest locationRequest = createLocationRequest();
//            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
            if (isCameraMoved == false)
            {
                Toast.makeText(this, "Retrieving current location", Toast.LENGTH_SHORT).show();
            }
//            locationCallback.onLocationResult(null);
            mMap.getUiSettings().setAllGesturesEnabled(true);
            mMap.getUiSettings().setMapToolbarEnabled(true);
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);


        } else {
            // Handle location permission denied
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            Toast.makeText(this, "Location permission denied. Please grant location permission to use the map.", Toast.LENGTH_SHORT).show();
            // Disable the map until the permission is granted
            mMap.getUiSettings().setAllGesturesEnabled(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);

        registerReceiver(locationUpdateReceiver, new IntentFilter("LocationUpdates"));
    }

    @Override
    protected void onPause() {
        super.onPause();
//        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        sensorManager.unregisterListener(this);

        unregisterReceiver(locationUpdateReceiver);
    }


    private float[] lowPassFilter( float input[], float output[] ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) {
            lastAccelerometer = lowPassFilter(event.values.clone(), lastAccelerometer);
            isAccelerometerSet = true;
        } else if (event.sensor == magnetometer) {
            lastMagnetometer = lowPassFilter(event.values.clone(), lastMagnetometer);
            isMagnetometerSet = true;
        }

        if (isAccelerometerSet && isMagnetometerSet) {
            SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer);
            SensorManager.getOrientation(rotationMatrix, orientation);
            float azimuthInRadians = orientation[0];
            float azimuthInDegrees = (float)(Math.toDegrees(azimuthInRadians)+360)%360;

                                                                                                //add this back in if you want the map to rotate
//            // Update the camera position to match the device's orientation
//            if (mMap != null) {
//                CameraPosition cameraPosition = new CameraPosition.Builder()
//                        .target(mMap.getCameraPosition().target) // keep the current target
//                        .zoom(mMap.getCameraPosition().zoom) // keep the current zoom
//                        .bearing(azimuthInDegrees) // update the bearing to match the device's orientation
//                        .build();
//                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
//            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle changes in sensor accuracy
    }

    //TODO: NEED TO ADD THIS AFTER REMOVING THE CURRENT IMPLEMENTATION
    private BroadcastReceiver locationUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the latitude and longitude from the Intent
            currentLatitude = intent.getDoubleExtra("Latitude", 0);
            currentLongitude = intent.getDoubleExtra("Longitude", 0);

//            mMap.clear();
            // Update the map's location
            LatLng currentLatLng = new LatLng(currentLatitude, currentLongitude);

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

//    private LocationRequest createLocationRequest() {
//
//        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
//                .setWaitForAccurateLocation(true)
//                .setMinUpdateIntervalMillis(500)
//                .setMaxUpdateDelayMillis(1000)
//                .build();
//
//        return locationRequest;
//    }
//
//    private LocationCallback locationCallback = new LocationCallback() {
//        @Override
//        public void onLocationResult(LocationResult locationResult) {
//            if (locationResult == null) {
//                return;
//            }
//            for (Location location : locationResult.getLocations()) {
//                // Update UI with location data
//                currentLatitude = location.getLatitude();
//                currentLongitude = location.getLongitude();
//            }
//
//            mMap.clear();
//
//            LatLng currentLatLng = new LatLng(currentLatitude, currentLongitude);
////            mMap.addMarker(new MarkerOptions().position(currentLatLng)
////                    .title("Current Location"));
//
//            if (!isCameraMoved) {
//                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
//                isCameraMoved = true;
//            }
//
//            double biasDistance = 0.01; // This is the distance from the center to the edges of the rectangle in degrees. Adjust as needed.
//            LatLng southwest = new LatLng(currentLatitude - biasDistance, currentLongitude - biasDistance);
//            LatLng northeast = new LatLng(currentLatitude + biasDistance, currentLongitude + biasDistance);
//            RectangularBounds bounds = RectangularBounds.newInstance(southwest, northeast);
//            autocompleteFragment.setLocationBias(bounds);
//            Log.i("bounds", "onCreate: " + bounds.toString());
//
//            Log.i("TAG", "onLocationResult: " + currentLatitude + " " + currentLongitude);
//        }
//    };

    private void searchForDestination() {
        if (selectedPlace == null) {
            Toast.makeText(this, "Please select a place first", Toast.LENGTH_SHORT).show();
            return;
        }

//        String destination = destinationEditText.getText().toString();

        // Construct the URL for the Google Maps Directions API
//        Log.i("eas", "searchForDestination: API_KEY = " + apikey);
        String url = "https://maps.googleapis.com/maps/api/directions/json" +
                "?destination=" + selectedPlace.latitude + "," + selectedPlace.longitude +
                "&mode=walking" +
                "&origin=" + currentLatitude + "," + currentLongitude +
                "&key=" + apikey;

        // Execute the AsyncTask to perform the API request
        new DirectionsTask(mMap, this).execute(url);

        Intent dfserviceintent = new Intent(this, DirectionForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(dfserviceintent);
        } else {
            startService(dfserviceintent);
        }
    }
}
