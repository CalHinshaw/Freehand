package com.freehand.note_editor;

import android.graphics.Canvas;
import android.graphics.RectF;

interface INoteCanvasListener {
	public void stylusAction(long time, float x, float y, float pressure, boolean stylusUp);
	public void fingerAction(long time, float x, float y, float pressure, boolean fingerUp);
	public void hoverAction(long time, float x, float y, boolean hoverUp);
	
	public void panZoomAction(float midpointX, float midpointY, float screenDx, float screenDy, float dZoom, RectF boundingRect);
	
	public void drawNote(Canvas c);
}