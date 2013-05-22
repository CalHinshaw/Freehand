package com.freehand.ink;

import java.util.LinkedList;

import com.freehand.misc.WrapList;

public class StrokeGeom {
//
//	/**
//	 * An array of evenly spaced points on the unit circle going counter-clockwise starting at theta == 0;
//	 */
//	public static final Point[] CIRCLE = {
//			new Point((float) Math.cos(0 * Math.PI / 10), (float) Math.sin(0 * Math.PI / 10)),
//			new Point((float) Math.cos(1 * Math.PI / 10), (float) Math.sin(1 * Math.PI / 10)),
//			new Point((float) Math.cos(2 * Math.PI / 10), (float) Math.sin(2 * Math.PI / 10)),
//			new Point((float) Math.cos(3 * Math.PI / 10), (float) Math.sin(3 * Math.PI / 10)),
//			new Point((float) Math.cos(4 * Math.PI / 10), (float) Math.sin(4 * Math.PI / 10)),
//			new Point((float) Math.cos(5 * Math.PI / 10), (float) Math.sin(5 * Math.PI / 10)),
//			new Point((float) Math.cos(6 * Math.PI / 10), (float) Math.sin(6 * Math.PI / 10)),
//			new Point((float) Math.cos(7 * Math.PI / 10), (float) Math.sin(7 * Math.PI / 10)),
//			new Point((float) Math.cos(8 * Math.PI / 10), (float) Math.sin(8 * Math.PI / 10)),
//			new Point((float) Math.cos(9 * Math.PI / 10), (float) Math.sin(9 * Math.PI / 10)),
//			new Point((float) Math.cos(10 * Math.PI / 10), (float) Math.sin(10 * Math.PI / 10)),
//			new Point((float) Math.cos(11 * Math.PI / 10), (float) Math.sin(11 * Math.PI / 10)),
//			new Point((float) Math.cos(12 * Math.PI / 10), (float) Math.sin(12 * Math.PI / 10)),
//			new Point((float) Math.cos(13 * Math.PI / 10), (float) Math.sin(13 * Math.PI / 10)),
//			new Point((float) Math.cos(14 * Math.PI / 10), (float) Math.sin(14 * Math.PI / 10)),
//			new Point((float) Math.cos(15 * Math.PI / 10), (float) Math.sin(15 * Math.PI / 10)),
//			new Point((float) Math.cos(16 * Math.PI / 10), (float) Math.sin(16 * Math.PI / 10)),
//			new Point((float) Math.cos(17 * Math.PI / 10), (float) Math.sin(17 * Math.PI / 10)),
//			new Point((float) Math.cos(18 * Math.PI / 10), (float) Math.sin(18 * Math.PI / 10)),
//			new Point((float) Math.cos(19 * Math.PI / 10), (float) Math.sin(19 * Math.PI / 10)),
//	};
//	private static final double STEP_SIZE = (2 * Math.PI) / CIRCLE.length;
//
//	
//	public static WrapList<Point> getCircularPoly(Point center, float radius) {
//		WrapList<Point> toReturn = new WrapList<Point>(CIRCLE.length);
//		for (int i = CIRCLE.length - 1; i >= 0; i--) {
//			toReturn.add(new Point(CIRCLE[i].x * radius + center.x, CIRCLE[i].y * radius + center.y));
//		}
//		return toReturn;
//	}
//
//
//	/**
//	 * Only guaranteed to work if from and to are within 1 ulp of being on the circle. Will almost definitely work if they're close, though. I think...
//	 */
//	public static LinkedList<Point> traceCircularPath (final Point center, final float radius, final boolean clockwise, final Point from, final Point to) {
//		LinkedList<Point> path = new LinkedList<Point>();
//		
//		// Find the indexes of the points on the circle the path is starting and ending at
//		int fromIndex = findAdjacentCircleIndex(from, center, clockwise);
//		int toIndex = findAdjacentCircleIndex(to, center, !clockwise);
//		
//		if ((fromIndex == toIndex+1 && clockwise == false) || (fromIndex == 0 && toIndex == CIRCLE.length-1 && clockwise == false ) ||
//			(fromIndex == toIndex-1 && clockwise == true) || (fromIndex == CIRCLE.length-1 && toIndex == 0 && clockwise == true)) {
//			return path;
//		}
//		
//		Point[] scaledCircle = new Point[CIRCLE.length];
//		for (int i = 0; i < scaledCircle.length; i++) {
//			scaledCircle[i] = new Point(CIRCLE[i].x * radius + center.x, CIRCLE[i].y * radius + center.y);
//		}
//		
//		int step;
//		if (clockwise == true) {
//			step = -1;
//		} else {
//			step = 1;
//		}
//		
//		int counter = fromIndex;
//		while (true) {
//			path.add(scaledCircle[counter]);
//			
//			if (counter == toIndex) {
//				break;
//			}
//			
//			counter += step;
//			
//			if (counter >= scaledCircle.length) {
//				counter = 0;
//			} else if (counter < 0) {
//				counter = scaledCircle.length - 1;
//			}
//		}
//		
//		return path;
//	}
//	
//	/**
//	 * @param p The point on the circle you're trying to find the adjacent point to
//	 * @param center The center of the circle
//	 * @param clockwise	if true, return the index of the point directly clockwise. if false return the index of the point directly counterclockwise.
//	 * @return The index of the point in circle that's next to p in the direction specified by clockwise or -1 if something went wrong.
//	 */
//	public static int findAdjacentCircleIndex (Point p, Point center, boolean clockwise) {
//		
//		float mag = MiscGeom.distance(p.x, p.y, center.x, center.y);
//		
//		double angle;
//		if (p.y - center.y >= 0) {
//			angle = Math.acos((p.x-center.x)/mag);
//		} else {
//			angle = 2*Math.PI - Math.acos((p.x-center.x)/mag);
//		}
//		double continuousIndex = angle/STEP_SIZE;
//
//		int toReturn;
//		
//		if (clockwise == true) {
//			toReturn = (int) Math.floor(continuousIndex);
//		} else {
//			toReturn = (int) Math.ceil(continuousIndex);
//			if (toReturn >= CIRCLE.length) {
//				toReturn = 0;
//			}
//		}
//
//		return toReturn;
//	}
//
//	/**
//	 * Calculate the points at which the external bitangents to the two circles intersect the circles, if the external bitangent lines exist.
//	 * 
//	 * @param c1 The center of circle 1
//	 * @param r1 The radius of circle 1
//	 * @param c2 The center of circle 2
//	 * @param r2 The radius of circle 2
//	 * @return null if one circle is contained by another, the points of bitangency if not. Handedness is based on the vector pointing from c1 to c2.
//	 * Index 0 is the left handed point on c1, index 1 is the left handed point on c2, index 2 is the right handed point on c1, and index 3 is the
//	 * right handed point on c2.
//	 */
//	public static Point[] calcExternalBitangentPoints (Point c1, float r1, Point c2, float r2) {
//		// This function calculates the external bitangent points in four steps:
//		// 1) Calculate the unit vector pointing from c1 to c2, c
//		// 2) Calculate the sine and cosine terms of the rotation matrix that we'll use to find the unit vectors orthogonal to the tangent lines, C for cosine and S for sine
//		// 3) Take the inner product of c and the rotation matrix to find the radial vectors orthogonal to the tangent lines, ra and rb
//		// 4) Use ra and rb to calculate the points of tangency we're going to return.
//				
//		final float distSq = MiscGeom.distSq(c1, c2);
//		final float dr = r1-r2;
//		
//		// Check to make sure the circles have bitangents.
//		if (distSq <= dr*dr) return null;
//		
//		final float d = (float) Math.sqrt(distSq);	// The distance between c1 and c2
//		final float c_x = (c2.x - c1.x)/d;			// The x component of the unit vector from c1 to c2
//		final float c_y = (c2.y - c1.y)/d;			// The y component of the unit vector from c1 to c2
//		final float C = dr/d;							// The cosine of the angle between c and the vector perpendicular to it, r
//		final float S = (float) Math.sqrt(1.0 - C*C);	// The sine of the angle between c and the vector perpendicular to it, r
//		
//		final float ra_x = c_x*C - c_y*S;				// The unit vector orthogonal to the left-handed tangent line
//		final float ra_y = c_x*S + c_y*C;
//		final float rb_x = c_x*C + c_y*S;				// The unit vector orthogonal to the right-handed tangent line
//		final float rb_y = -c_x*S + c_y*C;
//		
//		Point[] tangentPoints = new Point[4];
//		tangentPoints[0] = new Point(c1.x + r1*ra_x, c1.y + r1*ra_y);
//		tangentPoints[1] = new Point(c2.x + r2*ra_x, c2.y + r2*ra_y);
//		tangentPoints[2] = new Point(c1.x + r1*rb_x, c1.y + r1*rb_y);
//		tangentPoints[3] = new Point(c2.x + r2*rb_x, c2.y + r2*rb_y);
//		
//		return tangentPoints;
//	}
//	
//	
//	
//	
//	
//	
//	public static void startNewStroke (LinkedList<Point> currentPoly, Point[] tangentPoints, Point prevPoint, float prevSize) {
//		currentPoly.addAll(StrokeGeom.traceCircularPath(prevPoint, prevSize, false, tangentPoints[0], tangentPoints[2]));
//		currentPoly.addFirst(tangentPoints[0]);
//		currentPoly.addFirst(tangentPoints[1]);
//		currentPoly.addLast(tangentPoints[2]);
//		currentPoly.addLast(tangentPoints[3]);
//	}
//	
//	
//	public static void standardAddToStroke (LinkedList<Point> currentPoly, Point[] tangentPoints, Point prevPoint, float prevSize) {
//		// Handle the left handed side
//		if (MiscGeom.cross(currentPoly.get(0), currentPoly.get(1), tangentPoints[1], tangentPoints[0]) >= 0) {
//			Point intersection = MiscGeom.calcIntersection(currentPoly.get(0), currentPoly.get(1), tangentPoints[1], tangentPoints[0]);
//			if (intersection == null) {
//				currentPoly.addFirst(tangentPoints[0]);
//				currentPoly.addFirst(tangentPoints[1]);
//			} else {
//				currentPoly.removeFirst();
//				currentPoly.addFirst(intersection);
//				currentPoly.addFirst(tangentPoints[1]);
//			}
//		} else {
//			LinkedList<Point> join = StrokeGeom.traceCircularPath(prevPoint, prevSize, true, currentPoly.getFirst(), tangentPoints[0]);
//			for (Point p : join) {
//				currentPoly.addFirst(p);
//			}
//			currentPoly.addFirst(tangentPoints[0]);
//			currentPoly.addFirst(tangentPoints[1]);
//		}
//		
//		// Handle the right handed side
//		if (MiscGeom.cross(currentPoly.get(currentPoly.size()-1), currentPoly.get(currentPoly.size()-2), tangentPoints[3], tangentPoints[2]) <= 0) {
//			Point intersection = MiscGeom.calcIntersection(currentPoly.get(currentPoly.size()-1), currentPoly.get(currentPoly.size()-2), tangentPoints[3], tangentPoints[2]);
//			if (intersection == null) {
//				currentPoly.addLast(tangentPoints[2]);
//				currentPoly.addLast(tangentPoints[3]);
//			} else {
//				currentPoly.removeLast();
//				currentPoly.addLast(intersection);
//				currentPoly.addLast(tangentPoints[3]);
//			}
//		} else {
//			LinkedList<Point> join = StrokeGeom.traceCircularPath(prevPoint, prevSize, false, currentPoly.getLast(), tangentPoints[2]);
//			for (Point p : join) {
//				currentPoly.addLast(p);
//			}
//			currentPoly.addLast(tangentPoints[2]);
//			currentPoly.addLast(tangentPoints[3]);
//		}
//	}

}