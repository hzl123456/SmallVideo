#ifndef SMALLVIDEO_FFMPEG_CONFIG_H
#define SMALLVIDEO_FFMPEG_CONFIG_H

extern "C"
{
#include "include/libavcodec/avcodec.h"
#include "include/libavformat/avformat.h"
#include "include/libavcodec/avcodec.h"
#include "include/libavutil/opt.h"
}

class FFmpegConfig {
public:
    static void flush_encoder(AVFormatContext *fmt_ctx, unsigned int stream_index);
    static int ffmpeg_cmd_run(int argc, char **argv);

};

#endif //SMALLVIDEO_FFMPEG_CONFIG_H
