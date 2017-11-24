#include "ffmpeg_encode_aac.h"
#include "ffmpeg_config.h"

void FFmpegEncodeAAC::initAACFile(const char *filePath,int coreCount) {
    //输出aac的文件
    audio_i = 0;
    audio_out_file = filePath;
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
    audio_pCodecCtx->bit_rate = 128 * 1000;
    audio_pCodecCtx->thread_count = coreCount;

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

void FFmpegEncodeAAC::pushDataToAACFile(uint8_t *src_) {
    audio_frame_buf = src_;
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

void FFmpegEncodeAAC::getAACFile() {
    //Flush Encoder
    FFmpegConfig::flush_encoder(audio_pFormatCtx, 0);
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