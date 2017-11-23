package com.hzl.smallvideo.util;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.hzl.smallvideo.application.MainApplication;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * 作者：请叫我百米冲刺 on 2017/10/20 下午1:51
 * 邮箱：mail@hezhilin.cc
 */

public class CommonUtil {

    private static Toast toast;

    private static SweetAlertDialog dialog;

    public static void showDialog(String text) {
        if (dialog == null || dialog.getContext() == MainApplication.getCurrentActivity()) {
            dialog = new SweetAlertDialog(MainApplication.getCurrentActivity(), SweetAlertDialog.PROGRESS_TYPE);
            dialog.setCancelable(false);
        }
        dialog.setTitleText(text);
        dialog.show();
    }

    public static void disMissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    /**
     * 显示toast
     **/
    public static void showToast(final String text) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
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
