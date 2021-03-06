package com.freehand.editor.canvas;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.freehand.editor.canvas.Note.Action;
import com.freehand.ink.MiscGeom;
import com.freehand.ink.MiscPolyGeom;
import com.freehand.ink.Point;
import com.freehand.ink.Stroke;

public class StrokeEraser implements ITool {
	
	private final Note mNote;
	private final ICanvScreenConverter mConverter;
	
	private final boolean capDrawing;
	
	private List<Stroke> currentStrokes;
	private TreeSet<Integer> deletedStrokes = new TreeSet<Integer>();
	
	// This is the size of the eraser given in SCREEN PIXELS - it needs to be scaled
	// to canvas pixels to be used.
	private final float screenEraserSize;
	
	private Paint circlePaint;
	private Point circlePoint;
	
	// Mid-term state fields
	private Point prevPoint = null;
	private long prevTime = -1;
	
	private RectF dirtyRect = new RectF();
	
	public StrokeEraser (Note newNote, ICanvScreenConverter newConverter, float newEraserSize, boolean capDrawing) {
		mNote = newNote;
		mConverter = newConverter;
		
		this.capDrawing = capDrawing;
		
		screenEraserSize = newEraserSize;
		
		circlePaint = new Paint(Color.BLACK);
		circlePaint.setStyle(Paint.Style.STROKE);
		circlePaint.setAntiAlias(true);
		
		currentStrokes = mNote.getInkLayer();
	}
	
	private boolean ignoringCurrentMe = false;
	
	public boolean onMotionEvent(MotionEvent e) {
		
		if (e.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER && capDrawing == false) {
			return false;
		}
		
		if (e.getAction() == MotionEvent.ACTION_UP) {
			final Point p = new Point(e.getX(), e.getY());
			processTouchPoint(p, e.getPressure(), e.getEventTime());
			addEraseToNote();
			reset();
		}
		
		if (ignoringCurrentMe) return false;
		if (e.getPointerCount() > 1) {
			reset();
			ignoringCurrentMe = true;
			return false;
		}
		
		if (e.getAction() == MotionEvent.ACTION_DOWN) {
			reset();
		} else if (e.getAction() == MotionEvent.ACTION_MOVE) {
			for (int i = 0; i < e.getHistorySize(); i++) {
				final Point p = new Point(e.getHistoricalX(i), e.getHistoricalY(i));
				processTouchPoint(p, e.getHistoricalPressure(i), e.getHistoricalEventTime(i));
			}
			final Point p = new Point(e.getX(), e.getY());
			processTouchPoint(p, e.getPressure(), e.getEventTime());
		} else if (e.getActionMasked() == MotionEvent.ACTION_HOVER_ENTER || e.getActionMasked() == MotionEvent.ACTION_HOVER_MOVE) {
			if (circlePoint != null) {
				dirtyRect.union(getCircleAABB(circlePoint, mConverter.screenToCanvDist(screenEraserSize)));
			}
			circlePoint = new Point(e.getX(), e.getY());
			dirtyRect.union(getCircleAABB(circlePoint, mConverter.screenToCanvDist(screenEraserSize)));
		} else if (e.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
			if (circlePoint != null) {
				dirtyRect.union(getCircleAABB(circlePoint, mConverter.screenToCanvDist(screenEraserSize)));
			}
			reset();
		}
		
		return true;
	}
	
	public void reset () {
		currentStrokes = mNote.getInkLayer();
		deletedStrokes.clear();
		prevPoint = null;
		prevTime = -1;
		circlePoint = null;
		ignoringCurrentMe = false;
	}

	public boolean processTouchPoint(final Point p, final float pressure, final long t) {
		if (prevPoint == null) {
			deleteCapsule(p, p, mConverter.screenToCanvDist(screenEraserSize));
			prevPoint = p;
			prevTime = t;
		} else if (MiscGeom.distance(prevPoint, p) > mConverter.screenToCanvDist(10.0f) || t-prevTime >= 100) {
			deleteCapsule(prevPoint, p, mConverter.screenToCanvDist(screenEraserSize));
			prevPoint = p;
			prevTime = t;
		}
		
		if (circlePoint != null) {
			dirtyRect.union(getCircleAABB(circlePoint, mConverter.screenToCanvDist(screenEraserSize)));
		}
		circlePoint = p;
		dirtyRect.union(getCircleAABB(circlePoint, mConverter.screenToCanvDist(screenEraserSize)));
		
		return true;
	}

	private void addEraseToNote() {
		if (deletedStrokes.isEmpty() == false) {
			ArrayList<Action> action = new ArrayList<Action>(deletedStrokes.size());
			
			for (Integer i : deletedStrokes.descendingSet()) {
				action.add(new Action(currentStrokes.get(i.intValue()), i, false));
			}
			
			mNote.performActions(action);
		}
		
		reset();
	}
	
	public RectF getDirtyRect() {
		RectF toReturn = dirtyRect;
		dirtyRect = new RectF();
		return toReturn;
	}
	
	public void draw (Canvas c) {
		for (int i = 0; i < currentStrokes.size(); i++) {
			if (deletedStrokes.contains(i) == false) {
				currentStrokes.get(i).draw(c);
			}
		}
		
		if (circlePoint != null) {
			float scaledWidth = mConverter.screenToCanvDist(2.0f);
			circlePaint.setStrokeWidth(scaledWidth);
			
			float size = mConverter.screenToCanvDist(screenEraserSize) - scaledWidth;
			if (size < 1) {
				size = 1;
			}
			c.drawCircle(circlePoint.x, circlePoint.y, size, circlePaint);
		}
	}
	
	public void undo () { /* blank */ }
	public void redo () { /* blank */ }
	
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
					dirtyRect.union(currentStrokes.get(i).getAABoundingBox());
				}
			}
		}
	}
	
	private static RectF getCircleAABB (final Point c, final float r) {
		final RectF aabb = new RectF();
		aabb.left = c.x-r;
		aabb.right = c.x+r;
		aabb.top = c.y-r;
		aabb.bottom = c.y+r;
		return aabb;
	}
}