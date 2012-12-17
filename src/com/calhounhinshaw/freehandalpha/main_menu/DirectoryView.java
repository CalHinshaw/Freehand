package com.calhounhinshaw.freehandalpha.main_menu;

import java.io.File;
import java.util.LinkedList;
import com.calhounhinshaw.freehandalpha.R;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class DirectoryView extends ListView {
	private static final int BLUE_HIGHLIGHT = 0x600099CC;
	private static final float STATIONARY_RADIUS_SQUARED = 300;
	private static final long DRAG_ACTION_TIMER = 400;

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
			File clickedFile = (File) clickedView.getTag();
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
				dragNavigate(event);
				break;
		}

		return true;
	}


	// Handle navigation through NoteExplorer during DragEvent.
	// If the user stays on the left side of the screen
	private void dragNavigate(DragEvent event) {
		
		// Find the file represented by the view the user's finger is over
		int positionUnderPointer = this.pointToPosition((int) event.getX(), (int) event.getY());
		File fileUnderPointer = null;
		if (positionUnderPointer >= 0) {
			fileUnderPointer = mAdapter.getItem(positionUnderPointer);
		}

		// Watch to see if user wants to move up a directory
		if (event.getX() < this.getWidth() / 4) {

			// Start watching to see if user has been hovering on the left side of the screen for long enough to go up a directory
			if (actionTimeMarker == 0) {
				actionTimeMarker = System.currentTimeMillis();

			// If user's been on the left side for long enough go up a directory
			} else if ((System.currentTimeMillis() - actionTimeMarker) >= DRAG_ACTION_TIMER && !mExplorer.isInRootDirectory()) {
				mExplorer.moveUpDirectory();
			}

		// Watch to see if the user wants to open a folder
		} else if (fileUnderPointer != null && fileUnderPointer.isDirectory()) {

			// Compute distance of user's finger from setPoint for later
			float draggedDistanceSquared = STATIONARY_RADIUS_SQUARED + 5;
			if (setPoint != null) {
				draggedDistanceSquared = (setPoint.x - event.getX()) * (setPoint.x - event.getX()) + (setPoint.y - event.getY()) * (setPoint.y - event.getY());
			}

			// Start watching to see if user's finger has been hovering over a folder for long enough to open it
			if (actionTimeMarker == 0 || draggedDistanceSquared > STATIONARY_RADIUS_SQUARED) {
				Log.d("PEN", "reset timer");
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

}