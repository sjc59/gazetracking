/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_stevej_android_facedetection_OpenCV */

#ifndef _Included_org_stevej_android_facedetection_video_FFmpeg
#define _Included_org_stevej_android_facedetection_video_FFmpeg
#ifdef __cplusplus
extern "C" {
#endif

//JNIEXPORT void JNICALL Java_org_stevej_android_facedetection_video_FFmpeg_getVideoInfo(JNIEnv *, jclass, jstring);
JNIEXPORT void JNICALL Java_org_stevej_android_facedetection_video_FFmpeg_resetToStart(JNIEnv *, jclass);
JNIEXPORT void JNICALL Java_org_stevej_android_facedetection_video_FFmpeg_closeFile(JNIEnv *, jclass);
JNIEXPORT jboolean JNICALL Java_org_stevej_android_facedetection_video_FFmpeg_openFile(JNIEnv *, jclass, jstring, jintArray);
JNIEXPORT jboolean JNICALL Java_org_stevej_android_facedetection_video_FFmpeg_getFrame(JNIEnv *, jclass, jobject);
JNIEXPORT jboolean JNICALL Java_org_stevej_android_facedetection_video_FFmpeg_getFrameYUV(JNIEnv *, jclass, jbyteArray, jobject);
JNIEXPORT jboolean JNICALL Java_org_stevej_android_facedetection_video_FFmpeg_getFrameABGR(JNIEnv * , jclass , jintArray );
JNIEXPORT void JNICALL Java_org_stevej_android_facedetection_video_FFmpeg_getFrameBitmapAt(JNIEnv *, jclass, jobject, jint);
#ifdef __cplusplus
}
#endif
#endif

