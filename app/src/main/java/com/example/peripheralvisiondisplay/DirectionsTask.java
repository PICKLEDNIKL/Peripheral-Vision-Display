package com.example.peripheralvisiondisplay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



public class DirectionsTask extends AsyncTask<String, Void, String> {

    private GoogleMap mMap;
    private Context mContext;
    private List<String> stepsList = new ArrayList<>();
    private List<LatLng> stepsEndLocationList = new ArrayList<>();
    private List<Integer> stepsDistanceList = new ArrayList<>();
    private int currentStepIndex = 0;
    private LatLng currentStepEndLocation;
    private List<String> polylineList = new ArrayList<>();

    DirectionsTask(GoogleMap mMap, Context context) {
        this.mMap = mMap;
        this.mContext = context;
    }

    @Override
    protected String doInBackground(String... urls) {
        String response = "";
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(urls[0]);
            urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = urlConnection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            response = stringBuilder.toString();
        } catch (IOException e) {
            Log.e("DirectionsTask", "Error fetching data from URL", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return response;
    }

    @Override
    protected void onPostExecute(String result) {
        // Handle the result - the 'result' variable contains the API response as a JSON string
        Log.d("DirectionsTask", "API Response: " + result);
        // You can parse the JSON and extract directions data to display on the map or UI
        parseDirectionsData(result);


    }

    private void parseDirectionsData(String jsonData) {
        try {
            JSONObject jsonObject = new JSONObject(jsonData);

            // Check if the response status is OK
            String status = jsonObject.getString("status");
            if (status.equals("OK")) {
                JSONArray routes = jsonObject.getJSONArray("routes");
                JSONObject route = routes.getJSONObject(0); // Take the first route

                // Get the legs of the route
                JSONArray legs = route.getJSONArray("legs");
                for (int i = 0; i < legs.length(); i++) {
                    JSONObject leg = legs.getJSONObject(i);

                    // Get the steps of the leg
                    JSONArray steps = leg.getJSONArray("steps");

                    // Clear mMap of markers and polylines
//                    mMap.clear();
                    stepsList.clear();
                    stepsEndLocationList.clear();
                    stepsDistanceList.clear();

                    for (int j = 0; j < steps.length(); j++) {
                        JSONObject step = steps.getJSONObject(j);

                        // Get the text instruction for the step
                        String instruction = step.optString("maneuver");

                        // if maneuver is empty, get html_instructions
                        if (instruction.isEmpty()){
                            instruction = step.getString("html_instructions");
                            instruction = instruction.replace("<b>", "").replace("</b>", "");
                        }
                        stepsList.add(instruction);

                        // Get the start and end location of the step
                        JSONObject endLocation = step.getJSONObject("end_location");

                        // Get the end location of the step
                        double endLat = endLocation.getDouble("lat");
                        double endLng = endLocation.getDouble("lng");
                        currentStepEndLocation = new LatLng(endLat, endLng);
                        //TODO: MAYBE ADD THIS BACK LATER
//                        mMap.addMarker(new MarkerOptions().position(currentStepEndLocation).title("Step " + (j + 1) + " End"));
                        // adds currentstepend to list to send to directionforegroundservice
                        stepsEndLocationList.add(currentStepEndLocation);

                        // Get the start and end location of the step
                        JSONObject distance = step.getJSONObject("distance");
                        Integer distanceval = distance.getInt("value");
                        stepsDistanceList.add(distanceval);

                        // Get the polyline for each step
                        JSONObject polylineObject = step.getJSONObject("polyline");
                        String polyline = polylineObject.getString("points");

                        polylineList.add(polyline);

                        // Decode the polyline into LatLng points and draw it on the map
                        List<LatLng> decodedPolyline = PolyUtil.decode(polyline);
                        PolylineOptions polylineOptions = new PolylineOptions();
                        polylineOptions.addAll(decodedPolyline);

                        mMap.addPolyline(polylineOptions);
                    }
                    mMap.addMarker(new MarkerOptions().position(currentStepEndLocation).title("Destination"));
                }

                // Convert the polyline list to a single string
                String polylineData = String.join(";", polylineList);
                String markerData = String.join(";", stepsEndLocationList.toString());

                // Store the polyline data in SharedPreferences
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
                sendBroadcast(intent);

            } else {
                // Handle other status responses (e.g., ZERO_RESULTS, NOT_FOUND, etc.)
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
