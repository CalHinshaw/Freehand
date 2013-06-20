package com.freehand.note_editor.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.freehand.ink.MiscGeom;
import com.freehand.ink.MiscPolyGeom;
import com.freehand.ink.Point;
import com.freehand.ink.Stroke;
import com.freehand.note_editor.ICanvasEventListener;
import com.freehand.note_editor.Note;
import com.freehand.note_editor.Note.Action;

public class StrokeEraser implements ICanvasEventListener {
	
	private final Note mNote;
	private final DistConverter mConverter;
	
	private List<Stroke> currentStrokes;
	private TreeSet<Integer> deletedStrokes = new TreeSet<Integer>();
	
	// This is the size of the eraser given in SCREEN PIXELS - it needs to be scaled
	// to canvas pixels to be used.
	private final float eraserSize;
	
	private Paint circlePaint;
	private Point circlePoint;
	
	// Mid-term state fields
	private Point prevPoint = null;
	private long prevTime = -1;
	
	public StrokeEraser (Note newNote, DistConverter newConverter, float newEraserSize) {
		mNote = newNote;
		mConverter = newConverter;
		
		eraserSize = newEraserSize;
		
		circlePaint = new Paint(Color.BLACK);
		circlePaint.setStyle(Paint.Style.STROKE);
		circlePaint.setAntiAlias(true);
		
		currentStrokes = mNote.getInkLayer();
	}
	
	
	public void startPointerEvent() {
		currentStrokes = mNote.getInkLayer();
		deletedStrokes.clear();
		
		prevPoint = null;
		prevTime = -1;
	}

	public boolean continuePointerEvent(Point p, long time, float pressure) {
		if (prevPoint == null) {
			deleteCapsule(p, p, mConverter.screenToCanvasDist(eraserSize));
			prevPoint = p;
			prevTime = time;
		} else if (MiscGeom.distance(prevPoint, p) > mConverter.screenToCanvasDist(10.0f) || time-prevTime >= 100) {
			deleteCapsule(prevPoint, p, mConverter.screenToCanvasDist(eraserSize));
			prevPoint = p;
			prevTime = time;
		}
		
		circlePoint = p;
		
		return true;
	}


	
	public void canclePointerEvent() {
		currentStrokes = mNote.getInkLayer();
		deletedStrokes.clear();
		
		circlePoint = null;
	}

	public void finishPointerEvent() {
		if (deletedStrokes.isEmpty() == false) {
			ArrayList<Action> action = new ArrayList<Action>(deletedStrokes.size());
			
			for (Integer i : deletedStrokes.descendingSet()) {
				action.add(new Action(currentStrokes.get(i.intValue()), i, false));
			}
			
			mNote.performActions(action);
		}
		
		currentStrokes = mNote.getInkLayer();
		deletedStrokes.clear();
		
		circlePoint = null;
	}

	public void startPinchEvent() {
		circlePoint = null;
	}

	public boolean continuePinchEvent(Point mid, Point dMid, float dZoom, float dist, RectF startBoundingRect) {
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
		for (int i = 0; i < currentStrokes.size(); i++) {
			if (deletedStrokes.contains(i) == false) {
				currentStrokes.get(i).draw(c);
			}
		}
		
		if (circlePoint != null) {
			float scaledWidth = mConverter.screenToCanvasDist(2.0f);
			circlePaint.setStrokeWidth(scaledWidth);
			c.drawCircle(circlePoint.x, circlePoint.y, mConverter.screenToCanvasDist(eraserSize) - scaledWidth, circlePaint);
		}
	}
	
	//************************************** Utility Methods **********************************************************
	
	/**
	 * Deletes the strokes within rad units of the line segment defined by p1 and p2. The ordering of p1 and p2
	 * doesn't matter.
	 */
	private void deleteCapsule (Point p1, Point p2, float rad) {
		RectF eraseBox = MiscGeom.calcCapsuleAABB(p1, p2, rad);
		
		for (int i = 0; i < currentStrokes.size(); i++) {
			Stroke s = currentStrokes.get(i);
			if (RectF.intersects(eraseBox, s.getAABoundingBox())) {
				if (MiscPolyGeom.checkCapsulePolyIntersection(s.getPoly(), p1, p2, rad) == true) {
					deletedStrokes.add(i);
				}
			}
		}
	}
}