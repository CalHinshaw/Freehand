package com.calhounhinshaw.freehandalpha.ink;

import java.util.ArrayList;
import java.util.LinkedList;

public class BooleanPolyGeom {

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
	
	
//	public static Vertex[] buildRawGraph (LinkedList<Point> p1, LinkedList<Point> p2) {
//		ArrayList<Vertex> l1 = new ArrayList<Vertex>();
//		ArrayList<Vertex> l2 = new ArrayList<Vertex>();
//		
//		for (int i = 0; i < p1.size()-1; i++) {
//			for (int j = 0; j < p2.size()-1; j++) {
//				Object[] data = segmentIntersection(p1.get(i+1), p1.get(i), p2.get(j+1), p2.get(j));
//				
//				if (data != null) {
//					Point intersection = (Point) data[0];
//					Float p1Percentage = (Float) data[1];
//					Float p2Percentage = (Float) data[2];
//					
//					if ((p1Percentage > 0 && p1Percentage < 1) || (p2Percentage > 0 && p2Percentage < 1)) {
//						// Normal case, just add the two vertexes to the graph
//						Vertex v1 = new Vertex(intersection, p1Percentage, false, i, p1);
//						Vertex v2 = new Vertex(intersection, p2Percentage, false, j, p2);
//						v1.neighbor = v2;
//						v2.neighbor = v1;
//						
//						l1.add(v1);
//						l2.add(v2);
//					} else {
//						
//						Log.d("PEN", "point-on-point intersection");
//						
//						// This is an end on end intersection and will require more care. Only head/tail or tail/head intersections get added.
//						// In addition to that, point-on-point intersections with colinear segments on each side shouldn't be added.
//						// Also, there should only be two point-on-point intersections in each individual polygon's graph for each point - when
//						// there are more it means a polygon is being double counted on a cutout. 
//						
//						// Starting with the p1 head:
//						if (p1Percentage == 1 && p2Percentage == 0) {
//							Point before1 = p1.get(i);
//							Point after1 = (i+2 == p1.size()) ? p1.get(0) : p1.get(i+2);
//							Point before2 = (j-1 >= 0) ? p2.get(j) : p2.get(p2.size()-1);
//							Point after2 = p2.get(j+1);
//
//							if (TanGeom.cross(before1, p1.get(i+1), before2, p2.get(j)) != 0 || TanGeom.cross(after1, p1.get(i+1), after2, p2.get(j)) != 0) {
//								Vertex v1 = new Vertex(intersection, p1Percentage, true, i, p1);
//								Vertex v2 = new Vertex(intersection, p2Percentage, true, j, p2);
//								v1.neighbor = v2;
//								v2.neighbor = v1;
//								
//								l1.add(v1);
//								l2.add(v2);
//							}
//						} else if (p1Percentage == 0 && p2Percentage == 1) {
//							// p2 is the head here
//							Point before1 = (i-1 >= 0) ? p1.get(i) : p1.get(p1.size()-1);
//							Point after1 = p1.get(i+1);
//							Point before2 = p2.get(j);
//							Point after2 = (j+2 == p2.size()) ? p2.get(0) : p2.get(j+2);
//							
//							if (TanGeom.cross(before1, p1.get(i+1), before2, p2.get(j)) != 0 || TanGeom.cross(after1, p1.get(i+1), after2, p2.get(j)) != 0) {
//								Vertex v1 = new Vertex(intersection, p1Percentage, true, i, p1);
//								Vertex v2 = new Vertex(intersection, p2Percentage, true, j, p2);
//								v1.neighbor = v2;
//								v2.neighbor = v1;
//								
//								l1.add(v1);
//								l2.add(v2);
//							}
//						}
//					}
//				}
//			}
//		}
//		
//		Collections.sort(l2);
//		
//		if (l1.size() > 0 && l2.size() > 0) {
//			Vertex[] toReturn = new Vertex[2];
//			toReturn[0] = linkVerts(l1);
//			toReturn[1] = linkVerts(l2);
//			return toReturn;
//		} else {
//			return null;
//		}
//	}
//	
//	public static Vertex linkVerts (List<Vertex> l) {
//		for (int i = 0; i < l.size()-1; i++) {
//			l.get(i).next = l.get(i+1);
//			l.get(i+1).previous = l.get(i);
//		}
//		l.get(0).previous = l.get(l.size()-1);
//		l.get(l.size()-1).next = l.get(0);
//		
//		return l.get(0);
//	}
//	
//	public static Vertex[] populateGraph (Vertex[] g, LinkedList<Point> p1, LinkedList<Point> p2) {
//		boolean nextIsEntry = !pointInPoly(p1.getFirst(), p2);
//		Vertex current = g[0];
//		do {
//			current.isEntry = nextIsEntry;
//			current = current.next;
//		} while (current != g[0]);
//		
//		return g;
//	}
	
//	public static Vertex[] buildGraph (LinkedList<Point> p1, LinkedList<Point> p2) {
//		Vertex[] graph = populateGraph(buildRawGraph(p1, p2), p1, p2);
//		
//		return null;
//	}
	
	

	
	
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
		if (MiscGeom.intersectionPossible(aH, aT, bH, bT) == false) {
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
	
}