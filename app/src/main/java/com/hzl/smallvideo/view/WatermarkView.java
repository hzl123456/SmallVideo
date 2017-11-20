package com.hzl.smallvideo.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hzl.smallvideo.R;
import com.hzl.smallvideo.listener.WatermarkCallbackListener;

/**
 * 作者：请叫我百米冲刺 on 2017/11/17 下午1:33
 * 邮箱：mail@hezhilin.cc
 */

public class WatermarkView extends FrameLayout implements View.OnClickListener {

    public TextView tvCancel;
    public TextView tvOk;
    public RelativeLayout layoutControll;

    private WatermarkCallbackListener listener;

    public void setWatermarkCallbackListener(WatermarkCallbackListener listener) {
        this.listener = listener;
    }

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
        View rootView = View.inflate(getContext(), R.layout.layout_watermark, null);
        tvCancel = (TextView) rootView.findViewById(R.id.tv_cancel);
        tvOk = (TextView) rootView.findViewById(R.id.tv_ok);
        layoutControll = (RelativeLayout) rootView.findViewById(R.id.layout_controll);
        removeAllViews();
        addView(rootView);

        tvCancel.setOnClickListener(this);
        tvOk.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == tvOk) { //表示确定完成
            if (listener != null) {
                Bitmap bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.mipmap.ic_launcher_round);
                listener.onResult(bitmap);
            }
        } else if (v == tvCancel) { //表示取消
            if (listener != null) {
                listener.onCancel();
            }
        }
    }
}
