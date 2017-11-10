package com.hzl.smallvideo.manager;

import android.os.Environment;

import com.hzl.smallvideo.manager.camera.CameraSurfaceView;
import com.hzl.smallvideo.manager.listener.RecordListener;
import com.hzl.smallvideo.util.DialogUtil;
import com.hzl.smallvideo.util.FFmpegUtil;

import java.io.File;
import java.io.IOException;

/**
 * 作者：请叫我百米冲刺 on 2017/11/7 上午11:28
 * 邮箱：mail@hezhilin.cc
 */
@SuppressWarnings("deprecation")
public class RecordManager extends RecordListener {

    private VideoRecordManager mVideoRecordManager;
    private AudioRecordManager mAudioRecordManager;

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
            final String filePath = Environment.getExternalStorageDirectory().getPath() + File.separator + "ffmpeg" + System.currentTimeMillis() + ".mp4";
            //创建pcm文件
            final File file = new File(filePath);
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    FFmpegUtil.getMP4File(mVideoRecordManager.getFilePath(), mAudioRecordManager.getFilePath(), filePath);
                    //完成之后删除h264和aac
                    new File(mVideoRecordManager.getFilePath()).delete();
                    new File(mAudioRecordManager.getFilePath()).delete();
                    DialogUtil.showToast("视频录制成功");
                    isVideoComplete = false;
                    isAudioComplete = false;
                }
            }).start();
        }
    }

    public void takePicture() {mVideoRecordManager.takePicture();}

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

    public void stopRecord() {
        mVideoRecordManager.stopRecord();
        mAudioRecordManager.stopRecord();
    }
}
