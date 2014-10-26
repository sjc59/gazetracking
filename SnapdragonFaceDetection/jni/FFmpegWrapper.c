#include <imageprocessor/FFmpegWrapper.h>
#ifdef HAVE_NEON
#include <arm_neon.h>
#endif
#include <android/log.h>
#include <stdlib.h>
#include <stdio.h>
#include <time.h>
#include <imageprocessor/globals.h>
/*
 * Copyright 2011 - Churn Labs, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This is mostly based off of the FFMPEG tutorial:
 * http://dranger.com/ffmpeg/
 * With a few updates to support Android output mechanisms and to update
 * places where the APIs have shifted.
 */

#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <android/log.h>
#include <android/bitmap.h>

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/avutil.h>
#include <libswscale/swscale.h>

#define LOG_TAG "FFmpeg"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)

/* Cheat to keep things simple and just use some globals. */
AVFormatContext *pFormatCtx;
AVCodecContext *pCodecCtx;
AVCodec *pCodec;
AVFrame *pFrame;
AVFrame *pFrameYUV;
AVFrame *pFrameABGR;
int videoStream;
struct SwsContext *img_convert_ctx_abgr;
struct SwsContext *img_convert_ctx_yuv;
uint8_t *yuv_buffer;
uint8_t *abgr_buffer;
int yuv_numBytes;
int abgr_numBytes;
AVInputFormat *foo;
int64_t frame_num;
int64_t last_num = 0;
int last_ok = 0;

JNIEXPORT void JNICALL Java_org_stevej_android_facedetection_video_FFmpeg_closeFile(JNIEnv *env, jclass clazz) {
	av_free(pFrameYUV);
	LOGD("Freed pFrameYUV");
	av_free(pFrameABGR);
	LOGD("Freed pFrameRGBA");
	av_free(yuv_buffer);
	LOGD("Freed yuv_buffer");
	av_free(abgr_buffer);
	LOGD("Freed rgba_buffer");
	sws_freeContext(img_convert_ctx_abgr);
	sws_freeContext(img_convert_ctx_yuv);
	LOGD("Freed sws_freeContext");

	/*close the video codec*/
	avcodec_close(pCodecCtx);
	LOGD("Closed pCodecCtx");
	/*close the video file*/
	avformat_close_input(&pFormatCtx);
	LOGD("Closed pFormatCtx");
}

JNIEXPORT void JNICALL Java_org_stevej_android_facedetection_video_FFmpeg_resetToStart(JNIEnv *env, jclass clazz) {
	LOGD("resetToStart");
	if (seekMs(1000)==0) {
		LOGE("seek_frame to %d failed ", 0);
	}
}

JNIEXPORT jboolean JNICALL Java_org_stevej_android_facedetection_video_FFmpeg_openFile(JNIEnv * env, jclass clazz, jstring file_path, jintArray dimensions) {
	char *c_file_path = (char *) (*env)->GetStringUTFChars(env, file_path, NULL);
	jint *c_dimensions = (*env)->GetIntArrayElements(env, dimensions, NULL);

	LOGD("openFile(%s)", c_file_path);
	int err;
	int i;

	av_register_all();
	LOGD("Registered formats");
	pFormatCtx = avformat_alloc_context();
	LOGD("Allocated context");
	err = avformat_open_input(&pFormatCtx, c_file_path, NULL, NULL);
	LOGD("Called avformat_open_input : %s ", pFormatCtx->iformat->name);
	LOGD("Called avformat_open_input : %s ", pFormatCtx->iformat->long_name);
	LOGD("Called avformat_open_input : %d ", pFormatCtx->bit_rate);

	if (err != 0) {
		LOGE("Couldn't open file");
		(*env)->ReleaseStringUTFChars(env, file_path, c_file_path);
		(*env)->ReleaseIntArrayElements(env, dimensions, c_dimensions, 0);
		return JNI_FALSE;
	}
	LOGD("Opened file");

	if (avformat_find_stream_info(pFormatCtx, NULL) < 0) {
		LOGE("Unable to get stream info");
		(*env)->ReleaseStringUTFChars(env, file_path, c_file_path);
		(*env)->ReleaseIntArrayElements(env, dimensions, c_dimensions, 0);
		return JNI_FALSE;
	}
	LOGD("Got stream info");
	videoStream = -1;

	for (i = 0; i < pFormatCtx->nb_streams; i++) {
		LOGD("Stream %d : %s", i, avcodec_get_name(pFormatCtx->streams[i]->codec->codec_id));
		LOGD("Stream %d : %s", i, av_get_media_type_string(pFormatCtx->streams[i]->codec->codec_type));
		if (pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
			videoStream = i;
			break;
		}
	}
	LOGD("Got stream id");
	if (videoStream == -1) {
		LOGE("Unable to find video stream");
		(*env)->ReleaseStringUTFChars(env, file_path, c_file_path);
		(*env)->ReleaseIntArrayElements(env, dimensions, c_dimensions, 0);
		return JNI_FALSE;
	}

	LOGD("Video stream is [%d]", videoStream);

	pCodecCtx = pFormatCtx->streams[videoStream]->codec;

	LOGD("Got pCodecCtx");

	pCodec = avcodec_find_decoder(pCodecCtx->codec_id);

	LOGD("Got pCodec");

	if (pCodec == NULL) {
		LOGE("codec == NULL");
		(*env)->ReleaseStringUTFChars(env, file_path, c_file_path);
		(*env)->ReleaseIntArrayElements(env, dimensions, c_dimensions, 0);
		return JNI_FALSE;
	}

	if (avcodec_open2(pCodecCtx, pCodec, NULL) < 0) {
		LOGE("Unable to open codec");
		(*env)->ReleaseStringUTFChars(env, file_path, c_file_path);
		(*env)->ReleaseIntArrayElements(env, dimensions, c_dimensions, 0);
		return JNI_FALSE;
	}
	LOGD("Video size is [%d x %d], bit rate = %d", pCodecCtx->width, pCodecCtx->height, pCodecCtx->bit_rate);

	pFrameYUV = avcodec_alloc_frame();
	pFrameABGR = avcodec_alloc_frame();

	yuv_numBytes = avpicture_get_size(PIX_FMT_NV21, pCodecCtx->width, pCodecCtx->height);
	yuv_buffer = (uint8_t *) av_malloc(yuv_numBytes * sizeof(uint8_t) + FF_INPUT_BUFFER_PADDING_SIZE);
	LOGD("yuv numBytes is [%d]", yuv_numBytes);
	yuv_numBytes = avpicture_fill((AVPicture *) pFrameYUV, yuv_buffer, PIX_FMT_NV21, pCodecCtx->width, pCodecCtx->height);
	LOGD("yuv numBytes is [%d]", yuv_numBytes);

	abgr_numBytes = avpicture_get_size(PIX_FMT_ARGB, pCodecCtx->width, pCodecCtx->height);
	abgr_buffer = (uint8_t *) av_malloc(abgr_numBytes * sizeof(uint8_t) + FF_INPUT_BUFFER_PADDING_SIZE);
	LOGD("abgr numBytes is [%d]", abgr_numBytes);
	abgr_numBytes = avpicture_fill((AVPicture *) pFrameABGR, abgr_buffer, PIX_FMT_ARGB, pCodecCtx->width, pCodecCtx->height);
	LOGD("abgr numBytes is [%d]", abgr_numBytes);

	LOGD("Done avpicture_fill");

	c_dimensions[0] = pCodecCtx->width;
	c_dimensions[1] = pCodecCtx->height;
	c_dimensions[2] = yuv_numBytes;
//	c_dimensions[2] = abgr_numBytes;
	LOGD("Video size is [%d x %d]", c_dimensions[0], c_dimensions[1]);

	img_convert_ctx_abgr = sws_getContext(pCodecCtx->width, pCodecCtx->height, pCodecCtx->pix_fmt, pCodecCtx->width, pCodecCtx->height, PIX_FMT_RGBA, SWS_BICUBIC, NULL, NULL, NULL);
	img_convert_ctx_yuv = sws_getContext(pCodecCtx->width, pCodecCtx->height, pCodecCtx->pix_fmt, pCodecCtx->width, pCodecCtx->height, PIX_FMT_NV21, SWS_BICUBIC, NULL, NULL, NULL);

	LOGD("Done sws_getContext");

	(*env)->ReleaseStringUTFChars(env, file_path, c_file_path);
	(*env)->ReleaseIntArrayElements(env, dimensions, c_dimensions, 0);
	if (img_convert_ctx_abgr == NULL) {
		LOGE("could not initialize abgr conversion context\n");
		return JNI_FALSE;
	}
	if (img_convert_ctx_yuv == NULL) {
		LOGE("could not initialize yuv conversion context\n");
		return JNI_FALSE;
	}
	//LOGD("Exit openFile");
	return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_org_stevej_android_facedetection_video_FFmpeg_getFrameABGR(JNIEnv * env, jclass clazz, jintArray return_pixels) {
	jint *c_return_pixels = (*env)->GetIntArrayElements(env, return_pixels, NULL);
	//clock_t startTime = clock();

	int i = 0;
	int frameFinished = 0;
	AVPacket packet;
	av_init_packet(&packet);
	int state;

	pFrame = avcodec_alloc_frame();

	while ((i == 0) && ((state = av_read_frame(pFormatCtx, &packet)) >= 0)) {
		//LOGD("got packet");

		if (packet.stream_index == videoStream) {
			avcodec_decode_video2(pCodecCtx, pFrame, &frameFinished, &packet);
			//LOGD("decoded packet");

			if (frameFinished) {
				//LOGD("frame finished");
				//LOGD("Calling sws_scale");
				sws_scale(img_convert_ctx_abgr, (const uint8_t* const *) pFrame->data, pFrame->linesize, 0, pCodecCtx->height, pFrameABGR->data, pFrameABGR->linesize);

				//LOGD("Calling memcpy");
				memcpy(c_return_pixels, pFrameABGR->data[0], abgr_numBytes);
				i = 1;

				//LOGD("Done frame finished");
			}
		}
	}
	//LOGD("calling av_free_packet");
	av_free_packet(&packet);
	//LOGD("done av_free_packet");
	//LOGD("calling av_free(pFrame)");
	//if (pFrame != NULL) {
	av_free(pFrame);
	//}
	//LOGD("done av_free(pFrame)");

	//LOGD("%.8f", 1000.0 * (double)( clock() - startTime ) / (double)CLOCKS_PER_SEC);

	(*env)->ReleaseIntArrayElements(env, return_pixels, c_return_pixels, 0);

	if (state >= 0) {
		return JNI_TRUE;
	} else {
		return JNI_FALSE;
	}
}
JNIEXPORT jboolean JNICALL Java_org_stevej_android_facedetection_video_FFmpeg_getFrameYUV(JNIEnv * env, jclass clazz, jbyteArray return_pixels, jobject bitmap) {
	jbyte *c_return_pixels = (*env)->GetByteArrayElements(env, return_pixels, NULL);
	void* pixels;
	int i;
	int frameFinished = 0;
	AVPacket packet;
	i = 0;
	int state;

	AndroidBitmap_lockPixels(env, bitmap, &pixels);

	av_init_packet(&packet);
	pFrame = avcodec_alloc_frame();
	while ((i == 0) && ((state = av_read_frame(pFormatCtx, &packet)) >= 0)) {

		if (packet.stream_index == videoStream) {

			avcodec_decode_video2(pCodecCtx, pFrame, &frameFinished, &packet);

			if (frameFinished) {
				sws_scale(img_convert_ctx_yuv, (const uint8_t* const *) pFrame->data, pFrame->linesize, 0, pCodecCtx->height, pFrameYUV->data, pFrameYUV->linesize);
				memcpy(c_return_pixels, pFrameYUV->data[0], yuv_numBytes);

				sws_scale(img_convert_ctx_abgr, (const uint8_t* const *) pFrame->data, pFrame->linesize, 0, pCodecCtx->height, pFrameABGR->data, pFrameABGR->linesize);
				memcpy(pixels, pFrameABGR->data[0], abgr_numBytes);

				i = 1;
			}
		}
	}
	av_free_packet(&packet);
	av_free(pFrame);
	AndroidBitmap_unlockPixels(env, bitmap);
	(*env)->ReleaseByteArrayElements(env, return_pixels, c_return_pixels, 0);

	if (state >= 0) {
		return JNI_TRUE;
	} else {
		return JNI_FALSE;
	}
}

JNIEXPORT jboolean JNICALL Java_org_stevej_android_facedetection_video_FFmpeg_getFrame(JNIEnv * env, jclass clazz, jobject bitmap) {
	AndroidBitmapInfo info;
	void* pixels;
	int ret;

	int err;
	int i;
	int frameFinished = 0;
	AVPacket packet;
	static struct SwsContext *img_convert_ctx;
	int64_t seek_target;

	if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
		//LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
		return JNI_FALSE;
	}
	////LOGE("Checked on the bitmap");

	if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
		//LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
		return JNI_FALSE;
	}
////LOGE("Grabbed the pixels");

	i = 0;
	int state;
	while ((i == 0) && ((state = av_read_frame(pFormatCtx, &packet)) >= 0)) {

		//int state = av_read_frame(pFormatCtx, &packet);
		//if (state >= 0) {
		if (packet.stream_index == videoStream) {
			avcodec_decode_video2(pCodecCtx, pFrame, &frameFinished, &packet);

			if (frameFinished) {
				////LOGE("packet pts %llu", packet.pts);
				// This is much different than the tutorial, sws_scale
				// replaces img_convert, but it's not a complete drop in.
				// This version keeps the image the same size but swaps to
				// RGB24 format, which works perfect for PPM output.
//				img_convert_ctx = sws_getContext(pCodecCtx->width, pCodecCtx->height, pCodecCtx->pix_fmt, pCodecCtx->width, pCodecCtx->height, PIX_FMT_NV21, SWS_BICUBIC, NULL, NULL, NULL);
//				if (img_convert_ctx == NULL) {
//					//LOGE("could not initialize conversion context\n");
//					return JNI_FALSE;
//				}
				////LOGD("Calling sws_scale");
				sws_scale(img_convert_ctx_yuv, (const uint8_t* const *) pFrame->data, pFrame->linesize, 0, pCodecCtx->height, pFrameYUV->data, pFrameYUV->linesize);

				// save_frame(pFrameRGB, target_width, target_height, i);
				////LOGD("Setting bitmap pixels");
				//fill_bitmap(&info, pixels, pFrameRGB);
				memcpy(pixels, pFrameYUV->data[0], pCodecCtx->width * pCodecCtx->height * sizeof(uint32_t));
				i = 1;
				////LOGD("Done setting bitmap pixels");
			}
		}
		av_free_packet(&packet);
	}

	AndroidBitmap_unlockPixels(env, bitmap);

	if (state >= 0) {
		return JNI_TRUE;
	} else {
		return JNI_FALSE;
	}
}
int seekMs(int tsms) {

	frame_num = av_rescale(tsms, pFormatCtx->streams[videoStream]->time_base.den, pFormatCtx->streams[videoStream]->time_base.num);
	frame_num /= 1000;

	return seekFrame(frame_num);
}

int seekFrame(int64_t frame) {
	if ((last_ok == 0) || ((last_ok == 1) && (frame <= last_num || frame > last_num))) {
		if (avformat_seek_file(pFormatCtx, videoStream, 0, frame, frame, AVSEEK_FLAG_FRAME) < 0) {
			LOGI("avformat_seek_file failed for frame : %d",frame);
			last_ok = 0;
			return 0;
		}

		avcodec_flush_buffers(pCodecCtx);

		last_num = frame;
		last_ok = 1;
	}

	return 1;
}

int seek_frame(int tsms) {
	int64_t frame;

	frame = av_rescale(tsms, pFormatCtx->streams[videoStream]->time_base.den, pFormatCtx->streams[videoStream]->time_base.num);
	frame /= 1000;

	if (avformat_seek_file(pFormatCtx, videoStream, 0, frame, frame, AVSEEK_FLAG_FRAME) < 0) {
		return 0;
	}

	avcodec_flush_buffers(pCodecCtx);

	return 1;
}

JNIEXPORT void JNICALL Java_org_stevej_android_facedetection_video_FFmpeg_getFrameBitmapAt(JNIEnv * env, jclass clazz, jobject bitmap, jint secs) {
	LOGI("getFrameBitmapAt");
	AndroidBitmapInfo info;
	void* pixels;
	int ret;

	int err;
	int i;
	int frameFinished = 0;
	AVPacket packet;
	int64_t seek_target;

	if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
		LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
		return;
	}
	//LOGD("Got bitmap info");

	// check if bitmap is in desired format
	if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		LOGE("Bitmap format is not RGBA_8888 !");
		return;
	}
	//LOGD("Bitmap format OK");

	if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
		LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
	}
	//LOGD("Locked the pixels");

	if (seekMs(secs * 1000)==0) {
		LOGE("seek_frame to %d failed ", secs * 1000);
	}

	i = 0;
	pFrame = avcodec_alloc_frame();
	while ((i == 0) && (av_read_frame(pFormatCtx, &packet) >= 0)) {

		LOGD("av_read_frame success : stream index is [%d]", packet.stream_index);

		if (packet.stream_index == videoStream) {
			LOGD("Calling avcodec_decode_video2");

			avcodec_decode_video2(pCodecCtx, pFrame, &frameFinished, &packet);
			LOGD("Done avcodec_decode_video2 : frameFinished is [%d]", frameFinished);

			if (frameFinished) {
				// This is much different than the tutorial, sws_scale
				// replaces img_convert, but it's not a complete drop in.
				// This version keeps the image the same size but swaps to
				// RGB24 format, which works perfect for PPM output.
				//LOGD("Calling sws_getContext");
//				img_convert_ctx = sws_getContext(pCodecCtx->width, pCodecCtx->height, pCodecCtx->pix_fmt, pCodecCtx->width, pCodecCtx->height, PIX_FMT_RGBA, SWS_BICUBIC, NULL, NULL, NULL);
//				if (img_convert_ctx == NULL) {
//					LOGE("could not initialize conversion context\n");
//					return;
//				}
				LOGD("Calling sws_scale");
				sws_scale(img_convert_ctx_abgr, (const uint8_t* const *) pFrame->data, pFrame->linesize, 0, pCodecCtx->height, pFrameABGR->data, pFrameABGR->linesize);

				// save_frame(pFrameRGB, target_width, target_height, i);
				LOGD("Setting bitmap pixels");
				//fill_bitmap(&info, pixels, pFrameRGB);
				LOGD("%d x %d = %d, %d", pCodecCtx->width, pCodecCtx->height, pCodecCtx->width * pCodecCtx->height, abgr_numBytes);
				memcpy(pixels, pFrameABGR->data[0], abgr_numBytes);
				i = 1;
				LOGD("Done setting bitmap pixels");

				//sws_freeContext(img_convert_ctx);
				LOGD("Done sws_freeContext");
			}
		}
	}
	LOGD("Freeing the packet");
	av_free_packet(&packet);
	av_free(pFrame);

	//LOGD("Unlocking the bitmap pixels");
	AndroidBitmap_unlockPixels(env, bitmap);
	//LOGD("DONE getFrameBitmapAt");
}

