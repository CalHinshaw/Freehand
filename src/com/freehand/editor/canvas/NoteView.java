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
	private final Paint paperSidePaint = new Paint();
	private float paperSideX;
	private float paperHeight;
	private float yMax;
	private int numPages;
	private final Paint paperDividerPaint = new Paint();
	private final Paint pageNumberPaint = new Paint();
	
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
		paperSidePaint.setColor(0xaacccccc);
		paperSidePaint.setStyle(Style.FILL);
		paperSidePaint.setAntiAlias(true);
		paperPath.setFillType(Path.FillType.INVERSE_WINDING);
		
		paperDividerPaint.setColor(0xaacccccc);
		paperDividerPaint.setStyle(Style.STROKE);
		paperDividerPaint.setStrokeWidth(3.0f);
		paperDividerPaint.setStrokeCap(Paint.Cap.BUTT);
		paperDividerPaint.setAntiAlias(true);
		
		pageNumberPaint.setColor(Color.DKGRAY);
		pageNumberPaint.setTextSize(22.0f);
		pageNumberPaint.setTextAlign(Paint.Align.RIGHT);
		pageNumberPaint.setAntiAlias(true);
		
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
			paperHeight = t.height;
			numPages = t.numPages;
			yMax = t.yMax;
			paperSideX = t.width/2.0f;
			final RectF r = new RectF(-paperSideX, -yMax, paperSideX, yMax);
			paperPath.moveTo(r.left, r.top);
			paperPath.addRect(r, Path.Direction.CW);
			
			canvPosTracker.setPos(getResources().getDisplayMetrics().widthPixels/2.0f, yMax, 1);
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
	
	public void start () {
		mZoomNotifier.start();
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
		
		if (paperPath.isEmpty() == false) {
			c.drawPath(paperPath, paperSidePaint);

			int topDivider = (int) ((yMax-canvPosTracker.getCanvY())/paperHeight + 1.0f);
			if (topDivider < 1) topDivider = 1;
			int botDivider = (int) (((yMax-canvPosTracker.getCanvY()) + this.getHeight() / canvPosTracker.getZoomMult()) / paperHeight);
			if (botDivider > numPages) botDivider = numPages;
			for (int i = topDivider; i <= botDivider; i++) {
				if (i != numPages) c.drawLine(-paperSideX, i*paperHeight-yMax, paperSideX, i*paperHeight-yMax, paperDividerPaint);
				c.drawText(Integer.toString(i), paperSideX-15, i*paperHeight-yMax-20, pageNumberPaint);
			}
		}
	}
}