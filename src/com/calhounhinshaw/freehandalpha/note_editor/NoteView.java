package com.calhounhinshaw.freehandalpha.note_editor;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import com.calhounhinshaw.freehandalpha.note_orginazion.INoteHierarchyItem;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

public class NoteView extends View {
	private static final int WAITING = 0;
	private static final int WORKING = 1;
	private static final int MOVING = 2;
	
	private static final int DRAWING = 0;
	private static final int ERASING = 1;
	private static final int SELECTING = 2;
	
	// The state of the view
	private int mTouchState = WAITING;
	private int mWorkState = DRAWING;
	
	// The variables the paint new strokes are being added to the note with are defined by
	private int currentPaintColor = Color.BLUE;
	private float currentPaintSize = 4.5F;
	
	// The amount values on the screen are multiplied by to keep the data in the note consistent with the amount of zoom
	private float zoomMultiplier = 1;
	
	// The x and y values of the upper left corner of the screen
	private float windowX = 0;
	private float windowY = 0;
	
	// The data that makes up the stroke currently being drawn
	private Vector<PointF> currentStroke = new Vector<PointF>();
	
	// the data for the note
	private Note mNote;
	

	
	private float eraserSizeOnScreen = 30.0F;
	private float oldX;
	private float oldY;
	
	private LinkedList<PointF[]> toAdd = new LinkedList<PointF[]>();
	
	private float lastScreenX = 0;
	private float lastScreenY = 0;
	
	
	private NoteEditorPresenter mPresenter;
	
	private LinkedList<Long> times = new LinkedList<Long>();
	private LinkedList<Float> xs = new LinkedList<Float>();
	private LinkedList<Float> ys = new LinkedList<Float>();
	private LinkedList<Float> pressures = new LinkedList<Float>();
	
//************************************* Constructors ************************************************
	private void init() {
		mNote = new Note();
	}

	public NoteView(Context context) {
		super(context);
		init();
	}
	
	public NoteView (Context context, AttributeSet attrs) {
		super (context, attrs);
		init();
	}
	
	public NoteView (Context context, AttributeSet attrs, int defStyle) {
		super (context, attrs, defStyle);
		init();
	}

//---------------------------------------------------------------------------------------	
	

	public void setPresenter (NoteEditorPresenter newPresenter) {
		mPresenter = newPresenter;
	}



//****************************** Touch Handling Methods *********************************************

	private float previousX = Float.NaN;
	private float previousY = Float.NaN;
	private float previousDistance = Float.NaN;
	
	private boolean canDraw = true;
	
	

	private void initMoveVars (MotionEvent e) {
		previousX = (e.getX(0)+e.getX(1)) / 2;
		previousY = (e.getY(0)+e.getY(1)) / 2;
		previousDistance = distance(e.getX(0), e.getY(0), e.getX(1), e.getY(1));
	}
	
	
	
	
	@Override
	public boolean onTouchEvent (MotionEvent event) {
		
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_POINTER_UP: {
				if (event.getPointerCount() >= 2) {
					initMoveVars(event);
				} else {
					previousX = Float.NaN;
					previousY = Float.NaN;
					previousDistance = Float.NaN;
				}
				
				break;
			}
			
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP: {
				previousX = Float.NaN;
				previousY = Float.NaN;
				previousDistance = Float.NaN;
				
				break;
			}
		}
		
		// Pen Event
		if (event.getPointerCount() == 1 && canDraw == true) {
			times.clear();
			xs.clear();
			ys.clear();
			pressures.clear();
			
			for(int i = 0; i < event.getHistorySize(); i++) {
				times.add(event.getHistoricalEventTime(i));
				xs.add(event.getHistoricalX(i));
				ys.add(event.getHistoricalY(i));
				pressures.add(event.getHistoricalPressure(i));
			}
			
			times.add(event.getEventTime());
			xs.add(event.getX());
			ys.add(event.getY());
			pressures.add(event.getPressure());
			
			mPresenter.penAction(times, xs, ys, pressures);
			
		// Pan/Zoom event
		} else if (event.getPointerCount() >= 2) {
			
			float currentDistance = distance(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
			float currentX = (event.getX(0)+event.getX(1)) / 2;
			float currentY = (event.getY(0)+event.getY(1)) / 2;
			
			if (Float.isNaN(previousX) == false && Float.isNaN(previousY) == false && Float.isNaN(previousDistance) == false) {
				Log.d("PEN", "currentX: " + Float.toString(currentX) + "  currentY: " + Float.toString(currentY) + "  currentDistance: " + Float.toString(currentDistance));
				Log.d("PEN", "previousX: " + Float.toString(previousX) + "  previousY: " + Float.toString(previousY) + "  previousDistance: " + Float.toString(previousDistance));
				
				float dZoom = currentDistance / previousDistance;
				
				float dx = previousX - currentX/dZoom;
				float dy = previousY - currentY/dZoom;
				
				mPresenter.panZoomAction(currentX, currentY, dx, dy, dZoom);
			}
			
			previousDistance = currentDistance;
			previousX = currentX;
			previousY = currentY;
		}
		
		// Store and update all of the information needed to calculate the deltas for panning and zooming
		switch (event.getActionMasked()) {			
			case MotionEvent.ACTION_POINTER_DOWN: {
				if (event.getPointerCount() == 1) {
					initMoveVars(event);
				}
				
				canDraw = false;
				break;
			}
			
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP: {
				previousX = Float.NaN;
				previousY = Float.NaN;
				previousDistance = Float.NaN;
				canDraw = true;
				
				break;
			}
		}
		
		invalidate();
		return true;
	}
	
	
	@Override
	public boolean onHoverEvent (MotionEvent event) {
		
		times.clear();
		xs.clear();
		ys.clear();
		pressures.clear();
		
		for(int i = 0; i < event.getHistorySize(); i++) {
			times.add(event.getHistoricalEventTime(i));
			xs.add(event.getHistoricalX(i));
			ys.add(event.getHistoricalY(i));
			pressures.add(-1.0f);
		}
		
		times.add(event.getEventTime());
		xs.add(event.getX());
		ys.add(event.getY());//
		pressures.add(-1.0f);
		
		mPresenter.penAction(times, xs, ys, pressures);
		
		return true;
	}
	
//	private void draw (MotionEvent event) {
//		Rect boundingRect = new Rect((int) lastScreenX, (int) lastScreenY, (int) lastScreenX, (int) lastScreenY);;
//		
//		switch (event.getAction()) {
//		case MotionEvent.ACTION_DOWN:
//			synchronized (currentStroke) {
//				currentStroke = new Vector<PointF>();
//			}
//			
//			currentStroke.add(new PointF(windowX + event.getX()*zoomMultiplier, windowY + zoomMultiplier * event.getY()));
//			
//			lastScreenX = event.getX();
//			lastScreenY = event.getY();
//			break;
//		case MotionEvent.ACTION_MOVE:
//			for (int i = 0; i < event.getHistorySize(); i++) {
//				float actualX = (event.getHistoricalX(i)*zoomMultiplier) + windowX;
//				float actualY = (event.getHistoricalY(i)*zoomMultiplier) + windowY;
//				
//				float lastX = (currentStroke.get(currentStroke.size()-1).x - windowX)/zoomMultiplier;
//				float lastY = (currentStroke.get(currentStroke.size()-1).y - windowY)/zoomMultiplier;
//				
//				lastScreenX = event.getHistoricalX(i);
//				lastScreenY = event.getHistoricalY(i);
//				
//				boundingRect.union((int) event.getHistoricalX(i), (int) event.getHistoricalY(i));
//				
//				if (event.getHistoricalX(i)-lastX >= 3 || event.getHistoricalX(i)-lastX <= -3 || event.getHistoricalY(i)-lastY >= 3 || event.getHistoricalY(i)-lastY <= -3) {
//					currentStroke.add(new PointF(actualX, actualY));
//				}
//			}
//			
//			float actualX = (event.getX()*zoomMultiplier) + windowX;
//			float actualY = (event.getY()*zoomMultiplier) + windowY;
//			
//			float lastX = (currentStroke.get(currentStroke.size()-1).x - windowX)/zoomMultiplier;
//			float lastY = (currentStroke.get(currentStroke.size()-1).y - windowY)/zoomMultiplier;
//			
//			lastScreenX = event.getX();
//			lastScreenY = event.getY();
//			
//			boundingRect.union((int) event.getX(), (int) event.getY());
//			
//			if (event.getX()-lastX >= 3 || event.getX()-lastX <= -3 || event.getY()-lastY >= 3 || event.getY()-lastY <= -3) {
//				currentStroke.add(new PointF(actualX, actualY));
//			}
//			
//			
//			boundingRect.inset((int) (-1*currentPaintSize/zoomMultiplier) - 10, (int) (-1*currentPaintSize/zoomMultiplier) - 10);
//			invalidate(boundingRect);
//			
//			
//			break;
//		case MotionEvent.ACTION_CANCEL:
//		case MotionEvent.ACTION_UP:			
//			currentStroke.add(new PointF(windowX + zoomMultiplier * event.getX(), windowY + zoomMultiplier * event.getY()));
//			boundingRect.union((int) (windowX + zoomMultiplier * event.getX()), (int) (windowY + zoomMultiplier * event.getY()));
//			
//			if (currentStroke.size() ==2) {
//				currentStroke.add(new PointF(windowX + zoomMultiplier * event.getX() + 0.001F, windowY + zoomMultiplier * event.getY() + 0.001F));
//				boundingRect.union((int) event.getX(), (int) event.getY());
//			}
//			
//			synchronized (toAdd) {
//				toAdd.offer(currentStroke.toArray(new PointF[0]));
//			}
//			
//			synchronized (currentStroke) {
//				currentStroke = new Vector<PointF>();
//			}
//
//			
//			mTouchState = WAITING;
//			
//			boundingRect.inset((int) (-1*currentPaintSize/zoomMultiplier) - 10, (int) (-1*currentPaintSize/zoomMultiplier) - 10);
//			invalidate(boundingRect);
//			
//			break;
//		}
//	}
//	
//	private void erase(MotionEvent event) {
//		switch (event.getAction()) {
//		case MotionEvent.ACTION_DOWN:
//			mNote.initializeErasure();
//			
//			oldX = event.getX();
//			oldY = event.getY();
//			break;
//		case MotionEvent.ACTION_MOVE:
//			mNote.eraseLineSegment(eraserSizeOnScreen*zoomMultiplier, windowX + zoomMultiplier *oldX, windowY + zoomMultiplier *oldY, windowX + zoomMultiplier * event.getX(), windowY + zoomMultiplier * event.getY());
//			oldX = event.getX();
//			oldY = event.getY();
//			
//			break;
//		case MotionEvent.ACTION_CANCEL:
//		case MotionEvent.ACTION_UP:
//			mNote.eraseLineSegment(eraserSizeOnScreen*zoomMultiplier, windowX + zoomMultiplier *oldX, windowY + zoomMultiplier *oldY, windowX + zoomMultiplier * event.getX(), windowY + zoomMultiplier * event.getY());
//			
//			mNote.finilizeErasure();
//			
//			mTouchState = WAITING;
//			break;
//		}
//		
//		invalidate();
//	}
//	
//	private void select(MotionEvent event) {
//Rect boundingRect = new Rect((int) lastScreenX, (int) lastScreenY, (int) lastScreenX, (int) lastScreenY);;
//		
//		switch (event.getAction()) {
//		case MotionEvent.ACTION_DOWN:
//			synchronized (currentStroke) {
//				currentStroke = new Vector<PointF>();
//			}
//			
//			currentStroke.add(new PointF(windowX + event.getX()*zoomMultiplier, windowY + zoomMultiplier * event.getY()));
//			break;
//		case MotionEvent.ACTION_MOVE:
//			for (int i = 0; i < event.getHistorySize(); i++) {
//				float actualX = (event.getHistoricalX(i)*zoomMultiplier) + windowX;
//				float actualY = (event.getHistoricalY(i)*zoomMultiplier) + windowY;
//				
//				float lastX = (currentStroke.get(currentStroke.size()-1).x - windowX)/zoomMultiplier;
//				float lastY = (currentStroke.get(currentStroke.size()-1).y - windowY)/zoomMultiplier;
//				
//				lastScreenX = event.getHistoricalX(i);
//				lastScreenY = event.getHistoricalY(i);
//				
//				boundingRect.union((int) event.getHistoricalX(i), (int) event.getHistoricalY(i));
//				
//				if (event.getHistoricalX(i)-lastX >= 3 || event.getHistoricalX(i)-lastX <= -3 || event.getHistoricalY(i)-lastY >= 3 || event.getHistoricalY(i)-lastY <= -3) {
//					currentStroke.add(new PointF(actualX, actualY));
//				}
//			}
//			
//			boundingRect.inset((int) (-1*currentPaintSize/zoomMultiplier) - 10, (int) (-1*currentPaintSize/zoomMultiplier) - 10);
//			invalidate(boundingRect);
//			
//			
//			break;
//		case MotionEvent.ACTION_CANCEL:
//		case MotionEvent.ACTION_UP:			
//			currentStroke.add(new PointF(windowX + zoomMultiplier * event.getX(), windowY + zoomMultiplier * event.getY()));
//			
//			if (currentStroke.size() ==2) {
//				currentStroke.add(new PointF(windowX + zoomMultiplier * event.getX() + 0.001F, windowY + zoomMultiplier * event.getY() + 0.001F));
//			}
//			
//			mNote.select(currentStroke.toArray(new PointF[0]));
//			
//			synchronized (currentStroke) {
//				currentStroke = new Vector<PointF>();
//			}
//
//			mTouchState = WAITING;
//			
//			invalidate();
//			
//			break;
//		}
//	}
//	
//	private void moveSelection(MotionEvent event) {
//		if (event.getPointerCount() == 2) {
//			switch (event.getAction()) {
//			case MotionEvent.ACTION_POINTER_2_DOWN:
//				mNote.initalizeMove();
//				
//				oldX = windowX + event.getX()*zoomMultiplier;
//				oldY = windowY + event.getY()*zoomMultiplier;
//				initialDistance = distance(event.getX(0), event.getX(1), event.getY(0), event.getY(1));
//				break;
//			case MotionEvent.ACTION_MOVE:
//			case MotionEvent.ACTION_UP:
//				float currentDistance = distance(event.getX(0), event.getX(1), event.getY(0), event.getY(1));
//				float dZoom = currentDistance/initialDistance;
//				float currentX = windowX + event.getX()*zoomMultiplier;
//				float currentY = windowY + event.getY()*zoomMultiplier;
//				mNote.moveSelected(currentX - oldX, currentY - oldY, dZoom, currentX, currentY);
//				oldX = currentX;
//				oldY = currentY;
//				initialDistance = currentDistance;
//				break;
//			}
//		}
//		
//		if (event.getAction() == MotionEvent.ACTION_UP) {
//			mTouchState = WAITING;
//			mNote.finalizeMove();
//		}
//		
//		invalidate();
//	}
//	
//	private void move (MotionEvent event) {
//		switch (event.getAction()) {
//		case MotionEvent.ACTION_POINTER_2_DOWN:
//			if (event.getPointerCount() == 2) {
//				initMove(event);
//			}
//			
//			break;
//		case MotionEvent.ACTION_MOVE:
//		case MotionEvent.ACTION_UP:
//			if (event.getPointerCount() == 2) {
//				float newZoomMultiplier = initialZoom*(initialDistance/distance(event.getX(0), event.getX(1), event.getY(0), event.getY(1)));
//
//				if (newZoomMultiplier >= 0.10f && newZoomMultiplier <= 10.0f) {
//					zoomMultiplier = newZoomMultiplier;
//				} else if (newZoomMultiplier < 0.10f) {
//					zoomMultiplier = 0.10f;
//					initMove(event);
//				} else {
//					zoomMultiplier = 10.0f;
//					initMove(event);
//				}
//				
//				
//				float newWindowX = initialX - event.getX(0)*zoomMultiplier;
//				float newWindowY = initialY - event.getY(0)*zoomMultiplier;
//				
//				if (newWindowX <= 1000000f && newWindowX >= -1000000f) {
//					windowX = newWindowX;
//				} else if (newWindowX > 1000000f) {
//					windowX = 1000000f;
//					initMove(event);
//					Toast.makeText(getContext(), "Edge of canvas reached.", Toast.LENGTH_SHORT).show();
//				} else {
//					windowX = -1000000f;
//					initMove(event);
//					Toast.makeText(getContext(), "Edge of canvas reached.", Toast.LENGTH_SHORT).show();
//				}
//				
//				if (newWindowY <= 1000000f && newWindowY >= -1000000f) {
//					windowY = newWindowY;
//				} else if (newWindowY > 1000000f) {
//					windowY = 1000000f;
//					initMove(event);
//					Toast.makeText(getContext(), "Edge of canvas reached.", Toast.LENGTH_SHORT).show();
//				} else {
//					windowY = -1000000f;
//					initMove(event);
//					Toast.makeText(getContext(), "Edge of canvas reached.", Toast.LENGTH_SHORT).show();
//				}
//			}
//			
//			break;
//		}
//		
//		if (event.getAction() == MotionEvent.ACTION_UP) {
//			mTouchState = WAITING;
//		}
//		
//		invalidate();
//	}
//	
//	private void initMove (MotionEvent event) {
//		initialX = windowX + event.getX(0)*zoomMultiplier;
//		initialY = windowY + event.getY(0)*zoomMultiplier;
//		initialZoom = zoomMultiplier;
//		initialDistance = distance(event.getX(0), event.getX(1), event.getY(0), event.getY(1));
//	}
	
	private float distance (float x1, float y1, float x2, float y2) {
		return (float)Math.sqrt(Math.pow((x1-x2), 2) + Math.pow((y1-y2), 2));
	}
	
//-------------------------------------------------------------------------------------------------------------
	
	
//********************************* onDraw Methods *************************************************************
	@Override
	public void onDraw (Canvas c) {
//		mNote.drawNote(c, windowX, windowY, 1/zoomMultiplier);
//		
//		if (mWorkState != ERASING) {
//			drawCurrentActivity(c);
//		}
//		
//		while (toAdd.size()>0) {
//			mNote.addDrawingStroke(currentPaintColor, currentPaintSize, toAdd.poll());
//		}
		
		
		mPresenter.drawNote(c);
		
	}
	
	private void drawCurrentActivity (Canvas c) {
		Path path = new Path();
		Paint tempPaint = new Paint();
		
		tempPaint.setStyle(Paint.Style.STROKE);
		tempPaint.setStrokeJoin(Paint.Join.ROUND);
		tempPaint.setStrokeCap(Paint.Cap.ROUND);
		tempPaint.setAntiAlias(true);
		
		
		synchronized (toAdd) {
			for (PointF[] points : toAdd) {
				if (points.length >= 2) {
					path.moveTo((points[0].x-windowX)/zoomMultiplier, (points[0].y-windowY)/zoomMultiplier);
					
					for (int i = 1; i < points.length; i++) {
						path.lineTo((points[i].x-windowX)/zoomMultiplier, (points[i].y-windowY)/zoomMultiplier);
					}
					
					tempPaint.setColor(currentPaintColor);
					tempPaint.setStrokeWidth(currentPaintSize/zoomMultiplier);
					c.drawPath(path, tempPaint);
				}
			}		
		}
		
		
		synchronized (currentStroke) {
			if (currentStroke.size() > 0) {
				path.moveTo((currentStroke.get(0).x - windowX)/zoomMultiplier, (currentStroke.get(0).y - windowY)/zoomMultiplier);
				
				for (int i = 1; i < currentStroke.size(); i++) {
					path.lineTo((currentStroke.get(i).x - windowX)/zoomMultiplier, (currentStroke.get(i).y - windowY)/zoomMultiplier);
				}
				
				if (mWorkState == DRAWING) {
					tempPaint.setColor(currentPaintColor);
					tempPaint.setStrokeWidth(currentPaintSize/zoomMultiplier);
				} else if(mWorkState == SELECTING) {
					tempPaint.setColor(Color.RED);
					tempPaint.setStrokeWidth(3.5F);
					tempPaint.setPathEffect(new DashPathEffect(new float[] {10, 20}, 0));
				}
				
				c.drawPath(path, tempPaint);
			}
		}
	}

//------------------------------------------------------------------------------------------------------------------
	
}
	
