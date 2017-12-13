#include <stdio.h>
#include <jni.h>
#include "ffmpeg_encode_aac.h"
#include "ffmpeg_encode_h264.h"
#include "ffmpeg_encode_mp4.h"
#include "ffmpeg_watermark.h"

FFmpegEncodeH264 *h264_encoder;
FFmpegEncodeAAC *aac_encoder;
FFmpegEncodeMp4 *mp4_encoder;
FFmpegWatermark *watermark;

//一些参数的保存
const char *h264_file_path;
const char *aac_file_path;
const char *mp4_file_path;
const long *timeStamp;
int rate;
int width;
int height;
int coreCount;

extern "C"
JNIEXPORT void JNICALL
Java_com_hzl_smallvideo_util_FFmpegUtil_initH264File(JNIEnv *env, jclass type, jstring filePath_,
                                                     jint rate_,
                                                     jint width_, jint height_, int coreCount_,
                                                     jstring filter_) {
    if (h264_encoder == NULL) {
        h264_encoder = new FFmpegEncodeH264();
    }
    h264_file_path = env->GetStringUTFChars(filePath_, NULL);
    rate = rate_;
    width = width_;
    height = height_;
    coreCount = coreCount_;
    const char *filter = env->GetStringUTFChars(filter_, NULL);
    h264_encoder->initH264File(h264_file_path, rate, width, height, coreCount, filter);
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
Java_com_hzl_smallvideo_util_FFmpegUtil_initAACFile(JNIEnv *env, jclass type, jstring filePath_,
                                                    int coreCount_) {
    if (aac_encoder == NULL) {
        aac_encoder = new FFmpegEncodeAAC();
    }
    coreCount = coreCount_;
    aac_file_path = env->GetStringUTFChars(filePath_, NULL);
    aac_encoder->initAACFile(aac_file_path, coreCount);
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
Java_com_hzl_smallvideo_util_FFmpegUtil_getMP4File(JNIEnv *env, jclass type, jstring mp4FilePath_,
                                                   jlongArray timeStamp_) {
    if (mp4_encoder == NULL) {
        mp4_encoder = new FFmpegEncodeMp4();
    }
    mp4_file_path = env->GetStringUTFChars(mp4FilePath_, 0);
    timeStamp = (long *) env->GetLongArrayElements(timeStamp_, NULL);

    mp4_encoder->getMP4File(h264_file_path, aac_file_path, mp4_file_path, timeStamp);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_hzl_smallvideo_util_FFmpegUtil_addWatermark(JNIEnv *env, jclass type, jstring filter_,
                                                     jstring outH264FilePath_,
                                                     jstring outMp4FilePath_) {
    const char *filter = env->GetStringUTFChars(filter_, 0);
    const char *outH264FilePath = env->GetStringUTFChars(outH264FilePath_, 0);
    const char *outMp4FilePath = env->GetStringUTFChars(outMp4FilePath_, 0);

    if (watermark == NULL) {
        watermark = new FFmpegWatermark();
    }
    watermark->encode_watermark_file(h264_file_path, rate, width, height, coreCount, filter,
                                     outH264FilePath, aac_file_path, outMp4FilePath, timeStamp);
}
