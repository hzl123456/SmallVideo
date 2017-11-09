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
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.hzl.smallvideo.R;
import com.hzl.smallvideo.manager.api.CameraSurfaceApi;
import com.hzl.smallvideo.manager.listener.CameraYUVDataListener;
import com.hzl.smallvideo.util.CameraUtil;

/**
 * 作者：请叫我百米冲刺 on 2017/11/7 上午10:24
 * 邮箱：mail@hezhilin.cc
 * <p>
 * 将camera和sufaceview结合起来的控件
 */
public class CameraSurfaceView extends FrameLayout implements CameraSurfaceApi, Camera.PreviewCallback, SurfaceHolder.Callback {

    private SurfaceView mSurfaceView;
    private ImageView ivFoucView;
    private CameraUtil mCameraUtil;

    private CameraYUVDataListener listener;

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
        mSurfaceView = (SurfaceView) view.findViewById(R.id.surface);
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
        holder.setFixedSize(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCameraUtil.releaseCamera();
    }

    @Override
    public void setCameraType(int cameraType) {
        mCameraUtil.setCurrentCameraType(cameraType);
    }

    @Override
    public void openCamera() {
        mCameraUtil.openCamera(mCameraUtil.getCurrentCameraType());
    }

    @Override
    public void releaseCamera() {
        mCameraUtil.releaseCamera();
    }

    @Override
    public void changeCamera() {
        mCameraUtil.releaseCamera();
        if (mCameraUtil.getCurrentCameraType() == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mCameraUtil.openCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
        } else {
            mCameraUtil.openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
        }
        mSurfaceView.post(new Runnable() {
            @Override
            public void run() {
                mCameraUtil.handleCameraStartPreview(mSurfaceView.getHolder(), CameraSurfaceView.this);
            }
        });
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
        return true;
    }
}
