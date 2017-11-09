package com.hzl.smallvideo.util;

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
    public static native void initH264File(String filePath, int rate, int width, int height);

    public static native void pushDataToH264File(byte[] src);

    public static native void getH264File();

    //---------------这里是把pcm编码成aac的方法-------------------------
    public static native void initAACFile(String filePath);

    public static native void pushDataToAACFile(byte[] src);

    public static native void getAACFile();

    //---------------这里是把h624和aac合成mp4的方法---------------------
    public static native void getMP4File(String h624FilePath, String aacFilePath, String mp4FilePath);

}
