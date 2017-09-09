package org.mixare;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import com.squareup.picasso.Picasso;

public class ViewMemo extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    Preview preview;
    Camera camera;
    Context ctx;
    // Camera.CameraInfo.CAMERA_FACING_FRONT or Camera.CameraInfo.CAMERA_FACING_BACK
    private final static int CAMERA_FACING = Camera.CameraInfo.CAMERA_FACING_BACK;
    ImageView iv;

    public void startCamera() {

        if ( preview == null ) {
            preview = new Preview(this, (SurfaceView) findViewById(R.id.surfaceView));
            preview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));
            ((FrameLayout) findViewById(R.id.layout)).addView(preview);
            preview.setKeepScreenOn(true);

            /* 프리뷰 화면 눌렀을 때  사진을 찍음
            preview.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    camera.takePicture(shutterCallback, rawCallback, jpegCallback);
                }
            });*/
        }

        preview.setCamera(null);
        if (camera != null) {
            camera.release();
            camera = null;
        }

        int numCams = Camera.getNumberOfCameras();
        if (numCams > 0) {
            try {

                camera = Camera.open(CAMERA_FACING);
                // camera orientation
                camera.setDisplayOrientation(setCameraDisplayOrientation(this, CAMERA_FACING,
                        camera));
                // get Camera parameters
                Camera.Parameters params = camera.getParameters();
                // picture image orientation
                params.setRotation(setCameraDisplayOrientation(this, CAMERA_FACING, camera));
                camera.startPreview();

            } catch (RuntimeException ex) {
                Toast.makeText(ctx, "camera_not_found " + ex.getMessage().toString(),
                        Toast.LENGTH_LONG).show();
                Log.d(TAG, "camera_not_found " + ex.getMessage().toString());
            }
        }

        preview.setCamera(camera);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = this;

        //상태바 없애기
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_view_memo);

        Intent intent = getIntent(); //이 액티비티를 부른 인텐트를 받는다.
        String url = intent.getStringExtra("url"); //"jizard"문자 받아옴
        Log.d("check url", url);

        iv = (ImageView)findViewById(R.id.imageView2);
        Picasso.with(this)
                .load(url.substring(8,url.length()))
                .into(iv);

        Button goBack = (Button)findViewById(R.id.goback2);
        goBack.setOnClickListener(new Button.OnClickListener() {public void onClick(View v) {Intent intent = new Intent(ViewMemo.this, MixView.class);startActivity(intent);}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Surface will be destroyed when we return, so stop the preview.
        if(camera != null) {
            // Call stopPreview() to stop updating the preview surface
            camera.stopPreview();
            preview.setCamera(null);
            camera.release();
            camera = null;
        }
        ((FrameLayout) findViewById(R.id.layout)).removeView(preview);
        preview = null;

    }

    public static int setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }
}