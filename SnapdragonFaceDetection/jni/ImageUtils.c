#include <android/log.h>
#include <stdlib.h>
#include <imageprocessor/ImageUtils.h>
#include <fastcv/fastcv.h>
#include <arm_neon.h>

#define HSV_YELLOW_H_LOW 40//30
#define HSV_YELLOW_H_HIGH 65//40
#define HSV_YELLOW_S_LOW 230
#define HSV_YELLOW_S_HIGH 255
#define HSV_YELLOW_V_LOW 110
#define HSV_YELLOW_V_HIGH 255

#define LOG_TAG "ImageUtils"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

uint32_t num_contours;
uint32_t num_contours_to_find = 20; // ((double) max_num_contours / 100.0) * (frame_size / 200);
uint32_t num_points_in_contours[20];
uint32_t** contour_start_points = NULL;
uint32_t point_buffer_size;
uint32_t* point_buffer = NULL;
void* contour_handle = NULL;
uint8_t* rgb_888 = NULL;
uint8_t* bgr_888 = NULL;
uint8_t* hsv_888 = NULL;
uint8_t* mask = NULL;

JNIEXPORT void JNICALL Java_org_stevej_android_facedetection_utils_ImageUtils_RGBAToYUV(JNIEnv *env, jclass clazz, jintArray rgba, jbyteArray yuv, jint width, jint height) {
	jbyte *c_yuv;
	jint *c_rgba;

	c_yuv = (*env)->GetByteArrayElements(env, yuv, NULL);
	c_rgba = (*env)->GetIntArrayElements(env, rgba, NULL);

	fcvColorRGBA8888ToYCbCr420PseudoPlanaru8((uint8_t*) c_rgba, width, height, 0, c_yuv, c_yuv + (width * height), 0, 0);

	(*env)->ReleaseIntArrayElements(env, rgba, c_rgba, 0);
	(*env)->ReleaseByteArrayElements(env, yuv, c_yuv, 0);
}

JNIEXPORT void JNICALL Java_org_stevej_android_facedetection_utils_ImageUtils_init(JNIEnv * env, jclass clazz, jint width, jint height) {
	contour_start_points = (uint32_t**) malloc(num_contours_to_find * sizeof(uint32_t*));

	point_buffer_size = (width * height) / 2;
	point_buffer = (uint32_t*) malloc(point_buffer_size * sizeof(uint32_t*));
	contour_handle = fcvFindContoursAllocate(width);

	rgb_888 = (uint8_t*) malloc(width * height * 3);
	bgr_888 = (uint8_t*) malloc(width * height * 3);
	hsv_888 = (uint8_t*) malloc(width * height * 3);
	mask = (uint8_t*) malloc(width * height);
}
JNIEXPORT void JNICALL Java_org_stevej_android_facedetection_utils_ImageUtils_deinit(JNIEnv * env, jclass clazz) {
	if (point_buffer != NULL) {
		free(point_buffer);
	}
	free(rgb_888);
	free(bgr_888);
	free(hsv_888);
	free(mask);
	if (contour_handle != NULL) {
		fcvFindContoursDelete(contour_handle);
	}

	contour_start_points = NULL;
	point_buffer = NULL;
	contour_handle = NULL;
	rgb_888 = NULL;
	bgr_888 = NULL;
	hsv_888 = NULL;
	mask = NULL;
}
JNIEXPORT void JNICALL Java_org_stevej_android_facedetection_utils_ImageUtils_findMarker(JNIEnv * env, jclass clazz, jbyteArray yuv, jint width, jint height, jintArray marker, jboolean mirroring) {
	jbyte *c_yuv;
	jint *c_marker;

	c_yuv = (*env)->GetByteArrayElements(env, yuv, NULL);
	c_marker = (*env)->GetIntArrayElements(env, marker, NULL);

	fcvColorYCbCr420PseudoPlanarToRGB888u8((uint8_t*) c_yuv, (uint8_t*) (c_yuv + (width * height)), width, height, 0, 0, rgb_888, 0);
	fcvColorRGB888ToBGR888u8(rgb_888, width, height, 0, bgr_888, 0);
	fcvColorRGB888ToHSV888u8(bgr_888, width, height, width * 3, hsv_888, width * 3);

	in_range_neon(mask, hsv_888, (width * height * 3) / 24, (uint8_t) 35, (uint8_t) 50, (uint8_t) 127, (uint8_t) 255, (uint8_t) 127, (uint8_t) 255);

	fcvFindContoursListu8(mask, width, height, width, num_contours_to_find, &num_contours, num_points_in_contours, contour_start_points, point_buffer, point_buffer_size, contour_handle);

	int x1 = 0;
	int y1 = 0;
	int x2 = 0;
	int y2 = 0;
	int largest = 0;
	int i;
	for (i = 0; i < num_contours; i++) {
		uint32_t* sp = contour_start_points[i];
		uint32_t num_points = num_points_in_contours[i];
		uint32_t x, y, w, h;
		fcvBoundingRectangle(sp, num_points, &x, &y, &w, &h);
//		LOGD("contour %d : %d,%d  %d,%d", i, x, y, (x + w - 1), (y + h - 1));
		if (w * h > largest) {
			x1 = x;
			y1 = y;
			x2 = x + w - 1;
			y2 = y + h - 1;
			largest = w * h;
		}
	}
//	LOGD("contour : %d,%d  %d,%d", x1, y1, x2, y2);
	if (mirroring) {
		x1 = width - x1 - 1;
		x2 = width - x2 - 1;
	}
	c_marker[0] = x1;
	c_marker[1] = y1;
	c_marker[2] = x2;
	c_marker[3] = y2;

	(*env)->ReleaseIntArrayElements(env, marker, c_marker, 0);
	(*env)->ReleaseByteArrayElements(env, yuv, c_yuv, 0);
}

JNIEXPORT void JNICALL Java_org_stevej_android_facedetection_utils_ImageUtils_GtoAGGG(JNIEnv *env, jclass clazz, jbyteArray in, jint width, jint height, jintArray out) {
	jbyte *c_in;
	c_in = (*env)->GetByteArrayElements(env, in, NULL);
	jint *c_out;
	c_out = (*env)->GetIntArrayElements(env, out, NULL);

	int i;
	for (i = 0; i < (width * height); i++) {
		uint8_t grey = c_in[i];
//		c_out[i] = 0xEE000000 | grey << 16 | grey << 8 | grey;
//		c_out[i] = (grey << 24) & 0xFFFF0000;
		if (grey == 0) {
			c_out[i] = 0x0000000;
		} else {
			c_out[i] = 0xFF0000FF;
		}
	}

	(*env)->ReleaseByteArrayElements(env, in, c_in, 0);
	(*env)->ReleaseIntArrayElements(env, out, c_out, 0);

}

