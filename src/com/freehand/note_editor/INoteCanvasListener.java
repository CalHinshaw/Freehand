package com.freehand.note_editor;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;

interface INoteCanvasListener {
	public void stylusAction(List<Long> times, List<Float> xs, List<Float> ys, List<Float> pressures, boolean stylusUp);
	public void fingerAction(List<Long> times, List<Float> xs, List<Float> ys, List<Float> pressures, boolean fingerUp);
	public void hoverAction(ArrayList<Long> times, ArrayList<Float> xs, ArrayList<Float> ys, boolean hoverEnded);
	
	public void panZoomAction(float midpointX, float midpointY, float screenDx, float screenDy, float dZoom);
	
	public void drawNote(Canvas c);
}