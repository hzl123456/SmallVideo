#ifndef SMALLVIDEO_FFMPEG_ENCODE_AAC_H
#define SMALLVIDEO_FFMPEG_ENCODE_AAC_H

extern "C"
{
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libavutil/opt.h"
}
class FFmpegEncodeAAC {

public:
    void initAACFile(const char *filePath);

    void pushDataToAACFile(uint8_t *src_);

    void getAACFile();

private:
    AVFormatContext *audio_pFormatCtx;
    AVOutputFormat *audio_fmt;
    AVStream *audio_st;
    AVCodecContext *audio_pCodecCtx;
    AVCodec *audio_pCodec;
    uint8_t *audio_frame_buf;
    AVFrame *audio_pFrame;
    AVPacket audio_pkt;
    int audio_i;
    const char *audio_out_file;
};

#endif //SMALLVIDEO_FFMPEG_ENCODE_AAC_H
