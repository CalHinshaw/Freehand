package com.freehand.editor.canvas;

import java.util.LinkedList;

import android.view.MotionEvent;

class MotionEventFilter {
	private float stylusPressureCutoff = -1.0f;
	
	private final LinkedList<MotionEventRow> queue = new LinkedList<MotionEventRow>();
	private MotionEventRow lastValidRow = null;
	
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
	public MotionEvent filter (final MotionEvent e) {
		if (e.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) return e;		// We're not doing any filtering of capacitive events yet
		if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
			lastValidRow = new MotionEventRow(e.getEventTime(), e.getX(), e.getY(), e.getPressure(), e.getSize());
			return e;
		}
		
		
		// Add everything in e to queue
		for (int i = 0; i < e.getHistorySize(); i++) {
			queue.add(new MotionEventRow(e.getHistoricalEventTime(i), e.getHistoricalX(i), e.getHistoricalY(i), e.getHistoricalPressure(i), e.getHistoricalSize(i)));
		}
		queue.add(new MotionEventRow(e.getEventTime(), e.getX(), e.getY(), e.getPressure(), e.getSize()));
		
		int lastIncludedIndex = -1;
		for (int i = queue.size()-1; i >= 0; i--) {
			if (queue.get(i).pressure > stylusPressureCutoff) {
				lastIncludedIndex = i;
				break;
			}
		}
		
		
		MotionEvent newEvent;
		
		// Build the filtered MotionEvent
		if (lastIncludedIndex > -1) {
			newEvent = MotionEvent.obtain(e.getDownTime(), queue.get(0).time, e.getActionMasked(), queue.get(0).x,
				queue.get(0).y, queue.get(0).pressure, queue.get(0).size, e.getMetaState(), e.getXPrecision(),
				e.getYPrecision(), e.getDeviceId(), e.getEdgeFlags());
			
			for (int i = 1; i <= lastIncludedIndex; i++) {
				newEvent.addBatch(queue.get(i).time, queue.get(i).x, queue.get(i).y, queue.get(i).pressure, queue.get(i).size, e.getMetaState());
			}
			
			lastValidRow = new MotionEventRow(queue.get(lastIncludedIndex).time, queue.get(lastIncludedIndex).x, queue.get(lastIncludedIndex).y,
				queue.get(lastIncludedIndex).pressure, queue.get(lastIncludedIndex).size);
			
			for (int i = 0; i <= lastIncludedIndex; i++) {
				queue.removeFirst();
			}
		} else if (e.getActionMasked() == MotionEvent.ACTION_UP) {
			// If the current MotionEvent isn't valid sends the last valid row received again for up.
			// A number of devices always do this, so any code that consumes this should be able to deal with it.
			newEvent = MotionEvent.obtain(e.getDownTime(), lastValidRow.time, MotionEvent.ACTION_UP, lastValidRow.x,
				lastValidRow.y, lastValidRow.pressure, lastValidRow.size, e.getMetaState(), e.getXPrecision(),
				e.getYPrecision(), e.getDeviceId(), e.getEdgeFlags());
		} else {
			newEvent = null;
		}
		
		if (e.getActionMasked() == MotionEvent.ACTION_UP) {
			reset();
		}
		
		return newEvent;
	}

	
	private void reset () {
		queue.clear();
		lastValidRow = null;
	}
	
	
	private static class MotionEventRow {
		public final long time;
		public final float x;
		public final float y;
		public final float pressure;
		public final float size;
		
		
		public MotionEventRow(long time, float x, float y, float pressure, float size) {
			this.time = time;
			this.x = x;
			this.y = y;
			this.pressure = pressure;
			this.size = size;
		}
	}
}