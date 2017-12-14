#include "ffmpeg_watermark.h"
#include "android_log.h"

long getCurrentTime() {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

void FFmpegWatermark::decode(AVCodecContext *dec_ctx, AVFrame *frame, AVPacket *pkt) {
    int ret;
    ret = avcodec_send_packet(dec_ctx, pkt);
    if (ret < 0) {
        LOGI("Error sending a packet for decoding\n");
        exit(1);
    }
    while (ret >= 0) {
        ret = avcodec_receive_frame(dec_ctx, frame);
        if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF)
            return;
        else if (ret < 0) {
            LOGI("Error during decoding\n");
            exit(1);
        }
        LOGI("saving frame %3d\n", dec_ctx->frame_number);
        //对解码出来的yuv数据进行重新编码操作
        for (int i = 0; i < buffer_height; i++) {
            memccpy(buffer + buffer_width * i, frame->data[0] + frame->linesize[0] * i, 1,
                    buffer_width);
        }
        for (int i = 0; i < buffer_height / 2; i++) {
            memccpy(buffer + buffer_y_size + buffer_width / 2 * i,
                    frame->data[1] + frame->linesize[1] * i, 1,
                    buffer_width);
        }
        for (int i = 0; i < buffer_height / 2; i++) {
            memccpy(buffer + buffer_y_size + buffer_u_size + buffer_width / 2 * i,
                    frame->data[2] + frame->linesize[2] * i, 1,
                    buffer_width);
        }
        long time = getCurrentTime();
        h264_encoder->pushDataToH264File(buffer);
        LOGI("time:%ld", (getCurrentTime() - time));
    }
}

void FFmpegWatermark::decode_h264_file(const char *inputPath,
                                       const char *in_filename_v, const char *in_filename_a,
                                       const char *out_filename, const long *timeStamp) {
    avcodec_register_all();
    pkt = av_packet_alloc();
    if (!pkt) exit(1);
    /* set end of buffer to 0 (this ensures that no overreading happens for damaged MPEG streams) */
    memset(inbuf + INBUF_SIZE, 0, AV_INPUT_BUFFER_PADDING_SIZE);
    /* find the MPEG-1 video decoder */
    codec = avcodec_find_decoder(AV_CODEC_ID_H264);
    if (!codec) {
        LOGI("Codec not found\n");
        exit(1);
    }
    parser = av_parser_init(codec->id);
    if (!parser) {
        LOGI("parser not found\n");
        exit(1);
    }
    c = avcodec_alloc_context3(codec);
    if (!c) {
        LOGI("Could not allocate video codec context\n");
        exit(1);
    }
    /* For some codecs, such as msmpeg4 and mpeg4, width and height
       MUST be initialized there because this information is not
       available in the bitstream. */
    /* open it */
    if (avcodec_open2(c, codec, NULL) < 0) {
        LOGI("Could not open codec\n");
        exit(1);
    }
    f = fopen(inputPath, "rb");
    if (!f) {
        LOGI("Could not open %s\n", inputPath);
        exit(1);
    }
    frame = av_frame_alloc();
    if (!frame) {
        LOGI("Could not allocate video frame\n");
        exit(1);
    }
    while (!feof(f)) {
        /* read raw data from the input file */
        data_size = fread(inbuf, 1, INBUF_SIZE, f);
        if (!data_size)
            break;
        /* use the parser to split the data into frames */
        data = inbuf;
        while (data_size > 0) {
            ret = av_parser_parse2(parser, c, &pkt->data, &pkt->size,
                                   data, data_size, AV_NOPTS_VALUE, AV_NOPTS_VALUE, 0);
            if (ret < 0) {
                LOGI("Error while parsing\n");
                exit(1);
            }
            data += ret;
            data_size -= ret;
            if (pkt->size)
                decode(c, frame, pkt);
        }
    }
    /* flush the decoder */
    decode(c, frame, NULL);
    fclose(f);
    av_parser_close(parser);
    avcodec_free_context(&c);
    av_frame_free(&frame);
    av_packet_free(&pkt);

    //完成之后先进行h264文件的生成，然后进行mp4文件的合成
    h264_encoder->getH264File();
    mp4_encoder->getMP4File(in_filename_v, in_filename_a, out_filename, timeStamp);
    //完成之后删除多余的文件，只保留最终输出的文件
    remove(in_filename_v);
    remove(in_filename_a);
    remove(inputPath);
}

void FFmpegWatermark::encode_watermark_file(const char *filePath, int rate, int width, int height,
                                            int coreCount, const char *filter,
                                            const char *in_filename_v, const char *in_filename_a,
                                            const char *out_filename, const long *timeStamp) {
    buffer_width = width;
    buffer_height = height;
    buffer_y_size = buffer_width * buffer_height;
    buffer_u_size = buffer_y_size / 4;
    buffer = (uint8_t *) malloc(buffer_y_size * 3 / 2);
    //创建h264编码器
    h264_encoder = new FFmpegEncodeH264();
    h264_encoder->initH264File(in_filename_v, rate, width, height, coreCount, filter);
    //创建合成mp4的合成器
    mp4_encoder = new FFmpegEncodeMp4();
    //进行解码操作
    decode_h264_file(filePath, in_filename_v, in_filename_a, out_filename, timeStamp);
}

