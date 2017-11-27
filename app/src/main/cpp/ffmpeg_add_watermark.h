#ifndef SMALLVIDEO_FFMPEG_ADD_WATERMARK_H
#define SMALLVIDEO_FFMPEG_ADD_WATERMARK_H

extern "C"
{
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavfilter/avfiltergraph.h>
#include <libavfilter/buffersink.h>
#include <libavfilter/buffersrc.h>
#include <libavutil/avutil.h>
#include <libswscale/swscale.h>
};

class Watermark {

public:
    void
    addWatermark(const char *inputPath, const char *outputPath, const char *watermarkPath, int left,
                 int top,
                 int width, int height);
};

#endif //SMALLVIDEO_FFMPEG_ADD_WATERMARK_H
