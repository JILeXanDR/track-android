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

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

class GetNearbyPlacesData extends AsyncTask<Object, String, String> {

    private String googlePlacesDataJson = "";
    public OnResult onResultHandler;

    interface OnResult {
        public void call(List<HashMap<String, String>> placesList);
    }

    @Override
    protected String doInBackground(Object... params) {

        String url = (String) params[0];

        try {

            Request request = new Request.Builder().url(url).build();
            Response response = (new OkHttpClient()).newCall(request).execute();

            googlePlacesDataJson = response.body().string();

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
