package com.freehand.organizer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.freehand.note_editor.NoteActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
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
	
	private final LinearLayout mLayout = new LinearLayout(this.getContext());
	
	private int pxPerFolder = 0;
	private int foldersPerScreen = 0;
	
	private int indexToShow = -1;
	
	private boolean dragInProgress = false;
	private int mDragRegionWidth = 0;
	private long timeSinceDragUpdate = -1;
	private boolean drawLeftHighlight = false;
	private boolean drawRightHighlight = false;

	public FolderBrowser(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.setOverScrollMode(HorizontalScrollView.OVER_SCROLL_ALWAYS);
		this.setWillNotDraw(false);
		
		mDragRegionWidth = (int) (DRAG_SCROLL_REGION_WIDTH_DIP * getResources().getDisplayMetrics().density);
		
		this.addView(mLayout);
	}
	
	public void setRootDirectory (File root) {
		mLayout.removeAllViews();
		openFolder(root);
	}
	
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		// I need to check to see if the user is starting a drag on an already selected file
		// inside of one of the FolderViews - if they aren't I pass the MotionEvent to super
		// so It can scroll. This check belongs here because I only want to check to see if
		// the pointer is over the already selected files at the beginning of the event.
		
		boolean eventConsumed = false;
		for(int i = 0; i < mLayout.getChildCount(); i++) {
			FolderView current = (FolderView) mLayout.getChildAt(i);
			if (current.checkConsumeTouchEvent(event.getX()-current.getLeft(), event.getY()-current.getTop()) == true) {
				eventConsumed = true;
			}
		}
		
		if (eventConsumed == false) {
			return super.onInterceptTouchEvent(event);
		} else {
			return false;
		}
	}	
	
	//********************************************* Drag Methods **************************************************
	public void startDrag() {
		
		Log.d("PEN", "startDrag called");
		
		int itemCount = getSelections().size();
		if (itemCount <= 0) { return; }
		
		Log.d("PEN", "startDrag past guard clause");
		
		NoteMovementDragShadowBuilder shadowBuilder = new NoteMovementDragShadowBuilder(itemCount, 350);
		this.startDrag(null, shadowBuilder, null, 0);
		dragInProgress = true;
	}
	
	public boolean dragInProgress () {
		return dragInProgress;
	}
	
	@Override
	public boolean onDragEvent (DragEvent event) {
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
			
			dragInProgress = false;
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
				
				dragInProgress = false;
				return true;
			}
		}
		
		// Figure out which child view the DragEvent is over and send them the coordinates
		//  of the event. Send all of the other children the info that they aren't selected.
		if (event.getAction() == DragEvent.ACTION_DRAG_LOCATION || event.getAction() == DragEvent.ACTION_DRAG_STARTED) {
			// TODO setting the selected folder during a drag goes somewhere around here.
			for (int i = 0; i < mLayout.getChildCount(); i++) {
				FolderView toUpdate = (FolderView) mLayout.getChildAt(i);
				if (pointInView(toUpdate, getScrollX() + event.getX(), getScrollY() + event.getY())) {
					toUpdate.dragListener(event.getX() - toUpdate.getLeft(), event.getY() - toUpdate.getTop());
				} else {
					toUpdate.dragExitedListener();
				}
			}
			
			return true;
		}
		
		return true;
	}
	
	private boolean horizontalScrollDragListener (DragEvent event) {
		
		// Watch for left scroll
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
		
		pxPerFolder = folderWidth;
		foldersPerScreen = numFoldersToShow;
		
		super.onLayout(changed, left, top, right, bottom);
		
		for (int i = 0; i < mLayout.getChildCount(); i++) {
			mLayout.getChildAt(i).getLayoutParams().width = pxPerFolder;
		}
	}
	
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
	
	
	
	

	

	

	
	
	
	private List<File> getSelections () {
		
		Log.d("PEN", "getSelectionsCalled");
		
		ArrayList<File> selections = new ArrayList<File>();
		for (int i = 0; i < mLayout.getChildCount(); i++) {
			selections.addAll(((FolderView) mLayout.getChildAt(i)).getSelections());
		}
		return selections;
	}
	
	private void moveSelectionsToDirectory (File destination) {
		//List<File> selections = this.getSelections();
		
	}
	
	public void cancleSelections () {
		
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