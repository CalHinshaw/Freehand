package com.calhounhinshaw.freehandalpha.note_editor;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.calhounhinshaw.freehandalpha.ink.BooleanPolyGeom;
import com.calhounhinshaw.freehandalpha.ink.MiscGeom;
import com.calhounhinshaw.freehandalpha.ink.Point;
import com.calhounhinshaw.freehandalpha.ink.Stroke;
import com.calhounhinshaw.freehandalpha.ink.StrokeGeom;
import com.calhounhinshaw.freehandalpha.ink.StrokePolyBuilder;
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
	private ArrayList<Point> rawPoints = new ArrayList<Point>();
	private ArrayList<Float> rawPressures = new ArrayList<Float>();
	
	private Paint currentPaint = new Paint();
	
	// The zoom scalar that all values going into and out of the note are multiplied by after translation by windowX and windowY
	private float zoomMultiplier = 1;
	
	// The x and y values of the upper left corner of the screen relative to the note data
	private float windowX = 0;
	private float windowY = 0;
	
	private int penColor = 0xff000000;
	private float penSize = 6.5f;
	
	
	
	private Matrix transformMatrix = new Matrix();
	private float[] matVals = {1, 0, 0, 0, 1, 0, 0, 0, 1};
	
	// The transformation matrix transforms the stuff drawn to the canvas as if (0, 0) is the upper left hand corner of the screen,
	// not the View the canvas is drawing to.
	private float canvasYOffset = -1;
	private float canvasXOffset = -1;
	
	
	private Paint debugPaint1 = new Paint();
	private Paint debugPaint2 = new Paint();
	private Paint inPaint = new Paint();
	private Paint outPaint = new Paint();
	
	
	private final StrokePolyBuilder builder = new StrokePolyBuilder();
	
	
	
	public NoteEditorPresenter () {
		currentPaint.setColor(penColor);
		currentPaint.setStyle(Paint.Style.STROKE);
		currentPaint.setStrokeWidth(0);
		currentPaint.setAntiAlias(true);

		debugPaint1.setColor(Color.BLACK);
		debugPaint1.setStyle(Paint.Style.STROKE);
		debugPaint1.setStrokeWidth(0);
		debugPaint1.setAntiAlias(true);
		
		debugPaint2.setColor(0xa0ff0000);
		debugPaint2.setStyle(Paint.Style.STROKE);
		debugPaint2.setStrokeWidth(1);
		debugPaint2.setAntiAlias(true);
		
		inPaint.setColor(Color.GREEN);
		inPaint.setStyle(Paint.Style.FILL);
		inPaint.setAntiAlias(true);
		
		outPaint.setColor(Color.BLUE);
		outPaint.setStyle(Paint.Style.FILL);
		outPaint.setAntiAlias(true);
	}
	
	public void setPen (int newColor, float newSize) {
		penColor = newColor;
		currentPaint.setColor(penColor);
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
	
	public void hoverAction (ArrayList<Long> times, ArrayList<Float> xs, ArrayList<Float> ys) {
		//TODO
	}

	
	public void penAction (List<Long> times, List<Float> xs, List<Float> ys, List<Float> pressures, boolean penUp) {
		
		// Process the new points coming in from the stylus
		for (int i = 0; i < times.size(); i++) {
			// First scale the input to the current canvas position and pressure sensitivity
			Point currentPoint = new Point(-windowX + xs.get(i)/zoomMultiplier, -windowY + ys.get(i)/zoomMultiplier);
			float currentSize = penSize*0.33333f + pressures.get(i)*penSize*0.66666f;
			
			// Second add the points to the historical data
			rawPoints.add(currentPoint);
			rawPressures.add(pressures.get(i));
			
			builder.add(currentPoint, currentSize, zoomMultiplier);
		}

		// Terminate strokes when they end
		if (penUp == true) {
			WrapList<Point> finalPoly = builder.getFinalPoly();
			if (finalPoly.size() >= 3) {
				mStrokes.add(new Stroke(penColor, finalPoly));
			}
			builder.reset();
			
			rawPoints.clear();
			rawPressures.clear();
		}
	}
	
	
	
	
	public void panZoomAction (float midpointX, float midpointY, float screenDx, float screenDy, float dZoom) {
		windowX += screenDx/zoomMultiplier;
		windowY += screenDy/zoomMultiplier;
		zoomMultiplier *= dZoom;
	}
	
	private void calcPanZoom (Canvas c) {
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
	}
	
	
	public void drawNote (Canvas c) {
		calcPanZoom(c);
		c.drawColor(0xffffffff);
		
		for (Stroke s : mStrokes) {
			s.draw(c);
		}
		
		builder.draw(c, currentPaint);
	}
	
	
	
	
//	private void visualPolyTests (Canvas c) {
//		WrapList<Point> square1 = new WrapList<Point>();
//		WrapList<Point> square3 = new WrapList<Point>();
//		
//		square1.add(new Point(-100, -400));
//		square1.add(new Point(-100, 50));
//		square1.add(new Point(-50, 25));
//		square1.add(new Point(0, 50));
//		square1.add(new Point(0, -400));
//		
//		square3.add(new Point(200, 40f));
//		square3.add(new Point(200, -90));
//		square3.add(new Point(-200, -90));
//		square3.add(new Point(-200, 40f));
//		
//		drawDebugPolys(c, square1, square3);
//		
//		
//		WrapList<Point> shape1 = new WrapList<Point>();
//		WrapList<Point> shape2 = new WrapList<Point>();
//		
//		shape1.add(new Point(-100, 100));
//		shape1.add(new Point(-100, 550));
//		shape1.add(new Point(-50, 525));
//		shape1.add(new Point(0, 550));
//		shape1.add(new Point(0, 100));
//		
//		shape2.add(new Point(0, 550f));
//		shape2.add(new Point(0, 400));
//		shape2.add(new Point(-200, 400));
//		shape2.add(new Point(-200, 550f));
//		
//		drawDebugPolys(c, shape1, shape2);
//		
//		
//		WrapList<Point> hole1 = new WrapList<Point>();
//		WrapList<Point> hole2 = new WrapList<Point>();
//		
//		hole1.add(new Point(500, 700));
//		hole1.add(new Point(550, 750));
//		hole1.add(new Point(700, 700));
//		hole1.add(new Point(700, 500));
//		hole1.add(new Point(500, 500));
//		
//		hole2.add(new Point(550, 600));
//		hole2.add(new Point(550, 450));
//		hole2.add(new Point(650, 450));
//		hole2.add(new Point(650, 600));
//		hole2.add(new Point(700, 600));
//		hole2.add(new Point(700, 400));
//		hole2.add(new Point(500, 400));
//		
//		drawDebugPolys(c, hole1, hole2);
//		
//		
//		WrapList<Point> inside1 = new WrapList<Point>();
//		WrapList<Point> inside2 = new WrapList<Point>();
//		
//		inside1.add(new Point(600, 100));
//		inside1.add(new Point(500, 100));
//		inside1.add(new Point(550, 200));
//		
//		inside2.add(new Point(550, 150));
//		inside2.add(new Point(545, 150));
//		inside2.add(new Point(547.5f, 155));
//		
//		drawDebugPolys(c, inside1, inside2);
//		
//		
//		ArrayList<Point> points = new ArrayList<Point>();
//		ArrayList<Float> sizes = new ArrayList<Float>();
//		
//		points.add(new Point(100, 600));
//		points.add(new Point(110, 600));
//		points.add(new Point(110, 615));
//		points.add(new Point(90, 625));
//		points.add(new Point(90, 650));
//		points.add(new Point(90, 650));
//		points.add(new Point(100, 640));
//		points.add(new Point(91, 590));
//		
//		sizes.add(8f);
//		sizes.add(7f);
//		sizes.add(4f);
//		sizes.add(4f);
//		sizes.add(5f);
//		sizes.add(3f);
//		sizes.add(6f);
//		sizes.add(3f);
//		
//		WrapList<Point> poly = BooleanPolyGeom.buildPolygon(points, sizes);
//		
//		currentPath.reset();
//		currentPath.moveTo(poly.get(0).x, poly.get(0).y);
//		for (int i = 0; i <= poly.size(); i++) {
//			currentPath.lineTo(poly.get(i).x, poly.get(i).y);
//		}
//		c.drawPath(currentPath, currentPaint);
//		
//		
//		currentPath.reset();
//		
//		currentPath.moveTo(100, 800);
//		currentPath.lineTo(100, 800);
//		currentPath.lineTo(100, 1000);
//		currentPath.lineTo(300, 1000);
//		currentPath.lineTo(300, 1000);
//		currentPath.lineTo(300, 900);
//		currentPath.lineTo(50, 900);
//		currentPath.lineTo(50, 930);
//		currentPath.lineTo(250, 930);
//		currentPath.lineTo(250, 960);
//		currentPath.lineTo(150, 960);
//		currentPath.lineTo(150, 800);
//		
//		c.drawPath(currentPath, currentPaint);
//		
//	}
//	
//	
//	
//	private void drawDebugPolys (Canvas c, WrapList<Point> poly1, WrapList<Point> poly2) {
//
//		WrapList<Vertex> graph = BooleanPolyGeom.buildPolyGraph(poly1, poly2);
//
//		WrapList<Point> union = BooleanPolyGeom.union(graph, poly1, poly2);
//		
//		currentPath.reset();
//		currentPath.moveTo(poly1.get(0).x, poly1.get(0).y);
//		for (int i = 0; i <= poly1.size(); i++) {
//			currentPath.lineTo(poly1.get(i).x, poly1.get(i).y);
//		}
//		c.drawPath(currentPath, debugPaint1);
//		
//		currentPath.reset();
//		currentPath.moveTo(poly2.get(0).x, poly2.get(0).y);
//		for (int i = 0; i <= poly2.size(); i++) {
//			currentPath.lineTo(poly2.get(i).x, poly2.get(i).y);
//		}
//		c.drawPath(currentPath, debugPaint1);
//		
//		for (int i = 0; i < union.size(); i++) {
//			c.drawLine(union.get(i).x, union.get(i).y, union.get(i+1).x, union.get(i+1).y, debugPaint2);
//		}
//		
//		if (graph.size() > 1) {
//			c.drawCircle(graph.get(0).intersection.x, graph.get(0).intersection.y, 1.5f, inPaint);
//		}
//		
//		
//		for (Vertex v : graph) {
//			if (v.poly1Entry == true) {
//				c.drawCircle(v.intersection.x, v.intersection.y, 0.75f, inPaint);
//			} else {
//				c.drawCircle(v.intersection.x, v.intersection.y, 0.75f, outPaint);
//			}
//		}
//	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}