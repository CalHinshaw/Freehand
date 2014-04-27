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
	
	private float zoomThreshold = 0.20f;
	private float xThreshold = 50.0f;
	private float yThreshold = 50.0f;
	
	private boolean zoomThresholdPassed = false;
	
	private boolean xThresholdPassed = false;
	
	private boolean yThresholdPassed = false;
	
	private float initScreenDist = Float.NaN;
	private float initScreenX = Float.NaN;
	private float initScreenY = Float.NaN;
	
	private float prevScreenPinchMidpointX = Float.NaN;
	private float prevScreenPinchMidpointY = Float.NaN;
	private float prevScreenPinchDist = Float.NaN;
	
	
	public void setPos (final float newX, final float newY, final float newZoomMult) {
		canvX = newX;
		canvY = newY;
		zoomMult = newZoomMult;
		
		updateMats();
	}
	
	
	public void setThresholds (final float zoomThreshold, final float xThreshold, final float yThreshold) {
		this.zoomThreshold = zoomThreshold;
		this.xThreshold = xThreshold;
		this.yThreshold = yThreshold;
	}
	
	
	public void update (final MotionEvent e) {
		final float curScreenDist = MiscGeom.distance(e.getX(0), e.getY(0), e.getX(1), e.getY(1));
		final float curScreenX = (e.getX(0)+e.getX(1)) / 2;
		final float curScreenY = (e.getY(0)+e.getY(1)) / 2;
		
		if (!Float.isNaN(prevScreenPinchMidpointX) && !Float.isNaN(prevScreenPinchMidpointY) && !Float.isNaN(prevScreenPinchDist)) {
			if (Float.isNaN(initScreenDist) || Float.isNaN(initScreenX) || Float.isNaN(initScreenY)) {
				initScreenDist = curScreenDist;
				initScreenX = curScreenX;
				initScreenY = curScreenY;
			}
			
			final float dZoom = calcDZoom(curScreenDist);
			
			canvX += calcDX(dZoom, curScreenX);
			canvY += calcDY(dZoom, curScreenY);
			zoomMult *= dZoom;
			
			updateMats();
		}
		
		prevScreenPinchDist = curScreenDist;
		prevScreenPinchMidpointX = curScreenX;
		prevScreenPinchMidpointY = curScreenY;
	}
	
	private float calcDZoom (final float curScreenDist) {
		final float totalZoom = curScreenDist / initScreenDist;
				
		if (!zoomThresholdPassed && !(totalZoom > (1.0f + zoomThreshold) || totalZoom < (1.0f - zoomThreshold))) {
			return 1.0f;
		} else if (!zoomThresholdPassed && (totalZoom > (1.0f + zoomThreshold) || totalZoom < (1.0f - zoomThreshold))) {
			zoomThresholdPassed = true;
			return totalZoom;
		} else {
			return curScreenDist / prevScreenPinchDist;
		}
	}
	
	private float calcDX (final float dZoom, final float curScreenX) {
		final float totalDX = curScreenX - initScreenX;
		
		if (!xThresholdPassed && !(totalDX > xThreshold || totalDX < -xThreshold)) {
			return initScreenX*(1-dZoom)/(dZoom*zoomMult);
		} else if (!xThresholdPassed && (totalDX > xThreshold || totalDX < -xThreshold)) {
			xThresholdPassed = true;
			return (curScreenX/dZoom - initScreenX)/zoomMult;
		} else {
			return (curScreenX/dZoom - prevScreenPinchMidpointX)/zoomMult;
		}
	}
	
	private float calcDY (final float dZoom, final float curScreenY) {
		final float totalDY = curScreenY - initScreenY;
		
		if (!yThresholdPassed && !(totalDY > yThreshold || totalDY < -yThreshold)) {
			return initScreenY*(1-dZoom)/(dZoom*zoomMult);
		} else if (!yThresholdPassed && (totalDY > yThreshold || totalDY < -yThreshold)) {
			yThresholdPassed = true;
			return (curScreenY/dZoom - initScreenY)/zoomMult;
		} else {
			return (curScreenY/dZoom - prevScreenPinchMidpointY)/zoomMult;
		}
	}
	
	
	private void updateMats () {
		final float[] canvToScreenVals = {	zoomMult,	0,			canvX*zoomMult,
											0,			zoomMult,	canvY*zoomMult,
											0,			0,			1				};
		canvToScreenMat.setValues(canvToScreenVals);

		final float[] screenToCanvVals = {	1.0f/zoomMult,	0,				-canvX,
											0,				1.0f/zoomMult,	-canvY,
											0,				0,				1			};
		screenToCanvMat.setValues(screenToCanvVals);
	}
	
	
	public void clearPinchState () {
		initScreenDist = Float.NaN;
		initScreenX = Float.NaN;
		initScreenY = Float.NaN;
		
		prevScreenPinchMidpointX = Float.NaN;
		prevScreenPinchMidpointY = Float.NaN;
		prevScreenPinchDist = Float.NaN;
		
		zoomThresholdPassed = false;
		xThresholdPassed = false;
		yThresholdPassed = false;
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