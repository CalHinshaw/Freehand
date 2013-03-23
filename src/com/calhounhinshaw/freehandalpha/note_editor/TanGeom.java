package com.calhounhinshaw.freehandalpha.note_editor;

import android.util.Log;

public class TanGeom {
	
	
	public static Point[] calcExternalBitangents (float r1, float r2, Point c1, Point c2) {
		// Make sure one circle isn't entirely contained by another
		if (Geometry.distance(c1, c2) <= Math.abs(r1-r2)) {
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
			
			float hDist = Geometry.distance(h, c1);
			
			totalAngle = (float) Math.asin(Math.sqrt(hDist*hDist-r1*r1) / hDist);
			offsetAngle = (float) Math.atan((c1.y-h.y)/(c1.x-h.x));			
		}
		
		Point posUnitVect = new Point((float) Math.cos(offsetAngle + totalAngle), (float) Math.sin(offsetAngle + totalAngle));
		Point negUnitVect = new Point((float) Math.cos(offsetAngle - totalAngle), (float) Math.sin(offsetAngle - totalAngle));
		
		Point[] toReturn = new Point[4];
		toReturn[0] = new Point(c1.x+posUnitVect.x*r1, c1.y+posUnitVect.y*r1);
		toReturn[1] = new Point(c2.x+posUnitVect.x*r2, c2.y+posUnitVect.y*r2);
		toReturn[2] = new Point(c1.x+negUnitVect.x*r1, c1.y+negUnitVect.y*r1);
		toReturn[3] = new Point(c2.x+negUnitVect.x*r2, c2.y+negUnitVect.y*r2);
		
		return toReturn;
		
		
	}
	
}