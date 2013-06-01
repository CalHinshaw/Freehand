package com.freehand.ink;

import java.util.ArrayList;
import java.util.LinkedList;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

import com.freehand.misc.WrapList;

public class StrokePolyBuilder {
	private ArrayList<Point> points = new ArrayList<Point>(1000);
	private ArrayList<Float> sizes = new ArrayList<Float>(1000);
	
	private Path path = new Path();
	private Paint paint = new Paint();
	
	private LinkedList<Point> poly = new LinkedList<Point>();
	private LinkedList<Point> cap = new LinkedList<Point>();
	
	private int containingIndex = -1;
	
	public StrokePolyBuilder () {
		path.setFillType(Path.FillType.WINDING);
		
		paint.setColor(Color.BLACK);
		paint.setStyle(Paint.Style.FILL);
		paint.setAntiAlias(true);
	}
	
	public void reset () {
		points.clear();
		sizes.clear();
		poly.clear();
		cap.clear();
		containingIndex = -1;
	}
	
	public void setColor(int newColor) {
		paint.setColor(newColor);
	}
	
	public WrapList<Point> getFinalPoly () {
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
	
	public void add (Point p, float s, float zoomMultiplier) {
		if (points.size() > 0 && s == sizes.get(sizes.size()-1) &&
			MiscGeom.distance(p, points.get(points.size()-1)) < 2.5f/zoomMultiplier) {
			return;
		}
		
		points.add(p);
		sizes.add(s);
		
		if (points.size() == 2) {
			startPoly();
		} else if (points.size() > 2) {
			addToPoly();
		}
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
	
	
	public void draw (Canvas c) {
		updateCap();
		
//		Paint debug = new Paint();
//		debug.setColor(0x40ff0000);
//		debug.setStyle(Paint.Style.STROKE);
//		debug.setStrokeWidth(0);
//		debug.setAntiAlias(true);
		
//		for (int i = 0; i < sizes.size(); i++) {
//			c.drawCircle(points.get(i).x, points.get(i).y, sizes.get(i), debug);
//		}
		
		if (poly != null && poly.size() >= 3) {
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
	
}