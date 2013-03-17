package com.calhounhinshaw.freehandalpha.note_editor;

public final class Point {
	public final float x;
	public final float y;
	
	public Point (float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	@Override
	public boolean equals (Object o) {
		Point p = (Point) o;
		
		if (Math.abs(p.x - x) <= 0.0001 && Math.abs(p.y - y) <= 0.0001) {
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