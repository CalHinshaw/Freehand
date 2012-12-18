package com.calhounhinshaw.freehandalpha.main_menu;

import java.io.File;
import java.util.LinkedList;
import com.calhounhinshaw.freehandalpha.R;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class DirectoryView extends ListView {
	private static final int BLUE_HIGHLIGHT = 0x600099CC;
	private static final int ORANGE_HIGHLIGHT = 0xFFFFBB33;
	private static final int NO_COLOR = 0x00FFFFFF;
	
	private static final float STATIONARY_RADIUS_SQUARED = 300;
	private static final long DRAG_ACTION_TIMER = 400;
	private static final float DIRECTORY_UP_REGION_MULTIPLIER = 0.250f;
	private static final float SCROLL_REGION_MULTIPLIER = 0.10f;
	private static final int SCROLL_DISTANCE = 40;
	private static final int SCROLL_DURATION = 60;
	
	// Items for various callbacks and core functionality
	private NoteExplorer mExplorer;
	private DirectoryViewAdapter mAdapter;
	private File mDirectory;

	// These get passed to DirectoryViewAdapters when they're created
	private Drawable folderDrawable;
	private Drawable defaultNoteDrawable;

	// These store the persistent information for dragWatcher
	private boolean watchForDrag = false;
	private View dragView = null;

	// These store the persistent information for all of the drag gestures
	private PointF setPoint = null;
	private long actionTimeMarker = 0;
	
	// These hold the highlight information for drag events
	private boolean drawDirectoryUpHighlight = false;
	private boolean drawScrollUpHighlight = false;
	private boolean drawScrollDownHighlight = false;
	private int folderOpenHighlight = -1;
	private int dropUnderHighlight = -1;	// the view the drop highlight draws UNDER


	public DirectoryView(Context context, File newDirectory,
		NoteExplorer newExplorer) {
		super(context);
		mExplorer = newExplorer;
		mDirectory = newDirectory;

		folderDrawable = this.getContext().getResources()
			.getDrawable(R.drawable.folder);
		defaultNoteDrawable = this.getContext().getResources()
			.getDrawable(R.drawable.pencil);

		File filesInDir[] = mDirectory.listFiles();
		LinkedList<File> validFilesInDir = new LinkedList<File>();

		// Remove files in the directory that are hidden or aren't .note files
		for (File f : filesInDir) {
			if (f.isDirectory() && !f.isHidden()) {
				validFilesInDir.add(f);
			} else if (f.isFile() && !f.isHidden()) {
				if (f.getName().contains(".note")) {
					validFilesInDir.add(f);
				}
			}
		}

		// Create the adapter for this list view using the cleaned list of
		// files, validFilesInDir
		mAdapter = new DirectoryViewAdapter(this.getContext(),
			R.layout.directoryview_row,
			validFilesInDir.toArray(new File[0]), folderDrawable,
			defaultNoteDrawable);

		this.setAdapter(mAdapter);
		this.setOnItemClickListener(DirectoryViewItemClickListener);
		this.setOnItemLongClickListener(DirectoryViewSelectListener);
	}

	// Open folder or note when clicked.
	private OnItemClickListener DirectoryViewItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View clickedView,
			int position, long id) {
			// know clickedView's tag is a file because of how it's created in DirectoryViewAdapter.getView
			File clickedFile = ((DirectoryViewAdapter.RowDataHolder) clickedView.getTag()).file;
			if (mAdapter.hasSelections())
				mAdapter.clearSelections();

			// Clicking on directory opens it
			if (clickedFile.isDirectory()) {
				mExplorer.addView(new DirectoryView(mExplorer.getContext(),
					clickedFile, mExplorer));
				mExplorer.showNext();
			}

			// TODO: implement click on file (opens the note)
		}
	};

	private OnItemLongClickListener DirectoryViewSelectListener = new OnItemLongClickListener() {
		public boolean onItemLongClick(AdapterView<?> parent, View pressedView,
			int position, long id) {
			mAdapter.addSelection(position);
			pressedView.setBackgroundColor(BLUE_HIGHLIGHT);

			watchForDrag = true;
			dragView = pressedView;

			return true;
		}
	};


	public File getDirectory() {
		return mDirectory;
	}


	public boolean adapterHasSelections() {
		return mAdapter.hasSelections();
	}


	public void clearAdapterSelections() {
		mAdapter.clearSelections();
	}


	// This method watches for the drag and drop gesture without interfering
	// with any of the class' other
	// behaviors.
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		super.onTouchEvent(event);
		dragWatcher(event);

		return true;
	}


	// If drag gesture call initDrag
	private void dragWatcher(MotionEvent event) {
		if (watchForDrag == true
			&& event.getAction() == MotionEvent.ACTION_MOVE) {
			if (setPoint == null) {
				setPoint = new PointF(event.getX(), event.getY());
			} else {
				float draggedDistanceSquared = (setPoint.x - event.getX())
					* (setPoint.x - event.getX())
					+ (setPoint.y - event.getY())
					* (setPoint.y - event.getY());
				if (draggedDistanceSquared > STATIONARY_RADIUS_SQUARED) {
					initDrag(event);

					setPoint = null;
					watchForDrag = false;
					dragView = null;
				}
			}
		} else if (watchForDrag == true
			&& (event.getAction() == MotionEvent.ACTION_UP
				|| event.getAction() == MotionEvent.ACTION_CANCEL || event
				.getAction() == MotionEvent.ACTION_DOWN)) {
			setPoint = null;
			watchForDrag = false;
			dragView = null;
		}
	}


	private void initDrag(MotionEvent event) {
		File[] files = mAdapter.getSelections();

		// Cancel drag event because no files selected
		if (files.length <= 0) {
			return;
		}

		String[] mime = new String[1];
		mime[0] = ClipDescription.MIMETYPE_TEXT_PLAIN;
		ClipDescription description = new ClipDescription("files", mime);

		// ClipData.Item required for constructor
		ClipData.Item constructorItem = new ClipData.Item(
			files[0].getAbsolutePath());

		ClipData data = new ClipData(description, constructorItem);

		for (int i = 1; i < files.length; i++) {
			data.addItem(new ClipData.Item(files[i].getAbsolutePath()));
		}

		mAdapter.greySelections();

		DirectoryViewDragShadowBuilder shadowBuilder = new DirectoryViewDragShadowBuilder(
			files.length, dragView.getWidth() / 3);
		this.startDrag(data, shadowBuilder, null, 0);
	}


	public boolean onDragEvent(DragEvent event) {
		final int action = event.getAction();

		switch (action) {
			case DragEvent.ACTION_DRAG_STARTED:
			case DragEvent.ACTION_DRAG_LOCATION:
				setDragAccents(event);
				dragNavigate(event);
				break;
				
			case DragEvent.ACTION_DRAG_ENDED:
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
	private void dragNavigate(DragEvent event) {
		
		// Find the file represented by the view the user's finger is over
		int positionUnderPointer = this.pointToPosition((int) event.getX(), (int) event.getY());
		File fileUnderPointer = null;
		if (positionUnderPointer >= 0) {
			fileUnderPointer = mAdapter.getItem(positionUnderPointer);
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
			} else if ((System.currentTimeMillis() - actionTimeMarker) >= DRAG_ACTION_TIMER && !mExplorer.isInRootDirectory()) {
				mExplorer.moveUpDirectory();
			}

		// Watch to see if the user wants to open a valid folder
		} else if (fileUnderPointer != null && fileUnderPointer.isDirectory() && !mAdapter.isSelected(positionUnderPointer)) {

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
				mExplorer.addView(new DirectoryView(mExplorer.getContext(), fileUnderPointer, mExplorer));
				mExplorer.showNext();
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
		File fileUnderPointer = null;
		if (positionUnderPointer >= 0) {
			fileUnderPointer = mAdapter.getItem(positionUnderPointer);
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
		if (fileUnderPointer != null && fileUnderPointer.isDirectory() && !mAdapter.isSelected(positionUnderPointer)) {
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
	
	
	
	
	
	
	
	
	
	
	
}