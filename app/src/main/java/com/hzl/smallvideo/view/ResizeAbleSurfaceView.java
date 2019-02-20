package com.hzl.smallvideo.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

/**
 * 作者：请叫我百米冲刺 on 2019/2/19 4:10 PM
 * 邮箱：mail@hezhilin.cc
 */
public class ResizeAbleSurfaceView extends SurfaceView {

    private int left;
    private int top;
    private int width = -1;
    private int height = -1;

    public ResizeAbleSurfaceView(Context context) {
        super(context);
    }

    public ResizeAbleSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ResizeAbleSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (width == -1 && height == -1) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {
            setMeasuredDimension(width, height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (width == -1 && height == -1) {
            super.onLayout(changed, left, top, right, bottom);
        } else {
            super.onLayout(changed, this.left, this.top, this.left + this.width, this.top + this.height);
        }

    }

    public void resize(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        requestLayout();
    }
}