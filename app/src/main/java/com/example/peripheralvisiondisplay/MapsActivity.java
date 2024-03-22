package com.example.peripheralvisiondisplay;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.Manifest;
import android.location.Location;
import android.widget.Toast;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.peripheralvisiondisplay.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.maps.android.PolyUtil;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * This class is used to display the map and user's location.
 * It also allows the user to search for a destination and get directions to it.
 */
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private FusedLocationProviderClient fusedLocationProviderClient;
    Button searchButton;
    Button clearButton;
    double currentLatitude;
    double currentLongitude;
    public static LatLng selectedPlace;
    private String apikey = BuildConfig.apiKey;
    private AutocompleteSupportFragment autocompleteFragment;
    private boolean isCameraMoved = false;
    private static final String PREFS_NAME = "HomeActivityPrefs";
    private static final String PREFS_TOGGLE_LOCATION_SERVICE = "toggleLocationService";
    private Queue<Location> locationQueue = new LinkedList<>();
    private Handler handler = new Handler();
    private Runnable runnable;
    private boolean isRunnableRunning = false;
    BottomNavigationView bottomNavigationView;
    private int stepcount = 0;

    /**
     * This method is called when the activity is created.
     * It retrieves the state of the location service from sharedPref.
     * It initializes the map and AutocompleteSupportFragment.
     * It sets listeners for the search and clear buttons.
     * @param savedInstanceState The saved state of the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve the state of toggleLocationService from SharedPreferences to see if the location service is running.
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean toggleLocationService = prefs.getBoolean(PREFS_TOGGLE_LOCATION_SERVICE, false);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Set up the search button and handle when clicked.
        searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(view -> searchForDestination());

        // Set up the clear button and handle when clicked.
        clearButton = findViewById(R.id.clearButton);

        // Clear the map and stop the DirectionForegroundService.
        clearButton.setOnClickListener(view -> {
            clearMap();

            // Create an Intent to stop the DirectionForegroundService
            Intent serviceIntent = new Intent(MapsActivity.this, DirectionForegroundService.class);
            stopService(serviceIntent);
        });

        // Set up the bottom navigation view and handle when an item is selected.
        bottomNavigationView = findViewById(R.id.bottom_menu);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Intent intent;
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                intent = new Intent(MapsActivity.this, HomeActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.map) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    if (toggleLocationService) {
                        intent = new Intent(MapsActivity.this, MapsActivity.class);
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
                intent = new Intent(MapsActivity.this, BluetoothActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.settings) {
                intent = new Intent(MapsActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        // Initialize the Places SDK with the API key
        Places.initialize(getApplicationContext(), apikey);

        // Initialize the AutocompleteSupportFragment
        autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);


        // Set the fields to specify which type of place data to return after the user selects a place
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        autocompleteFragment.setCountry("UK");

        // Use PlaceSelectionListener to handle the response when a user selects a place in the autocomplete widget.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                selectedPlace = place.getLatLng();
            }

            @Override
            public void onError(Status status) {
            }
        });
    }

    /**
     * This method is called when the map is ready to be used.
     * It checks the location permissions and sets up the map.
     * It adds the saved state of the polyline and marker to the map.
     *
     * @param googleMap The Google map object.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Check if the app has location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // If the map camera hasn't moved yet, show a message to the user.
            if (isCameraMoved == false)
            {
                Toast.makeText(this, "Retrieving current location", Toast.LENGTH_SHORT).show();
            }

            // Enable the map settings.
            mMap.getUiSettings().setAllGesturesEnabled(true);
            mMap.getUiSettings().setMapToolbarEnabled(true);
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        }
        // Handle when location permissions are denied
        else {
            Toast.makeText(this, "Location permission denied. Please grant location permission to use the map.", Toast.LENGTH_SHORT).show();
            // Disable the map until the permission is granted
            mMap.getUiSettings().setAllGesturesEnabled(false);
        }

        // Retrieve the polyline and marker data from SharedPreferences
        SharedPreferences sharedPref = getSharedPreferences("PolylineData", Context.MODE_PRIVATE);
        String polyline = sharedPref.getString("polyline", "");
        String marker = sharedPref.getString("marker", "");

        // Check if there is marker data and transform it into a LatLng object
        if (!marker.isEmpty()) {
            // Remove the "[" and "]" from the marker string
            marker = marker.replace("[", "").replace("]", "");
            String[] markerArray = marker.split(", ");

            // Remove "lat/lng: (" and ")" from the markerString
            String markerString = markerArray[markerArray.length - 1];
            markerString = markerString.replace("lat/lng: (", "").replace(")", "");

            // Split the marker string into latitude and longitude
            String[] latLng = markerString.split(",");
            double lat = Double.parseDouble(latLng[0]);
            double lng = Double.parseDouble(latLng[1]);
            LatLng markerLocation = new LatLng(lat, lng);

            // Add a marker to the saved navigation destination.
            mMap.addMarker(new MarkerOptions().position(markerLocation).title("Destination"));
        }

        // Check if there is polyline data
        if (!polyline.isEmpty()) {
            // Split the polyline string into an array of polylines
            String[] polylineArray = polyline.split(";");

            // Iterate over the array and decode each polyline into LatLng points and draw it on the map
            for (String polylineString : polylineArray) {
                List<LatLng> decodedPolyline = PolyUtil.decode(polylineString);
                PolylineOptions polylineOptions = new PolylineOptions();
                polylineOptions.addAll(decodedPolyline);
                mMap.addPolyline(polylineOptions);
            }
        }
    }

    /**
     * This method is called when the activity is resumed.
     * It registers the locationUpdateReceiver to get location updates.
     */
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(locationUpdateReceiver, new IntentFilter("LocationUpdates"));
    }

    /**
     * This method is called when the activity is paused.
     * It unregisters the locationUpdateReceiver to stop location updates.
     */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(locationUpdateReceiver);
    }


    /**
     * This method is called when the activity is destroyed.
     * It stops the DirectionForegroundService.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * This BroadcastReceiver is used to receive location updates from the LocationForegroundService.
     * It updates the map's camera to the user's location and adds a marker on the map to show the users location updates.
     */
    private BroadcastReceiver locationUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // Get the latitude and longitude from the Intent
            currentLatitude = intent.getDoubleExtra("Latitude", 0);
            currentLongitude = intent.getDoubleExtra("Longitude", 0);

            // Update the map's location
            LatLng currentLatLng = new LatLng(currentLatitude, currentLongitude);

            // Move the camera to the current location when the first location is received.
            if (!isCameraMoved) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                isCameraMoved = true;
            }

            // Set the bounds for the autocomplete fragment based on the current location
            double biasDistance = 0.01;
            LatLng southwest = new LatLng(currentLatitude - biasDistance, currentLongitude - biasDistance);
            LatLng northeast = new LatLng(currentLatitude + biasDistance, currentLongitude + biasDistance);
            RectangularBounds bounds = RectangularBounds.newInstance(southwest, northeast);
            autocompleteFragment.setLocationBias(bounds);

            // TODO: COMMENT OUT
            // This is used to show the user's location updates on the map for user testing purposes.
            float movementThreshold = 5; // 5 meters
            Location currentLocation = new Location("");
            currentLocation.setLatitude(currentLatitude);
            currentLocation.setLongitude(currentLongitude);

            // If the location queue is empty, add the first location to the queue without checking movement threshold.
            if (locationQueue.isEmpty()) {
                locationQueue.add(currentLocation);
            } else {
                // Calculate the distance between the new location and the last location in the queue
                Location lastLocation = locationQueue.peek();
                float distanceMoved = lastLocation.distanceTo(currentLocation);

                // If the distance is greater than the movementThreshold, add the new location to the queue
                if (distanceMoved > movementThreshold) {
                    locationQueue.add(currentLocation);

                    if (!isRunnableRunning) {
                        isRunnableRunning = true;
                        runnable = new Runnable() {
                            @Override
                            public void run() {
                                stepcount++;
                                // Check if the counter is an even number
                                if (stepcount % 2 == 0) {
                                    mMap.addMarker(new MarkerOptions().position(currentLatLng).title("User Location " + (locationQueue.size())));
                                }

                                isRunnableRunning = false;
                            }
                        };
                        handler.postDelayed(runnable, 5000); // Delay of 5 seconds
                    }
                }
            }
        }
    };

    /**
     * This method is used to search for a destination and get directions to it.
     * It clears the map before drawing a new path.
     * This method is called when the search button is clicked.
     */
    private void searchForDestination() {
        // If the user hasn't selected a place, request the user to select a place first.
        if (selectedPlace == null) {
            Toast.makeText(this, "Please select a place first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Clear the map before drawing a new path
        mMap.clear();

        // Create URL for Google Maps Directions API
        String url = "https://maps.googleapis.com/maps/api/directions/json" +
                "?destination=" + selectedPlace.latitude + "," + selectedPlace.longitude +
                "&mode=walking" +
                "&origin=" + currentLatitude + "," + currentLongitude +
                "&key=" + apikey;

        // Execute the AsyncTask to perform the API request
        new DirectionsTask(mMap, this).execute(url);

        // Start the DirectionForegroundService to show the directions in the foreground
        Intent dfserviceintent = new Intent(this, DirectionForegroundService.class);
        startForegroundService(dfserviceintent);
        Toast.makeText(this, "Destination has been confirmed", Toast.LENGTH_SHORT).show();
    }

    /**
     * This method is used to clear the map and stop the DirectionForegroundService.
     * This method is called when the clear button is clicked.
     */
    public void clearMap(){
        // If there is a map, clear it, clear the autocomplete fragment, clear data of previous navigation and stop the DirectionForegroundService
        if (mMap != null) {
            Intent dfserviceintent = new Intent(this, DirectionForegroundService.class);
            stopService(dfserviceintent);
            mMap.clear();
            selectedPlace = null;
            autocompleteFragment.setText("");

            // Clear the polyline and marker data from SharedPreferences
            SharedPreferences sharedPref = getSharedPreferences("PolylineData", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.remove("polyline");
            editor.remove("marker");
            editor.apply();
        }
    }
}