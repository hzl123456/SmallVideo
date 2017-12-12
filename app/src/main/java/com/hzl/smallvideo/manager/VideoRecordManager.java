package com.hzl.smallvideo.manager;

import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;

import com.hzl.smallvideo.application.MainApplication;
import com.hzl.smallvideo.listener.CameraPictureListener;
import com.hzl.smallvideo.listener.CameraYUVDataListener;
import com.hzl.smallvideo.listener.RecordListener;
import com.hzl.smallvideo.manager.api.MangerApi;
import com.hzl.smallvideo.manager.camera.CameraSurfaceView;
import com.hzl.smallvideo.util.AppUtil;
import com.hzl.smallvideo.util.BitmapUtil;
import com.hzl.smallvideo.util.CameraUtil;
import com.hzl.smallvideo.util.CommonUtil;
import com.hzl.smallvideo.util.FFmpegUtil;
import com.libyuv.util.YuvUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    //输出的宽高,这里给的是540p
    private int outWidth = 540;
    private int outHeight = 960;

    private volatile boolean isRunning;
    private volatile boolean isFirstOnDrawFrame = true;
    private BlockingQueue<byte[]> yuvList;
    private List<Long> timeList;
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

    public int changeCamera() {
        if (isRunning) {
            CommonUtil.showToast("录制中不能切换摄像头");
            return mCameraUtil.getCurrentCameraType();
        }
        return mCameraSurfaceView.changeCamera();
    }

    public void setLightState(boolean isOpen) {
        mCameraSurfaceView.setLightingState(isOpen);
    }

    public void takePicture(CameraPictureListener listener) {
        mCameraSurfaceView.takePicture(listener);
    }

    public long[] getTimeList() {
        long[] times = new long[timeList.size()];
        for (int i = 0; i < timeList.size(); i++) {
            times[i] = timeList.get(i);
        }
        return times;
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
            //获取默认的水印信息并且保存为本地的png图片
            final String waterPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "water.png";
            Bitmap bitmap = BitmapUtil.getDefaultWatermarkBitmap();
            AppUtil.saveBitmapToFile(bitmap, waterPath);
            final String filters = String.format("movie=%s[wm];[in][wm]overlay=0:15[out]", waterPath);
            //进行h264文件编码的操作
            FFmpegUtil.initH264File(filePath, mCameraUtil.getFrameRate(), outWidth, outHeight, AppUtil.getCpuCores(), filters);
            //一些数据的初始化操作
            yuvList = new LinkedBlockingQueue<>();
            timeList = new ArrayList<Long>();
            yuvData = new byte[outWidth * outHeight * 3 / 2];
            yuvThread = null;
            isFirstOnDrawFrame = false;
        }
        isRunning = true;
        isPause = false;
    }

    @Override
    public void pauseRecord() {
        isPause = true;
    }

    @Override
    public void stopRecord() {
        isRunning = false;
        isPause = false;
        //表示要重新去操作了
        isFirstOnDrawFrame = true;
    }

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
            timeList.add(System.currentTimeMillis());
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
                                    int morientation = mCameraUtil.getMorientation();
                                    YuvUtil.compressYUV(data, mCameraUtil.getCameraWidth(), mCameraUtil.getCameraHeight(), yuvData, outHeight, outWidth, 0, morientation, morientation == 270 ? mCameraUtil.isMirror() : false);
                                    FFmpegUtil.pushDataToH264File(yuvData);
                                }
                            } else if (!isRunning && !isPause) {
                                FFmpegUtil.getH264File();
                                if (listener != null) {
                                    listener.videoComplete();
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
