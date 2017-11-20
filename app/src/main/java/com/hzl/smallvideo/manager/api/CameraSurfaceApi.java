package com.hzl.smallvideo.manager.api;

import com.hzl.smallvideo.listener.CameraPictureListener;

/**
 * 作者：请叫我百米冲刺 on 2017/11/7 上午10:38
 * 邮箱：mail@hezhilin.cc
 */
public interface CameraSurfaceApi {

    void startAutoFocus(float x, float y);

    void setLightingState(boolean isOpen);

    void openCamera();

    void releaseCamera();

    void takePicture(CameraPictureListener listener);

    int changeCamera();
}
