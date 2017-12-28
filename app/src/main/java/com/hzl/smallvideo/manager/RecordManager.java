package com.hzl.smallvideo.manager;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.hzl.smallvideo.listener.CameraPictureListener;
import com.hzl.smallvideo.listener.RecordFinishListener;
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
public class RecordManager {

    //生成的mp4文件
    private String filePath;

    //小视频录制的时长为15秒,给一个100ms的偏移量
    public static final float RECORD_TIME = 15.1f;

    private VideoRecordManager mVideoRecordManager;
    private AudioRecordManager mAudioRecordManager;

    private RecordFinishListener mRecordFinishListener;

    public RecordManager(CameraSurfaceView mSurfaceView) {
        mVideoRecordManager = new VideoRecordManager(mSurfaceView);
        mAudioRecordManager = AudioRecordManager.getInstance();
    }

    public int changeCamera() {
        return mVideoRecordManager.changeCamera();
    }

    public void initMp4File() {
        filePath = Environment.getExternalStorageDirectory().getPath() + File.separator + System.currentTimeMillis() + ".mp4";
        //创建文件
        try {
            new File(filePath).createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FFmpegUtil.initMP4File(filePath, this, "encodeComplete");
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
        initMp4File();
        mVideoRecordManager.startRecord();
        mAudioRecordManager.startRecord();
    }

    public void stopRecord(RecordFinishListener mRecordFinishListener) {
        this.mRecordFinishListener = mRecordFinishListener;
        CommonUtil.showDialog("生成视频中");
        mVideoRecordManager.stopRecord();
        mAudioRecordManager.stopRecord();
    }


    /**
     * 给jni进行调用的回调函数啊啥的
     **/
    public void encodeComplete() {
        CommonUtil.disMissDialog();
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (mRecordFinishListener != null) {
                    mRecordFinishListener.onRecordFinish(filePath);
                }
            }
        });
    }
}
