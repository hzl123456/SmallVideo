#ifndef SMALLVIDEO_FFMPEG_CONFIG_H
#define SMALLVIDEO_FFMPEG_CONFIG_H

extern "C"
{
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libavutil/opt.h"
}

class FFmpegConfig {
public:
    static void flush_encoder(AVFormatContext *fmt_ctx, unsigned int stream_index);
};

#endif //SMALLVIDEO_FFMPEG_CONFIG_H
