package org.stevej.android.facedetection.ui;

import org.stevej.android.facedetection.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;

public class VideoPreview extends DisplaySurface implements Callback {
	private static final String	TAG			= "VideoPreview";
	private Bitmap				bitmap		= null;
	private Boolean				mirroring	= false;

	@Override
	public boolean handleMessage(Message message) {
//		 Log.d(TAG,"S handleMessage()");
		bitmap = (Bitmap) message.obj;
		canvas = surface_holder.lockCanvas();
		if (bitmap == null || canvas == null) {
			return true;
		}

		synchronized (mirroring) {
			if (mirroring) {
				canvas.scale(-1f, 1f, canvas.getWidth() / 2f, canvas.getHeight() / 2f);
			}

			canvas.drawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), new Rect(0, 0, getWidth(), getHeight()), null);

			if (mirroring) {
				canvas.scale(-1f, 1f, canvas.getWidth() / 2f, canvas.getHeight() / 2f);
			}
		}
		surface_holder.unlockCanvasAndPost(canvas);

//		 Log.d(TAG,"F handleMessage()");
		return true;
	}

	public void setMirroring(boolean mirror) {
		synchronized (mirroring) {
			mirroring = mirror;
		}
	}

	public VideoPreview(Context context, AttributeSet attributes) {
		super(context, attributes, R.id.video_preview);

		message_handler = new Handler(looper, this);

	}
}
