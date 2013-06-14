package com.freehand.note_editor;


import com.freehand.ink.MiscGeom;
import com.freehand.ink.Point;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class NoteView extends View {
	private INoteCanvasListener mListener;
	
//************************************* Constructors ************************************************

	public NoteView(Context context) {
		super(context);
	}
	
	public NoteView (Context context, AttributeSet attrs) {
		super (context, attrs);
	}
	
	public NoteView (Context context, AttributeSet attrs, int defStyle) {
		super (context, attrs, defStyle);
	}

//---------------------------------------------------------------------------------------	
	

	public void setListener (INoteCanvasListener newListener) {
		mListener = newListener;
	}



//****************************** Touch Handling Methods *********************************************

	private float previousX = Float.NaN;
	private float previousY = Float.NaN;
	private float previousDistance = Float.NaN;
	private RectF prevBoundingRect = null;
	
	private boolean canDraw = true;
	

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
			processDraw(event);
		} else if (event.getPointerCount() >= 2) {
			canDraw = false;
			processPanZoom(event);
		}
		
		// If a pan/zoom action has ended make sure the user can draw during the next touch event
		if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP) {
			canDraw = true;
		}
		
		invalidate();
		return true;
	}
	
	private void processDraw (MotionEvent e) {
		
		if (e.getAction() == MotionEvent.ACTION_DOWN) {
			mListener.startPointerEvent();
		}
		
		for(int i = 0; i < e.getHistorySize(); i++) {
			if (e.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
				mListener.continuePointerEvent(e.getHistoricalEventTime(i), e.getHistoricalX(i),
					e.getHistoricalY(i), e.getHistoricalPressure(i));
			}
		}

		if (e.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
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
			
			if (event.getPointerCount() == 2 && event.getAction() == MotionEvent.ACTION_POINTER_DOWN) {
				mListener.startPinchEvent();
			}
			
			mListener.continuePinchEvent(currentX, currentY, dx, dy, dZoom, prevBoundingRect);
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