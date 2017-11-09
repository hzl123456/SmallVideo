package com.hzl.smallvideo.util;

import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.hzl.smallvideo.application.MainApplication;

/**
 * 作者：请叫我百米冲刺 on 2017/10/20 下午1:51
 * 邮箱：mail@hezhilin.cc
 */

public class DialogUtil {

    private static Handler handler = new Handler(Looper.getMainLooper());

    private static Toast toast;

    private static ProgressDialog dialog;

    /**
     * 显示loading的dialog
     **/
    public static void showLoadingDialog(String text) {
        if (dialog == null) {
            dialog = new ProgressDialog(MainApplication.getCurrentActivity());
        }
        dialog.setTitle(text);
        dialog.show();
    }

    /**
     * 取消loading的dialog
     **/
    public static void disMissLoadingDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    /**
     * 显示toast
     **/
    public static void showToast(final String text) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (toast == null) {
                    toast = Toast.makeText(MainApplication.getInstance(), text, Toast.LENGTH_SHORT);
                } else {
                    toast.setText(text);
                }
                toast.show();
            }
        });
    }
}
