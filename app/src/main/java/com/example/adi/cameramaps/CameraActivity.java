package com.example.adi.cameramaps;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

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
    private boolean isReverted = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        surfaceView = (SurfaceView)findViewById(R.id.camera_activity_surface_view);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

         // notify when underlying surface is created or destroyed
        surfaceHolder.addCallback(this);

        arrow = (ImageView)findViewById(R.id.activity_camera_arrow_image_view);

        debugTextView = (TextView)findViewById(R.id.activity_camera_debut_text);

        mySensorListener = new MySensorListener();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        camera = Camera.open();
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
        mySensorListener.startSensor();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        camera.stopPreview();
        camera.release();
        camera = null;
        mySensorListener.stopSensor();
    }


    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onStart() {
        super.onStart();

    }


    private class MySensorListener implements SensorEventListener{
        private SensorManager mSensorManager;
        private float currentDegree = 0f;
        private int animationDuration = 250;
        private float degree;
        private float currentDeviceTilt;
        private float imageTilt = 0;
        private float threshold = 60;
        private float reverseValue = 1;


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
            currentDeviceTilt = Math.round(sensorEvent.values[1]);

            if(degree > threshold && degree < (360 - threshold)){
                isReverted = false;
                imageTilt = -1 * 60;
                reverseValue = -1;
            }else{
                imageTilt = 60;
                isReverted = true;
                reverseValue = 1;
            }

            if(-179.9 < currentDeviceTilt && currentDeviceTilt < -90){
                currentDeviceTilt = -179 + (currentDeviceTilt * -1);
            }

            // rotate for device tilt
            ObjectAnimator animation = ObjectAnimator.ofFloat(arrow, "rotationX",
                    imageTilt - (currentDeviceTilt / 5 * reverseValue), 0f);
            animation.setDuration(animationDuration);
            animation.start();
            currentDegree = -degree;


            // rotate for compass
            RotateAnimation ra = new RotateAnimation(
                    currentDegree,
                    -degree,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f);
            ra.setDuration(animationDuration);
            ra.setFillAfter(true);
            arrow.startAnimation(ra);


            debugTextView.setText("0: " + sensorEvent.values[0] +
                "\n1: " + sensorEvent.values[1]);


        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }
}
