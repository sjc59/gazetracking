
    .text
    .align 2
    .global extract_hsv_neon
    .type   extract_hsv_neon, %function


#define in_ptr			r0
#define c1_out_ptr		r1
#define c2_out_ptr		r2
#define c3_out_ptr		r3
#define len 			r4

#define c1      d0
#define c2      d1
#define c3      d2


extract_hsv_neon:
	.fnstart
		PUSH 	{r4-r12,lr}
		LDR		len, [sp, #10*4]

    loop1:
    	VLD3.8 {c1, c2, c3}, [in_ptr]!

    	VST1.8 {c1 }, [c1_out_ptr]!
    	VST1.8 {c2 }, [c2_out_ptr]!
    	VST1.8 {c3 }, [c3_out_ptr]!

    	SUBS    len, len, #1
    	BNE 	loop1

		POP 	{r4-r12,pc}
	.fnend
