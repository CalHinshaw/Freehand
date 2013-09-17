package com.freehand.editor.canvas;

import com.freehand.ink.MiscGeom;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class NoteView extends View {
	private IScreenEventListener mListener;
	
	private boolean capDrawing = true;
	
	private float previousX = Float.NaN;
	private float previousY = Float.NaN;
	private float previousDistance = Float.NaN;
	private RectF prevBoundingRect = null;
	
	private boolean currentlyPanZoom = false;
	private boolean canDraw = true;
	
	private float stylusPressureCutoff = 2.0f;
	
//************************************* Constructors ************************************************

	public NoteView(Context context) {
		super(context);
		setDeviceSpecificPressureCutoffs();
	}
	
	public NoteView (Context context, AttributeSet attrs) {
		super (context, attrs);
		setDeviceSpecificPressureCutoffs();
	}
	
	public NoteView (Context context, AttributeSet attrs, int defStyle) {
		super (context, attrs, defStyle);
		setDeviceSpecificPressureCutoffs();
	}
	
	public void setDeviceSpecificPressureCutoffs() {
		if (android.os.Build.PRODUCT.equals("SGH-I717")) {
			stylusPressureCutoff = 1.0f / 253.0f;
		} else if (android.os.Build.BRAND.equals("samsung")) {
			stylusPressureCutoff = 1.0f / 1020.0f;
		}
	}

//************************************* Outward facing methods **************************************	

	public void setListener (IScreenEventListener newListener) {
		mListener = newListener;
	}
	
	public void setUsingCapDrawing (boolean usingCapDrawing) {
		capDrawing = usingCapDrawing;
	}

//****************************** Touch Handling Methods *********************************************

	@Override
	public boolean onTouchEvent (MotionEvent event) {
		// If the user has lifted a finger invalidate all of the pan/zoom variables
		if (event.getAction() == MotionEvent.ACTION_POINTER_UP || event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP) {
			previousX = Float.NaN;
			previousY = Float.NaN;
			previousDistance = Float.NaN;
			prevBoundingRect = null;
		}
		
		if (event.getPointerCount() == 1 && canDraw == true) {
			currentlyPanZoom = false;
			processDraw(event);
		} else if (event.getPointerCount() == 2) {
			if (canDraw == true) {
				mListener.canclePointerEvent();
			}
			
			if (currentlyPanZoom == false) {
				mListener.startPinchEvent();
			}
			
			canDraw = false;
			currentlyPanZoom = true;
			processPanZoom(event);
		} else {
			if (currentlyPanZoom == true) {
				mListener.finishPinchEvent();
			}
			currentlyPanZoom = false;			
		}
		
		// If a pan/zoom action has ended make sure the user can draw during the next touch event
		if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP) {
			canDraw = true;
			currentlyPanZoom = false;
		}
		
		Rect dirty = mListener.getDirtyRect();
		if (dirty == null) {
			invalidate();
		} else {
			invalidate(dirty);
		}
		
		return true;
	}
	
	private void processDraw (MotionEvent e) {
		boolean usingStylus = e.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS;
		
		if (!usingStylus && !(e.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER && capDrawing == true)) {
			return;
		}
		
		if (e.getAction() == MotionEvent.ACTION_DOWN) {
			mListener.startPointerEvent();
		}
		
		for(int i = 0; i < e.getHistorySize(); i++) {
			if (usingStylus && e.getHistoricalPressure(i) < stylusPressureCutoff) {
				continue;
			}
			
			mListener.continuePointerEvent(e.getHistoricalEventTime(i), e.getHistoricalX(i), e.getHistoricalY(i), e.getHistoricalPressure(i));
		}

		
		if (!usingStylus || e.getPressure() > stylusPressureCutoff) {
			mListener.continuePointerEvent(e.getEventTime(), e.getX(), e.getY(), e.getPressure());
		}
		
		if (e.getAction() == MotionEvent.ACTION_UP) {
			mListener.finishPointerEvent();
		}
	}
	
	private void processPanZoom (MotionEvent event) {
		float currentDistance = MiscGeom.distance(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
		float currentX = (event.getX(0)+event.getX(1)) / 2;
		float currentY = (event.getY(0)+event.getY(1)) / 2;
		
		RectF boundingRect = new RectF(event.getX(0), event.getY(0), event.getX(0), event.getY(0));
		for (int i = 1; i < event.getPointerCount(); i++) {
			boundingRect.union(event.getX(i), event.getY(i));
		}
		
		if (Float.isNaN(previousX) == false && Float.isNaN(previousY) == false && Float.isNaN(previousDistance) == false && prevBoundingRect != null) {
			float dZoom = currentDistance / previousDistance;
			float dx = currentX/dZoom - previousX;
			float dy = currentY/dZoom - previousY;
			mListener.continuePinchEvent(currentX, currentY, dx, dy, dZoom, currentDistance, prevBoundingRect);
		}
		
		previousDistance = currentDistance;
		previousX = currentX;
		previousY = currentY;
		prevBoundingRect = boundingRect;
	}
	
	@Override
	public boolean onHoverEvent (MotionEvent e) {
		
		if (e.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
			mListener.startHoverEvent();
		}
		
		for(int i = 0; i < e.getHistorySize(); i++) {
			mListener.continueHoverEvent(e.getHistoricalEventTime(i), e.getHistoricalX(i), e.getHistoricalY(i));
		}
		
		mListener.continueHoverEvent(e.getEventTime(), e.getX(), e.getY());
		
		if (e.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
			mListener.finishHoverEvent();
		}
		
		invalidate();
		return true;
	}
	
	@Override
	public void onDraw (Canvas c) {
		mListener.drawNote(c);
	}
}