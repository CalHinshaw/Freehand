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
	private Note mNote;
	
	private float pressureSensitivity = 0.50f;
	private boolean capacitiveDrawing = true;
	
	private final MotionEventFilter motionEventFilter = new MotionEventFilter();
	private final CanvPosTracker canvPosTracker = new CanvPosTracker();
	private final ZoomNotifier mZoomNotifier;
	
	private ITool currentTool = new Pen(mNote, canvPosTracker, pressureSensitivity, Color.BLACK, 6.0f, true);

	
//************************************* Constructors ************************************************

	public NoteView(Context context) {
		super(context);
		mZoomNotifier = new ZoomNotifier(this);
	}
	
	public NoteView (Context context, AttributeSet attrs) {
		super (context, attrs);
		mZoomNotifier = new ZoomNotifier(this);
	}
	
	public NoteView (Context context, AttributeSet attrs, int defStyle) {
		super (context, attrs, defStyle);
		mZoomNotifier = new ZoomNotifier(this);
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
		event = motionEventFilter.filter(event);
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