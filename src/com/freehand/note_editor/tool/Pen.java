package com.freehand.note_editor.tool;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;

import com.freehand.ink.MiscGeom;
import com.freehand.ink.Point;
import com.freehand.ink.Stroke;
import com.freehand.misc.WrapList;
import com.freehand.note_editor.ICanvasEventListener;
import com.freehand.note_editor.Note;
import com.freehand.note_editor.Note.Action;

public class Pen implements ICanvasEventListener {
	private static final int ARC_RES = 20;
	
	private final Note mNote;
	private final DistConverter mConverter;
	
	private final float pressureSensitivity;
	
	private final float baseSize;
	private final int color;
	
	private ArrayList<Point> points = new ArrayList<Point>(1000);
	private ArrayList<Float> sizes = new ArrayList<Float>(1000);
	
	private Path path = new Path();
	private Paint paint = new Paint();
	
	private LinkedList<Point> poly = new LinkedList<Point>();
	private List<Point> cap = new ArrayList<Point>();
	
	private int containingIndex = -1;
	
	private RectF dirtyRect = null;
	
	public Pen (Note newNote, DistConverter newConverter, float pressureSensitivity, int penColor, float penSize) {
		mNote = newNote;
		mConverter = newConverter;
		this.pressureSensitivity = pressureSensitivity;
		color = penColor;
		baseSize = penSize;
		
		path.setFillType(Path.FillType.WINDING);
		
		paint.setColor(color);
		paint.setStyle(Paint.Style.STROKE);
		paint.setAntiAlias(true);
	}
	
	//********************************************* ICanvasEventListener Methods *****************************************

	public void startPointerEvent() {
		points.clear();
		sizes.clear();
		poly.clear();
		cap.clear();
		containingIndex = -1;
	}

	public boolean continuePointerEvent(Point p, long time, float pressure) {
		float newSize = baseSize * this.scalePressure(pressure);
		
		if (points.size() > 0 && newSize == sizes.get(sizes.size()-1) &&
			MiscGeom.distance(p, points.get(points.size()-1)) <= mConverter.screenToCanvasDist(2.0f)) {
			return true;
		}
		
		points.add(p);
		sizes.add(newSize);
		
		if (points.size() == 2) {
			startPoly();
		} else if (points.size() > 2) {
			addToPoly();
		}
		
		return true;
	}

	public void canclePointerEvent() {
		points.clear();
		sizes.clear();
		poly.clear();
		cap.clear();
		containingIndex = -1;
	}

	public void finishPointerEvent() {
		WrapList<Point> finalPoly = getFinalPoly();
		if (finalPoly.size() >= 3) {
			
			ArrayList<Action> action = new ArrayList<Action>(1);
			
			action.add(new Action(new Stroke(color, finalPoly), mNote.getInkLayer().size(), true));
			mNote.performActions(action);
		}
		
		points.clear();
		sizes.clear();
		poly.clear();
		cap.clear();
		containingIndex = -1;
	}

	public void startPinchEvent() { canclePointerEvent(); }
	public boolean continuePinchEvent(Point mid, Point dMid, float dZoom, float dist, RectF startBoundingRect) { return false; }
	public void canclePinchEvent() { /* blank */ }
	public void finishPinchEvent() { /* blank */ }
	public void startHoverEvent() { /* blank */ }
	public boolean continueHoverEvent(Point p, long time) { return false; }
	public void cancleHoverEvent() { /* blank */ }
	public void finishHoverEvent() { /* blank */ }

	public RectF getDirtyRect() {
		updateCap();
		return dirtyRect;
	}
	
	public void drawNote(Canvas c) {
		resetDirtyRect();
		
		for (Stroke s : mNote.getInkLayer()) {
			s.draw(c);
		}
		
		if (poly != null && poly.size() >= 3) {
			updateCap();
			path.reset();
			path.moveTo(poly.get(0).x, poly.get(0).y);
			for (Point p : poly) {
				path.lineTo(p.x, p.y);
			}
			for (Point p : cap) {
				path.lineTo(p.x, p.y);
			}
			c.drawPath(path, paint);
		}
		
//		// test code
//		Paint red = new Paint();
//		red.setColor(0xA0FF0000);
//		red.setStyle(Paint.Style.STROKE);
//		red.setStrokeWidth(0);
//		
//		Paint black = new Paint();
//		black.setColor(0xA0000000);
//		black.setStyle(Paint.Style.STROKE);
//		black.setStrokeWidth(0);
//		
//		boolean isRed = true;
//		
//		for (int i = 0; i < points.size(); i++) {
//			c.drawCircle(points.get(i).x, points.get(i).y, sizes.get(i), isRed ? red : black);
//			isRed = !isRed;
//		}
	}
	
	public void undoCalled() { /* blank */ }
	public void redoCalled() { /* blank */ }
	
	//**************************************** Utility Methods ************************************************
	
	private void resetDirtyRect () {
		dirtyRect = null;
		
		if (poly.size() >= 1) {
			addPointToDirtyRect(poly.getFirst());
			addPointToDirtyRect(poly.getLast());
		}
	}
	
	
	
	private WrapList<Point> getFinalPoly () {
		updateCap();
		WrapList<Point> finalPoly = new WrapList<Point>(poly.size()+cap.size());
		
		for (Point p : poly) {
			finalPoly.add(p);
		}
		
		for (Point p : cap) {
			finalPoly.add(p);
		}
		
		return finalPoly;
	}
	
	private void startPoly () {
		Point[] tangentPoints = MiscGeom.calcExternalBitangentPoints(points.get(0), sizes.get(0), points.get(1), sizes.get(1));
		if (tangentPoints != null) {
			startNewStroke(tangentPoints, points.get(0), sizes.get(0));
		} else {
			if (sizes.get(0) >= sizes.get(1)) {		// first contains second
				containingIndex = 0;
				LinkedList<Point> start = MiscGeom.getLinkedCircularPoly(points.get(0), sizes.get(0));
				for (int i = 0; i < start.size()/2; i++) {
					addLast(start.get(i));
				}
			} else {								// second contains first. THIS IS THE PROBLEM ONE!!!!
				poly.clear();
				points.remove(0);
				sizes.remove(0);
			}
		}
	}
	
	private void addToPoly () {
		if (containingIndex >= 0) {
			breakContainment();
			return;
		}
		
		Point[] tangentPoints = MiscGeom.calcExternalBitangentPoints(points.get(points.size()-2),
			sizes.get(sizes.size()-2), points.get(points.size()-1), sizes.get(sizes.size()-1));
		
		if (tangentPoints != null) {
			addToLhs(tangentPoints[0], tangentPoints[1], points.get(points.size()-2), sizes.get(sizes.size()-2), points.get(points.size()-1), sizes.get(sizes.size()-1));
			addToRhs(tangentPoints[2], tangentPoints[3], points.get(points.size()-2), sizes.get(sizes.size()-2), points.get(points.size()-1), sizes.get(sizes.size()-1));
		} else {
			if (sizes.get(sizes.size()-1) <= sizes.get(sizes.size()-2)) {	// new contained by old -> Start containment
				containingIndex = sizes.size()-2;
			} else {														// old contained by new -> backtrack
				backtrack();
			}
		}
	}
	
	private void backtrack () {
		// calculate the points to trace the circle to
		Point[] offsets = MiscGeom.calcPerpOffset(points.get(points.size()-1), points.get(points.size()-2), sizes.get(sizes.size()-1));
		
		while (poly.size() >= 2) {
			Point[] pts = MiscGeom.circleSegmentIntersection(points.get(points.size()-1), sizes.get(sizes.size()-1), poly.get(poly.size()-1), poly.get(poly.size()-2));
			poly.removeLast();
			if (pts[0] != null) {
				addLast(pts[0]);

				List<Point> right = MiscGeom.approximateCircularArc(points.get(points.size()-1), sizes.get(sizes.size()-1), false, poly.getLast(), offsets[1], ARC_RES);
				for (Point p : right) {
					addLast(p);
				}
				
				break;
			}
		}
		
		while (poly.size() >= 2) {
			Point[] pts = MiscGeom.circleSegmentIntersection(points.get(points.size()-1), sizes.get(sizes.size()-1), poly.get(0), poly.get(1));
			poly.removeFirst();
			if (pts[0] != null) {
				addFirst(pts[0]);
				
				List<Point> left = MiscGeom.approximateCircularArc(points.get(points.size()-1), sizes.get(sizes.size()-1), true, poly.getFirst(), offsets[0], ARC_RES);
				for (Point p : left) {
					addFirst(p);
				}
				
				break;
			}
		}
		
		if (poly.size() < 2) {
			Point tempPoint = points.get(points.size()-1);
			float tempSize = sizes.get(sizes.size()-1);
			poly.clear();
			points.clear();
			sizes.clear();
			points.add(tempPoint);
			sizes.add(tempSize);
		}
	}
	
	private void breakContainment () {
		if (MiscGeom.checkCircleContainment(points.get(containingIndex), sizes.get(containingIndex), points.get(points.size()-1), sizes.get(sizes.size()-1)) == true) {
			return;
		}
		
		Point[] tanPts = MiscGeom.calcExternalBitangentPoints(points.get(points.size()-2), sizes.get(sizes.size()-2), points.get(points.size()-1), sizes.get(sizes.size()-1));
		Point[] tanIntPts = {null, null};
		if (tanPts != null) {
			tanIntPts[0] = MiscGeom.circleSegmentIntersection(points.get(containingIndex), sizes.get(containingIndex), tanPts[0], tanPts[1])[0];
			tanIntPts[1] = MiscGeom.circleSegmentIntersection(points.get(containingIndex), sizes.get(containingIndex), tanPts[2], tanPts[3])[0];
		}
		Point[] circIntPts = MiscGeom.circleCircleIntersection(points.get(containingIndex), sizes.get(containingIndex), points.get(points.size()-1), sizes.get(sizes.size()-1));
		
		// Left hand side
		if (tanIntPts[0] != null) {
			List<Point> left = MiscGeom.approximateCircularArc(points.get(containingIndex), sizes.get(containingIndex), true, poly.getFirst(), tanIntPts[0], ARC_RES);
			for (Point p : left) {
				addFirst(p);
			}
			addFirst(tanIntPts[0]);
			addFirst(tanPts[1]);
		} else if (circIntPts != null) {
			List<Point> left = MiscGeom.approximateCircularArc(points.get(containingIndex), sizes.get(containingIndex), true, poly.getFirst(), circIntPts[0], ARC_RES);
			for (Point p : left) {
				addFirst(p);
			}
			addFirst(circIntPts[0]);
		} else {
			Log.d("PEN", "containment broken but no intersections");
			return;
		}
		
		// Right hand side
		if (tanIntPts[1] != null) {
			List<Point> right = MiscGeom.approximateCircularArc(points.get(containingIndex), sizes.get(containingIndex), false, poly.getLast(), tanIntPts[1], ARC_RES);
			for (Point p : right) {
				addLast(p);
			}
			addLast(tanIntPts[1]);
			addLast(tanPts[3]);
		} else if (circIntPts != null) {
			List<Point> right = MiscGeom.approximateCircularArc(points.get(containingIndex), sizes.get(containingIndex), false, poly.getLast(), circIntPts[1], ARC_RES);
			for (Point p : right) {
				addLast(p);
			}
			addLast(circIntPts[1]);
		} else {
			Log.d("PEN", "containment broken but no intersections");
			return;
		}
		
		containingIndex = -1;
	}
	
	private void updateCap () {
		
		Log.d("PEN", Integer.toString(points.size()) + "     " + Integer.toString(containingIndex));
		
		if (points.size() == 1) {
			cap = MiscGeom.getLinkedCircularPoly(points.get(0), sizes.get(0));
		} else if (containingIndex >= 0) {
			cap = MiscGeom.approximateCircularArc(points.get(containingIndex), sizes.get(containingIndex), false, poly.getLast(), poly.getFirst(), ARC_RES);
		} else if (points.size() > 1) {
			cap = MiscGeom.approximateCircularArc(points.get(points.size()-1), sizes.get(sizes.size()-1), false, poly.getLast(), poly.getFirst(), ARC_RES);
		}
		
		for (Point p : cap) {
			addPointToDirtyRect(p);
		}
	}
	
	private void startNewStroke (Point[] tangentPoints, Point prevPoint, float prevSize) {
		List<Point> frontCap = MiscGeom.approximateCircularArc(prevPoint, prevSize, false, tangentPoints[0], tangentPoints[2], ARC_RES);
		for (Point p : frontCap) {
			addLast(p);
		}
		
		addFirst(tangentPoints[0]);
		addFirst(tangentPoints[1]);
		addLast(tangentPoints[2]);
		addLast(tangentPoints[3]);
	}
	
	private void addToLhs (Point tail, Point head, Point joinCenter, float joinRad, Point newCenter, float newRad) {
		float handedness = MiscGeom.cross(poly.get(0), poly.get(1), head, poly.get(1));
		if (handedness <= 0) {
			List<Point> join = MiscGeom.approximateCircularArc(joinCenter, joinRad, true, poly.getFirst(), tail, ARC_RES);
			for (Point p : join) {
				addFirst(p);
			}
			addFirst(tail);
			addFirst(head);
		} else {
			Point intersection = MiscGeom.calcIntersection(poly.get(0), poly.get(1), head, tail);
			if (intersection != null) {
				poly.removeFirst();
				addFirst(intersection);
				addFirst(head);
			} else {
				addFirst(tail);
				addFirst(head);
			}
		}
	}
	
	private void addToRhs (Point tail, Point head, Point joinCenter, float joinRad, Point newCenter, float newRad) {
		float handedness = MiscGeom.cross(poly.get(poly.size()-1), poly.get(poly.size()-2), head, poly.get(poly.size()-2));
		if (handedness >= 0) {
			List<Point> join = MiscGeom.approximateCircularArc(joinCenter, joinRad, false, poly.getLast(), tail, ARC_RES);
			for (Point p : join) {
				addLast(p);
			}
			addLast(tail);
			addLast(head);
		} else {
			Point intersection = MiscGeom.calcIntersection(poly.get(poly.size()-1), poly.get(poly.size()-2), head, tail);
			
			if (intersection != null) {
				poly.removeLast();
				addLast(intersection);
				addLast(head);
			} else {
				addLast(tail);
				addLast(head);
			}
		}
	}
	
	private float scalePressure (float pressure) {
		return 1.0f - pressureSensitivity + pressure*pressureSensitivity;
	}
	
	private void addLast (Point p) {
		addPointToDirtyRect(p);
		poly.addLast(p);
	}
	
	private void addFirst (Point p) {
		addPointToDirtyRect(p);
		poly.addFirst(p);
	}
	
	private void addPointToDirtyRect (Point p) {
		if (dirtyRect != null) {
			dirtyRect.union(p.x, p.y);
		} else {
			dirtyRect = new RectF(p.x, p.y, p.x+0.001f, p.y+0.001f);
		}
	}
}