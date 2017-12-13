//
// Created by hzl on 2017/12/6.
//

#ifndef SMALLVIDEO_FFMPEG_WATERMARK_H
#define SMALLVIDEO_FFMPEG_WATERMARK_H
#define INBUF_SIZE 4096

#include "ffmpeg_encode_h264.h"
#include "ffmpeg_encode_mp4.h"


extern "C"
{
#include "libavcodec/avcodec.h"
}

class FFmpegWatermark {

public:

    void
    encode_watermark_file(const char *filePath, int rate, int width, int height, int coreCount,
                          const char *filter,
                          const char *in_filename_v, const char *in_filename_a,
                          const char *out_filename, const long *timeStamp);

private:

    void
    decode_h264_file(const char *inputPath, const char *in_filename_v, const char *in_filename_a,
                     const char *out_filename, const long *timeStamp);

    void decode(AVCodecContext *dec_ctx, AVFrame *frame, AVPacket *pkt);


    const AVCodec *codec;
    AVCodecParserContext *parser;
    AVCodecContext *c = NULL;
    FILE *f;
    AVFrame *frame;
    uint8_t inbuf[INBUF_SIZE + AV_INPUT_BUFFER_PADDING_SIZE];
    uint8_t *data;
    size_t data_size;
    int ret;
    AVPacket *pkt;
    uint8_t *buffer;
    int buffer_width;
    int buffer_height;
    int buffer_y_size;
    int buffer_u_size;
    FFmpegEncodeH264 *h264_encoder;
    FFmpegEncodeMp4 *mp4_encoder;
};


#endif //SMALLVIDEO_FFMPEG_WATERMARK_H
