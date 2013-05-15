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
			Log.d("PEN", "normal start");
			startNewStroke(poly, tangentPoints, points.get(0), sizes.get(0));
		} else {
			Log.d("PEN", "abnormal start");
			if (sizes.get(0) >= sizes.get(1)) {		// first contains second
				containingIndex = 0;
				LinkedList<Point> start = MiscGeom.getCircularPoly(points.get(0), sizes.get(0));
				for (Point p : start) {
					poly.addFirst(p);
				}
			} else {								// second contains first
				LinkedList<Point> start = MiscGeom.getCircularPoly(points.get(1), sizes.get(1));
				for (Point p : start) {
					poly.addFirst(p);
				}
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
	
	
	/**
	 * right handed first
	 */
	private static Point[] calcPerpOffset (Point p, Point ref, float dist) {
		Point perpVect = new Point((ref.y - p.y), (p.x - ref.x));
		float scalar1 = dist/(2*(float) Math.hypot(perpVect.x, perpVect.y));

		Point[] points = new Point[2];
		points[0] = new Point(p.x + scalar1*perpVect.x, p.y + scalar1*perpVect.y);
		points[1] = new Point(p.x - scalar1*perpVect.x, p.y - scalar1*perpVect.y);
		
		return points;
	}
	
	
	private void backtrack () {
		
		// calculate the points to trace the circle to
		Point[] offsets = calcPerpOffset(points.get(points.size()-1), points.get(points.size()-2), sizes.get(sizes.size()-1));
		
		while (poly.size() >= 2) {
			Point[] pts = MiscGeom.circleSegmentIntersection(points.get(points.size()-1), sizes.get(sizes.size()-1), poly.get(poly.size()-1), poly.get(poly.size()-2));
			poly.removeLast();
			if (pts[0] != null) {
				poly.addLast(pts[0]);

				LinkedList<Point> right = MiscGeom.traceCircularPath(points.get(points.size()-1), sizes.get(sizes.size()-1), false, poly.getLast(), offsets[1]);
				for (Point p : right) {
					poly.addLast(p);
				}
				
				break;
			}
		}
		
		if (poly.size() < 2) {
			poly.clear();
			
			if (Float.isNaN(offsets[0].x) == false) {
				LinkedList<Point> left = MiscGeom.traceCircularPath(points.get(points.size()-1), sizes.get(sizes.size()-1), true, offsets[0], offsets[1]);
				for (Point p : left) {
					poly.addFirst(p);
				}
			} else {
				poly = MiscGeom.getCircularPoly(points.get(points.size()-1), sizes.get(sizes.size()-1));
			}
			
			return;
		}
		
		while (poly.size() >= 2) {
			Point[] pts = MiscGeom.circleSegmentIntersection(points.get(points.size()-1), sizes.get(sizes.size()-1), poly.get(0), poly.get(1));
			poly.removeFirst();
			if (pts[0] != null) {
				poly.addFirst(pts[0]);
				
				LinkedList<Point> left = MiscGeom.traceCircularPath(points.get(points.size()-1), sizes.get(sizes.size()-1), true, poly.getFirst(), offsets[0]);
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
		if (tanIntPts[1] != null) {
			LinkedList<Point> left = MiscGeom.traceCircularPath(points.get(containingIndex), sizes.get(containingIndex), true, poly.getFirst(), tanIntPts[1]);
			for (Point p : left) {
				poly.addFirst(p);
			}
			poly.add(tanPts[2]);
		} else {
			LinkedList<Point> left = MiscGeom.traceCircularPath(points.get(containingIndex), sizes.get(containingIndex), true, poly.getFirst(), circIntPts[1]);
			for (Point p : left) {
				poly.addFirst(p);
			}
		}
		
		// Right hand side
		if (tanIntPts[0] != null) {
			LinkedList<Point> right = MiscGeom.traceCircularPath(points.get(containingIndex), sizes.get(containingIndex), false, poly.getLast(), tanIntPts[0]);
			for (Point p : right) {
				poly.addLast(p);
			}
			poly.add(tanPts[0]);
		} else {
			LinkedList<Point> right = MiscGeom.traceCircularPath(points.get(containingIndex), sizes.get(containingIndex), false, poly.getLast(), circIntPts[0]);
			for (Point p : right) {
				poly.addLast(p);
			}
		}
		
		containingIndex = -1;
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
	
	
	private void addToLhs (Point tail, Point head, Point joinCenter, float joinRad, Point newCenter, float newRad) {
		
		if (MiscGeom.cross(poly.get(0), poly.get(1), head, poly.get(1)) <= 0) {
			LinkedList<Point> join = MiscGeom.traceCircularPath(joinCenter, joinRad, true, poly.getFirst(), tail);
			for (Point p : join) {
				poly.addFirst(p);
			}
			poly.addFirst(tail);
			poly.addFirst(head);
		} else {
			Point intersection = MiscGeom.calcIntersection(poly.get(0), poly.get(1), head, tail);
			if (intersection == null) {
				poly.addFirst(tail);
				poly.addFirst(head);
			} else {
				poly.removeFirst();
				poly.addFirst(intersection);
				poly.addFirst(head);
			}
		}
	}
	
	private void addToRhs (Point tail, Point head, Point joinCenter, float joinRad, Point newCenter, float newRad) {
		if (MiscGeom.cross(poly.get(poly.size()-1), poly.get(poly.size()-2), head, poly.get(poly.size()-2)) >= 0) {
			LinkedList<Point> join = MiscGeom.traceCircularPath(joinCenter, joinRad, false, poly.getLast(), tail);
			for (Point p : join) {
				poly.addLast(p);
			}
			poly.addLast(tail);
			poly.addLast(head);
		} else {
			Point intersection = MiscGeom.calcIntersection(poly.get(poly.size()-1), poly.get(poly.size()-2), head, tail);
			if (intersection == null) {
				poly.addLast(tail);
				poly.addLast(head);
			} else {
				poly.removeLast();
				poly.addLast(intersection);
				poly.addLast(head);
			}
		}
	}
	
}