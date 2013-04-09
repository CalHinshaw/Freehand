package com.calhounhinshaw.freehandalpha.ink;

import java.util.ArrayList;

import com.calhounhinshaw.freehandalpha.misc.WrapList;

public class UnitPolyGeom {

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
	private static final int POLY_SIZE = (CIRCLE.length) + 7;
	
	/**
	 * Builds a polygon out of a single inputed segment. The first and last entries in the list are duplicated to make
	 * iteration easier.
	 * 
	 * @param r1 The radius of the first point
	 * @param r2 The radius of the second point
	 * @param c1 The first point
	 * @param c2 The second point.
	 * 
	 * @return The unit poly if one can be constructed, null if not.
	 */
	public static WrapList<Point> buildUnitPoly (float r1, float r2, Point c1, Point c2) {
		float[] angles = UnitPolyGeom.calcBitangentAngles(r1, r2, c1, c2);
		
		if (angles == null) {
			return null;
		}
		
		WrapList<Point> poly = new WrapList<Point>(POLY_SIZE);

		Point lPoint, rPoint;
		float lRad, rRad;
		
		if (c1.x < c2.x) {
			lPoint = c1;
			lRad = r1;
			rPoint = c2;
			rRad = r2;
		} else if (c1.x > c2.x) {
			lPoint = c2;
			lRad = r2;
			rPoint = c1;
			rRad = r1;
		} else {
			if (c1.y > c2.y) {
				lPoint = c1;
				lRad = r1;
				rPoint = c2;
				rRad = r2;
			} else {
				lPoint = c2;
				lRad = r2;
				rPoint = c1;
				rRad = r1;
			}
			
			// angles[0] on left - it has to be on right
			if (Math.cos(angles[0]) < 0) {
				float temp = angles[0];
				angles[0] = angles[1];
				angles[1] = temp;
			}
		}
		
		Point topUnitVect = new Point((float) Math.cos(angles[0]), (float) Math.sin(angles[0]));
		Point botUnitVect = new Point((float) Math.cos(angles[1]), (float) Math.sin(angles[1]));

		// points is clockwise starting with the upper left point
		Point[] points = new Point[4];
		
		points[0] = new Point(lPoint.x+topUnitVect.x*lRad, lPoint.y+topUnitVect.y*lRad);
		points[1] = new Point(rPoint.x+topUnitVect.x*rRad, rPoint.y+topUnitVect.y*rRad);
		points[2] = new Point(rPoint.x+botUnitVect.x*rRad, rPoint.y+botUnitVect.y*rRad);
		points[3] = new Point(lPoint.x+botUnitVect.x*lRad, lPoint.y+botUnitVect.y*lRad);
		
		poly.add(points[0]);
		poly.add(points[1]);
		poly.addAll(UnitPolyGeom.traceCircularPath(rPoint, rRad, true, angles[0], angles[1]));
		poly.add(points[2]);
		poly.add(points[3]);
		poly.addAll(UnitPolyGeom.traceCircularPath(lPoint, lRad, true, angles[1], angles[0]));
		
		return poly;
	}
	
	/**
	 * @return zero index is top angle, one index is bottom angle
	 */
	public static float[] calcBitangentAngles (float r1, float r2, Point c1, Point c2) {
		// Make sure one circle isn't entirely contained by another
		if (MiscGeom.distance(c1, c2) <= Math.abs(r1-r2)) {
			return null;
		}
		
		float offsetAngle;
		float totalAngle;
		
		// The circles don't have an external homothetic center - their tangent line is perpendicular to the line that connects their centers.
		if (r1 == r2) {
			offsetAngle = (float) (Math.atan((c1.y-c2.y)/(c1.x-c2.x)));
			totalAngle = (float) (Math.PI/2);
		} else {
			// Calculate the external homothetic center, h
			float a1 = -r2/(r1-r2);
			float a2 = r1/(r1-r2);
			Point h = new Point(a1*c1.x + a2*c2.x, a1*c1.y + a2*c2.y);
			
			float hDist = MiscGeom.distance(h, c1);
			
			totalAngle = (float) Math.asin(Math.sqrt(hDist*hDist-r1*r1) / hDist);
			offsetAngle = (float) Math.atan((c1.y-h.y)/(c1.x-h.x));			
		}
		
		return new float[] {(offsetAngle+totalAngle), (offsetAngle-totalAngle)};
	}
	
	/**
	 * Approximates a circular path as a poly-line.
	 * 
	 * @param center The center of the circle
	 * @param radius The radius of the circle
	 * @param clockwise The direction around the circle the path is going
	 * @param from The angle the path to be traced starts at
	 * @param to The angle the path to be traced ends at
	 * @return The path
	 */
	public static ArrayList<Point> traceCircularPath (final Point center, final float radius, final boolean clockwise, final float from, final float to) {
		ArrayList<Point> path = new ArrayList<Point>();
		
		// Find the indexes of the points on the circle the path is starting and ending at
		int fromIndex = findAdjacentCircleIndex(from, clockwise);
		int toIndex = findAdjacentCircleIndex(to, !clockwise);
		
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
	public static int findAdjacentCircleIndex (float angle, boolean clockwise) {

		double continuousIndex = angle/STEP_SIZE;

		if (continuousIndex < 0) {
			continuousIndex += 12;
		}
		
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