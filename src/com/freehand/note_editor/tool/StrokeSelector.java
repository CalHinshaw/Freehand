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
	
	
	
	// Selector UI fields
	private final WrapList<Point> lassoPoints = new WrapList<Point>(500);
	private final Path lassoPath;
	private final Paint lassoBorderPaint;
	private final Paint lassoShadePaint;
	
	private Paint selectionRectPaint;
	private RectF initSelRect = null;
	private RectF curSelRect = null;
	
	// Selector data fields
	private TreeSet<Integer> selectedStrokes = null;
	private float selZoomMult = 1;
	private Point initSelAnchor;
	private Point curSelAnchor;
	
	
	
	
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
		
		selectionRectPaint = new Paint();
		selectionRectPaint.setColor(0xAA33B5E5);
		selectionRectPaint.setStyle(Paint.Style.STROKE);
		selectionRectPaint.setAntiAlias(true);
	}
	

	
	
	
	
	
	
	public void startPointerEvent() {
		lassoPoints.clear();
		selectedStrokes = null;
		initSelRect = null;
		curSelRect = null;
	}

	public boolean continuePointerEvent(Point p, long time, float pressure) {
		lassoPoints.add(p);
		return true;
	}

	public void canclePointerEvent() { /* blank */ }

	public void finishPointerEvent() {
		
		if (lassoPoints.size() < 3) {
			lassoPoints.clear();
			return;
		}
		
		RectF lassoRect = MiscPolyGeom.calcAABoundingBox(lassoPoints);
		selectedStrokes = new TreeSet<Integer>();
		
		// Add all strokes that intersect the lasso poly to selectedStrokes
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
				if (initSelRect == null) {
					initSelRect = new RectF(mNote.get(i).getAABoundingBox());
				} else {
					initSelRect.union(mNote.get(i).getAABoundingBox());
				}
			}
			
			float buffer = mConverter.screenToCanvasDist(15.0f);
			initSelRect.left -= buffer;
			initSelRect.top -= buffer;
			initSelRect.right += buffer;
			initSelRect.bottom += buffer;
			
			if (initSelRect != null) {
				curSelRect = new RectF(initSelRect);
			}
			
		}
		
		lassoPoints.clear();
	}

	public void startPinchEvent() {
		// TODO Auto-generated method stub
		
	}

	public boolean continuePinchEvent(Point mid, Point dMid, float dZoom, RectF startBoundingRect) {
		
		
		if (curSelRect != null) {
			if (curSelRect.contains(startBoundingRect)) {
				curSelAnchor = mid;
				
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
				
				return true;
			}
			
			
			
		}
		return false;
	}

	public void canclePinchEvent() {
		// TODO Auto-generated method stub
		
	}

	public void finishPinchEvent() {
		// TODO Auto-generated method stub
		
	}

	public void startHoverEvent() {
		// TODO Auto-generated method stub
		
	}

	public boolean continueHoverEvent(Point p, long time) {
		// TODO Auto-generated method stub
		return false;
	}

	public void cancleHoverEvent() {
		// TODO Auto-generated method stub
		
	}

	public void finishHoverEvent() {
		// TODO Auto-generated method stub
		
	}

	public void drawNote(Canvas c) {

		// Draw all of the non-selected strokes in mNote
		for (int i = 0; i < mNote.size(); i++) {
			if (selectedStrokes == null || selectedStrokes.contains(i) == false) {
				mNote.get(i).draw(c);
			} else {
				mNote.get(i).drawSelected(c, mConverter.screenToCanvasDist(10.0f));
			}
		}
		
		// Draw all of the selectedStrokes with their selection highlights as they're being transformed
		if (selectedStrokes != null) {
			// TODO loop thru all of the selected strokes and draw their transformations by performing the shift and dropping that straight into a Path
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
		
		if (curSelRect != null) {
			selectionRectPaint.setStrokeWidth(mConverter.screenToCanvasDist(3.0f));
			selectionRectPaint.setPathEffect(new DashPathEffect(new float[] {mConverter.screenToCanvasDist(12.0f), mConverter.screenToCanvasDist(7.0f)}, 0));
			c.drawRect(curSelRect, selectionRectPaint);
		}
	}
	
}