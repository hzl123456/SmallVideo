package com.hzl.smallvideo.manager.camera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.hzl.smallvideo.util.AppUtil;
import com.hzl.smallvideo.util.DialogUtil;

/**
 * 作者：请叫我百米冲刺 on 2017/11/10 上午11:58
 * 邮箱：mail@hezhilin.cc
 * <p>
 * 触摸进行录制的控件
 */
public class CaptureButton extends View {

    public final String TAG = "CaptureButtom";

    private Paint mPaint;

    //绿色进度条和半径
    private Paint paintArc;
    private int arcWidth = AppUtil.dip2px(5);

    private float btn_center_Y;
    private float btn_center_X;

    private float btn_inside_radius;
    private float btn_outside_radius;
    //半径变化前
    private float btn_before_inside_radius;
    private float btn_before_outside_radius;
    //半径变化后
    private float btn_after_inside_radius;
    private float btn_after_outside_radius;

    private float btn_left_X, btn_right_X, btn_result_radius;

    //状态
    private int STATE_SELECTED;
    private final int STATE_LESSNESS = 0;
    private final int STATE_KEY_DOWN = 1;
    private final int STATE_CAPTURED = 2;
    private final int STATE_RECORD = 3;
    private final int STATE_PICTURE_BROWSE = 4;
    private final int STATE_RECORD_BROWSE = 5;
    private final int STATE_READYQUIT = 6;
    private final int STATE_RECORDED = 7;

    private float key_down_Y;

    private float progress = 0;
    private LongPressRunnable longPressRunnable = new LongPressRunnable();
    private RecordRunnable recordRunnable = new RecordRunnable();
    private ValueAnimator record_anim = ValueAnimator.ofFloat(0, 360);
    private CaptureListener mCaptureListener;

    public CaptureButton(Context context) {
        this(context, null);
    }

    public CaptureButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CaptureButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        STATE_SELECTED = STATE_LESSNESS;

        //初始化绿色进度条的画笔
        paintArc = new Paint();
        paintArc.setAntiAlias(true);
        paintArc.setColor(0xFF00CC00);
        paintArc.setStyle(Paint.Style.STROKE);
        paintArc.setStrokeWidth(arcWidth);


    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int width = widthSize;
        int height = (width / 9) * 4;
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        btn_center_X = getWidth() / 2;
        btn_center_Y = getHeight() / 2;

        btn_outside_radius = (float) (getWidth() / 9);
        btn_inside_radius = (float) (btn_outside_radius * 0.75);

        btn_before_outside_radius = (float) (getWidth() / 9);
        btn_before_inside_radius = (float) (btn_outside_radius * 0.75);
        btn_after_outside_radius = (float) (getWidth() / 6);
        btn_after_inside_radius = (float) (btn_outside_radius * 0.6);

        btn_result_radius = (float) (getWidth() / 9);
        btn_left_X = getWidth() / 2;
        btn_right_X = getWidth() / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (STATE_SELECTED == STATE_LESSNESS || STATE_SELECTED == STATE_RECORD) {
            //绘制拍照按钮
            mPaint.setColor(0xFFEEEEEE);
            canvas.drawCircle(btn_center_X, btn_center_Y, btn_outside_radius, mPaint);
            mPaint.setColor(Color.WHITE);
            canvas.drawCircle(btn_center_X, btn_center_Y, btn_inside_radius, mPaint);

            //绘制绿色进度条
            int width = arcWidth / 2;
            canvas.drawArc(new RectF(btn_center_X - (btn_after_outside_radius - width),
                    btn_center_Y - (btn_after_outside_radius - width),
                    btn_center_X + (btn_after_outside_radius - width),
                    btn_center_Y + (btn_after_outside_radius - width)), -90, progress, false, paintArc);

        } else if (STATE_SELECTED == STATE_RECORD_BROWSE || STATE_SELECTED == STATE_PICTURE_BROWSE) {
            //拍完照或者录完视频需要绘制的内容
            mPaint.setColor(0xFFEEEEEE);
            canvas.drawCircle(btn_left_X, btn_center_Y, btn_result_radius, mPaint);
            mPaint.setColor(Color.WHITE);
            canvas.drawCircle(btn_right_X, btn_center_Y, btn_result_radius, mPaint);

            //绘制左边返回按钮
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(AppUtil.dip2px(2));
            Path path = new Path();

            int length = AppUtil.dip2px(6);
            path.moveTo(btn_left_X, btn_center_Y + length);
            path.lineTo(btn_left_X + length, btn_center_Y + length);
            path.arcTo(new RectF(btn_left_X, btn_center_Y - length, btn_left_X + length * 2, btn_center_Y + length), 90, -180);
            path.lineTo(btn_left_X - length, btn_center_Y - length);
            canvas.drawPath(path, paint);

            paint.setStyle(Paint.Style.FILL);
            path.reset();
            path.moveTo(btn_left_X - length, btn_center_Y - length - AppUtil.dip2px(4));
            path.lineTo(btn_left_X - length, btn_center_Y - length + AppUtil.dip2px(4));
            path.lineTo(btn_left_X - length - AppUtil.dip2px(4), btn_center_Y - length);
            path.close();
            canvas.drawPath(path, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(0xFF00CC00);
            paint.setStrokeWidth(AppUtil.dip2px(2));
            path.reset();
            path.moveTo(btn_right_X - AppUtil.dip2px(28f / 3), btn_center_Y);
            path.lineTo(btn_right_X - AppUtil.dip2px(8f / 3), btn_center_Y + AppUtil.dip2px(22f / 3));
            path.lineTo(btn_right_X + AppUtil.dip2px(30f / 3), btn_center_Y - AppUtil.dip2px(20f / 3));
            path.lineTo(btn_right_X - AppUtil.dip2px(8f / 3), btn_center_Y + AppUtil.dip2px(18f / 3));
            path.close();
            canvas.drawPath(path, paint);
        }
    }

    //事件处理
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //空状态
                if (STATE_SELECTED == STATE_LESSNESS) {
                    //拍照事件按下
                    if (event.getY() > btn_center_Y - btn_outside_radius && event.getY() < btn_center_Y + btn_outside_radius && event.getX() > btn_center_X - btn_outside_radius && event.getX() < btn_center_X + btn_outside_radius && event.getPointerCount() == 1) {
                        key_down_Y = event.getY();
                        STATE_SELECTED = STATE_KEY_DOWN;
                        postCheckForLongTouch(event.getX(), event.getY());
                    }
                } else if (STATE_SELECTED == STATE_RECORD_BROWSE || STATE_SELECTED == STATE_PICTURE_BROWSE) {
                    if (event.getY() > btn_center_Y - btn_result_radius &&
                            event.getY() < btn_center_Y + btn_result_radius &&
                            event.getX() > btn_left_X - btn_result_radius &&
                            event.getX() < btn_left_X + btn_result_radius &&
                            event.getPointerCount() == 1
                            ) {
                        if (mCaptureListener != null) {

                            if (STATE_SELECTED == STATE_RECORD_BROWSE) {
                                mCaptureListener.deleteRecordResult();
                            } else if (STATE_SELECTED == STATE_PICTURE_BROWSE) {
                                mCaptureListener.cancel();
                            }
                        }
                        STATE_SELECTED = STATE_LESSNESS;
                        btn_left_X = btn_center_X;
                        btn_right_X = btn_center_X;
                        invalidate();
                    } else if (event.getY() > btn_center_Y - btn_result_radius &&
                            event.getY() < btn_center_Y + btn_result_radius &&
                            event.getX() > btn_right_X - btn_result_radius &&
                            event.getX() < btn_right_X + btn_result_radius &&
                            event.getPointerCount() == 1
                            ) {
                        if (mCaptureListener != null) {
                            if (STATE_SELECTED == STATE_RECORD_BROWSE) {
                                mCaptureListener.getRecordResult();
                            } else if (STATE_SELECTED == STATE_PICTURE_BROWSE) {
                                mCaptureListener.determine();
                            }
                        }
                        STATE_SELECTED = STATE_LESSNESS;
                        btn_left_X = btn_center_X;
                        btn_right_X = btn_center_X;
                        invalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                removeCallbacks(longPressRunnable);
                if (STATE_SELECTED == STATE_KEY_DOWN) {
                    if (event.getY() > btn_center_Y - btn_outside_radius &&
                            event.getY() < btn_center_Y + btn_outside_radius &&
                            event.getX() > btn_center_X - btn_outside_radius &&
                            event.getX() < btn_center_X + btn_outside_radius) {
                        STATE_SELECTED = STATE_PICTURE_BROWSE;
                        captureAnimation(getWidth() / 5, (getWidth() / 5) * 4);
                        invalidate();
                        if (mCaptureListener != null) {
                            mCaptureListener.capture();
                        }
                        if (btn_outside_radius == btn_after_outside_radius && btn_inside_radius == btn_after_inside_radius) {
                            startAnimation(btn_after_outside_radius, btn_before_outside_radius, btn_after_inside_radius, btn_before_inside_radius);
                        } else {
                            startAnimation(btn_after_outside_radius, btn_before_outside_radius, btn_after_inside_radius, btn_before_inside_radius);
                        }
                    }
                } else if (STATE_SELECTED == STATE_RECORD) {
                    if (record_anim.getCurrentPlayTime() < 1000) {
                        STATE_SELECTED = STATE_LESSNESS;
                        DialogUtil.showToast("时间太短了");
                        progress = 0;
                        invalidate();
                        record_anim.cancel();
                    } else {
                        STATE_SELECTED = STATE_RECORD_BROWSE;
                        removeCallbacks(recordRunnable);
                        captureAnimation(getWidth() / 5, (getWidth() / 5) * 4);
                        record_anim.cancel();
                        progress = 0;
                        invalidate();
                        if (mCaptureListener != null) {
                            mCaptureListener.rencodEnd();
                        }
                    }
                    if (btn_outside_radius == btn_after_outside_radius && btn_inside_radius == btn_after_inside_radius) {
                        startAnimation(btn_after_outside_radius, btn_before_outside_radius, btn_after_inside_radius, btn_before_inside_radius);
                    } else {
                        startAnimation(btn_after_outside_radius, btn_before_outside_radius, btn_after_inside_radius, btn_before_inside_radius);
                    }
                }
                break;
        }
        return true;
    }

    public void captureSuccess() {
        captureAnimation(getWidth() / 5, (getWidth() / 5) * 4);
    }

    //长按事件处理
    private void postCheckForLongTouch(float x, float y) {
        longPressRunnable.setPressLocation(x, y);
        postDelayed(longPressRunnable, 500);
    }


    private class LongPressRunnable implements Runnable {
        private int x, y;

        public void setPressLocation(float x, float y) {
            this.x = (int) x;
            this.y = (int) y;
        }

        @Override
        public void run() {
            startAnimation(btn_before_outside_radius, btn_after_outside_radius, btn_before_inside_radius, btn_after_inside_radius);
            STATE_SELECTED = STATE_RECORD;
        }
    }

    private class RecordRunnable implements Runnable {
        @Override
        public void run() {
            if (mCaptureListener != null) {
                mCaptureListener.record();
            }
            record_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (STATE_SELECTED == STATE_RECORD) {
                        progress = (float) animation.getAnimatedValue();
                    }
                    invalidate();
                }
            });
            record_anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (STATE_SELECTED == STATE_RECORD) {
                        STATE_SELECTED = STATE_RECORD_BROWSE;
                        progress = 0;
                        invalidate();
                        captureAnimation(getWidth() / 5, (getWidth() / 5) * 4);
                        if (btn_outside_radius == btn_after_outside_radius && btn_inside_radius == btn_after_inside_radius) {
                            startAnimation(btn_after_outside_radius, btn_before_outside_radius, btn_after_inside_radius, btn_before_inside_radius);
                        } else {
                            startAnimation(btn_after_outside_radius, btn_before_outside_radius, btn_after_inside_radius, btn_before_inside_radius);
                        }
                        if (mCaptureListener != null) {
                            mCaptureListener.rencodEnd();
                        }
                    }
                }
            });
            record_anim.setInterpolator(new LinearInterpolator());
            //设置最大录制时间为15秒
            record_anim.setDuration(15 * 1000);
            record_anim.start();
        }
    }

    private void startAnimation(float outside_start, float outside_end, float inside_start, float inside_end) {

        ValueAnimator outside_anim = ValueAnimator.ofFloat(outside_start, outside_end);
        ValueAnimator inside_anim = ValueAnimator.ofFloat(inside_start, inside_end);
        outside_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                btn_outside_radius = (float) animation.getAnimatedValue();
                invalidate();
            }

        });
        outside_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (STATE_SELECTED == STATE_RECORD) {
                    postDelayed(recordRunnable, 100);
                }
            }
        });
        inside_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                btn_inside_radius = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        outside_anim.setDuration(100);
        inside_anim.setDuration(100);
        outside_anim.start();
        inside_anim.start();
    }

    private void captureAnimation(float left, float right) {
        ValueAnimator left_anim = ValueAnimator.ofFloat(btn_left_X, left);
        ValueAnimator right_anim = ValueAnimator.ofFloat(btn_right_X, right);
        left_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                btn_left_X = (float) animation.getAnimatedValue();
                invalidate();
            }

        });
        right_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                btn_right_X = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        left_anim.setDuration(200);
        right_anim.setDuration(200);
        left_anim.start();
        right_anim.start();
    }

    public void setCaptureListener(CaptureListener mCaptureListener) {
        this.mCaptureListener = mCaptureListener;
    }


    //回调接口
    public interface CaptureListener {
        void capture();

        void cancel();

        void determine();

        void record();

        void rencodEnd();

        void getRecordResult();

        void deleteRecordResult();
    }
}
