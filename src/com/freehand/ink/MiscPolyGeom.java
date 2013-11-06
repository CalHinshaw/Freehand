package com.freehand.ink;

import java.util.ArrayList;
import java.util.List;
import android.graphics.RectF;
import com.freehand.misc.Util;

public class MiscPolyGeom {
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
	public static List<Point> buildUnitPoly (Point c1, float r1, Point c2, float r2) {
		Point[] tanPts = MiscGeom.calcExternalBitangentPoints(c1, r1, c2, r2);
		
		if (tanPts == null) {
			if(r1 >= r2) {
				return MiscGeom.getWrapCircularPoly(c1, r1);
			} else {
				return MiscGeom.getWrapCircularPoly(c2, r2);
			}
		}
		
		List<Point> poly = new ArrayList<Point>();
		
		poly.add(tanPts[0]);
		poly.add(tanPts[1]);
		poly.addAll(MiscGeom.approximateCircularArc(c2, r2, true, tanPts[1], tanPts[3], 20));
		poly.add(tanPts[3]);
		poly.add(tanPts[2]);
		poly.addAll(MiscGeom.approximateCircularArc(c1, r1, true, tanPts[2], tanPts[0], 20));

		return poly;
	}
	
	/**
	 * Calculate the winding number of a point in a polygon.
	 * @return the winding number if the point isn't on the poly, Integer.MIN_VALUE if it is.
	 */
	public static int calcWindingNum (Point point, List<Point> poly) {
		int windingNum = 0;    // the  winding number counter

		for (int i=0; i < poly.size(); i++) {
			if (poly.get(i).y <= point.y && poly.get(Util.pWrap(poly, i+1)).y > point.y) {
				// The current polygon edge is crossing the horizontal line y = point.y going up in this branch.
				// The equalities in the conditional are important to ensure that intersections, even directly on
				// vertices and horizontal edges, are counted once and only once.
				
				// While we already know there is an upwards intersection with the horizontal line, we need to know
				// if the edge intersects the horizontal ray that extends to the right of point, or if point lies
				// directly on the line.
				float handedness = MiscGeom.cross(poly.get(Util.pWrap(poly, i+1)), poly.get(i), point, poly.get(i));
				if (handedness > 0) {
					windingNum++;
				} else if (handedness == 0) {
					return Integer.MIN_VALUE;
				}
			} else if (poly.get(i).y > point.y && poly.get(Util.pWrap(poly, i+1)).y <= point.y) {
				// See comments above (but flip the equalities)
				float handedness = MiscGeom.cross(poly.get(Util.pWrap(poly, i+1)), poly.get(i), point, poly.get(i));
				if (handedness < 0) {
					windingNum--;
				} else if (handedness == 0) {
					return Integer.MIN_VALUE;
				}
			}
		}

		return windingNum;
	}
	
	public static boolean nzPointInPoly (Point point, List<Point> poly) {
		return calcWindingNum(point, poly) != 0;
	}
	
	public static boolean jctPointInPoly (Point point, List<Point> poly) {
		return calcWindingNum(point, poly)%2 != 0;
	}
	
	public static RectF calcAABoundingBox (List<Point> points) {
		RectF box = new RectF(points.get(0).x, points.get(0).y, points.get(0).x, points.get(0).y);
		
		for (Point p : points) {
			if (p.x < box.left) {
				box.left = p.x;
			} else if (p.x > box.right) {
				box.right = p.x;
			}
			
			if (p.y < box.top) {
				box.top = p.y;
			} else if (p.y > box.bottom) {
				box.bottom = p.y;
			}
		}
		
		return box;
	}
	
	public static boolean checkPolyIntersection (List<Point> p1, List<Point> p2) {
		if (nzPointInPoly(p1.get(0), p2) == true) {
			return true;
		}
		
		if (nzPointInPoly(p2.get(0), p1)) {
			return true;
		}
		
		for (int i = 0; i < p1.size(); i++) {
			for (int j = 0; j < p2.size(); j++) {
				if (MiscGeom.calcIntersection(p1.get(Util.pWrap(p1, i+1)), p1.get(i), p2.get(Util.pWrap(p2, j+1)), p2.get(j)) != null) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	public static boolean checkCapsulePolyIntersection (List<Point> poly, Point sT, Point sH, float rad) {
		// Check to see if the capsule is inside of the poly
		if (nzPointInPoly(sT, poly) == true) {
			return true;
		}
		
		// Check to see if any of the edges of poly are within the radius of the capsule
		float radSq = rad*rad;
		for (int i = 0; i < poly.size(); i++) {
			if (MiscGeom.segDistSquared(sT, sH, poly.get(i), poly.get(Util.pWrap(poly, i+1))) <= radSq) {
				return true;
			}
		}
		
		return false;
	}
}