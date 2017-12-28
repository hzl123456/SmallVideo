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

import com.hzl.smallvideo.manager.RecordManager;
import com.hzl.smallvideo.util.AppUtil;

/**
 * 作者：请叫我百米冲刺 on 2017/11/10 上午11:58
 * 邮箱：mail@hezhilin.cc
 * <p>
 * 触摸进行录制的控件
 */
public class CaptureButton extends View {

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
    private final int STATE_RECORD = 2;
    private final int STATE_PICTURE_BROWSE = 3;
    private final int STATE_RECORD_BROWSE = 4;

    //抬起的时候的操作变化，一种是视频录制成功，一种是图片拍照成功，当然还有一种初始为0的状态
    private int STATE_UP;

    //是否是完整的一个录制流程
    private boolean isQuitRecord;

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
            mPaint.setColor(Color.WHITE);
            canvas.drawCircle(btn_left_X, btn_center_Y, btn_result_radius, mPaint);
            mPaint.setColor(0xFFEEEEEE);
            canvas.drawCircle(btn_center_X, btn_center_Y, btn_result_radius, mPaint);
            mPaint.setColor(Color.WHITE);
            canvas.drawCircle(btn_right_X, btn_center_Y, btn_result_radius, mPaint);


            //---------------绘制左边返回按钮----------------
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

            //---------------绘制中间的操作按钮----------------
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.BLACK);
            paint.setStrokeWidth(AppUtil.dip2px(2));
            path.reset();
            length = AppUtil.dip2px(10);
            path.moveTo(btn_center_X - length, btn_center_Y - AppUtil.dip2px(6));
            path.lineTo(btn_center_X + length, btn_center_Y - AppUtil.dip2px(6));
            path.moveTo(btn_center_X - length, btn_center_Y);
            path.lineTo(btn_center_X + length, btn_center_Y);
            path.moveTo(btn_center_X - length, btn_center_Y + AppUtil.dip2px(6));
            path.lineTo(btn_center_X + length, btn_center_Y + AppUtil.dip2px(6));
            canvas.drawPath(path, paint);


            //---------------绘制右边的确定按钮----------------
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (STATE_SELECTED == STATE_LESSNESS) {
                    //此时是中间的那个开始按钮的按下
                    if (event.getY() > btn_center_Y - btn_outside_radius && event.getY() < btn_center_Y + btn_outside_radius && event.getX() > btn_center_X - btn_outside_radius && event.getX() < btn_center_X + btn_outside_radius && event.getPointerCount() == 1) {
                        STATE_SELECTED = STATE_KEY_DOWN;
                        //延迟500毫秒启动一个缩放的动画
                        postDelayed(longPressRunnable, 500);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                removeCallbacks(longPressRunnable);
                if (isQuitRecord) {
                    isQuitRecord = false;
                    break;
                }
                if (STATE_SELECTED == STATE_KEY_DOWN) {
                    if (event.getY() > btn_center_Y - btn_outside_radius &&
                            event.getY() < btn_center_Y + btn_outside_radius &&
                            event.getX() > btn_center_X - btn_outside_radius &&
                            event.getX() < btn_center_X + btn_outside_radius) {
                        //表示此时是拍照成功的操作
                        STATE_UP = STATE_PICTURE_BROWSE;
                        if (mCaptureListener != null) {
                            mCaptureListener.capture();
                        }
                    }
                } else if (STATE_SELECTED == STATE_RECORD) {
                    //在这里只需要调用record_anim.cancel();就会进行end的回调
                    record_anim.cancel();
                } else if (STATE_SELECTED == STATE_RECORD_BROWSE || STATE_SELECTED == STATE_PICTURE_BROWSE) {
                    //此时是左边那个取消按钮的按下
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
                        STATE_SELECTED = STATE_UP = STATE_LESSNESS;
                        btn_left_X = btn_center_X;
                        btn_right_X = btn_center_X;
                        invalidate();
                    } else if (event.getY() > btn_center_Y - btn_result_radius && //此时是右边那个确认按钮的按下
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
                        STATE_SELECTED = STATE_UP = STATE_LESSNESS;
                        btn_left_X = btn_center_X;
                        btn_right_X = btn_center_X;
                        invalidate();
                    } else if (event.getY() > btn_center_Y - btn_outside_radius &&
                            event.getY() < btn_center_Y + btn_outside_radius &&
                            event.getX() > btn_center_X - btn_outside_radius &&
                            event.getX() < btn_center_X + btn_outside_radius &&
                            event.getPointerCount() == 1) {
                        if (mCaptureListener != null) {
                            mCaptureListener.actionRecord();
                        }
                    }
                } else {
                    //表示没有任何操作
                    STATE_SELECTED = STATE_UP = STATE_LESSNESS;
                }
                break;
        }
        return true;
    }

    public void showControllerButtons() {
        STATE_SELECTED = STATE_UP;
        captureAnimation(getWidth() / 5, (getWidth() / 5) * 4);
    }

    private class LongPressRunnable implements Runnable {

        @Override
        public void run() {
            STATE_SELECTED = STATE_RECORD;
            startAnimation(btn_before_outside_radius, btn_after_outside_radius, btn_before_inside_radius, btn_after_inside_radius, true);
        }
    }

    private class RecordRunnable implements Runnable {

        private long time;

        @Override
        public void run() {
            if (mCaptureListener != null) {
                mCaptureListener.record();
            }
            record_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    progress = (float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            record_anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    time = System.currentTimeMillis();
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if ((System.currentTimeMillis() - time) < 1000) {
                        STATE_SELECTED = STATE_UP = STATE_LESSNESS;
                    } else {
                        //表示此时是录制成功的操作,此时的录制时间是ok的
                        STATE_UP = STATE_RECORD_BROWSE;
                        //给一个50ms的误差
                        if (Math.abs(record_anim.getCurrentPlayTime() - record_anim.getDuration()) < 50) {
                            isQuitRecord = true;
                        }
                    }
                    if (mCaptureListener != null) {
                        mCaptureListener.rencodEnd(STATE_UP != STATE_RECORD_BROWSE);
                    }
                    //回归原始的状态
                    progress = 0;
                    //这里要调用一个缩小的动画
                    startAnimation(btn_after_outside_radius, btn_before_outside_radius, btn_after_inside_radius, btn_before_inside_radius, false);
                }
            });
            record_anim.setInterpolator(null);
            //设置最大录制时间为15秒
            record_anim.setDuration((long) (RecordManager.RECORD_TIME * 1000));
            record_anim.start();
        }
    }

    private void startAnimation(float outside_start, float outside_end, float inside_start, float inside_end, final boolean start_record) {
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
                //表示要进行视频录制的操作
                if (start_record && STATE_SELECTED == STATE_RECORD) {
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

        void rencodEnd(boolean isShortTime);

        void getRecordResult();

        void deleteRecordResult();

        void actionRecord();
    }
}
