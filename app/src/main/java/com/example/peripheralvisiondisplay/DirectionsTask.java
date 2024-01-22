package com.example.peripheralvisiondisplay;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

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
    private int currentStepIndex = 0;
    private LatLng currentStepEndLocation;

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
                    for (int j = 0; j < steps.length(); j++) {
                        JSONObject step = steps.getJSONObject(j);

                        // Get the start and end location of the step
//                        JSONObject startLocation = step.getJSONObject("start_location");
                        JSONObject endLocation = step.getJSONObject("end_location");

                        // Get the text instruction for the step
                        String instruction = step.getString("html_instructions");
                        stepsList.add(instruction);

                        // Get the end location of the step
                        double endLat = endLocation.getDouble("lat");
                        double endLng = endLocation.getDouble("lng");
                        currentStepEndLocation = new LatLng(endLat, endLng);
//                        Log.d("eas", "parseDirectionsData: " + currentStepEndLocation.toString());
                        mMap.addMarker(new MarkerOptions().position(currentStepEndLocation).title("Step " + (j + 1) + " End"));
                        // adds currentstepend to list to send to directionforegroundservice
                        stepsEndLocationList.add(currentStepEndLocation);

//                        // Get the distance and duration of the step
//                        JSONObject distance = step.getJSONObject("distance");
//                        JSONObject duration = step.getJSONObject("duration");

                    }
                }

                JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                String encodedPolyline = overviewPolyline.getString("points");

                // Decode the polyline into LatLng points and draw it on the map
                List<LatLng> decodedPolyline = decodePolyline(encodedPolyline);
                PolylineOptions polylineOptions = new PolylineOptions();
                polylineOptions.addAll(decodedPolyline);
                mMap.addPolyline(polylineOptions);

                // Send the steps data to DirectionForegroundService
                Intent intent = new Intent("StepsData");
                intent.putStringArrayListExtra("StepsList", new ArrayList<>(stepsList));
//                sendBroadcast(intent);

                //todo: send the steps end location data to DirectionForegroundService - they for some reason dont work together?
//                intent = new Intent("StepsEndLocationData");
                intent.putParcelableArrayListExtra("StepsEndLocationList", new ArrayList<>(stepsEndLocationList));

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

    // Decode polyline string to a list of LatLng points
    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1F) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1F) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((lat / 1E5), (lng / 1E5));
            poly.add(p);
            Log.e("eas", "decodePolyline: " + p.toString());
        }
        return poly;
    }

//    public boolean isStepFulfilled(LatLng userLocation) {
//        // Calculate the distance between the user's location and the end location of the current step
//        double distance = calculateDistance(userLocation, currentStepEndLocation);
//
//        // Consider the step as fulfilled if the distance is less than a certain threshold
//        // The threshold can be adjusted based on your requirements
//        return distance < 10; // 10 meters
//    }

//    public double calculateDistance(LatLng point1, LatLng point2) {
//        double earthRadius = 6371; // Radius of the earth in km
//        double latDiff = Math.toRadians(point2.latitude - point1.latitude);
//        double lngDiff = Math.toRadians(point2.longitude - point1.longitude);
//        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2)
//                + Math.cos(Math.toRadians(point1.latitude)) * Math.cos(Math.toRadians(point2.latitude))
//                * Math.sin(lngDiff / 2) * Math.sin(lngDiff / 2);
//        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
//        double distance = earthRadius * c; // Convert to meters
//        return distance * 1000;
//    }


//    public String getNextStep() {
//        if (currentStepIndex < stepsList.size()) {
//            return stepsList.get(currentStepIndex++);
//        } else {
//            return null; // No more steps
//        }
//    }

}
