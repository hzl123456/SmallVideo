package com.hzl.smallvideo.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

/**
 * 作者：请叫我百米冲刺 on 2017/12/4 上午9:25
 * 邮箱：mail@hezhilin.cc
 */

public class BitmapUtil {

    public static Bitmap getDefaultWatermarkBitmap() {
        TextPaint mPaint = new TextPaint();
        mPaint.setDither(true);
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.WHITE);
        mPaint.setTextSize(25);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        Bitmap bitmap = Bitmap.createBitmap(200, 55, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        String text = "请叫我百米冲刺";
        StaticLayout textLayout = new StaticLayout(text, mPaint, bitmap.getWidth(), Layout.Alignment.ALIGN_NORMAL, 1f, 0, false);
        canvas.translate(bitmap.getWidth() / 2, 0);
        textLayout.draw(canvas);

        float textSize = mPaint.getTextSize();
        text = "mail@hezhilin.cc";
        mPaint.setTextSize(20);
        textLayout = new StaticLayout(text, mPaint, bitmap.getWidth(), Layout.Alignment.ALIGN_NORMAL, 1f, 0, false);
        canvas.translate(0, textSize + 3);
        textLayout.draw(canvas);
        return bitmap;
    }
}
