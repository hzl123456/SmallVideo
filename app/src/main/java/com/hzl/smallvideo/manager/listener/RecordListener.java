package com.hzl.smallvideo.manager.listener;

/**
 * 作者：请叫我百米冲刺 on 2017/11/7 上午11:33
 * 邮箱：mail@hezhilin.cc
 */

public abstract class RecordListener {

    //表示h264编码结束
    public void videoComplete(float fps) {
    }

    //表示aac编码结束
    public void audioComplete() {
    }
}
