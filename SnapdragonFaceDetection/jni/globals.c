#include <jni.h>
#include <stdint.h>

/*
 * Dimensions of a preview frame passed as a 1-D array of pixel values to the native methods.
 *
 * size = width * height (number of pixels)
 * width_sample_blocks = width/2 : each UV value pair in the preview YUV data is for a 2x2 pixel block of the image giving width/2 blocks
 * frame_sample_blocks = size/4 : given the above there are size/4 sampled blocks in the image
 *
 * These are initialised in CameraPreviewProcessor_init
 */

jint frame_width;
jint frame_height;
jint frame_size;
jint frame_width_sample_blocks;
jint frame_sample_blocks;
jint min_range_value;
jint max_range_value;
jint min_num_contours;
jint max_num_contours;
int neon_supported;
int arm_supported;
uint8_t* h = NULL;
uint8_t* s = NULL;
uint8_t* v = NULL;
uint8_t* hsv = NULL;

void int_to_binary(int x, char * str) {
	int cnt, ch_cnt = 0, mask = 1 << 31;

	for (cnt = 1; cnt <= 32; ++cnt) {
		str[ch_cnt++] = ((x & mask) == 0) ? '0' : '1';
		x <<= 1;

		if (cnt % 8 == 0 && cnt != 32) {
			str[ch_cnt++] = ' ';
		}
	}
	str[35] = '\0';
}

