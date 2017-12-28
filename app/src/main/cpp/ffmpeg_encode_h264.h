#ifndef SMALLVIDEO_FFMPEG_ENCODE_H264_H
#define SMALLVIDEO_FFMPEG_ENCODE_H264_H

#include "threadsafe_queue.cpp"

extern "C"
{
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libavutil/opt.h"
#include "libavfilter/avfilter.h"
#include "libavfilter/avfiltergraph.h"
#include "libavfilter/buffersink.h"
#include "libavfilter/buffersrc.h"
#include "libavutil/imgutils.h"
}

class FFmpegEncodeH264 {

public:

    bool isCompleted; //是否已经完成编码操作

    void initH264File(const char *filePath, int rate, int width, int height, int coreCount,
                      const char *filter);

    void pushDataToH264File(uint8_t *src_);

    void endEncode();

    void init_filters(const char *filters_descr);

    static void *startEncode(void *obj);

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
    threadsafe_queue<uint8_t *> frame_queue;
    bool is_end; //表示数据结束了

    AVFilterContext *buffersink_ctx;
    AVFilterContext *buffersrc_ctx;
    AVFilterGraph *filter_graph;
    AVFrame *frame_in;
    unsigned char *frame_buffer_in;
};


#endif //SMALLVIDEO_FFMPEG_ENCODE_H264_H
