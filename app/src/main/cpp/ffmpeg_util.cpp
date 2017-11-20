#include <stdio.h>
#include <jni.h>
#include "ffmpeg_encode_aac.h"
#include "ffmpeg_encode_h264.h"
#include "ffmpeg_encode_mp4.h"

FFmpegEncodeH264 *h264_encoder;
FFmpegEncodeAAC *aac_encoder;
FFmpegEncodeMp4 *mp4_encoder;

//-----------------------这边是录制视频文件需要的------------------------------
extern "C"
JNIEXPORT void JNICALL
Java_com_hzl_smallvideo_util_FFmpegUtil_initH264File(JNIEnv *env, jclass type, jstring filePath_,
                                                     jint rate,
                                                     jint width, jint height) {
    if (h264_encoder == NULL) {
        h264_encoder = new FFmpegEncodeH264();
    }
    const char *out_file = env->GetStringUTFChars(filePath_, NULL);
    h264_encoder->initH264File(out_file, rate, width, height);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_hzl_smallvideo_util_FFmpegUtil_pushDataToH264File(JNIEnv *env, jclass type,
                                                           jbyteArray src_) {
    uint8_t *buffer = (uint8_t *) env->GetByteArrayElements(src_, NULL);
    h264_encoder->pushDataToH264File(buffer);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_hzl_smallvideo_util_FFmpegUtil_getH264File(JNIEnv *env, jclass type) {
    h264_encoder->getH264File();
}

//-----------------------这边是录制音频文件需要的------------------------------
extern "C"
JNIEXPORT void JNICALL
Java_com_hzl_smallvideo_util_FFmpegUtil_initAACFile(JNIEnv *env, jclass type, jstring filePath_) {
    if (aac_encoder == NULL) {
        aac_encoder = new FFmpegEncodeAAC();
    }
    const char *file = env->GetStringUTFChars(filePath_, NULL);
    aac_encoder->initAACFile(file);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_hzl_smallvideo_util_FFmpegUtil_pushDataToAACFile(JNIEnv *env, jclass type,
                                                          jbyteArray src_) {
    uint8_t *buffer = (uint8_t *) env->GetByteArrayElements(src_, NULL);
    aac_encoder->pushDataToAACFile(buffer);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_hzl_smallvideo_util_FFmpegUtil_getAACFile(JNIEnv *env, jclass type) {
    aac_encoder->getAACFile();
}

//-----------------------这边是将音视频文件合成需要的------------------------------
extern "C"
JNIEXPORT void JNICALL
Java_com_hzl_smallvideo_util_FFmpegUtil_getMP4File(JNIEnv *env, jclass type, jstring h264FilePath_,
                                                   jstring aacFilePath_, jstring mp4FilePath_,
                                                   jint defaultFps, jdouble fps) {
    if (mp4_encoder == NULL) {
        mp4_encoder = new FFmpegEncodeMp4();
    }
    const char *in_filename_v = env->GetStringUTFChars(h264FilePath_, 0);
    const char *in_filename_a = env->GetStringUTFChars(aacFilePath_, 0);
    const char *out_filename = env->GetStringUTFChars(mp4FilePath_, 0);

    mp4_encoder->getMP4File(in_filename_v, in_filename_a, out_filename, defaultFps, fps);
}

//-----------------------------这边是添加水印需要---------------------------------
extern "C"
JNIEXPORT void JNICALL
Java_com_hzl_smallvideo_util_FFmpegUtil_addWatermark(JNIEnv *env, jclass type, jstring inputPath_,
                                                     jstring outputPath_, jstring watermarkPath_,
                                                     jint left, jint top, jint width, jint height) {
    const char *inputPath = env->GetStringUTFChars(inputPath_, 0);
    const char *outputPath = env->GetStringUTFChars(outputPath_, 0);
    const char *watermarkPath = env->GetStringUTFChars(watermarkPath_, 0);




}
