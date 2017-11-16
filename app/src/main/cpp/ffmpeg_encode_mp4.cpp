#include "ffmpeg_encode_mp4.h"
#include "ffmpeg_config.h"
#include "android_log.h"

void FFmpegEncodeMp4::getMP4File(const char *in_filename_v, const char *in_filename_a,
                                 const char *out_filename, int defaultFps, double fps) {

//    //------------先调整h264文件的fps和速度-----------------
//    //根据fps计算需要调整的数据
//    double percent = fps / defaultFps;
//    LOGI("percent:%lf", percent);
//
//    //输出文件地址
//    char *new_out_filename = new char[strlen(in_filename_v) + 5];
//    strcpy(new_out_filename, in_filename_v);
//    strcat(new_out_filename, ".temp");
//    LOGI("new_file:%s", new_out_filename);
//
//    //把double的fps转化为char*
//    char *charFps = new char[sizeof(fps)];
//    sprintf(charFps, "%lf", fps);
//    LOGI("fps:%s", charFps);
//
//    //得到percent的char*
//    char *charPercent = new char[sizeof(percent)];
//    sprintf(charPercent, "%lf", percent);
//
//    char *setPts = new char[strlen(charPercent) + 13];
//    strcpy(setPts, "\"setpts=");
//    strcat(setPts, charPercent);
//    strcat(setPts, "*PTS\"");
//
//    LOGI("setpts:%s", setPts);
//
//    //先调整h264文件的fps
//    char *fpsCmd[9];
//    fpsCmd[0] = "ffmpeg";
//    fpsCmd[1] = "-i";
//    fpsCmd[2] = (char *) in_filename_v; //输入的文件
//    fpsCmd[3] = "-an";
//    fpsCmd[4] = "-r";
//    fpsCmd[5] = charFps;    //fps
//    fpsCmd[6] = "-filter:v";
//    fpsCmd[7] = setPts; //pts
//    fpsCmd[8] = new_out_filename;  //输出的文件
//    FFmpegConfig::ffmpeg_cmd_run(9, fpsCmd);

// ffmpeg -i input.mkv -an -r 60 -filter:v "setpts=2.0*PTS" output.mkv


//------------先将h264和aac文件合成mp4文件--------------
    char *cmd[10];
    cmd[0] = "ffmpeg";
    cmd[1] = "-i";
    cmd[2] = (char *) in_filename_v;
    cmd[3] = "-i";
    cmd[4] = (char *) in_filename_a;
    cmd[5] = "-c:v";
    cmd[6] = "copy";
    cmd[7] = "-c:a";
    cmd[8] = "copy";
    cmd[9] = (char *) out_filename;
    FFmpegConfig::ffmpeg_cmd_run(10, cmd);
}