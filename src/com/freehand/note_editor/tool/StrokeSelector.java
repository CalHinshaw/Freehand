package com.freehand.note_editor.tool;

import java.util.List;
import java.util.TreeSet;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;

import com.freehand.ink.MiscPolyGeom;
import com.freehand.ink.Point;
import com.freehand.ink.Stroke;
import com.freehand.misc.WrapList;
import com.freehand.note_editor.ICanvasEventListener;

public class StrokeSelector implements ICanvasEventListener {
	
	private final List<Stroke> mNote;
	private final DistConverter mConverter;
	
	
	
	// Lasso stuff
	private final WrapList<Point> lassoPoints = new WrapList<Point>(500);
	private final Path lassoPath;
	private final Paint lassoBorderPaint;
	private final Paint lassoShadePaint;
	
	// Selection stuff
	private TreeSet<Integer> selectedStrokes = new TreeSet<Integer>();
	private Paint selBorderPaint;
	private Paint selBodyPaint;
	private Path selPath;
	
	private Paint selRectPaint;
	private RectF selRect = null;
	
	// Translation stuff
	private Point initMid = null;
	private Float initDist = null;
	private Point currentMid = null;
	private Float currentDist = null;
	
	
	
	
	
	
	
	public StrokeSelector (List<Stroke> note, DistConverter converter) {
		mNote = note;
		mConverter = converter;
		
		lassoPath = new Path();
		lassoPath.setFillType(Path.FillType.WINDING);
		
		lassoBorderPaint = new Paint();
		lassoBorderPaint.setColor(Color.RED);
		lassoBorderPaint.setStyle(Paint.Style.STROKE);
		lassoBorderPaint.setAntiAlias(true);
		
		lassoShadePaint = new Paint();
		lassoShadePaint.setColor(0x50CCCCCC);
		lassoShadePaint.setStyle(Paint.Style.FILL_AND_STROKE);
		lassoShadePaint.setAntiAlias(true);
		
		selPath = new Path();
		selPath.setFillType(Path.FillType.WINDING);
		
		selBorderPaint = new Paint();
		selBorderPaint.setColor(Color.BLACK);
		selBorderPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		selBorderPaint.setAntiAlias(true);
		
		selBodyPaint = new Paint();
		selBodyPaint.setStyle(Paint.Style.FILL);
		selBodyPaint.setAntiAlias(true);
		
		selRectPaint = new Paint();
		selRectPaint.setColor(0xAA33B5E5);
		selRectPaint.setStyle(Paint.Style.STROKE);
		selRectPaint.setAntiAlias(true);
	}
	

	
	
	
	
	
	
	public void startPointerEvent() {
		lassoPoints.clear();
		selectedStrokes.clear();
		selRect = null;
		selRect = null;
	}

	public boolean continuePointerEvent(Point p, long time, float pressure) {
		lassoPoints.add(p);
		return true;
	}

	public void canclePointerEvent() {
		lassoPoints.clear();
	}

	public void finishPointerEvent() {
		if (lassoPoints.size() < 3) {
			lassoPoints.clear();
			return;
		}
		
		RectF lassoRect = MiscPolyGeom.calcAABoundingBox(lassoPoints);
		selectedStrokes.clear();
		
		for (int i = 0; i < mNote.size(); i++) {
			Stroke s = mNote.get(i);
			if (RectF.intersects(lassoRect, s.getAABoundingBox())) {
				if (MiscPolyGeom.checkPolyIntersection(s.getPoly(), lassoPoints) == true) {
					selectedStrokes.add(Integer.valueOf(i));
				}
			}
		}
		
		// Calculate the AABB of selectedStrokes
		if (selectedStrokes.size() > 0) {
			for (Integer i : selectedStrokes) {
				if (selRect == null) {
					selRect = new RectF(mNote.get(i).getAABoundingBox());
				} else {
					selRect.union(mNote.get(i).getAABoundingBox());
				}
			}
			
			float buffer = mConverter.screenToCanvasDist(15.0f);
			selRect.left -= buffer;
			selRect.top -= buffer;
			selRect.right += buffer;
			selRect.bottom += buffer;
			
		}
		
		lassoPoints.clear();
	}

	private boolean isTransforming = false;
	private boolean setIsTransforming = false;
	
	public void startPinchEvent() {
		setIsTransforming = true;
	}

	public boolean continuePinchEvent(Point mid, Point dMid, float dZoom, float dist, RectF startBoundingRect) {
		if (setIsTransforming == true) {
			if (selRect != null && selRect.contains(startBoundingRect)) {
				initMid = mid;
				initDist = dist;
				isTransforming = true;
			} else {
				isTransforming = false;
			}
			setIsTransforming = false;
		}
		
		
		if (isTransforming == true) {
			currentMid = mid;
			currentDist = dist;
			return true;
		} else {
			return false;
		}
	}

	public void canclePinchEvent() {
		resetTrans();
	}

	public void finishPinchEvent() {
		resetTrans();
	}

	public void startHoverEvent() { /* blank */	}
	public boolean continueHoverEvent(Point p, long time) {	return false; }
	public void cancleHoverEvent() { /* blank */ }
	public void finishHoverEvent() { /* blank */ }

	public void drawNote(Canvas c) {

		// Draw all of the non-selected strokes in mNote
		for (int i = 0; i < mNote.size(); i++) {
			if (selectedStrokes == null || selectedStrokes.contains(i) == false) {
				mNote.get(i).draw(c);
			}
		}
		
		// Draw all of the selectedStrokes with their selection highlights as they're being transformed
		if (selectedStrokes.isEmpty() == false && isTransforming == true) {
			selBorderPaint.setStrokeWidth(mConverter.screenToCanvasDist(8.0f));
			float dZoom = currentDist / initDist;
			
			for (Integer i : selectedStrokes) {
				WrapList<Point> poly = mNote.get(i).getPoly();
				
				selPath.reset();
				selPath.moveTo((poly.get(0).x-initMid.x) * dZoom + currentMid.x, (poly.get(0).y-initMid.y) * dZoom + currentMid.y);
				for (Point p : poly) {
					selPath.lineTo((p.x-initMid.x) * dZoom + currentMid.x, (p.y-initMid.y) * dZoom + currentMid.y);
				}
				
				c.drawPath(selPath, selBorderPaint);
				selBodyPaint.setColor(Color.WHITE);
				c.drawPath(selPath, selBodyPaint);
				selBodyPaint.setColor(mNote.get(i).getColor());
				c.drawPath(selPath, selBodyPaint);
			}
			
			// Selection rect
			selRectPaint.setStrokeWidth(mConverter.screenToCanvasDist(3.0f));
			selRectPaint.setPathEffect(new DashPathEffect(new float[] {mConverter.screenToCanvasDist(12.0f), mConverter.screenToCanvasDist(7.0f)}, 0));
			
			if (isTransforming == true) {
				RectF curRect = new RectF();
				curRect.left = (selRect.left - initMid.x) * dZoom + currentMid.x;
				curRect.right = (selRect.right - initMid.x) * dZoom + currentMid.x;
				curRect.top = (selRect.top - initMid.y) * dZoom + currentMid.y;
				curRect.bottom = (selRect.bottom - initMid.y) * dZoom + currentMid.y;
				
				c.drawRect(curRect, selRectPaint);
			}
		} else if (selectedStrokes.isEmpty() == false) {
			selBorderPaint.setStrokeWidth(mConverter.screenToCanvasDist(8.0f));
			
			for (Integer i : selectedStrokes) {
				WrapList<Point> poly = mNote.get(i).getPoly();
				
				selPath.reset();
				selPath.moveTo(poly.get(0).x, poly.get(0).y);
				for (Point p : poly) {
					selPath.lineTo(p.x, p.y);
				}
				
				c.drawPath(selPath, selBorderPaint);
				selBodyPaint.setColor(Color.WHITE);
				c.drawPath(selPath, selBodyPaint);
				selBodyPaint.setColor(mNote.get(i).getColor());
				c.drawPath(selPath, selBodyPaint);
			}
			
			selRectPaint.setStrokeWidth(mConverter.screenToCanvasDist(3.0f));
			selRectPaint.setPathEffect(new DashPathEffect(new float[] {mConverter.screenToCanvasDist(12.0f), mConverter.screenToCanvasDist(7.0f)}, 0));
			c.drawRect(selRect, selRectPaint);
		} else {
			Log.d("PEN", "neither");
		}
		
		
		if (lassoPoints.size() > 0) {
			lassoBorderPaint.setStrokeWidth(mConverter.screenToCanvasDist(3.0f));
			lassoShadePaint.setStrokeWidth(mConverter.screenToCanvasDist(3.0f));
			lassoBorderPaint.setPathEffect(new DashPathEffect(new float[] {mConverter.screenToCanvasDist(12.0f), mConverter.screenToCanvasDist(7.0f)}, 0));
			
			lassoPath.reset();
			lassoPath.moveTo(lassoPoints.get(0).x, lassoPoints.get(0).y);
			for (Point p : lassoPoints) {
				lassoPath.lineTo(p.x, p.y);
			}
			
			c.drawPath(lassoPath, lassoShadePaint);
			c.drawPath(lassoPath, lassoBorderPaint);
		}
		
	}
	
	
	
	
	private void resetTrans() {
		isTransforming = false;
		setIsTransforming = false;
		initMid = null;
		initDist = null;
		currentMid = null;
		currentDist = null;
	}
}