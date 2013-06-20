package com.freehand.note_editor;

import com.freehand.ink.Point;

import android.graphics.Canvas;
import android.graphics.RectF;

public interface ICanvasEventListener {
	public void startPointerEvent();
	public boolean continuePointerEvent(Point p, long time, float pressure);
	public void canclePointerEvent();
	public void finishPointerEvent();
	
	public void startPinchEvent();
	public boolean continuePinchEvent(Point mid, Point dMid, float dZoom, float dist, RectF startBoundingRect);
	public void canclePinchEvent();
	public void finishPinchEvent();
	
	public void startHoverEvent();
	public boolean continueHoverEvent(Point p, long time);
	public void cancleHoverEvent();
	public void finishHoverEvent();
	
	public void undoCalled();
	public void redoCalled();
	
	public void drawNote(Canvas c);
}