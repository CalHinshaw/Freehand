package com.calhounhinshaw.freehandalpha.note_editor;

import java.util.LinkedList;
import java.util.List;

import android.util.Log;

public class Geometry {
	
	
	public static float distance (float x1, float y1, float x2, float y2) {
		return (float)Math.sqrt(Math.pow((x1-x2), 2) + Math.pow((y1-y2), 2));
	}
	
	public static float distance (Point p1, Point p2) {
		return distance(p1.x, p1.y, p2.x, p2.y);
	}
	
	public static float distSq (float x1, float y1, float x2, float y2) {
		return (float) (Math.pow((x1-x2), 2) + Math.pow((y1-y2), 2));
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
		float aOnLeft = Geometry.cross(p1, p2, p1a, p2);
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
	
	@SuppressWarnings("unchecked")
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
			
			Object[] left = Geometry.joinSegments(poly.get(0), poly.get(1), toAdd[1], toAdd[0], true, rawPoints.get(i-1), penSize*rawPressure.get(i-1)*0.5f);
			if (left[0] != null) {
				poly.removeFirst();
				poly.addFirst((Point) left[0]); 
			}
			for (Point p : (LinkedList<Point>) left[1]) {
				poly.addFirst(p);
			}
			
			Object[] right = Geometry.joinSegments(poly.get(poly.size()-1), poly.get(poly.size()-2), toAdd[3], toAdd[2], false, rawPoints.get(i-1), penSize*rawPressure.get(i-1)*0.5f);
			if (right[0] != null) {
				poly.removeLast();
				poly.addLast((Point) right[0]);
			}
			for (Point p : (LinkedList<Point>) right[1]) {
				poly.addLast(p);
			}
		}
		
		// Add cap to the end of the stroke polygon
		if (poly.size() >= 2) {
			poly.addAll(Geometry.traceCircularPath(rawPoints.getLast(), penSize*rawPressure.getLast()*0.5f, true, poly.getLast(), poly.getFirst()));
		}
		
		return poly;
	}
	
	
	/**
	 * Calculates the join between the old and new segments of the Intermediate Polygon.
	 * 
	 * @param oldH The head of the old segment
	 * @param oldT The tail of the old segment
	 * @param newH the head of the new segment
	 * @param newT the tail of the new segment
	 * @param leftHanded True if the join is on the left handed side of the polygon
	 * @param center The center of the joint
	 * @param radius The radius of the joint
	 * @return an Object[]. Index zero is the point to replace oldH with in the polygon - if no replacement is needed the zero index is null.
	 * Index one is a LinkedList<Point> of new points to add to the polygon.
	 */
	public static Object[] joinSegments (Point oldH, Point oldT, Point newH, Point newT, boolean leftHanded, Point center, float radius) {
		Point replacement = null;
		LinkedList<Point> additions = new LinkedList<Point>();
		
		boolean segCcvToOld = (cross(oldH, oldT, newH, newT) < 0) == leftHanded;
		boolean joinCcvToOld = (cross(oldH, oldT, newT, oldH) < 0) == leftHanded;
		boolean segCcvToJoin = (cross(newT, oldH, newH, newT) < 0) == leftHanded;
		
		if (segCcvToOld == false && joinCcvToOld == false && segCcvToJoin == false) {
			for (Point p : Geometry.traceCircularPath(center, radius, !leftHanded, oldH, newT)) {
				additions.add(p);
			}
			additions.add(newT);
			additions.add(newH);
		} else if ((segCcvToOld == false && joinCcvToOld == false && segCcvToJoin == true) || (segCcvToOld == true && joinCcvToOld == true && segCcvToJoin == false)) {
			replacement = Geometry.intersectLineIntoSegment(oldH, oldT, newH, newT);
			additions.add(newH);
		} else if ((segCcvToOld == false && joinCcvToOld == true && segCcvToJoin == false) || (segCcvToOld == true && joinCcvToOld == false && segCcvToJoin == true)) {
			replacement = Geometry.intersectLineIntoSegment(newH, newT, oldH, oldT);
			additions.add(newH);
		} else if (segCcvToOld == true && joinCcvToOld == false && segCcvToJoin == false) {
			replacement = Geometry.intersectLineIntoSegment(oldH, oldT, newH, newT);
			additions.add(newH);
		} else {
			
			Point intersection = Geometry.intersectLineIntoSegment(oldH, oldT, newH, newT);
			if (intersection != null) {
				additions.add(intersection);
				additions.add(newH);
				//Log.d("PEN", "intersection");
			} else {
				//Log.d("PEN", "no intersection");
			}
		}
			
		Object[] toReturn = new Object[2];
		toReturn[0] = replacement;
		toReturn[1] = additions;
		return toReturn;
	}
	
	/**
	 * Calculates the join between the old and new segments of the Intermediate Polygon.
	 * 
	 * @param oldH The head of the old segment
	 * @param oldT The tail of the old segment
	 * @param newH the head of the new segment
	 * @param newT the tail of the new segment
	 * @param leftHanded True if the join is on the left handed side of the polygon
	 * @param center The center of the joint
	 * @param radius The radius of the joint
	 * @return an Object[]. Index zero is the point to replace oldH with in the polygon - if no replacement is needed the zero index is null.
	 * Index one is a LinkedList<Point> of new points to add to the polygon.
	 */
	public static Object[] joinSegs (Point oldH, Point oldT, Point newH, Point newT, boolean leftHanded, Point center, float radius) {
		Point replacement = null;
		LinkedList<Point> additions = new LinkedList<Point>();
		
		boolean segCcvToOld = (cross(oldH, oldT, newH, newT) < 0) == leftHanded;
		boolean joinCcvToOld = (cross(oldH, oldT, newT, oldH) < 0) == leftHanded;
		boolean segCcvToJoin = (cross(newT, oldH, newH, newT) < 0) == leftHanded;
		
		if (segCcvToOld == false && joinCcvToOld == false && segCcvToJoin == false) {
			for (Point p : Geometry.traceCircularPath(center, radius, !leftHanded, oldH, newT)) {
				additions.add(p);
			}
			additions.add(newT);
			additions.add(newH);
		} else {
			Point intersection = twoWayIntersect(newH, newT, oldH, oldT);
			if (intersection != null) {
				replacement = intersection;
				additions.add(newH);
			}
		}
			
		Object[] toReturn = new Object[2];
		toReturn[0] = replacement;
		toReturn[1] = additions;
		return toReturn;
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
		if (Math.abs(denominator) < 0.000000000001) {
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
	
	public static Point twoWayIntersect (Point a, Point b, Point c, Point d) {
		float denominator = (d.y-c.y)*(b.x-a.x)-(d.x-c.x)*(b.y-a.y);

		// Close enough to parallel that we might as well just call them parallel.
		if (Math.abs(denominator) < 0.000000000001) {
			return null;
		}

		// Note: I tried adding the divisions into the if statement but that actually slowed the benchmarks down. I think it might be a branch prediction
		// issue.
		float Ta = ((d.x-c.x)*(a.y-c.y)-(d.y-c.y)*(a.x-c.x))/denominator;
		float Tc = ((b.x-a.x)*(a.y-c.y)-(b.y-a.y)*(a.x-c.x))/denominator;

		if ((Ta <= 1 && Ta >= 0) || (Tc <= 1 && Tc >= 0)) {
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
	
	
	/**
	 * Compute a cross b.
	 * 
	 * @param aH the head of the a vector
	 * @param aT the tail of the a vector
	 * @param bH the head of the b vector
	 * @param bT the tail of the b vector
	 * @return z-axis value of a cross b
	 */
	public static float cross(Point aH, Point aT, Point bH, Point bT) {
		return ((aH.x-aT.x)*(bH.y-bT.y) - (aH.y-aT.y)*(bH.x-bT.x));
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
		
		if ((fromIndex == toIndex+1 && clockwise == false) || (fromIndex == 0 && toIndex == CIRCLE.length-1 && clockwise == false ) ||
			(fromIndex == toIndex-1 && clockwise == true) || (fromIndex == CIRCLE.length-1 && toIndex == 0 && clockwise == true)) {
			return path;
		}
		
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
	
	
	public static LinkedList<Float> smoothPressures (LinkedList<Float> in) {
		if (in.size() < 3) {
			return in;
		}
		
		LinkedList<Float> toReturn = new LinkedList<Float>();
		
		toReturn.addLast(in.getFirst());
		for (int i = 1; i < in.size()-1; i++) {
			if ((in.get(i) < in.get(i-1) && in.get(i) < in.get(i+1)) || (in.get(i) > in.get(i-1) && in.get(i) > in.get(i+1))) {
				toReturn.addLast(Float.valueOf((in.get(i-1) + in.get(i+1))/2));
			} else {
				toReturn.addLast(in.get(i));
			}
		}
		toReturn.addLast(in.getLast());
		
		return toReturn;
	}
	
	private static final float SG0 = 17.0f / 35.0f;
	private static final float SG1 = 12.0f / 35.0f;
	private static final float SG2 = -3.0f / 35.0f;
	
	public static LinkedList<Float> sgSmooth (LinkedList<Float> in) {
		if (in.size() < 5) {
			return in;
		}
		
		LinkedList<Float> toReturn = new LinkedList<Float>();
		
		toReturn.add(in.get(0));
		toReturn.add(in.get(1));
		
		for (int i = 2; i < in.size()-2; i++) {
			toReturn.add(Float.valueOf(in.get(i-2)*SG2 + in.get(i-1)*SG1 + in.get(i)*SG0 + in.get(i+1)*SG1 + in.get(i+2)*SG2));
		}
		
		toReturn.add(in.get(in.size()-2));
		toReturn.add(in.get(in.size()-1));
		
		return toReturn;
	}
	
	
	
	
	
	
}