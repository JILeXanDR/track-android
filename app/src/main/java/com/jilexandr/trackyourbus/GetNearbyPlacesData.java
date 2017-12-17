package com.jilexandr.trackyourbus;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;

class GetNearbyPlacesData extends AsyncTask<Object, String, String> {

    private String googlePlacesDataJson = "";
    private GoogleMap mMap;
    public OnResult onResultHandler;

    interface OnResult {
        public void call(List<HashMap<String, String>> placesList);
    }

    @Override
    protected String doInBackground(Object... params) {

        mMap = (GoogleMap) params[0];
        String url = (String) params[1];

        try {
            googlePlacesDataJson = HttpRequest.get(url);
        } catch (Exception e) {
            Log.d("GooglePlacesReadTask", e.toString());
        }

        return googlePlacesDataJson;
    }

    @Override
    protected void onPostExecute(String result) {

        DataParser dataParser = new DataParser();

        if (onResultHandler != null) {
            onResultHandler.call(dataParser.parse(result));
        }
    }
}
