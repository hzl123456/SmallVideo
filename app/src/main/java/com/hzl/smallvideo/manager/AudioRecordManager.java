package com.hzl.smallvideo.manager;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;

import com.hzl.smallvideo.manager.api.MangerApi;
import com.hzl.smallvideo.util.AppUtil;
import com.hzl.smallvideo.util.FFmpegUtil;

import java.io.File;
import java.io.IOException;

/**
 * 作者：请叫我百米冲刺 on 2017/10/23 上午9:07
 * 邮箱：mail@hezhilin.cc
 * <p>
 * 采集原始的pcm音频数据的操作类
 */
public class AudioRecordManager implements MangerApi {

    private String aacPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "ffmpeg.aac";

    private static AudioRecordManager mInstance;

    private Thread recordThread;
    private AudioRecord mRecorder;

    private volatile boolean isRunning;
    private volatile boolean isFirstOnDrawFrame = true;

    private int bufferSize;

    public AudioRecordManager() {
        bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
    }

    public static AudioRecordManager getInstance() {
        if (mInstance == null) {
            synchronized (AudioRecordManager.class) {
                if (mInstance == null) {
                    mInstance = new AudioRecordManager();
                }
            }
        }
        return mInstance;
    }

    /**
     * 录音线程
     */
    Runnable recordRunnable = new Runnable() {
        @Override
        public void run() {
            if (mRecorder == null) {
                mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            }
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            int bytesRecord;
            //如果是单声道的话用2048，立体声的话用4096
            byte[] tempBuffer = new byte[2048];
            if (mRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                stopRecord();
                return;
            }
            mRecorder.startRecording();
            while (isRunning) {
                bytesRecord = mRecorder.read(tempBuffer, 0, 2048);
                if (bytesRecord == AudioRecord.ERROR_INVALID_OPERATION || bytesRecord == AudioRecord.ERROR_BAD_VALUE) {
                    continue;
                }
                if (bytesRecord != 0 && bytesRecord != -1) {
                    FFmpegUtil.pushDataToAACFile(tempBuffer);
                } else {
                    break;
                }
            }
        }
    };

    @Override
    public void onResume() {

    }

    @Override
    public void onStop() {
    }

    @Override
    public void onDestroy() {
        stopRecord();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
    }

    /**
     * 启动录音
     */
    @Override
    public void startRecord() {
        //创建一些数据
        if (isFirstOnDrawFrame) {
            try {
                //创建pcm文件
                File file = new File(aacPath);
                if (file.exists()) {
                    file.delete();
                }
                file.createNewFile();
                //实例化生成aac的操作
                FFmpegUtil.initAACFile(aacPath, AppUtil.getCpuCores());
            } catch (IOException e) {
                e.printStackTrace();
            }
            recordThread = null;
            isFirstOnDrawFrame = false;
        }
        if (recordThread == null) {
            recordThread = new Thread(recordRunnable);
        }
        if (!recordThread.isAlive()) {
            isRunning = true;
            recordThread.start();
        }
    }

    /**
     * 停止录音
     */
    @Override
    public void stopRecord() {
        isRunning = false;
        //表示要重新去操作了
        isFirstOnDrawFrame = true;
        //在这里只需要停止就好了，，只有退出应用的时候才需要释放
        if (mRecorder != null && mRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
            mRecorder.stop();
        }
        //结束aac的编码操作
        FFmpegUtil.endEcodeAAC();
    }
}
