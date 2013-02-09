package com.calhounhinshaw.freehandalpha.note_editor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.Log;

class NoteEditorPresenter {
	private NoteView mNoteView;
	
	private LinkedList<Stroke> mStrokes = new LinkedList<Stroke>();
	
	// Hold the current stroke's raw data
	private LinkedList<Point> rawPoints = new LinkedList<Point>();
	private LinkedList<Float> rawPressure = new LinkedList<Float>();
	
	// Holds the current stroke's data for drawing
	private LinkedList<Point> currentPolygon = new LinkedList<Point>();
	private Path currentPath = new Path();
	private Paint currentPaint = new Paint();
	
	// The zoom scalar that all values going into and out of the note are multiplied by after translation by windowX and windowY
	private float zoomMultiplier = 1;
	
	// The x and y values of the upper left corner of the screen relative to the note data
	private float windowX = 0;
	private float windowY = 0;
	
	private int penColor = 0xff000000;
	private float penSize = 6.5f;
	
	private LinkedList<Point> debugDots = new LinkedList<Point>();
	private Paint debugPaint = new Paint(Color.RED);
	
	
	public NoteEditorPresenter () {
		currentPaint.setColor(penColor);
		currentPaint.setStyle(Paint.Style.STROKE);
		currentPaint.setStrokeWidth(1);
		currentPaint.setAntiAlias(true);
	}
	
	
	public void setNoteView (NoteView newNoteView) {
		mNoteView = newNoteView;
	}
	
	public void setPen (int newColor, float newSize) {
		penColor = newColor;
		penSize = newSize;
	}
	
	public void setEraser () {
		mNoteView.onErase();
	}
	
	public void setSelector () {
		mNoteView.onSelect();
	}
	
	public void undo () {
		mNoteView.onUndo();
	}
	
	public void redo () {
		mNoteView.onRedo();
	}
	
	
	public void penAction (List<Long> times, List<Float> xs, List<Float> ys, List<Float> pressures) {
		if (pressures.get(0) == -1) {
			if (currentPolygon.size() >= 3) {
				mStrokes.add(new Stroke(penColor, new ArrayList<Point>(currentPolygon)));
			}
			
			currentPolygon.clear();
			
			rawPoints.clear();
			rawPressure.clear();
		} else {
			for (int i = 0; i < times.size(); i++) {
				rawPoints.addLast(new Point(xs.get(i), ys.get(i)));
				rawPressure.add(pressures.get(i));
			}
			
			if (rawPoints.size() >= 2) {
				currentPolygon.clear();
				
				Point oldPoint = rawPoints.get(0);
				Float oldPressure = rawPressure.get(0);
				
				for (int i = 1; i < rawPoints.size(); i++) {					
					Point[] toAdd = getLines(penSize*oldPressure, penSize*rawPressure.get(i), oldPoint, rawPoints.get(i));
					
					if (toAdd != null) {
						currentPolygon.addFirst(toAdd[1]);
						currentPolygon.addFirst(toAdd[0]);
						currentPolygon.addLast(toAdd[2]);
						currentPolygon.addLast(toAdd[3]);
						
						oldPoint = rawPoints.get(i);
						oldPressure = rawPressure.get(i);
					}
				}
			}
		}
	}
	
	/**
	 * Calculates the two line segments that represent the line segment passed using p1 and p2 in the polygon.
	 * 
	 * @param w1 the width of the stroke at p1
	 * @param w2 the width of the stroke at p2
	 * @param p1 the first point of the line segment
	 * @param p2 the second point of the line segment
	 * 
	 * @return a 4 element array of points - the first two are the left-handed line (think Greene's theorem). If the points are equal returns null.
	 */
	private Point[] getLines (float w1, float w2, Point p1, Point p2) {
		
		// if the pen didn't move return null
		if (p1.x == p2.x && p1.y == p2.y) {
			return null;
		}
		
		// A vector perpendicular to the segment
		Point perpVect = new Point((p2.y - p1.y), (p1.x - p2.x));
		
		// The magnitude of perpVect
		float magnitude = (float) Math.hypot(perpVect.x, perpVect.y);
		
		// The amount to scale perpVect by to get the points on either side of p1
		float scalar1 = w1/(2*magnitude);
		
		// The points on either side of p1
		Point p1a = new Point(p1.x + scalar1*perpVect.x, p1.y + scalar1*perpVect.y);
		Point p1b = new Point(p1.x - scalar1*perpVect.x, p1.y - scalar1*perpVect.y);
		
		// The amount to scale perpVect to get the points on either side of p2
		float scalar2 = w2/(2*magnitude);

		// The points on either side of p2
		Point p2a = new Point(p2.x + scalar2*perpVect.x, p2.y + scalar2*perpVect.y);
		Point p2b = new Point(p2.x - scalar2*perpVect.x, p2.y - scalar2*perpVect.y);
		
		// Put the left handed points first in the array
		Point[] lines = new Point[4];
		int aOnLeft = 0;
		if (p1.y < p2.y) {				// Up
			if (p1a.x < p1.x) {			// a on left
				aOnLeft = 1;
			} else {					// b on left
				aOnLeft = -1;
			}
		} else if (p1.y > p2.y) {		// Down
			if (p1a.x > p1.x) {			// a on left
				aOnLeft = 1;
			} else {					// b on left
				aOnLeft = -1;
			}
		} else if (p1.x < p2.x) {		// Straight right
			if (p1a.y > p1.y) {
				aOnLeft = 1;
			} else {					// b on left
				aOnLeft = -1;
			}
		} else if (p1.x > p2.x) {		// Straight left
			if (p1a.y < p1.y) {
				aOnLeft = 1;
			} else {					// b on left
				aOnLeft = -1;
			}
		}
		
		if (aOnLeft > 0) {			// a on left
			lines[0] = p1a;
			lines[1] = p2a;
			lines[2] = p1b;
			lines[3] = p2b;
		} else if (aOnLeft < 0){					// b on left
			lines[0] = p1b;
			lines[1] = p2b;
			lines[2] = p1a;
			lines[3] = p2a;
		} else {
			Log.d("PEN", "Couldn't determine which segment was on the left handed side");
			return null;
		}

		return lines;
	}
	
	
	private boolean segmentIntersectionTest (Point a, Point b, Point c, Point d) {
		
		// Exclusion criteria
		boolean aHigher = (a.y < b.y);
		boolean aLefter = (a.x < b.x);
		boolean cHigher = (c.y < d.y);
		boolean cLefter = (c.x < d.x);
		
		if (  !(  ( (aHigher ? a.y : b.y) <= (cHigher ? d.y : c.y) ) && ( (aHigher ? b.y : a.y) >= (cHigher ? c.y : d.y) )  )  ) {
			return false;
		}
		
		if (  !(  ( (cLefter ? c.x : d.x) <= (aLefter ? b.x : a.x) ) && ( (cLefter ? d.x : c.x) >= (aLefter ? a.x : b.x) )  )  ) {
			return false;
		}
		
		// Test using floating point calculations
		float denominator = (d.y-c.y)*(b.x-a.x)-(d.x-c.x)*(b.y-a.y);

		if (denominator == 0) {
			denominator+= 0.001f;
		}

		float Ta = ((d.x-c.x)*(a.y-c.y)-(d.y-c.y)*(a.x-c.x))/denominator;
		float Tb = ((b.x-a.x)*(a.y-c.y)-(b.y-a.y)*(a.x-c.x))/denominator;

		if (Ta <= 1 && Ta >= 0 && Tb <= 1 && Tb >= 0) {
			return true;
		} else {
			return false;
		}
	}
	
	
	public void panZoomAction (float startX, float startY, float dx, float dy, float dZoom) {
		//zoomMultiplier *= dZoom;
		
	}
	
	public void drawNote (Canvas c) {
		c.drawColor(0xffffffff);
		
		for (Stroke s : mStrokes) {
			s.draw(c);
		}
		
		if (currentPolygon.size() >= 3) {
			currentPaint.setColor(penColor);
			currentPath.reset();
			
			currentPath.moveTo(currentPolygon.get(0).x, currentPolygon.get(0).y);
			for (int i = 1; i < currentPolygon.size(); i++) {
				currentPath.lineTo(currentPolygon.get(i).x, currentPolygon.get(i).y);
			}
			currentPath.lineTo(currentPolygon.get(0).x, currentPolygon.get(0).y);
			
			c.drawPath(currentPath, currentPaint);
		}
		
		for (Point p : debugDots) {
			c.drawCircle(p.x, p.y, 1, debugPaint);
		}
	}
	
}