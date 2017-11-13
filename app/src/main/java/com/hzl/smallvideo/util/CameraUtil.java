package com.hzl.smallvideo.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.hzl.smallvideo.application.MainApplication;
import com.hzl.smallvideo.manager.listener.CameraPictureListener;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 作者：请叫我百米冲刺 on 2017/9/14 上午9:07
 * 邮箱：mail@hezhilin.cc
 */
@SuppressWarnings("deprecation")
public class CameraUtil {

    //视频的帧率设置
    private static int MAX_FRAME_RATE = 25;
    private int mFrameRate = MAX_FRAME_RATE;

    public int getFrameRate() {
        return mFrameRate;
    }

    private Camera mCamera;
    private boolean isFocusing;  //是否正在对焦
    private boolean isStartPreview; //是否已经开始预览了

    /**
     * 推流出去的数据是否镜像,默认镜像
     **/
    private boolean mirror = true;

    public boolean isMirror() {
        return mirror;
    }

    /**
     * 摄像头的旋转角度
     **/
    private int morientation;

    public int getMorientation() {
        return morientation;
    }

    /**
     * 预览的一个摄像头画面大小，默认为1080p，如果不支持的话就用摄像头默认的
     */
    private int cameraWidth = 1280;
    private int cameraHeight = 720;

    public int getCameraWidth() {
        return cameraWidth;
    }

    public int getCameraHeight() {
        return cameraHeight;
    }

    /**
     * 摄像头的前后置,默认为后置摄像头
     **/
    private int mCurrentCameraType = Camera.CameraInfo.CAMERA_FACING_BACK;

    public void setCurrentCameraType(int mCurrentCameraType) {
        this.mCurrentCameraType = mCurrentCameraType;
    }

    public int getCurrentCameraType() {
        return mCurrentCameraType;
    }


    private void choosePreviewSize(Camera.Parameters parms, int width, int height) {
        //先判断是否支持该分辨率
        Camera.Size maxSize = null;
        for (Camera.Size size : parms.getSupportedPreviewSizes()) {
            //满足16:9的才进行使用并且小于等于cameraWidth的才进行使用
            if (size.width * 9 == size.height * 16 && size.width <= cameraWidth) {
                if (maxSize == null) {
                    maxSize = size;
                } else if (maxSize.width < size.width) {
                    maxSize = size;
                }
            }
            if (size.width == width && size.height == height) {
                parms.setPreviewSize(width, height);
                return;
            }
        }
        if (maxSize != null) {  //如果存在maxSize的话就采用maxSize
            parms.setPreviewSize(maxSize.width, maxSize.height);
        } else {  //如果没有16:9的话就采用默认的
            Camera.Size ppsfv = parms.getPreferredPreviewSizeForVideo();
            parms.setPreviewSize(maxSize.width, ppsfv.height);
        }
    }

    private int setCameraDisplayOrientation(int cameraId) {
        Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = MainApplication.getCurrentActivity().getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            //前置摄像头需要镜像,转化后进行设置
            mCamera.setDisplayOrientation((360 - result) % 360);
        } else {
            result = (info.orientation - degrees + 360) % 360;
            //后置摄像头直接进行显示
            mCamera.setDisplayOrientation(result);
        }
        return result;
    }

    public void openCamera(int cameraType) {
        if (mCamera != null) {
            //释放camera
            releaseCamera();
        }
        Camera.CameraInfo info = new Camera.CameraInfo();
        int cameraId = 0;
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == cameraType) {
                cameraId = i;
                mCamera = Camera.open(i);
                mCurrentCameraType = cameraType;
                break;
            }
        }
        if (mCamera == null) {
            throw new RuntimeException("unable to open camera");
        }

        //这边是设置旋转的
        morientation = setCameraDisplayOrientation(cameraId);

        Camera.Parameters parameters = mCamera.getParameters();
        choosePreviewSize(parameters, cameraWidth, cameraHeight);
        List<String> focusModes = parameters.getSupportedFocusModes();
        //这边采用自动对焦的模式
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        //设置帧率
        List<Integer> rates = parameters.getSupportedPreviewFrameRates();
        if (rates != null) {
            if (rates.contains(MAX_FRAME_RATE)) {
                mFrameRate = MAX_FRAME_RATE;
            } else {
                boolean findFrame = false;
                Collections.sort(rates);
                for (int i = rates.size() - 1; i >= 0; i--) {
                    if (rates.get(i) <= MAX_FRAME_RATE) {
                        mFrameRate = rates.get(i);
                        findFrame = true;
                        break;
                    }
                }
                if (!findFrame) {
                    mFrameRate = rates.get(0);
                }
            }
        }
        parameters.setPreviewFrameRate(mFrameRate);
        //设置图片的大小
        parameters.setPictureSize(cameraWidth, cameraHeight);
        parameters.setPreviewFormat(ImageFormat.NV21);
        mCamera.setParameters(parameters);
        /**
         * 请注意这个地方, camera返回的图像并不一定是设置的大小（因为可能并不支持）
         */
        Camera.Size size = mCamera.getParameters().getPreviewSize();
        cameraWidth = size.width;
        cameraHeight = size.height;
    }

    /**
     * 检测是否支持指定特性
     */
    private boolean isSupported(List<String> list, String key) {
        return list != null && list.contains(key);
    }

    public void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
            isStartPreview = false;
            isFocusing = false;
        }
    }

    public void handleCameraStartPreview(SurfaceHolder surfaceHolder, Camera.PreviewCallback callback) {
        mCamera.setPreviewCallback(callback);
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
        isStartPreview = true;
        isFocusing = false;
        //进行一次自动对焦
        startAutoFocus();
    }

    public void startAutoFocus() {
        try {
            if (mCamera != null && !isFocusing && isStartPreview) { //camera不为空，并且isFocusing=false的时候才去对焦
                mCamera.cancelAutoFocus();
                isFocusing = true;
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        isFocusing = false;
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 打开或者关闭闪光灯
     **/
    public void setLightingState(boolean isOpen) {
        if (mCamera == null) {
            return;
        }
        try {
            Camera.Parameters mParameters = mCamera.getParameters();
            if (isOpen) {
                mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            } else {
                mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
            mCamera.setParameters(mParameters);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 调整焦距,true表示+1，false表示减1
     **/
    public void setZoomPlus(boolean isPlus) {
        if (mCamera == null) {
            return;
        }
        try {
            Camera.Parameters mParameters = mCamera.getParameters();
            int value = mParameters.getZoom();
            value += (isPlus ? 1 : -1);
            if (value < 0) {
                value = 0;
            } else if (value > mParameters.getMaxZoom()) {
                value = mParameters.getMaxZoom();
            }
            mParameters.setZoom(value);
            mCamera.setParameters(mParameters);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 进行拍照
     **/
    public void takePicture(final CameraPictureListener listener) {
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                mCamera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        Matrix matrix = new Matrix();
                        //进行旋转，如果是前置摄像头还需要镜像
                        matrix.postRotate(morientation);
                        if (mCurrentCameraType == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                            matrix.postScale(-1, 1);
                        }
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        if (listener != null) {
                            listener.onPictureBitmap(bitmap);
                        }
                    }
                });
            }
        });
    }
}
