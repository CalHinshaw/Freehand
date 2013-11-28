package com.freehand.editor.canvas;

import com.freehand.editor.tool_bar.IActionBarListener;
import com.freehand.ink.MiscGeom;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class NoteView extends View implements IActionBarListener, ICanvScreenConverter {
	
	private float zoomMult = 1;
	private float canvX = 0;
	private float canvY = 0;
	private Matrix screenToCanvMat = new Matrix();
	private Matrix canvToScreenMat = new Matrix();
	
	private float prevScreenPinchMidpointX = Float.NaN;
	private float prevScreenPinchMidpointY = Float.NaN;
	private float prevScreenPinchDist = Float.NaN;
	
	private ZoomNotifier mZoomNotifier;
	
	private float stylusPressureCutoff = 2.0f;
	private float pressureSensitivity = 0.50f;
	private boolean capacitiveDrawing = true;
	
	private boolean ignoreCurrentMotionEvent = false;
	private float prevCanvStylusX = Float.NaN;
	private float prevCanvStylusY = Float.NaN;
	private float prevStylusPressure = Float.NaN;
	private long prevStylusTime = Long.MIN_VALUE;
	
	private Note mNote;
	private ITool currentTool = new Pen(mNote, this, pressureSensitivity, Color.BLACK, 6.0f, true);
	
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
		canvX = pos[0];
		canvY = pos[1];
		zoomMult = pos[2];
		invalidate();
	}
	
	public float[] getPos () {
		final float pos[] = {canvX, canvY, zoomMult};
		return pos;
	}
	

//****************************** Touch Handling Methods *********************************************

	@Override
	public boolean onTouchEvent (MotionEvent event) {
		event.transform(screenToCanvMat);
		event = filterMotionEvent(event);
		if (event == null) return true;
		
		RectF dirty = null;
		
		if (currentTool.onMotionEvent(event)) {
			dirty = currentTool.getDirtyRect();
			prevScreenPinchMidpointX = Float.NaN;
			prevScreenPinchMidpointY = Float.NaN;
			prevScreenPinchDist = Float.NaN;
		} else if (event.getPointerCount() >= 2) {
			event.transform(canvToScreenMat);
			panZoom(event);
			mZoomNotifier.update(this.zoomMult);
		}
		
		if (event.getActionMasked() == MotionEvent.ACTION_UP ||
			event.getActionMasked() == MotionEvent.ACTION_CANCEL ||
			event.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
			prevScreenPinchMidpointX = Float.NaN;
			prevScreenPinchMidpointY = Float.NaN;
			prevScreenPinchDist = Float.NaN;
		}
		
		if (dirty == null) {
			invalidate();
		} else {
			invalidate(canvasRectToScreenRect(dirty));
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
	
	
	private void panZoom(final MotionEvent e) {
		final float curScreenDist = MiscGeom.distance(e.getX(0), e.getY(0), e.getX(1), e.getY(1));
		final float curScreenX = (e.getX(0)+e.getX(1)) / 2;
		final float curScreenY = (e.getY(0)+e.getY(1)) / 2;
		
		if (!Float.isNaN(prevScreenPinchMidpointX) && !Float.isNaN(prevScreenPinchMidpointY) && !Float.isNaN(prevScreenPinchDist)) {
			final float dZoom = curScreenDist / prevScreenPinchDist;
			canvX += (curScreenX/dZoom - prevScreenPinchMidpointX)/zoomMult;
			canvY += (curScreenY/dZoom - prevScreenPinchMidpointY)/zoomMult;
			zoomMult *= dZoom;
			
			final float[] canvToScreenVals = {	zoomMult,	0,			canvX*zoomMult,
												0,			zoomMult,	canvY*zoomMult,
												0,			0,			1				};
			canvToScreenMat.setValues(canvToScreenVals);
			
			final float[] screenToCanvVals = {	1.0f/zoomMult,	0,				-canvX,
												0,				1.0f/zoomMult,	-canvY,
												0,				0,				1			};
			screenToCanvMat.setValues(screenToCanvVals);
		}
		
		prevScreenPinchDist = curScreenDist;
		prevScreenPinchMidpointX = curScreenX;
		prevScreenPinchMidpointY = curScreenY;
	}
	
	@Override
	public boolean onHoverEvent (MotionEvent e) {
		e.transform(screenToCanvMat);
		currentTool.onMotionEvent(e);
		
		RectF dirty = currentTool.getDirtyRect();
		if (dirty == null) {
			invalidate();
		} else {
			invalidate(canvasRectToScreenRect(dirty));
		}
		
		return true;
	}
	
	private Rect canvasRectToScreenRect (RectF canvRect) {
		Rect screenRect = new Rect();
		
		screenRect.left = (int) ((canvRect.left + canvX) * zoomMult);
		screenRect.right = (int) ((canvRect.right + canvX) * zoomMult) + 1;
		screenRect.top = (int) ((canvRect.top + canvY) * zoomMult);
		screenRect.bottom = (int) ((canvRect.bottom + canvY) * zoomMult) + 1;
		
		return screenRect;
	}
	
	
	//************************************************** IActionBarListener Methods *******************************************************
	
	
	public void setTool (Tool newTool, float size, int color) {
		switch (newTool) {
			case PEN:
				currentTool = new Pen(mNote, this, pressureSensitivity, color, size, capacitiveDrawing);
				break;
			case STROKE_ERASER:
				currentTool = new StrokeEraser(mNote, this, size, capacitiveDrawing);
				break;
			case STROKE_SELECTOR:
				currentTool = new StrokeSelector(mNote, this, capacitiveDrawing);
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
	
	
	//************************************************ ICanvScreenConverter *************************************************************
	
	public float canvToScreenDist(final float canvDist) {
		return canvDist*zoomMult;
	}
	
	public float screenToCanvDist(final float screenDist) {
		return screenDist/zoomMult;
	}
	
	
	//********************************************** Rendering ****************************************************************
	
	@Override
	public void onDraw (Canvas c) {
		c.drawColor(Color.WHITE);
		c.concat(canvToScreenMat);
		currentTool.draw(c);
	}
}