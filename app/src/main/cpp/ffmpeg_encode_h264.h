#ifndef SMALLVIDEO_FFMPEG_ENCODE_H264_H
#define SMALLVIDEO_FFMPEG_ENCODE_H264_H

extern "C"
{
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libavutil/opt.h"
}

class FFmpegEncodeH264 {

public:
    void initH264File(const char *filePath, int rate, int width, int height, int coreCount);

    void pushDataToH264File(uint8_t *src_);

    void getH264File();

private:
    int video_i;
    AVFormatContext *pFormatCtx;
    AVOutputFormat *fmt;
    AVStream *video_st;
    AVCodecContext *pCodecCtx;
    AVCodec *pCodec;
    AVPacket pkt;
    AVFrame *pFrame;
    uint8_t *picture_buf;
    int picture_size;
    int y_size;
    const char *out_file;
};


#endif //SMALLVIDEO_FFMPEG_ENCODE_H264_H
