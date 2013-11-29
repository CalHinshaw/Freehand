package com.freehand.editor.canvas;

import com.freehand.editor.tool_bar.IActionBarListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class NoteView extends View implements IActionBarListener {
	
	private final CanvPosTracker canvPosTracker = new CanvPosTracker();
	private final ZoomNotifier mZoomNotifier;
	
	private float stylusPressureCutoff = 2.0f;
	private float pressureSensitivity = 0.50f;
	private boolean capacitiveDrawing = true;
	
	private boolean ignoreCurrentMotionEvent = false;
	private float prevCanvStylusX = Float.NaN;
	private float prevCanvStylusY = Float.NaN;
	private float prevStylusPressure = Float.NaN;
	private long prevStylusTime = Long.MIN_VALUE;
	
	private Note mNote;
	private ITool currentTool = new Pen(mNote, canvPosTracker, pressureSensitivity, Color.BLACK, 6.0f, true);
	
//************************************* Constructors ************************************************

	public NoteView(Context context) {
		super(context);
		setDeviceSpecificPressureCutoffs();
		mZoomNotifier = new ZoomNotifier(this);
	}
	
	public NoteView (Context context, AttributeSet attrs) {
		super (context, attrs);
		setDeviceSpecificPressureCutoffs();
		mZoomNotifier = new ZoomNotifier(this);
	}
	
	public NoteView (Context context, AttributeSet attrs, int defStyle) {
		super (context, attrs, defStyle);
		setDeviceSpecificPressureCutoffs();
		mZoomNotifier = new ZoomNotifier(this);
	}
	
	public void setDeviceSpecificPressureCutoffs() {
		if (android.os.Build.PRODUCT.equals("SGH-I717")) {
			stylusPressureCutoff = 1.0f / 253.0f;
		} else if (android.os.Build.BRAND.equals("samsung")) {
			stylusPressureCutoff = 1.0f / 1020.0f;
		}
	}
	

//************************************* Outward facing class methods **************************************
	
	public void setUsingCapDrawing (boolean usingCapDrawing) {
		capacitiveDrawing = usingCapDrawing;
	}
	
	public void setNote (final Note note) {
		mNote = note;
		invalidate();
	}
	
	public void setPressureSensitivity (final float sensitivity) {
		pressureSensitivity = sensitivity;
	}
	
	public void setPos (final float[] pos) {
		canvPosTracker.setPos(pos[0], pos[1], pos[2]);
		invalidate();
	}
	
	public float[] getPos () {
		final float pos[] = {canvPosTracker.getCanvX(), canvPosTracker.getCanvY(), canvPosTracker.getZoomMult()};
		return pos;
	}
	

//****************************** Touch Handling Methods *********************************************

	@Override
	public boolean onTouchEvent (MotionEvent event) {
		event.transform(canvPosTracker.getScreenToCanvMat());
		event = filterMotionEvent(event);
		if (event == null) return true;
		
		RectF dirty = null;
		
		if (currentTool.onMotionEvent(event)) {
			dirty = currentTool.getDirtyRect();
			canvPosTracker.clearPinchState();
		} else if (event.getPointerCount() >= 2) {
			event.transform(canvPosTracker.getCanvToScreenMat());
			canvPosTracker.update(event);
			mZoomNotifier.update(canvPosTracker.getZoomMult());
		}
		
		if (event.getActionMasked() == MotionEvent.ACTION_UP ||
			event.getActionMasked() == MotionEvent.ACTION_CANCEL ||
			event.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
			canvPosTracker.clearPinchState();
		}
		
		if (dirty == null) {
			invalidate();
		} else {
			invalidate(canvPosTracker.canvRectToScreenRect(dirty));
		}
		
		return true;
	}
	
	
	private MotionEvent filterMotionEvent(final MotionEvent event) {
		if (event.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) return event;
		
		if (ignoreCurrentMotionEvent == true && event.getActionMasked() == MotionEvent.ACTION_UP) {
			ignoreCurrentMotionEvent = false;
			clearPrevStylusFields();
			return null;
		} else if (ignoreCurrentMotionEvent == true) {
			return null;
		}
		
		MotionEvent toReturn = event;
		
		for (int i = 0; i < event.getHistorySize(); i++) {
			if (event.getHistoricalPressure(i) < stylusPressureCutoff) {
				toReturn = stripLowPressures(event);
				ignoreCurrentMotionEvent = true;
			}
		}
		
		if (event.getPressure() < stylusPressureCutoff) {
			toReturn = stripLowPressures(event);
			ignoreCurrentMotionEvent = true;
		}
		
		if (event.getActionMasked() == MotionEvent.ACTION_UP) {
			ignoreCurrentMotionEvent = false;
			clearPrevStylusFields();
		} else {
			prevCanvStylusX = event.getX();
			prevCanvStylusY = event.getY();
			prevStylusPressure = event.getPressure();
			prevStylusTime = event.getEventTime();
		}
		
		return toReturn;
	}
	
	private MotionEvent stripLowPressures (final MotionEvent e) {
		MotionEvent newEvent = null;
		
		// non-historical pointer must be below threshold because once you're below you never go up and this
		// method is only called if we're sure there's at least one low pressure, so we're ignoring
		
		for (int i = e.getHistorySize()-1; i >=0; i--) {
			if (e.getHistoricalPressure(i) >= stylusPressureCutoff) {
				if (newEvent == null) {
					newEvent = MotionEvent.obtain(e.getDownTime(), e.getHistoricalEventTime(i), MotionEvent.ACTION_UP, e.getHistoricalX(i),
						e.getHistoricalY(i), e.getHistoricalPressure(i), e.getHistoricalSize(i), e.getMetaState(), e.getXPrecision(),
						e.getYPrecision(), e.getDeviceId(), e.getEdgeFlags());
				} else {
					newEvent.addBatch(e.getHistoricalEventTime(i), e.getHistoricalX(i), e.getHistoricalY(i), e.getHistoricalPressure(i),
						e.getHistoricalSize(i), e.getMetaState());
				}
			}
		}
		
		if (newEvent == null) {
			newEvent = MotionEvent.obtain(e.getDownTime(), prevStylusTime, MotionEvent.ACTION_UP, prevCanvStylusX,
				prevCanvStylusY, prevStylusPressure, e.getSize(), e.getMetaState(), e.getXPrecision(),
				e.getYPrecision(), e.getDeviceId(), e.getEdgeFlags());
			
		}
		
		return newEvent;
	}
	
	
	private void clearPrevStylusFields () {
		prevCanvStylusX = Float.NaN;
		prevCanvStylusY = Float.NaN;
		prevStylusPressure = Float.NaN;
		prevStylusTime = Long.MIN_VALUE;
	}
	
	
	@Override
	public boolean onHoverEvent (MotionEvent e) {
		e.transform(canvPosTracker.getScreenToCanvMat());
		currentTool.onMotionEvent(e);
		
		RectF dirty = currentTool.getDirtyRect();
		if (dirty == null) {
			invalidate();
		} else {
			invalidate(canvPosTracker.canvRectToScreenRect(dirty));
		}
		
		return true;
	}
	
	
	//************************************************** IActionBarListener Methods *******************************************************
	
	
	public void setTool (Tool newTool, float size, int color) {
		switch (newTool) {
			case PEN:
				currentTool = new Pen(mNote, canvPosTracker, pressureSensitivity, color, size, capacitiveDrawing);
				break;
			case STROKE_ERASER:
				currentTool = new StrokeEraser(mNote, canvPosTracker, size, capacitiveDrawing);
				break;
			case STROKE_SELECTOR:
				currentTool = new StrokeSelector(mNote, canvPosTracker, capacitiveDrawing);
				break;
		}
		
		invalidate();
	}

	public void undo () {
		currentTool.undo();
		mNote.undo();
		invalidate();
	}

	public void redo () {
		currentTool.redo();
		mNote.redo();
		invalidate();
	}
	
	//********************************************** Rendering ****************************************************************
	
	@Override
	public void onDraw (Canvas c) {
		c.drawColor(Color.WHITE);
		c.concat(canvPosTracker.getCanvToScreenMat());
		currentTool.draw(c);
	}
}