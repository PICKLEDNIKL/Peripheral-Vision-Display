package com.example.peripheralvisiondisplay;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
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

    DirectionsTask(GoogleMap mMap) {
        this.mMap = mMap;
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

                JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                String encodedPolyline = overviewPolyline.getString("points");

                // Decode the polyline into LatLng points and draw it on the map
                List<LatLng> decodedPolyline = decodePolyline(encodedPolyline);
                PolylineOptions polylineOptions = new PolylineOptions();
                polylineOptions.addAll(decodedPolyline);
                mMap.addPolyline(polylineOptions);
            } else {
                // Handle other status responses (e.g., ZERO_RESULTS, NOT_FOUND, etc.)
            }
        } catch (JSONException e) {
            e.printStackTrace();
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

}
