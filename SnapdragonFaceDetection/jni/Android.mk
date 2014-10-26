LOCAL_PATH := $(call my-dir)

FFMPEG_LIB_TYPE := shared
FACEDETECT_LIB_TYPE := shared

## our app needs the FFmpeg shared (dynamic) library
ifeq ($(FFMPEG_LIB_TYPE),shared)
	include $(CLEAR_VARS)
	LOCAL_ARM_MODE := arm
	LOCAL_MODULE := ffmpeg_prebuilt
	LOCAL_SRC_FILES := ../lib/$(TARGET_ARCH_ABI)/ffmpeg/libffmpeg.so 
	include $(PREBUILT_SHARED_LIBRARY)
endif

ifeq ($(FACEDETECT_LIB_TYPE),shared)
	include $(CLEAR_VARS)
	LOCAL_ARM_MODE := arm
	LOCAL_MODULE := facedetect_prebuilt
	LOCAL_SRC_FILES := ../lib/armeabi/libfacialproc_jni.so 
	include $(PREBUILT_SHARED_LIBRARY)
endif

## our app needs the FastCV static library
include $(CLEAR_VARS)
LOCAL_ARM_MODE := arm
LOCAL_MODULE := fastcv_prebuilt
LOCAL_SRC_FILES := ../lib/fastcv/libfastcv.a 
include $(PREBUILT_STATIC_LIBRARY)


include $(CLEAR_VARS)
LOCAL_ARM_MODE := arm
LOCAL_MODULE    := FaceDetection

########################################
#     add necessary include paths      #
########################################

LOCAL_C_INCLUDES += $(LOCAL_PATH)/include
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include/imageprocessor
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include/fastcv
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include/ffmpeg/$(TARGET_ARCH_ABI)



###################################
#     add C/C++ source files      #
###################################

LOCAL_SRC_FILES :=   globals.c  FFmpegWrapper.c  ImageUtils.c inrange.neon.S splitHSV.neon.S


#########################################################################
#     add support for native Android logging and  bitmap manipulation   #
#########################################################################

LOCAL_LDLIBS :=   -llog -ljnigraphics

## need to support C++ exceptions and runtime type identification
LOCAL_CPP_FEATURES := exceptions rtti


#################################
#     add shared libraries      #
#################################

ifeq ($(FACEDETECT_LIB_TYPE),shared)
	LOCAL_SHARED_LIBRARIES +=    facedetect_prebuilt 
endif
ifeq ($(FFMPEG_LIB_TYPE),shared)
	LOCAL_SHARED_LIBRARIES +=    ffmpeg_prebuilt
endif

LOCAL_STATIC_LIBRARIES := fastcv_prebuilt

LOCAL_ARM_NEON := true
LOCAL_CFLAGS += -mfloat-abi=softfp -mfpu=neon

## now build our applications shared (dynamic) library
include $(BUILD_SHARED_LIBRARY)

