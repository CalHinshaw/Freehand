package com.freehand.editor.canvas;

import com.freehand.editor.canvas.Note.PaperType;
import com.freehand.editor.tool_bar.IActionBarListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class NoteView extends View implements IActionBarListener {
	private Note mNote;
	
	private final Path paperPath = new Path();
	private final Paint paperPaint = new Paint();
	
	private float pressureSensitivity = 0.50f;
	private boolean capacitiveDrawing = true;
	
	private final MotionEventFilter motionEventFilter = new MotionEventFilter();
	private final CanvPosTracker canvPosTracker = new CanvPosTracker();
	private final ZoomNotifier mZoomNotifier = new ZoomNotifier(this);
	
	private ITool currentTool = new Pen(mNote, canvPosTracker, this, pressureSensitivity, Color.BLACK, 6.0f, true);

	
//************************************* Constructors ************************************************

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
	
	private void init () {
		canvPosTracker.setPos(-getResources().getDisplayMetrics().widthPixels/2.0f, 0, 1);
		
		paperPaint.setColor(0xaacccccc);
		paperPaint.setStyle(Style.FILL);
		paperPaint.setAntiAlias(true);
		paperPath.setFillType(Path.FillType.INVERSE_WINDING);
		
		invalidate();
	}

//************************************* Outward facing class methods **************************************
	
	public void setUsingCapDrawing (boolean usingCapDrawing) {
		capacitiveDrawing = usingCapDrawing;
	}
	
	public void setNote (final Note note) {
		mNote = note;
		
		paperPath.rewind();
		if (mNote.getPaperType() != Note.PaperType.WHITEBOARD) {
			PaperType t = mNote.getPaperType();
			final RectF r = new RectF(-t.width/2.0f, -1000000.0f, t.width/2.0f, 1000000.0f);
			Log.d("PEN", r.toString());
			paperPath.moveTo(r.left, r.top);
			paperPath.addRect(r, Path.Direction.CW);
			
			canvPosTracker.setPos(getResources().getDisplayMetrics().widthPixels/2.0f, 1000000.0f, 1);
		}
		
		invalidate();
	}
	
	public void setPressureSensitivity (final float sensitivity) {
		pressureSensitivity = sensitivity;
	}
	
	public void setPos (final float[] pos) {
		canvPosTracker.setPos(pos[0], pos[1], pos[2]);
		invalidate();
	}
	
	public void setZoomThreshold(final float threshold) {
		canvPosTracker.setZoomThreshold(threshold);
	}
	
	public float[] getPos () {
		final float pos[] = {canvPosTracker.getCanvX(), canvPosTracker.getCanvY(), canvPosTracker.getZoomMult()};
		return pos;
	}
	
	public void finish () {
		mZoomNotifier.finish();
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
				currentTool = new Pen(mNote, canvPosTracker, this, pressureSensitivity, color, size, capacitiveDrawing);
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
		
		c.drawPath(paperPath, paperPaint);
	}
}