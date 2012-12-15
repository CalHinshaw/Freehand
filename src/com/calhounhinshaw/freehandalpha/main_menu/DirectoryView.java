package com.calhounhinshaw.freehandalpha.main_menu;

import java.io.File;
import java.util.LinkedList;
import com.calhounhinshaw.freehandalpha.R;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class DirectoryView extends ListView {
	private static final int BLUE_HIGHLIGHT = 0x600099CC;

	private NoteExplorer mExplorer;
	private DirectoryViewAdapter mAdapter;
	private File mDirectory;

	// These get passed to DirectoryViewAdapters when they're created
	private Drawable folderDrawable;
	private Drawable defaultNoteDrawable;

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
			File clickedFile = (File) clickedView.getTag(); // know
															// clickedView's tag
															// is a file because
															// of how it's
															// created in
															// DirectoryViewAdapter.getView
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

	private PointF startPoint;
	private static final float DRAG_RADIUS_SQUARED = 200;
	private boolean watchForDrag = false;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		super.onTouchEvent(event);

		// start drag event if long press and drag
		if (watchForDrag == true
				&& event.getAction() == MotionEvent.ACTION_MOVE) {
			if (startPoint == null) {
				startPoint = new PointF(event.getX(), event.getY());
			} else {
				float draggedDistanceSquared = (startPoint.x - event.getX())*(startPoint.x - event.getX()) + (startPoint.y - event.getY())*(startPoint.y - event.getY());
				if (draggedDistanceSquared > DRAG_RADIUS_SQUARED) {
					// START DRAG EVENT

					Log.d("PEN", "START DRAG EVENT");
					
					startPoint = null;
					watchForDrag = false;
				}
			}
		} else if (watchForDrag == true
				&& (event.getAction() == MotionEvent.ACTION_UP
						|| event.getAction() == MotionEvent.ACTION_CANCEL || event
						.getAction() == MotionEvent.ACTION_DOWN)) {
			startPoint = null;
			watchForDrag = false;
		}

		return true;
	}
}