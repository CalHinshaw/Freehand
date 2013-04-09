package com.calhounhinshaw.freehandalpha.note_editor;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.calhounhinshaw.freehandalpha.ink.BooleanPolyGeom;
import com.calhounhinshaw.freehandalpha.ink.MiscGeom;
import com.calhounhinshaw.freehandalpha.ink.Point;
import com.calhounhinshaw.freehandalpha.ink.Stroke;
import com.calhounhinshaw.freehandalpha.ink.UnitPolyGeom;
import com.calhounhinshaw.freehandalpha.ink.Vertex;
import com.calhounhinshaw.freehandalpha.misc.WrapList;
import com.calhounhinshaw.freehandalpha.note_orginazion.INoteHierarchyItem;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

class NoteEditorPresenter {
	private LinkedList<Stroke> mStrokes = new LinkedList<Stroke>();
	
	// Hold the current stroke's raw data
	private LinkedList<Point> rawPoints = new LinkedList<Point>();
	private LinkedList<Float> rawPressure = new LinkedList<Float>();
	
	// Holds the current stroke's data for drawing
	private WrapList<Point> currentPolygon = new WrapList<Point>();
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
	private Paint debugPaint = new Paint();
	
	private Matrix transformMatrix = new Matrix();
	private float[] matVals = {1, 0, 0, 0, 1, 0, 0, 0, 1};
	
	// The transformation matrix transforms the stuff drawn to the canvas as if (0, 0) is the upper left hand corner of the screen,
	// not the View the canvas is drawing to.
	private float canvasYOffset = -1;
	private float canvasXOffset = -1;
	
	
	public NoteEditorPresenter () {
		currentPaint.setColor(Color.RED);
		currentPaint.setStyle(Paint.Style.STROKE);
		currentPaint.setStrokeWidth(1);
		currentPaint.setAntiAlias(true);
		
		debugPaint.setColor(Color.BLACK);
		debugPaint.setStyle(Paint.Style.STROKE);
		debugPaint.setStrokeWidth(1);
		debugPaint.setAntiAlias(true);
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
		
		//TODO: I should probably split hover events and draw events into their own methods, but I'm not sure...
		
		// Hover event
		if (pressures.get(0) == -1) {
//			if (currentPolygon.size() >= 3) {
//				mStrokes.add(new Stroke(penColor, new ArrayList<Point>(currentPolygon)));
//			}
			
			currentPolygon.clear();
			
			rawPoints.clear();
			rawPressure.clear();
			
		// Draw event
		} else {
			// Translate and scale new pen data then add it to raw lists
			
			Point sLastAdded;
			if (rawPoints.isEmpty() == false) {
				sLastAdded = new Point(windowX + rawPoints.getLast().x*zoomMultiplier, windowY + rawPoints.getLast().y*zoomMultiplier);
			} else {
				sLastAdded = new Point(10000000000f, 10000000000f);
			}
			
			for (int i = 0; i < times.size(); i++) {
				float newScaledPressure = 0.333333f + pressures.get(i)*0.6666667f;
				
				if(MiscGeom.distSq(xs.get(i), ys.get(i), sLastAdded.x, sLastAdded.y) >= 4 || Math.abs(rawPressure.getLast() - newScaledPressure) >= 0.1f) {
					sLastAdded = new Point(xs.get(i), ys.get(i));
					
					rawPoints.addLast(new Point(-windowX + xs.get(i)/zoomMultiplier, -windowY + ys.get(i)/zoomMultiplier));
					rawPressure.addLast(newScaledPressure);
				}
			}
			
			//currentPolygon = Geometry.buildIntermediatePoly(rawPoints, Geometry.smoothPressures(rawPressure), penSize);
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

		c.drawColor(0xffffffff); 
		
//		for (Stroke s : mStrokes) {
//			s.draw(c);
//		}
//		
//		if (currentPolygon.size() >= 3) {
//			currentPaint.setColor(penColor);
//			currentPath.reset();
//			
//			currentPath.moveTo(currentPolygon.get(0).x, currentPolygon.get(0).y);
//			for (int i = 1; i < currentPolygon.size()/2; i++) {
//				currentPath.lineTo(currentPolygon.get(i).x, currentPolygon.get(i).y);
//			}
//			currentPath.lineTo(currentPolygon.get(0).x, currentPolygon.get(0).y);
//			
//			c.drawPath(currentPath, currentPaint);
//			
//			currentPaint.setColor(Color.RED);
//			currentPath.reset();
//			
//			currentPath.moveTo(currentPolygon.get(currentPolygon.size()/2).x, currentPolygon.get(currentPolygon.size()/2).y);
//			for (int i = currentPolygon.size()/2; i < currentPolygon.size(); i++) {
//				currentPath.lineTo(currentPolygon.get(i).x, currentPolygon.get(i).y);
//			}
//			currentPath.lineTo(currentPolygon.get(0).x, currentPolygon.get(0).y);
//			
//			c.drawPath(currentPath, currentPaint);
//			
//		}
		
		

		
//		for (int i = 0; i < rawPoints.size(); i++) {
//			c.drawCircle(rawPoints.get(i).x, rawPoints.get(i).y, penSize*rawPressure.get(i)*0.5f, debugPaint);
//		}
		
//		boolean useDebug = true;
//		
//		for (int i = 1; i < rawPoints.size(); i++) {
//			LinkedList<Point> poly = UnitPolyGeom.buildUnitPoly(penSize*rawPressure.get(i-1)*0.5f, penSize*rawPressure.get(i)*0.5f, rawPoints.get(i-1), rawPoints.get(i));
//			
//			currentPath.reset();
//			
//			if (poly != null && poly.size() > 2) {
//				currentPath.reset();
//				currentPath.moveTo(poly.getFirst().x, poly.getFirst().y);
//				for (Point p : poly) {
//					currentPath.lineTo(p.x, p.y);
//				}
//				currentPath.lineTo(poly.getFirst().x, poly.getFirst().y);
//			}
//			
//			c.drawPath(currentPath, useDebug? debugPaint : currentPaint);
//			
//			useDebug = !useDebug;
//		}
		
		
		WrapList<Point> square1 = new WrapList<Point>();
		WrapList<Point> square3 = new WrapList<Point>();
		
		
		square1.add(new Point(-100, -400));
		square1.add(new Point(-100, 50));
		square1.add(new Point(-50, 25));
		square1.add(new Point(0, 50));
		square1.add(new Point(0, -400));
		square1.add(new Point(-100, -400));
		
		square3.add(new Point(-100, 50f));
		square3.add(new Point(200, 50f));
		square3.add(new Point(200, -100));
		square3.add(new Point(-100, -100));
		square3.add(new Point(-100, 50f));
		
		
		currentPath.reset();
		currentPath.moveTo(square1.get(0).x, square1.get(0).y);
		for (int i = 0; i <= square1.size(); i++) {
			currentPath.lineTo(square1.get(i).x, square1.get(i).y);
		}
		c.drawPath(currentPath, currentPaint);
		
		currentPath.reset();
		currentPath.moveTo(square3.get(0).x, square3.get(0).y);
		for (int i = 0; i <= square3.size(); i++) {
			currentPath.lineTo(square3.get(i).x, square3.get(i).y);
		}
		c.drawPath(currentPath, currentPaint);
		
//		ArrayList<Vertex> verts = BooleanPolyGeom.buildPolyGraph(square1, square3);
//		
//		Log.d("PEN", "printing distances");
//		for (Vertex v : verts) {
//			
//			Log.d("PEN", v.intersection.toString());
//			Log.d("PEN", "Dist in 1:  " + Float.toString(v.distIn1));
//			Log.d("PEN", "Dist in 2:  " + Float.toString(v.distIn2));
//			
//			if (v.poly1Entry) {
//				c.drawCircle(v.intersection.x, v.intersection.y, 3, debugPaint);
//			} else {
//				c.drawCircle(v.intersection.x, v.intersection.y, 3, currentPaint);
//			}
//			
//		}

	}
	
}