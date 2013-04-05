package com.calhounhinshaw.freehandalpha.ink;


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
	public Vertex previous1 = null;
	public Vertex previous2 = null;
	
	
	public Vertex (Point intersection, float distIn1, float distIn2) {
		this.intersection = intersection;
		this.distIn1 = distIn1;
		this.distIn2 = distIn2;
	}
}