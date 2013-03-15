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
		} else if (aOnLeft < 0){	// b on left
			lines[0] = p1b;
			lines[1] = p2b;
			lines[2] = p1a;
			lines[3] = p2a;
		} else {
			Log.d("PEN", "Thickness is zero.");
			return null;
		}

		return lines;
	}
	
	public static LinkedList<Point> buildIntermediatePoly (LinkedList<Point> rawPoints, LinkedList<Float> rawPressure, float penSize) {
		LinkedList<Point> poly = new LinkedList<Point>();
		
		// Short circuit if we were passed nothing or a point
		if (rawPoints.size() < 2) {
			return poly;
		}
			
		for (int i = 1; i < rawPoints.size(); i++) {
			Point[] toAdd = Geometry.buildPolyLines(penSize*rawPressure.get(i-1), penSize*rawPressure.get(i), rawPoints.get(i-1), rawPoints.get(i));
			
			// Handle edge cases
			if (toAdd == null) {	// The points at i-1 and i are the same, do nothing
				continue;
			} else if (poly.size() < 4 || i < 2) {	// Add the first raw line segment's polygon segments without further processing
				poly.addFirst(toAdd[0]);
				poly.addFirst(toAdd[1]);
				
				LinkedList<Point> cap = Geometry.traceCircularPath(rawPoints.get(i-1), penSize*rawPressure.get(i-1)*0.5f, true, toAdd[0], toAdd[2]);
				poly.addAll(cap);				
				
				poly.addLast(toAdd[2]);
				poly.addLast(toAdd[3]);
				continue;
			}
			
			// Handle normal polygon construction
			Point rightIntersection = intersectLineIntoSegment(poly.get(poly.size()-2), poly.get(poly.size()-1), toAdd[2], toAdd[3]);
			Point leftIntersection = intersectLineIntoSegment(poly.get(0), poly.get(1), toAdd[0], toAdd[1]);
			
			if (rightIntersection == null) {
				//poly.addLast(toAdd[2]);
				poly.addLast(toAdd[3]);
			} else {
				poly.removeLast();
				poly.addLast(rightIntersection);
				poly.addLast(toAdd[3]);
			}
			
			if (leftIntersection == null) {
				//poly.addFirst(toAdd[0]);
				poly.addFirst(toAdd[1]);
			} else {
				poly.removeFirst();
				poly.addFirst(leftIntersection);
				poly.addFirst(toAdd[1]);
			}
		}
		
		// Add cap to the end of the stroke polygon
		poly.addAll(Geometry.traceCircularPath(rawPoints.getLast(), penSize*rawPressure.getLast()*0.5f, true, poly.getLast(), poly.getFirst()));
		
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

		// Close enough to parallel that we might as well just call them parallel.
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
	 * Calculate the intersection point of the line defined by l1 and l2 with the segment defined by s1 and s2.
	 *
	 * @return the intersection point, or null if it doesn't exist.
	 */
	public static Point intersectLineIntoSegment(Point l1, Point l2, Point s1, Point s2) {
		float denominator = (s2.y-s1.y)*(l2.x-l1.x)-(s2.x-s1.x)*(l2.y-l1.y);

		// Close enough to parallel that we might as well just call them parallel.
		if (Math.abs(denominator) < 0.00001) {
			return null;
		}

		// Note: I tried moving the division into the if statement but that actually slowed the benchmarks down. I think it might be a branch prediction
		// issue.
		float t = ((l2.x-l1.x)*(l1.y-s1.y)-(l2.y-l1.y)*(l1.x-s1.x))/denominator;

		if (t <= 1 && t >= 0) {
			return new Point(s1.x + t*(s2.x - s1.x), s1.y + t*(s2.y - s1.y));
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
	
	/**
	 * Check to see if check is counter-clockwise of ref based on origin.
	 * 
	 * @param check		The Point who's orientation is being checked
	 * @param origin	The shared origin point
	 * @param ref		The reference point
	 * @return -1 if counter-clockwise, 1 if clockwise, 0 if co-linear
	 */
	public static int checkClockwise(Point check, Point origin, Point ref) {
		float cross = (ref.x - origin.x)*(check.y - origin.y) - (check.x - origin.x)*(ref.y - origin.y);
		
		if (cross > 0) {
			return -1;
		} else if (cross < 0) {
			return 1;
		} else {
			return 0;
		}
	}
	
	
	/**
	 * An array of evenly spaced points on the unit circle going counter-clockwise starting at theta == 0;
	 */
	private static final Point[] CIRCLE ={	new Point ((float) Math.cos(0 * Math.PI/6), (float) Math.sin(0 * Math.PI/6)),
											new Point ((float) Math.cos(1 * Math.PI/6), (float) Math.sin(1 * Math.PI/6)),
											new Point ((float) Math.cos(2 * Math.PI/6), (float) Math.sin(2 * Math.PI/6)),
											new Point ((float) Math.cos(3 * Math.PI/6), (float) Math.sin(3 * Math.PI/6)),
											new Point ((float) Math.cos(4 * Math.PI/6), (float) Math.sin(4 * Math.PI/6)),
											new Point ((float) Math.cos(5 * Math.PI/6), (float) Math.sin(5 * Math.PI/6)),
											new Point ((float) Math.cos(6 * Math.PI/6), (float) Math.sin(6 * Math.PI/6)),
											new Point ((float) Math.cos(7 * Math.PI/6), (float) Math.sin(7 * Math.PI/6)),
											new Point ((float) Math.cos(8 * Math.PI/6), (float) Math.sin(8 * Math.PI/6)),
											new Point ((float) Math.cos(9 * Math.PI/6), (float) Math.sin(9 * Math.PI/6)),
											new Point ((float) Math.cos(10 * Math.PI/6), (float) Math.sin(10 * Math.PI/6)),
											new Point ((float) Math.cos(11 * Math.PI/6), (float) Math.sin(11 * Math.PI/6)),
										   };
	
	private static final double STEP_SIZE = (2 * Math.PI) / CIRCLE.length;
	
	/**
	 * Only guaranteed to work if from and to are within 1 ulp of being on the circle. Will almost definitely work if they're close, though. I think...
	 */
	public static LinkedList<Point> traceCircularPath (final Point center, final float radius, final boolean clockwise, final Point from, final Point to) {
		LinkedList<Point> path = new LinkedList<Point>();
		
		// Find the indexes of the points on the circle the path is starting and ending at
		int fromIndex = findAdjacentCircleIndex(from, center, clockwise);
		int toIndex = findAdjacentCircleIndex(to, center, !clockwise);
		
		Point[] scaledCircle = new Point[CIRCLE.length];
		for (int i = 0; i < scaledCircle.length; i++) {
			scaledCircle[i] = new Point(CIRCLE[i].x * radius + center.x, CIRCLE[i].y * radius + center.y);
		}
		
		int step;
		if (clockwise == true) {
			step = -1;
		} else {
			step = 1;
		}
		
		int counter = fromIndex;
		while (true) {
			path.add(scaledCircle[counter]);
			
			if (counter == toIndex) {
				break;
			}
			
			counter += step;
			
			if (counter >= scaledCircle.length) {
				counter = 0;
			} else if (counter < 0) {
				counter = scaledCircle.length - 1;
			}
		}
		
		return path;
	}
	
	/**
	 * @param p The point on the circle you're trying to find the adjacent point to
	 * @param center The center of the circle
	 * @param clockwise	if true, return the index of the point directly clockwise. if false return the index of the point directly counterclockwise.
	 * @return The index of the point in circle that's next to p in the direction specified by clockwise or -1 if something went wrong.
	 */
	public static int findAdjacentCircleIndex (Point p, Point center, boolean clockwise) {
		
		float mag = Geometry.distance(p.x, p.y, center.x, center.y);
		
		double angle;
		if (p.y - center.y >= 0) {
			angle = Math.acos((p.x-center.x)/mag);
		} else {
			angle = 2*Math.PI - Math.acos((p.x-center.x)/mag);
		}
		double continuousIndex = angle/STEP_SIZE;

		int toReturn;
		
		if (clockwise == true) {
			toReturn = (int) Math.floor(continuousIndex);
		} else {
			toReturn = (int) Math.ceil(continuousIndex);
			if (toReturn >= CIRCLE.length) {
				toReturn = 0;
			}
		}

		return toReturn;
	}
	
	
	
	
	
	
	
	
	
	
	
}