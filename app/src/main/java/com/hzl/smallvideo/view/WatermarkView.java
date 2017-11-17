package com.hzl.smallvideo.view;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * 作者：请叫我百米冲刺 on 2017/11/17 下午1:33
 * 邮箱：mail@hezhilin.cc
 */

public class WatermarkView extends FrameLayout {

    public WatermarkView(@NonNull Context context) {
        super(context);
        init();
    }

    public WatermarkView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WatermarkView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

    }
}
