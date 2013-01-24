package com.calhounhinshaw.freehandalpha.main_menu;

import java.util.List;

import com.calhounroberthinshaw.freehand.R;
import com.calhounhinshaw.freehandalpha.main_menu.MainMenuPresenter.HierarchyWrapper;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class FolderView extends ListView {
	private static final int BLUE_HIGHLIGHT = 0x600099CC;
	private static final int SOLID_BLUE_HIGHLIGHT = 0xFF0099CC;
	private static final int ORANGE_HIGHLIGHT = 0xFFFFBB33;
	private static final int NO_COLOR = 0x00FFFFFF;
	
	private static final float STATIONARY_RADIUS_SQUARED = 300;
	private static final long DRAG_ACTION_TIMER = 400;
	private static final float DIRECTORY_UP_REGION_MULTIPLIER = 0.250f;
	private static final float SCROLL_REGION_MULTIPLIER = 0.10f;
	private static final int SCROLL_DISTANCE = 40;
	private static final int SCROLL_DURATION = 60;
	
	
	private FolderAdapter mAdapter;
	private MainMenuPresenter mPresenter;

	// These store the persistent information for dragWatcher
	private boolean watchForDrag = false;

	// These store the persistent information for all of the drag gestures
	private PointF setPoint = null;
	private long actionTimeMarker = 0;
	
	// These hold the highlight information for drag events
	private boolean drawScrollUpHighlight = false;
	private boolean drawScrollDownHighlight = false;
	private int folderOpenHighlight = -1;
	private boolean dropHighlight = false;
	
	// Variables responsible for highlighting this view when it's selected
	private boolean selectedState = false;
	private Paint mSelectedPaint;
	
	private Paint mDividerPaint;
	
	// Click listeners
	private OnItemClickListener DirectoryViewItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View clickedView, int position, long id) {
			
			// know clickedView's tag is a file because of how it's created in DirectoryViewAdapter.getView
			HierarchyWrapper clickedItem = ((FolderAdapter.RowDataHolder) clickedView.getTag()).viewItem;

			// Clicking on directory opens it
			if (clickedItem.isFolder) {
				FolderView newView = mPresenter.openFolder(clickedItem, FolderView.this);
				mPresenter.setSelectedFolderView(newView);
			} else {
				mPresenter.openNote(clickedItem);
			}
			
			mPresenter.clearSelections();
		}
	};
	
	private OnItemLongClickListener DirectoryViewSelectListener = new OnItemLongClickListener() {
		public boolean onItemLongClick(AdapterView<?> parent, View pressedView, int position, long id) {
			if (mAdapter.getItem(position).isSelected) {
				mPresenter.removeSelection(mAdapter.getItem(position));
			} else {
				mPresenter.addSelection(mAdapter.getItem(position));
				
				pressedView.setBackgroundColor(BLUE_HIGHLIGHT);

				watchForDrag = true;
			}
			

			return true;
		}
	};

	
	public FolderView(Context context, MainMenuPresenter newPresenter) {
		super(context);
		mPresenter = newPresenter;

		// Create and set the adapter for this ListView
		mAdapter = new FolderAdapter(this.getContext(), R.layout.directoryview_row);
		this.setAdapter(mAdapter);
		
		this.setOnItemClickListener(DirectoryViewItemClickListener);
		this.setOnItemLongClickListener(DirectoryViewSelectListener);
		

		
		mDividerPaint = new Paint();
		mDividerPaint.setAntiAlias(true);
		mDividerPaint.setColor(Color.DKGRAY);
		mDividerPaint.setStrokeWidth(4);
		
		mSelectedPaint = new Paint();
		mSelectedPaint.setAntiAlias(true);
		mSelectedPaint.setColor(SOLID_BLUE_HIGHLIGHT);
		mSelectedPaint.setStrokeWidth(6);
	}


	// This method watches for the drag and drop gesture without interfering with any of the class' other behaviors.
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mPresenter.setSelectedFolderView(this);
		
		super.onTouchEvent(event);
		selectedItemDragWatcher(event);
		dragWatcher(event);
		// Consume this touch event if a drag is started
		return true;
	}
	
	public boolean checkConsumeTouchEvent (MotionEvent event) {
		selectedItemDragWatcher(event);
		return watchForDrag;
	}
	
	private void selectedItemDragWatcher(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			int itemAtTouchPosition = this.pointToPosition((int) event.getX(), (int) event.getY());
			if (itemAtTouchPosition != -1 && mAdapter.getItem(itemAtTouchPosition).isSelected) {
				watchForDrag = true;
				Log.d("PEN", "watching for drag");
			}
		}
	}


	// If drag gesture call initDrag
	private void dragWatcher(MotionEvent event) {
		if (watchForDrag == true && (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_DOWN)) {
			if (setPoint == null) {
				setPoint = new PointF(event.getX(), event.getY());
			} else {
				float draggedDistanceSquared = (setPoint.x - event.getX())
					* (setPoint.x - event.getX())
					+ (setPoint.y - event.getY())
					* (setPoint.y - event.getY());
				if (draggedDistanceSquared > STATIONARY_RADIUS_SQUARED) {
					mPresenter.dragStarted(this);

					setPoint = null;
					watchForDrag = false;
				}
			}
		} else if (watchForDrag == true
			&& (event.getAction() == MotionEvent.ACTION_UP
				|| event.getAction() == MotionEvent.ACTION_CANCEL || event
				.getAction() == MotionEvent.ACTION_DOWN)) {
			setPoint = null;
			watchForDrag = false;
		}
	}


	public void startDrag(int numberOfItems) {
		// Cancel drag event because no items selected
		if (numberOfItems <= 0) {
			return;
		}
		
		mAdapter.greySelections();

		// Build the drag shadow needed for startDrag
		NoteMovementDragShadowBuilder shadowBuilder = new NoteMovementDragShadowBuilder(numberOfItems, this.getWidth() / 3);
		
		// Start drag without ClipData and with myLocalState equaling selectedItems (has to be cast when DragEvent is received).
		this.startDrag(null, shadowBuilder, null, 0);
	}
	
	public void dragListener (float x, float y) {
		setDragAccents(x, y);
		navigateDrag(x, y);
	}
	
	public void dragExitedListener () {
		clearDragHighlightMarkers();
		mAdapter.ungreySelections();
	}

	private void clearDragHighlightMarkers() {
		drawScrollUpHighlight = false;
		drawScrollDownHighlight = false;
		folderOpenHighlight = -1;
		dropHighlight = false;
		
		this.invalidate();
	}
	
	// Handle navigation through NoteExplorer during DragEvent.
	private void navigateDrag(float x, float y) {
		
		// Don't start timing until after the in animation has ended
		if (this.getAnimation() != null && !this.getAnimation().hasEnded()) {
			actionTimeMarker = 0;
			return;
		}
		
		// Find the file represented by the view the user's finger is over
		int positionUnderPointer = this.pointToPosition((int) x, (int) y);
		HierarchyWrapper itemUnderPointer = null;
		if (positionUnderPointer >= 0) {
			itemUnderPointer = mAdapter.getItem(positionUnderPointer);
		}

		// Watch to see if the user wants to scroll up
		if (y < this.getHeight() * SCROLL_REGION_MULTIPLIER) {
			if (this.canScrollVertically(-1)) {
				this.smoothScrollBy(-SCROLL_DISTANCE, SCROLL_DURATION);
			}
			
			actionTimeMarker = 0;
			setPoint = null;
			
		// Watch to see if user wants to scroll down
		} else if (y > this.getHeight() - this.getHeight() * SCROLL_REGION_MULTIPLIER) {
			if (this.canScrollVertically(1)) {
				this.smoothScrollBy(SCROLL_DISTANCE, SCROLL_DURATION);
			}
			
			actionTimeMarker = 0;
			setPoint = null;
			
		// Watch to see if the user wants to open a valid folder
		} else if (itemUnderPointer != null && itemUnderPointer.isFolder && !itemUnderPointer.isSelected) {

			// Compute distance of user's finger from setPoint for later
			float draggedDistanceSquared = STATIONARY_RADIUS_SQUARED + 5;
			if (setPoint != null) {
				draggedDistanceSquared = (setPoint.x - x) * (setPoint.x - x) + (setPoint.y - y) * (setPoint.y - y);
			}

			// Start watching to see if user's finger has been hovering over a folder for long enough to open it
			if (actionTimeMarker == 0 || draggedDistanceSquared > STATIONARY_RADIUS_SQUARED) {
				actionTimeMarker = System.currentTimeMillis();
				setPoint = new PointF(x, y);

			// If user has been hovering over folder for long enough open it
			} else if (((System.currentTimeMillis() - actionTimeMarker) >= DRAG_ACTION_TIMER) && (draggedDistanceSquared <= STATIONARY_RADIUS_SQUARED)) {
				mPresenter.openFolder(itemUnderPointer, this);
			}

		// No actions are possible, reset timer
		} else {
			actionTimeMarker = 0;
			setPoint = null;
		}
	}

	
	private void setDragAccents (float x, float y) {
		
		// Find the file represented by the view the user's finger is over
		int positionUnderPointer = this.pointToPosition((int) x, (int) y);
		HierarchyWrapper itemUnderPointer = null;
		if (positionUnderPointer >= 0) {
			itemUnderPointer = mAdapter.getItem(positionUnderPointer);
		}
		
		dropHighlight = true;
		
		// Trigger scroll highlight on top of screen
		if (y < this.getHeight() * SCROLL_REGION_MULTIPLIER) {
			drawScrollUpHighlight = true;
		} else {
			drawScrollUpHighlight = false;
		}
		
		// Trigger scroll highlight on bottom of screen
		if (y > this.getHeight() - this.getHeight() * SCROLL_REGION_MULTIPLIER) {
			drawScrollDownHighlight = true;
		} else {
			drawScrollDownHighlight = false;
		}
		
		// Trigger directory open highlight on right of the directory's row
		if (itemUnderPointer != null && itemUnderPointer.isFolder && !itemUnderPointer.isSelected) {
			folderOpenHighlight = positionUnderPointer;
		} else {
			folderOpenHighlight = -1;
		}
		
		this.invalidate();
	}
	

	
	// Returns null if view at wantedPosition isn't on screen or wanted position doesn't exist
	private View getViewAtPosition (int wantedPosition) {
		int firstPosition = this.getFirstVisiblePosition() - this.getHeaderViewsCount();
		int wantedChild = wantedPosition - firstPosition;
		
		if (wantedChild >= 0 && wantedChild < this.getChildCount()) {
			return this.getChildAt(wantedChild);
		} else {
			return null;
		}
	}
	
	@Override
	protected void dispatchDraw (Canvas canvas) {
		super.dispatchDraw(canvas);
		canvas.drawLine(this.getWidth(), 0, this.getWidth(), this.getHeight(), mDividerPaint);
		drawHighlights(canvas);
		
		if (selectedState == true) {
			canvas.drawLine(this.getWidth()-3, 0, this.getWidth()-3, this.getHeight(), mSelectedPaint);
			canvas.drawLine(3, 0, 3, this.getHeight(), mSelectedPaint);
			canvas.drawLine(0, 3, this.getWidth(), 3, mSelectedPaint);
			canvas.drawLine(0, this.getHeight()-3, this.getWidth(), this.getHeight()-3, mSelectedPaint);
		}
	}
	
	private void drawHighlights (Canvas canvas) {
		Rect highlightRect;
		Shader highlightShader;
		Paint highlightPaint = new Paint();
		
		if (drawScrollUpHighlight) {
			highlightRect = new Rect(0, 0, this.getWidth(), (int)(this.getHeight()*SCROLL_REGION_MULTIPLIER));
			highlightShader = new LinearGradient(0, 0, 0, this.getHeight()*SCROLL_REGION_MULTIPLIER, ORANGE_HIGHLIGHT, NO_COLOR, Shader.TileMode.CLAMP);
			highlightPaint.setShader(highlightShader);
			
			canvas.drawRect(highlightRect, highlightPaint);
		} else if (drawScrollDownHighlight) {
			highlightRect = new Rect(0, this.getHeight()-(int)(this.getHeight()*SCROLL_REGION_MULTIPLIER), this.getWidth(), this.getHeight());
			highlightShader = new LinearGradient(0, this.getHeight(), 0, this.getHeight() - this.getHeight()*SCROLL_REGION_MULTIPLIER, ORANGE_HIGHLIGHT, NO_COLOR, Shader.TileMode.CLAMP);
			highlightPaint.setShader(highlightShader);
			
			canvas.drawRect(highlightRect, highlightPaint);
		} else if (folderOpenHighlight >= 0) {
			View v = getViewAtPosition(folderOpenHighlight);	// The highlighted view
			highlightRect = new Rect((int)(v.getRight() - v.getWidth()*DIRECTORY_UP_REGION_MULTIPLIER), v.getTop(), v.getRight(), v.getBottom());
			highlightShader = new LinearGradient(v.getRight(), v.getTop(), (v.getRight() - v.getWidth()*DIRECTORY_UP_REGION_MULTIPLIER), v.getTop(), ORANGE_HIGHLIGHT, NO_COLOR, Shader.TileMode.CLAMP);
			highlightPaint.setShader(highlightShader);
			
			canvas.drawRect(highlightRect, highlightPaint);
		} else if (dropHighlight == true) {
			highlightPaint.setColor(ORANGE_HIGHLIGHT);
			highlightPaint.setStrokeWidth(6);
			
			canvas.drawLine(this.getWidth()-3, 0, this.getWidth()-3, this.getHeight(), highlightPaint);
			canvas.drawLine(3, 0, 3, this.getHeight(), highlightPaint);
			canvas.drawLine(0, 3, this.getWidth(), 3, highlightPaint);
			canvas.drawLine(0, this.getHeight()-3, this.getWidth(), this.getHeight()-3, highlightPaint);
		}
	}
	
	
	public void updateContent (List<HierarchyWrapper> newContent) {
		mAdapter.updateContent(newContent);
	}
	
	public void setSelectedState (boolean newSelectedState) {
		selectedState = newSelectedState;
		this.invalidate();
	}
}