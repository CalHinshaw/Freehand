package com.freehand.editor.canvas;

import com.freehand.editor.tool_bar.IActionBarListener;
import com.freehand.editor.tool_bar.IActionBarListener.Tool;
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
import android.widget.Toast;

public class NoteView extends View implements IActionBarListener, ICanvScreenConverter {
	private Note mNote;
	private ITool currentTool = new Pen(mNote, mConverter, pressureSensitivity, Color.BLACK, 6.0f);
	
	// The transformation matrix transforms the stuff drawn to the canvas as if (0, 0) is the upper left hand corner of the screen,
	// not the View the canvas is drawing to. The offsets fix that.
	private float canvOffsetX = -1;
	private float canvOffsetY = -1;
	
	private float zoomMult = 1;
	private float canvX = 0;
	private float canvY = 0;
	private Matrix transMat = new Matrix();
	
	private boolean capacitiveDrawing = true;
	
	private float prevX = Float.NaN;
	private float prevY = Float.NaN;
	private float prevDist = Float.NaN;
	
	private float stylusPressureCutoff = 2.0f;
	private float pressureSensitivity = 0.50f;
	
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
		event.transform(transMat);
		
		if (currentTool.onMotionEvent(event)) {
			prevX = Float.NaN;
			prevY = Float.NaN;
			prevDist = Float.NaN;
		} else {
			panZoom(event);
		}
		
		RectF dirty = currentTool.getDirtyRect();
		if (dirty == null) {
			invalidate();
		} else {
			invalidate(canvasRectToScreenRect(dirty));
		}
		
		
		return true;
	}
	
	private void panZoom(final MotionEvent e) {
		final float curDist = MiscGeom.distance(e.getX(0), e.getY(0), e.getX(1), e.getY(1));
		final float curX = (e.getX(0)+e.getX(1)) / 2;
		final float curY = (e.getY(0)+e.getY(1)) / 2;
		
		if (!Float.isNaN(prevX) && !Float.isNaN(prevY) && !Float.isNaN(prevDist)) {
			final float dZoom = curDist / prevDist;
			canvX += (curX/dZoom - prevX)/zoomMult;
			canvY += (curY/dZoom - prevY)/zoomMult;
			zoomMult *= dZoom;
			
			final float[] matVals = {	zoomMult,	0,			canvX*zoomMult + canvOffsetX,
										0,			zoomMult,	canvY*zoomMult + canvOffsetY,
										0,			0,			1							   };
			
			transMat.setValues(matVals);
		}
		
		prevDist = curDist;
		prevX = curX;
		prevY = curY;
	}
	
	@Override
	public boolean onHoverEvent (MotionEvent e) {
		e.transform(transMat);
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
				currentTool = new Pen(mNote, mConverter, pressureSensitivity, color, size);
				break;
			case STROKE_ERASER:
				currentTool = new StrokeEraser(mNote, mConverter, size);
				break;
			case STROKE_SELECTOR:
				currentTool = new StrokeSelector(mNote, mConverter);
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
	
	private void initOffsets (Canvas c) {
		// Set the transformMatrix's offsets if they haven't been set yet
		if (canvOffsetX < 0 || canvOffsetY < 0) {
			float[] values = new float[9];
			c.getMatrix().getValues(values);
			canvOffsetX = values[2];
			canvOffsetY = values[5];
		}
	}
	
	@Override
	public void onDraw (Canvas c) {
		initOffsets(c);
		c.drawColor(0xffffffff);
		currentTool.draw(c);
	}
}