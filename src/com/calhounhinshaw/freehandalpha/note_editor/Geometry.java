package com.calhounhinshaw.freehandalpha.note_editor;

import java.util.LinkedList;
import java.util.List;

import android.util.Log;

public class Geometry {
	
	
	public static float distance (float x1, float y1, float x2, float y2) {
		return (float)Math.sqrt(Math.pow((x1-x2), 2) + Math.pow((y1-y2), 2));
	}
	
	/**
	 * Calculates the two line segments that represent the polygon from p1 to p2.
	 * 
	 * @param w1 the width of the stroke at p1
	 * @param w2 the width of the stroke at p2
	 * @param p1 the first point of the line segment
	 * @param p2 the second point of the line segment
	 * 
	 * @return a 4 element array of points - the first two are the left-handed line (think Greene's theorem). If the points are equal returns null.
	 */
	public static Point[] buildPolyLines (float w1, float w2, Point p1, Point p2) {
		
		// if the pen didn't move return null
		if (p1.x == p2.x && p1.y == p2.y) {
			return null;
		}
		
		// A vector perpendicular to the segment
		Point perpVect = new Point((p2.y - p1.y), (p1.x - p2.x));
		
		// The magnitude of perpVect
		float magnitude = (float) Math.hypot(perpVect.x, perpVect.y);
		
		// The amount to scale perpVect by to get the points on either side of p1
		float scalar1 = w1/(2*magnitude);
		
		// The points on either side of p1
		Point p1a = new Point(p1.x + scalar1*perpVect.x, p1.y + scalar1*perpVect.y);
		Point p1b = new Point(p1.x - scalar1*perpVect.x, p1.y - scalar1*perpVect.y);
		
		// The amount to scale perpVect to get the points on either side of p2
		float scalar2 = w2/(2*magnitude);

		// The points on either side of p2
		Point p2a = new Point(p2.x + scalar2*perpVect.x, p2.y + scalar2*perpVect.y);
		Point p2b = new Point(p2.x - scalar2*perpVect.x, p2.y - scalar2*perpVect.y);
		
		// Put the left handed points first in the array
		Point[] lines = new Point[4];
		int aOnLeft = checkClockwise(p1a, p1, p2);
		if (aOnLeft > 0) {			// a on left
			lines[0] = p1a;
			lines[1] = p2a;
			lines[2] = p1b;
			lines[3] = p2b;
		} else if (aOnLeft < 0){					// b on left
			lines[0] = p1b;
			lines[1] = p2b;
			lines[2] = p1a;
			lines[3] = p2a;
		} else {
			Log.d("PEN", "Couldn't determine which segment was on the left handed side");
			return null;
		}

		return lines;
	}
	
	public static LinkedList<Point> buildIntermediatePoly (List<Point> rawPoints, List<Float> rawPressure, float penSize) {
		LinkedList<Point> poly = new LinkedList<Point>();
		
		if (rawPoints.size() >= 2) {
			for (int i = 1; i < rawPoints.size(); i++) {
				Point[] toAdd = Geometry.buildPolyLines(penSize*rawPressure.get(i-1), penSize*rawPressure.get(i), rawPoints.get(i-1), rawPoints.get(i));
				
				if (toAdd != null) {
					
					// Only remove intersections if the line being added isn't the first
					if (poly.size() >= 4) {
						Point leftIntersection = Geometry.calcIntersection(poly.get(0), poly.get(1), toAdd[0], toAdd[1]);
						Point rightIntersection = Geometry.calcIntersection(poly.get(poly.size()-2), poly.get(poly.size()-1), toAdd[2], toAdd[3]);
						
						if (leftIntersection == null) {
							poly.addFirst(toAdd[0]);
							poly.addFirst(toAdd[1]);
						} else {
							poly.removeFirst();
							poly.addFirst(leftIntersection);
							poly.addFirst(toAdd[1]);
						}
						
						if (rightIntersection == null) {
							poly.addLast(toAdd[2]);
							poly.addLast(toAdd[3]);
						} else {
							poly.removeLast();
							poly.addLast(rightIntersection);
							poly.addLast(toAdd[3]);
						}
						
					} else {
						poly.addFirst(toAdd[0]);
						poly.addFirst(toAdd[1]);
						poly.addLast(toAdd[2]);
						poly.addLast(toAdd[3]);
					}
				}
			}
		}
		
		return poly;
	}
	
	
	/**
	 * Calculates the intersection of the two line segments. The line segments are considered closed.
	 * 
	 * @return The point of intersection if it exists, null otherwise.
	 */
	public static Point calcIntersection (Point a, Point b, Point c, Point d) {
		
		if (intersectionPossible(a, b, c, d) == false) {
			return null;
		}
		
		float denominator = (d.y-c.y)*(b.x-a.x)-(d.x-c.x)*(b.y-a.y);

		if (Math.abs(denominator) < 0.00001) {
			return null;
		}

		// Note: I tried adding the divisions into the if statement but that actually slowed the benchmarks down. I think it might be a branch prediction
		// issue.
		float Ta = ((d.x-c.x)*(a.y-c.y)-(d.y-c.y)*(a.x-c.x))/denominator;
		float Tc = ((b.x-a.x)*(a.y-c.y)-(b.y-a.y)*(a.x-c.x))/denominator;

		if (Ta <= 1 && Ta >= 0 && Tc <= 1 && Tc >= 0) {
			return new Point(a.x + Ta*(b.x - a.x), a.y + Ta*(b.y - a.y));
		} else {
			return null;
		}
	}
	
	/**
	 * Determines whether it's possible for two line segments to intersect by checking their bounding rectangles.

	 * @return true if the line segments can intersect, false if they can't.
	 */
	public static boolean intersectionPossible (Point a, Point b, Point c, Point d) {
		boolean aHigher = (a.y < b.y);
		boolean aLefter = (a.x < b.x);
		boolean cHigher = (c.y < d.y);
		boolean cLefter = (c.x < d.x);
		
		if (  !(  ( (aHigher ? a.y : b.y) <= (cHigher ? d.y : c.y) ) && ( (aHigher ? b.y : a.y) >= (cHigher ? c.y : d.y) )  )  ) {
			return false;
		} else if (  !(  ( (cLefter ? c.x : d.x) <= (aLefter ? b.x : a.x) ) && ( (cLefter ? d.x : c.x) >= (aLefter ? a.x : b.x) )  )  ) {
			return false;
		} else {
			return true;
		}
	}
	
	public static int checkClockwise(Point c, Point a, Point b) {
		float cross = (b.x - a.x)*(c.y - a.y) - (c.x - a.x)*(b.y - a.y);
		
		if (cross > 0) {
			return -1;
		} else if (cross < 0) {
			return 1;
		} else {
			return 0;
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}