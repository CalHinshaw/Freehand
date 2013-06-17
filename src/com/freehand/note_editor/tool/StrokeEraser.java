package com.freehand.note_editor.tool;

import java.util.Iterator;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.freehand.ink.MiscGeom;
import com.freehand.ink.MiscPolyGeom;
import com.freehand.ink.Point;
import com.freehand.ink.Stroke;
import com.freehand.note_editor.ICanvasEventListener;

public class StrokeEraser implements ICanvasEventListener {
	
	private final List<Stroke> subjectNote;
	private final DistConverter mConverter;
	
	// This is the size of the eraser given in SCREEN PIXELS - it needs to be scaled
	// to canvas pixels to be used.
	private final float eraserSize;
	
	private Paint circlePaint;
	private Point circlePoint;
	
	// Mid-term state fields
	private Point prevPoint = null;
	private long prevTime = -1;
	
	public StrokeEraser (List<Stroke> newNote, DistConverter newConverter, float newEraserSize) {
		subjectNote = newNote;
		mConverter = newConverter;
		
		eraserSize = newEraserSize;
		
		circlePaint = new Paint(Color.BLACK);
		circlePaint.setStyle(Paint.Style.STROKE);
		circlePaint.setAntiAlias(true);
	}

	
	
	
	public void startPointerEvent() {
		prevPoint = null;
		prevTime = -1;
	}

	public boolean continuePointerEvent(Point p, long time, float pressure) {
		if (prevPoint == null) {
			deleteCapsule(subjectNote, p, p, mConverter.screenToCanvasDist(eraserSize));
			prevPoint = p;
			prevTime = time;
		} else if (MiscGeom.distance(prevPoint, p) > mConverter.screenToCanvasDist(10.0f) || time-prevTime >= 100) {
			deleteCapsule(subjectNote, prevPoint, p, mConverter.screenToCanvasDist(eraserSize));
			prevPoint = p;
			prevTime = time;
		}
		
		circlePoint = p;
		
		return true;
	}


	
	public void canclePointerEvent() {
		circlePoint = null;
	}

	public void finishPointerEvent() {
		circlePoint = null;
	}

	public void startPinchEvent() {
		circlePoint = null;
	}

	public boolean continuePinchEvent(Point mid, Point dMid, float dZoom, RectF startBoundingRect) {
		return false;
	}
	
	public void canclePinchEvent() { /* blank */ }
	public void finishPinchEvent() { /* blank */ }
	public void startHoverEvent() { /* blank */ }

	public boolean continueHoverEvent(Point p, long time) {
		circlePoint = p;
		return true;
	}

	public void cancleHoverEvent() {
		circlePoint = null;
	}

	public void finishHoverEvent() {
		circlePoint = null;
	}

	public void drawNote(Canvas c) {
		for (Stroke s : subjectNote) {
			s.draw(c);
		}
		
		if (circlePoint != null) {
			float scaledWidth = mConverter.screenToCanvasDist(2.0f);
			circlePaint.setStrokeWidth(scaledWidth);
			c.drawCircle(circlePoint.x, circlePoint.y, mConverter.screenToCanvasDist(eraserSize) - scaledWidth, circlePaint);
		}
	}
	
	
	
	
	
	
	
	
	
	/**
	 * Deletes the strokes within rad units of the line segment defined by p1 and p2. The ordering of p1 and p2
	 * doesn't matter.
	 */
	private static void deleteCapsule (List<Stroke> strokes, Point p1, Point p2, float rad) {
		RectF eraseBox = MiscGeom.calcCapsuleAABB(p1, p2, rad);
		Iterator<Stroke> iter = strokes.iterator();
		while (iter.hasNext()) {
			Stroke s = iter.next();
			if (RectF.intersects(eraseBox, s.getAABoundingBox())) {
				if (MiscPolyGeom.checkCapsulePolyIntersection(s.getPoly(), p1, p2, rad) == true) {
					iter.remove();
				}
			}
		}
	}
}