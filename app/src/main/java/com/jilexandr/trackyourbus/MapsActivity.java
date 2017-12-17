package com.jilexandr.trackyourbus;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.provider.Settings.Secure;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private Marker currentUserMarker;
    private Marker currentBusStationMarker;
    private Location lastLocation;

    private void log(String message) {
        Logger logger = Logger.getLogger("com.jilexandr.xxx");
        logger.log(new MyLevel("DISASTER", Level.SEVERE.intValue() + 1), message);
    }

    private Socket mSocket;

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

    private String getUrl(double latitude, double longitude, double radius) {

        StringBuilder googlePlacesUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlacesUrl.append("location=" + latitude + "," + longitude);
        googlePlacesUrl.append("&radius=" + radius);
        googlePlacesUrl.append("&type=" + "bus_station");
        googlePlacesUrl.append("&sensor=true");
        googlePlacesUrl.append("&key=" + "AIzaSyB9sHnLpEKYxM8pd0DutRyvko6Lgxzdf3Y");
        Log.d("getUrl", googlePlacesUrl.toString());
        return googlePlacesUrl.toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        ws();

        SeekBar speed = findViewById(R.id.speed);
        speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Location location = new Location(LocationManager.GPS_PROVIDER);
                location.setSpeed(seekBar.getProgress());
                onLocationChangedFn(location);
            }
        });

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Context context = getApplicationContext();
                CharSequence text = "Hello toast!";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        });

        // find nearest bus station
        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Object[] data = new Object[2];

                data[0] = mMap;
                data[1] = getUrl(lastLocation.getLatitude(), lastLocation.getLongitude(), 300);

                GetNearbyPlacesData getNearbyPlacesData = new GetNearbyPlacesData();
                getNearbyPlacesData.onResultHandler = new GetNearbyPlacesData.OnResult() {
                    @Override
                    public void call(List<HashMap<String, String>> placesList) {
                        Toast.makeText(MapsActivity.this, String.format("Found stations: %s", placesList.size()), Toast.LENGTH_LONG).show();
                        for (int i = 0; i < placesList.size(); i++) {
                            HashMap<String, String> googlePlace = placesList.get(i);
                            createMarker(googlePlace);
                        }
                    }
                };
                getNearbyPlacesData.execute(data);
            }
        });
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

    private void ws() {

        try {
            mSocket = IO.socket("http://192.168.0.101:3000");

            mSocket.on(Socket.EVENT_CONNECT, onConnect);

            mSocket.on("checkInBus", new Emitter.Listener() {
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
            });

            mSocket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                public void call(Object... args) {
                    log("Socket.EVENT_DISCONNECT");
                }
            });

            mSocket.on(Socket.EVENT_ERROR, new Emitter.Listener() {
                public void call(Object... args) {
                    log("Socket.EVENT_ERROR");
                }
            });

            mSocket.connect();

        } catch (URISyntaxException e) {
            log(e.getMessage());
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        // click on bus station marker
        // choose interested bus
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                if (currentBusStationMarker != null) {
                    currentBusStationMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                }

                currentBusStationMarker = marker;
                currentBusStationMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

                TextView selectedBusStationText = (TextView) findViewById(R.id.selectedBusStationText);
                selectedBusStationText.setText(currentBusStationMarker.getTitle());

                ChooseBusDialogFragment dialog = new ChooseBusDialogFragment();
                dialog.setOnConfirmResult(new ConfirmResult() {
                    @Override
                    public void on(List<BusRoute> selectedItems) {
                        TextView text = (TextView) findViewById(R.id.textSelectedRoutes);
                        text.setText(selectedItems.toString());
                    }
                });
                dialog.show(getSupportFragmentManager(), "choose_bus");

                return false;
            }
        });

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastLocation != null) {
            onLocationChangedFn(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
        }

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000,
                10,
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
        lastLocation = location;
        LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
        if (currentUserMarker == null) {
            currentUserMarker = mMap.addMarker(new MarkerOptions().position(position));
        } else {
            currentUserMarker.setPosition(position);
        }
        try {
            JSONObject data = new JSONObject();
            JSONObject coords = new JSONObject();
            coords.put("latitude", location.getLatitude());
            coords.put("longitude", location.getLongitude());
            data.put("coords", coords);
            data.put("speed", location.getSpeed());
            mSocket.emit("locationWasChanged", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(10));
    }
}
