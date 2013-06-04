package com.freehand.note_editor;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import com.freehand.ink.MiscGeom;
import com.freehand.ink.MiscPolyGeom;
import com.freehand.ink.Point;
import com.freehand.ink.Stroke;
import com.freehand.ink.StrokePolyBuilder;
import com.freehand.misc.WrapList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;

class NoteEditorController implements IActionBarListener, INoteCanvasListener {
	
	private NoteView mNoteView;
	
	// The note data about all of the old strokes
	private LinkedList<Stroke> mStrokes = new LinkedList<Stroke>();

	// The data for the current stroke
	private WrapList<Point> points = new WrapList<Point>();
	private ArrayList<Float> sizes = new ArrayList<Float>();
	private ArrayList<Long> times = new ArrayList<Long>();
	private final StrokePolyBuilder mBuilder = new StrokePolyBuilder();
	
	// The information about where the screen is on the canvas
	private float zoomMultiplier = 1;
	private float windowX = 0;
	private float windowY = 0;
	private Matrix transformMatrix = new Matrix();
	private float[] matVals = {1, 0, 0, 0, 1, 0, 0, 0, 1};
	
	// Current tool information
	private IActionBarListener.Tool currentTool = IActionBarListener.Tool.PEN;
	private int toolColor = 0xff000000;
	private float toolSize = 6.5f;
	
	// The transformation matrix transforms the stuff drawn to the canvas as if (0, 0) is the upper left hand corner of the screen,
	// not the View the canvas is drawing to.
	private float canvasYOffset = -1;
	private float canvasXOffset = -1;
	
	// Eraser fields
	private Point eCircCent;
	private float eCircRad;
	private Paint eCircPaint;
	
	// Selector fields
	private Paint lassoBorderPaint;
	private Paint lassoShadePaint;
	private Path lassoPath = new Path();
	private Paint selectionRectPaint;
	private ArrayList<Stroke> selections = null;
	private RectF selectionRect = null;
	
	public NoteEditorController (NoteView newNoteView) {
		mNoteView = newNoteView;
		
		eCircPaint = new Paint(Color.BLACK);
		eCircPaint.setStyle(Paint.Style.STROKE);
		eCircPaint.setAntiAlias(true);
		
		lassoPath.setFillType(Path.FillType.WINDING);
		
		lassoBorderPaint = new Paint();
		lassoBorderPaint.setColor(Color.RED);
		lassoBorderPaint.setStyle(Paint.Style.STROKE);
		lassoBorderPaint.setAntiAlias(true);
		
		lassoShadePaint = new Paint();
		lassoShadePaint.setColor(0x50CCCCCC);
		lassoShadePaint.setStyle(Paint.Style.FILL_AND_STROKE);
		lassoShadePaint.setAntiAlias(true);
		
		selectionRectPaint = new Paint();
		selectionRectPaint.setColor(0xFF33B5E5);
		selectionRectPaint.setStyle(Paint.Style.STROKE);
		selectionRectPaint.setAntiAlias(true);
	}
	
	//*********************************** INoteCanvasListener Methods ****************************************************************
	
	public void stylusAction (long time, float x, float y, float pressure, boolean stylusUp) {
		switch (currentTool) {
			case PEN:
				points.add(scaleRawPoint(x, y));
				sizes.add(scaleRawPressure(pressure));
				processPen(stylusUp);
				break;
			case STROKE_ERASER:
				Point currentPoint = scaleRawPoint(x, y);
				
				if (points.size() == 0 || MiscGeom.distance(currentPoint, points.get(points.size()-1)) > 10.0f/zoomMultiplier || time-times.get(times.size()-1) >= 100) {
					times.add(time);
					points.add(currentPoint);
					processStrokeErase(stylusUp);
				}
				
				if (stylusUp == false) {
					updateEraseCircle(currentPoint);
				} else {
					resetEraseCircle();
					points.clear();
					sizes.clear();
					times.clear();
				}
				break;
			case STROKE_SELECTOR:
				points.add(scaleRawPoint(x, y));
				
				if (stylusUp == true) {
					RectF lassoRect = MiscPolyGeom.calcAABoundingBox(points);
					selections = new ArrayList<Stroke>();
					
					for (Stroke s : mStrokes) {
						if (RectF.intersects(lassoRect, s.getAABoundingBox())) {
							if (MiscPolyGeom.checkPolyIntersection(s.getPoly(), points) == true) {
								selections.add(s);
							}
						}
					}
					
					selectionRect = null;
					if (selections.size() > 0) {
						for (Stroke s : selections) {
							if (selectionRect == null) {
								selectionRect = new RectF(s.getAABoundingBox());
							} else {
								selectionRect.union(s.getAABoundingBox());
							}
						}
						
						float buffer = 15.0f/zoomMultiplier;
						selectionRect.left -= buffer;
						selectionRect.top -= buffer;
						selectionRect.right += buffer;
						selectionRect.bottom += buffer;
					}
					
					points.clear();
				}
				
				break;
		}
	}
	
	public void fingerAction (long time, float x, float y, float pressure, boolean fingerUp) {
		// TODO Implement based on user settings gathered at first launch and changed in the menu
	}
	
	public void hoverAction (long time, float x, float y, boolean hoverEnded) {
		// TODO Implement for logging. This method shouldn't effect the cached node representation.
		
		if (hoverEnded == false) {
			updateEraseCircle(scaleRawPoint(x, y));
		} else {
			resetEraseCircle();
		}
	}
	
	public void panZoomAction (float midpointX, float midpointY, float screenDx, float screenDy, float dZoom, RectF boundingRect) {
		
		boolean consumed = false;
		if (currentTool == IActionBarListener.Tool.STROKE_SELECTOR && selectionRect != null) {
			scaleRawRect(boundingRect);
			if (selectionRect.contains(boundingRect)) {
				Log.d("PEN", "transform");
				consumed = true;
			}
		}
		
		if (consumed == false) {
			windowX += screenDx/zoomMultiplier;
			windowY += screenDy/zoomMultiplier;
			zoomMultiplier *= dZoom;
		}
	}
	
	public void drawNote (Canvas c) {
		updatePanZoom(c);
		c.drawColor(0xffffffff);
		
		
		
		if (currentTool == IActionBarListener.Tool.PEN) {
			for (Stroke s : mStrokes) {
				s.draw(c);
			}
			mBuilder.draw(c);
		} else if (currentTool == IActionBarListener.Tool.STROKE_ERASER) {
			for (Stroke s : mStrokes) {
				s.draw(c);
			}
			
			if (eCircCent != null) {
				float scaledWidth = 2.0f/zoomMultiplier;
				eCircPaint.setStrokeWidth(scaledWidth);
				c.drawCircle(eCircCent.x, eCircCent.y, eCircRad/zoomMultiplier - scaledWidth, eCircPaint);
			}
			
		} else if (currentTool == IActionBarListener.Tool.STROKE_SELECTOR) {
			
			for (Stroke s : mStrokes) {
				if (selections != null && selections.contains(s) == true) {
					s.drawSelected(c);
				} else {
					s.draw(c);
				}
			}
			
			if (points.size() > 0) {
				lassoBorderPaint.setStrokeWidth(3.0f/zoomMultiplier);
				lassoBorderPaint.setPathEffect(new DashPathEffect(new float[] {12.0f/zoomMultiplier, 7.0f/zoomMultiplier}, 0));
				
				lassoPath.reset();
				lassoPath.moveTo(points.get(0).x, points.get(0).y);
				for (Point p : points) {
					lassoPath.lineTo(p.x, p.y);
				}
				
				c.drawPath(lassoPath, lassoShadePaint);
				c.drawPath(lassoPath, lassoBorderPaint);
			}
			
			if (selectionRect != null) {
				selectionRectPaint.setStrokeWidth(3.0f/zoomMultiplier);
				selectionRectPaint.setPathEffect(new DashPathEffect(new float[] {12.0f/zoomMultiplier, 7.0f/zoomMultiplier}, 0));
				c.drawRect(selectionRect, selectionRectPaint);
			}
			
		}
	}
	
	
	
	//************************************************** IActionBarListener Methods *******************************************************
	
	public void setTool (Tool newTool, float size, int color) {
		currentTool = newTool;
		toolSize = size;
		toolColor = color;
		mBuilder.setColor(color);
		
		mNoteView.invalidate();
	}

	public void undo () {
		// TODO
	}

	public void redo () {
		// TODO
	}
	
	
	
	//********************************************** Helper Methods **********************************************
	
	private void updatePanZoom (Canvas c) {
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
	
	private void processPen (boolean stylusUp) {
		mBuilder.add(points.get(points.size()-1), sizes.get(sizes.size()-1), zoomMultiplier);

		// Terminate strokes when they end
		if (stylusUp == true) {
			WrapList<Point> finalPoly = mBuilder.getFinalPoly();
			if (finalPoly.size() >= 3) {
				mStrokes.add(new Stroke(toolColor, finalPoly));
			}
			mBuilder.reset();
			
			points.clear();
			sizes.clear();
		}
	}
	
	private void processStrokeErase (boolean stylusUp) {
		
		float scaledSize = toolSize/zoomMultiplier;
		
		if (points.size() == 1) {
			RectF eraseBox = MiscGeom.calcCapsuleAABB(points.get(0), points.get(0), scaledSize);
			
			Iterator<Stroke> iter = mStrokes.iterator();
			while (iter.hasNext()) {
				Stroke s = iter.next();
				if (RectF.intersects(eraseBox, s.getAABoundingBox())) {
					if (MiscPolyGeom.checkCapsulePolyIntersection(s.getPoly(), points.get(0), points.get(0), scaledSize) == true) {
						iter.remove();
					}
				}
			}
		} else if (points.size() >= 2) {
			RectF eraseBox = MiscGeom.calcCapsuleAABB(points.get(points.size()-2), points.get(points.size()-1), scaledSize);
			
			Iterator<Stroke> iter = mStrokes.iterator();
			while (iter.hasNext()) {
				Stroke s = iter.next();
				if (RectF.intersects(eraseBox, s.getAABoundingBox())) {
					if (MiscPolyGeom.checkCapsulePolyIntersection(s.getPoly(), points.get(points.size()-2), points.get(points.size()-1), scaledSize) == true) {
						iter.remove();
					}
				}
			}
		}
	}
	
	private Point scaleRawPoint (float x, float y) {
		return new Point(-windowX + x/zoomMultiplier, -windowY + y/zoomMultiplier);
	}
	
	private void scaleRawRect (RectF rect) {
		rect.left = -windowX + rect.left/zoomMultiplier;
		rect.right = -windowX + rect.right/zoomMultiplier;
		rect.top = -windowY + rect.top/zoomMultiplier;
		rect.bottom = -windowY + rect.bottom/zoomMultiplier;
	}
	
	private float scaleRawPressure (float pressure) {
		return toolSize*0.6f + pressure*toolSize*0.4f;
	}
	
	private void updateEraseCircle (Point newPoint) {
		eCircCent = newPoint;
		eCircRad = toolSize;
	}
	
	private void resetEraseCircle () {
		eCircCent = null;
		eCircRad = 0;
	}
	
//	private void visualPolyTests (Canvas c) {
	
	//	private Paint debugPaint1 = new Paint();
	//	private Paint debugPaint2 = new Paint();
	//	private Paint inPaint = new Paint();
	//	private Paint outPaint = new Paint();
	
	
	//	debugPaint1.setColor(Color.BLACK);
	//	debugPaint1.setStyle(Paint.Style.STROKE);
	//	debugPaint1.setStrokeWidth(0);
	//	debugPaint1.setAntiAlias(true);
	//	
	//	debugPaint2.setColor(0xa0ff0000);
	//	debugPaint2.setStyle(Paint.Style.STROKE);
	//	debugPaint2.setStrokeWidth(1);
	//	debugPaint2.setAntiAlias(true);
	//	
	//	inPaint.setColor(Color.GREEN);
	//	inPaint.setStyle(Paint.Style.FILL);
	//	inPaint.setAntiAlias(true);
	//	
	//	outPaint.setColor(Color.BLUE);
	//	outPaint.setStyle(Paint.Style.FILL);
	//	outPaint.setAntiAlias(true);
	
	
	
	
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