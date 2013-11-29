package com.freehand.editor.canvas;

import com.freehand.ink.MiscGeom;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;

class CanvPosTracker implements ICanvScreenConverter {
	
	private float canvX = 0;
	private float canvY = 0;
	private float zoomMult = 1;
	
	private Matrix screenToCanvMat = new Matrix();
	private Matrix canvToScreenMat = new Matrix();
	
	private boolean thresholdPassed = false;
	private float thresholdDZoom = 1.0f;
	private float thresholdCanvDx = 0;
	private float thresholdCanvDy = 0;
	
	private float prevScreenPinchMidpointX = Float.NaN;
	private float prevScreenPinchMidpointY = Float.NaN;
	private float prevScreenPinchDist = Float.NaN;
	
	
	public void setPos (final float newX, final float newY, final float newZoomMult) {
		canvX = newX;
		canvY = newY;
		zoomMult = newZoomMult;
	}
	
	public void clearPinchState () {
		prevScreenPinchMidpointX = Float.NaN;
		prevScreenPinchMidpointY = Float.NaN;
		prevScreenPinchDist = Float.NaN;
	}
	
	public void update (MotionEvent e) {
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
	
	public float getCanvX () {
		return canvX;
	}
	
	public float getCanvY () {
		return canvY;
	}
	
	public float getZoomMult () {
		return zoomMult;
	}
	
	/**
	 * @return the actual mutable screenToCanvMat this class uses for performance reasons. If you modify it you might break everything.
	 */
	public Matrix getScreenToCanvMat () {
		return screenToCanvMat;
	}
	
	/**
	 * @return the actual mutable canvToScreenMat this class uses for performance reasons. If you modify it you might break everything.
	 */
	public Matrix getCanvToScreenMat () {
		return canvToScreenMat;
	}


	public float canvToScreenDist(final float canvDist) {
		return canvDist*zoomMult;
	}
	
	public float screenToCanvDist(final float screenDist) {
		return screenDist/zoomMult;
	}
	
	public Rect canvRectToScreenRect (RectF canvRect) {
		Rect screenRect = new Rect();
		
		screenRect.left = (int) ((canvRect.left + canvX) * zoomMult);
		screenRect.right = (int) ((canvRect.right + canvX) * zoomMult) + 1;
		screenRect.top = (int) ((canvRect.top + canvY) * zoomMult);
		screenRect.bottom = (int) ((canvRect.bottom + canvY) * zoomMult) + 1;
		
		return screenRect;
	}
}