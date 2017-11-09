package com.hzl.smallvideo.application;

import android.app.Activity;
import android.app.Application;

/**
 * 作者：请叫我百米冲刺 on 2017/9/29 下午1:55
 * 邮箱：mail@hezhilin.cc
 */

public class MainApplication extends Application {

    private static MainApplication INSTANCE;

    private static Activity CURRENT_ACTIVITY;

    public static MainApplication getInstance() {
        return INSTANCE;
    }

    public static void setCurrentActivity(Activity activity) {
        CURRENT_ACTIVITY = activity;
    }

    public static Activity getCurrentActivity() {
        return CURRENT_ACTIVITY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
    }
}
