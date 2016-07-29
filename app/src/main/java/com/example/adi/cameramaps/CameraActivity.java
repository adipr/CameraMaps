package com.example.adi.cameramaps;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.example.adi.cameramaps.listeners.MyLocationListener;
import com.example.adi.cameramaps.utils.GPS.EarthCalc;
import com.example.adi.cameramaps.utils.GPS.Point;

import java.io.IOException;

/**
 * Created by adi on 25/07/16.
 */
public class CameraActivity extends Activity implements SurfaceHolder.Callback{
    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private MySensorListener mySensorListener;
    private ImageView arrow;
    private TextView debugTextView;
    private Switch cameraSwitch;

    private LocationManager locationManager;
    private MyLocationListener locationListener;

    private double destinationLongitude;
    private double destinationLatitude;
    private double currentLocationLongitude;
    private double currentLocationLatitude;
    private Point destinationPoint;
    private Point currentPoint;


    private TextView coortinatesTextView;
    private TextView destinationLongLatTextView;
    private TextView angleTextView;
    private TextView newAngleTextView;
    private boolean isLocationManagerSet = false;

    private float declination; // positive means the magnetic field is rotated east that much from true north, negative is opposite


    private double bearing;

    private SeekBar seekBar;
    private float errorDegree = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        coortinatesTextView = (TextView)findViewById(R.id.activity_camera_coordinates_text_view);

        locationListener = new MyLocationListener(this, coortinatesTextView);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        configureLocationManager();

        destinationLongLatTextView = (TextView)findViewById(R.id.camera_activity_dest_long_lat);
        angleTextView = (TextView)findViewById(R.id.camera_activity_angle);
        newAngleTextView = (TextView)findViewById(R.id.camera_activity_new_angle);


        Bundle bundle = getIntent().getExtras();
        try{
            destinationLongitude     = bundle.getDouble("destinationLongitude");
            destinationLatitude      = bundle.getDouble("destinationLatitude");
            currentLocationLongitude = bundle.getDouble("currentLongitude");
            currentLocationLatitude  = bundle.getDouble("currentLatitude");
            declination              = bundle.getFloat("declination");
        }catch (Exception e){
            e.printStackTrace();
        }

        currentPoint = new Point(currentLocationLatitude, currentLocationLongitude);
        destinationPoint = new Point(destinationLatitude, destinationLongitude);

        destinationLongLatTextView.setText("Long: " + destinationLongitude + "\nLat: " + destinationLatitude);

        surfaceView = (SurfaceView) findViewById(R.id.camera_activity_surface_view);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // notify when underlying surface is created or destroyed
        surfaceHolder.addCallback(this);

        arrow = (ImageView) findViewById(R.id.activity_camera_arrow_image_view);

        debugTextView = (TextView) findViewById(R.id.activity_camera_debut_text);

        cameraSwitch = (Switch) findViewById(R.id.activity_camera_switch);
        cameraSwitch.setChecked(true);
        cameraSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (compoundButton.isChecked()) {
                    startCamera();
                    surfaceView.setBackgroundColor(ContextCompat.getColor(CameraActivity.this, R.color.transparent));

                } else {
                    stopCamera();
                    surfaceView.setBackgroundColor(ContextCompat.getColor(CameraActivity.this, R.color.white));
                }
            }
        });

        mySensorListener = new MySensorListener();

        seekBar = (SeekBar)findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                newAngleTextView.setText(seekBar.getScrollX() + "\ni: " + i);
                errorDegree = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

//        compass = new Compass();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        startCamera();
        mySensorListener.startSensor();
//        compass.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        stopCamera();
        mySensorListener.stopSensor();
//        compass.stop();
    }

    private void startCamera(){
        if(camera == null){
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPreviewFrameRate(20);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            camera.setParameters(parameters);
            try {
                camera.setPreviewDisplay(surfaceHolder);
                camera.setDisplayOrientation(90);
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopCamera(){
        if(camera != null){
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        locationManager.removeUpdates(locationListener);
        isLocationManagerSet = false;

    }

    @Override
    protected void onStart() {
        super.onStart();
        configureLocationManager();

    }

    private class MySensorListener implements SensorEventListener{
        private SensorManager mSensorManager;
        private int animationDuration = 8000;
        private float currentDegree = 0f;
        private float degree;
        private float currentDeviceTilt;
        private float imageTilt = 60;
        private float threshold = 90;
        private float reverseValue = 1;
        private float distance = 0;


        public MySensorListener() {
            mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        }

        public void startSensor(){
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(
                    Sensor.TYPE_ORIENTATION),SensorManager.SENSOR_DELAY_GAME);
        }

        public void stopSensor(){
            mSensorManager.unregisterListener(this);
        }

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            // get the angle around the z-axis rotated
            degree = Math.round(sensorEvent.values[0]);
            bearing = EarthCalc.getBearing(currentPoint, destinationPoint);
            distance = (float) EarthCalc.getDistance(currentPoint, destinationPoint);

            degree = Math.round(sensorEvent.values[0]) + (float)bearing + declination + errorDegree;

            currentDeviceTilt = Math.round(sensorEvent.values[1]);

            if(degree > threshold && degree < (360 - threshold)){
                imageTilt = -1 * 60;
                reverseValue = -1;
            }else{
                imageTilt = 60;
                reverseValue = 1;
            }


            if(-179.99 < currentDeviceTilt && currentDeviceTilt < -90){
                currentDeviceTilt = -179.99f + (currentDeviceTilt * -1);
            }

            // rotate for device tilt
            ObjectAnimator animation = ObjectAnimator.ofFloat(arrow, "rotationX",
                    imageTilt - (currentDeviceTilt / 6 * reverseValue), 0f);
            animation.setDuration(animationDuration);
            animation.setInterpolator(new AccelerateDecelerateInterpolator());
            animation.start();
            currentDegree = -degree;



//            // rotate for compass
            RotateAnimation ra = new RotateAnimation(
                    currentDegree,
                    -degree,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f);
            ra.setDuration(animationDuration);
            ra.setFillAfter(true);
            arrow.startAnimation(ra);


            debugTextView.setText("Sensors data\n0: " + sensorEvent.values[0] +
                "\n1: " + sensorEvent.values[1] +
                    "\n2: " + sensorEvent.values[2] +
            "\ndeg: " + degree +
            "\ndist: "  + distance);




        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            if (sensor.getType() == Sensor.TYPE_ORIENTATION && accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
                Log.w("CameraActivity", "Compass data unreliable");
                newAngleTextView.setText("\nCompass unreliable");
            }else{
                newAngleTextView.setText("\nCompass reliable");
            }
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

    public void updateCurrentPoint(double longitude, double latitude){
        currentLocationLatitude = latitude;
        currentLocationLongitude = longitude;
        currentPoint = new Point(latitude, longitude);
        if(angleTextView != null){
            angleTextView.setText(String.valueOf(bearing) + "\n decl:" + declination);
        }
    }

    public void setNewAngle(double angle){
//        0.setText("New angle\n" + String.valueOf(angle));
    }


}
