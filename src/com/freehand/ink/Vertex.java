package com.freehand.ink;

import java.util.Comparator;
import java.util.List;

/**
 * A Vertex is the node of the embedded planar graph used to perform boolean operations on polygons. The graphs are entirely linked data structures because
 * having an edge list doesn't make sense when there are different categories of edges. Any one Vertex in a graph is connected to every other Vertex and thus
 * defines the entire graph.
 * @author cal
 *
 */
public class Vertex {
	public Point intersection;
	public float distIn1;
	public float distIn2;
	public int precedingIndex1;
	public int precedingIndex2;

	public boolean poly1Entry;
	
	public boolean wasVisited;
	
	public Vertex next1 = null;
	public Vertex next2 = null;
	public Vertex prev1 = null;
	public Vertex prev2 = null;
	
	public List<Point> poly1;
	public List<Point> poly2;
	
	
	public Vertex (Point intersection, float distIn1, float distIn2) {
		this.intersection = intersection;
		this.distIn1 = distIn1;
		this.distIn2 = distIn2;
	}
	
	public Vertex getNext(boolean poly) {
		if (poly == true) {
			return next1;
		} else {
			return next2;
		}
	}
	
	public Vertex getPrevious (boolean poly) {
		if (poly == true) {
			return prev1;
		} else {
			return prev2;
		}
	}
	
	public float getDistIn (boolean poly) {
		if (poly == true) {
			return distIn1;
		} else {
			return distIn2;
		}
	}
	
	public int getPrecedingIndex (boolean poly) {
		if (poly == true) {
			return precedingIndex1;
		} else {
			return precedingIndex2;
		}
	}
	
	public List<Point> getPoly (boolean poly) {
		if (poly == true) {
			return poly1;
		} else {
			return poly2;
		}
	}
	
	public Point getPoint (boolean poly, int offset) {
		if (poly == true) {
			if (offset == 0) {
				return intersection;
			} else if (offset > 0) {
				return poly1.get(precedingIndex1+offset);
			} else {
				return poly1.get(precedingIndex1+offset+1);
			}
		} else {
			if (offset == 0) {
				return intersection;
			} else if (offset > 0) {
				return poly2.get(precedingIndex2+offset);
			} else {
				return poly2.get(precedingIndex2+offset+1);
			}
		}
	}
	
	public boolean getWasEntry (boolean poly) {
		if (poly == true) {
			return poly1Entry;
		} else {
			return !poly1Entry;
		}
	}
	
	public void setWasEntry (boolean poly, boolean wasEntry) {
		if (poly == true) {
			poly1Entry = wasEntry;
		} else {
			poly1Entry = !wasEntry;
		}
	}
	
	
	public static Comparator<Vertex> getComparator (boolean poly) {
		if (poly == true) {
			return new p1Comparator();
		} else {
			return new p2Comparator();
		}
	}
	
	public static class p1Comparator implements Comparator<Vertex> {
		public int compare(Vertex lhs, Vertex rhs) {
			if (lhs.precedingIndex1 < rhs.precedingIndex1) {
				return -1;
			} else if (lhs.precedingIndex1 > rhs.precedingIndex1) {
				return 1;
			} else {
				if (lhs.distIn1 < rhs.distIn1) {
					return -1;
				} else if (lhs.distIn1 > rhs.distIn1) {
					return 1;
				} else {
					return 0;
				}
			}
		}
	}
	
	public static class p2Comparator implements Comparator<Vertex> {
		public int compare(Vertex lhs, Vertex rhs) {
			if (lhs.precedingIndex2 < rhs.precedingIndex2) {
				return -1;
			} else if (lhs.precedingIndex2 > rhs.precedingIndex2) {
				return 1;
			} else {
				if (lhs.distIn2 < rhs.distIn2) {
					return -1;
				} else if (lhs.distIn2 > rhs.distIn2) {
					return 1;
				} else {
					return 0;
				}
			}
		}
	}
}