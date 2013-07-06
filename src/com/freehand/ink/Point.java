package com.freehand.ink;

public final class Point {
	public final float x;
	public final float y;
	
	public Point (float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	public Point (double x, double y) {
		this.x = (float) x;
		this.y = (float) y;
	}
	
	@Override
	public boolean equals (Object o) {
		Point p = (Point) o;
		
		if (this.x == p.x && this.y == p.y) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public String toString () {
		return ("(" + Float.toString(x) + ", " + Float.toString(y) + ")");
	}
}