package com.jilexandr.trackyourbus;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.INTERNET;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private Marker currentUserMarker;
    private Marker currentBusStationMarker;
    private Location lastLocation;

    private Button btnImHere;
    private Button btnNearestStations;
    private SeekBar seekBarChangeSpeed;

    private Socket mSocket;

//    final private static String MODE_TRACKING = "tracking";
//    final private static String MODE_SUBSCRIBING = "subscribing";

    private Boolean isInTransport = false;
    private String transportId = null;

    private String getType() {
        return isInTransport ? "bus" : "client";
    }

//    private String currentMode = MODE_SUBSCRIBING;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION, INTERNET}, 225);

        String android_id = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
        log(android_id);

        btnImHere = (Button) findViewById(R.id.button_am_here);
        btnNearestStations = (Button) findViewById(R.id.button_find_nearest_bus_station);
        seekBarChangeSpeed = (SeekBar) findViewById(R.id.speed);

        Callable callable = () -> "Hello from callable!";

        try {
            callable.call();
        } catch (Exception e) {
            e.printStackTrace();
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // change fake speed
        seekBarChangeSpeed.setOnSeekBarChangeListener(new SeekBarChangeSpeedChangeListener());

        // show current position
        btnImHere.setOnClickListener(new BtnImHereClickListener());

        // find nearest bus station
        btnNearestStations.setOnClickListener(new BtnNearestStationsClickListener());

        initWebSockets();
    }

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {

            mSocket.emit("androidMessage", "I'm android device!");

            String deviceId = Secure.getString(getApplicationContext().getContentResolver(), Secure.ANDROID_ID);

            try {
                JSONObject data = new JSONObject();
                JSONObject deviceInfo = new JSONObject();
                deviceInfo.put("id", deviceId);
                deviceInfo.put("type", "android");
                deviceInfo.put("device", android.os.Build.DEVICE);
                deviceInfo.put("model", android.os.Build.MODEL);
                data.put("deviceId", deviceId);
                data.put("deviceInfo", deviceInfo);
                mSocket.emit("deviceWasIdentified", data);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private void log(String message) {
        Logger logger = Logger.getLogger("com.jilexandr.xxx");
        logger.log(new MyLevel("DISASTER", Level.SEVERE.intValue() + 1), message);
    }

    private String getUrl(double lat, double lng) {

        return "http://localhost:3000/api/near?coords=" + lat + "," + lng;
    }

    private void createMarker(HashMap<String, String> googlePlace) {

        MarkerOptions markerOptions = new MarkerOptions();

        double lat = Double.parseDouble(googlePlace.get("lat"));
        double lng = Double.parseDouble(googlePlace.get("lng"));

        String placeName = googlePlace.get("place_name");
        String vicinity = googlePlace.get("vicinity");

        LatLng latLng = new LatLng(lat, lng);

        markerOptions.position(latLng);
        markerOptions.title(placeName + " : " + vicinity);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

        mMap.addMarker(markerOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
    }

    private void initWebSockets() {

        try {
            mSocket = IO.socket("http://192.168.0.101:3000");

            mSocket.on(Socket.EVENT_CONNECT, onConnect);
            mSocket.on("checkInBus", new OnCheckMyBusEvent());
            mSocket.on(Socket.EVENT_DISCONNECT, new OnDisconnectEvent());
            mSocket.on(Socket.EVENT_ERROR, new OnErrorEvent());
            mSocket.connect();

        } catch (URISyntaxException e) {
            log(e.getMessage());
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        // click on bus station marker
        // choose interested bus
        mMap.setOnMarkerClickListener(marker -> {

            // don't process click on user's position marker
            if (marker.equals(currentUserMarker)) {
                return false;
            }

            if (currentBusStationMarker != null) {
                currentBusStationMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            }

            currentBusStationMarker = marker;
            currentBusStationMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

            TextView selectedBusStationText = (TextView) findViewById(R.id.selectedBusStationText);
            selectedBusStationText.setText(currentBusStationMarker.getTitle());

            ChooseBusDialogFragment dialog = new ChooseBusDialogFragment();

            dialog.setOnConfirmResult(selectedItems -> {
                TextView text = (TextView) findViewById(R.id.textSelectedRoutes);
                text.setText(selectedItems.toString());
            });

            dialog.show(getSupportFragmentManager(), "choose_bus");

            return false;
        });

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

//        if (lastLocation == null) {
//            lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//        }

        if (lastLocation != null) {
            onLocationChangedFn(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
        }

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,
                0,
                new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        onLocationChangedFn(location);
                    }

                    @Override
                    public void onStatusChanged(String s, int i, Bundle bundle) {

                    }

                    @Override
                    public void onProviderEnabled(String s) {

                    }

                    @Override
                    public void onProviderDisabled(String s) {

                    }
                });
    }

    private void onLocationChangedFn(Location location) {
        if (location == null) {
            return;
        }
        lastLocation = location;
        LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
        if (currentUserMarker == null) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(position);
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_round));
            currentUserMarker = mMap.addMarker(markerOptions);
        } else {
            currentUserMarker.setPosition(position);
        }
        try {
            JSONObject data = new JSONObject();
            JSONObject coords = new JSONObject();
            JSONObject transport = new JSONObject();

            coords.put("lat", location.getLatitude());
            coords.put("lng", location.getLongitude());

            transport.put("id", transportId);

            data.put("type", getType());
            data.put("coords", coords);
            data.put("speed", location.getSpeed());
            data.put("transport", transport);
            mSocket.emit("updateLocation", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(10));
    }

    class GetNearbyPlacesDataOnResult implements GetNearbyPlacesData.OnResult {
        @Override
        public void call(List<HashMap<String, String>> placesList) {
            Toast.makeText(MapsActivity.this, String.format("Found stations: %s", placesList.size()), Toast.LENGTH_LONG).show();
            for (int i = 0; i < placesList.size(); i++) {
                HashMap<String, String> googlePlace = placesList.get(i);
                createMarker(googlePlace);
            }
        }
    }

    private class OnCheckMyBusEvent implements Emitter.Listener {
        public void call(Object... args) {

            JSONObject data = (JSONObject) args[0];

            DialogFragment newFragment = new CheckBusDialogFragment();

            try {
                String speed = data.getString("speed");

                Bundle bundle = new Bundle();
                bundle.putString("content", String.format("Your speed was changed to %s. Are you in the bus?", speed));

                newFragment.setArguments(bundle);
                newFragment.show(getSupportFragmentManager(), "missiles");

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class OnDisconnectEvent implements Emitter.Listener {
        public void call(Object... args) {
            log("Socket.EVENT_DISCONNECT");
        }
    }

    private class OnErrorEvent implements Emitter.Listener {
        public void call(Object... args) {
            log("Socket.EVENT_ERROR");
        }
    }

    private class BtnImHereClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {

            LatLng position = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());

            mMap.moveCamera(CameraUpdateFactory.newLatLng(position));

            Context context = getApplicationContext();
            CharSequence text = "Hello toast!";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
    }

    private class BtnNearestStationsClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {

            Object[] data = new Object[1];

            data[0] = getUrl(lastLocation.getLatitude(), lastLocation.getLongitude());

            GetNearbyPlacesData getNearbyPlacesData = new GetNearbyPlacesData();
            getNearbyPlacesData.onResultHandler = new GetNearbyPlacesDataOnResult();
            getNearbyPlacesData.execute(data);
        }
    }

    private class SeekBarChangeSpeedChangeListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            lastLocation.setSpeed(seekBar.getProgress());
            onLocationChangedFn(lastLocation);
        }
    }
}
