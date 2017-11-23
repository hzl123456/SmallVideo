#include "ffmpeg_encode_h264.h"
#include "ffmpeg_config.h"

void FFmpegEncodeH264::initH264File(const char *filePath, int rate, int width, int height) {
//获取yuv数据和路径已经大小，一些数据的初始化
    video_i = 0;
    out_file = filePath;
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
    //视频的码率设置，540p默认为2M
    pCodecCtx->bit_rate = 2 * 1024 * 1024;
    //每xxx帧插入1个I帧，I帧越少，视频越小
    pCodecCtx->gop_size = 15;  //关键帧的间隔数
    //帧率的基本单位，我们用分数来表示，帧率通过外面传进来
    pCodecCtx->time_base = (AVRational) {1, rate};
    //编码的线程
    pCodecCtx->thread_count = 15;
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

void FFmpegEncodeH264::pushDataToH264File(uint8_t *src_) {
    picture_buf = src_;
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

void FFmpegEncodeH264::getH264File() {
    //Flush Encoder
    FFmpegConfig::flush_encoder(pFormatCtx, 0);
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