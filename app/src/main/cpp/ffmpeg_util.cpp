#include <stdio.h>
#include <jni.h>
#include <android/log.h>

extern "C" {
#include <libavutil/opt.h>
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
};

#ifndef LOG_TAG
#define LOG_TAG "FFmpegUtil"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG ,__VA_ARGS__) // 定义LOGD类型
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG ,__VA_ARGS__) // 定义LOGI类型
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,LOG_TAG ,__VA_ARGS__) // 定义LOGW类型
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG ,__VA_ARGS__) // 定义LOGE类型
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,LOG_TAG ,__VA_ARGS__) // 定义LOGF类型
#endif

//-----------------------公用的flush_encoder方法------------------------------
void flush_encoder(AVFormatContext *fmt_ctx, unsigned int stream_index) {
    int ret;
    int got_frame;
    AVPacket enc_pkt;
    if (!(fmt_ctx->streams[stream_index]->codec->codec->capabilities & CODEC_CAP_DELAY)) {
        return;
    }
    while (1) {
        enc_pkt.data = NULL;
        enc_pkt.size = 0;
        av_init_packet(&enc_pkt);
        ret = avcodec_encode_video2(fmt_ctx->streams[stream_index]->codec, &enc_pkt, NULL,
                                    &got_frame);
        av_frame_free(NULL);
        if (ret < 0)
            break;
        if (!got_frame) {
            ret = 0;
            break;
        }
        LOGI("Flush Encoder: Succeed to encode 1 frame!\tsize:%5d\n", enc_pkt.size);
        /* mux encoded frame */
        ret = av_write_frame(fmt_ctx, &enc_pkt);
        if (ret < 0)
            break;
    }
}

//-----------------------这边是录制视频文件需要的------------------------------
jint video_i;
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

extern "C"
JNIEXPORT void JNICALL
Java_com_hzl_smallvideo_util_FFmpegUtil_initH264File(JNIEnv *env, jclass type, jstring filePath_,
                                                     jint rate,
                                                     jint width, jint height) {
    //获取yuv数据和路径已经大小，一些数据的初始化
    video_i = 0;
    out_file = env->GetStringUTFChars(filePath_, NULL);
    y_size = width * height;

    //注册FFmpeg所有编解码器。
    av_register_all();
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
    //视频的码率设置
    pCodecCtx->bit_rate = 4 * 1000 * 1000;
    //每xxx帧插入1个I帧，I帧越少，视频越小
    pCodecCtx->gop_size = 15;  //关键帧的间隔数
    //帧率的基本单位，我们用分数来表示，帧率通过外面传进来
    pCodecCtx->time_base = (AVRational) {1, rate};
    //编码的线程
    pCodecCtx->thread_count = 12;
    //两个非B帧之间允许出现多少个B帧数
    pCodecCtx->max_b_frames = 0;
    pCodecCtx->qmin = 0;
    pCodecCtx->qmax = 51;

    // Set Option
    AVDictionary *param = 0;
    //H.264
    if (pCodecCtx->codec_id == AV_CODEC_ID_H264) {
        av_dict_set(&param, "preset", "slow", 0);
        av_dict_set(&param, "tune", "zerolatency", 0);
        av_dict_set(&param, "profile", "main", 0);
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
}

extern "C"
JNIEXPORT void JNICALL
Java_com_hzl_smallvideo_util_FFmpegUtil_pushDataToH264File(JNIEnv *env, jclass type,
                                                           jbyteArray src_) {
    picture_buf = (uint8_t *) env->GetByteArrayElements(src_, NULL);
    //将yuv数据进行写入
    pFrame->data[0] = picture_buf;              // Y
    pFrame->data[1] = picture_buf + y_size;      // U
    pFrame->data[2] = picture_buf + y_size * 5 / 4;  // V

    pFrame->pts = (video_i++);

    int got_picture = 0;
    //Encode
    avcodec_encode_video2(pCodecCtx, &pkt, pFrame, &got_picture);
    if (got_picture == 1) {
        pkt.stream_index = video_st->index;
        av_write_frame(pFormatCtx, &pkt);
        av_free_packet(&pkt);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_hzl_smallvideo_util_FFmpegUtil_getH264File(JNIEnv *env, jclass type) {
    //Flush Encoder
    flush_encoder(pFormatCtx, 0);
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

//-----------------------这边是录制音频文件需要的------------------------------
AVFormatContext *audio_pFormatCtx;
AVOutputFormat *audio_fmt;
AVStream *audio_st;
AVCodecContext *audio_pCodecCtx;
AVCodec *audio_pCodec;
uint8_t *audio_frame_buf;
AVFrame *audio_pFrame;
AVPacket audio_pkt;
jint audio_i;
const char *audio_out_file;

extern "C"
JNIEXPORT void JNICALL
Java_com_hzl_smallvideo_util_FFmpegUtil_initAACFile(JNIEnv *env, jclass type, jstring filePath_) {

    //输出aac的文件
    audio_i = 0;
    audio_out_file = env->GetStringUTFChars(filePath_, NULL);
    //进行注册
    av_register_all();

    //Method 1.
    audio_pFormatCtx = avformat_alloc_context();
    audio_fmt = av_guess_format(NULL, audio_out_file, NULL);
    audio_pFormatCtx->oformat = audio_fmt;

    //Open output URL
    avio_open(&audio_pFormatCtx->pb, audio_out_file, AVIO_FLAG_READ_WRITE);
    audio_st = avformat_new_stream(audio_pFormatCtx, 0);

    audio_pCodecCtx = audio_st->codec;
    audio_pCodecCtx->codec_id = AV_CODEC_ID_AAC;
    audio_pCodecCtx->codec_type = AVMEDIA_TYPE_AUDIO;
    audio_pCodecCtx->sample_fmt = AV_SAMPLE_FMT_S16;
    audio_pCodecCtx->sample_rate = 44100;
    audio_pCodecCtx->channel_layout = AV_CH_LAYOUT_MONO;
    audio_pCodecCtx->channels = av_get_channel_layout_nb_channels(audio_pCodecCtx->channel_layout);
    audio_pCodecCtx->bit_rate = 1024 * 1000;
    audio_pCodecCtx->thread_count = 8;

    //Show some information
    av_dump_format(audio_pFormatCtx, 0, audio_out_file, 1);
    audio_pCodec = avcodec_find_encoder(audio_pCodecCtx->codec_id);
    //open encoder
    avcodec_open2(audio_pCodecCtx, audio_pCodec, NULL);

    audio_pFrame = av_frame_alloc();
    audio_pFrame->nb_samples = audio_pCodecCtx->frame_size;
    audio_pFrame->format = audio_pCodecCtx->sample_fmt;

    int size = av_samples_get_buffer_size(NULL, audio_pCodecCtx->channels,
                                          audio_pCodecCtx->frame_size,
                                          audio_pCodecCtx->sample_fmt, 1);
    audio_frame_buf = (uint8_t *) av_malloc(size);
    avcodec_fill_audio_frame(audio_pFrame, audio_pCodecCtx->channels, audio_pCodecCtx->sample_fmt,
                             (const uint8_t *) audio_frame_buf, size, 1);
    //写入头部
    avformat_write_header(audio_pFormatCtx, NULL);
    av_new_packet(&audio_pkt, size);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_hzl_smallvideo_util_FFmpegUtil_pushDataToAACFile(JNIEnv *env, jclass type,
                                                          jbyteArray src_) {

    audio_frame_buf = (uint8_t *) env->GetByteArrayElements(src_, NULL);
    //将pcm文件进行写入的操作
    audio_pFrame->data[0] = audio_frame_buf;
    audio_pFrame->pts = (audio_i++);
    int got_frame = 0;
    //Encode
    avcodec_encode_audio2(audio_pCodecCtx, &audio_pkt, audio_pFrame, &got_frame);
    if (got_frame == 1) {
        audio_pkt.stream_index = audio_st->index;
        av_write_frame(audio_pFormatCtx, &audio_pkt);
        av_free_packet(&audio_pkt);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_hzl_smallvideo_util_FFmpegUtil_getAACFile(JNIEnv *env, jclass type) {
    //Flush Encoder
    flush_encoder(audio_pFormatCtx, 0);
    //Write Trailer
    av_write_trailer(audio_pFormatCtx);
    //Clean
    if (audio_st) {
        avcodec_close(audio_st->codec);
        av_free(audio_pFrame);
        //av_free(audio_frame_buf);
    }
    avio_close(audio_pFormatCtx->pb);
    avformat_free_context(audio_pFormatCtx);
}

//-----------------------这边是将音视频文件合成需要的------------------------------
extern "C"
JNIEXPORT void JNICALL
Java_com_hzl_smallvideo_util_FFmpegUtil_getMP4File(JNIEnv *env, jclass type, jstring h624FilePath_,
                                                   jstring aacFilePath_, jstring mp4FilePath_) {
    //文件路径信息
    const char *in_filename_v = env->GetStringUTFChars(h624FilePath_, 0);
    const char *in_filename_a = env->GetStringUTFChars(aacFilePath_, 0);
    const char *out_filename = env->GetStringUTFChars(mp4FilePath_, 0);

    AVOutputFormat *ofmt = NULL;
    //Input AVFormatContext and Output AVFormatContext  
    AVFormatContext *ifmt_ctx_v = NULL, *ifmt_ctx_a = NULL, *ofmt_ctx = NULL;
    AVPacket pkt;
    int i;
    int videoindex_v = -1, videoindex_out = -1;
    int audioindex_a = -1, audioindex_out = -1;
    int frame_index = 0;
    int64_t cur_pts_v = 0, cur_pts_a = 0;

    av_register_all();
    //Input  
    if (avformat_open_input(&ifmt_ctx_v, in_filename_v, 0, 0) < 0) {
        goto end;
    }
    if (avformat_find_stream_info(ifmt_ctx_v, 0) < 0) {
        goto end;
    }
    if (avformat_open_input(&ifmt_ctx_a, in_filename_a, 0, 0) < 0) {
        goto end;
    }
    if (avformat_find_stream_info(ifmt_ctx_a, 0) < 0) {
        goto end;
    }
    //输入信息
    av_dump_format(ifmt_ctx_v, 0, in_filename_v, 0);
    av_dump_format(ifmt_ctx_a, 0, in_filename_a, 0);
    //输出信息
    avformat_alloc_output_context2(&ofmt_ctx, NULL, NULL, out_filename);
    if (!ofmt_ctx) {
        goto end;
    }
    ofmt = ofmt_ctx->oformat;

    for (i = 0; i < ifmt_ctx_v->nb_streams; i++) {
        if (ifmt_ctx_v->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
            AVStream *in_stream = ifmt_ctx_v->streams[i];
            AVStream *out_stream = avformat_new_stream(ofmt_ctx, in_stream->codec->codec);
            videoindex_v = i;
            if (!out_stream) {
                goto end;
            }
            videoindex_out = out_stream->index;
            if (avcodec_copy_context(out_stream->codec, in_stream->codec) < 0) {
                goto end;
            }
            out_stream->codec->codec_tag = 0;
            if (ofmt_ctx->oformat->flags & AVFMT_GLOBALHEADER)
                out_stream->codec->flags |= CODEC_FLAG_GLOBAL_HEADER;
            break;
        }
    }

    for (i = 0; i < ifmt_ctx_a->nb_streams; i++) {
        if (ifmt_ctx_a->streams[i]->codec->codec_type == AVMEDIA_TYPE_AUDIO) {
            AVStream *in_stream = ifmt_ctx_a->streams[i];
            AVStream *out_stream = avformat_new_stream(ofmt_ctx, in_stream->codec->codec);
            audioindex_a = i;
            if (!out_stream) {
                goto end;
            }
            audioindex_out = out_stream->index;
            if (avcodec_copy_context(out_stream->codec, in_stream->codec) < 0) {
                goto end;
            }
            out_stream->codec->codec_tag = 0;
            if (ofmt_ctx->oformat->flags & AVFMT_GLOBALHEADER)
                out_stream->codec->flags |= CODEC_FLAG_GLOBAL_HEADER;
            break;
        }
    }
    av_dump_format(ofmt_ctx, 0, out_filename, 1);
    //Open output file
    if (!(ofmt->flags & AVFMT_NOFILE)) {
        if (avio_open(&ofmt_ctx->pb, out_filename, AVIO_FLAG_WRITE) < 0) {
            goto end;
        }
    }
    //Write file header  
    if (avformat_write_header(ofmt_ctx, NULL) < 0) {
        goto end;
    }
    //对于某些封装格式（例如MP4/FLV/MKV等）中的H.264，需要用到名称为“h264_mp4toannexb”的bitstream filter。
#if USE_H264BSF
    AVBitStreamFilterContext* h264bsfc =  av_bitstream_filter_init("h264_mp4toannexb");
#endif
    //对于某些封装格式（例如MP4/FLV/MKV等）中的AAC，需要用到名称为“aac_adtstoasc”的bitstream filter。
#if USE_AACBSF
    AVBitStreamFilterContext* aacbsfc =  av_bitstream_filter_init("aac_adtstoasc");
#endif
    while (1) {
        AVFormatContext *ifmt_ctx;
        int stream_index = 0;
        AVStream *in_stream, *out_stream;

        //Get an AVPacket  
        if (av_compare_ts(cur_pts_v, ifmt_ctx_v->streams[videoindex_v]->time_base, cur_pts_a,
                          ifmt_ctx_a->streams[audioindex_a]->time_base) <= 0) {
            ifmt_ctx = ifmt_ctx_v;
            stream_index = videoindex_out;

            if (av_read_frame(ifmt_ctx, &pkt) >= 0) {
                do {
                    in_stream = ifmt_ctx->streams[pkt.stream_index];
                    out_stream = ofmt_ctx->streams[stream_index];

                    if (pkt.stream_index == videoindex_v) {
                        //FIX：No PTS (Example: Raw H.264)
                        //Simple Write PTS
                        if (pkt.pts == AV_NOPTS_VALUE) {
                            //Write PTS  
                            AVRational time_base1 = in_stream->time_base;
                            //Duration between 2 frames (us)  
                            int64_t calc_duration =
                                    (double) AV_TIME_BASE / av_q2d(in_stream->r_frame_rate);
                            //Parameters  
                            pkt.pts = (double) (frame_index * calc_duration) /
                                      (double) (av_q2d(time_base1) * AV_TIME_BASE);
                            pkt.dts = pkt.pts;
                            pkt.duration = (double) calc_duration /
                                           (double) (av_q2d(time_base1) * AV_TIME_BASE);
                            frame_index++;
                        }
                        cur_pts_v = pkt.pts;
                        break;
                    }
                } while (av_read_frame(ifmt_ctx, &pkt) >= 0);
            } else {
                break;
            }
        } else {
            ifmt_ctx = ifmt_ctx_a;
            stream_index = audioindex_out;
            if (av_read_frame(ifmt_ctx, &pkt) >= 0) {
                do {
                    in_stream = ifmt_ctx->streams[pkt.stream_index];
                    out_stream = ofmt_ctx->streams[stream_index];
                    if (pkt.stream_index == audioindex_a) {
                        //FIX：No PTS
                        //Simple Write PTS  
                        if (pkt.pts == AV_NOPTS_VALUE) {
                            //Write PTS  
                            AVRational time_base1 = in_stream->time_base;
                            //Duration between 2 frames (us)  
                            int64_t calc_duration =
                                    (double) AV_TIME_BASE / av_q2d(in_stream->r_frame_rate);
                            //Parameters  
                            pkt.pts = (double) (frame_index * calc_duration) /
                                      (double) (av_q2d(time_base1) * AV_TIME_BASE);
                            pkt.dts = pkt.pts;
                            pkt.duration = (double) calc_duration /
                                           (double) (av_q2d(time_base1) * AV_TIME_BASE);
                            frame_index++;
                        }
                        cur_pts_a = pkt.pts;

                        break;
                    }
                } while (av_read_frame(ifmt_ctx, &pkt) >= 0);
            } else {
                break;
            }

        }
        //FIX:Bitstream Filter
#if USE_H264BSF
        av_bitstream_filter_filter(h264bsfc, in_stream->codec, NULL, &pkt.data, &pkt.size, pkt.data, pkt.size, 0);
#endif
#if USE_AACBSF
        av_bitstream_filter_filter(aacbsfc, out_stream->codec, NULL, &pkt.data, &pkt.size, pkt.data, pkt.size, 0);
#endif
        //Convert PTS/DTS
        pkt.pts = av_rescale_q_rnd(pkt.pts, in_stream->time_base, out_stream->time_base,
                                   (AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
        pkt.dts = av_rescale_q_rnd(pkt.dts, in_stream->time_base, out_stream->time_base,
                                   (AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
        pkt.duration = av_rescale_q(pkt.duration, in_stream->time_base, out_stream->time_base);
        pkt.pos = -1;
        pkt.stream_index = stream_index;
        //Write
        if (av_interleaved_write_frame(ofmt_ctx, &pkt) < 0) {
            break;
        }
        av_free_packet(&pkt);
    }
    //Write file trailer  
    av_write_trailer(ofmt_ctx);
#if USE_H264BSF
    av_bitstream_filter_close(h264bsfc);
#endif
#if USE_AACBSF
    av_bitstream_filter_close(aacbsfc);
#endif
    end:
    avformat_close_input(&ifmt_ctx_v);
    avformat_close_input(&ifmt_ctx_a);
    /* close output */
    if (ofmt_ctx && !(ofmt->flags & AVFMT_NOFILE)) {
        avio_close(ofmt_ctx->pb);
    }
    avformat_free_context(ofmt_ctx);
}

