package com.libyuv.util;

/**
 * 作者：请叫我百米冲刺 on 2017/8/28 上午11:05
 * 邮箱：mail@hezhilin.cc
 */

public class YuvUtil {

    static {
        System.loadLibrary("yuvutil");
    }

    /**
     * @param src        原始数据
     * @param width      原始的宽
     * @param height     原始的高
     * @param dst        输出数据
     * @param dst_width  输出的宽
     * @param dst_height 输出的高
     * @param mode       压缩模式。这里为0，1，2，3 速度由快到慢，质量由低到高，一般用0就好了，因为0的速度最快
     * @param degree     旋转的角度，90，180和270三种
     * @param isMirror   是否镜像，一般只有270的时候才需要镜像
     **/
    public static native void compressYUV(byte[] src, int width, int height, byte[] dst, int dst_width, int dst_height, int mode, int degree, boolean isMirror);

    /**
     * 实例化推流使用的一些参数
     *
     * @param width      原始的宽
     * @param height     原始的高
     * @param dst_width  输出的宽
     * @param dst_height 输出的高
     **/
    public static native void init(int width, int height, int dst_width, int dst_height);

    /**
     * 实例化水印的数据
     *
     * @param src    rgba数据
     * @param width  水印的宽
     * @param height 水印的高
     * @param startX 水印的x的开始位置
     * @param startY 水印的y的开始位置
     **/
    public static native void initWaterMark(byte[] src, int width, int height, int startX, int startY);
}
