package com.freehand.organizer;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.calhounroberthinshaw.freehand.R;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class FolderView extends ListView {
	private static final int BLUE_HIGHLIGHT = 0x600099CC;
	private static final int SOLID_BLUE_HIGHLIGHT = 0xFF0099CC;
	private static final int ORANGE_HIGHLIGHT = 0xFFFFBB33;
	private static final int NO_COLOR = 0x00FFFFFF;
	
	private static final float STATIONARY_RADIUS_SQUARED = 1500;
	private static final long DRAG_ACTION_TIMER = 400;
	private static final float DIRECTORY_UP_REGION_MULTIPLIER = 0.250f;
	private static final float SCROLL_REGION_MULTIPLIER = 0.10f;
	private static final int SCROLL_DISTANCE = 40;
	private static final int SCROLL_DURATION = 60;
	
	public final File folder;
	
	private final FolderBrowser mBrowser;
	private final FolderAdapter mAdapter;

	// These store the persistent information for dragWatcher
	private final float startDragDistSq = STATIONARY_RADIUS_SQUARED*getResources().getDisplayMetrics().density*getResources().getDisplayMetrics().density;
	private boolean watchingForDrag = false;
	private PointF dragWatcherStartPos = null;
	private boolean dragWatcherEventResolved = false;

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
			File clickedFile = ((FolderAdapter.RowDataHolder) clickedView.getTag()).file;
			Log.d("PEN", "clicked");
			mBrowser.openFile(clickedFile);
			mAdapter.notifyDataSetChanged();
		}
	};
	
	private OnItemLongClickListener DirectoryViewSelectListener = new OnItemLongClickListener() {
		public boolean onItemLongClick(AdapterView<?> parent, View clickedView, int position, long id) {
//			if (mBrowser.dragInProgress() == true) {
//				return true;
//			}
			
			final File clickedFile = ((FolderAdapter.RowDataHolder) clickedView.getTag()).file;
			watchingForDrag = mAdapter.toggleSelection(clickedFile);
			dragWatcherEventResolved = false;
			dragWatcherStartPos = null;	// I need to set set point inside of the drag watcher method because I don't get coords in here
			return true;
		}
	};

	
	public FolderView(Context context, FolderBrowser browser, File root) {
		super(context);
		mBrowser = browser;
		folder = root;

		// Create and set the adapter for this ListView
		mAdapter = new FolderAdapter(this.getContext(), R.layout.directoryview_row, root);
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

	//***************************************** Drag Start Detection Methods *************************************************
	
	/**
	 * @return true if the point is over a selected item and this FolderView needs to watch for a drag.
	 */
	public boolean checkStartWatchingForDrag (float x, float y) {
		dragWatcherStartPos = null;
		dragWatcherEventResolved = false;
		watchingForDrag = getPointOverSelectedItem(x, y);
		return watchingForDrag;
	}
	
	@Override
	public boolean onTouchEvent (MotionEvent event) {
		if (watchingForDrag == true) {
			
			if (dragWatcherStartPos == null) {
				dragWatcherStartPos = new PointF(event.getX(), event.getY());
			}
			
			if (dragWatcherEventResolved == false && pointOutOfCircleTest(event.getX(), event.getY(), dragWatcherStartPos, startDragDistSq)) {
				dragWatcherEventResolved = true;
				mBrowser.startDrag();
			}
			return true;
		} else {
			return super.onTouchEvent(event);
		}
	}

	public void onWatchingForDragTouchEvent (MotionEvent event) {
		if (dragWatcherEventResolved == true) { return; }
		
		if (dragWatcherStartPos == null) {
			dragWatcherStartPos = new PointF(event.getX(), event.getY());
		}
		
		// Drag start watcher
		if (pointOutOfCircleTest(event.getX(), event.getY(), dragWatcherStartPos, startDragDistSq)) {
			dragWatcherEventResolved = true;
			mBrowser.startDrag();
		}
		
		// Click watcher
		if (event.getAction() == MotionEvent.ACTION_UP) {
			final int touchedIndex = this.pointToPosition((int) event.getX(), (int) event.getY());
			final View touchedView = this.getViewAtPosition(touchedIndex);
			final File touchedFile = ((FolderAdapter.RowDataHolder) touchedView.getTag()).file;
			mBrowser.openFile(touchedFile);
			
			dragWatcherEventResolved = true;
		}
		
		// Long click watcher
		if (event.getEventTime()-event.getDownTime() > ViewConfiguration.getLongPressTimeout()) {
			final int touchedIndex = this.pointToPosition((int) event.getX(), (int) event.getY());
			final View touchedView = this.getViewAtPosition(touchedIndex);
			final File touchedFile = ((FolderAdapter.RowDataHolder) touchedView.getTag()).file;
			watchingForDrag = mAdapter.toggleSelection(touchedFile);
			mAdapter.notifyDataSetChanged();
			
			dragWatcherEventResolved = true;
		}
	}
	
	/**
	 * @return true if the point defined by (x, y) is outside of the circle define by center and radSq.
	 */
	private static boolean pointOutOfCircleTest (float x, float y, PointF center, float radSq) {
		final float draggedDistanceSquared = (center.x-x)*(center.x-x) + (center.y-y)*(center.y-y);
		return draggedDistanceSquared > radSq;
	}
	
	/**
	 * @return true if the point specified by x and y is over a selected file's view in the list
	 */
	private boolean getPointOverSelectedItem (float x, float y) {
		final int touchedIndex = this.pointToPosition((int) x, (int) y);
		if (touchedIndex == -1) { return false; }
		final View touchedView = this.getViewAtPosition(touchedIndex);
		final File fileUnderPointer = ((FolderAdapter.RowDataHolder) touchedView.getTag()).file;
		return mAdapter.getFileSelectionStatus(fileUnderPointer);
	}
	
	
	//*********************************************************** Handle drag events that are in progress **********************************************
	
	public void dragListener (float x, float y) {
		clearDragHighlightMarkers();
		
		// Wait for animations to finish before doing stuff
		if (this.getAnimation() != null && !this.getAnimation().hasEnded()) {
			actionTimeMarker = 0;
			return;
		}

		// Watch to see if the user wants to scroll up, reset the timer and return if we do
		if (y < this.getHeight() * SCROLL_REGION_MULTIPLIER && this.canScrollVertically(-1)) {
			this.smoothScrollBy(-SCROLL_DISTANCE, SCROLL_DURATION);
			actionTimeMarker = 0;
			setPoint = null;
			drawScrollUpHighlight = true;
			return;
		}
		
		// Watch to see if user wants to scroll down, reset the timer and return if we do
		if (y > this.getHeight() - this.getHeight() * SCROLL_REGION_MULTIPLIER && this.canScrollVertically(1)) {
			this.smoothScrollBy(SCROLL_DISTANCE, SCROLL_DURATION);
			actionTimeMarker = 0;
			setPoint = null;
			drawScrollDownHighlight = true;
			return;
		}
		
		// Find the file represented by the view the user's finger is over
		int positionUnderPointer = this.pointToPosition((int) x, (int) y);
		File fileUnderPointer = null;
		if (positionUnderPointer >= 0) {
			fileUnderPointer = mAdapter.getItem(positionUnderPointer);
		}
		
		// Don't want to be able to open selected folders
		if (fileUnderPointer != null && fileUnderPointer.isDirectory() && mAdapter.getFileSelectionStatus(fileUnderPointer) == false) {

			// Compute distance of user's finger from setPoint for later
			float draggedDistanceSquared = STATIONARY_RADIUS_SQUARED + 5;
			if (setPoint != null) {
				draggedDistanceSquared = (setPoint.x - x) * (setPoint.x - x) + (setPoint.y - y) * (setPoint.y - y);
			}

			// Start watching to see if user's finger has been hovering over a folder for long enough to open it
			if (actionTimeMarker == 0 || draggedDistanceSquared > STATIONARY_RADIUS_SQUARED) {
				actionTimeMarker = System.currentTimeMillis();
				setPoint = new PointF(x, y);
				folderOpenHighlight = positionUnderPointer;
			} else if (((System.currentTimeMillis() - actionTimeMarker) >= DRAG_ACTION_TIMER) && (draggedDistanceSquared <= STATIONARY_RADIUS_SQUARED)) {
				mBrowser.openFile(fileUnderPointer);
				mAdapter.notifyDataSetChanged();
			}

		// No actions are possible, reset timer
		} else {
			actionTimeMarker = 0;
			setPoint = null;
			dropHighlight = true;
		}
		
		this.invalidate();
	}
	
	public void dragExitedListener () {
		clearDragHighlightMarkers();
		
		setPoint = null;
		actionTimeMarker = 0;
		
		this.invalidate();
		mAdapter.notifyDataSetChanged();
	}

	private void clearDragHighlightMarkers() {
		drawScrollUpHighlight = false;
		drawScrollDownHighlight = false;
		folderOpenHighlight = -1;
		dropHighlight = false;
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

	
	
	
	public List<File> getSelections () {
		return new ArrayList<File>(mAdapter.selections);
	}
	
	public void removeAllSelections () {
		mAdapter.removeAllSelections();
	}
	
	public void notifyDataSetChanged () {
		Log.d("PEN", "notify");
		mAdapter.notifyDataSetChanged();
	}
	
	
	private class FolderAdapter extends ArrayAdapter<File> {
		private final Activity inflaterActivity;
		private final int mRowViewResourceId;
		
		private final Set<File> selections = new TreeSet<File>();
		
		public FolderAdapter (Context newContext, int newRowViewResourceId, File root) {
			super(newContext, newRowViewResourceId, new ArrayList<File>());
			this.clear();
			
			File[] files = root.listFiles(new FileFilter() {
				public boolean accept(File f) {
					return f.isDirectory() || f.getName().endsWith(".note");
				}
			});
			
			this.addAll(files);
			
			inflaterActivity = (Activity) newContext;
			mRowViewResourceId = newRowViewResourceId;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			RowDataHolder holder;

			// If convertView is new initialize it
			if (convertView == null) {
				LayoutInflater inflater = inflaterActivity.getLayoutInflater();
				convertView = inflater.inflate(mRowViewResourceId, parent, false);
				
				// Set holder to convertView's sub-views for easier modification and faster retrieval
				holder = new RowDataHolder();
				holder.thumbnail = (ImageView) convertView.findViewById(R.id.DirectoryViewRowThumbnail);
				holder.name = (TextView) convertView.findViewById(R.id.DirectoryViewRowName);
				holder.dateModified = (TextView) convertView.findViewById(R.id.DirectoryViewRowDate);
				convertView.setTag(holder);
			} else {
				holder = (RowDataHolder) convertView.getTag();
			}
			
			// Set the content of convertView's sub-views
			holder.file = this.getItem(position);
			holder.name.setText(holder.file.getName().replace(".note", ""));
			holder.dateModified.setText(new Date(holder.file.lastModified()).toString());
			
			if (holder.file.isDirectory()) {
				holder.thumbnail.setImageDrawable(inflaterActivity.getResources().getDrawable(R.drawable.folder));
			} else {
				holder.thumbnail.setImageDrawable(inflaterActivity.getResources().getDrawable(R.drawable.pencil));
			}
			
			// Change background color as appropriate
			if (selections.contains(holder.file) && mBrowser.dragInProgress()) {
				convertView.setBackgroundColor(Color.LTGRAY);
			} else if (selections.contains(holder.file)) {
				convertView.setBackgroundColor(BLUE_HIGHLIGHT);
			} else if (mBrowser.getDisplayStatus(holder.file)) {
				convertView.setBackgroundColor(ORANGE_HIGHLIGHT);
			} else {
				convertView.setBackgroundColor(0x0000000000);
			}

			return convertView;
		}

		// simple class to make getView a bit clearer (and much faster)
		private class RowDataHolder {
			File file;
			ImageView thumbnail;
			TextView name;
			TextView dateModified;
		}
		
		/**
		 * @return true if the file is now selected, false if it is no longer selected
		 */
		public boolean toggleSelection(File toToggle) {
			if (selections.remove(toToggle) == false) {
				selections.add(toToggle);
			}
			this.notifyDataSetChanged();
			mBrowser.selectionsChanged();
			return selections.contains(toToggle);
		}
		
		public void removeAllSelections () {
			selections.clear();
			this.notifyDataSetChanged();
			mBrowser.selectionsChanged();
		}
		
		public boolean getFileSelectionStatus (File toGet) {
			return selections.contains(toGet);
		}
	}
}