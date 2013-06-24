package com.freehand.note_editor;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

interface IScreenEventListener {
	
	public void startPointerEvent();
	public void continuePointerEvent(long time, float x, float y, float pressure);
	public void canclePointerEvent();
	public void finishPointerEvent();
	
	public void startPinchEvent();
	public void continuePinchEvent(float midpointX, float midpointY, float screenDx, float screenDy, float dZoom, float dist, RectF startBoundingRect);
	public void canclePinchEvent();
	public void finishPinchEvent();
	
	public void startHoverEvent();
	public void continueHoverEvent(long time, float x, float y);
	public void cancleHoverEvent();
	public void finishHoverEvent();
	
	public Rect getDirtyRect();
	public void drawNote(Canvas c);
}