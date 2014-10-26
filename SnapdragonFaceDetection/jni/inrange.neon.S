
    .text
    .align 2
    .global in_range_neon
    .type   in_range_neon, %function


#define out_ptr			r0
#define HSV_in_ptr		r1
#define len				r2
#define min_H 			r3
#define max_H 			r4
#define min_S 			r5
#define max_S 			r6
#define min_V 			r7
#define max_V 			r8

#define H      			d0
#define S      			d1
#define V      			d2
#define min_H_vec		d3
#define max_H_vec 		d4
#define min_S_vec 		d5
#define max_S_vec 		d6
#define min_V_vec 		d7
#define max_V_vec 		d8
#define tmp_mask_1		d9
#define tmp_mask_2		d10
#define out_mask		d11

in_range_neon:
	.fnstart
		PUSH 	{r4-r12,lr}

		LDR		max_H, 			[sp, #10*4]
		LDR		min_S, 			[sp, #11*4]
		LDR		max_S, 			[sp, #12*4]
		LDR		min_V, 			[sp, #13*4]
		LDR		max_V, 			[sp, #14*4]

		VDUP.8	min_H_vec, 		min_H
		VDUP.8	max_H_vec, 		max_H
		VDUP.8	min_S_vec, 		min_S
		VDUP.8	max_S_vec, 		max_S
		VDUP.8	min_V_vec, 		min_V
		VDUP.8	max_V_vec, 		max_V


    loop1:
    	VLD3.8 		{H, S, V}, 		[HSV_in_ptr]!

    	VCGE.U8		out_mask,	H,	min_H_vec
    	VCGT.U8		tmp_mask_1,	H,	max_H_vec
    	VQSUB.U8	out_mask,	out_mask,		tmp_mask_1

    	VCGE.U8		tmp_mask_1,	S,				min_S_vec
    	VCGT.U8		tmp_mask_2,	S,				max_S_vec
    	VQSUB.U8	tmp_mask_1,	tmp_mask_1,		tmp_mask_2
    	VAND		out_mask,	out_mask,		tmp_mask_1

    	VCGE.U8		tmp_mask_1,	V,				min_V_vec
    	VCGT.U8		tmp_mask_2,	V,				max_V_vec
    	VQSUB.U8	tmp_mask_1,	tmp_mask_1,		tmp_mask_2
    	VAND		out_mask,	out_mask,		tmp_mask_1


		VST1.8 		out_mask,	[out_ptr]!

    	SUBS    len, len, #1
    	BNE 	loop1

		POP 	{r4-r12,pc}
	.fnend



