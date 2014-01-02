package com.freehand.editor.canvas;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.freehand.editor.canvas.Note.Action;
import com.freehand.ink.MiscGeom;
import com.freehand.ink.Point;
import com.freehand.ink.Stroke;

public class Pen implements ITool {
	private static final int ARC_RES = 20;
	
	private final Note mNote;
	private final ICanvScreenConverter mConverter;
	private View invalidator;
	
	private final float pressureSensitivity;
	private final boolean capDrawing;
	
	private final float baseSize;
	private final int color;
	
	private ArrayList<Point> points = new ArrayList<Point>(1000);
	private ArrayList<Float> sizes = new ArrayList<Float>(1000);
	
	private Path path = new Path();
	private Paint paint = new Paint();
	
	
	private List<Point> cap = new ArrayList<Point>();
	
	private int containingIndex = -1;
	
	private boolean ignoringCurrentMe = false;
	
	private RectF dirtyRect = null;
	
	private LinkedList<Point> poly = new LinkedList<Point>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 2622708965156041658L;

		@Override
		public void addFirst(Point toAdd) {
			addPointToDirtyRect(toAdd);
			super.addFirst(toAdd);
		}
		
		@Override
		public void addLast(Point toAdd) {
			addPointToDirtyRect(toAdd);
			super.addLast(toAdd);
		}
	};
	
	
	public Pen (Note newNote, ICanvScreenConverter newConverter, View invalidator, float pressureSensitivity, int penColor, float penSize, boolean capDrawing) {
		mNote = newNote;
		mConverter = newConverter;
		this.invalidator = invalidator;
		this.pressureSensitivity = pressureSensitivity;
		this.capDrawing = capDrawing;
		color = penColor;
		baseSize = penSize;
		
		path.setFillType(Path.FillType.WINDING);
		
		paint.setColor(color);
		paint.setStyle(Paint.Style.FILL);
		paint.setAntiAlias(true);
	}
	
	
	//**************************** ITool stuff *****************************
	
	public boolean onMotionEvent(MotionEvent e) {
		
		if (e.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER && capDrawing == false) {
			return false;
		}
		
		if (e.getAction() == MotionEvent.ACTION_UP) {
			if (ignoringCurrentMe == false) {
				final Point p = new Point(e.getX(), e.getY());
				processPoint(p, e.getPressure());
			}
			
			if (invalidator != null) {
				invalidator.invalidate(mConverter.canvRectToScreenRect(getDirtyRect()));
			}
			addStrokeToNote();
			clear();
			return true;
		}
		
		if (ignoringCurrentMe) return false;
		if (e.getPointerCount() > 1) {
			clear();
			ignoringCurrentMe = true;
			return false;
		}
		
		if (e.getAction() == MotionEvent.ACTION_MOVE) {
			for (int i = 0; i < e.getHistorySize(); i++) {
				final Point p = new Point(e.getHistoricalX(i), e.getHistoricalY(i));
				processPoint(p, e.getHistoricalPressure(i));
			}
			final Point p = new Point(e.getX(), e.getY());
			processPoint(p, e.getPressure());
		}
		
		return true;
	}
	
	private void clear () {
		points.clear();
		sizes.clear();
		poly.clear();
		cap.clear();
		containingIndex = -1;
		ignoringCurrentMe = false;
	}

	private void processPoint (final Point p, final float pressure) {
		final float newSize = baseSize * this.scalePressure(pressure);
		
		if (points.size() > 0 && newSize == sizes.get(sizes.size()-1) &&
			MiscGeom.distance(p, points.get(points.size()-1)) <= mConverter.screenToCanvDist(2.0f)) {
			return;
		}
		
		points.add(p);
		sizes.add(newSize);
		
		if (points.size() == 2) {
			startPoly();
		} else if (points.size() > 2) {
			addToPoly();
		}
	}

	private void addStrokeToNote () {
		List<Point> finalPoly = getFinalPoly();
		if (finalPoly.size() >= 3) {
			ArrayList<Action> action = new ArrayList<Action>(1);
			action.add(new Action(new Stroke(color, finalPoly), mNote.getInkLayer().size(), true));
			mNote.performActions(action);
		}
	}

	public RectF getDirtyRect() {
		updateCap();
		RectF toReturn = dirtyRect;
		dirtyRect = new RectF();
		
		if (poly.size() >= 1) {
			addPointToDirtyRect(poly.getFirst());
			addPointToDirtyRect(poly.getLast());
		}
		
		return toReturn;
	}
	
	public void draw (Canvas c) {
		for (Stroke s : mNote.getInkLayer()) {
			s.draw(c);
		}
		
		if (poly != null && poly.size() >= 3) {
			updateCap();
			path.reset();
			path.moveTo(poly.get(0).x, poly.get(0).y);
			for (Point p : poly) {
				path.lineTo(p.x, p.y);
			}
			for (Point p : cap) {
				path.lineTo(p.x, p.y);
			}
			c.drawPath(path, paint);
		}
	}
	
	public void undo() { /* blank */ }
	public void redo() { /* blank */ }
	
	//**************************************** Utility Methods ************************************************
	
	
	private List<Point> getFinalPoly () {
		updateCap();
		List<Point> finalPoly = new ArrayList<Point>(poly.size()+cap.size());
		
		for (Point p : poly) {
			finalPoly.add(p);
		}
		
		for (Point p : cap) {
			finalPoly.add(p);
		}
		
		return finalPoly;
	}
	
	private void startPoly () {
		Point[] tangentPoints = MiscGeom.calcExternalBitangentPoints(points.get(0), sizes.get(0), points.get(1), sizes.get(1));
		if (tangentPoints != null) {
			startNewStroke(tangentPoints, points.get(0), sizes.get(0));
		} else {
			if (sizes.get(0) >= sizes.get(1)) {
				// first contains second
				containingIndex = 0;
				LinkedList<Point> start = MiscGeom.getLinkedCircularPoly(points.get(0), sizes.get(0));
				for (int i = 0; i < start.size()/2; i++) {
					poly.addFirst(start.get(i));
				}
			} else {
				// second contains first
				poly.clear();
				points.remove(0);
				sizes.remove(0);
			}
		}
	}
	
	private void addToPoly () {
		if (containingIndex >= 0) {
			breakContainment();
			return;
		}
		
		Point[] tangentPoints = MiscGeom.calcExternalBitangentPoints(points.get(points.size()-2),
			sizes.get(sizes.size()-2), points.get(points.size()-1), sizes.get(sizes.size()-1));
		
		if (tangentPoints != null) {
			addToLhs(tangentPoints[0], tangentPoints[1], points.get(points.size()-2), sizes.get(sizes.size()-2), points.get(points.size()-1), sizes.get(sizes.size()-1));
			addToRhs(tangentPoints[2], tangentPoints[3], points.get(points.size()-2), sizes.get(sizes.size()-2), points.get(points.size()-1), sizes.get(sizes.size()-1));
		} else if (sizes.get(sizes.size()-1) <= sizes.get(sizes.size()-2)) {	// new contained by old -> Start containment
			containingIndex = sizes.size()-2;
		} else {														// old contained by new -> backtrack
			oldCircleContainedByNewCircle(poly, points, sizes);
		}
	}
	
	private static void oldCircleContainedByNewCircle (final LinkedList<Point> poly, final List<Point> points, final List<Float> sizes) {
		Point[] offsets = null;
		for (int i = points.size()-1; i >= 0; i--) {
			if (points.get(points.size()-1).equals(points.get(i)) == false) {
				offsets = MiscGeom.calcPerpOffset(points.get(points.size()-1), points.get(i), sizes.get(sizes.size()-1)*2);
				break;
			}
		}
		if (offsets == null) return;
		
		poly.addFirst(poly.getLast());
		poly.addLast(poly.get(1));
		
		List<Point> arc = MiscGeom.approximateCircularArc(points.get(points.size()-1), sizes.get(sizes.size()-1), true, offsets[1], offsets[0], ARC_RES);
		
		for (int i = arc.size()/2 - 1; i < arc.size(); i++) {
			poly.addFirst(arc.get(i));
		}
		
		for (int i = arc.size()/2; i >= 0; i--) {
			poly.addLast(arc.get(i));
		}
	}
	
	private void breakContainment () {
		if (MiscGeom.checkCircleContainment(points.get(containingIndex), sizes.get(containingIndex), points.get(points.size()-1), sizes.get(sizes.size()-1)) == true) {
			return;
		}
		
		Point[] tanPts = MiscGeom.calcExternalBitangentPoints(points.get(points.size()-2), sizes.get(sizes.size()-2), points.get(points.size()-1), sizes.get(sizes.size()-1));
		Point[] tanIntPts = {null, null};
		if (tanPts != null) {
			tanIntPts[0] = MiscGeom.circleSegmentIntersection(points.get(containingIndex), sizes.get(containingIndex), tanPts[0], tanPts[1])[0];
			tanIntPts[1] = MiscGeom.circleSegmentIntersection(points.get(containingIndex), sizes.get(containingIndex), tanPts[2], tanPts[3])[0];
		}
		Point[] circIntPts = MiscGeom.circleCircleIntersection(points.get(containingIndex), sizes.get(containingIndex), points.get(points.size()-1), sizes.get(sizes.size()-1));
		
		// Left hand side
		if (tanIntPts[0] != null) {
			List<Point> left = MiscGeom.approximateCircularArc(points.get(containingIndex), sizes.get(containingIndex), true, poly.getFirst(), tanIntPts[0], ARC_RES);
			for (Point p : left) {
				poly.addFirst(p);
			}
			poly.addFirst(tanIntPts[0]);
			poly.addFirst(tanPts[1]);
		} else if (circIntPts != null) {
			List<Point> left = MiscGeom.approximateCircularArc(points.get(containingIndex), sizes.get(containingIndex), true, poly.getFirst(), circIntPts[0], ARC_RES);
			for (Point p : left) {
				poly.addFirst(p);
			}
			poly.addFirst(circIntPts[0]);
		} else {
			Log.d("PEN", "containment broken but no left handed intersections");
			return;
		}
		
		// Right hand side
		if (tanIntPts[1] != null) {
			List<Point> right = MiscGeom.approximateCircularArc(points.get(containingIndex), sizes.get(containingIndex), false, poly.getLast(), tanIntPts[1], ARC_RES);
			for (Point p : right) {
				poly.addLast(p);
			}
			poly.addLast(tanIntPts[1]);
			poly.addLast(tanPts[3]);
		} else if (circIntPts != null) {
			List<Point> right = MiscGeom.approximateCircularArc(points.get(containingIndex), sizes.get(containingIndex), false, poly.getLast(), circIntPts[1], ARC_RES);
			for (Point p : right) {
				poly.addLast(p);
			}
			poly.addLast(circIntPts[1]);
		} else {
			Log.d("PEN", "containment broken but no right handed intersections");
			return;
		}
		
		containingIndex = -1;
	}
	
	private void updateCap () {
		
		//Log.d("PEN", Integer.toString(points.size()) + "     " + Integer.toString(containingIndex));
		
		if (points.size() == 1) {
			cap = MiscGeom.getLinkedCircularPoly(points.get(0), sizes.get(0));
		} else if (containingIndex >= 0) {
			cap = MiscGeom.approximateCircularArc(points.get(containingIndex), sizes.get(containingIndex), false, poly.getLast(), poly.getFirst(), ARC_RES);
			//cap = new ArrayList<Point>(0);
		} else if (points.size() > 1) {
			cap = MiscGeom.approximateCircularArc(points.get(points.size()-1), sizes.get(sizes.size()-1), false, poly.getLast(), poly.getFirst(), ARC_RES);
		}
		
		for (Point p : cap) {
			addPointToDirtyRect(p);
		}
	}
	
	private void startNewStroke (Point[] tangentPoints, Point prevPoint, float prevSize) {
		List<Point> frontCap = MiscGeom.approximateCircularArc(prevPoint, prevSize, false, tangentPoints[0], tangentPoints[2], ARC_RES);
		for (Point p : frontCap) {
			poly.addLast(p);
		}
		
		poly.addFirst(tangentPoints[0]);
		poly.addFirst(tangentPoints[1]);
		poly.addLast(tangentPoints[2]);
		poly.addLast(tangentPoints[3]);
	}
	
	private void addToLhs (Point tail, Point head, Point joinCenter, float joinRad, Point newCenter, float newRad) {
		float handedness = MiscGeom.cross(poly.get(0), poly.get(1), head, poly.get(1));
		if (handedness <= 0) {
			List<Point> join = MiscGeom.approximateCircularArc(joinCenter, joinRad, true, poly.getFirst(), tail, ARC_RES);
			for (Point p : join) {
				poly.addFirst(p);
			}
			poly.addFirst(tail);
			poly.addFirst(head);
		} else {
			Point intersection = MiscGeom.calcIntersection(poly.get(0), poly.get(1), head, tail);
			if (intersection != null) {
				poly.removeFirst();
				poly.addFirst(intersection);
				poly.addFirst(head);
			} else {
				poly.addFirst(tail);
				poly.addFirst(head);
			}
		}
	}
	
	private void addToRhs (Point tail, Point head, Point joinCenter, float joinRad, Point newCenter, float newRad) {
		float handedness = MiscGeom.cross(poly.get(poly.size()-1), poly.get(poly.size()-2), head, poly.get(poly.size()-2));
		if (handedness >= 0) {
			List<Point> join = MiscGeom.approximateCircularArc(joinCenter, joinRad, false, poly.getLast(), tail, ARC_RES);
			for (Point p : join) {
				poly.addLast(p);
			}
			poly.addLast(tail);
			poly.addLast(head);
		} else {
			Point intersection = MiscGeom.calcIntersection(poly.get(poly.size()-1), poly.get(poly.size()-2), head, tail);
			
			if (intersection != null) {
				poly.removeLast();
				poly.addLast(intersection);
				poly.addLast(head);
			} else {
				poly.addLast(tail);
				poly.addLast(head);
			}
		}
	}
	
	private float scalePressure (float pressure) {
		return 1.0f - pressureSensitivity + pressure*pressureSensitivity;
	}

	
	private void addPointToDirtyRect (Point p) {
		if (dirtyRect != null) {
			dirtyRect.union(p.x, p.y);
		} else {
			dirtyRect = new RectF(p.x, p.y, p.x+0.001f, p.y+0.001f);
		}
	}
}