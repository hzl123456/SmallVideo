package com.hzl.smallvideo.manager;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.util.Log;

import com.hzl.smallvideo.application.MainApplication;
import com.hzl.smallvideo.listener.CameraPictureListener;
import com.hzl.smallvideo.listener.CameraYUVDataListener;
import com.hzl.smallvideo.listener.RecordListener;
import com.hzl.smallvideo.manager.api.MangerApi;
import com.hzl.smallvideo.manager.camera.CameraSurfaceView;
import com.hzl.smallvideo.util.AppUtil;
import com.hzl.smallvideo.util.CameraUtil;
import com.hzl.smallvideo.util.FFmpegUtil;
import com.libyuv.util.YuvUtil;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static android.content.Context.SENSOR_SERVICE;

/**
 * 作者：请叫我百米冲刺 on 2017/11/7 上午10:52
 * 邮箱：mail@hezhilin.cc
 */
@SuppressWarnings("deprecation")
public class VideoRecordManager implements MangerApi, SensorEventListener, CameraYUVDataListener {

    private String filePath = Environment.getExternalStorageDirectory().getPath() + File.separator + "ffmpeg.h264";

    private CameraSurfaceView mCameraSurfaceView;
    private CameraUtil mCameraUtil;
    private long startTime;//开始的时间
    private double time;//编码的时长，最后要根据这个去计算平均的fps，这里的单位用的是秒
    private int count; //编码的总的帧数，最后要根据这个去计算平均的fps

    //输出的宽高,这里给的是540p
    private int outWidth = 540;
    private int outHeight = 960;

    private volatile boolean isRunning;
    private volatile boolean isFirstOnDrawFrame = true;
    private BlockingQueue<byte[]> yuvList;
    private boolean isPause;
    private byte[] yuvData;
    private Thread yuvThread;
    private RecordListener listener;

    //传感器需要，这边使用的是重力传感器
    private SensorManager mSensorManager;
    private boolean mInitialized = false; //第一次实例化的时候是不需要的
    private float mLastX = 0f;
    private float mLastY = 0f;
    private float mLastZ = 0f;

    public VideoRecordManager(CameraSurfaceView cameraSurfaceView) {
        mCameraSurfaceView = cameraSurfaceView;
        mCameraUtil = cameraSurfaceView.getCameraUtil();
        mCameraSurfaceView.setCameraYUVDataListener(this);

        mSensorManager = (SensorManager) MainApplication.getInstance().getSystemService(SENSOR_SERVICE);
    }

    public int getCameraFps() {
        return mCameraUtil.getFrameRate();
    }

    public int changeCamera() {
        return mCameraSurfaceView.changeCamera();
    }

    public void setLightState(boolean isOpen) {
        mCameraSurfaceView.setLightingState(isOpen);
    }

    public void takePicture(CameraPictureListener listener) {
        mCameraSurfaceView.takePicture(listener);
    }

    @Override
    public void onResume() {
        //打开摄像头
        mCameraSurfaceView.openCamera();
        //注册加速度传感器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onStop() {
        //释放摄像头
        mCameraSurfaceView.releaseCamera();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void startRecord() {
        if (isFirstOnDrawFrame) {
            //实例化yuvutil
            YuvUtil.init(mCameraUtil.getCameraHeight(), mCameraUtil.getCameraWidth(), outWidth, outHeight);
//            //实例化水印的一些参数,默认是采用argb_888去读取一张图片
//            Bitmap bitmap = BitmapFactory.decodeResource(MainApplication.getInstance().getResources(), R.mipmap.ic_launcher_round);
//            ByteBuffer buffer = ByteBuffer.allocate(bitmap.getByteCount());
//            bitmap.copyPixelsToBuffer(buffer);
//            int startX = 0;
//            int startY = 0;
//            YuvUtil.initWaterMark(buffer.array(), bitmap.getWidth(), bitmap.getHeight(), startX, startY);
            //实例化ffmpeg的信息
            try {
                File file = new File(filePath);
                if (file.isFile()) {
                    file.delete();
                }
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            FFmpegUtil.initH264File(filePath, mCameraUtil.getFrameRate(), outWidth, outHeight, AppUtil.getCpuCores());
            //一些数据的初始化操作
            yuvList = new LinkedBlockingQueue<>();
            yuvData = new byte[outWidth * outHeight * 3 / 2];
            yuvThread = null;
            isFirstOnDrawFrame = false;
        }
        isRunning = true;
        isPause = false;
        count = 0;
        startTime = System.currentTimeMillis();
    }

    @Override
    public void pauseRecord() {
        isPause = true;
    }

    @Override
    public void stopRecord() {
        time = (System.currentTimeMillis() - startTime) / (double) 1000;
        isRunning = false;
        isPause = false;
        //表示要重新去操作了
        isFirstOnDrawFrame = true;
        stopTime = System.currentTimeMillis();
    }

    private long stopTime;

    @Override
    public String getFilePath() {
        return filePath;
    }

    @Override
    public void setRecordListener(RecordListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCallback(byte[] data) {
        if (!isRunning || isPause) { //如果是没有开启录制和暂停就进行返回
            return;
        }
        //添加数据
        try {
            yuvList.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (yuvThread == null) {
            yuvThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            if (yuvList.size() > 0) {
                                byte[] data = yuvList.take();
                                if (data != null) {
                                    long time = System.currentTimeMillis();
                                    int morientation = mCameraUtil.getMorientation();
                                    YuvUtil.compressYUV(data, mCameraUtil.getCameraWidth(), mCameraUtil.getCameraHeight(), yuvData, outHeight, outWidth, 0, morientation, morientation == 270 ? mCameraUtil.isMirror() : false);
                                    FFmpegUtil.pushDataToH264File(yuvData);
                                    Log.i("dddd->", (System.currentTimeMillis() - time) + "");
                                    count++;
                                }
                            } else if (!isRunning && !isPause) {
                                FFmpegUtil.getH264File();
                                Log.i("dddd->", "time:" + (System.currentTimeMillis() - stopTime));
                                Log.i("ddddssss->", "count:" + count + ",time:" + time + ",fps:" + (count / time));
                                if (listener != null) {
                                    listener.videoComplete(count / time);
                                }
                                break;
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            yuvThread.start();
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        if (!mInitialized) {
            mLastX = x;
            mLastY = y;
            mLastZ = z;
            mInitialized = true;
        }

        float deltaX = Math.abs(mLastX - x);
        float deltaY = Math.abs(mLastY - y);
        float deltaZ = Math.abs(mLastZ - z);

        if (mCameraSurfaceView != null && (deltaX > 0.6 || deltaY > 0.6 || deltaZ > 0.6)) {
            mCameraSurfaceView.startAutoFocus(-1, -1);
        }

        mLastX = x;
        mLastY = y;
        mLastZ = z;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
