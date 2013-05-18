package com.calhounhinshaw.freehandalpha.note_editor;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import com.calhounhinshaw.freehandalpha.ink.MiscGeom;
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
	private NoteEditorPresenter mPresenter;
	
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
	

	public void setPresenter (NoteEditorPresenter newPresenter) {
		mPresenter = newPresenter;
	}



//****************************** Touch Handling Methods *********************************************

	private float previousX = Float.NaN;
	private float previousY = Float.NaN;
	private float previousDistance = Float.NaN;
	
	private boolean canDraw = true;
	

	@Override
	public boolean onTouchEvent (MotionEvent event) {
		
		// If the user has lifted a finger invalidate all of the pan/zoom variables
		if (event.getAction() == MotionEvent.ACTION_POINTER_UP || event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP) {
			previousX = Float.NaN;
			previousY = Float.NaN;
			previousDistance = Float.NaN;
		}
		
		if (event.getPointerCount() == 1 && canDraw == true && event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
			processDraw(event);
		} else if (event.getPointerCount() >= 2) {
			canDraw = false;
			processPanZoom(event);
		}
		
		// If a pan/zoom action has ended make sure the user can draw during the next touch event
		if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP) {
			canDraw = true;
		}
		
		return true;
	}
	
	private void processDraw (MotionEvent event) {
		ArrayList<Long> times = new ArrayList<Long>(event.getHistorySize()+1);
		ArrayList<Float> xs = new ArrayList<Float>(event.getHistorySize()+1);
		ArrayList<Float> ys = new ArrayList<Float>(event.getHistorySize()+1);
		ArrayList<Float> pressures = new ArrayList<Float>(event.getHistorySize()+1);
		
		for(int i = 0; i < event.getHistorySize(); i++) {
			times.add(event.getHistoricalEventTime(i));
			xs.add(event.getHistoricalX(i));
			ys.add(event.getHistoricalY(i));
			pressures.add(event.getHistoricalPressure(i));
			//Log.d("PEN", Float.toString(event.getHistoricalX(i)) + "  " + Float.toString(event.getHistoricalY(i)) + "  " + Float.toString(event.getHistoricalPressure(i)));
		}
		
		times.add(event.getEventTime());
		xs.add(event.getX());
		ys.add(event.getY());
		pressures.add(event.getPressure());
		//Log.d("PEN", Float.toString(event.getX()) + "  " + Float.toString(event.getY()) + "  " + Float.toString(event.getPressure()));
		
		mPresenter.penAction(times, xs, ys, pressures, event.getAction() == MotionEvent.ACTION_UP);
		
		invalidate();
	}
	
	private void processPanZoom (MotionEvent event) {
		float currentDistance = MiscGeom.distance(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
		float currentX = (event.getX(0)+event.getX(1)) / 2;
		float currentY = (event.getY(0)+event.getY(1)) / 2;
		
		if (Float.isNaN(previousX) == false && Float.isNaN(previousY) == false && Float.isNaN(previousDistance) == false) {
			float dZoom = currentDistance / previousDistance;
			
			float dx = currentX/dZoom - previousX;
			float dy = currentY/dZoom - previousY;
			
			mPresenter.panZoomAction(currentX, currentY, dx, dy, dZoom);
		}
		
		previousDistance = currentDistance;
		previousX = currentX;
		previousY = currentY;
		
		invalidate();
	}
	
	
	@Override
	public boolean onHoverEvent (MotionEvent event) {
		
		ArrayList<Long> times = new ArrayList<Long>(event.getHistorySize()+1);
		ArrayList<Float> xs = new ArrayList<Float>(event.getHistorySize()+1);
		ArrayList<Float> ys = new ArrayList<Float>(event.getHistorySize()+1);
		
		for(int i = 0; i < event.getHistorySize(); i++) {
			times.add(event.getHistoricalEventTime(i));
			xs.add(event.getHistoricalX(i));
			ys.add(event.getHistoricalY(i));
		}
		
		times.add(event.getEventTime());
		xs.add(event.getX());
		ys.add(event.getY());//
		
		mPresenter.hoverAction(times, xs, ys);
		
		return true;
	}
	
	
	@Override
	public void onDraw (Canvas c) {
		mPresenter.drawNote(c);
	}
}