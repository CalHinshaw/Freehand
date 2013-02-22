package com.calhounhinshaw.freehandalpha.note_editor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.calhounhinshaw.freehandalpha.note_orginazion.INoteHierarchyItem;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
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
	
	private Matrix transformMatrix = new Matrix();
	
	private float[] matVals = {1, 0, 0, 0, 1, 0, 0, 0, 1};
	
	// The transformation matrix transforms the stuff drawn to the canvas as if (0, 0) is the upper left hand corner of the screen,
	// not the View the canvas is drawing to.
	private float canvasYOffset = -1;
	private float canvasXOffset = -1;
	
	
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
		//TODO
	}
	
	public void setSelector () {
		//TODO
	}
	
	public void undo () {
		//TODO
	}
	
	public void redo () {
		//TODO
	}
	
	public void openNote (INoteHierarchyItem newNote) {
		//TODO
	}
	
	public void saveNote () {
		//TODO
	}
	
	public void rename (String newName) {
		//TODO
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
			
			// Add new pen data to raw lists
			for (int i = 0; i < times.size(); i++) {
				rawPoints.addLast(new Point(-windowX + xs.get(i)/zoomMultiplier, -windowY + ys.get(i)/zoomMultiplier));
				rawPressure.add(pressures.get(i));
			}
			
			// Construct the polygon that represents the stroke.
			if (rawPoints.size() >= 2) {
				currentPolygon.clear();
				
				for (int i = 1; i < rawPoints.size(); i++) {
					Point[] toAdd = getLines(penSize*rawPressure.get(i-1), penSize*rawPressure.get(i), rawPoints.get(i-1), rawPoints.get(i));
					
					if (toAdd != null) {
						
						// Only remove intersections if the line being added isn't the first
						if (currentPolygon.size() >= 4) {
							Point leftIntersection = calcIntersection(currentPolygon.get(0), currentPolygon.get(1), toAdd[0], toAdd[1]);
							Point rightIntersection = calcIntersection(currentPolygon.get(currentPolygon.size()-2), currentPolygon.get(currentPolygon.size()-1), toAdd[2], toAdd[3]);
							
							if (leftIntersection == null) {
								currentPolygon.addFirst(toAdd[0]);
								currentPolygon.addFirst(toAdd[1]);
							} else {
								currentPolygon.removeFirst();
								currentPolygon.addFirst(leftIntersection);
								currentPolygon.addFirst(toAdd[1]);
							}
							
							if (rightIntersection == null) {
								currentPolygon.addLast(toAdd[2]);
								currentPolygon.addLast(toAdd[3]);
							} else {
								currentPolygon.removeLast();
								currentPolygon.addLast(rightIntersection);
								currentPolygon.addLast(toAdd[3]);
							}
							
						} else {
							currentPolygon.addFirst(toAdd[0]);
							currentPolygon.addFirst(toAdd[1]);
							currentPolygon.addLast(toAdd[2]);
							currentPolygon.addLast(toAdd[3]);
						}
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
	private static Point[] getLines (float w1, float w2, Point p1, Point p2) {
		
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
	
	/**
	 * Calculates the intersection of the two line segments. The line segments are considered open.
	 * 
	 * @return The point of intersection if it exists, null otherwise.
	 */
	private static Point calcIntersection (Point a, Point b, Point c, Point d) {

		// Test using floating point calculations
		float denominator = (d.y-c.y)*(b.x-a.x)-(d.x-c.x)*(b.y-a.y);

		if (Math.abs(denominator) < 0.00001) {
			return null;
		}

		float Ta = ((d.x-c.x)*(a.y-c.y)-(d.y-c.y)*(a.x-c.x))/denominator;
		float Tc = ((b.x-a.x)*(a.y-c.y)-(b.y-a.y)*(a.x-c.x))/denominator;

		if (Ta < 1 && Ta > 0 && Tc < 1 && Tc > 0) {
			return new Point(a.x + Ta*(b.x - a.x), a.y + Ta*(b.y - a.y));
		} else {
			return null;
		}
	}
	
	// If intersections are rare only calling calcIntersection if intersectionPossible returns true speeds collision detection.
	private static boolean intersectionPossible (Point a, Point b, Point c, Point d) {
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
	
	
	public void panZoomAction (float midpointX, float midpointY, float screenDx, float screenDy, float dZoom) {
		windowX += screenDx/zoomMultiplier;
		windowY += screenDy/zoomMultiplier;
		zoomMultiplier *= dZoom;
		
		
	}
	
	public void drawNote (Canvas c) {
		
		// Set the transformMatrix's offsets if they haven't been set yet
		if (canvasYOffset < 0 || canvasXOffset < 0) {
			float[] values = new float[9];
			c.getMatrix().getValues(values);
			canvasXOffset = values[2];
			canvasYOffset = values[5];
		}
		
		matVals[0] = zoomMultiplier;
		matVals[2] = windowX*zoomMultiplier + canvasXOffset;
		matVals[4] = zoomMultiplier;
		matVals[5] = windowY*zoomMultiplier + canvasYOffset;
		
		transformMatrix.setValues(matVals);
		c.setMatrix(transformMatrix);
		
		//Log.d("PEN", c.getMatrix().toString());
		
		
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
		
//		for (Point p : debugDots) {
//			c.drawCircle((p.x-windowX)*zoomMultiplier, (p.y-windowY)*zoomMultiplier, 5, debugPaint);
//		}
	}
	
}