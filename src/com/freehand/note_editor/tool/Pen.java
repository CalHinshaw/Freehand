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
	
	private final Note mNote;
	private final DistConverter mConverter;
	
	private final float baseSize;
	private final int color;
	
	private ArrayList<Point> points = new ArrayList<Point>(1000);
	private ArrayList<Float> sizes = new ArrayList<Float>(1000);
	
	private Path path = new Path();
	private Paint paint = new Paint();
	
	private LinkedList<Point> poly = new LinkedList<Point>();
	private LinkedList<Point> cap = new LinkedList<Point>();
	
	private int containingIndex = -1;
	
	public Pen (Note newNote, DistConverter newConverter, int penColor, float penSize) {
		mNote = newNote;
		mConverter = newConverter;
		color = penColor;
		baseSize = penSize;
		
		path.setFillType(Path.FillType.WINDING);
		
		paint.setColor(color);
		paint.setStyle(Paint.Style.FILL);
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
			MiscGeom.distance(p, points.get(points.size()-1)) <= mConverter.screenToCanvasDist(1.0f)) {
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
			
			action.add(new Action(new Stroke(color, getFinalPoly()), mNote.getInkLayer().size(), true));
			mNote.performActions(action);
		}
		
		points.clear();
		sizes.clear();
		poly.clear();
		cap.clear();
		containingIndex = -1;
	}

	public void startPinchEvent() { /* blank */	}
	public boolean continuePinchEvent(Point mid, Point dMid, float dZoom, float dist, RectF startBoundingRect) { return false; }
	public void canclePinchEvent() { /* blank */ }
	public void finishPinchEvent() { /* blank */ }
	public void startHoverEvent() { /* blank */ }
	public boolean continueHoverEvent(Point p, long time) { return false; }
	public void cancleHoverEvent() { /* blank */ }
	public void finishHoverEvent() { /* blank */ }

	public void drawNote(Canvas c) {
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
	}
	
	//**************************************** Utility Methods ************************************************
	
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
			startNewStroke(poly, tangentPoints, points.get(0), sizes.get(0));
		} else {
			if (sizes.get(0) >= sizes.get(1)) {		// first contains second
				containingIndex = 0;
				LinkedList<Point> start = MiscGeom.getLinkedCircularPoly(points.get(0), sizes.get(0));
				for (int i = 0; i < start.size()/2; i++) {
					poly.addLast(start.get(i));
				}
			} else {								// second contains first. THIS IS THE PROBLEM ONE!!!!
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
				poly.addLast(pts[0]);

				LinkedList<Point> right = MiscGeom.approximateCircularArc(points.get(points.size()-1), sizes.get(sizes.size()-1), false, poly.getLast(), offsets[1]);
				for (Point p : right) {
					poly.addLast(p);
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
			
			return;
		}
		
		while (poly.size() >= 2) {
			Point[] pts = MiscGeom.circleSegmentIntersection(points.get(points.size()-1), sizes.get(sizes.size()-1), poly.get(0), poly.get(1));
			poly.removeFirst();
			if (pts[0] != null) {
				poly.addFirst(pts[0]);
				
				LinkedList<Point> left = MiscGeom.approximateCircularArc(points.get(points.size()-1), sizes.get(sizes.size()-1), true, poly.getFirst(), offsets[0]);
				for (Point p : left) {
					poly.addFirst(p);
				}
				
				break;
			}
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
			LinkedList<Point> left = MiscGeom.approximateCircularArc(points.get(containingIndex), sizes.get(containingIndex), true, poly.getFirst(), tanIntPts[0]);
			for (Point p : left) {
				poly.addFirst(p);
			}
			poly.addFirst(tanIntPts[0]);
			poly.add(tanPts[1]);
		} else if (circIntPts != null) {
			LinkedList<Point> left = MiscGeom.approximateCircularArc(points.get(containingIndex), sizes.get(containingIndex), true, poly.getFirst(), circIntPts[0]);
			for (Point p : left) {
				poly.addFirst(p);
			}
			poly.addFirst(circIntPts[0]);
		} else {
			Log.d("PEN", "containment broken but no intersections");
			return;
		}
		
		// Right hand side
		if (tanIntPts[1] != null) {
			LinkedList<Point> right = MiscGeom.approximateCircularArc(points.get(containingIndex), sizes.get(containingIndex), false, poly.getLast(), tanIntPts[1]);
			for (Point p : right) {
				poly.addLast(p);
			}
			poly.addLast(tanIntPts[1]);
			poly.addLast(tanPts[3]);
		} else if (circIntPts != null) {
			LinkedList<Point> right = MiscGeom.approximateCircularArc(points.get(containingIndex), sizes.get(containingIndex), false, poly.getLast(), circIntPts[1]);
			for (Point p : right) {
				poly.addLast(p);
			}
			poly.addLast(circIntPts[1]);
		} else {
			Log.d("PEN", "containment broken but no intersections");
			return;
		}
		
		containingIndex = -1;
	}
	
	private void updateCap () {
		if (points.size() == 1) {
			cap = MiscGeom.getLinkedCircularPoly(points.get(0), sizes.get(0));
		} else if (containingIndex >= 0) {
			cap = MiscGeom.approximateCircularArc(points.get(containingIndex), sizes.get(containingIndex), false, poly.getLast(), poly.getFirst());
		} else if (points.size() > 1) {
			cap = MiscGeom.approximateCircularArc(points.get(points.size()-1), sizes.get(sizes.size()-1), false, poly.getLast(), poly.getFirst());
		}
	}
	
	private static void startNewStroke (LinkedList<Point> currentPoly, Point[] tangentPoints, Point prevPoint, float prevSize) {
		currentPoly.addAll(MiscGeom.approximateCircularArc(prevPoint, prevSize, false, tangentPoints[0], tangentPoints[2]));
		currentPoly.addFirst(tangentPoints[0]);
		currentPoly.addFirst(tangentPoints[1]);
		currentPoly.addLast(tangentPoints[2]);
		currentPoly.addLast(tangentPoints[3]);
	}
	
	private void addToLhs (Point tail, Point head, Point joinCenter, float joinRad, Point newCenter, float newRad) {
		Point intersection = MiscGeom.calcIntersection(poly.get(0), poly.get(1), head, tail);
		
		if (intersection != null) {
			poly.removeFirst();
			poly.addFirst(intersection);
			poly.addFirst(head);
		} else {
			LinkedList<Point> join = MiscGeom.approximateCircularArc(joinCenter, joinRad, true, poly.getFirst(), tail);
			for (Point p : join) {
				poly.addFirst(p);
			}
			poly.addFirst(tail);
			poly.addFirst(head);
		}
	}
	
	private void addToRhs (Point tail, Point head, Point joinCenter, float joinRad, Point newCenter, float newRad) {
		Point intersection = MiscGeom.calcIntersection(poly.get(poly.size()-1), poly.get(poly.size()-2), head, tail);
		
		if (intersection != null) {
			poly.removeLast();
			poly.addLast(intersection);
			poly.addLast(head);
		} else {
			LinkedList<Point> join = MiscGeom.approximateCircularArc(joinCenter, joinRad, false, poly.getLast(), tail);
			for (Point p : join) {
				poly.addLast(p);
			}
			poly.addLast(tail);
			poly.addLast(head);
		}
	}
	
	private float scalePressure (float pressure) {
		return 0.6f + pressure*0.4f;
	}
}