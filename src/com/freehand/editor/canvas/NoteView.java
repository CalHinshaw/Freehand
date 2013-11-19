package com.freehand.editor.canvas;

import com.freehand.editor.tool_bar.IActionBarListener;
import com.freehand.ink.MiscGeom;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Debug;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class NoteView extends View implements IActionBarListener, ICanvScreenConverter {
	
	private float zoomMult = 1;
	private float canvX = 0;
	private float canvY = 0;
	private Matrix screenToCanvMat = new Matrix();
	private Matrix canvToScreenMat = new Matrix();
	
	private float prevScreenX = Float.NaN;
	private float prevScreenY = Float.NaN;
	private float prevScreenDist = Float.NaN;
	
	private float stylusPressureCutoff = 2.0f;
	private float pressureSensitivity = 0.50f;
	private boolean capacitiveDrawing = true;
	
	private Note mNote;
	private ITool currentTool = new Pen(mNote, this, pressureSensitivity, Color.BLACK, 6.0f, true);
	
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
		// TODO doesn't filter out bad Galaxy Note stylus points
		event.transform(screenToCanvMat);
		RectF dirty = null;
		
//		if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
//			Debug.startMethodTracing("freehand");
//		}
		
		
		
		
		if (currentTool.onMotionEvent(event)) {
			dirty = currentTool.getDirtyRect();
			prevScreenX = Float.NaN;
			prevScreenY = Float.NaN;
			prevScreenDist = Float.NaN;
		} else if (event.getPointerCount() >= 2) {
			event.transform(canvToScreenMat);
			panZoom(event);
		}
		
		if (event.getActionMasked() == MotionEvent.ACTION_UP ||
			event.getActionMasked() == MotionEvent.ACTION_CANCEL ||
			event.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
			prevScreenX = Float.NaN;
			prevScreenY = Float.NaN;
			prevScreenDist = Float.NaN;
		}
		
		if (dirty == null) {
			invalidate();
		} else {
			invalidate(canvasRectToScreenRect(dirty));
		}
		
		
		
//		if (event.getActionMasked() == MotionEvent.ACTION_UP) {
//			Debug.stopMethodTracing();
//		}
		
		
		
		
		return true;
	}
	
	private void panZoom(final MotionEvent e) {
		final float curScreenDist = MiscGeom.distance(e.getX(0), e.getY(0), e.getX(1), e.getY(1));
		final float curScreenX = (e.getX(0)+e.getX(1)) / 2;
		final float curScreenY = (e.getY(0)+e.getY(1)) / 2;
		
		if (!Float.isNaN(prevScreenX) && !Float.isNaN(prevScreenY) && !Float.isNaN(prevScreenDist)) {
			final float dZoom = curScreenDist / prevScreenDist;
			canvX += (curScreenX/dZoom - prevScreenX)/zoomMult;
			canvY += (curScreenY/dZoom - prevScreenY)/zoomMult;
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
		
		prevScreenDist = curScreenDist;
		prevScreenX = curScreenX;
		prevScreenY = curScreenY;
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