package com.hzl.smallvideo.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.hzl.smallvideo.R;
import com.hzl.smallvideo.application.MainApplication;
import com.hzl.smallvideo.manager.RecordManager;
import com.hzl.smallvideo.manager.camera.CameraSurfaceView;

public class MainActivity extends Activity implements View.OnClickListener {

    private CameraSurfaceView mSurfaceView;
    private Button mBtnCamera;
    private Button mBtnStart;

    //音视频的处理的类
    private RecordManager mRecordManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainApplication.setCurrentActivity(this);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        mSurfaceView = (CameraSurfaceView) findViewById(R.id.camera_surface);
        mBtnCamera = (Button) findViewById(R.id.btn_camera);
        mBtnStart = (Button) findViewById(R.id.btn_start);

        mBtnCamera.setOnClickListener(this);
        mBtnStart.setOnClickListener(this);

        mRecordManager = new RecordManager(mSurfaceView);
    }


    @Override
    public void onClick(View v) {
        if (v == mBtnCamera) { //摄像头旋转
            mRecordManager.changeCamera();
        } else if (v == mBtnStart) {//开启录制
            if (mBtnStart.getText().toString().equals("开启录制")) {
                mBtnStart.setText("停止录制");
                mRecordManager.startRecord();
            } else {
                mBtnStart.setText("开启录制");
                mRecordManager.stopRecord();
            }
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
