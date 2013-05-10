package com.calhounhinshaw.freehandalpha.ink;

import java.util.ArrayList;
import java.util.LinkedList;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

import com.calhounhinshaw.freehandalpha.misc.WrapList;

public class StrokePolyBuilder {
	private ArrayList<Point> points = new ArrayList<Point>(1000);
	private ArrayList<Float> sizes = new ArrayList<Float>(1000);
	private Path path = new Path();
	
	private LinkedList<Point> poly = new LinkedList<Point>();
	private LinkedList<Point> cap = new LinkedList<Point>();
	
	private int containingIndex = -1;
	
	
	public StrokePolyBuilder () {
		path.setFillType(Path.FillType.WINDING);
	}
	
	public void reset () {
		points.clear();
		sizes.clear();
		poly.clear();
		cap.clear();
		containingIndex = -1;
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
		
		//Log.d("PEN", Integer.toString(containingIndex));
		
		
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
			
			// TODO next line is temporary
			poly.addAll(MiscGeom.getCircularPoly(points.get(0), sizes.get(0)));
			
			if (sizes.get(0) >= sizes.get(1)) {		// first contains second
				//containingIndex = 0;
			} else {								// second contains first
				// TODO
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
			StrokePolyBuilder.addNewSeg(poly, tangentPoints[0], tangentPoints[1], points.get(points.size()-2), sizes.get(sizes.size()-2), true);
			StrokePolyBuilder.addNewSeg(poly, tangentPoints[2], tangentPoints[3], points.get(points.size()-2), sizes.get(sizes.size()-2), false);
		} else {
			if (sizes.get(sizes.size()-1) <= sizes.get(sizes.size()-2)) {	// new contained by old -> Start containment
				Log.d("PEN", "containing index set");
				containingIndex = sizes.size()-2;
			} else {														// old contained by new -> backtrack
				// TODO implement all of the backtracking crap
				Log.d("PEN", "should backtrack");
			}
		}

	}
	
	private void breakContainment () {

		Point[] tangentPoints = MiscGeom.calcExternalBitangentPoints(points.get(points.size()-2), sizes.get(sizes.size()-2), points.get(points.size()-1), sizes.get(sizes.size()-1));
		
		if (tangentPoints != null) {
			Point[] left = MiscGeom.calcCircleSegmentIntersection(points.get(containingIndex), sizes.get(containingIndex), tangentPoints[0], tangentPoints[1]);
			Point[] right = MiscGeom.calcCircleSegmentIntersection(points.get(containingIndex), sizes.get(containingIndex), tangentPoints[2], tangentPoints[3]);
			
			if (left[0] != null && right[0] != null) {
				StrokePolyBuilder.addNewSeg(poly, left[0], tangentPoints[1], points.get(containingIndex), sizes.get(containingIndex), true);
				StrokePolyBuilder.addNewSeg(poly, right[0], tangentPoints[3], points.get(containingIndex), sizes.get(containingIndex), false);
				containingIndex = -1;
			} else if (left[0] != null) {
				StrokePolyBuilder.addNewSeg(poly, left[0], tangentPoints[1], points.get(containingIndex), sizes.get(containingIndex), true);
				
				// need to intersect the two circles, pick the correct intersection point, and trace along the containing circle clockwise to the point.
				
				Point[] intersections = MiscGeom.circleCircleIntersection(points.get(containingIndex), sizes.get(containingIndex), points.get(points.size()-1), sizes.get(sizes.size()-1));
				// trace along circle to intersections[0]
				
				LinkedList<Point> joinPoints = MiscGeom.traceCircularPath(points.get(points.size()-1), sizes.get(sizes.size()-1), true, poly.getLast(), intersections[0]);
				
				for (Point p : joinPoints) {
					poly.addLast(p);
				}
				
				poly.addFirst(intersections[0]);
				
				containingIndex = -1;
			} else if (right[0] != null) {
				StrokePolyBuilder.addNewSeg(poly, right[0], tangentPoints[3], points.get(containingIndex), sizes.get(containingIndex), false);
				
				// need to intersect the two circles, pick the correct intersection point, and trace along the containing circle clockwise to the point.
				
				Point[] intersections = MiscGeom.circleCircleIntersection(points.get(containingIndex), sizes.get(containingIndex), points.get(points.size()-1), sizes.get(sizes.size()-1));
				// trace along circle to intersections[0]
				
				LinkedList<Point> joinPoints = MiscGeom.traceCircularPath(points.get(points.size()-1), sizes.get(sizes.size()-1), false, intersections[1], poly.getFirst());
				
				for (Point p : joinPoints) {
					poly.addFirst(p);
				}
				
				poly.addLast(intersections[1]);
				
				
				
				containingIndex = -1;
			}
		}
	}
	
	
	public void draw (Canvas c, Paint paint) {
		updateCap();
		
		Paint debug = new Paint();
		debug.setColor(0x40ff0000);
		debug.setStyle(Paint.Style.STROKE);
		debug.setStrokeWidth(0);
		debug.setAntiAlias(true);
		
		for (int i = 0; i < sizes.size(); i++) {
			c.drawCircle(points.get(i).x, points.get(i).y, sizes.get(i), debug);
		}
		
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
			cap = MiscGeom.getCircularPoly(points.get(0), sizes.get(0));
		} else if (containingIndex >= 0) {
			cap = MiscGeom.traceCircularPath(points.get(containingIndex), sizes.get(containingIndex), false, poly.getLast(), poly.getFirst());
		} else if (points.size() > 1) {
			cap = MiscGeom.traceCircularPath(points.get(points.size()-1), sizes.get(sizes.size()-1), false, poly.getLast(), poly.getFirst());
		}
	}
	
	
	
	
	
	
	private static void startNewStroke (LinkedList<Point> currentPoly, Point[] tangentPoints, Point prevPoint, float prevSize) {
		currentPoly.addAll(MiscGeom.traceCircularPath(prevPoint, prevSize, false, tangentPoints[0], tangentPoints[2]));
		currentPoly.addFirst(tangentPoints[0]);
		currentPoly.addFirst(tangentPoints[1]);
		currentPoly.addLast(tangentPoints[2]);
		currentPoly.addLast(tangentPoints[3]);
	}
	
	
	
	private static void addNewSeg (LinkedList<Point> currentPoly, Point tail, Point head, Point joinCenter, float joinRad, boolean leftHanded) {
		if (leftHanded == true) {
			if (MiscGeom.cross(currentPoly.get(0), currentPoly.get(1), head, tail) >= 0) {
				Point intersection = MiscGeom.calcIntersection(currentPoly.get(0), currentPoly.get(1), head, tail);
				if (intersection == null) {
					currentPoly.addFirst(tail);
					currentPoly.addFirst(head);
				} else {
					currentPoly.removeFirst();
					currentPoly.addFirst(intersection);
					currentPoly.addFirst(head);
				}
			} else {
				LinkedList<Point> join = MiscGeom.traceCircularPath(joinCenter, joinRad, true, currentPoly.getFirst(), tail);
				for (Point p : join) {
					currentPoly.addFirst(p);
				}
				currentPoly.addFirst(tail);
				currentPoly.addFirst(head);
			}
		} else {
			if (MiscGeom.cross(currentPoly.get(currentPoly.size()-1), currentPoly.get(currentPoly.size()-2), head, tail) <= 0) {
				Point intersection = MiscGeom.calcIntersection(currentPoly.get(currentPoly.size()-1), currentPoly.get(currentPoly.size()-2), head, tail);
				if (intersection == null) {
					currentPoly.addLast(tail);
					currentPoly.addLast(head);
				} else {
					currentPoly.removeLast();
					currentPoly.addLast(intersection);
					currentPoly.addLast(head);
				}
			} else {
				LinkedList<Point> join = MiscGeom.traceCircularPath(joinCenter, joinRad, false, currentPoly.getLast(), tail);
				for (Point p : join) {
					currentPoly.addLast(p);
				}
				currentPoly.addLast(tail);
				currentPoly.addLast(head);
			}
		}
		
		
		
		
	}
}