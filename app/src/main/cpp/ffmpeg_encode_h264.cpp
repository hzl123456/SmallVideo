#include "ffmpeg_encode_h264.h"
#include "ffmpeg_config.h"
#include "android_log.h"

void FFmpegEncodeH264::init_filters(const char *filters_descr) {
    char args[512];
    int ret = 0;
    AVFilter *buffersrc = avfilter_get_by_name("buffer");
    AVFilter *buffersink = avfilter_get_by_name("buffersink");
    AVFilterInOut *outputs = avfilter_inout_alloc();
    AVFilterInOut *inputs = avfilter_inout_alloc();
    enum AVPixelFormat pix_fmts[] = {AV_PIX_FMT_YUVA420P, AV_PIX_FMT_NONE};
    filter_graph = avfilter_graph_alloc();
    if (!outputs || !inputs || !filter_graph) {
        ret = AVERROR(ENOMEM);
        goto end;
    }
    /* buffer video source: the decoded frames from the decoder will be inserted here. */
    snprintf(args, sizeof(args),
             "video_size=%dx%d:pix_fmt=%d:time_base=%d/%d:pixel_aspect=%d/%d",
             pCodecCtx->width, pCodecCtx->height, pCodecCtx->pix_fmt,
             pCodecCtx->time_base.num, pCodecCtx->time_base.den,
             pCodecCtx->sample_aspect_ratio.num, pCodecCtx->sample_aspect_ratio.den);
    ret = avfilter_graph_create_filter(&buffersrc_ctx, buffersrc, "in",
                                       args, NULL, filter_graph);
    if (ret < 0) {
        LOGI("Cannot create buffer source\n");
        goto end;
    }
    /* buffer video sink: to terminate the filter chain. */
    ret = avfilter_graph_create_filter(&buffersink_ctx, buffersink, "out",
                                       NULL, NULL, filter_graph);
    if (ret < 0) {
        LOGI("Cannot create buffer sink\n");
        goto end;
    }
    ret = av_opt_set_int_list(buffersink_ctx, "pix_fmts", pix_fmts,
                              AV_PIX_FMT_NONE, AV_OPT_SEARCH_CHILDREN);
    if (ret < 0) {
        LOGI("Cannot set output pixel format\n");
        goto end;
    }
    /*
     * Set the endpoints for the filter graph. The filter_graph will
     * be linked to the graph described by filters_descr.
     */
    /*
     * The buffer source output must be connected to the input pad of
     * the first filter described by filters_descr; since the first
     * filter input label is not specified, it is set to "in" by
     * default.
     */
    outputs->name = av_strdup("in");
    outputs->filter_ctx = buffersrc_ctx;
    outputs->pad_idx = 0;
    outputs->next = NULL;
    /*
     * The buffer sink input must be connected to the output pad of
     * the last filter described by filters_descr; since the last
     * filter output label is not specified, it is set to "out" by
     * default.
     */
    inputs->name = av_strdup("out");
    inputs->filter_ctx = buffersink_ctx;
    inputs->pad_idx = 0;
    inputs->next = NULL;
    if ((ret = avfilter_graph_parse_ptr(filter_graph, filters_descr,
                                        &inputs, &outputs, NULL)) < 0)
        goto end;
    if ((ret = avfilter_graph_config(filter_graph, NULL)) < 0)
        goto end;
    end:
    avfilter_inout_free(&inputs);
    avfilter_inout_free(&outputs);
}

void FFmpegEncodeH264::initH264File(const char *filePath, int rate, int width, int height,
                                    int coreCount, const char *filter) {
    //获取yuv数据和路径已经大小，一些数据的初始化
    video_i = 0;
    out_file = filePath;
    y_size = width * height;

    //注册FFmpeg所有编解码器。
    av_register_all();
    //注册水印
    avfilter_register_all();
    pFormatCtx = avformat_alloc_context();
    fmt = av_guess_format(NULL, out_file, NULL);
    pFormatCtx->oformat = fmt;

    //打开输出文件
    avio_open(&pFormatCtx->pb, out_file, AVIO_FLAG_READ_WRITE);
    //创建输出码流的AVStream。
    video_st = avformat_new_stream(pFormatCtx, 0);

    //Param that must set
    pCodecCtx = video_st->codec;
    pCodecCtx->codec_id = AV_CODEC_ID_H264;
    pCodecCtx->codec_type = AVMEDIA_TYPE_VIDEO;
    pCodecCtx->pix_fmt = AV_PIX_FMT_YUV420P;
    //视频的宽高设置
    pCodecCtx->width = width;
    pCodecCtx->height = height;
    //视频的码率设置，540p默认为2M
    pCodecCtx->bit_rate = 2 * 1024 * 1024;
    //每xxx帧插入1个I帧，I帧越少，视频越小
    pCodecCtx->gop_size = 50;  //关键帧的间隔数
    //帧率的基本单位，我们用分数来表示，帧率通过外面传进来
    pCodecCtx->time_base = (AVRational) {1, rate};
    //编码的线程
    pCodecCtx->thread_count = coreCount;
    //两个非B帧之间允许出现多少个B帧数
    pCodecCtx->max_b_frames = 0;
    pCodecCtx->qmin = 18;
    pCodecCtx->qmax = 28;

    // Set Option
    AVDictionary *param = 0;
    //H.264
    if (pCodecCtx->codec_id == AV_CODEC_ID_H264) {
        av_dict_set(&param, "tune", "zerolatency", 0);
        av_dict_set(&param, "profile", "baseline", 0);
        av_opt_set(pCodecCtx->priv_data, "preset", "ultrafast", 0);
    }
    //Show some Information
    av_dump_format(pFormatCtx, 0, out_file, 1);
    //查找编码器
    pCodec = avcodec_find_encoder(pCodecCtx->codec_id);
    avcodec_alloc_context3(pCodec);
    //打开编码器
    avcodec_open2(pCodecCtx, pCodec, &param);

    pFrame = av_frame_alloc();
    picture_size = avpicture_get_size(pCodecCtx->pix_fmt, pCodecCtx->width, pCodecCtx->height);
    uint8_t *buf = (uint8_t *) av_malloc(picture_size);
    avpicture_fill((AVPicture *) pFrame, buf, pCodecCtx->pix_fmt, pCodecCtx->width,
                   pCodecCtx->height);

    //写文件的头部
    avformat_write_header(pFormatCtx, NULL);
    av_new_packet(&pkt, picture_size);

    //水印的实例化
    init_filters(filter);
    frame_in = av_frame_alloc();
    frame_buffer_in = (unsigned char *) av_malloc(
            av_image_get_buffer_size(AV_PIX_FMT_YUV420P, pCodecCtx->width, pCodecCtx->height, 1));
    av_image_fill_arrays(frame_in->data, frame_in->linesize, frame_buffer_in, AV_PIX_FMT_YUV420P,
                         pCodecCtx->width, pCodecCtx->height, 1);
    frame_in->width = pCodecCtx->width;
    frame_in->height = pCodecCtx->height;
    frame_in->format = AV_PIX_FMT_YUV420P;
}

void FFmpegEncodeH264::pushDataToH264File(uint8_t *src_) {
    //input Y,U,V
    picture_buf = src_;
    //将yuv数据进行写入
    frame_in->data[0] = picture_buf;              // Y
    frame_in->data[1] = picture_buf + y_size;      // U
    frame_in->data[2] = picture_buf + y_size * 5 / 4;  // V
    frame_in->pts = (video_i++);
    int got_picture = 0;

    av_buffersrc_add_frame(buffersrc_ctx, frame_in);
    av_buffersink_get_frame(buffersink_ctx, pFrame);

    //Encode
    avcodec_encode_video2(pCodecCtx, &pkt, pFrame, &got_picture);
    if (got_picture == 1) {
        pkt.stream_index = video_st->index;
        av_write_frame(pFormatCtx, &pkt);
        av_free_packet(&pkt);
    }
}

void FFmpegEncodeH264::getH264File() {
    //Flush Encoder
    FFmpegConfig::flush_encoder_video(pFormatCtx, 0);
    //Write file trailer
    av_write_trailer(pFormatCtx);
    //Clean
    if (video_st) {
        avcodec_close(video_st->codec);
        av_free(pFrame);
        //free(picture_buf);
    }
    avio_close(pFormatCtx->pb);
    avformat_free_context(pFormatCtx);
}