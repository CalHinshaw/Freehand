package com.calhounhinshaw.freehandalpha.main_menu;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

public class FolderBrowserScrollView extends HorizontalScrollView {
	private int mScrollIncrement = 0;
	private int mIncrementsPerScreen = 0;
	
	private boolean scrollDirty = false;
	private int scrollCounter = 0;
	private int oldScrollCounter = 0;
	
	private int lastManualScroll = 0;

	public FolderBrowserScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void setScrollIncrement (int newIncrement, int newIncrementsPerScreen) {
		mScrollIncrement = newIncrement;
		mIncrementsPerScreen = newIncrementsPerScreen;
	}
	
	@Override
	protected void onScrollChanged (int left, int top, int oldLeft, int oldTop) {
		super.onScrollChanged(left, top, oldLeft, oldTop);
		lastManualScroll = left;
	}
	
	@Override
	public boolean onTouchEvent (MotionEvent event) {
		boolean returnValue = super.onTouchEvent(event);
		
		if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) && mScrollIncrement > 0) {
			int scrolled = Math.round(((float) lastManualScroll)/((float) mScrollIncrement));
			this.smoothScrollTo(mScrollIncrement * scrolled, 0);
			scrollCounter = scrolled+mIncrementsPerScreen;
			oldScrollCounter = scrollCounter;
		}
		
		return returnValue;
	}
	
	public void setScrollCounter(int newValue) {
		oldScrollCounter = scrollCounter;
		scrollCounter = newValue;
		scrollDirty = true;
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		
		if (scrollDirty == true) {
			
			Log.d("PEN", "old:  " + Integer.toString(oldScrollCounter) + "      new:  " + Integer.toString(scrollCounter));
			
			
			if (oldScrollCounter != scrollCounter) {
				this.smoothScrollTo(((scrollCounter-mIncrementsPerScreen) * mScrollIncrement), 0);
			}
			
			scrollDirty = false;
		}
		
		
		super.onDraw(canvas);
		
	}
	
	@Override
	public void computeScroll() {
		super.computeScroll();
		if (scrollDirty == true) {
			//TODO pretty choppy, need to fix it.
			this.overScrollBy(9, 0, ((oldScrollCounter-mIncrementsPerScreen) * mScrollIncrement), 0, 100000, 0, 100000, 0, false);
		}
	}
	
	
	
}