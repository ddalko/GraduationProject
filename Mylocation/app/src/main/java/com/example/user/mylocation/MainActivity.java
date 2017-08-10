package com.example.user.mylocation;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    Preview preview;
    Camera camera;
    Context ctx;
    private final static int PERMISSIONS_REQUEST_CODE = 100;
    // Camera.CameraInfo.CAMERA_FACING_FRONT or Camera.CameraInfo.CAMERA_FACING_BACK
    private final static int CAMERA_FACING = Camera.CameraInfo.CAMERA_FACING_BACK;
    private AppCompatActivity mActivity;
    private DrawLine drawLine = null;

    LinearLayout llCanvas;
    private String uniqueId;
    public static String tempDir;
    public String current = null;
    View mView;
    File mypath;






    public static void doRestart(Context c) {
        //http://stackoverflow.com/a/22345538
        try {
            //check if the context is given
            if (c != null) {
                //fetch the packagemanager so we can get the default launch activity
                // (you can replace this intent with any other activity if you want
                PackageManager pm = c.getPackageManager();
                //check if we got the PackageManager
                if (pm != null) {
                    //create the intent with the default start activity for your application
                    Intent mStartActivity = pm.getLaunchIntentForPackage(
                            c.getPackageName()
                    );
                    if (mStartActivity != null) {
                        mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        //create a pending intent so the application is restarted
                        // after System.exit(0) was called.
                        // We use an AlarmManager to call this intent in 100ms
                        int mPendingIntentId = 223344;
                        PendingIntent mPendingIntent = PendingIntent
                                .getActivity(c, mPendingIntentId, mStartActivity,
                                        PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr =
                                (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                        //kill the application
                        System.exit(0);
                    } else {
                        Log.e(TAG, "Was not able to restart application, " +
                                "mStartActivity null");
                    }
                } else {
                    Log.e(TAG, "Was not able to restart application, PM null");
                }
            } else {
                Log.e(TAG, "Was not able to restart application, Context null");
            }
        } catch (Exception ex) {
            Log.e(TAG, "Was not able to restart application");
        }
    }

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
        mActivity = this;

        //상태바 없애기
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        tempDir = Environment.getExternalStorageDirectory() + "/" + getResources().getString(R.string.external_dir) + "/";
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir(getResources().getString(R.string.external_dir), Context.MODE_PRIVATE);
        prepareDirectory();
        uniqueId = getTodaysDate() + "_" + getCurrentTime() + "_" + Math.random();
        current = uniqueId + ".png";
        mypath= new File(directory,current);

        Button button = (Button)findViewById(R.id.btnCapture);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camera.takePicture(shutterCallback, rawCallback, jpegCallback);
            }
        });

        Button mGetSign = (Button)findViewById(R.id.getsign);
        mGetSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mView.setDrawingCacheEnabled(true);
                drawLine.save(mView);
            }
        });


        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //API 23 이상이면
                // 런타임 퍼미션 처리 필요
                int hasCameraPermission = ContextCompat.checkSelfPermission(this,
                        Manifest.permission.CAMERA);
                int hasWriteExternalStoragePermission =
                        ContextCompat.checkSelfPermission(this,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if ( hasCameraPermission == PackageManager.PERMISSION_GRANTED
                        && hasWriteExternalStoragePermission==PackageManager.PERMISSION_GRANTED){
                    ;//이미 퍼미션을 가지고 있음
                }
                else {
                    //퍼미션 요청
                    ActivityCompat.requestPermissions( this,
                            new String[]{Manifest.permission.CAMERA,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISSIONS_REQUEST_CODE);
                }
            }
            else{

            }
        } else {
            Toast.makeText(MainActivity.this, "Camera not supported",
                    Toast.LENGTH_LONG).show();
        }
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

    private void resetCam() {
        startCamera();
    }

    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    ShutterCallback shutterCallback = new ShutterCallback() {
        public void onShutter() {
            Log.d(TAG, "onShutter'd");
        }
    };

    PictureCallback rawCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "onPictureTaken - raw");
        }
    };

    //참고 : http://stackoverflow.com/q/37135675
    PictureCallback jpegCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            //이미지의 너비와 높이 결정
            int w = camera.getParameters().getPictureSize().width;
            int h = camera.getParameters().getPictureSize().height;
            int orientation = setCameraDisplayOrientation(MainActivity.this,
                    CAMERA_FACING, camera);

            //byte array를 bitmap으로 변환
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeByteArray( data, 0, data.length, options);
            //int w = bitmap.getWidth();
            //int h = bitmap.getHeight();

            //이미지를 디바이스 방향으로 회전
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);
            bitmap =  Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);

            //bitmap을 byte array로 변환
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] currentData = stream.toByteArray();

            //파일로 저장
            new SaveImageTask().execute(currentData);
            resetCam();
            Log.d(TAG, "onPictureTaken - jpeg");
        }
    };

    private class SaveImageTask extends AsyncTask<byte[], Void, Void> {

        @Override
        protected Void doInBackground(byte[]... data) {
            FileOutputStream outStream = null;
            // Write to SD Card
            try {
                File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File (sdCard.getAbsolutePath() + "/camtest");
                dir.mkdirs();
                String fileName = String.format("%d.jpg", System.currentTimeMillis());
                File outFile = new File(dir, fileName);
                outStream = new FileOutputStream(outFile);
                outStream.write(data[0]);
                outStream.flush();
                outStream.close();
                Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to "
                        + outFile.getAbsolutePath());
                refreshGallery(outFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
            return null;
        }
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {
        if ( requestCode == PERMISSIONS_REQUEST_CODE && grandResults.length > 0) {
            int hasCameraPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA);
            int hasWriteExternalStoragePermission =
                    ContextCompat.checkSelfPermission(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if ( hasCameraPermission == PackageManager.PERMISSION_GRANTED
                    && hasWriteExternalStoragePermission == PackageManager.PERMISSION_GRANTED ){
                //이미 퍼미션을 가지고 있음
                doRestart(this);
            }
            else{
                checkPermissions();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        int hasCameraPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);
        int hasWriteExternalStoragePermission =
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
        boolean cameraRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA);
        boolean writeExternalStorageRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if ( (hasCameraPermission == PackageManager.PERMISSION_DENIED && cameraRationale)
                || (hasWriteExternalStoragePermission== PackageManager.PERMISSION_DENIED
                && writeExternalStorageRationale))
            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
        else if ( (hasCameraPermission == PackageManager.PERMISSION_DENIED && !cameraRationale)
                || (hasWriteExternalStoragePermission== PackageManager.PERMISSION_DENIED
                && !writeExternalStorageRationale))
            showDialogForPermissionSetting("퍼미션 거부 + Don't ask again(다시 묻지 않음) " +
                    "체크 박스를 설정한 경우로 설정에서 퍼미션 허가해야합니다.");
        else if ( hasCameraPermission == PackageManager.PERMISSION_GRANTED
                || hasWriteExternalStoragePermission== PackageManager.PERMISSION_GRANTED ) {
            doRestart(this);
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //퍼미션 요청
                ActivityCompat.requestPermissions( MainActivity.this,
                        new String[]{Manifest.permission.CAMERA,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.create().show();
    }

    private void showDialogForPermissionSetting(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(true);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + mActivity.getPackageName()));
                myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
                myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mActivity.startActivity(myAppSettings);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.create().show();
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        //hasFocus : 앱이 화면에 보여졌을때 true로 설정되어 호출됨.
        //만약 그리기 뷰 전역변수에 값이 없을경우 전역변수를 초기화 시킴.
        if(hasFocus && drawLine == null)
        {
            //그리기 뷰가 보여질(나타날) 레이아웃 찾기..
            llCanvas = (LinearLayout)findViewById(R.id.llCanvas);

            if(llCanvas != null) //그리기 뷰가 보여질 레이아웃이 있으면...
            {
                //그리기 뷰 레이아웃의 넓이와 높이를 찾아서 Rect 변수 생성.
                Rect rect = new Rect(0, 0, llCanvas.getMeasuredWidth(), llCanvas.getMeasuredHeight());

                //그리기 뷰 초기화..
                drawLine = new DrawLine(this, rect);

                //그리기 뷰를 그리기 뷰 레이아웃에 넣기 -- 이렇게 하면 그리기 뷰가 화면에 보여지게 됨.
                llCanvas.addView(drawLine);
                mView = llCanvas;
            }

            //이건.. 상단 메뉴(RED, BLUE ~~~)버튼 설정...
            //일단 초기값은 0번(RED)으로.. ^^
            resetCurrentMode(0);
        }

        super.onWindowFocusChanged(hasFocus);
    }

    //코딩 하기 쉽게 하기 위해서.. 사용할 상단 메뉴 버튼들의 아이디를 배열에 넣는다..
    private int[] btns = {R.id.btnRED, R.id.btnBLUE, R.id.btnGREEN, R.id.btnWHITE};
    //코딩 하기 쉽게 하기 위해서.. 상단 메뉴 버튼의 배열과 똑같이 실제 색상값을 배열로 만든다.
    private int[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.WHITE};

    //선택한 색상에 맞도록 버튼의 배경색과 글자색을 바꾸고, 그리기 뷰에도 알려 준다.
    private void resetCurrentMode(int curMode)
    {
        for(int i=0;i<btns.length;i++)
        {
            //이건.. 배열 뒤지면서... 버튼이 있는지 체크..
            Button btn = (Button)findViewById(btns[i]);
            if(btn != null)
            {
                //버튼 있으면 배경색과 글자색 변경..
                //만약 선택한 버튼값과 찾은 버튼이 동일하면 회색배경에 흰색글자 버튼으로 변경.
                //동일하지 않으면 흰색배경에 회색글자 버튼으로 변경.
                btn.setBackgroundColor(i==curMode?0xff555555:0xffffffff);
                btn.setTextColor(i==curMode?0xffffffff:0xff555555);
            }
        }

        //만약 그리기 뷰가 초기화 되었으면, 그리기 뷰에 글자색을 알려줌..
        if(drawLine != null) drawLine.setLineColor(colors[curMode]);
    }

    //버튼을 클릭했을때 호출 되는 함수.
    //이 함수가 호출될때 어떤 버튼(뷰)에서 호출했는지를 같이 알려준다.
    //버튼 클릭시 이 함수를 호출 하게 하기 위해서는...
    //main.xml에서
    //<Button ~~~~ android:onClick="btnClick" ~~~~ />
    //이렇게 btnClick이라는 함수명을 넣어 줘야함.
    public void btnClick(View view)
    {
        if(view == null) return;

        for(int i=0;i<btns.length;i++)
        {
            //배열 뒤지면서 클릭한 버튼이 있는지 확인..
            if(btns[i] == view.getId())
            {
                //만약 선택한 버튼이 있으면.. 버튼모양 및 그리기 뷰 설정을 하기 위해서 함수 호출..
                resetCurrentMode(i);

                //더이상 처리를 할 필요가 없으니까.. for문을 빠져 나옴..
                break;
            }
        }
    }








    private String getTodaysDate() {

        final Calendar c = Calendar.getInstance();
        int todaysDate =     (c.get(Calendar.YEAR) * 10000) +
                ((c.get(Calendar.MONTH) + 1) * 100) +
                (c.get(Calendar.DAY_OF_MONTH));
        Log.w("DATE:",String.valueOf(todaysDate));
        return(String.valueOf(todaysDate));

    }

    private String getCurrentTime() {

        final Calendar c = Calendar.getInstance();
        int currentTime =     (c.get(Calendar.HOUR_OF_DAY) * 10000) +
                (c.get(Calendar.MINUTE) * 100) +
                (c.get(Calendar.SECOND));
        Log.w("TIME:",String.valueOf(currentTime));
        return(String.valueOf(currentTime));

    }


    private boolean prepareDirectory()
    {
        try
        {
            if (makedirs())
            {
                return true;
            } else {
                return false;
            }
        } catch (Exception e)
        {
            e.printStackTrace();
            Toast.makeText(this, "Could not initiate File System.. Is Sdcard mounted properly?", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private boolean makedirs()
    {
        File tempdir = new File(tempDir);
        if (!tempdir.exists())
            tempdir.mkdirs();

        if (tempdir.isDirectory())
        {
            File[] files = tempdir.listFiles();
            for (File file : files)
            {
                if (!file.delete())
                {
                    System.out.println("Failed to delete " + file);
                }
            }
        }
        return (tempdir.isDirectory());
    }






    public class DrawLine extends View {
        //현재 그리기 조건(색상, 굵기, 등등.)을 기억 하는 변수.
        private Paint paint = null;
        //그리기를 할 bitmap 객체. -- 도화지라고 생각하면됨.
        private Bitmap bitmap = null;
        //bitmap 객체의 canvas 객체. 실제로 그리기를 하기 위한 객체.. -- 붓이라고 생각하면됨.
        private Canvas canvas = null;
        //마우스 포인터(손가락)이 이동하는 경로 객체.
        private Path path;
        //마우스 포인터(손가락)이 가장 마지막에 위치한 x좌표값 기억용 변수.
        private float   oldX;
        //마우스 포인터(손가락)이 가장 마지막에 위치한 y좌표값 기억용 변수.
        private float   oldY;


        public void save(View v)
        {
            Log.v("log_tag", "Width: " + v.getWidth());
            Log.v("log_tag", "Height: " + v.getHeight());
            if(bitmap == null)
            {
                bitmap =  Bitmap.createBitmap (bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);;
            }
            Canvas canvas = new Canvas(bitmap);
            try
            {
                FileOutputStream mFileOutStream = new FileOutputStream(mypath);

                v.draw(canvas);
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, mFileOutStream);
                mFileOutStream.flush();
                mFileOutStream.close();
                String url = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "title", null);
                Log.v("log_tag","url: " + url);
                //In case you want to delete the file
                //boolean deleted = mypath.delete();
                //Log.v("log_tag","deleted: " + mypath.toString() + deleted);
                //If you want to convert the image to string use base64 converter

            }
            catch(Exception e)
            {
                Log.v("log_tag", e.toString());
            }
        }

        public DrawLine(Context context, Rect rect) {
            this(context);
            //그리기를 할 bitmap 객체 생성.
            bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
            //그리기 bitmap에서 canvas를 알아옴.
            canvas = new Canvas(bitmap);
            //경로 초기화.
            path = new Path();
        }

        @Override
        protected void onDetachedFromWindow() {
            //앱 종료시 그리기 bitmap 초기화 시킴...
            if(bitmap!= null) bitmap.recycle();
            bitmap = null;
            super.onDetachedFromWindow();
        }

        @Override
        public void onDraw(Canvas canvas) {
            //그리기 bitmap이 있으면 현재 화면에 bitmap을 그린다.
            //자바의 view는 onDraw할때 마다 화면을 싹 지우고 다시 그리게 됨.
            if(bitmap != null) {
                canvas.drawBitmap(bitmap, 0, 0, null);
            }
        }

        //이벤트 처리용 함수..
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN: {
                    //최초 마우스를 눌렀을때(손가락을 댓을때) 경로를 초기화 시킨다.
                    path.reset();
                    //그다음.. 현재 경로로 경로를 이동 시킨다.
                    path.moveTo(x, y);
                    //포인터 위치값을 기억한다.
                    oldX = x;
                    oldY = y;
                    //계속 이벤트 처리를 하겠다는 의미.
                    return true;
                } case MotionEvent.ACTION_MOVE: {
                    //포인트가 이동될때 마다 두 좌표(이전에눌렀던 좌표와 현재 이동한 좌료)간의 간격을 구한다.
                    float dx = Math.abs(x - oldX);
                    float dy = Math.abs(y - oldY);
                    //두 좌표간의 간격이 4px이상이면 (가로든, 세로든) 그리기 bitmap에 선을 그린다.
                    if (dx >= 4 || dy >= 4) {
                        //path에 좌표의 이동 상황을 넣는다. 이전 좌표에서 신규 좌표로..
                        //lineTo를 쓸수 있지만.. 좀더 부드럽게 보이기 위해서 quadTo를 사용함.
                        path.quadTo(oldX, oldY, x, y);
                        //포인터의 마지막 위치값을 기억한다.
                        oldX = x;
                        oldY = y;
                        //그리기 bitmap에 path를 따라서 선을 그린다.
                        canvas.drawPath(path, paint);
                    }
                    //화면을 갱신시킴.. 이 함수가 호출 되면 onDraw 함수가 실행됨.
                    invalidate();
                    //계속 이벤트 처리를 하겠다는 의미.
                    return true;
                }
            }
            //더이상 이벤트 처리를 하지 않겠다는 의미.
            return false;
        }

        /**
         * 펜 색상 세팅
         * @param color 색상
         */
        public void setLineColor(int color) {
            paint = new Paint();
            paint.setColor(color);
            paint.setAlpha(255);
            paint.setDither(true);
            paint.setStrokeWidth(10);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setAntiAlias(true);
        }

        public DrawLine(Context context) {
            super(context);
        }
    }
}