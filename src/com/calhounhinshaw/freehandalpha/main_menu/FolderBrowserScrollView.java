package com.calhounhinshaw.freehandalpha.main_menu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;

public class FolderBrowserScrollView extends HorizontalScrollView {
	private static final long DRAG_SCROLL_TIMER = 400;
	private static final int ORANGE_HIGHLIGHT = 0xFFFFBB33;
	private static final int NO_COLOR = 0x00FFFFFF;
	
	private int mScrollIncrement = 0;
	private int mIncrementsPerScreen = 0;
	
	private boolean triggerScrollRight = false;
	private boolean triggerFixScroll = false;
	
	private int scrollCounter = 0;
	private int oldScrollCounter = 0;
	
	private boolean scrollInProgress = false;
	
	private int lastManualScroll = 0;
	private float lastX = 0;
	private float lastLastX = 0;
	
	private int mDragRegionWidth = 0;
	private long timeEnteredDragRegion = -1;
	private boolean drawLeftHighlight = false;
	private boolean drawRightHighlight = false;

	public FolderBrowserScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.setOverScrollMode(HorizontalScrollView.OVER_SCROLL_ALWAYS);
		this.setWillNotDraw(false);
	}
	
	public void setParameters (int newIncrement, int newIncrementsPerScreen, int newDragRegionWidth) {
		mScrollIncrement = newIncrement;
		mIncrementsPerScreen = newIncrementsPerScreen;
		mDragRegionWidth = newDragRegionWidth;
	}
	
	@Override
	protected void onScrollChanged (int left, int top, int oldLeft, int oldTop) {
		super.onScrollChanged(left, top, oldLeft, oldTop);
		lastManualScroll = left;
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		// First, pass the touch event to children to see if they want to do anything with a priority higher than scrolling
		boolean runMyTouchStuff = true;
		for (int i = 0; i < this.getChildCount(); i++) {
			if (((FolderBrowser) this.getChildAt(i)).checkConsumeTouchEvent(event) == true) {
				runMyTouchStuff = false;
			}
		}
		
		if (runMyTouchStuff == true) {
			return super.onInterceptTouchEvent(event);
		}
		
		return false;
	}
	
	
	@Override
	public boolean onTouchEvent (MotionEvent event) {
		super.onTouchEvent(event);
		
		if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) && mScrollIncrement > 0) {
			
			// Calling velocity, may need a time component to work well
			float velocity = lastLastX - lastX;
			
			float position =  (float) (((double) lastManualScroll)/((double) mScrollIncrement)) - ((float) scrollCounter) + mIncrementsPerScreen;
			int definateDeltaPosition = (int) position;
			float positionDecisionValue = position - definateDeltaPosition;
			
			if (positionDecisionValue + velocity/10 <= -0.5) {
				definateDeltaPosition -= 1;
			} else if (positionDecisionValue + velocity/10 >= 0.5) {
				definateDeltaPosition += 1;
			}

			oldScrollCounter = scrollCounter;
			scrollCounter += definateDeltaPosition;
			this.smoothScrollTo((scrollCounter - mIncrementsPerScreen) * mScrollIncrement, 0);
		}
		
		lastLastX = lastX;
		lastX = event.getX();
		
		return true;
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
	
	public boolean dragScrollListener (DragEvent event) {
		if (event.getX()-this.getScrollX() <= mDragRegionWidth && this.getScrollX() >= mScrollIncrement - 3) {
			drawLeftHighlight = true;
			drawRightHighlight = false;
			this.invalidate();
				
			if (timeEnteredDragRegion == -1) {
				timeEnteredDragRegion = System.currentTimeMillis();
			} else if (System.currentTimeMillis() - timeEnteredDragRegion >= DRAG_SCROLL_TIMER) {
				timeEnteredDragRegion = -1;
				oldScrollCounter = scrollCounter;
				scrollCounter -= 1;
				this.smoothScrollTo((scrollCounter - mIncrementsPerScreen) * mScrollIncrement, 0);
			}
			
			return true;
		} else if (event.getX()-this.getScrollX() >= (this.getWidth() - mDragRegionWidth) && this.getScrollX() <= getMaxScrollX() - mScrollIncrement + 3) {
			drawLeftHighlight = false;
			drawRightHighlight = true;
			this.invalidate();
			
			if (timeEnteredDragRegion == -1) {
				timeEnteredDragRegion = System.currentTimeMillis();
				
			} else if (System.currentTimeMillis() - timeEnteredDragRegion >= DRAG_SCROLL_TIMER) {
				timeEnteredDragRegion = -1;
				oldScrollCounter = scrollCounter;
				scrollCounter += 1;
				this.smoothScrollTo((scrollCounter - mIncrementsPerScreen) * mScrollIncrement, 0);
			}
			
			return true;
		} else {
			drawLeftHighlight = false;
			drawRightHighlight = false;
			this.invalidate();
			
			timeEnteredDragRegion = -1;
			return false;
		}
	}
	
	private int getMaxScrollX () {
		return this.getChildAt(0).getMeasuredWidth()- this.getWidth();
	}
	
	@Override
	protected void dispatchDraw (Canvas canvas) {
		super.dispatchDraw(canvas);
		
		
	}
	
	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw (Canvas canvas) {
		//super.onDraw(canvas);
		
		if (drawLeftHighlight == true) {
			Rect highlightRect = new Rect(this.getScrollX(), 0, this.getScrollX() + mDragRegionWidth, this.getHeight());
			Shader highlightShader = new LinearGradient(this.getScrollX(), 0, this.getScrollX() + mDragRegionWidth, 0, ORANGE_HIGHLIGHT, NO_COLOR, Shader.TileMode.CLAMP);
			Paint highlightPaint = new Paint();
			highlightPaint.setShader(highlightShader);
			
			canvas.drawRect(highlightRect, highlightPaint);
		} else if (drawRightHighlight == true) {
			Rect highlightRect = new Rect(this.getScrollX() + this.getWidth() - mDragRegionWidth, 0, this.getScrollX() + this.getWidth(), this.getHeight());
			Shader highlightShader = new LinearGradient(this.getScrollX() + this.getWidth(), 0, this.getScrollX() + this.getWidth() - mDragRegionWidth, 0, ORANGE_HIGHLIGHT, NO_COLOR, Shader.TileMode.CLAMP);
			Paint highlightPaint = new Paint();
			highlightPaint.setShader(highlightShader);
			
			canvas.drawRect(highlightRect, highlightPaint);
		}
	}
	
}