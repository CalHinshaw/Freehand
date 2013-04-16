package com.calhounhinshaw.freehandalpha.ink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import com.calhounhinshaw.freehandalpha.misc.WrapList;

import android.util.Log;

public class BooleanPolyGeom {
	
	public static void perturbPolys (WrapList<Point> p1, WrapList<Point> p2) {
		for (int i = 0; i < p1.size(); i++) {
			for (int j = 0; j < p2.size(); j++) {
				if (pointOnSeg(p1.get(i+1), p2.get(j+1), p2.get(j))) {
					//Log.d("PEN", "Perturbed p1");
					Point newPoint = perturb(p1.get(i+1), p1.get(i), p2.get(j+1), p2.get(j));
					p1.remove(i+1);
					p1.add(i+1, newPoint);
				} else if (pointOnSeg(p2.get(j+1), p1.get(i+1), p1.get(i))) {
					//Log.d("PEN", "Perturbed p2");
					Point newPoint = perturb(p2.get(j+1), p2.get(j), p1.get(i+1), p1.get(i));
					p2.remove(j+1);
					p2.add(j+1, newPoint);
				} else if (pointOnSeg(p1.get(i), p2.get(j+1), p2.get(j))) {
					//Log.d("PEN", "Perturbed p1");
					Point newPoint = perturb(p1.get(i), p1.get(i+1), p2.get(j+1), p2.get(j));
					p1.remove(i);
					p1.add(i, newPoint);
				} else if (pointOnSeg(p2.get(j), p1.get(i+1), p1.get(i))) {
					//Log.d("PEN", "Perturbed p2");
					Point newPoint = perturb(p2.get(j), p2.get(j+1), p1.get(i+1), p1.get(i));
					p2.remove(j);
					p2.add(j, newPoint);
				}
			}
		}
	}
	
	public static Point perturb (Point p1, Point p2, Point l1, Point l2) {
//		if ((l1.x==l2.x && p1.y==p2.y) || (l1.y==l2.y && p1.x==p2.x)) {
//			Log.d("PEN", "both");
//			return new Point (p1.x+2, p1.y+2);
//		} else 
		if (l1.x != l2.x) {
			return new Point (p1.x, p1.y+2);
		} else {
			return new Point (p1.x+2, p1.y);
		}
	}
	
	public static boolean pointOnSeg (Point p, Point s1, Point s2) {
		if (s1.x != s2.x) {
			return (MiscGeom.cross(p, s2, s1, s2) == 0 && isBetween(p.x, s1.x, s2.x));
		} else {
			return (MiscGeom.cross(p, s2, s1, s2) == 0 && isBetween(p.y, s1.y, s2.y));
		}
		
	}
	
	public static boolean isBetween (float check, float b1, float b2) {
		return ((b1<b2 ? b1:b2) <= check && (b1<b2 ? b2:b1) >= check);
	}
	
	
	
	
	
	
	/**
	 * Intersects the two polygons and returns a list of Vertex objects that represent the intersections. COLINEAR SEGMENTS ARE CONSIDERED NON-INTERSECTING.
	 * @param p1 corresponds to all of the fields in the returned Vertexes marked 1.
	 * @param p2 corresponds to all of the fields in the returned Vertexes marked 2.
	 */
	public static WrapList<Vertex> intersectPolys (WrapList<Point> p1, WrapList<Point> p2) {
		WrapList<Vertex> intersections = new WrapList<Vertex>(10);
		
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
			return null;
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

	
	public static void setInOut (WrapList<Vertex> graph, WrapList<Point> p1, WrapList<Point> p2) {
		Collections.sort(graph, new Vertex.p1Comparator());
		boolean currentlyOut = !pointInPoly(p1.get(0), p2);
		
		for (Vertex v : graph) {
			v.poly1Entry = currentlyOut;
			currentlyOut = !currentlyOut;
		}
	}
	
	public static void link (WrapList<Vertex> graph) {
		Collections.sort(graph, new Vertex.p1Comparator());
		for (int i = 0; i < graph.size(); i++) {
			graph.get(i).next1 = graph.get(i+1);
			graph.get(i).prev1 = graph.get(i-1);
		}
		
		Collections.sort(graph, new Vertex.p2Comparator());
		for (int i = 0; i < graph.size(); i++) {
			graph.get(i).next2 = graph.get(i+1);
			graph.get(i).prev2 = graph.get(i-1);
		}
	}
	
	public static WrapList<Vertex> buildPolyGraph (WrapList<Point> p1, WrapList<Point> p2) {
		if (p1.size() < 3 || p2.size() < 3) {
			return new WrapList<Vertex>(1);
		}
		
		perturbPolys(p1, p2);
		WrapList<Vertex> graph = intersectPolys(p1, p2);
		
		if (graph.size() < 2) {
			Log.d("PEN", "graph size < 2");
			return graph;
		}

		setInOut(graph, p1, p2);
		link(graph);
		
		return graph;
	}
	
	public static void resetGraph (WrapList<Vertex> graph) {
		for (Vertex v : graph) {
			v.wasVisited = false;
		}
	}
	
	public static WrapList<Point> union (WrapList<Vertex> graph, WrapList<Point> p1, WrapList<Point> p2) {
		resetGraph(graph);
		
		WrapList<Point> union = new WrapList<Point>(p1.size()+p2.size()+graph.size());
		
		Vertex current = graph.get(0);
		Vertex next = current.getNext(!current.poly1Entry);
		
		Log.d("PEN", "starting union loop");
		while (current.wasVisited == false) {
			
			
			Log.d("PEN", Boolean.toString(current.poly1Entry));
			
			union.add(current.intersection);
			//union.addAll(current.getPoly(onPolyOne).subList(current.getIndex(onPolyOne)+1, next.getIndex(onPolyOne)));
			
			if (current.getPrecedingIndex(!current.poly1Entry) != next.getPrecedingIndex(!current.poly1Entry)) {
				current.getPoly(!current.poly1Entry).addRangeToList(union, current.getPrecedingIndex(!current.poly1Entry)+1, next.getPrecedingIndex(!current.poly1Entry));
			}
			
			
			current.wasVisited = true;
			current = next;
			next = next.getNext(!current.poly1Entry);
		}
		
		
		return union;
	}

	
	
	
	
}