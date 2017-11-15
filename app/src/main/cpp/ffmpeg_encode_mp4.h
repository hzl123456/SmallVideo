#ifndef SMALLVIDEO_FFMPEG_ENCODE_MP4_H
#define SMALLVIDEO_FFMPEG_ENCODE_MP4_H

extern "C"
{
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libavutil/opt.h"
}

class FFmpegEncodeMp4 {
public:
    void getMP4File(const char *h264FilePath_, const char *aacFilePath_, const char *mp4FilePath_,
                    float fps);
};
#endif //SMALLVIDEO_FFMPEG_ENCODE_MP4_H
