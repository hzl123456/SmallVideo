#include <stdio.h>
#include <jni.h>
#include "vector.h"
#include "ffmpeg_encode_aac.h"
#include "ffmpeg_encode_h264.h"
#include "ffmpeg_encode_mp4.h"
#include "ffmpeg_watermark.h"
#include "android_log.h"
#include <thread>

FFmpegEncodeH264 *h264_encoder;
FFmpegEncodeAAC *aac_encoder;
FFmpegEncodeMp4 *mp4_encoder;
FFmpegWatermark *watermark;
Vector *timeVector;

//一些参数的保存
const char *h264_file_path;
const char *aac_file_path;
const char *mp4_file_path;

//视频帧相关，fps，视频宽高，处理的线程数
int rate;
int width;
int height;
int coreCount;

//线程等待执行
pthread_t thread;
JavaVM *g_jvm = NULL;
const char *methodName;
jobject obj;

void *encodeComplete(void *pojo) {
    while (true) {
        if (h264_encoder->isCompleted && aac_encoder->isCompleted) {
            //进行mp4的合成，然后通知java层进行ui的更新啊啥的
            if (mp4_encoder == NULL) {
                mp4_encoder = new FFmpegEncodeMp4();
            }
            mp4_encoder->getMP4File(h264_file_path, aac_file_path, mp4_file_path, timeVector->data);

            //Attach主线程
            JNIEnv *env;
            g_jvm->AttachCurrentThread(&env, NULL);

            jclass clazz = env->GetObjectClass(obj);
            jmethodID methodId = env->GetMethodID(clazz, methodName, "()V");
            env->CallVoidMethod(obj, methodId);

            //Detach主线程
            g_jvm->DetachCurrentThread();

            return 0;
        } else {
            std::chrono::milliseconds dura(50);
            std::this_thread::sleep_for(dura);
        }
    }
}

//-----------------------这边是录制视频文件需要的------------------------------
extern "C"
JNIEXPORT void JNICALL
Java_com_hzl_smallvideo_util_FFmpegUtil_initH264File(JNIEnv *env, jclass type, jstring filePath_,
                                                     jint rate_,
                                                     jint width_, jint height_, int coreCount_,
                                                     jstring filter_) {
    if (h264_encoder == NULL) {
        h264_encoder = new FFmpegEncodeH264();
    }
    if (timeVector == NULL) {
        timeVector = new Vector();
    }
    h264_file_path = env->GetStringUTFChars(filePath_, NULL);
    //视频相关数据的初始化
    rate = rate_;
    width = width_;
    height = height_;
    coreCount = coreCount_;
    //时间戳数据的初始化
    timeVector->vector_init();
    const char *filter = env->GetStringUTFChars(filter_, NULL);
    h264_encoder->initH264File(h264_file_path, rate, width, height, coreCount, filter);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_hzl_smallvideo_util_FFmpegUtil_pushDataToH264File(JNIEnv *env, jclass type,
                                                           jbyteArray src_, jlong time) {
    uint8_t *buffer = (uint8_t *) env->GetByteArrayElements(src_, NULL);
    h264_encoder->pushDataToH264File(buffer);
    //把时间戳添加到队列里面
    timeVector->vector_append(time);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_hzl_smallvideo_util_FFmpegUtil_endEcodeH264(JNIEnv *env, jclass type) {
    if (thread == NULL) {
        pthread_create(&thread, NULL, encodeComplete, NULL);
    }
    h264_encoder->endEncode();
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
Java_com_hzl_smallvideo_util_FFmpegUtil_endEcodeAAC(JNIEnv *env, jclass type) {
    if (thread == NULL) {
        pthread_create(&thread, NULL, encodeComplete, NULL);
    }
    aac_encoder->endEncode();
}

//-----------------------这边是将音视频文件合成需要的------------------------------
extern "C"
JNIEXPORT void JNICALL
Java_com_hzl_smallvideo_util_FFmpegUtil_initMP4File(JNIEnv *env, jclass type, jstring mp4FilePath_,
                                                    jobject obj_, jstring methodName_) {
    mp4_file_path = env->GetStringUTFChars(mp4FilePath_, 0);
    methodName = env->GetStringUTFChars(methodName_, 0);
    if (thread != NULL) {
        thread = NULL;
    }
    //保存全局的jvm和obj
    env->GetJavaVM(&g_jvm);
    obj = env->NewGlobalRef(obj_);
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
                                     outH264FilePath, aac_file_path, outMp4FilePath,
                                     timeVector->data);
}
