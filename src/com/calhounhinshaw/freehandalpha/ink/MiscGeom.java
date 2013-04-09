package com.calhounhinshaw.freehandalpha.ink;

import java.util.ArrayList;

import com.calhounhinshaw.freehandalpha.misc.WrapList;

public class MiscGeom {
	private static final float SG0 = 17.0f / 35.0f;
	private static final float SG1 = 12.0f / 35.0f;
	private static final float SG2 = -3.0f / 35.0f;
	
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
	
	/**
	 * Calculate the intersection of the line defined by a and b into the segment defined by c and d, or the line defined by b and c into the segment defined by a and b.
	 */
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
	 * Simple, small effect Savitzky-Golay filter.
	 */
	public static ArrayList<Float> sgSmooth (ArrayList<Float> in) {
		if (in.size() < 5) {
			return in;
		}
		
		ArrayList<Float> toReturn = new ArrayList<Float>(in.size());
		
		toReturn.add(in.get(0));
		toReturn.add(in.get(1));
		
		for (int i = 2; i < in.size()-2; i++) {
			toReturn.add(Float.valueOf(in.get(i-2)*SG2 + in.get(i-1)*SG1 + in.get(i)*SG0 + in.get(i+1)*SG1 + in.get(i+2)*SG2));
		}
		
		toReturn.add(in.get(in.size()-2));
		toReturn.add(in.get(in.size()-1));
		
		return toReturn;
	}
	
	/**
	 * UNTESTED, DON'T TRUST! (it's from the prototype ink when I was using polylines and PointF)
	 */
	public static boolean pointInPoly (Point point, WrapList<Point> poly) {
		// -1 if not valid, else 0 if below and 1 if above
		int ptState = -1;
		int intersections = 0;
		
		for (int i = 0; i <= poly.size(); i++) {
			if (poly.get(i).x < point.x) {
				ptState = -1;
			} else {
				if (ptState == -1) {
					if (poly.get(i).y >= point.y) {
						ptState = 1;
					} else {
						ptState = 0;
					}
				} else if ((poly.get(i).y >= point.y) && ptState == 0) {
					intersections++;
					ptState = 1;
				} else if ((poly.get(i).y < point.y) && ptState == 1) {
					intersections++;
					ptState = 0;
				}
			}
		}
		
		if (intersections >=1 && intersections%2 == 1) {
			return true;
		} else {
			return false;
		}
	}
}