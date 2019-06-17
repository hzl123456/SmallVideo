package com.hzl.smallvideo.manager.camera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.hzl.smallvideo.R;
import com.hzl.smallvideo.listener.CameraPictureListener;
import com.hzl.smallvideo.listener.CameraYUVDataListener;
import com.hzl.smallvideo.manager.api.CameraSurfaceApi;
import com.hzl.smallvideo.util.CameraUtil;
import com.hzl.smallvideo.view.ResizeAbleSurfaceView;

/**
 * 作者：请叫我百米冲刺 on 2017/11/7 上午10:24
 * 邮箱：mail@hezhilin.cc
 * <p>
 * 将camera和sufaceview结合起来的控件
 */
@SuppressWarnings("deprecation")
public class CameraSurfaceView extends FrameLayout implements CameraSurfaceApi, Camera.PreviewCallback, SurfaceHolder.Callback {

    private ResizeAbleSurfaceView mSurfaceView;

    private ImageView ivFoucView;

    private CameraUtil mCameraUtil;

    private CameraYUVDataListener listener;

    private double pointLength;  //双指刚按下去时候的距离

    public void setCameraYUVDataListener(CameraYUVDataListener listener) {
        this.listener = listener;
    }

    public CameraSurfaceView(Context context) {
        super(context);
        init();
    }

    public CameraSurfaceView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    public CameraSurfaceView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init();
    }

    private void init() {
        View view = View.inflate(getContext(), R.layout.layout_camera, null);
        mSurfaceView = (ResizeAbleSurfaceView) view.findViewById(R.id.surface);
        ivFoucView = (ImageView) view.findViewById(R.id.iv_focus);
        removeAllViews();
        addView(view);

        //创建所需要的camera和surfaceview
        mCameraUtil = new CameraUtil();
        mSurfaceView.getHolder().addCallback(this);
    }

    public CameraUtil getCameraUtil() {
        return mCameraUtil;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        camera.addCallbackBuffer(data);
        //进行回调
        if (listener != null) {
            listener.onCallback(data);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCameraUtil.handleCameraStartPreview(mSurfaceView.getHolder(), this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCameraUtil.releaseCamera();
    }

    @Override
    public void setLightingState(boolean isOpen) {
        mCameraUtil.setLightingState(isOpen);
    }

    @Override
    public void openCamera() {
        mCameraUtil.openCamera(mCameraUtil.getCurrentCameraType());
        mSurfaceView.post(new Runnable() {
            @Override
            public void run() {
                mCameraUtil.handleCameraStartPreview(mSurfaceView.getHolder(), CameraSurfaceView.this);
                //这里可以获取真正的预览的分辨率，在这里要进行屏幕的适配，主要适配非16:9的屏幕
                //获取屏幕的宽高，然后计算偏移
                //根据所得到的宽高和摄像头的宽高来设置比例,这里要让视频进行全屏显示
                int width = mSurfaceView.getWidth();
                int height = mSurfaceView.getHeight();
                int screenWidth = width;
                int screenHeight = height;
                //这边是相反的
                int cameraWidth = mCameraUtil.getCameraHeight();
                int cameraHeight = mCameraUtil.getCameraWidth();
                //判断宽高比例
                float radio = ((float) screenWidth) / screenHeight;
                float cameraRadio = ((float) cameraWidth) / cameraHeight;
                //进行比例的计算
                if (radio > cameraRadio) { //此时表示比较宽，以宽来算，让高度裁剪掉
                    width = screenWidth;
                    height = (int) (screenWidth / cameraRadio);
                } else {//此时比较高,以高来算，让宽度裁剪掉
                    width = (int) (screenHeight * cameraRadio);
                    height = screenHeight;
                }
                int left = (screenWidth - width) / 2;
                int top = (screenHeight - height) / 2;
                mSurfaceView.resize(left, top, width, height);
            }
        });
    }

    @Override
    public void releaseCamera() {
        mCameraUtil.releaseCamera();
    }

    @Override
    public void takePicture(CameraPictureListener listener) {
        mCameraUtil.takePicture(listener);
    }

    @Override
    public int changeCamera() {
        mCameraUtil.releaseCamera();
        if (mCameraUtil.getCurrentCameraType() == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mCameraUtil.setCurrentCameraType(Camera.CameraInfo.CAMERA_FACING_BACK);
        } else {
            mCameraUtil.setCurrentCameraType(Camera.CameraInfo.CAMERA_FACING_FRONT);
        }
        openCamera();
        return mCameraUtil.getCurrentCameraType();
    }

    @Override
    public void startAutoFocus(float x, float y) {
        //后置摄像头才有对焦功能
        if (mCameraUtil != null && mCameraUtil.getCurrentCameraType() == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return;
        }
        if (x != -1 && y != -1) { //这里有一个对焦的动画
            //设置位置和初始状态
            ivFoucView.setTranslationX(x - (ivFoucView.getWidth()) / 2);
            ivFoucView.setTranslationY(y - (ivFoucView.getWidth()) / 2);
            ivFoucView.clearAnimation();

            //执行动画
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(ivFoucView, "scaleX", 1.75f, 1.0f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(ivFoucView, "scaleY", 1.75f, 1.0f);
            AnimatorSet animSet = new AnimatorSet();
            animSet.play(scaleX).with(scaleY);
            animSet.setDuration(500);
            animSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    ivFoucView.setVisibility(VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    ivFoucView.setVisibility(GONE);
                }
            });
            animSet.start();
        }
        mCameraUtil.startAutoFocus();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 1 && event.getAction() == MotionEvent.ACTION_DOWN) {
            startAutoFocus(event.getX(), event.getY());
        }
        //这个时候要使用的是调整那个缩放
        if (event.getPointerCount() == 2) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    pointLength = Math.sqrt(Math.pow(event.getX(0) - event.getX(1), 2) + Math.pow(event.getY(0) - event.getY(1), 2));
                    break;
                case MotionEvent.ACTION_MOVE:
                    double length = Math.sqrt(Math.pow(event.getX(0) - event.getX(1), 2) + Math.pow(event.getY(0) - event.getY(1), 2));
                    //计算差值,
                    double extLength = length - pointLength;
                    //大于0表示放大，小于0表示缩小
                    if (Math.abs(extLength) > 0) {
                        mCameraUtil.setZoomPlus(extLength > 0);
                        pointLength = length;
                    }
                    break;
            }
        }
        return true;
    }
}
