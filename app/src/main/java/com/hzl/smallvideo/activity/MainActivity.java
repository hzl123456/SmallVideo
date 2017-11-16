package com.hzl.smallvideo.activity;

import android.Manifest;
import android.app.Activity;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.VideoView;

import com.hzl.smallvideo.R;
import com.hzl.smallvideo.application.MainApplication;
import com.hzl.smallvideo.manager.RecordManager;
import com.hzl.smallvideo.manager.camera.CameraSurfaceView;
import com.hzl.smallvideo.manager.camera.CaptureButton;
import com.hzl.smallvideo.manager.listener.CameraPictureListener;
import com.hzl.smallvideo.manager.listener.RecordFinishListener;
import com.hzl.smallvideo.util.AppUtil;
import com.hzl.smallvideo.util.DialogUtil;
import com.hzl.smallvideo.util.PermissionsUtils;

import java.io.File;

public class MainActivity extends Activity implements View.OnClickListener {

    private final int REQUEST_CODE_PERMISSIONS = 10;

    private CameraSurfaceView mSurfaceView;
    private CaptureButton mBtnStart;
    private VideoView mVideoView;
    private ImageView mBtnCamera;
    private ImageView mBtnLight;
    private ImageView ivImage;

    //音视频的处理的类
    private RecordManager mRecordManager;

    private boolean isLighting;

    //拍照的图片
    private Bitmap bitmap;
    //合成视频的路径
    private String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainApplication.setCurrentActivity(this);
        //设置底部虚拟状态栏为透明，并且可以充满，4.4以上才有
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
        //权限申请使用
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            final String[] PERMISSIONS;
            PERMISSIONS = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
            PermissionsUtils.checkAndRequestMorePermissions(this, PERMISSIONS, REQUEST_CODE_PERMISSIONS,
                    new PermissionsUtils.PermissionRequestSuccessCallBack() {
                        @Override
                        public void onHasPermission() {
                            setContentView(R.layout.activity_main);
                            initView();
                        }
                    });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (PermissionsUtils.isPermissionRequestSuccess(grantResults)) {
            setContentView(R.layout.activity_main);
            initView();
        }
    }

    private void initView() {
        mSurfaceView = (CameraSurfaceView) findViewById(R.id.camera_surface);
        mBtnStart = (CaptureButton) findViewById(R.id.btn_start);
        mVideoView = (VideoView) findViewById(R.id.video_view);
        mBtnCamera = (ImageView) findViewById(R.id.btn_camera);
        mBtnLight = (ImageView) findViewById(R.id.btn_light);
        ivImage = (ImageView) findViewById(R.id.iv_image);

        mBtnCamera.setOnClickListener(this);
        mBtnLight.setOnClickListener(this);

        mRecordManager = new RecordManager(mSurfaceView);

        mBtnStart.setCaptureListener(new CaptureButton.CaptureListener() {
            @Override
            public void capture() { //进行拍照
                //进行拍照
                mRecordManager.takePicture(new CameraPictureListener() {

                    @Override
                    public void onPictureBitmap(Bitmap btmp) {
                        //完成拍照了，通知控件进行按钮控件的更新
                        mBtnStart.showControllerButtons();
                        //这里得到正确旋转和翻转之后的图片
                        MainActivity.this.bitmap = btmp;
                        ivImage.setImageBitmap(btmp);
                        ivImage.setVisibility(View.VISIBLE);
                        //显示到界面上去，然后刷新camera
                        mRecordManager.onStop();
                        mRecordManager.onResume();
                    }
                });
            }

            @Override
            public void cancel() { //拍照取消
                MainActivity.this.bitmap = null;
                ivImage.setVisibility(View.GONE);
            }

            @Override
            public void determine() { //拍照确定
                final String filePath = Environment.getExternalStorageDirectory().getPath() + File.separator + System.currentTimeMillis() + ".png";
                AppUtil.saveBitmapToFile(MainActivity.this.bitmap, filePath);
                DialogUtil.showToast("图片保存成功");
                ivImage.setVisibility(View.GONE);
            }

            @Override
            public void record() { //开始录制
                //开始录制
                mRecordManager.startRecord();
            }

            @Override
            public void rencodEnd(final boolean isShortTime) { //停止录制
                //录制结束
                mRecordManager.stopRecord(new RecordFinishListener() {
                    @Override
                    public void onRecordFinish(String filePath) {
                        if (isShortTime) {
                            //删除录制的小视频
                            new File(filePath).delete();
                            DialogUtil.showToast("录制时间太短");
                        } else {
                            //得到合成的mp4的文件路径
                            MainActivity.this.filePath = filePath;
                            //完成录制了，通知控件进行按钮控件的更新
                            mBtnStart.showControllerButtons();
                            //进行小视频的循环播放
                            startVideo(filePath);
                        }
                    }
                });
            }

            @Override
            public void getRecordResult() { //需要录制
                DialogUtil.showToast("视频保存成功");
                mVideoView.stopPlayback();
                mVideoView.setVisibility(View.GONE);
            }

            @Override
            public void deleteRecordResult() {//删除录制
                new File(filePath).delete();
                mVideoView.stopPlayback();
                mVideoView.setVisibility(View.GONE);
            }

            @Override
            public void actionRecord() { //对视频和图片进行操作，主要是添加水印
                DialogUtil.showToast("该功能暂未开放");
            }
        });
    }

    public void startVideo(String videoPath) {
        mVideoView.setVisibility(View.VISIBLE);
        mVideoView.setZOrderMediaOverlay(true);
        mVideoView.setVideoPath("file://" + videoPath);
        mVideoView.start();
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
                mp.start();
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
