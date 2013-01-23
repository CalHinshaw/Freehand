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
	
	private boolean triggerScrollRight = false;
	private boolean triggerFixScroll = false;
	
	private int scrollCounter = 0;
	private int oldScrollCounter = 0;
	
	private boolean scrollInProgress = false;
	
	private int lastManualScroll = 0;

	public FolderBrowserScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.setOverScrollMode(HorizontalScrollView.OVER_SCROLL_ALWAYS);
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
		//Scrolling left
		if (scrollCounter > newValue) {			
			oldScrollCounter = newValue;
			scrollCounter = newValue;
			this.smoothScrollTo(((scrollCounter-mIncrementsPerScreen) * mScrollIncrement), 0);
			triggerFixScroll = false;
			scrollInProgress = true;
		} else if (scrollCounter < newValue){
			oldScrollCounter = scrollCounter;
			scrollCounter = newValue;
			triggerFixScroll = true;
			triggerScrollRight = true;
		} else {
			oldScrollCounter = newValue;
			scrollCounter = newValue;
			triggerFixScroll = true;
		}
	}
	
	@Override
	public void computeScroll() {
		int temp = this.getScrollX();
		
		super.computeScroll();
		
		// Check to see if a scroll is in progress
		if (temp == this.getScrollX()) {
			scrollInProgress = false;
		} else {
			scrollInProgress = true;
		}
		
		if (triggerFixScroll == true) {
			this.setScrollX((oldScrollCounter-mIncrementsPerScreen) * mScrollIncrement);
			triggerFixScroll = false;
		}
		
		if (triggerScrollRight == true) {
			if (oldScrollCounter != scrollCounter) {
				this.smoothScrollTo(((scrollCounter-mIncrementsPerScreen) * mScrollIncrement), 0);
			}
			triggerScrollRight = false;
		}
	}
	
	public int getScrollCounter() {
		return scrollCounter;
	}
	
	public boolean isScrollInProgress () {
		return scrollInProgress;
	}
	
}