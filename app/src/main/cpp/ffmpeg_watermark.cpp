#include "ffmpeg_watermark.h"
#include "android_log.h"

void FFmpegWatermark::open_input_file(const char *filename) {
    int ret;
    AVCodec *dec;
    if (avformat_open_input(&fmt_ctx, filename, NULL, NULL) < 0) {
        LOGI("Cannot open input file\n");
        return;
    }
    if (avformat_find_stream_info(fmt_ctx, NULL) < 0) {
        LOGI("Cannot find stream information\n");
        return;
    }
    /* select the video stream */
    ret = av_find_best_stream(fmt_ctx, AVMEDIA_TYPE_VIDEO, -1, -1, &dec, 0);
    if (ret < 0) {
        LOGI("Cannot find a video stream in the input file\n");
        return;
    }
    video_stream_index = ret;
    dec_ctx = fmt_ctx->streams[video_stream_index]->codec;
    av_opt_set_int(dec_ctx, "refcounted_frames", 1, 0);
    /* init the video decoder */
    if (avcodec_open2(dec_ctx, dec, NULL) < 0) {
        LOGI("Cannot open video decoder\n");
        return;
    }
}

void FFmpegWatermark::init_filters(const char *filters_descr) {
    char args[512];
    int ret = 0;
    AVFilter *buffersrc = avfilter_get_by_name("buffer");
    AVFilter *buffersink = avfilter_get_by_name("buffersink");
    AVFilterInOut *outputs = avfilter_inout_alloc();
    AVFilterInOut *inputs = avfilter_inout_alloc();
    AVRational time_base = fmt_ctx->streams[video_stream_index]->time_base;
    enum AVPixelFormat pix_fmts[] = {AV_PIX_FMT_GRAY8, AV_PIX_FMT_NONE};
    filter_graph = avfilter_graph_alloc();
    if (!outputs || !inputs || !filter_graph) {
        ret = AVERROR(ENOMEM);
        goto end;
    }
    /* buffer video source: the decoded frames from the decoder will be inserted here. */
    snprintf(args, sizeof(args),
             "video_size=%dx%d:pix_fmt=%d:time_base=%d/%d:pixel_aspect=%d/%d",
             dec_ctx->width, dec_ctx->height, dec_ctx->pix_fmt,
             time_base.num, time_base.den,
             dec_ctx->sample_aspect_ratio.num, dec_ctx->sample_aspect_ratio.den);
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

void FFmpegWatermark::display_frame(const AVFrame *frame, AVRational time_base) {
    int x, y;
    uint8_t *p0, *p;
    int64_t delay;
    if (frame->pts != AV_NOPTS_VALUE) {
        if (last_pts != AV_NOPTS_VALUE) {
            /* sleep roughly the right amount of time;
             * usleep is in microseconds, just like AV_TIME_BASE. */
            delay = av_rescale_q(frame->pts - last_pts,
                                 time_base, AV_TIME_BASE_Q);
            if (delay > 0 && delay < 1000000)
                usleep(delay);
        }
        last_pts = frame->pts;
    }
    /* Trivial ASCII grayscale display. */
    p0 = frame->data[0];
    puts("\033c");
    for (y = 0; y < frame->height; y++) {
        p = p0;
        for (x = 0; x < frame->width; x++)
            putchar(" .-+#"[*(p++) / 52]);
        putchar('\n');
        p0 += frame->linesize[0];
    }
    fflush(stdout);
}

void FFmpegWatermark::add_watermark(const char *input_path, const char *filters_descr) {
    int ret;
    AVPacket packet;
    AVFrame *frame = av_frame_alloc();
    AVFrame *filt_frame = av_frame_alloc();
    int got_frame;
    if (!frame || !filt_frame) {
        LOGI("Could not allocate frame");
        return;
    }
    av_register_all();
    avfilter_register_all();

    //open input android filter
    open_input_file(input_path);
    init_filters(filters_descr);

    /* read all packets */
    while (1) {
        if ((ret = av_read_frame(fmt_ctx, &packet)) < 0)
            break;
        if (packet.stream_index == video_stream_index) {
            got_frame = 0;
            ret = avcodec_decode_video2(dec_ctx, frame, &got_frame, &packet);
            if (ret < 0) {
                LOGI("Error decoding video\n");
                break;
            }
            if (got_frame) {
                frame->pts = av_frame_get_best_effort_timestamp(frame);
                /* push the decoded frame into the filtergraph */
                if (av_buffersrc_add_frame_flags(buffersrc_ctx, frame, AV_BUFFERSRC_FLAG_KEEP_REF) < 0) {
                    LOGI("Error while feeding the filtergraph\n");
                    break;
                }
                /* pull filtered frames from the filtergraph */
                while (1) {
                    ret = av_buffersink_get_frame(buffersink_ctx, filt_frame);
                    if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF)
                        break;
                    if (ret < 0)
                        goto end;
                    display_frame(filt_frame, buffersink_ctx->inputs[0]->time_base);
                    av_frame_unref(filt_frame);
                }
                av_frame_unref(frame);
            }
        }
        av_packet_unref(&packet);
    }
    end:
    avfilter_graph_free(&filter_graph);
    avcodec_close(dec_ctx);
    avformat_close_input(&fmt_ctx);
    av_frame_free(&frame);
    av_frame_free(&filt_frame);
    if (ret < 0 && ret != AVERROR_EOF) {
        LOGI("Error occurred: %s\n", av_err2str(ret));
        return;
    }
}

