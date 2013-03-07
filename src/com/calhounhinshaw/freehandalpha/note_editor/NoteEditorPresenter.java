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
			if (currentPolygon.size() >= 3) {
				mStrokes.add(new Stroke(penColor, new ArrayList<Point>(currentPolygon)));
			}
			
			currentPolygon.clear();
			
			rawPoints.clear();
			rawPressure.clear();
			
		// Draw event
		} else {
			// Translate and scale new pen data then add it to raw lists
			for (int i = 0; i < times.size(); i++) {
				rawPoints.addLast(new Point(-windowX + xs.get(i)/zoomMultiplier, -windowY + ys.get(i)/zoomMultiplier));
				rawPressure.addLast(pressures.get(i));
			}
			
			currentPolygon = Geometry.buildIntermediatePoly(rawPoints, rawPressure, penSize);
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