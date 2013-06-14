package com.freehand.note_editor;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

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
	
	// Eraser UI fields
	private Point eCircCent;
	private float eCircRad;
	private Paint eCircPaint;
	
	// Selector UI fields
	private Paint lassoBorderPaint;
	private Paint lassoShadePaint;
	private Path lassoPath = new Path();
	private Paint selectionRectPaint;
	private RectF initSelRect = null;
	private RectF curSelRect = null;
	
	// Selector data fields
	private TreeSet<Integer> selections = null;
	private float selZoomMult = 1;
	private Point initSelAnchor;
	private Point curSelAnchor;
	
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
					selections = new TreeSet<Integer>();
					
					for (int i = 0; i < mStrokes.size(); i++) {
						Stroke s = mStrokes.get(i);
						if (RectF.intersects(lassoRect, s.getAABoundingBox())) {
							if (MiscPolyGeom.checkPolyIntersection(s.getPoly(), points) == true) {
								selections.add(Integer.valueOf(i));
							}
						}
					}
					
					initSelRect = null;
					curSelRect = null;
					if (selections.size() > 0) {
						for (Integer i : selections) {
							if (initSelRect == null) {
								initSelRect = new RectF(mStrokes.get(i).getAABoundingBox());
							} else {
								initSelRect.union(mStrokes.get(i).getAABoundingBox());
							}
						}
						
						float buffer = 15.0f/zoomMultiplier;
						initSelRect.left -= buffer;
						initSelRect.top -= buffer;
						initSelRect.right += buffer;
						initSelRect.bottom += buffer;
						
						curSelRect = new RectF(initSelRect);
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
		if (hoverEnded == false) {
			updateEraseCircle(scaleRawPoint(x, y));
		} else {
			resetEraseCircle();
		}
	}
	
	public void panZoomAction (float midpointX, float midpointY, float screenDx, float screenDy, float dZoom, RectF startBoundingRect) {
		boolean consumed = false;
		if (currentTool == IActionBarListener.Tool.STROKE_SELECTOR && curSelRect != null) {
			screenRectToCanvRect(startBoundingRect);
			
			if (curSelRect.contains(startBoundingRect)) {
				curSelAnchor = this.scaleRawPoint(midpointX, midpointY);
				
				if (initSelAnchor == null) {
					Log.d("PEN", "initSelAnchor set");
					initSelAnchor = curSelAnchor;
				}
				
				selZoomMult *= dZoom;
				
				curSelRect = new RectF(initSelRect);
				
				
				curSelRect.left = (initSelRect.left-curSelAnchor.x)*selZoomMult + curSelAnchor.x;
				curSelRect.right = (initSelRect.right-curSelAnchor.x)*selZoomMult + curSelAnchor.x;
				curSelRect.top = (initSelRect.top-curSelAnchor.y)*selZoomMult + curSelAnchor.y;
				curSelRect.bottom = (initSelRect.bottom-curSelAnchor.y)*selZoomMult + curSelAnchor.y;
				
				curSelRect.offset(curSelAnchor.x - initSelAnchor.x, curSelAnchor.y - initSelAnchor.y);
				
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
			
			for (int i = 0; i < mStrokes.size(); i++) {
				if (selections == null || selections.contains(i) == false) {
					mStrokes.get(i).draw(c);
				}
			}
			
			if (selections != null) {
				// TODO loop thru all of the selected strokes and draw their transformations by performing the shift and dropping that straight into a Path
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
			
			if (curSelRect != null) {
				selectionRectPaint.setStrokeWidth(3.0f/zoomMultiplier);
				selectionRectPaint.setPathEffect(new DashPathEffect(new float[] {12.0f/zoomMultiplier, 7.0f/zoomMultiplier}, 0));
				c.drawRect(curSelRect, selectionRectPaint);
			}
			
		}
		
		if (curSelAnchor != null) {
			c.drawCircle(curSelAnchor.x, curSelAnchor.y, 3, eCircPaint);
		}
	}
	
	
	
	//************************************************** IActionBarListener Methods *******************************************************
	
	public void setTool (Tool newTool, float size, int color) {
		currentTool = newTool;
		toolSize = size;
		toolColor = color;
		mBuilder.setColor(color);
		
		selections = null;
		initSelRect = null;
		curSelRect = null;
		selZoomMult = 1;
		initSelAnchor = null;
		curSelAnchor = null;
		
		eCircCent = null;
		
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
	
	private void screenRectToCanvRect (RectF rect) {
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

}