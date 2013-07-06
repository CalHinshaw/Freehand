package com.freehand.note_editor;


import com.freehand.ink.Point;
import com.freehand.note_editor.tool.DistConverter;
import com.freehand.note_editor.tool.Pen;
import com.freehand.note_editor.tool.StrokeEraser;
import com.freehand.note_editor.tool.StrokeSelector;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.widget.Toast;

class NoteEditorController implements IActionBarListener, IScreenEventListener {
	
	private final Note mNote;
	private NoteView mNoteView;

	private ICanvasEventListener currentTool;
	
	// The information about where the screen is on the canvas
	private float zoomMultiplier = 1;
	private float windowX = 0;
	private float windowY = 0;
	private Matrix transformMatrix = new Matrix();
	private float[] matVals = {1, 0, 0, 0, 1, 0, 0, 0, 1};
	
	// The transformation matrix transforms the stuff drawn to the canvas as if (0, 0) is the upper left hand corner of the screen,
	// not the View the canvas is drawing to.
	private float canvasYOffset = -1;
	private float canvasXOffset = -1;
	
	private final float pressureSensitivity;
	

	private final DistConverter mConverter = new DistConverter () {
		@Override
		public float canvasToScreenDist(float canvasDist) {
			return canvasDist*zoomMultiplier;
		}

		@Override
		public float screenToCanvasDist(float screenDist) {
			return screenDist/zoomMultiplier;
		}
	};
	
	
	
	public NoteEditorController (Note note, float pressureSensitivity) {
		this.pressureSensitivity = pressureSensitivity;
		mNote = note;
		currentTool = new Pen(mNote, mConverter, pressureSensitivity, Color.BLACK, 6.0f);
	}
	
	public void setNoteView (NoteView newView) {
		mNoteView = newView;
	}
	
	public void saveNote () {
		if (mNote.save() == true) {
			Toast.makeText(mNoteView.getContext(), "Save Successful!", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(mNoteView.getContext(), "Save Failed", Toast.LENGTH_LONG).show();
		}
	}
	
	public Note getNote () {
		return mNote;
	}
	
	public boolean renameNote (String newName) {
		return mNote.rename(newName);
	}
	
	//*********************************** INoteCanvasListener Methods ****************************************************************
	

	public void startPointerEvent() {
		currentTool.startPointerEvent();
	}

	public void continuePointerEvent(long time, float x, float y, float pressure) {
		Point canvasPoint = this.scaleRawPoint(x, y);
		currentTool.continuePointerEvent(canvasPoint, time, pressure);
	}

	public void canclePointerEvent() {
		currentTool.canclePointerEvent();
	}
	
	public void finishPointerEvent() {
		currentTool.finishPointerEvent();
	}

	public void startPinchEvent() {
		currentTool.startPinchEvent();
	}

	public void continuePinchEvent(float midpointX, float midpointY, float midpointDx, float midpointDy, float dZoom, float dist, RectF startBoundingRect) {
		Point mid = this.scaleRawPoint(midpointX, midpointY);
		Point dMid = new Point(midpointDx/zoomMultiplier, midpointDy/zoomMultiplier);
		float canvDist = mConverter.screenToCanvasDist(dist);
		RectF canvRect = this.screenRectToCanvRect(startBoundingRect);
		
		// Return if currentTool consumes the pinch event
		if (currentTool.continuePinchEvent(mid, dMid, dZoom, canvDist, canvRect) == true) {
			return;
		}
		
		// If the tool doesn't consume the pinch event, translate the canvas
		windowX += midpointDx/zoomMultiplier;
		windowY += midpointDy/zoomMultiplier;
		zoomMultiplier *= dZoom;
	}

	public void canclePinchEvent() {
		currentTool.canclePinchEvent();
	}

	public void finishPinchEvent() {
		currentTool.finishPinchEvent();
	}
	
	public void startHoverEvent() {
		currentTool.startHoverEvent();
	}

	public void continueHoverEvent(long time, float x, float y) {
		Point canvPoint = this.scaleRawPoint(x, y);
		currentTool.continueHoverEvent(canvPoint, time);
	}

	public void cancleHoverEvent() {
		currentTool.cancleHoverEvent();
	}

	public void finishHoverEvent() {
		currentTool.finishHoverEvent();
	}
	
	public Rect getDirtyRect() {
//		RectF dirty = currentTool.getDirtyRect();
//		
//		if (dirty == null) {
//			return null;
//		} else {
//			return canvasRectToScreenRect(dirty);
//		}
		
		return null;
	}
	
	public void drawNote (Canvas c) {
		updatePanZoom(c);
		c.drawColor(0xffffffff);
		
		currentTool.drawNote(c);
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
		
		if (mNoteView != null) {
			mNoteView.invalidate();
		}
	}

	public void undo () {
		currentTool.undoCalled();
		mNote.undo();
		if (mNoteView != null) {
			mNoteView.invalidate();
		}
	}

	public void redo () {
		currentTool.redoCalled();
		mNote.redo();
		if (mNoteView != null) {
			mNoteView.invalidate();
		}
	}
	
	//********************************************** Helper Methods **********************************************
	
	private void updatePanZoom (Canvas c) {
		// Set the transformMatrix's offsets if they haven't been set yet
		if (canvasYOffset < 0 || canvasXOffset < 0) {
			float[] values = new float[9];
			c.getMatrix().getValues(values);
			canvasXOffset = values[2];
			canvasYOffset = values[5];
		}
		
		matVals[0] = zoomMultiplier;
		matVals[2] = windowX*zoomMultiplier + canvasXOffset;
		matVals[4] = zoomMultiplier;
		matVals[5] = windowY*zoomMultiplier + canvasYOffset;

		transformMatrix.setValues(matVals);
		c.setMatrix(transformMatrix);
	}
	
	private Point scaleRawPoint (float x, float y) {
		return new Point(-windowX + x/zoomMultiplier, -windowY + y/zoomMultiplier);
	}
	
	private RectF screenRectToCanvRect (RectF screenRect) {
		RectF canvRect = new RectF();
		
		canvRect.left = -windowX + screenRect.left/zoomMultiplier;
		canvRect.right = -windowX + screenRect.right/zoomMultiplier;
		canvRect.top = -windowY + screenRect.top/zoomMultiplier;
		canvRect.bottom = -windowY + screenRect.bottom/zoomMultiplier;
		
		return canvRect;
	}
	
	private Rect canvasRectToScreenRect (RectF canvRect) {
		Rect screenRect = new Rect();
		
		screenRect.left = (int) ((canvRect.left + windowX) * zoomMultiplier);
		screenRect.right = (int) ((canvRect.right + windowX) * zoomMultiplier) + 1;
		screenRect.top = (int) ((canvRect.top + windowY) * zoomMultiplier);
		screenRect.bottom = (int) ((canvRect.bottom + windowY) * zoomMultiplier) + 1;
		
		return screenRect;
	}
	
}