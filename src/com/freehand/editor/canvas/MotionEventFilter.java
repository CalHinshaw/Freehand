package com.freehand.editor.canvas;

import android.view.MotionEvent;

class MotionEventFilter {
	private float stylusPressureCutoff = -1.0f;
	
	private boolean ignoreCurrentMotionEvent = false;
	private float prevCanvStylusX = Float.NaN;
	private float prevCanvStylusY = Float.NaN;
	private float prevStylusPressure = Float.NaN;
	private long prevStylusTime = Long.MIN_VALUE;
	
	public MotionEventFilter () {
		if (android.os.Build.PRODUCT.equals("SGH-I717")) {
			stylusPressureCutoff = 1.0f / 253.0f;
		} else if (android.os.Build.BRAND.equals("samsung")) {
			stylusPressureCutoff = 1.0f / 1020.0f;
		}
	}
	
	/**
	 * Filters points that shouldn't be there (due to bugs in the way Galaxy Note devices handle their stylus inputs
	 * as of the writing of this comment) from the stream of MotionEvents that defines a continuous touch.
	 * <p>
	 * If this method must receive all of the MotionEvents in a continuous touch to work correctly.
	 */
	public MotionEvent filter (final MotionEvent event) {
		if (stylusPressureCutoff < 0) return event;
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
}