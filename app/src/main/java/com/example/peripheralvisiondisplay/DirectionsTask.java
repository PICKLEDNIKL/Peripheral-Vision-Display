package com.example.peripheralvisiondisplay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class is used to get directions data from Google Directions API.
 * It uses an Executor and handler to get retrieve data from the API and parse the JSON response.
 * The JSON response is handled and information about the navigation journey is displayed on MapsActivity.
 * Shared preferences are used to store JSON data to be used in the DirectionForegroundService.
 */
public class DirectionsTask {

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private GoogleMap mMap;
    private Context mContext;
    private List<String> stepsList = new ArrayList<>();
    private List<LatLng> stepsEndLocationList = new ArrayList<>();
    private List<Integer> stepsDistanceList = new ArrayList<>();
    private int currentStepIndex = 0;
    private LatLng currentStepEndLocation;
    private List<String> polylineList = new ArrayList<>();
    private LatLng firstlatlng;


    /**
     * Constructor for the DirectionsTask class.
     * Initializes Google map and context instances for this class.
     *
     * @param mMap The Google map in MapsActivity.
     * @param context The context of the calling activity.
     */
    DirectionsTask(GoogleMap mMap, Context context) {
        this.mMap = mMap;
        this.mContext = context;
    }

    /**
     * This method is called to execute the DirectionsTask.
     * An executor is used to run the task in a separate thread.
     * The handler is used to post the result back to the main thread.
     *
     * @param url The URL to fetch data from the Google Directions API.
     */
    public void execute(String url) {
        executor.execute(() -> {
            String result = doInBackground(url);
            handler.post(() -> onPostExecute(result));
        });
    }

    /**
     * This method is called to get data from the Google Directions API and return the response as a string.
     *
     * @param urls The URL to get data from the Google Directions API.
     * @return The response from the API as a string.
     */
    protected String doInBackground(String... urls) {
        String response = "";
        HttpURLConnection urlConnection = null;
        try {
            // Create URL object and open connection
            URL url = new URL(urls[0]);
            urlConnection = (HttpURLConnection) url.openConnection();

            InputStream in = urlConnection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            // Read the response line by line and append the response together to create a single response string.
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            response = stringBuilder.toString();
        } catch (IOException e) {
            // Log an error message for exception.
            Log.e("DirectionsTask", "Error fetching data from URL", e);
        } finally {
            // After the response fetched, disconnect the URL connection.
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return response;
    }

    /**
     * This method is called after the doInBackground method has finished executing, using the handler.
     * The parseDirectionsData method is called and the result from doInBackground is passed.
     *
     * @param result The result of doInBackground, which is a the string response from the Google Directions API.
     */
    protected void onPostExecute(String result) {
        parseDirectionsData(result);
    }

    /**
     * This method is called to parse the directions data from the Google Directions API.
     * The JSON response is parsed and the steps strings, end locations, and distance of each step are stored in lists.
     * The polyline for each step is decoded and drawn on the map.
     * Stores data in shared preferences to be used in MapsActivity send data to DirectionForegroundService.
     *
     * @param jsonData The JSON response from Google's Directions API.
     */
    private void parseDirectionsData(String jsonData) {
        try {
            // Create a JSON object from JSON response.
            JSONObject jsonObject = new JSONObject(jsonData);

            // Check if the response status is OK.
            String status = jsonObject.getString("status");
            if (status.equals("OK")) {
                // Get the routes from the JSON object.
                JSONArray routes = jsonObject.getJSONArray("routes");
                // Only get the first route as there is only one route.
                JSONObject route = routes.getJSONObject(0);

                // Get the legs of the route.
                JSONArray legs = route.getJSONArray("legs");
                for (int i = 0; i < legs.length(); i++) {
                    JSONObject leg = legs.getJSONObject(i);

                    // Get the steps of the leg
                    JSONArray steps = leg.getJSONArray("steps");

                    // Clear the steps list, steps end location list, and steps distance list
                    stepsList.clear();
                    stepsEndLocationList.clear();
                    stepsDistanceList.clear();

                    for (int j = 0; j < steps.length(); j++) {
                        JSONObject step = steps.getJSONObject(j);

                        // Get the text instruction for steps.
                        String instruction = step.optString("maneuver");

                        // If there isn't any maneuver data, get the html_instruction for the step instead.
                        if (instruction.isEmpty()){
                            instruction = step.getString("html_instructions");
                            instruction = instruction.replace("<b>", "").replace("</b>", "");
                        }
                        stepsList.add(instruction);

                        // Get the start location of the first step and add start marker to the map
                        if (j == 0) {
                            JSONObject startLocation = step.getJSONObject("start_location");
                            double startLat = startLocation.getDouble("lat");
                            double startLng = startLocation.getDouble("lng");
                            firstlatlng = new LatLng(startLat, startLng);
                            mMap.addMarker(new MarkerOptions().position(firstlatlng).title("Start"));
                        }

                        // Get the end location of the step
                        JSONObject endLocation = step.getJSONObject("end_location");
                        double endLat = endLocation.getDouble("lat");
                        double endLng = endLocation.getDouble("lng");
                        currentStepEndLocation = new LatLng(endLat, endLng);

                        // Adding this back will add markers for each step end
//                        mMap.addMarker(new MarkerOptions().position(currentStepEndLocation).title("Step " + (j + 1) + " End"));

                        // Add current step end location to list.
                        stepsEndLocationList.add(currentStepEndLocation);

                        // Get the distance data of the step
                        JSONObject distance = step.getJSONObject("distance");
                        Integer distanceval = distance.getInt("value");
                        stepsDistanceList.add(distanceval);

                        // Get the polyline data for each step
                        JSONObject polylineObject = step.getJSONObject("polyline");
                        String polyline = polylineObject.getString("points");
                        polylineList.add(polyline);

                        // Decode the polyline into a list of LatLng points and show it on the map
                        List<LatLng> decodedPolyline = PolyUtil.decode(polyline);
                        PolylineOptions polylineOptions = new PolylineOptions();
                        polylineOptions.addAll(decodedPolyline);
                        mMap.addPolyline(polylineOptions);
                    }
                    // Add destination marker to the map
                    mMap.addMarker(new MarkerOptions().position(currentStepEndLocation).title("Destination"));
                }

                // Convert the polyline list and marker list to strings.
                String polylineData = String.join(";", polylineList);
                String markerData = String.join(";", stepsEndLocationList.toString());

                // Store the polyline and marker data in SharedPreferences to use in MapsActivity.
                SharedPreferences sharedPref = mContext.getSharedPreferences("PolylineData", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("polyline", polylineData);
                editor.putString("marker", markerData);
                editor.apply();

                // Send all steps data to DirectionForegroundService
                Intent intent = new Intent("StepsData");
                intent.putStringArrayListExtra("StepsList", new ArrayList<>(stepsList));
                intent.putParcelableArrayListExtra("StepsEndLocationList", new ArrayList<>(stepsEndLocationList));
                intent.putIntegerArrayListExtra("StepsDistanceList", new ArrayList<>(stepsDistanceList));
                intent.putExtra("FirstLatLng", firstlatlng);
                sendBroadcast(intent);

            } else {
                Log.e("DirectionsTask", "Error fetching data from URL");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // This method is required because DirectionsTask is not a Context
    // We get the application context from the GoogleMap instance
    private void sendBroadcast(Intent intent) {
        if (mMap != null && mContext != null) {
            mContext.sendBroadcast(intent);
        }
    }
}
