package com.hzl.smallvideo.util;

import com.hzl.smallvideo.manager.RecordManager;

/**
 * 作者：请叫我百米冲刺 on 2017/9/29 下午3:39
 * 邮箱：mail@hezhilin.cc
 */
public class FFmpegUtil {

    static {
        System.loadLibrary("avutil");
        System.loadLibrary("fdk-aac");
        System.loadLibrary("avcodec");
        System.loadLibrary("avformat");
        System.loadLibrary("swscale");
        System.loadLibrary("swresample");
        System.loadLibrary("avfilter");
        System.loadLibrary("ffmpegutil");
    }

    //---------------这里是把yuv编码成h264的方法------------------------
    public static native void initH264File(String filePath, int rate, int width, int height, int coreCount, String filter);

    public static native void pushDataToH264File(byte[] src, long time);

    public static native void endEcodeH264();

    //---------------这里是把pcm编码成aac的方法-------------------------
    public static native void initAACFile(String filePath, int coreCount);

    public static native void pushDataToAACFile(byte[] src);

    public static native void endEcodeAAC();

    //---------------这里是把h264和aac合成mp4的方法---------------------
    public static native void initMP4File(String mp4FilePath, RecordManager recordManager, String methodName);

    //----------------------这里是添加水印的方法------------------------
    public static native void addWatermark(String filter, String outH264FilePath, String outMp4FilePath);
}
