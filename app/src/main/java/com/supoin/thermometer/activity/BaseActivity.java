package com.supoin.thermometer.activity;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.supoin.thermometer.R;
import com.supoin.thermometer.service.ResService;
import com.supoin.thermometer.widget.DrawHelper;
import com.supoin.thermometer.widget.FaceRectView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.supoin.thermometer.service.ResService.setCamData;

public class BaseActivity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnClickListener {

    private static final int IMG_WIDTH = 1920;//图面宽度
    private static final int IMG_HEIGHT = 1080;//图片高度
    private static final int IMG_ORIENTATION = 90;//图片旋转角度
    private SurfaceView mSurfaceView;
    public DrawHelper drawHelper;
    private Camera mCamera;

    private byte[] imgData = null;
    private Paint mPaint;
    private ImageView imageCircle;
    private ImageView imgFaceIcon;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        Intent intentService = new Intent(this, ResService.class);
        startService(intentService);
        //权限
        initPermissions();
        initView();
        initCamera();
        handleRotate();
    }

    private void initView() {
        mSurfaceView = findViewById(R.id.sv_camera);
        imageCircle = findViewById(R.id.img_circle);
        imgFaceIcon = findViewById(R.id.image_face_icon);
    }
    //测温区转动效果
    public void handleRotate(){
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(imageCircle, "rotation", 0f, 360f);
        objectAnimator.setDuration(1000);
        objectAnimator.setRepeatMode(ValueAnimator.RESTART);
        objectAnimator.setInterpolator(new LinearInterpolator());
        objectAnimator.setRepeatCount(ValueAnimator.INFINITE);
        objectAnimator.start();
    }

    //申请所有需要的权限
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initPermissions() {
        if(checkSelfPermission(Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.INTERNET)!= PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.INTERNET,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_PHONE_STATE},1);
        }
    }

    //初始化相机
    private void initCamera() {
        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.setFixedSize(IMG_WIDTH,IMG_HEIGHT);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mCamera = Camera.open(1);
        Log.d("camera","打开相机");
        initFaceRect();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        if (mCamera != null){
            Camera.Parameters param = mCamera.getParameters();
            param.setPreviewFormat(ImageFormat.NV21);
            param.setPreviewSize(IMG_WIDTH,IMG_HEIGHT);
            mCamera.setParameters(param);
            mCamera.setDisplayOrientation(IMG_ORIENTATION);
        }
        assert mCamera != null;
        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes, Camera camera) {
                imgData = bytes;
                setCamData(imgData);
            }
        });
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if(mCamera!=null){
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private void initFaceRect() {
        drawHelper = new DrawHelper(IMG_WIDTH,IMG_HEIGHT,mSurfaceView.getWidth(),mSurfaceView.getHeight(),IMG_ORIENTATION,1,false,false,false);
    }

    @Override
    public void onClick(View view) {
    }
}
