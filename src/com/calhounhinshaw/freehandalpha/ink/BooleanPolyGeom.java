package com.calhounhinshaw.freehandalpha.ink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import com.calhounhinshaw.freehandalpha.misc.WrapList;

import android.util.Log;

public class BooleanPolyGeom {
	/**
	 * Intersects the two polygons and returns a list of Vertex objects that represent the intersections. COLINEAR SEGMENTS ARE CONSIDERED NON-INTERSECTING.
	 * @param p1 corresponds to all of the fields in the returned Vertexes marked 1.
	 * @param p2 corresponds to all of the fields in the returned Vertexes marked 2.
	 */
	public static ArrayList<Vertex> intersectPolys (WrapList<Point> p1, WrapList<Point> p2) {
		ArrayList<Vertex> intersections = new ArrayList<Vertex>(10);
		
		for (int i = 0; i < p1.size(); i++) {
			for (int j = 0; j < p2.size(); j++) {
				Vertex v = segmentIntersection(p1.get(i+1), p1.get(i), p2.get(j+1), p2.get(j));
				
				if (v != null) {
					v.poly1 = p1;
					v.precedingIndex1 = i;
					v.poly2 = p2;
					v.precedingIndex2 = j;
					intersections.add(v);
				}
			}
		}
		
		return intersections;
	}
	
	/**
	 * Calculates the intersection of the two line segments. The line segments are considered closed. Considers coincident lines 
	 * to not intersect even if they only share endpoints.
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
		if (MiscGeom.intersectionPossible(aH, aT, bH, bT) == false) {
			return null;
		}
		
		float denominator = (bH.y-bT.y)*(aH.x-aT.x)-(bH.x-bT.x)*(aH.y-aT.y);

		// Check for endpoint-on-endpoint intersections. If there aren't any return null.
		if (denominator == 0) {
			if (aH.equals(bH)) {
				if ((aH.x > aT.x && bH.x < bT.x) || (aH.x < aT.x && bH.x > bT.x) || (aH.y > aT.y && bH.y < bT.y) || (aH.y < aT.y && bH.y > bT.y)) {
					return new Vertex(aH, 1, 1);
				}
			} else if (aH.equals(bT)) {
				if ((aH.x > aT.x && bH.x > bT.x) || (aH.x < aT.x && bH.x < bT.x) || (aH.y > aT.y && bH.y > bT.y) || (aH.y < aT.y && bH.y < bT.y)) {
					return new Vertex(aH, 1, 0);
				}
			} else if (aT.equals(bH)) {
				if ((aH.x < aT.x && bH.x < bT.x) || (aH.x > aT.x && bH.x > bT.x) || (aH.y < aT.y && bH.y < bT.y) || (aH.y > aT.y && bH.y > bT.y)) {
					return new Vertex(aH, 0, 1);
				}
			} else if (aT.equals(bT)) {
				if ((aH.x > aT.x && bH.x < bT.x) || (aH.x < aT.x && bH.x > bT.x) || (aH.y > aT.y && bH.y < bT.y) || (aH.y < aT.y && bH.y > bT.y)) {
					return new Vertex(aH, 0, 0);
				}
			} else {
				return null;
			}
		}

		// Note: I tried adding the divisions into the if statement but that actually slowed the benchmarks down. I think it might be a branch prediction
		// issue. It's been a while (and a lot of other changes) since I ran the benchmark, I should probably test it again if there's a performance problem.
		float Ta = ((bH.x-bT.x)*(aT.y-bT.y)-(bH.y-bT.y)*(aT.x-bT.x))/denominator;
		float Tb = ((aH.x-aT.x)*(aT.y-bT.y)-(aH.y-aT.y)*(aT.x-bT.x))/denominator;

		if (Ta <= 1 && Ta >= 0 && Tb <= 1 && Tb >= 0) {
			Point intersection = new Point(aT.x + Ta*(aH.x - aT.x), aT.y + Ta*(aH.y - aT.y));
			return new Vertex(intersection, Ta, Tb);
		} else {
			return null;
		}
	}
	
	/**
	 * UNTESTED, DON'T TRUST! (it's from the prototype ink when I was using polylines and PointF)
	 * Is supposed to be closed.
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
					} else if (poly.get(i).y < point.y){
						ptState = 0;
					}
				} else if ((poly.get(i).y >= point.y) && ptState == 0) {
					intersections++;
					ptState = 1;
				} else if ((poly.get(i).y <= point.y) && ptState == 1) {
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

	public static void removeSimilarEndToEndVertices (ArrayList<Vertex> graph) {
		Iterator<Vertex> iter = graph.iterator();
		while (iter.hasNext()) {
			Vertex v = iter.next();
			if ((v.distIn1 == 0 && v.distIn2 == 0) || (v.distIn1 == 1 && v.distIn2 == 1)) {
				iter.remove();
			}
		}
	}
	
	public static void removeInternalVertsAndSetInOut (ArrayList<Vertex> graph, boolean poly) {
		if (graph.size() < 2) {
			return;
		}
		
		Collections.sort(graph, Vertex.getComparator(poly));
		Iterator<Vertex> iter = graph.iterator();
		
		boolean currentlyOut = !pointInPoly(graph.get(0).getPoly(poly).get(0), graph.get(0).getPoly(!poly));
		while (iter.hasNext()) {
			Vertex v = iter.next();
			
			if (currentlyOut == true) {
				v.setWasEntry(poly, true);
				currentlyOut = false;
			} else {
				if (v.getDistIn(poly) > 0 && v.getDistIn(poly) < 1 && v.getDistIn(!poly) > 0 && v.getDistIn(!poly) < 1) {				// Line-on-line intersection, has to go out
					v.setWasEntry(poly, false);
					currentlyOut = true;
				} else if (v.getDistIn(poly) == 1) {														// Head-on-something intersection, delete Vertex
					iter.remove();
					continue;
				} else if (v.getDistIn(!poly) > 0 && v.getDistIn(!poly) < 1) {										// Tail-on-segment intersection, need single cross to test
					boolean goesOut = MiscGeom.cross(v.getPoint(!poly, 1), v.getPoint(!poly, -1), v.getPoint(poly, 1), v.getPoint(poly, 0)) > 0;
					if (goesOut == true) {
						v.setWasEntry(poly, false);
						currentlyOut = true;
					} else {
						iter.remove();
						continue;
					}
				} else {																			// tail-on-point, need two crosses to test
					boolean segBeforeGoesOut = MiscGeom.cross(v.getPoint(!poly, 1), v.getPoint(!poly, -1), v.getPoint(poly, 1), v.getPoint(poly, 0)) > 0;
					boolean segAfterGoesOut = MiscGeom.cross(v.getPoint(!poly, -1), v.getPoint(!poly, -2), v.getPoint(poly, 2), v.getPoint(poly, 1)) > 0;
					if (segBeforeGoesOut == true || segAfterGoesOut == true) {
						v.setWasEntry(poly, false);
						currentlyOut = true;
					} else {
						iter.remove();
						continue;
					}
				}
			}
		}
	}
	
	public static ArrayList<Vertex> buildPolyGraph (WrapList<Point> p1, WrapList<Point> p2) {
		if (p1.size() < 3 || p2.size() < 3) {
			return new ArrayList<Vertex>(1);
		}
		
		ArrayList<Vertex> graph = intersectPolys(p1, p2);
		removeSimilarEndToEndVertices(graph);

		removeInternalVertsAndSetInOut(graph, true);
		removeInternalVertsAndSetInOut(graph, false);
		
		
		return graph;
	}
	
	

	
	
	
	
}