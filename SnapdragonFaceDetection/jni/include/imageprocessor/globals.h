#define ARGB_8888  1
#define ABGR_8888  2
#define GREYSCALE_AGGG_8888  3
#define BGRA_8888  4
#define RGBA_8888  5
#define	RED_CHANNEL			6
#define	GREEN_CHANNEL		 7
#define	BLUE_CHANNEL		 8

extern jint frame_size;
extern jint frame_width_sample_blocks;
extern jint frame_sample_blocks;
extern jint frame_width;
extern jint frame_height;
extern jint min_range_value;
extern jint max_range_value;
extern jint min_num_contours;
extern jint max_num_contours;
extern int neon_supported;
extern int arm_supported;
extern uint8_t* h;
extern uint8_t* s;
extern uint8_t* v;
extern uint8_t* hsv;


extern void int_to_binary(int, char *);
