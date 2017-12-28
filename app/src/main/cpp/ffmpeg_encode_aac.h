#ifndef SMALLVIDEO_FFMPEG_ENCODE_AAC_H
#define SMALLVIDEO_FFMPEG_ENCODE_AAC_H

#include "threadsafe_queue.cpp"

extern "C"
{
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libavutil/opt.h"
}

class FFmpegEncodeAAC {

public:

    bool isCompleted; //是否已经完成编码操作

    void initAACFile(const char *filePath, int coreCount);

    void pushDataToAACFile(uint8_t *src_);

    void endEncode();

    static void* startEncode(void* obj);

private:
    threadsafe_queue<uint8_t *> frame_queue;
    bool is_end; //表示数据结束了

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
