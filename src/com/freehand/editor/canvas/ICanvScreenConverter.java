package com.freehand.editor.canvas;

import android.graphics.Rect;
import android.graphics.RectF;

public interface ICanvScreenConverter {
	public float canvToScreenDist(final float canvasDist);
	public float screenToCanvDist(final float screenDist);
	public Rect canvRectToScreenRect (RectF canvRect);
}