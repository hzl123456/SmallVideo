package com.hzl.smallvideo.listener;

import android.graphics.Bitmap;

/**
 * 作者：请叫我百米冲刺 on 2017/11/17 下午3:02
 * 邮箱：mail@hezhilin.cc
 */

public interface WatermarkCallbackListener {

    void onResult(Bitmap bitmap);

    void onCancel();
}
