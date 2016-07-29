package com.example.adi.cameramaps;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.adi.cameramaps.listeners.MyLocationListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private GoogleMap mMap;
    private LocationManager locationManager;
    private MyLocationListener locationListener;
    private Marker currentLocationMarker;
    private Marker destinationLocationMarker;
    private double destinationLongitude;
    private double destinationLatitude;
    private double currentLocationLongitude;
    private double currentLocationLatitude;
    private GoogleApiClient googleApiClient;
    private Button startRoutingButton;
    private TextView coortinatesTextView;
    private boolean isLocationManagerSet = false;

    private GeomagneticField geomagneticField;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        startRoutingButton = (Button)findViewById(R.id.activity_maps_start_navigation_button);
        startRoutingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(locationListener.isDeviceConnectedToGPS() && destinationLocationMarker != null){
                    Intent intent = new Intent(MapsActivity.this, CameraActivity.class);
                    intent.putExtra("destinationLongitude", destinationLongitude);
                    intent.putExtra("destinationLatitude", destinationLatitude);
                    intent.putExtra("currentLongitude", currentLocationLongitude);
                    intent.putExtra("currentLatitude", currentLocationLatitude);
                    intent.putExtra("declination", geomagneticField.getDeclination());
                    startActivity(intent);
                }else if(!locationListener.isDeviceConnectedToGPS()){
                    Toast.makeText(MapsActivity.this, "Device not connected to GPS", Toast.LENGTH_SHORT).show();
                }else if(destinationLocationMarker == null){
                    Toast.makeText(MapsActivity.this, "Destination point not defined", Toast.LENGTH_SHORT).show();
                }
            }
        });

        coortinatesTextView = (TextView)findViewById(R.id.activity_maps_coordinates_text_view);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        locationListener = new MyLocationListener(this, coortinatesTextView);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 10);
                return;
            } else {
                configureLocationManager();
            }
        } else {
            configureLocationManager();
        }

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 10:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    configureLocationManager();
                }
                break;

        }
    }

    private void configureLocationManager() {
        Log.e("LocationManager", "Configuring location manager");
        if(!isLocationManagerSet){
            locationManager.requestLocationUpdates("gps", 500, 0, (android.location.LocationListener) locationListener);
            isLocationManagerSet = true;
            Log.e("LocationManager", "Location manager set");
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
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                setDestinationLocation(latLng);

            }
        });

    }


    /**
     * this function is used to set current location, it is used to set initial current location
     * @param location
     * @param title
     */
    public void setCurrentLocation(Location location, String title) {
        if(coortinatesTextView != null){
            if(location != null)
                coortinatesTextView.setText("long: " + location.getLongitude() + "\nlat: " + location.getLatitude());
        }

        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
        if(currentLocationMarker != null){
            currentLocationMarker.remove();
        }
        currentLocationMarker = mMap.addMarker(new MarkerOptions()
                .position(currentLocation)
                .title(title));

        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
        mMap.animateCamera(CameraUpdateFactory.zoomIn());
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
    }

    /**
     * this function is used to change current location of marker and to store that new location
     * in current location variable
     * @param location
     * @param title
     */
    public void changeCurrentLocation(Location location, String title){
        setGeomagneticField(location);
        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

        if(currentLocationMarker != null){
            currentLocationMarker.remove();
        }
        currentLocationMarker = mMap.addMarker(new MarkerOptions()
                .position(currentLocation)
                .title(title));
        currentLocationLatitude = location.getLatitude();
        currentLocationLongitude = location.getLongitude();
    }

    /**
     * when user clicks on map new marker is added if one does not exist
     * if marker is added, that marker is removed and new is added
     * there is no option to change marker location, to change marker location the old need to be removed
     * and after that, new marker needs to be added
     * @param location
     */
    public void setDestinationLocation(LatLng location){
        if(destinationLocationMarker != null){
            destinationLocationMarker.remove();
        }
        destinationLatitude = location.latitude;
        destinationLongitude = location.longitude;
        destinationLocationMarker = mMap.addMarker(new MarkerOptions()
                .position(location)
                .title("Destination")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
    }

    /**
     * this function is used to set current location of device based on last known location
     * @param bundle
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 10);
                    return;
                } else {
                    Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                            googleApiClient);

                    setCurrentLocation(mLastLocation, "Last known location");
                }
            } else {
                Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                        googleApiClient);
                setCurrentLocation(mLastLocation, "Last known location");
            }
            return;
        }else{
            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    googleApiClient);
            setCurrentLocation(mLastLocation, "Last known location");
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    @Override
    protected void onStart() {
        configureLocationManager();
        googleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.removeUpdates(locationListener);
        isLocationManagerSet = false;
        super.onStop();
    }

    public void setGeomagneticField(Location loaction){
        geomagneticField = new GeomagneticField((float)loaction.getLatitude(), (float)loaction.getLongitude(),
                (float)loaction.getAltitude(), System.currentTimeMillis());
    }

}
