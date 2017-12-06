//
// Created by hzl on 2017/12/6.
//

#ifndef SMALLVIDEO_FFMPEG_WATERMARK_H
#define SMALLVIDEO_FFMPEG_WATERMARK_H

#include <unistd.h>

extern "C"
{
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavfilter/avfiltergraph.h>
#include <libavfilter/buffersink.h>
#include <libavfilter/buffersrc.h>
#include <libavutil/opt.h>

}

class FFmpegWatermark {

private:
    void open_input_file(const char *filename);

    void init_filters(const char *filters_descr);

    void display_frame(const AVFrame *frame, AVRational time_base);

public:

    void add_watermark(const char *input_path, const char *filters_descr);

    AVFormatContext *fmt_ctx;
    AVCodecContext *dec_ctx;
    AVFilterContext *buffersink_ctx;
    AVFilterContext *buffersrc_ctx;
    AVFilterGraph *filter_graph;
    int video_stream_index = -1;
    int64_t last_pts = AV_NOPTS_VALUE;
};


#endif //SMALLVIDEO_FFMPEG_WATERMARK_H
