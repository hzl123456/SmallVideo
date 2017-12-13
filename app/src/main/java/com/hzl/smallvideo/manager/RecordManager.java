package com.hzl.smallvideo.manager;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.hzl.smallvideo.listener.CameraPictureListener;
import com.hzl.smallvideo.listener.RecordFinishListener;
import com.hzl.smallvideo.listener.RecordListener;
import com.hzl.smallvideo.manager.camera.CameraSurfaceView;
import com.hzl.smallvideo.util.CommonUtil;
import com.hzl.smallvideo.util.FFmpegUtil;

import java.io.File;
import java.io.IOException;

/**
 * 作者：请叫我百米冲刺 on 2017/11/7 上午11:28
 * 邮箱：mail@hezhilin.cc
 */
@SuppressWarnings("deprecation")
public class RecordManager extends RecordListener {

    //小视频录制的时长为15秒,给一个100ms的偏移量
    public static final float RECORD_TIME = 15.1f;

    private VideoRecordManager mVideoRecordManager;
    private AudioRecordManager mAudioRecordManager;
    private RecordFinishListener mRecordFinishListener;

    private boolean isVideoComplete;
    private boolean isAudioComplete;


    public RecordManager(CameraSurfaceView mSurfaceView) {
        mVideoRecordManager = new VideoRecordManager(mSurfaceView);
        mAudioRecordManager = AudioRecordManager.getInstance();

        mVideoRecordManager.setRecordListener(this);
        mAudioRecordManager.setRecordListener(this);
    }

    public int changeCamera() {
        return mVideoRecordManager.changeCamera();
    }

    @Override
    public void videoComplete() {
        isVideoComplete = true;
        getMP4File();
    }

    @Override
    public void audioComplete() {
        isAudioComplete = true;
        getMP4File();
    }

    public synchronized void getMP4File() {
        if (isVideoComplete && isAudioComplete) {
            final String filePath = Environment.getExternalStorageDirectory().getPath() + File.separator + System.currentTimeMillis() + ".mp4";
            //创建文件
            try {
                new File(filePath).createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    FFmpegUtil.getMP4File(filePath, mVideoRecordManager.getTimeList());
                    isVideoComplete = false;
                    isAudioComplete = false;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            CommonUtil.disMissDialog();
                            if (mRecordFinishListener != null) {
                                mRecordFinishListener.onRecordFinish(filePath);
                            }
                        }
                    });
                }
            }).start();

        }
    }

    public void takePicture(CameraPictureListener listener) {
        mVideoRecordManager.takePicture(listener);
    }

    public void setLightingState(boolean isOpen) {
        mVideoRecordManager.setLightState(isOpen);
    }

    public void onResume() {
        mVideoRecordManager.onResume();
        mAudioRecordManager.onResume();
    }

    public void onStop() {
        mVideoRecordManager.onStop();
        mAudioRecordManager.onStop();
    }

    public void onDestroy() {
        mVideoRecordManager.onDestroy();
        mAudioRecordManager.onDestroy();
    }

    public void startRecord() {
        mVideoRecordManager.startRecord();
        mAudioRecordManager.startRecord();
    }

    public void pauseRecord() {
        mVideoRecordManager.pauseRecord();
        mAudioRecordManager.pauseRecord();
    }

    public void stopRecord(RecordFinishListener mRecordFinishListener) {
        this.mRecordFinishListener = mRecordFinishListener;
        CommonUtil.showDialog("生成视频中");
        mVideoRecordManager.stopRecord();
        mAudioRecordManager.stopRecord();
    }
}
