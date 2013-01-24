package com.calhounhinshaw.freehandalpha.main_menu;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

public class FolderBrowser extends RelativeLayout {
	private static final float MIN_CHILD_WIDTH_DIP = 300;
	private static final float DRAG_SCROLL_REGION_WIDTH_DIP = 100;
	
	private final FolderBrowserScrollView mParentView;
	private MainMenuPresenter mPresenter = null;
	
	private int childWidth = -1;
	private int childrenPerScreen = -1;
	
	private int dragScrollRegionWidth = -1;
	
	public FolderBrowser(Context context, FolderBrowserScrollView scrollView) {
		super(context);
		mParentView = scrollView;
	}
	
	public void setPresenter(MainMenuPresenter newPresenter) {
		mPresenter = newPresenter;
	}
	
	public void requestUpdateViews (final List<View> newViews) {
		int oldScrollCounter = mParentView.getScrollCounter();
		mParentView.setScrollCounter(newViews.size());
		if (oldScrollCounter > newViews.size()) {
			Runnable newRunnable = new Runnable () {
				public void run() {
					if (mParentView.isScrollInProgress()) {
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
	
	private void updateViews (List<View> newViews) {
		Log.d("PEN", Integer.toString(newViews.size()));
		
		int i = 0;
		for ( ; i<newViews.size(); i++) {
			newViews.get(i).setId(i+1);
			
			if (newViews.get(i) == this.getChildAt(i) && this.getChildAt(i).getWidth() == childWidth) {
				
			} else if (i == 0) {
				
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(childWidth, LayoutParams.MATCH_PARENT);
				params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				newViews.get(i).setLayoutParams(params);
				
				if (this.getChildAt(i) != null) {
					this.removeViewAt(i);
				}
				this.addView(newViews.get(i), i);
			} else {
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(childWidth, LayoutParams.MATCH_PARENT);
				params.addRule(RelativeLayout.RIGHT_OF, this.getChildAt(i-1).getId());
				params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				newViews.get(i).setLayoutParams(params);
				
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
	
	
	@Override
	protected void dispatchDraw (Canvas canvas) {
		if (childWidth == -1) {
			setChildWidth();
		}
		
		super.dispatchDraw(canvas);
	}
	
	private void setChildWidth () {
		final float parentWidthPx = mParentView.getWidth();
		
		if (parentWidthPx > 0) {
			final float scale = getResources().getDisplayMetrics().density;
			final float minChildWidthPx = MIN_CHILD_WIDTH_DIP * scale;
			float numFoldersToShow = parentWidthPx/minChildWidthPx;
			childWidth = (int) (parentWidthPx/((int)numFoldersToShow));
			childrenPerScreen = (int)numFoldersToShow;
			
			ArrayList<View> views = new ArrayList<View>(this.getChildCount());
			for (int i = 0; i < this.getChildCount(); i++) {
				views.add(this.getChildAt(i));
			}
			requestUpdateViews(views);

			dragScrollRegionWidth = (int) (DRAG_SCROLL_REGION_WIDTH_DIP * scale);
			mParentView.setParameters(childWidth, childrenPerScreen, dragScrollRegionWidth);
		} else {
			childWidth = -1;
		}
	}
	
	/**
	 * Request that this class and its associated FolderBrowserScrollView display the View toShow.
	 * @param toShow will be displayed if possible
	 */
	public void requestShow (View toShow) {
		for (int i = 0; i < this.getChildCount(); i++) {
			if (this.getChildAt(i) == toShow) {
				if (i < mParentView.getScrollCounter()-childrenPerScreen || i >= mParentView.getScrollCounter()) {
					mParentView.setScrollCounter(i+1);
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
		if (mParentView.isScrollInProgress()) {
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
			if (mParentView.dragScrollListener(event) == true) {
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
}