package com.hzl.smallvideo.manager;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.hzl.smallvideo.manager.camera.CameraSurfaceView;
import com.hzl.smallvideo.manager.listener.CameraPictureListener;
import com.hzl.smallvideo.manager.listener.RecordFinishListener;
import com.hzl.smallvideo.manager.listener.RecordListener;
import com.hzl.smallvideo.util.FFmpegUtil;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * 作者：请叫我百米冲刺 on 2017/11/7 上午11:28
 * 邮箱：mail@hezhilin.cc
 */
@SuppressWarnings("deprecation")
public class RecordManager extends RecordListener {

    //小视频录制的时长为15秒,给一个100ms的偏移量
    public static final float RECORD_TIME = 15.5f;

    private VideoRecordManager mVideoRecordManager;
    private AudioRecordManager mAudioRecordManager;
    private RecordFinishListener mRecordFinishListener;

    private boolean isVideoComplete;
    private boolean isAudioComplete;

    //最后视频的平均的fps的值
    private double fps;

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
    public void videoComplete(double fps) {
        //保留两位小数
        this.fps = BigDecimal.valueOf(fps).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
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
                    FFmpegUtil.getMP4File(mVideoRecordManager.getFilePath(), mAudioRecordManager.getFilePath(), filePath, mVideoRecordManager.getCameraFps(), RecordManager.this.fps);
                    //完成之后删除h264和aac
                    new File(mVideoRecordManager.getFilePath()).delete();
                    new File(mAudioRecordManager.getFilePath()).delete();
                    isVideoComplete = false;
                    isAudioComplete = false;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
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
        mVideoRecordManager.stopRecord();
        mAudioRecordManager.stopRecord();
    }
}
