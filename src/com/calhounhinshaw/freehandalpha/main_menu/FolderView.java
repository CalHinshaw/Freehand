package com.calhounhinshaw.freehandalpha.main_menu;

import java.util.LinkedList;
import java.util.List;

import com.calhounroberthinshaw.freehand.R;
import com.calhounhinshaw.freehandalpha.note_orginazion.INoteHierarchyItem;
import com.calhounhinshaw.freehandalpha.share.Sharer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.Log;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

public class FolderView extends ListView implements OnGestureListener {
	private static final int BLUE_HIGHLIGHT = 0x600099CC;
	private static final int ORANGE_HIGHLIGHT = 0xFFFFBB33;
	private static final int NO_COLOR = 0x00FFFFFF;
	
	private static final float STATIONARY_RADIUS_SQUARED = 300;
	private static final long DRAG_ACTION_TIMER = 400;
	private static final float DIRECTORY_UP_REGION_MULTIPLIER = 0.250f;
	private static final float SCROLL_REGION_MULTIPLIER = 0.10f;
	private static final int SCROLL_DISTANCE = 40;
	private static final int SCROLL_DURATION = 60;
	
	// Items that define this view
	private FolderAdapter mAdapter;
	
	private MainMenuPresenter mPresenter;

	// These store the persistent information for dragWatcher
	private boolean watchForDrag = false;

	// These store the persistent information for all of the drag gestures
	private PointF setPoint = null;
	private long actionTimeMarker = 0;
	
	// These hold the highlight information for drag events
	private boolean drawDirectoryUpHighlight = false;
	private boolean drawScrollUpHighlight = false;
	private boolean drawScrollDownHighlight = false;
	private int folderOpenHighlight = -1;
	private int dropUnderHighlight = -1;	// the view the drop highlight draws UNDER
	
	private final GestureDetector flingDetector;

	public FolderView(Context context, MainMenuPresenter newPresenter) {
		super(context);
		mPresenter = newPresenter;

		// Create and set the adapter for this ListView
		mAdapter = new FolderAdapter(this.getContext(), R.layout.directoryview_row);
		
		this.setAdapter(mAdapter);
		
		this.setOnItemClickListener(DirectoryViewItemClickListener);
		this.setOnItemLongClickListener(DirectoryViewSelectListener);
		
		// Set flingDetector
		flingDetector = new GestureDetector(this.getContext(), this, this.getHandler());
	}

	// Open folder or note when clicked.
	private OnItemClickListener DirectoryViewItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View clickedView, int position, long id) {
			
			// know clickedView's tag is a file because of how it's created in DirectoryViewAdapter.getView
			INoteHierarchyItem clickedItem = ((FolderAdapter.RowDataHolder) clickedView.getTag()).noteHierarchyItem;
			
			if (mAdapter.hasSelections()) {
				mAdapter.clearSelections();
			}

			// Clicking on directory opens it
			if (clickedItem.isFolder()) {
				mPresenter.openFolder(clickedItem, parent.getId());
				mPresenter.turnDefaultActionBarOn();
			} else {
				mPresenter.openNote(clickedItem);
			}
		}
	};

	private OnItemLongClickListener DirectoryViewSelectListener = new OnItemLongClickListener() {
		public boolean onItemLongClick(AdapterView<?> parent, View pressedView, int position, long id) {
			if (mAdapter.isSelected(position)) {
				mAdapter.removeSelection(position);
				if (!mAdapter.hasSelections()) {
					setFastScrollAlwaysVisible(false);
				}
			} else {
				mAdapter.addSelection(position);
				mPresenter.turnItemsSelectedActionBarOn();
				setFastScrollAlwaysVisible(true);
				
				pressedView.setBackgroundColor(BLUE_HIGHLIGHT);

				watchForDrag = true;
			}
			

			return true;
		}
	};

	public boolean adapterHasSelections() {
		return mAdapter.hasSelections();
	}


	public void clearAdapterSelections() {
		mAdapter.clearSelections();
		this.setFastScrollAlwaysVisible(false);
	}


	// This method watches for the drag and drop gesture without interfering with any of the class' other behaviors.
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		super.onTouchEvent(event);
		selectedItemDragWatcher(event);
		dragWatcher(event);
		flingDetector.onTouchEvent(event);

		return true;
	}
	
	private void selectedItemDragWatcher(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			int itemAtTouchPosition = this.pointToPosition((int) event.getX(), (int) event.getY());
			if (mAdapter.isSelected(itemAtTouchPosition)) {
				Log.d("PEN", Integer.toString(itemAtTouchPosition));
				
				watchForDrag = true;
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
					initDrag();

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


	private void initDrag() {
		List<INoteHierarchyItem> selectedItems = mAdapter.getSelections();

		// Cancel drag event because no items selected
		if (selectedItems.size() <= 0) {
			return;
		}
		
		mAdapter.greySelections();

		// Build the drag shadow needed for startDrag
		NoteMovementDragShadowBuilder shadowBuilder = new NoteMovementDragShadowBuilder(selectedItems.size(), this.getWidth() / 3);
		
		// Start drag without ClipData and with myLocalState equaling selectedItems (has to be cast when DragEvent is received).
		this.startDrag(null, shadowBuilder, selectedItems, 0);
	}


	@SuppressWarnings("unchecked")
	public boolean onDragEvent(DragEvent event) {
		switch (event.getAction()) {
			case DragEvent.ACTION_DRAG_STARTED:
			case DragEvent.ACTION_DRAG_LOCATION:
				setDragAccents(event);
				navigateDrag(event);
				break;
				
			case DragEvent.ACTION_DROP:
				List<INoteHierarchyItem> toMove = (List<INoteHierarchyItem>) event.getLocalState();
				mPresenter.move(toMove, this.getId());
				
				mPresenter.turnDefaultActionBarOn();
				clearDragHighlightMarkers();
				mAdapter.ungreySelections();
				mAdapter.clearSelections();
				break;
				
			case DragEvent.ACTION_DRAG_ENDED:
				mPresenter.turnDefaultActionBarOn();
				clearDragHighlightMarkers();
				mAdapter.ungreySelections();
				mAdapter.clearSelections();
			case DragEvent.ACTION_DRAG_EXITED:
				clearDragHighlightMarkers();
				break;
		}

		return true;
	}

	private void clearDragHighlightMarkers() {
		drawDirectoryUpHighlight = false;
		drawScrollUpHighlight = false;
		drawScrollDownHighlight = false;
		folderOpenHighlight = -1;
		dropUnderHighlight = -1;
		
		this.invalidate();
	}
	
	// Handle navigation through NoteExplorer during DragEvent.
	private void navigateDrag(DragEvent event) {
		
		// Don't start timing until after the in animation has ended
		if (this.getAnimation() != null && !this.getAnimation().hasEnded()) {
			actionTimeMarker = 0;
			return;
		}
		
		// Find the file represented by the view the user's finger is over
		int positionUnderPointer = this.pointToPosition((int) event.getX(), (int) event.getY());
		INoteHierarchyItem itemUnderPointer = null;
		if (positionUnderPointer >= 0) {
			itemUnderPointer = mAdapter.getItem(positionUnderPointer);
		}

		// Watch to see if the user wants to scroll up
		if (event.getY() < this.getHeight() * SCROLL_REGION_MULTIPLIER) {
			if (this.canScrollVertically(-1)) {
				this.smoothScrollBy(-SCROLL_DISTANCE, SCROLL_DURATION);
			}
			
			actionTimeMarker = 0;
			setPoint = null;
			
		// Watch to see if user wants to scroll down
		} else if (event.getY() > this.getHeight() - this.getHeight() * SCROLL_REGION_MULTIPLIER) {
			if (this.canScrollVertically(1)) {
				this.smoothScrollBy(SCROLL_DISTANCE, SCROLL_DURATION);
			}
			
			actionTimeMarker = 0;
			setPoint = null;
			
		// Watch to see if user wants to move up a directory
		} else if (event.getX() < this.getWidth() * DIRECTORY_UP_REGION_MULTIPLIER) {

			// Start keeping track of the amount of time the user has been hovering over the left side of the screen for
			if (actionTimeMarker == 0) {
				actionTimeMarker = System.currentTimeMillis();

			// If user's been on the left side for long enough go up a directory
			} else if ((System.currentTimeMillis() - actionTimeMarker) >= DRAG_ACTION_TIMER && !mPresenter.testInRootDirectory()) {
				mPresenter.closeCurrentFolder();
			}

		// Watch to see if the user wants to open a valid folder
		} else if (itemUnderPointer != null && itemUnderPointer.isFolder() && !mAdapter.isSelected(positionUnderPointer)) {

			// Compute distance of user's finger from setPoint for later
			float draggedDistanceSquared = STATIONARY_RADIUS_SQUARED + 5;
			if (setPoint != null) {
				draggedDistanceSquared = (setPoint.x - event.getX()) * (setPoint.x - event.getX()) + (setPoint.y - event.getY()) * (setPoint.y - event.getY());
			}

			// Start watching to see if user's finger has been hovering over a folder for long enough to open it
			if (actionTimeMarker == 0 || draggedDistanceSquared > STATIONARY_RADIUS_SQUARED) {
				actionTimeMarker = System.currentTimeMillis();
				setPoint = new PointF(event.getX(), event.getY());

			// If user has been hovering over folder for long enough open it
			} else if (((System.currentTimeMillis() - actionTimeMarker) >= DRAG_ACTION_TIMER) && (draggedDistanceSquared <= STATIONARY_RADIUS_SQUARED)) {
				mPresenter.openFolder(itemUnderPointer, this.getId());
			}

		// No actions are possible, reset timer
		} else {
			actionTimeMarker = 0;
			setPoint = null;
		}
	}

	
	private void setDragAccents (DragEvent event) {
		
		// Find the file represented by the view the user's finger is over
		int positionUnderPointer = this.pointToPosition((int) event.getX(), (int) event.getY());
		INoteHierarchyItem itemUnderPointer = null;
		if (positionUnderPointer >= 0) {
			itemUnderPointer = mAdapter.getItem(positionUnderPointer);
		}
		
		// Trigger highlight between list elements
		if (positionUnderPointer >= 0) {
			View v = getViewAtPosition(positionUnderPointer);
			float midpoint = v.getTop() + v.getHeight()/2;
			if (event.getY() > midpoint) {
				dropUnderHighlight = positionUnderPointer;
			} else if (positionUnderPointer > 0) {
				dropUnderHighlight = positionUnderPointer-1;
			} else {
				dropUnderHighlight = -1;
			}
		} else {
			dropUnderHighlight = -1;
		}
		
		// Trigger directory up highlight on left side of screen
		if (event.getX() < this.getWidth() * DIRECTORY_UP_REGION_MULTIPLIER) {
			drawDirectoryUpHighlight = true;
		} else {
			drawDirectoryUpHighlight = false;
		}
		
		
		// Trigger scroll highlight on top of screen
		if (event.getY() < this.getHeight() * SCROLL_REGION_MULTIPLIER) {
			drawScrollUpHighlight = true;
		} else {
			drawScrollUpHighlight = false;
		}
		
		// Trigger scroll highlight on bottom of screen
		if (event.getY() > this.getHeight() - this.getHeight() * SCROLL_REGION_MULTIPLIER) {
			drawScrollDownHighlight = true;
		} else {
			drawScrollDownHighlight = false;
		}
		
		// Trigger directory open highlight on right of the directory's row
		if (itemUnderPointer != null && itemUnderPointer.isFolder() && !mAdapter.isSelected(positionUnderPointer)) {
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
	
	// dispatchDraw lets me overlay highlights on the children of this view
	@Override
	protected void dispatchDraw (Canvas canvas) {
		super.dispatchDraw(canvas);
		drawHighlights(canvas);
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
		} else if (drawDirectoryUpHighlight) {
			highlightRect = new Rect(0, 0, (int)(this.getWidth()*DIRECTORY_UP_REGION_MULTIPLIER), this.getHeight());
			highlightShader = new LinearGradient(0, 0, this.getWidth()*DIRECTORY_UP_REGION_MULTIPLIER, 0, ORANGE_HIGHLIGHT, NO_COLOR, Shader.TileMode.CLAMP);
			highlightPaint.setShader(highlightShader);
			
			canvas.drawRect(highlightRect, highlightPaint);
		} else if (folderOpenHighlight >= 0) {
			View v = getViewAtPosition(folderOpenHighlight);	// The highlighted view
			highlightRect = new Rect((int)(v.getRight() - v.getWidth()*DIRECTORY_UP_REGION_MULTIPLIER), v.getTop(), v.getRight(), v.getBottom());
			highlightShader = new LinearGradient(v.getRight(), v.getTop(), (v.getRight() - v.getWidth()*DIRECTORY_UP_REGION_MULTIPLIER), v.getTop(), ORANGE_HIGHLIGHT, NO_COLOR, Shader.TileMode.CLAMP);
			highlightPaint.setShader(highlightShader);
			
			canvas.drawRect(highlightRect, highlightPaint);
		} else if (dropUnderHighlight >= 0) {
			int midline = getViewAtPosition(dropUnderHighlight).getBottom();
			int halfHeight = getViewAtPosition(dropUnderHighlight).getHeight()/4;
			
			highlightRect = new Rect(0, midline+halfHeight, this.getWidth(), midline-halfHeight);
			highlightShader = new LinearGradient(0, midline, 0, midline+halfHeight, ORANGE_HIGHLIGHT, NO_COLOR, Shader.TileMode.MIRROR);
			highlightPaint.setShader(highlightShader);
			
			canvas.drawRect(highlightRect, highlightPaint);
		}
	}
	
	
	
	public void deleteSelectedItems () {
		mPresenter.deleteWithConfirmation(mAdapter.getSelections());
	}

	public void newNote() {
		mPresenter.createNewNote(this.getId());
	}
	
	public void newFolder() {
		mPresenter.createNewFolder(this.getId());
	}
	
	
	public void shareSelected() {
		Log.d("PEN", "shareSelected in FolderView called");
		List<INoteHierarchyItem> selections = mAdapter.getSelections();
		LinkedList<INoteHierarchyItem> toShare = new LinkedList<INoteHierarchyItem>();
		
		for (INoteHierarchyItem i : selections) {
			if (i.isFolder()) {
				toShare.addAll(i.getAllRecursiveChildren());
			} else {
				toShare.add(i);
			}
		}
		
		if (toShare.size() == 1) {
			Sharer.shareNoteHierarchyItemsAsJPEG(toShare, this.getContext());
		} else {
			Toast.makeText(getContext(), "You can only share one note at a time right now. Sharing multiple notes coming soon!", Toast.LENGTH_LONG).show();
		}
		
	}
	
	
	
	public void updateContent (List<INoteHierarchyItem> newContent) {
		mAdapter.updateContent(newContent);
	}
	

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		if (Math.abs(e1.getY()-e2.getY()) <= this.getHeight()/12 && velocityX >= 3000) {
			if (e1.getX()-e2.getX() <= -this.getWidth()/4) {
				mPresenter.closeCurrentFolder();
				return true;
			}
		}
		
		return false;
	}

	// Unneeded OnGestureDetector methods
	public boolean onDown(MotionEvent arg0) { return false; }
	public void onLongPress(MotionEvent e) { }
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) { return false; }
	public void onShowPress(MotionEvent e) { }
	public boolean onSingleTapUp(MotionEvent e) { return false; }
}