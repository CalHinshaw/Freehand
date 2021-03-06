package com.freehand.ink;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.graphics.RectF;
import android.util.Log;

public class MiscGeom {
	private static final float SG0 = 17.0f / 35.0f;
	private static final float SG1 = 12.0f / 35.0f;
	private static final float SG2 = -3.0f / 35.0f;
	
	public static float distance (float x1, float y1, float x2, float y2) {
		return (float) Math.sqrt(Math.pow((x1-x2), 2) + Math.pow((y1-y2), 2));
	}
	
	public static float distance (Point p1, Point p2) {
		return distance(p1.x, p1.y, p2.x, p2.y);
	}
	
	public static float distSq (float x1, float y1, float x2, float y2) {
		return (float) (Math.pow((x1-x2), 2) + Math.pow((y1-y2), 2));
	}
	
	public static float distSq (Point p1, Point p2) {
		return distSq(p1.x, p1.y, p2.x, p2.y);
	}
	
	public static double distSqDbl (Point p1, Point p2) {
		double dx = p1.x - p2.x;
		double dy = p1.y - p2.y;
		return dx*dx + dy*dy;
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
	 * An array of evenly spaced points on the unit circle going counter-clockwise starting at theta == 0;
	 */
	public static final Point[] CIRCLE = {
			new Point((float) Math.cos(0 * Math.PI / 10), (float) Math.sin(0 * Math.PI / 10)),
			new Point((float) Math.cos(1 * Math.PI / 10), (float) Math.sin(1 * Math.PI / 10)),
			new Point((float) Math.cos(2 * Math.PI / 10), (float) Math.sin(2 * Math.PI / 10)),
			new Point((float) Math.cos(3 * Math.PI / 10), (float) Math.sin(3 * Math.PI / 10)),
			new Point((float) Math.cos(4 * Math.PI / 10), (float) Math.sin(4 * Math.PI / 10)),
			new Point((float) Math.cos(5 * Math.PI / 10), (float) Math.sin(5 * Math.PI / 10)),
			new Point((float) Math.cos(6 * Math.PI / 10), (float) Math.sin(6 * Math.PI / 10)),
			new Point((float) Math.cos(7 * Math.PI / 10), (float) Math.sin(7 * Math.PI / 10)),
			new Point((float) Math.cos(8 * Math.PI / 10), (float) Math.sin(8 * Math.PI / 10)),
			new Point((float) Math.cos(9 * Math.PI / 10), (float) Math.sin(9 * Math.PI / 10)),
			new Point((float) Math.cos(10 * Math.PI / 10), (float) Math.sin(10 * Math.PI / 10)),
			new Point((float) Math.cos(11 * Math.PI / 10), (float) Math.sin(11 * Math.PI / 10)),
			new Point((float) Math.cos(12 * Math.PI / 10), (float) Math.sin(12 * Math.PI / 10)),
			new Point((float) Math.cos(13 * Math.PI / 10), (float) Math.sin(13 * Math.PI / 10)),
			new Point((float) Math.cos(14 * Math.PI / 10), (float) Math.sin(14 * Math.PI / 10)),
			new Point((float) Math.cos(15 * Math.PI / 10), (float) Math.sin(15 * Math.PI / 10)),
			new Point((float) Math.cos(16 * Math.PI / 10), (float) Math.sin(16 * Math.PI / 10)),
			new Point((float) Math.cos(17 * Math.PI / 10), (float) Math.sin(17 * Math.PI / 10)),
			new Point((float) Math.cos(18 * Math.PI / 10), (float) Math.sin(18 * Math.PI / 10)),
			new Point((float) Math.cos(19 * Math.PI / 10), (float) Math.sin(19 * Math.PI / 10)),
	};

	
	public static LinkedList<Point> getLinkedCircularPoly(Point center, float radius) {
		LinkedList<Point> toReturn = new LinkedList<Point>();
		for (int i = CIRCLE.length - 1; i >= 0; i--) {
			toReturn.add(new Point(CIRCLE[i].x * radius + center.x, CIRCLE[i].y * radius + center.y));
		}
		return toReturn;
	}
	
	public static List<Point> getWrapCircularPoly(Point center, float radius) {
		List<Point> toReturn = new ArrayList<Point>(CIRCLE.length);
		for (int i = CIRCLE.length - 1; i >= 0; i--) {
			toReturn.add(new Point(CIRCLE[i].x * radius + center.x, CIRCLE[i].y * radius + center.y));
		}
		return toReturn;
	}
	
	


	public static List<Point> approximateCircularArc (final Point center, final float radius, final boolean clockwise, final Point from, final Point to, final int resolution) {
		// Calculate the angles we're starting and stopping the approximation at
		final double startAng = findAngleFromHorizontal(from, center);
		final double finishAng = findAngleFromHorizontal(to, center);
		final double deltaAng = findDirectionalAngularDist(startAng, finishAng, clockwise);

		return MiscGeom.approximateCircularArc(center, radius, clockwise, from, deltaAng, resolution);
	}
	
	
	public static List<Point> approximateCircularArc (final Point center, final float radius, final boolean clockwise, final Point from, final double deltaAng, final int resolution) {
		// Calculate the number and size of steps to take during approximation
		final int steps = (int) (deltaAng * resolution / (2*Math.PI));
		if (steps < 1) { return new ArrayList<Point>(0); }
		final double radPerStep = (clockwise ? -1 : 1) * deltaAng/steps;
		
		// Calculate the rotation matrix
		final double cosStepAng = Math.cos(radPerStep);
		final double sinStepAng = Math.sin(radPerStep);
		
		// Compute the approximation of the circular arc
		ArrayList<Point> arc = new ArrayList<Point>(steps);
		double lastX = from.x - center.x;
		double lastY = from.y - center.y;
		for (int i = 0; i < steps; i++) {
			double currentX = lastX*cosStepAng - lastY*sinStepAng;
			double currentY = lastX*sinStepAng + lastY*cosStepAng;
			arc.add(new Point(currentX+center.x, currentY+center.y));
			lastX = currentX;
			lastY = currentY;
		}
		
		return arc;
	}
	
	
	
	
	public static double findAngleFromHorizontal (Point p, Point c) {
		final double angle = Math.atan2(p.y-c.y, p.x-c.x);
		if (angle < 0) {
			return angle + 2*Math.PI;
		} else {
			return angle;
		}
	}
	
	public static double findDirectionalAngularDist (double startAng, double finishAng, boolean clockwise) {
		double angle;
		if (clockwise == false) {
			angle = finishAng-startAng;
			if (finishAng < startAng) {
				angle += 2 * Math.PI;
			}
		} else {
			angle = startAng - finishAng;
			if (finishAng > startAng) {
				angle += 2 * Math.PI;
			}
		}
		
		return angle;
	}

	/**
	 * Calculate the points at which the external bitangents to the two circles intersect the circles, if the external bitangent lines exist.
	 * 
	 * @param c1 The center of circle 1
	 * @param r1 The radius of circle 1
	 * @param c2 The center of circle 2
	 * @param r2 The radius of circle 2
	 * @return null if one circle is contained by another, the points of bitangency if not. Handedness is based on the vector pointing from c1 to c2.
	 * Index 0 is the left handed point on c1, index 1 is the left handed point on c2, index 2 is the right handed point on c1, and index 3 is the
	 * right handed point on c2.
	 */
	public static Point[] calcExternalBitangentPoints (Point c1, double r1, Point c2, double r2) {
		// This function calculates the external bitangent points in four steps:
		// 1) Calculate the unit vector pointing from c1 to c2, c
		// 2) Calculate the sine and cosine terms of the rotation matrix that we'll use to find the unit vectors orthogonal to the tangent lines, C for cosine and S for sine
		// 3) Take the inner product of c and the rotation matrix to find the radial vectors orthogonal to the tangent lines, ra and rb
		// 4) Use ra and rb to calculate the points of tangency we're going to return.
				
		final double distSq = MiscGeom.distSqDbl(c1, c2);
		final double dr = r1-r2;
		
		// Check to make sure the circles have bitangents.
		if (distSq <= dr*dr) return null;
		
		final double d = Math.sqrt(distSq);	// The distance between c1 and c2
		final double c_x = (c2.x - c1.x)/d;			// The x component of the unit vector from c1 to c2
		final double c_y = (c2.y - c1.y)/d;			// The y component of the unit vector from c1 to c2
		final double C = dr/d;							// The cosine of the angle between c and the vector perpendicular to it, r
		final double S = Math.sqrt(1.0 - C*C);	// The sine of the angle between c and the vector perpendicular to it, r
		
		final double ra_x = c_x*C - c_y*S;				// The unit vector orthogonal to the left-handed tangent line
		final double ra_y = c_x*S + c_y*C;
		final double rb_x = c_x*C + c_y*S;				// The unit vector orthogonal to the right-handed tangent line
		final double rb_y = -c_x*S + c_y*C;
		
		Point[] tangentPoints = new Point[4];
		tangentPoints[0] = new Point(c1.x + r1*ra_x, c1.y + r1*ra_y);
		tangentPoints[1] = new Point(c2.x + r2*ra_x, c2.y + r2*ra_y);
		tangentPoints[2] = new Point(c1.x + r1*rb_x, c1.y + r1*rb_y);
		tangentPoints[3] = new Point(c2.x + r2*rb_x, c2.y + r2*rb_y);
		
		return tangentPoints;
	}
	
	
	public static Point[] calcPtCircTangentPts (final Point p, final float r, final Point c) {
		final double d_c = MiscGeom.distance(p, c);
		if (d_c <= r) return null;
		final double d_t = Math.sqrt(d_c*d_c - r*r);
		final double cx = (c.x-p.x)/d_c;
		final double cy = (c.y-p.y)/d_c;
		final double S = d_t/d_c;
		final double C = r/d_c;
		
		final double L_x = r*(cx*C - cy*S);				// The vector orthogonal to the left-handed tangent line
		final double L_y = r*(cx*S + cy*C);
		final double R_x = r*(cx*C + cy*S);				// The vector orthogonal to the right-handed tangent line
		final double R_y = r*(-cx*S + cy*C);
		
		final Point[] tanPts = new Point[2];
		tanPts[0] = new Point(c.x+L_x, c.y+L_y);
		tanPts[1] = new Point(c.x+R_x, c.y+R_y);
		return tanPts;
	}
	
	
	public static Point[] circleSegmentIntersection (Point c, float r, Point T, Point H) {
		
		Point[] intersections = {null, null};
		
		final float dx = H.x - T.x;
		final float dy = H.y - T.y;
		final float Kx = T.x - c.x;
		final float Ky = T.y - c.y;
		
		final float A = dx*dx + dy*dy;
		final float B = 2 * (dx*Kx + dy*Ky);
		final float C = Kx*Kx + Ky*Ky - r*r;
		
		final float inside = B*B - 4*A*C;

		if (A == 0 || inside < 0) {
			intersections[0] = null;
			intersections[1] = null;
		} else if (inside == 0) {
			final float t = -B / (2*A);
			if (t >= 0 && t <= 1) {
				intersections[0] = new Point(T.x + t*dx, T.y + t*dy);
			}
		} else {
			final float sqrtInside = (float) Math.sqrt(inside);
			int addIndex = 0;
			
			final float t1 = (-B + sqrtInside) / (2*A);
			if (t1 >= 0 && t1 <= 1) {
				intersections[addIndex] = new Point(T.x + t1*dx, T.y + t1*dy);
				addIndex++;
			}
			
			final float t2 = (-B - sqrtInside) / (2*A);
			if (t2 >= 0 && t2 <= 1) {
				intersections[addIndex] = new Point(T.x + t2*dx, T.y + t2*dy);
			}
		}
		
		return intersections;
	}
	
	/**
	 * The zero index point is counterclockwise of the one-index point. 
	 */
	public static Point[] circleCircleIntersection (Point c1, float r1, Point c2, float r2) {
		final float d = MiscGeom.distance(c1, c2);
		
		if (d >= r1+r2 || d < Math.abs(r1 - r2) || d == 0) {
			return null;
		}
		
		final float a = (r1*r1 - r2*r2 + d*d) / (2*d);
		final float h = (float) Math.sqrt(r1*r1 - a*a);
		final float px = c1.x + (a/d) * (c2.x - c1.x);		
		final float py = c1.y + (a/d) * (c2.y - c1.y);
		
		Point[] intersections = new Point[2];
		intersections[0] = new Point(px + (h/d)*(c2.y - c1.y), py - (h/d)*(c2.x - c1.x));
		intersections[1] = new Point(px - (h/d)*(c2.y - c1.y), py + (h/d)*(c2.x - c1.x));
		return intersections;
	}
	
	public static boolean checkCircleContainment (Point refP, float refR, Point checkP, float checkR) {
		final float distSq = MiscGeom.distSq(refP, checkP);
		final float dr = checkR - refR;
		
		return distSq <= dr*dr && refR >= checkR;
	}
	
	/**
	 * right handed first
	 */
	public static Point[] calcPerpOffset (Point p, Point ref, float dist) {
		final Point perpVect = new Point((ref.y - p.y), (p.x - ref.x));
		final float scalar1 = dist/(2*(float) Math.hypot(perpVect.x, perpVect.y));

		Point[] points = new Point[2];
		points[0] = new Point(p.x + scalar1*perpVect.x, p.y + scalar1*perpVect.y);
		points[1] = new Point(p.x - scalar1*perpVect.x, p.y - scalar1*perpVect.y);
		
		return points;
	}
	
	
	public static float segDistSquared (Point aT, Point aH, Point bT, Point bH) {
		
		boolean aIsEqual = aT.equals(aH);
		boolean bIsEqual = bT.equals(bH);
		
		if (aIsEqual && bIsEqual) {
			return distSq(aT, bT);
		} else if (bIsEqual) {
			return ptSegDistSq(aT, aH, bT);
		} else if (aIsEqual) {
			return ptSegDistSq(bT, bH, aT);
		}
		
		float denominator = (bH.y-bT.y)*(aH.x-aT.x)-(bH.x-bT.x)*(aH.y-aT.y);

		if (denominator == 0) {
			denominator+= 0.001f;
		}

		float Ta = ((bH.x-bT.x)*(aT.y-bT.y)-(bH.y-bT.y)*(aT.x-bT.x))/denominator;
		float Tb = ((aH.x-aT.x)*(aT.y-bT.y)-(aH.y-aT.y)*(aT.x-bT.x))/denominator;

		if (Ta <= 1 && Ta >= 0 && Tb <= 1 && Tb >= 0) {
			return 0;     //segments intersect
		}

		float lowestDist = Float.MAX_VALUE;
		float tempDist;

		if (Ta<0) {
			tempDist = ptSegDistSq(bT, bH, aT);
			if (tempDist < lowestDist) {
				lowestDist = tempDist;
			}
		} else if (Ta>1) {
			tempDist = ptSegDistSq(bT, bH, aH);
			if (tempDist < lowestDist) {
				lowestDist = tempDist;
			}
		}

		if (Tb < 0) {
			tempDist = ptSegDistSq(aT, aH, bT);
			if (tempDist < lowestDist) {
				lowestDist = tempDist;
			}
		} else if (Tb>1) {
			tempDist = ptSegDistSq(aT, aH, bH);
			if (tempDist < lowestDist) {
				lowestDist = tempDist;
			}
		}

		return lowestDist;
	}

	public static float ptSegDistSq (Point sT, Point sH, Point p) {
		if (sT.equals(sH)) {
			return distSq(sT, p);
		}

		float u = ((p.x-sT.x)*(sH.x-sT.x) + (p.y-sT.y)*(sH.x-sT.y))/distSq(sT, sH);

		if (u < 0) {
			return distSq(sT, p);
		} else if (u > 1) {
			return distSq(sH, p);
		} else {
			Point closePtOnSeg = new Point(sT.x + u*(sH.x-sT.x), sT.y + u*(sH.y-sT.y));
			return distSq(closePtOnSeg, p);
		}
	}
	
	public static RectF calcCapsuleAABB (Point capT, Point capH, float rad) {
		RectF aabb = new RectF();
		
		if (capT.y < capH.y) {
			aabb.top = capT.y - rad;
			aabb.bottom = capH.y + rad;
		} else {
			aabb.top = capH.y - rad;
			aabb.bottom = capT.y + rad;
		}
		
		if (capT.x < capH.x) {
			aabb.left = capT.x - rad;
			aabb.right = capH.x + rad;
		} else {
			aabb.left = capH.x - rad;
			aabb.right = capT.x + rad;
		}
		
		return aabb;
	}
}