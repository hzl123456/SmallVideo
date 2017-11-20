#ifndef SMALLVIDEO_FFMPEG_ADD_WATERMARK_H
#define SMALLVIDEO_FFMPEG_ADD_WATERMARK_H


class Watermark {

public:
    void
    addWatermark(const char *inputPath, const char *outputPath, const char *watermarkPath, int left,
                 int top,
                 int width, int height);

};


#endif //SMALLVIDEO_FFMPEG_ADD_WATERMARK_H
