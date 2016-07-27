package com.example.adi.cameramaps.listeners;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.adi.cameramaps.CameraActivity;
import com.example.adi.cameramaps.MapsActivity;

/**
 * Created by adi on 27/07/16.
 */
public class MyLocationListener implements LocationListener {
    private Context context;
    private TextView debugTextView;
    boolean isDeviceConnectedToGPS = false;
    private Location currentLocation;
    private MapsActivity mapsActivity;
    private CameraActivity cameraActivity;

    public MyLocationListener(Context context, TextView debugTextView) {
        this.context = context;
        this.debugTextView = debugTextView;
        if(context.getClass().getSimpleName().equals("MapsActivity")){
            this.mapsActivity = (MapsActivity)context;
        }
        if(context.getClass().getSimpleName().equals("CameraActivity")){
            this.cameraActivity = (CameraActivity)context;
        }
    }

    // update current location based on gps device data
    @Override
    public void onLocationChanged(Location location) {
        this.currentLocation = location;
        if(mapsActivity != null){
            mapsActivity.changeCurrentLocation(location, "Current location");
        }
        if(cameraActivity != null){
            cameraActivity.updateCurrentPoint(location.getLongitude(), location.getLatitude());
        }
        if(!isDeviceConnectedToGPS){
            Toast.makeText(context, "Device connected to GPS", Toast.LENGTH_LONG).show();
            isDeviceConnectedToGPS = true;
        }
        Log.e("LocationChanged", "Location: " + location.toString());
        if(debugTextView != null){
            debugTextView.setText("long: " + location.getLongitude() + "\nlat: " + location.getLatitude());
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        context.startActivity(intent);
    }

    public Location getCurrentLocation(){
        return currentLocation;
    }

    public boolean isDeviceConnectedToGPS() {
        return isDeviceConnectedToGPS;
    }

}
