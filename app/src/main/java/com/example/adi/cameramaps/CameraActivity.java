package com.example.adi.cameramaps;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

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
    }

    @Override
    protected void onStart() {
        super.onStart();
        surfaceViewRunnable.resume();
    }

    private class CompassSurfaceViewRunnable  implements Runnable{
        Thread thread = null;
        SurfaceHolder holder;
        boolean isOK = false;

        public CompassSurfaceViewRunnable(SurfaceView surfaceView) {
            holder = surfaceView.getHolder();
            holder.setFormat(PixelFormat.TRANSLUCENT);
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            surfaceView.setZOrderMediaOverlay(true);

        }

        @Override
        public void run() {
            int x = 0, y = 0;
            int width = 500;
            int height = 500;
            while (isOK){
                Log.d("CompassSurface", "RUNNING");
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(!holder.getSurface().isValid()){
                    continue;
                }
                Canvas canvas = holder.lockCanvas();
                if(canvas != null){
                    canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                    //border's properties

                    Bitmap icon = BitmapFactory.decodeResource(getResources(),
                            R.mipmap.ic_launcher);

                    Rect dest = new Rect(0, 0, 800, 800);
                    Paint paint = new Paint();
                    paint.setFilterBitmap(true);
                    canvas.drawBitmap(icon, null, dest, paint);
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
}
