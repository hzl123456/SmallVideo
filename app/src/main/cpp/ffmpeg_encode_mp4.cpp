#include "ffmpeg_encode_mp4.h"
#include "ffmpeg_config.h"

void FFmpegEncodeMp4::getMP4File(const char *in_filename_v, const char *in_filename_a,
                                 const char *out_filename, float fps) {
    //将h264和aac合并成mp4文件
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
    //调整视频的fps

}