#include <jni.h>
#include <string>
#include "libyuv.h"

//分别用来存储1420，1420缩放，I420旋转和镜像的数据
static jbyte *Src_i420_data;
static jbyte *Src_i420_data_scale;
static jbyte *Src_i420_data_rotate;
//添加水印相关的使用
static jbyte *Src_i420_data_water;
static jint water_width;
static jint water_height;
static jint water_start_x;
static jint water_start_y;

extern "C"
JNIEXPORT void JNICALL
Java_com_libyuv_util_YuvUtil_init(JNIEnv *env, jclass type, jint width,
                                  jint height, jint dst_width,
                                  jint dst_height) {
    Src_i420_data = (jbyte *) malloc(sizeof(jbyte) * width * height * 3 / 2);
    Src_i420_data_scale = (jbyte *) malloc(sizeof(jbyte) * dst_width * dst_height * 3 / 2);
    Src_i420_data_rotate = (jbyte *) malloc(sizeof(jbyte) * dst_width * dst_height * 3 / 2);
}

JNIEXPORT void JNI_OnUnload(JavaVM *jvm, void *reserved) {
    //进行释放
    free(Src_i420_data);
    free(Src_i420_data_scale);
    free(Src_i420_data_rotate);
}

void scaleI420(jbyte *src_i420_data, jint width, jint height, jbyte *dst_i420_data, jint dst_width,
               jint dst_height, jint mode) {

    jint src_i420_y_size = width * height;
    jint src_i420_u_size = (width >> 1) * (height >> 1);
    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data + src_i420_y_size;
    jbyte *src_i420_v_data = src_i420_data + src_i420_y_size + src_i420_u_size;

    jint dst_i420_y_size = dst_width * dst_height;
    jint dst_i420_u_size = (dst_width >> 1) * (dst_height >> 1);
    jbyte *dst_i420_y_data = dst_i420_data;
    jbyte *dst_i420_u_data = dst_i420_data + dst_i420_y_size;
    jbyte *dst_i420_v_data = dst_i420_data + dst_i420_y_size + dst_i420_u_size;

    libyuv::I420Scale((const uint8 *) src_i420_y_data, width,
                      (const uint8 *) src_i420_u_data, width >> 1,
                      (const uint8 *) src_i420_v_data, width >> 1,
                      width, height,
                      (uint8 *) dst_i420_y_data, dst_width,
                      (uint8 *) dst_i420_u_data, dst_width >> 1,
                      (uint8 *) dst_i420_v_data, dst_width >> 1,
                      dst_width, dst_height,
                      (libyuv::FilterMode) mode);
}

void rotateI420(jbyte *src_i420_data, jint width, jint height, jbyte *dst_i420_data, jint degree) {
    jint src_i420_y_size = width * height;
    jint src_i420_u_size = (width >> 1) * (height >> 1);

    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data + src_i420_y_size;
    jbyte *src_i420_v_data = src_i420_data + src_i420_y_size + src_i420_u_size;

    jbyte *dst_i420_y_data = dst_i420_data;
    jbyte *dst_i420_u_data = dst_i420_data + src_i420_y_size;
    jbyte *dst_i420_v_data = dst_i420_data + src_i420_y_size + src_i420_u_size;

    if (degree == libyuv::kRotate90 || degree == libyuv::kRotate270) {
        libyuv::I420Rotate((const uint8 *) src_i420_y_data, width,
                           (const uint8 *) src_i420_u_data, width >> 1,
                           (const uint8 *) src_i420_v_data, width >> 1,
                           (uint8 *) dst_i420_y_data, height,
                           (uint8 *) dst_i420_u_data, height >> 1,
                           (uint8 *) dst_i420_v_data, height >> 1,
                           width, height,
                           (libyuv::RotationMode) degree);
    }
}

void mirrorI420(jbyte *src_i420_data, jint width, jint height, jbyte *dst_i420_data) {
    jint src_i420_y_size = width * height;
    jint src_i420_u_size = (width >> 1) * (height >> 1);

    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data + src_i420_y_size;
    jbyte *src_i420_v_data = src_i420_data + src_i420_y_size + src_i420_u_size;

    jbyte *dst_i420_y_data = dst_i420_data;
    jbyte *dst_i420_u_data = dst_i420_data + src_i420_y_size;
    jbyte *dst_i420_v_data = dst_i420_data + src_i420_y_size + src_i420_u_size;

    libyuv::I420Mirror((const uint8 *) src_i420_y_data, width,
                       (const uint8 *) src_i420_u_data, width >> 1,
                       (const uint8 *) src_i420_v_data, width >> 1,
                       (uint8 *) dst_i420_y_data, width,
                       (uint8 *) dst_i420_u_data, width >> 1,
                       (uint8 *) dst_i420_v_data, width >> 1,
                       width, height);
}


void nv21ToI420(jbyte *src_nv21_data, jint width, jint height, jbyte *src_i420_data) {
    jint src_y_size = width * height;
    jint src_u_size = (width >> 1) * (height >> 1);

    jbyte *src_nv21_y_data = src_nv21_data;
    jbyte *src_nv21_vu_data = src_nv21_data + src_y_size;

    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data + src_y_size;
    jbyte *src_i420_v_data = src_i420_data + src_y_size + src_u_size;


    libyuv::NV21ToI420((const uint8 *) src_nv21_y_data, width,
                       (const uint8 *) src_nv21_vu_data, width,
                       (uint8 *) src_i420_y_data, width,
                       (uint8 *) src_i420_u_data, width >> 1,
                       (uint8 *) src_i420_v_data, width >> 1,
                       width, height);
}

void addWaterMark(jbyte *src_data, jint width, jint height) {
    if (water_start_x + water_width > width || water_start_y + water_height > height) {
        return;
    }
    if (water_start_x % 2 != 0 || water_start_y % 2 != 0) {
        return;
    }
    jint src_y_size = width * height;
    jint src_u_size = (width >> 1) * (height >> 1);
    jint src_water_y_size = water_width * water_height;
    jint src_water_u_size = (water_width >> 1) * (water_height >> 1);
    //计算得到偏移量
    jint start_y_size = water_start_x + water_start_y * width;
    jint start_u_size = (water_start_x >> 1) + (water_start_y >> 1) * (width >> 1);
    //复制y的数据，过滤需要研究下
    for (int i = 0; i < src_water_y_size; i++) {
        if (Src_i420_data_water[i] == 16) { //边缘会有问题，待解决
            continue;
        }
        src_data[start_y_size + width * (i / water_width) +
                 i % water_width] = Src_i420_data_water[i];
    }
    //复制u的数据
    jbyte *src_u_data = src_data + src_y_size;
    jbyte *src_water_u_data = Src_i420_data_water + src_water_y_size;
    for (int i = 0; i < (water_height >> 1); i++) {
        memcpy(start_u_size + src_u_data + (width >> 1) * i,
               src_water_u_data + (water_width >> 1) * i, water_width >> 1);
    }
    //复制v的数据
    jbyte *src_v_data = src_data + src_y_size + src_u_size;
    jbyte *src_water_v_data = Src_i420_data_water + src_water_y_size + src_water_u_size;
    for (int i = 0; i < (water_height >> 1); i++) {
        memcpy(start_u_size + src_v_data + (width >> 1) * i,
               src_water_v_data + (water_width >> 1) * i, water_width >> 1);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_libyuv_util_YuvUtil_compressYUV(JNIEnv *env, jclass type,
                                         jbyteArray src_, jint width,
                                         jint height, jbyteArray dst_,
                                         jint dst_width, jint dst_height,
                                         jint mode, jint degree,
                                         jboolean isMirror) {
    jbyte *Src_data = env->GetByteArrayElements(src_, NULL);
    jbyte *Dst_data = env->GetByteArrayElements(dst_, NULL);
    //nv21转化为i420
    nv21ToI420(Src_data, width, height, Src_i420_data);
    //进行缩放的操作
    scaleI420(Src_i420_data, width, height, Src_i420_data_scale, dst_width, dst_height, mode);
    if (isMirror) {
        //进行旋转的操作
        rotateI420(Src_i420_data_scale, dst_width, dst_height, Src_i420_data_rotate, degree);
        //因为旋转的角度都是90和270，那后面的数据width和height是相反的
        mirrorI420(Src_i420_data_rotate, dst_height, dst_width, Dst_data);
    } else {
        rotateI420(Src_i420_data_scale, dst_width, dst_height, Dst_data, degree);
    }
    //最后进行水印的添加
    addWaterMark(Dst_data, dst_height, dst_width);
    env->ReleaseByteArrayElements(dst_, Dst_data, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_libyuv_util_YuvUtil_initWaterMark(JNIEnv *env, jclass type, jbyteArray src_, jint width,
                                           jint height, jint startX,
                                           jint startY) {
    //水印的argb数据
    jbyte *src = env->GetByteArrayElements(src_, NULL);
    water_width = width;
    water_height = height;
    water_start_x = startX;
    water_start_y = startY;
    //水印的yuv数据
    Src_i420_data_water = (jbyte *) malloc(sizeof(jbyte) * width * height * 3 / 2);

    jint src_y_size = width * height;
    jint src_u_size = (width >> 1) * (height >> 1);

    jbyte *src_i420_y_data = Src_i420_data_water;
    jbyte *src_i420_u_data = Src_i420_data_water + src_y_size;
    jbyte *src_i420_v_data = Src_i420_data_water + src_y_size + src_u_size;

    //将argb转化为yuvi420，argb888的是x4，argb444的是x2
    //libyuv表示的排列顺序和Bitmap的RGBA表示的顺序是反向的。所以实际要调用libyuv::ABGRToI420才能得到正确的结果。
    libyuv::ABGRToI420((const uint8 *) src, width << 2,
                       (uint8 *) src_i420_y_data, width,
                       (uint8 *) src_i420_u_data, width >> 1,
                       (uint8 *) src_i420_v_data, width >> 1,
                       width, height);

}





