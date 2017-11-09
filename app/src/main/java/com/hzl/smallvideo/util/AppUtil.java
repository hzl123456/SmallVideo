package com.hzl.smallvideo.util;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.hzl.smallvideo.application.MainApplication;

import java.lang.reflect.Method;

/**
 * 作者：请叫我百米冲刺 on 2017/9/29 下午1:52
 * 邮箱：mail@hezhilin.cc
 */

public class AppUtil {

    private static int screenWidth;
    private static int screenHeight;

    public static int getScreenWidth() {
        if (screenWidth == 0) {
            WindowManager wm = (WindowManager) MainApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);
            screenWidth = wm.getDefaultDisplay().getWidth();
        }
        return screenWidth;
    }

    public static int getScreenHeight() {
        if (screenHeight == 0) {
            //默认的高度获取方式
            WindowManager wm = (WindowManager) MainApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);
            screenHeight = wm.getDefaultDisplay().getHeight();
            //带有虚拟键盘的需要宁外获取
            Display display = wm.getDefaultDisplay();
            DisplayMetrics dm = new DisplayMetrics();
            Class c;
            try {
                c = Class.forName("android.view.Display");
                Method method = c.getMethod("getRealMetrics", DisplayMetrics.class);
                method.invoke(display, dm);
                screenHeight = dm.heightPixels;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return screenHeight;
    }

}
