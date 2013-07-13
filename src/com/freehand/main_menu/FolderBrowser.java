package com.freehand.main_menu;

import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

public class FolderBrowser extends HorizontalScrollView {
	private static final long DRAG_SCROLL_TIMER = 400;
	private static final float DRAG_SCROLL_REGION_WIDTH_DIP = 100;
	private static final int ORANGE_HIGHLIGHT = 0xFFFFBB33;
	private static final int NO_COLOR = 0x00FFFFFF;
	private static final float MIN_FOLDER_WIDTH_DIP = 300;
	
	FolderBrowserLayout mLayout = new FolderBrowserLayout(this.getContext());
	private MainMenuPresenter mPresenter = null;
	
	
	private int pxPerFolder = 0;
	private int foldersPerScreen = 0;
	
	private boolean triggerScrollRight = false;
	private boolean triggerFixScroll = false;
	
	private int scrollCounter = 0;
	private int oldScrollCounter = 0;
	
	private boolean scrollInProgress = false;
	
	private int mDragRegionWidth = 0;
	private long timeEnteredDragRegion = -1;
	private boolean drawLeftHighlight = false;
	private boolean drawRightHighlight = false;

	public FolderBrowser(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.setOverScrollMode(HorizontalScrollView.OVER_SCROLL_ALWAYS);
		this.setWillNotDraw(false);
		
		mDragRegionWidth = (int) (DRAG_SCROLL_REGION_WIDTH_DIP * getResources().getDisplayMetrics().density);
		
		this.addView(mLayout);
	}
	
	public void setPresenter (MainMenuPresenter newPresenter) {
		mPresenter = newPresenter;
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		// First, pass the touch event to children to see if they want to do anything with a priority higher than scrolling
		boolean runMyTouchStuff = true;
		for (int i = 0; i < this.getChildCount(); i++) {
			if (((FolderBrowserLayout) this.getChildAt(i)).checkConsumeTouchEvent(event) == true) {
				runMyTouchStuff = false;
			}
		}
		
		if (runMyTouchStuff == true) {
			return super.onInterceptTouchEvent(event);
		}
		
		return false;
	}
	
	
	// OVERSCROLL TEST AREA
	
//	boolean hasOverscrolled = false;
//	
//	@Override
//	public boolean onTouchEvent (MotionEvent event) {
//		
//		if (hasOverscrolled == false) {
//			//this.overScrollBy(1000, 0, 0, 0, 10000, 0, 10000, 0, false);
//		}
//		hasOverscrolled = true;
//		
//		
//		super.onTouchEvent(event);
//		
//		
//		
//		return true;
//	}
//	
//	
//	@Override
//	protected void onOverScrolled (int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
//		Log.d("PEN", "onOverScrolled called, clampedX == " + Boolean.toString(clampedX));
//	}
//	
//	@Override
//	protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
//		
//		
//		return super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX, scrollRangeY, 10000, maxOverScrollY, isTouchEvent);
//		
//	}
//	
//	
	
	
	
	
	
	// TEST AREA END
	
	
	
	
	private void setScrollCounter(int newValue) {
		//Scrolling left
		if (scrollCounter > newValue) {			
			oldScrollCounter = newValue;
			scrollCounter = newValue;
			this.smoothScrollTo(((scrollCounter-foldersPerScreen) * pxPerFolder), 0);
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
			this.setScrollX((oldScrollCounter-foldersPerScreen) * pxPerFolder);
			triggerFixScroll = false;
		}
		
		if (triggerScrollRight == true) {
			if (oldScrollCounter != scrollCounter) {
				this.smoothScrollTo(((scrollCounter-foldersPerScreen) * pxPerFolder), 0);
			}
			triggerScrollRight = false;
		}
	}
	
	public int getScrollCounter() {
		return scrollCounter;
	}
	
	public boolean dragScrollListener (DragEvent event) {
		if (event.getX()-this.getScrollX() <= mDragRegionWidth && this.getScrollX() >= pxPerFolder - 3) {
			drawLeftHighlight = true;
			drawRightHighlight = false;
			this.invalidate();
				
			if (timeEnteredDragRegion == -1) {
				timeEnteredDragRegion = System.currentTimeMillis();
			} else if (System.currentTimeMillis() - timeEnteredDragRegion >= DRAG_SCROLL_TIMER) {
				timeEnteredDragRegion = -1;
				oldScrollCounter = scrollCounter;
				scrollCounter -= 1;
				this.smoothScrollTo((scrollCounter - foldersPerScreen) * pxPerFolder, 0);
			}
			
			return true;
		} else if (event.getX()-this.getScrollX() >= (this.getWidth() - mDragRegionWidth) && this.getScrollX() <= getMaxScrollX() - pxPerFolder + 3) {
			drawLeftHighlight = false;
			drawRightHighlight = true;
			this.invalidate();
			
			if (timeEnteredDragRegion == -1) {
				timeEnteredDragRegion = System.currentTimeMillis();
				
			} else if (System.currentTimeMillis() - timeEnteredDragRegion >= DRAG_SCROLL_TIMER) {
				timeEnteredDragRegion = -1;
				oldScrollCounter = scrollCounter;
				scrollCounter += 1;
				this.smoothScrollTo((scrollCounter - foldersPerScreen) * pxPerFolder, 0);
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
		super.onDraw(canvas);
		
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
	
	@Override
	protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
		final int screenWidth = right-left;
		final float scale = getResources().getDisplayMetrics().density;
		final float minFolderWidthPx = MIN_FOLDER_WIDTH_DIP * scale;
		final int numFoldersToShow = (int) (screenWidth/minFolderWidthPx);
		final int folderWidth = (int) (screenWidth/numFoldersToShow);
		
		//Log.d("PEN", "folderWidth == " + Integer.toString(folderWidth) + "   foldersPerScreen == " + Integer.toString(numFoldersToShow));
		
		pxPerFolder = folderWidth;
		foldersPerScreen = numFoldersToShow;
		
		super.onLayout(changed, left, top, right, bottom);
	}
	
	
	public void requestShow (FolderView toShow) {
		mLayout.requestShow(toShow);
	}
	
	public void requestUpdateViews (final List<FolderView> newViews) {
		mLayout.requestUpdateViews(newViews);
	}
	
	
	
	
	
	
	
	
	
	
	
	private class FolderBrowserLayout extends LinearLayout {
		
		public FolderBrowserLayout(Context context) {
			super(context);
		}
		
		
		public void requestUpdateViews (final List<FolderView> newViews) {
			setScrollCounter(newViews.size());
			if (scrollCounter > newViews.size()) {
				Runnable newRunnable = new Runnable () {
					public void run() {
						if (scrollInProgress == true) {
							FolderBrowser.this.postDelayed(this, 10);
						} else {
							updateViews(newViews);
						}
					}
				};
				this.postDelayed(newRunnable, 10);
			} else {
				updateViews(newViews);
			}
		}
		
		private void updateViews (List<FolderView> newViews) {
			int i = 0;
			for ( ; i<newViews.size(); i++) {
				newViews.get(i).setId(i+1);
				
				if ((newViews.get(i) == this.getChildAt(i) && this.getChildAt(i).getWidth() == pxPerFolder) == false) {
					newViews.get(i).setLayoutParams(new LinearLayout.LayoutParams(pxPerFolder, LayoutParams.MATCH_PARENT));
					
					if (this.getChildAt(i) != null) {
						this.removeViewAt(i);
					}
					this.addView(newViews.get(i), i);
				}
			}
			
			for (; i < this.getChildCount();) {
				this.removeViewAt(i);
			}
		}
		
		/**
		 * Request that this class and its associated FolderBrowserScrollView display the View toShow.
		 * @param toShow will be displayed if possible
		 */
		public void requestShow (FolderView toShow) {
			for (int i = 0; i < this.getChildCount(); i++) {
				if (this.getChildAt(i) == toShow) {
					if (i < (scrollCounter-foldersPerScreen) || i >= scrollCounter) {
						setScrollCounter(i+1);
					}
					return;
				}
			}
		}
		
		public boolean checkConsumeTouchEvent (MotionEvent event) {
			boolean eventConsumed = false;
			
			for(int i = 0; i < this.getChildCount(); i++) {
				if (((FolderView) this.getChildAt(i)).checkConsumeTouchEvent(event) == true) {
					eventConsumed = true;
				}
			}
			
			return eventConsumed;
		}
		
		//*************************************** Drag and Drop stuff ***************************************
		
		@Override
		public boolean onDragEvent (DragEvent event) {
			
			// Don't do anything while scrolling
			if (scrollInProgress == true) {
				return true;
			}
			
			// If the drag event is a drop, call the move method in the presenter with the
			//  child view the drop is over as a parameter
			if (event.getAction() == DragEvent.ACTION_DROP) {
				for (int i = 0; i < this.getChildCount(); i++) {
					FolderView toTest = (FolderView) this.getChildAt(i);
					if (pointInView(event.getX(), event.getY(), toTest)) {
						mPresenter.moveTo(toTest);
						mPresenter.setSelectedFolderView(toTest);
					}
					toTest.dragExitedListener();
				}
				
				return true;
			}
			
			// If the drag event leaves the FolderBrowser or ends without a drop make sure all of the highlights are cleared
			if (event.getAction() == DragEvent.ACTION_DRAG_EXITED || event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
				for (int i = 0; i < this.getChildCount(); i++) {
					((FolderView) this.getChildAt(i)).dragExitedListener();
				}
			}
			
			// Check to see if the FolderBrowserScrollView is waiting for a valid scroll action
			if (event.getAction() == DragEvent.ACTION_DRAG_LOCATION || event.getAction() == DragEvent.ACTION_DRAG_STARTED) {
				if (dragScrollListener(event) == true) {
					for (int i = 0; i < this.getChildCount(); i++) {
						((FolderView) this.getChildAt(i)).dragExitedListener();
					}
					
					return true;
				}
			}
			
			// Figure out which child view the DragEvent is over and send them the coordinates
			//  of the event. Send all of the other children the info that they aren't selected.
			if (event.getAction() == DragEvent.ACTION_DRAG_LOCATION || event.getAction() == DragEvent.ACTION_DRAG_STARTED) {
				mPresenter.setSelectedFolderView(null);
				for (int i = 0; i < this.getChildCount(); i++) {
					FolderView toUpdate = (FolderView) this.getChildAt(i);
					if (pointInView(event.getX(), event.getY(), toUpdate)) {
						toUpdate.dragListener(event.getX() - toUpdate.getLeft(), event.getY() - toUpdate.getTop());
					} else {
						toUpdate.dragExitedListener();
					}
				}
				
				return true;
			}
			
			return true;
		}
		
		private boolean pointInView (float x, float y, View v) {
			if (x >= v.getLeft() && x <= v.getRight() && y >= v.getTop() && y <= v.getBottom()) {
				return true;
			} else {
				return false;
			}
		}
		
		
		@Override
		protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
			
			super.onLayout(changed, left, top, right, bottom);
			
			for (int i = 0; i < this.getChildCount(); i++) {
				this.getChildAt(i).getLayoutParams().width = pxPerFolder;
			}
			
			
			
		}
	}
	
}