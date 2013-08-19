package com.freehand.organizer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.freehand.note_editor.NoteActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.PaintDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

public class FolderBrowser extends HorizontalScrollView {
	private static final float DRAG_SCROLL_REGION_WIDTH_DIP = 100;
	private static final int ORANGE_HIGHLIGHT = 0xFFFFBB33;
	private static final int NO_COLOR = 0x00FFFFFF;
	private static final float MIN_FOLDER_WIDTH_DIP = 300;
	
	private MainMenuActivity mActivity;
	private final LinearLayout mLayout = new LinearLayout(this.getContext());
	
	private final Set<File> selections = new TreeSet<File>();
	
	private int pxPerFolder = 0;
	private int foldersPerScreen = 0;
	
	private int indexToShow = -1;
	
	private boolean dragInProgress = false;
	private int mDragRegionWidth = 0;
	private long timeSinceDragUpdate = -1;
	private boolean drawLeftHighlight = false;
	private boolean drawRightHighlight = false;

	private int dragStartWatcherView = -1;
	
	
	//****************************************** setup methods ****************************************************
	
	public FolderBrowser(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.setOverScrollMode(HorizontalScrollView.OVER_SCROLL_ALWAYS);
		this.setWillNotDraw(false);
		
		mDragRegionWidth = (int) (DRAG_SCROLL_REGION_WIDTH_DIP * getResources().getDisplayMetrics().density);
		
		final PaintDrawable divider = new PaintDrawable(Color.DKGRAY);
		divider.setIntrinsicWidth((int) (2*getResources().getDisplayMetrics().density + 1));
		mLayout.setDividerDrawable(divider);
		mLayout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE + LinearLayout.SHOW_DIVIDER_END);
		
		this.addView(mLayout);
	}
	
	@Override
	protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
		final int screenWidth = right-left;
		final float scale = getResources().getDisplayMetrics().density;
		final float minFolderWidthPx = MIN_FOLDER_WIDTH_DIP * scale;
		final int numFoldersToShow = (int) (screenWidth/minFolderWidthPx);
		final int folderWidth = (int) (screenWidth/numFoldersToShow);
		
		pxPerFolder = folderWidth;
		foldersPerScreen = numFoldersToShow;
		
		super.onLayout(changed, left, top, right, bottom);
		
		for (int i = 0; i < mLayout.getChildCount(); i++) {
			mLayout.getChildAt(i).getLayoutParams().width = pxPerFolder;
		}
	}
	
	public void setRootDirectory (File root) {
		mLayout.removeAllViews();
		openFolder(root);
	}
	
	public void setMainMenuActivity (MainMenuActivity activity) {
		mActivity = activity;
	}
	
	//*************************************** drag starting and touch event routing methods ******************************************
	
	/*		A NOTE ABOUT CUSTOM TOUCH EVENT HANDELING FOR STARTING DRAG EVENTS IN THE FolderBrowser/FolderView PAIR
	 * 											08/16/2013
	 * 
	 * Because touch event handling in Android is so fucked up, making the organizer UI work well requires rewriting a
	 * lot of the functionality normally provided by the Android UI framework when we might start a drag event. When a
	 * touch event starts the FolderBrowser intercepts it and checks with each FolderView to see if it's over a selected
	 * file. If it is, a variable inside of FolderBrowser is set telling it to dispatch all touch events directly to the
	 * FolderView in question's onWatchingForDragTouchEvent method. Inside of that method, item clicks and long clicks
	 * are detected, as is a drag event starting.
	 * 
	 * Starting a drag directly after a long press (without the finger being lifted) is handled internally by FolderView.
	 * 
	 * In all other cases touch events are handled the normal way. It's worth noting, however, that HorizontalScrollView,
	 * which FolderBrowser inherits from, seems to override onInterceptTouchEvent and take over if it detects a horizontal
	 * drag. That's expected, but can sometimes get in the way of more complicated touch event handling like what's going
	 * on here.
	 */
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {	
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			dragStartWatcherView = -1;
			for(int i = 0; i < mLayout.getChildCount(); i++) {
				FolderView current = (FolderView) mLayout.getChildAt(i);
				if (current.checkStartWatchingForDrag(event.getX()-current.getLeft()+getScrollX(), event.getY()-current.getTop()) == true) {
					dragStartWatcherView = i;
					return true;
				}
			}
		}

		return super.onInterceptTouchEvent(event);
	}
	
	@Override
	public boolean onTouchEvent (MotionEvent event) {
		if (dragStartWatcherView >= 0) {
			FolderView dispatchTarget = (FolderView) mLayout.getChildAt(dragStartWatcherView);
			event.setLocation(event.getX()-dispatchTarget.getLeft()+getScrollX(), event.getY()-dispatchTarget.getTop());
			dispatchTarget.onWatchingForDragTouchEvent(event);
			return true;
		}
		
		if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
			dragStartWatcherView = -1;
		}
		
		boolean returnValue = false;
		try {
			returnValue = super.onTouchEvent(event);
		} catch (IllegalArgumentException e) {
			Log.d("PEN", "Error");
		}
		
		return returnValue;
	}
	
	
	
	//********************************************* drag in progress methods **************************************************
	
	public void startDrag() {
		dragInProgress = true;
		
		int itemCount = selections.size();
		if (itemCount <= 0) { return; }
		
		NoteMovementDragShadowBuilder shadowBuilder = new NoteMovementDragShadowBuilder(itemCount, (int) (175.0f*getResources().getDisplayMetrics().density));
		this.startDrag(null, shadowBuilder, null, 0);
		dragStartWatcherView = -1;
		
		for (int i = 0; i < mLayout.getChildCount(); i++) {
			FolderView toUpdate = (FolderView) mLayout.getChildAt(i);
			toUpdate.notifyDataSetChanged();
		}
	}
	
	@Override
	public boolean onDragEvent (DragEvent event) {
		if (event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
			dragInProgress = false;
			invalidate();
		}
		
		// If the drag event is a drop, call the move method in the presenter with the
		//  child view the drop is over as a parameter
		if (event.getAction() == DragEvent.ACTION_DROP) {
			for (int i = 0; i < mLayout.getChildCount(); i++) {
				FolderView toTest = (FolderView) mLayout.getChildAt(i);
				if (pointInView(toTest, getScrollX() + event.getX(), getScrollY() + event.getY())) {
					this.moveSelectionsToDirectory(toTest.folder);
				}
				toTest.dragExitedListener();
			}
			return true;
		}
		
		// If the drag event leaves the FolderBrowser or ends without a drop make sure all of the highlights are cleared
		if (event.getAction() == DragEvent.ACTION_DRAG_EXITED || event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
			for (int i = 0; i < mLayout.getChildCount(); i++) {
				((FolderView) mLayout.getChildAt(i)).dragExitedListener();
			}
		}
		
		// Check to see HorizontalScrollView wants to scroll horizontally
		if (event.getAction() == DragEvent.ACTION_DRAG_LOCATION || event.getAction() == DragEvent.ACTION_DRAG_STARTED) {
			if (horizontalScrollDragListener(event) == true) {
				for (int i = 0; i < mLayout.getChildCount(); i++) {
					((FolderView) mLayout.getChildAt(i)).dragExitedListener();
				}
				return true;
			}
		}
		
		// Figure out which child view the DragEvent is over and send them the coordinates
		//  of the event. Send all of the other children the info that they aren't selected.
		if (event.getAction() == DragEvent.ACTION_DRAG_LOCATION || event.getAction() == DragEvent.ACTION_DRAG_STARTED) {
			for (int i = 0; i < mLayout.getChildCount(); i++) {
				FolderView toUpdate = (FolderView) mLayout.getChildAt(i);
				if (pointInView(toUpdate, getScrollX() + event.getX(), getScrollY() + event.getY())) {
					toUpdate.dragListener(event.getX() - toUpdate.getLeft()+getScrollX(), event.getY() - toUpdate.getTop());
				} else {
					toUpdate.dragExitedListener();
				}
			}
			
			return true;
		}
				
		return true;
	}
	
	private boolean horizontalScrollDragListener (DragEvent event) {
		if (this.canScrollHorizontally(-1) && event.getX() <= mDragRegionWidth) {
			drawLeftHighlight = true;
			drawRightHighlight = false;
				
			if (timeSinceDragUpdate > 0) {
				final long currentTime = System.currentTimeMillis();
				this.smoothScrollBy((int)((timeSinceDragUpdate-currentTime)/1.5f), 0);
			}
			
			timeSinceDragUpdate = System.currentTimeMillis();
			this.invalidate();
			return true;
		} else if (this.canScrollHorizontally(1) && event.getX() >= (this.getWidth() - mDragRegionWidth)) {
			drawLeftHighlight = false;
			drawRightHighlight = true;
			
			if (timeSinceDragUpdate > 0) {
				final long currentTime = System.currentTimeMillis();
				this.smoothScrollBy((int) ((currentTime-timeSinceDragUpdate)/1.5f), 0);
			}
			
			timeSinceDragUpdate = System.currentTimeMillis();
			this.invalidate();
			return true;
		} else {
			drawLeftHighlight = false;
			drawRightHighlight = false;
			this.invalidate();
			
			timeSinceDragUpdate = -1;
			return false;
		}
	}
	
	private boolean pointInView (View v, float x, float y) {
		if (x >= v.getLeft() && x <= v.getRight() && y >= v.getTop() && y <= v.getBottom()) {
			return true;
		} else {
			return false;
		}
	}
	
	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw (Canvas canvas) {
		super.onDraw(canvas);
		
		if (dragInProgress && drawLeftHighlight) {
			Rect highlightRect = new Rect(this.getScrollX(), 0, this.getScrollX() + mDragRegionWidth, this.getHeight());
			Shader highlightShader = new LinearGradient(this.getScrollX(), 0, this.getScrollX() + mDragRegionWidth, 0, ORANGE_HIGHLIGHT, NO_COLOR, Shader.TileMode.CLAMP);
			Paint highlightPaint = new Paint();
			highlightPaint.setShader(highlightShader);
			
			canvas.drawRect(highlightRect, highlightPaint);
		} else if (dragInProgress && drawRightHighlight) {
			Rect highlightRect = new Rect(this.getScrollX() + this.getWidth() - mDragRegionWidth, 0, this.getScrollX() + this.getWidth(), this.getHeight());
			Shader highlightShader = new LinearGradient(this.getScrollX() + this.getWidth(), 0, this.getScrollX() + this.getWidth() - mDragRegionWidth, 0, ORANGE_HIGHLIGHT, NO_COLOR, Shader.TileMode.CLAMP);
			Paint highlightPaint = new Paint();
			highlightPaint.setShader(highlightShader);
			
			canvas.drawRect(highlightRect, highlightPaint);
		}
	}
	
	public boolean dragInProgress () {
		return dragInProgress;
	}
	
	
	
	//*************************************** organization mutation methods****************************************
	
	/**
	 * @return true if the file is now selected, false if it is no longer selected
	 */
	public boolean toggleSelection(File toToggle) {
		if (selections.remove(toToggle) == false) {
			selections.add(toToggle);
		}
		selectionsChanged();
		return selections.contains(toToggle);
	}
	
	public void removeAllSelections () {
		selections.clear();
		selectionsChanged();
	}
	
	public boolean getFileSelectionStatus (File toGet) {
		return selections.contains(toGet);
	}
	
	private void selectionsChanged () {
		if (selections.size() == 0) {
			mActivity.setDefaultActionBarOn();
		} else {
			mActivity.setItemsSelectedActionBarOn();
		}
	}
	
	private void moveSelectionsToDirectory (File destination) {
		//List<File> selections = this.getSelections();
		
	}
	
	public void cancleSelections () {
		selections.clear();
	}
	

	
	public void deleteSelections () {
		
	}
	
	public void shareSelections () {
		
	}
	
	
	public void createNewFile (String name, boolean isFolder) {
		
	}
	
	
	
	
	//************************************************** Folder and Note opening ***********************************************
	public void openFile (File toOpen) {
		if (toOpen.isDirectory() == false) {
			openNote(toOpen);
		} else {
			openFolder(toOpen);
		}
	}
	
	private void openNote (File toOpen) {
		Intent i = new Intent(this.getContext(), NoteActivity.class);
		i.putExtra("note_path", toOpen.getPath());
		i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		this.getContext().startActivity(i);
	}
	
	private void openFolder (File folder) {
		if (getDisplayStatus(folder) == true) { return; }

		int targetIndex = 0;
		for ( ; targetIndex < mLayout.getChildCount(); targetIndex++) {
			File f = ((FolderView) mLayout.getChildAt(targetIndex)).folder;
			if (folder.getPath().startsWith(f.getPath()+"/") == false) {
				break;
			}
		}
		
		while (targetIndex < mLayout.getChildCount()) {
			mLayout.removeViewAt(targetIndex);
		}
		
		FolderView newView = new FolderView(this.getContext(), this, folder);
		newView.setLayoutParams(new LinearLayout.LayoutParams(pxPerFolder, LayoutParams.MATCH_PARENT));
		mLayout.addView(newView);
		requestShow(folder);
	}
	
	
	// ****************************************** non-drag scrolling methods ****************************************
	
	@Override
	public void computeScroll () {
		super.computeScroll();
		
		if (indexToShow >= 0) {
			final float leftFolderPos = ((float) getScrollX()) / ((float) pxPerFolder);
			if (indexToShow < leftFolderPos) {
				this.smoothScrollTo(indexToShow*pxPerFolder, 0);
			} else if (indexToShow > leftFolderPos + foldersPerScreen-1) {
				this.smoothScrollTo((indexToShow-foldersPerScreen+1) * pxPerFolder, 0);
			}
			indexToShow = -1;
		}
	}
	
	/**
	 * Request that this class and its associated FolderBrowserScrollView display the View toShow.
	 * @param toShow will be displayed if possible
	 */
	private void requestShow (File toShow) {
		for (int i = 0; i < mLayout.getChildCount(); i++) {
			if (((FolderView) mLayout.getChildAt(i)).folder.equals(toShow)) {
				indexToShow = i;
				return;
			}
		}
	}
	
	public boolean getDisplayStatus (File f) {
		for (int i = 0; i < mLayout.getChildCount(); i++) {
			if (((FolderView) mLayout.getChildAt(i)).folder.equals(f)) {
				return true;
			}
		}
		return false;
	}
}