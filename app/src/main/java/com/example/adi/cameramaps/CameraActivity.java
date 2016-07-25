package com.example.adi.cameramaps;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import java.io.IOException;

/**
 * Created by adi on 25/07/16.
 */
public class CameraActivity extends Activity implements SurfaceHolder.Callback{
    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private SurfaceView compassSurfaceView;
    private CompassSurfaceViewRunnable surfaceViewRunnable;
    private float degree = 0;
    private MySensorListener mySensorListener;
    private ImageView compassImageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        surfaceView = (SurfaceView)findViewById(R.id.camera_activity_surface_view);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        compassSurfaceView = (SurfaceView)findViewById(R.id.camera_activity_compass_surface_view);
        surfaceViewRunnable = new CompassSurfaceViewRunnable(compassSurfaceView);

         // notify when underlying surface is created or destroyed
        surfaceHolder.addCallback(this);

//        compassImageView = (ImageView)findViewById(R.id.compass_image_view);

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
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        camera.stopPreview();
        camera.release();
        camera = null;
    }


    @Override
    protected void onStop() {
        super.onStop();
        surfaceViewRunnable.pause();
        mySensorListener.stopSensor();
    }

    @Override
    protected void onStart() {
        super.onStart();
        surfaceViewRunnable.resume();
        mySensorListener.startSensor();

    }

    public void setDegree(float degree){
        this.degree = degree;
    }

    public class CompassSurfaceViewRunnable  implements Runnable{
        Thread thread = null;
        SurfaceHolder holder;
        boolean isOK = false;
        int imageDimension;
        float widthPercent = 0.3f;
        public CompassSurfaceViewRunnable(SurfaceView surfaceView) {
            holder = surfaceView.getHolder();
            holder.setFormat(PixelFormat.TRANSLUCENT);
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            surfaceView.setZOrderMediaOverlay(true);

            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            imageDimension = (int) (size.x * widthPercent);
        }

        @Override
        public void run() {

            while (isOK){
//                try {
//                    Thread.sleep(50);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                if(!holder.getSurface().isValid()){
                    continue;
                }
                Canvas canvas = holder.lockCanvas();
                if(canvas != null){
                    canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                    //border's properties

                    Bitmap icon = BitmapFactory.decodeResource(getResources(),
                            R.drawable.compass_icon);
                    // rotate and scale bitmap
                    Matrix matrix = new Matrix();
                    matrix.setRotate(- degree, imageDimension /2, imageDimension /2);
                    canvas.drawBitmap(Bitmap.createScaledBitmap(icon, imageDimension, imageDimension, false), matrix, new Paint());

                    holder.unlockCanvasAndPost(canvas);
                }
            }
        }

        public void pause(){
            isOK = false;
            while(true){
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            }
            thread = null;
        }

        public void resume(){
            isOK = true;
            thread = new Thread(this);
            thread.start();
        }

    }

    private class MySensorListener implements SensorEventListener{
        // record the compass picture angle turned
        private float currentDegree = 0f;
        private SensorManager mSensorManager;

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
            Log.d("SensorListener", "Degree: " + degree);
//
//            RotateAnimation ra = new RotateAnimation(
//                    currentDegree,
//                            -degree,
//                    Animation.RELATIVE_TO_SELF, 0.5f,
//                    Animation.RELATIVE_TO_SELF,
//            0.5f);
//
//            ra.setDuration(210);
//            ra.setFillAfter(true);
//            compassImageView.startAnimation(ra);
//            currentDegree = -degree;



        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }
}
