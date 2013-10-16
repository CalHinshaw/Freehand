package com.freehand.editor.canvas;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.freehand.editor.canvas.Note.Action;
import com.freehand.ink.MiscPolyGeom;
import com.freehand.ink.Point;
import com.freehand.ink.Stroke;
import com.freehand.misc.WrapList;
import com.freehand.tutorial.TutorialPrefs;


public class StrokeSelector implements ICanvasEventListener {
	
	private final Note mNote;
	private final DistConverter mConverter;
	
	private List<Stroke> currentStrokes;
	
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
	
	// Translation flags
	private boolean isTransforming = false;
	private boolean setIsTransforming = false;
	
	// Translation variables
	private Point initMid = null;
	private Float initDist = null;
	private Point currentMid = null;
	private Float currentDist = null;
	
	
	
	public StrokeSelector (Note note, DistConverter converter) {
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
		
		currentStrokes = mNote.getInkLayer();
	}
	

	
	public void startPointerEvent() {
		lassoPoints.clear();
		if (selRect != null) {
			setIsTransforming = true;
		}
	}

	public boolean continuePointerEvent(Point p, long time, float pressure) {
		if (setIsTransforming == true && selRect.contains(p.x, p.y)) {
			initMid = p;
			initDist = Float.valueOf(1);
			isTransforming = true;
		}
		setIsTransforming = false;
		
		if (isTransforming == true) {
			currentMid = p;
			currentDist = Float.valueOf(1);
		} else {
			lassoPoints.add(p);
		}
		return true;
	}

	public void canclePointerEvent() {
		lassoPoints.clear();
		// Removed so that transitioning from one finger to two during a translation isn't a problem.
		// This could cause problems in the future and is worth keeping an eye on.
		//resetTrans();
	}

	public void finishPointerEvent() {
		
		if (isTransforming == true) {
			applyTransToNote();
			resetTrans();
			lassoPoints.clear();
			currentStrokes = mNote.getInkLayer();
			return;
		}
		
		currentStrokes = mNote.getInkLayer();
		selectedStrokes.clear();
		selRect = null;
		selRect = null;
		
		if (lassoPoints.size() < 3) {
			lassoPoints.clear();
			return;
		}
		
		RectF lassoRect = MiscPolyGeom.calcAABoundingBox(lassoPoints);
		selectedStrokes.clear();
		
		for (int i = 0; i < currentStrokes.size(); i++) {
			Stroke s = currentStrokes.get(i);
			if (RectF.intersects(lassoRect, s.getAABoundingBox())) {
				if (MiscPolyGeom.checkPolyIntersection(s.getPoly(), lassoPoints) == true) {
					selectedStrokes.add(Integer.valueOf(i));
				}
			}
		}
		
		// Calculate the AABB of selectedStrokes
		if (selectedStrokes.size() > 0) {
			triggerTutorial();
			for (Integer i : selectedStrokes) {
				if (selRect == null) {
					selRect = new RectF(currentStrokes.get(i).getAABoundingBox());
				} else {
					selRect.union(currentStrokes.get(i).getAABoundingBox());
				}
			}
			
			float buffer = mConverter.screenToCanvasDist(15.0f);
			selRect.left -= buffer;
			selRect.top -= buffer;
			selRect.right += buffer;
			selRect.bottom += buffer;
			
		}
		
		lassoPoints.clear();
		currentStrokes = mNote.getInkLayer();
	}

	
	public void startPinchEvent() {
		lassoPoints.clear();
		if (selRect != null) {
			setIsTransforming = true;
		}
	}

	public boolean continuePinchEvent(Point mid, Point dMid, float dZoom, float dist, RectF startBoundingRect) {
		
		if (setIsTransforming == true && isTransforming == true) {
			initDist = dist;
			initMid = new Point(initMid.x + (mid.x - currentMid.x), initMid.y + (mid.y - currentMid.y));
		} else if (setIsTransforming == true && selRect.contains(startBoundingRect)) {
			initMid = mid;
			initDist = dist;
			isTransforming = true;
		}
		setIsTransforming = false;
		
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
		if (isTransforming == true) {
			applyTransToNote();
		}
		
		resetTrans();
	}

	public void startHoverEvent() { /* blank */	}
	public boolean continueHoverEvent(Point p, long time) {	return false; }
	public void cancleHoverEvent() { /* blank */ }
	public void finishHoverEvent() { /* blank */ }

	public RectF getDirtyRect() {
		return null;
	}
	
	public void drawNote(Canvas c) {

		// Draw all of the non-selected strokes in mNote
		for (int i = 0; i < currentStrokes.size(); i++) {
			if (selectedStrokes == null || selectedStrokes.contains(i) == false) {
				currentStrokes.get(i).draw(c);
			}
		}
		
		// Draw all of the selectedStrokes with their selection highlights as they're being transformed
		if (selectedStrokes.isEmpty() == false && isTransforming == true) {
			selBorderPaint.setStrokeWidth(mConverter.screenToCanvasDist(8.0f));
			float dZoom = currentDist / initDist;
			
			for (Integer i : selectedStrokes) {
				WrapList<Point> poly = currentStrokes.get(i).getPoly();
				
				selPath.reset();
				selPath.moveTo((poly.get(0).x-initMid.x) * dZoom + currentMid.x, (poly.get(0).y-initMid.y) * dZoom + currentMid.y);
				for (Point p : poly) {
					selPath.lineTo((p.x-initMid.x) * dZoom + currentMid.x, (p.y-initMid.y) * dZoom + currentMid.y);
				}
				selPath.lineTo((poly.get(0).x-initMid.x) * dZoom + currentMid.x, (poly.get(0).y-initMid.y) * dZoom + currentMid.y);
				
				c.drawPath(selPath, selBorderPaint);
				selBodyPaint.setColor(Color.WHITE);
				c.drawPath(selPath, selBodyPaint);
				selBodyPaint.setColor(currentStrokes.get(i).getColor());
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
				WrapList<Point> poly = currentStrokes.get(i).getPoly();
				
				selPath.reset();
				selPath.moveTo(poly.get(0).x, poly.get(0).y);
				for (Point p : poly) {
					selPath.lineTo(p.x, p.y);
				}
				selPath.lineTo(poly.get(0).x, poly.get(0).y);
				
				c.drawPath(selPath, selBorderPaint);
				selBodyPaint.setColor(Color.WHITE);
				c.drawPath(selPath, selBodyPaint);
				selBodyPaint.setColor(currentStrokes.get(i).getColor());
				c.drawPath(selPath, selBodyPaint);
			}
			
			selRectPaint.setStrokeWidth(mConverter.screenToCanvasDist(3.0f));
			selRectPaint.setPathEffect(new DashPathEffect(new float[] {mConverter.screenToCanvasDist(12.0f), mConverter.screenToCanvasDist(7.0f)}, 0));
			c.drawRect(selRect, selRectPaint);
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
	
	public void undoCalled() {
		currentStrokes = mNote.getInkLayer();
		lassoPoints.clear();
		selectedStrokes.clear();
		selRect = null;
		selRect = null;
	}
	
	public void redoCalled() {
		currentStrokes = mNote.getInkLayer();
		lassoPoints.clear();
		selectedStrokes.clear();
		selRect = null;
		selRect = null;
	}
	
	private void applyTransToNote () {
		setTutorialToOff();
		ArrayList<Action> actions = new ArrayList<Action>(selectedStrokes.size()*2);
		
		float dZoom = currentDist / initDist;
		
		int offsetCounter = 0;
		for (Integer i : selectedStrokes) {
			WrapList<Point> poly = currentStrokes.get(i).getPoly();
			
			WrapList<Point> transPoly = new WrapList<Point>(poly.size());
			
			for (Point p : poly) {
				transPoly.add(new Point((p.x-initMid.x) * dZoom + currentMid.x, (p.y-initMid.y) * dZoom + currentMid.y));
			}
			
			Stroke transStroke = new Stroke(currentStrokes.get(i).getColor(), transPoly);
			actions.add(new Action(transStroke, currentStrokes.size()+offsetCounter, true));
			offsetCounter++;
		}
		
		Iterator<Integer> iter = selectedStrokes.descendingIterator();
		
		while (iter.hasNext()) {
			int index = iter.next().intValue();
			
			actions.add(new Action(currentStrokes.get(index), index, false));
		}
		
		int numSelections = selectedStrokes.size();
		selectedStrokes.clear();
		for (int index = currentStrokes.size()-numSelections; index < currentStrokes.size(); index++) {
			selectedStrokes.add(index);
		}
		
		mNote.performActions(actions);
		
		RectF curRect = new RectF();
		curRect.left = (selRect.left - initMid.x) * dZoom + currentMid.x;
		curRect.right = (selRect.right - initMid.x) * dZoom + currentMid.x;
		curRect.top = (selRect.top - initMid.y) * dZoom + currentMid.y;
		curRect.bottom = (selRect.bottom - initMid.y) * dZoom + currentMid.y;
		selRect = curRect;
	}
	
	private void resetTrans() {
		isTransforming = false;
		setIsTransforming = false;
		
		initMid = null;
		initDist = null;
		
		currentMid = null;
		currentDist = null;
	}
	
	
	
	// *************************************** Tutorial Methods ************************************
	
	private void triggerTutorial() {
		final SharedPreferences prefs = TutorialPrefs.getPrefs();
		if (prefs == null) return;
		boolean used = prefs.getBoolean("move_resize_used", false);
		if (used == false) {
			TutorialPrefs.toast("Pinch inside blue rectangle to move and resize selections");
		}
	}
	
	private void setTutorialToOff() {
		final SharedPreferences prefs = TutorialPrefs.getPrefs();
		if (prefs == null) return;
		if (prefs.getBoolean("move_resize_used", false) == true) return;
		TutorialPrefs.toast("Pinching outside of the rectangle pans and zooms like usual");
		prefs.edit().putBoolean("move_resize_used", true).apply();
	}
}