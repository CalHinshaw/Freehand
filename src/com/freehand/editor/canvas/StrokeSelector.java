package com.freehand.editor.canvas;

import java.util.ArrayList;
import java.util.List;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;

import com.freehand.editor.canvas.Note.Action;
import com.freehand.ink.MiscGeom;
import com.freehand.ink.MiscPolyGeom;
import com.freehand.ink.Point;
import com.freehand.ink.Stroke;
import com.freehand.tutorial.TutorialPrefs;


public class StrokeSelector implements ITool {
	
	private final Note mNote;
	private final ICanvScreenConverter mConverter;
	private final boolean allowCapDrawing;
	
	private List<Stroke> currentStrokes;
	
	// Lasso stuff
	private List<Point> lassoPoints = new ArrayList<Point>(500);
	private final Path lassoPath;
	private final Paint lassoBorderPaint;
	private final Paint lassoShadePaint;
	
	// Selection stuff
	private List<WorkingStroke> selectedStrokes = new ArrayList<WorkingStroke>();
	private Paint selBorderPaint;
	private Paint selBodyPaint;
	private Path selPath;
	
	private Paint selRectPaint;
	private RectF selRect = null;
	
	private boolean eventInSelRect = false;
	private boolean eventIsMultiTouch = false;
	
	
	public StrokeSelector (Note note, ICanvScreenConverter converter, final boolean allowCapDrawing) {
		mNote = note;
		mConverter = converter;
		this.allowCapDrawing = allowCapDrawing;
		
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
		selBorderPaint.setStrokeJoin(Paint.Join.ROUND);
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
	
	
	//***************************************************** dispatch *****************************************
	
	private boolean ignoringCurrentMe = false;
	
	public boolean onMotionEvent(MotionEvent e) {
		
		if (e.getActionMasked() == MotionEvent.ACTION_HOVER_ENTER ||
			e.getActionMasked() == MotionEvent.ACTION_HOVER_MOVE ||
			e.getActionMasked() == MotionEvent.ACTION_HOVER_EXIT) {
			return false;
		}
		
		if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
			
			final RectF eventBox = new RectF(e.getX(0), e.getY(0), e.getX(0), e.getY(0));
			if (e.getPointerCount() > 1) {
				Log.d("PEN", "down has more than 1 ptr");
				eventBox.union(e.getX(1), e.getY(1));
			}
			
			if (selRect != null && selRect.contains(eventBox)) {
				eventInSelRect = true;
			}
		}
		
		if (e.getPointerCount() > 1) {
			eventIsMultiTouch = true;
			resetLasso();
		}
		
		boolean returnVar = false;
		if (eventInSelRect == true) {
			returnVar = transEvent(e);
		} else if (eventIsMultiTouch == false) {
			returnVar = lassoEvent(e);
		}
		
		
		if (e.getActionMasked() == MotionEvent.ACTION_UP) {
			eventInSelRect = false;
			eventIsMultiTouch = false;
		}
		
		return returnVar;
	}
	
	
	
	//*************************************** Lasso ***********************************************
	
	private boolean lassoEvent (MotionEvent e) {
		if (e.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER && allowCapDrawing == false) return false;
		if (ignoringCurrentMe) return false;
		if (e.getPointerCount() > 1) {
			resetLasso();
			ignoringCurrentMe = true;
			return false;
		}
		
		if (e.getAction() == MotionEvent.ACTION_DOWN) {
			lassoPoints.add(new Point(e.getX(), e.getY()));
		} else if (e.getAction() == MotionEvent.ACTION_MOVE) {
			for (int i = 0; i < e.getHistorySize(); i++) {
				lassoPoints.add(new Point(e.getHistoricalX(i), e.getHistoricalY(i)));
			}
			lassoPoints.add(new Point(e.getX(), e.getY()));
		} else if (e.getAction() == MotionEvent.ACTION_UP) {
			if (lassoPoints.size() >= 1) {
				resetTrans();
				selectedStrokes = getContainedIndexes(lassoPoints, currentStrokes);
				if (selectedStrokes.size() > 0) {
					triggerTransformationTutorial();
					final float aabbBuffer = mConverter.screenToCanvDist(15.0f);
					selRect = getAabbFromIndexes(selectedStrokes, aabbBuffer);
				}
			}
			
			resetLasso();
		}
		
		return true;
	}
	
	private void resetLasso () {
		lassoPoints.clear();
		ignoringCurrentMe = false;
	}
	
	private static List<WorkingStroke> getContainedIndexes (final List<Point> lasso, final List<Stroke> noteStrokes) {
		final List<WorkingStroke> selectedIndexes = new ArrayList<WorkingStroke>();
		
		final RectF lassoRect = MiscPolyGeom.calcAABoundingBox(lasso);
		for (int i = 0; i < noteStrokes.size(); i++) {
			Stroke s = noteStrokes.get(i);
			if (RectF.intersects(lassoRect, s.getAABoundingBox())) {
				if (MiscPolyGeom.checkPolyIntersection(s.getPoly(), lasso) == true) {
					final WorkingStroke ws = new WorkingStroke(i, new ArrayList<Point>(s.getPoly()), s.getColor());
					selectedIndexes.add(ws);
				}
			}
		}
		
		return selectedIndexes;
	}
	
	/**
	 * Calculates the axis aligned bounding box of the noteStrokes who's indexes are in indexes.
	 * @param buffer the buffer around the aabb
	 * @return the aabb if indexes.size() > 0 or an empty RectF if indexes.size() <= 0
	 */
	private static RectF getAabbFromIndexes (final List<WorkingStroke> strokes, final float buffer) {
		final RectF aabb = new RectF();
		for (WorkingStroke ws : strokes) {
			aabb.union(calcAABoundingBox(ws.poly));
		}
		//
		aabb.left -= buffer;
		aabb.top -= buffer;
		aabb.right += buffer;
		aabb.bottom += buffer;
		
		return aabb;
	}
	
	
	//************************************************* Transform *********************************************
	
	private MotionEvent prevEvent = null;
	
	private boolean transEvent (MotionEvent curEvent) {
		if (prevEvent != null && curEvent.getPointerCount() == prevEvent.getPointerCount()) {
			if (curEvent.getPointerCount() == 1) {
				onePointTransform(selectedStrokes, selRect, prevEvent, curEvent);
			} else {
				twoPointTransform(selectedStrokes, selRect, prevEvent, curEvent);
			}
		}
		
		prevEvent = MotionEvent.obtain(curEvent);
		
		if (curEvent.getActionMasked() == MotionEvent.ACTION_UP) {
			applyTransToNote();
			prevEvent = null;
		}
		
		return true;
	}
	

	
	private static void onePointTransform (final List<WorkingStroke> w, final RectF boundingRect, final MotionEvent prevEvent, final MotionEvent curEvent) {
		final float dx = curEvent.getX() - prevEvent.getX();
		final float dy = curEvent.getY() - prevEvent.getY();
		
		for (WorkingStroke s : w) {
			for (PointF p : s.poly) {
				p.x += dx;
				p.y += dy;
			}
		}
		
		boundingRect.offset(dx, dy);
	}
	
	private static void twoPointTransform (final List<WorkingStroke> w, final RectF boundingRect, final MotionEvent prevEvent, final MotionEvent curEvent) {
		final float prevX = (prevEvent.getX(0)+prevEvent.getX(1))/2;
		final float prevY = (prevEvent.getY(0)+prevEvent.getY(1))/2;
		final float curX = (curEvent.getX(0)+curEvent.getX(1))/2;
		final float curY = (curEvent.getY(0)+curEvent.getY(1))/2;
		
		final float prevDist = MiscGeom.distance(prevEvent.getX(0), prevEvent.getY(0), prevEvent.getX(1), prevEvent.getY(1));
		final float curDist = MiscGeom.distance(curEvent.getX(0), curEvent.getY(0), curEvent.getX(1), curEvent.getY(1));
		final float dStretch = curDist / prevDist;
		
		for (WorkingStroke s : w) {
			for (PointF p : s.poly) {
				p.x = (p.x-prevX) * dStretch + curX;
				p.y = (p.y-prevY) * dStretch + curY;
			}
		}
		
		boundingRect.left = (boundingRect.left - prevX) * dStretch + curX;
		boundingRect.right = (boundingRect.right - prevX) * dStretch + curX;
		boundingRect.top = (boundingRect.top - prevY) * dStretch + curY;
		boundingRect.bottom = (boundingRect.bottom - prevY) * dStretch + curY;
	}

	
	private void resetTrans () {
		selRect = null;
		prevEvent = null;
		selectedStrokes.clear();
	}

	public RectF getDirtyRect() {
		return null;
	}
	
	public void draw (Canvas c) {
		drawNotSelectedStrokes(c);
		drawSelectedStrokes(c);
		drawSelectionRect(c);
		drawLasso(c);
	}
	
	private void drawNotSelectedStrokes (final Canvas c) {
		if (selectedStrokes.size() > 0) {
			int prevIndex = -1;
			
			for (WorkingStroke curStroke : selectedStrokes) {
				for (int i = prevIndex+1; i < curStroke.index; i++) {
					currentStrokes.get(i).draw(c);
				}
				prevIndex = curStroke.index;
			}
			
			for (int i = prevIndex+1; i < currentStrokes.size(); i++) {
				currentStrokes.get(i).draw(c);
			}
		} else {
			for (Stroke s : currentStrokes) {
				s.draw(c);
			}
		}
	}
	
	private void drawSelectedStrokes (final Canvas c) {
		selBorderPaint.setStrokeWidth(mConverter.screenToCanvDist(8.0f));
		
		for (WorkingStroke s : selectedStrokes) {
			selPath.reset();
			selPath.moveTo(s.poly.get(0).x, s.poly.get(0).y);
			for (PointF p : s.poly) {
				selPath.lineTo(p.x, p.y);
			}
			selPath.lineTo(s.poly.get(0).x, s.poly.get(0).y);
			
			c.drawPath(selPath, selBorderPaint);
			selBodyPaint.setColor(s.color | 0xFF000000);
			c.drawPath(selPath, selBodyPaint);
		}
	}
	
	private void drawSelectionRect (final Canvas c) {
		if (selRect != null) {
			selRectPaint.setStrokeWidth(mConverter.screenToCanvDist(3.0f));
			selRectPaint.setPathEffect(new DashPathEffect(new float[] {mConverter.screenToCanvDist(12.0f), mConverter.screenToCanvDist(7.0f)}, 0));
			c.drawRect(selRect, selRectPaint);
		}
	}
	
	private void drawLasso (final Canvas c) {
		if (lassoPoints.size() > 0) {
			lassoBorderPaint.setStrokeWidth(mConverter.screenToCanvDist(3.0f));
			lassoShadePaint.setStrokeWidth(mConverter.screenToCanvDist(3.0f));
			lassoBorderPaint.setPathEffect(new DashPathEffect(new float[] {mConverter.screenToCanvDist(12.0f), mConverter.screenToCanvDist(7.0f)}, 0));
			
			lassoPath.reset();
			lassoPath.moveTo(lassoPoints.get(0).x, lassoPoints.get(0).y);
			for (Point p : lassoPoints) {
				lassoPath.lineTo(p.x, p.y);
			}
			
			c.drawPath(lassoPath, lassoShadePaint);
			c.drawPath(lassoPath, lassoBorderPaint);
		}
	}
	
	public void undo () {
		currentStrokes = mNote.getInkLayer();
		lassoPoints.clear();
		selectedStrokes.clear();
		selRect = null;
	}
	
	public void redo () {
		currentStrokes = mNote.getInkLayer();
		lassoPoints.clear();
		selectedStrokes.clear();
		selRect = null;
	}
	
	private void applyTransToNote () {
		if (selectedStrokes.size() == 0) return;
		
		setTutorialToOff();
		ArrayList<Note.Action> actions = new ArrayList<Note.Action>(selectedStrokes.size()*2);
		
		// Add the moved strokes to the front
		int offsetCounter = 0;
		for (WorkingStroke s : selectedStrokes) {
			ArrayList<Point> finalPoly = new ArrayList<Point>(s.poly.size());
			
			for (PointF p : s.poly) {
				finalPoly.add(new Point(p.x, p.y));
			}
			
			Stroke transStroke = new Stroke(s.color, finalPoly);
			actions.add(new Action(transStroke, currentStrokes.size()+offsetCounter, true));
			offsetCounter++;
		}
		
		// Delete the strokes from where they were
		for (int i = selectedStrokes.size()-1; i >= 0; i--) {
			final int index = selectedStrokes.get(i).index;
			actions.add(new Action(currentStrokes.get(index), index, false));
		}

		// Update the indexes of selectedStrokes
		for (int i = 0; i < selectedStrokes.size(); i++) {
			final int newIndex = currentStrokes.size()-selectedStrokes.size()+i;
			selectedStrokes.get(i).index = newIndex;
		}
		
		mNote.performActions(actions);
	}
	
	
	private static RectF calcAABoundingBox (List<PointF> points) {
		RectF box = new RectF(points.get(0).x, points.get(0).y, points.get(0).x, points.get(0).y);
		
		for (PointF p : points) {
			if (p.x < box.left) {
				box.left = p.x;
			} else if (p.x > box.right) {
				box.right = p.x;
			}
			
			if (p.y < box.top) {
				box.top = p.y;
			} else if (p.y > box.bottom) {
				box.bottom = p.y;
			}
		}
		
		return box;
	}
	
	
	// *************************************** Tutorial Methods ************************************
	
	private void triggerTransformationTutorial () {
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
	
	
	
	private static class WorkingStroke {
		public int index;
		public List<PointF> poly;
		public final int color;
		
		public WorkingStroke (final int index, final List<Point> poly, final int color) {
			this.index = index;
			this.color = color;
			
			this.poly = new ArrayList<PointF>(poly.size());
			for (Point p : poly) {
				this.poly.add(new PointF(p.x, p.y));
			}
		}
	}
}
