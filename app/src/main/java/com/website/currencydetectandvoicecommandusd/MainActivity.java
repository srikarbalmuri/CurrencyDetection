package com.website.currencydetectandvoicecommandusd;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private int REQUEST_CODE_PERMISSIONS = 10; //arbitrary number, can be changed accordingly
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};

    private Camera mCamera;
    private CameraPreview mPreview;
    FrameLayout preview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (allPermissionsGranted()) {
            startCamera(); //start camera if permission has been granted by user

        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    public void startCamera() {
        try {
            mCamera = getCameraInstance("Back", this);
            if (mCamera != null) {
                mPreview = new CameraPreview(MainActivity.this, mCamera);
                preview = (FrameLayout) findViewById(R.id.cam_surface);
                Camera.Parameters parameters = mCamera.getParameters();
                List<String> focusModes = parameters.getSupportedFocusModes();
                if(focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                } else
                if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)){
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }

                mCamera.setParameters(parameters);
                preview.addView(mPreview);
                try {
                    new Handler().postDelayed(new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public void run() {
                            mCamera.takePicture(null, null, new Camera.PictureCallback() {
                                @Override
                                public void onPictureTaken(byte[] bytes, Camera camera) {
                                    Bitmap pic = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                                    camera.stopPreview();
                                    camera.startPreview();

                                    String Fname = "";
                                    try {
                                        //File file = Environment.getExternalStoragePublicDirectory(
                                              //  Environment.DIRECTORY_PICTURES);
                                        File file = new File("/sdcard/CurrencyUSD/Photos");
                                        Log.i("TAG", "onPictureTaken: "+file.mkdirs());
                                        String filename = "IMG" + System.currentTimeMillis() + ".jpg";
                                        file.mkdirs();
                                        File finalfile = new File(file.getAbsolutePath(), filename);
                                        Log.i("TAG", "onPictureTaken: "+ finalfile.getAbsolutePath());
                                        Fname = finalfile.getAbsolutePath();
                                        OutputStream out = new FileOutputStream(finalfile);
                                        pic.compress(Bitmap.CompressFormat.JPEG, 100, out);
                                        out.flush();
                                        out.close();
                                        getResultsBackToPrevActivity(Fname);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    }, 6000);

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "camera null", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getResultsBackToPrevActivity(String filename) {
        Intent intent = new Intent();
        intent.putExtra(Utils.CAM_RESULT_OK_KEY, filename);
        setResult(RESULT_OK, intent);
        finish();
    }

    public static Camera getCameraInstance(String mode, Context con) {
        Camera c = null;
        try {
            if (mode.compareTo("Front") == 0) {
                c = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            } else if (mode.compareTo("Back") == 0) {
                c = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            }
        } catch (Exception e) {
            Toast.makeText(con, "getinstance:" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return c;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera != null) {
            mPreview = new CameraPreview(MainActivity.this, mCamera);
            preview = (FrameLayout) findViewById(R.id.cam_surface);
            preview.addView(mPreview);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null) {
//            mCamera.release();
            mCamera.stopPreview();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        //start camera when permissions have been granted otherwise exit app
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted() {
        //check if req permissions have been granted
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}