package com.hzl.smallvideo.activity;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.hzl.smallvideo.R;
import com.hzl.smallvideo.application.MainApplication;
import com.hzl.smallvideo.manager.RecordManager;
import com.hzl.smallvideo.manager.camera.CameraSurfaceView;
import com.hzl.smallvideo.manager.camera.CaptureButton;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity implements View.OnClickListener {

    private CameraSurfaceView mSurfaceView;

    private ImageView mBtnCamera;

    private ImageView mBtnLight;

    private CaptureButton mBtnStart;

    //音视频的处理的类
    private RecordManager mRecordManager;

    private boolean isLighting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainApplication.setCurrentActivity(this);
        setContentView(R.layout.activity_main);
        initView();
        //设置底部虚拟状态栏为透明，并且可以充满，4.4以上才有
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
    }

    private void initView() {
        mSurfaceView = (CameraSurfaceView) findViewById(R.id.camera_surface);
        mBtnCamera = (ImageView) findViewById(R.id.btn_camera);
        mBtnLight = (ImageView) findViewById(R.id.btn_light);
        mBtnStart = (CaptureButton) findViewById(R.id.btn_start);

        mBtnCamera.setOnClickListener(this);
        mBtnLight.setOnClickListener(this);

        mRecordManager = new RecordManager(mSurfaceView);

        mBtnStart.setCaptureListener(new CaptureButton.CaptureListener() {
            @Override
            public void capture() { //进行拍照
                //进行拍照
                mRecordManager.takePicture();
            }

            @Override
            public void cancel() { //拍照取消
                //TODO 删除bitmap，然后重新刷新下
            }

            @Override
            public void determine() { //拍照确定
                //TODO 保存bitmap，然后刷新下
            }

            @Override
            public void record() { //开始录制
                //开始录制
                mRecordManager.startRecord();
            }

            @Override
            public void rencodEnd() { //停止录制
                //录制结束
                mRecordManager.stopRecord();

            }

            @Override
            public void getRecordResult() { //需要录制

            }

            @Override
            public void deleteRecordResult() {//删除录制

            }
        });
    }


    @Override
    public void onClick(View v) {
        if (v == mBtnCamera) { //摄像头旋转
            int cameraType = mRecordManager.changeCamera();
            //如果是前置摄像头就不需要闪光灯，如果是后置的才需要闪关灯
            if (cameraType == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mBtnLight.setVisibility(View.GONE);
            } else {
                mBtnLight.setVisibility(View.VISIBLE);
            }
            mBtnLight.setImageResource(R.mipmap.light_close);
            isLighting = false;
            mRecordManager.setLightingState(false);
        } else if (v == mBtnLight) {
            if (isLighting) {
                mBtnLight.setImageResource(R.mipmap.light_close);
            } else {
                mBtnLight.setImageResource(R.mipmap.light_open);
            }
            isLighting = !isLighting;
            mRecordManager.setLightingState(isLighting);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRecordManager.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mRecordManager.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRecordManager.onDestroy();
    }
}
