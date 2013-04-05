package com.calhounhinshaw.freehandalpha.note_editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.graphics.PointF;
import android.util.Log;

public class TanGeom {
	
	public static float distance (float x1, float y1, float x2, float y2) {
		return (float)Math.sqrt(Math.pow((x1-x2), 2) + Math.pow((y1-y2), 2));
	}
	
	public static float distance (Point p1, Point p2) {
		return distance(p1.x, p1.y, p2.x, p2.y);
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
	public static LinkedList<Point> buildUnitPoly (float r1, float r2, Point c1, Point c2) {
		float[] angles = TanGeom.calcBitangentAngles(r1, r2, c1, c2);
		
		if (angles == null) {
			return null;
		}
		
		LinkedList<Point> poly = new LinkedList<Point>();

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
		poly.addAll(TanGeom.traceCircularPath(rPoint, rRad, true, angles[0], angles[1]));
		poly.add(points[2]);
		poly.add(points[3]);
		poly.addAll(TanGeom.traceCircularPath(lPoint, lRad, true, angles[1], angles[0]));
		poly.add(points[0]);
		
		return poly;
	}
	
	
	
	
	
	
	
	
	/**
	 * 
	 * @param r1
	 * @param r2
	 * @param c1
	 * @param c2
	 * @return zero index is top angle, one index is bottom angle
	 */
	public static float[] calcBitangentAngles (float r1, float r2, Point c1, Point c2) {
		// Make sure one circle isn't entirely contained by another
		if (distance(c1, c2) <= Math.abs(r1-r2)) {
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
			
			float hDist = distance(h, c1);
			
			totalAngle = (float) Math.asin(Math.sqrt(hDist*hDist-r1*r1) / hDist);
			offsetAngle = (float) Math.atan((c1.y-h.y)/(c1.x-h.x));			
		}
		
		return new float[] {(offsetAngle+totalAngle), (offsetAngle-totalAngle)};
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
	 * Approximates a circular path as a poly-line.
	 * 
	 * @param center The center of the circle
	 * @param radius The radius of the circle
	 * @param clockwise The direction around the circle the path is going
	 * @param from The angle the path to be traced starts at
	 * @param to The angle the path to be traced ends at
	 * @return The path
	 */
	public static LinkedList<Point> traceCircularPath (final Point center, final float radius, final boolean clockwise, final float from, final float to) {
		LinkedList<Point> path = new LinkedList<Point>();
		
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
	
	/**
	 * Intersects the two polygons and returns a list of Vertex objects that represent the intersections. COLINEAR SEGMENTS ARE CONSIDERED NON-INTERSECTING.
	 * @param p1 corresponds to all of the fields in the returned Vertexes marked 1.
	 * @param p2 corresponds to all of the fields in the returned Vertexes marked 2.
	 */
	public static ArrayList<Vertex> intersectPolys (LinkedList<Point> p1, LinkedList<Point> p2) {
		ArrayList<Vertex> intersections = new ArrayList<Vertex>(10);
		
		for (int i = 0; i < p1.size()-1; i++) {
			for (int j = 0; j < p2.size()-1; j++) {
				Vertex v = segmentIntersection(p1.get(i+1), p1.get(i), p2.get(j+1), p2.get(j));
				
				if (v != null) {
					v.precedingIndex1 = i;
					v.precedingIndex2 = j;
					intersections.add(v);
				}
			}
		}
		
		return intersections;
	}
	
	
	public static Vertex[] buildRawGraph (LinkedList<Point> p1, LinkedList<Point> p2) {
		ArrayList<Vertex> l1 = new ArrayList<Vertex>();
		ArrayList<Vertex> l2 = new ArrayList<Vertex>();
		
		for (int i = 0; i < p1.size()-1; i++) {
			for (int j = 0; j < p2.size()-1; j++) {
				Object[] data = segmentIntersection(p1.get(i+1), p1.get(i), p2.get(j+1), p2.get(j));
				
				if (data != null) {
					Point intersection = (Point) data[0];
					Float p1Percentage = (Float) data[1];
					Float p2Percentage = (Float) data[2];
					
					if ((p1Percentage > 0 && p1Percentage < 1) || (p2Percentage > 0 && p2Percentage < 1)) {
						// Normal case, just add the two vertexes to the graph
						Vertex v1 = new Vertex(intersection, p1Percentage, false, i, p1);
						Vertex v2 = new Vertex(intersection, p2Percentage, false, j, p2);
						v1.neighbor = v2;
						v2.neighbor = v1;
						
						l1.add(v1);
						l2.add(v2);
					} else {
						
						Log.d("PEN", "point-on-point intersection");
						
						// This is an end on end intersection and will require more care. Only head/tail or tail/head intersections get added.
						// In addition to that, point-on-point intersections with colinear segments on each side shouldn't be added.
						// Also, there should only be two point-on-point intersections in each individual polygon's graph for each point - when
						// there are more it means a polygon is being double counted on a cutout. 
						
						// Starting with the p1 head:
						if (p1Percentage == 1 && p2Percentage == 0) {
							Point before1 = p1.get(i);
							Point after1 = (i+2 == p1.size()) ? p1.get(0) : p1.get(i+2);
							Point before2 = (j-1 >= 0) ? p2.get(j) : p2.get(p2.size()-1);
							Point after2 = p2.get(j+1);

							if (TanGeom.cross(before1, p1.get(i+1), before2, p2.get(j)) != 0 || TanGeom.cross(after1, p1.get(i+1), after2, p2.get(j)) != 0) {
								Vertex v1 = new Vertex(intersection, p1Percentage, true, i, p1);
								Vertex v2 = new Vertex(intersection, p2Percentage, true, j, p2);
								v1.neighbor = v2;
								v2.neighbor = v1;
								
								l1.add(v1);
								l2.add(v2);
							}
						} else if (p1Percentage == 0 && p2Percentage == 1) {
							// p2 is the head here
							Point before1 = (i-1 >= 0) ? p1.get(i) : p1.get(p1.size()-1);
							Point after1 = p1.get(i+1);
							Point before2 = p2.get(j);
							Point after2 = (j+2 == p2.size()) ? p2.get(0) : p2.get(j+2);
							
							if (TanGeom.cross(before1, p1.get(i+1), before2, p2.get(j)) != 0 || TanGeom.cross(after1, p1.get(i+1), after2, p2.get(j)) != 0) {
								Vertex v1 = new Vertex(intersection, p1Percentage, true, i, p1);
								Vertex v2 = new Vertex(intersection, p2Percentage, true, j, p2);
								v1.neighbor = v2;
								v2.neighbor = v1;
								
								l1.add(v1);
								l2.add(v2);
							}
						}
					}
				}
			}
		}
		
		Collections.sort(l2);
		
		if (l1.size() > 0 && l2.size() > 0) {
			Vertex[] toReturn = new Vertex[2];
			toReturn[0] = linkVerts(l1);
			toReturn[1] = linkVerts(l2);
			return toReturn;
		} else {
			return null;
		}
	}
	
	public static Vertex linkVerts (List<Vertex> l) {
		for (int i = 0; i < l.size()-1; i++) {
			l.get(i).next = l.get(i+1);
			l.get(i+1).previous = l.get(i);
		}
		l.get(0).previous = l.get(l.size()-1);
		l.get(l.size()-1).next = l.get(0);
		
		return l.get(0);
	}
	
	public static Vertex[] populateGraph (Vertex[] g, LinkedList<Point> p1, LinkedList<Point> p2) {
		boolean nextIsEntry = !pointInPoly(p1.getFirst(), p2);
		Vertex current = g[0];
		do {
			current.isEntry = nextIsEntry;
			current = current.next;
		} while (current != g[0]);
		
		return g;
	}
	
	public static Vertex[] buildGraph (LinkedList<Point> p1, LinkedList<Point> p2) {
		Vertex[] graph = populateGraph(buildRawGraph(p1, p2), p1, p2);
		
		return null;
	}
	
	
	private static boolean pointInPoly (Point point, LinkedList<Point> poly) {
		// -1 if not valid, else 0 if below and 1 if above
		int ptState = -1;
		
		int intersections = 0;
		
		for (Point selPt : poly) {
			if (selPt.x < point.x) {
				ptState = -1;
			} else {
				if (ptState == -1) {
					if (selPt.y >= point.y) {
						ptState = 1;
					} else {
						ptState = 0;
					}
				} else if ((selPt.y >= point.y) && ptState == 0) {
					intersections++;
					ptState = 1;
				} else if ((selPt.y < point.y) && ptState == 1) {
					intersections++;
					ptState = 0;
				}
				
			}
		}
		
		if (poly.get(0).x < point.x) {
			ptState = -1;
		} else {
			if (ptState == -1) {
				if (poly.get(0).y >= point.y) {
					ptState = 1;
				} else {
					ptState = 0;
				}
			} else if ((poly.get(0).y >= point.y) && ptState == 0) {
				intersections++;
				ptState = 1;
			} else if ((poly.get(0).y < point.y) && ptState == 1) {
				intersections++;
				ptState = 0;
			}
		}
		
		if (intersections >=1 && intersections%2 == 1) {
			return true;
		} else {
			return false;
		}
	}
	
	
	public static LinkedList<Point> polygonUnion (Vertex[] graph) {
		
		
		
		return null;
	}
	
	
	/**
	 * Calculates the intersection of the two line segments. The line segments are considered closed. Considers coincident lines to not intersect (returns null).
	 * 
	 * @param aH The head of the first vector
	 * @param aT The tail of the first vector
	 * @param bH the head of the second vector
	 * @param bT the tail of the second vector
	 * @return If the segments don't intersect, null. If they do an Object array of length three: index 0 is the point of intersection, index 1 is the percentage
	 * into the a segment the intersection took place at, and index 2 is the percentage into segment b the intersection took place at. The percentages are represented
	 * as Float object that range from 0 to 1.
	 */
	public static Vertex segmentIntersection (Point aH, Point aT, Point bH, Point bT) {
		
		if (intersectionPossible(aH, aT, bH, bT) == false) {
			return null;
		}
		
		float denominator = (bT.y-bH.y)*(aT.x-aH.x)-(bT.x-bH.x)*(aT.y-aH.y);

		// Close enough to parallel that we might as well just call them parallel.
		if (Math.abs(denominator) < 0.000000000001) {
			return null;
		}

		// Note: I tried adding the divisions into the if statement but that actually slowed the benchmarks down. I think it might be a branch prediction
		// issue. It's been a while (and a lot of other changes) since I ran the benchmark, I should probably test it again if there's a performance problem.
		float Ta = ((bT.x-bH.x)*(aH.y-bH.y)-(bT.y-bH.y)*(aH.x-bH.x))/denominator;
		float Tb = ((aT.x-aH.x)*(aH.y-bH.y)-(aT.y-aH.y)*(aH.x-bH.x))/denominator;

		if (Ta <= 1 && Ta >= 0 && Tb <= 1 && Tb >= 0) {
			Point intersection = new Point(aH.x + Ta*(aT.x - aH.x), aH.y + Ta*(aT.y - aH.y));
			return new Vertex(intersection, Ta, Tb);
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
	
	
}