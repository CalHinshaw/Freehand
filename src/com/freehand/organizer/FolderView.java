package com.freehand.organizer;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.calhounroberthinshaw.freehand.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Vibrator;
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
	private final int TRANS_HIGHLIGHT;
	private final int SOLID_HIGHLIGHT;
	private static final int NO_COLOR = 0x00FFFFFF;
	
	private static final float STATIONARY_RADIUS_SQUARED = 1500;
	private static final long DRAG_ACTION_TIMER = 800;
	private static final float DIRECTORY_UP_REGION_MULTIPLIER = 0.250f;
	private static final float SCROLL_REGION_MULTIPLIER = 0.10f;
	private static final int SCROLL_DISTANCE = 40;
	private static final int SCROLL_DURATION = 60;
	
	public final File folder;
	
	private final FolderBrowser mBrowser;
	private final FolderAdapter mAdapter;

	private final float stationaryDistSq = STATIONARY_RADIUS_SQUARED*getResources().getDisplayMetrics().density*getResources().getDisplayMetrics().density;
	
	// These store the persistent information for dragWatcher
	private boolean watchingForDrag = false;
	private PointF dragWatcherStartPos = null;
	private boolean selectionAddedThisEvent = false;
	
	private final Timer mTimer = new Timer(true);
	private TimerTask currentLongClick;
	
	private Paint mSelectedPaint;
	private final int selectedRad;
	
	// Click listeners
	private OnItemClickListener DirectoryViewItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View clickedView, int position, long id) {
			File clickedFile = ((FolderAdapter.RowDataHolder) clickedView.getTag()).file;
			mBrowser.openFile(clickedFile);
			mAdapter.notifyDataSetChanged();
		}
	};
	
	private OnItemLongClickListener DirectoryViewSelectListener = new OnItemLongClickListener() {
		public boolean onItemLongClick(AdapterView<?> parent, View clickedView, int position, long id) {
			final File clickedFile = ((FolderAdapter.RowDataHolder) clickedView.getTag()).file;
			watchingForDrag = mBrowser.toggleSelection(clickedFile);
			mAdapter.notifyDataSetChanged();
			dragWatcherStartPos = null;	// I need to set set point inside of the drag watcher method because I don't get coords in here
			
			selectionAddedThisEvent = true;
			mBrowser.setDragWatcherView(FolderView.this);
			
			return true;
		}
	};

	
	public FolderView(Context context, FolderBrowser browser, File root) {
		super(context);
		mBrowser = browser;
		folder = root;
		
		TRANS_HIGHLIGHT = getResources().getColor(R.color.trans_highlight);
		SOLID_HIGHLIGHT = getResources().getColor(R.color.solid_highlight);

		// Create and set the adapter for this ListView
		mAdapter = new FolderAdapter(this.getContext(), R.layout.directoryview_row);
		this.setAdapter(mAdapter);
		
		this.setOnItemClickListener(DirectoryViewItemClickListener);
		this.setOnItemLongClickListener(DirectoryViewSelectListener);
		
		selectedRad = (int) (2.5*getResources().getDisplayMetrics().density);
		
		mSelectedPaint = new Paint();
		mSelectedPaint.setAntiAlias(true);
		mSelectedPaint.setStyle(Paint.Style.STROKE);
		mSelectedPaint.setColor(SOLID_HIGHLIGHT);
		mSelectedPaint.setStrokeWidth(2*selectedRad);
	}

	//***************************************** Drag Start Detection Methods *************************************************
	
	/**
	 * @return true if the point is over a selected item and this FolderView needs to watch for a drag.
	 */
	public boolean checkStartWatchingForDrag (float x, float y) {
		dragWatcherStartPos = null;
		watchingForDrag = getPointOverSelectedItem(x, y);
		return watchingForDrag;
	}
	
	@Override
	public boolean onTouchEvent (MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_UP) {
			selectionAddedThisEvent = false;
		}
		
		if (selectionAddedThisEvent == true) {
			onWatchingForDragTouchEvent(event);
			return true;
		} else {
			return super.onTouchEvent(event);
		}
	}

	public void onWatchingForDragTouchEvent (MotionEvent event) {
		if (dragWatcherStartPos == null) {
			dragWatcherStartPos = new PointF(event.getX(), event.getY());
		}
		
		// This only runs if, during the current touch event, a previously unselected file was selected.
		if (selectionAddedThisEvent == true) {
			if (pointOutOfCircleTest(event.getX(), event.getY(), dragWatcherStartPos, stationaryDistSq)) {
				mBrowser.startDrag();
			}
			return;
		}
		
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			final int touchedIndex = this.pointToPosition((int) event.getX(), (int) event.getY());
			final View touchedView = this.getViewAtPosition(touchedIndex);
			final File touchedFile = ((FolderAdapter.RowDataHolder) touchedView.getTag()).file;
			this.startLongClick(touchedFile);
		}
		
		// Drag start watcher
		if (pointOutOfCircleTest(event.getX(), event.getY(), dragWatcherStartPos, stationaryDistSq) && currentLongClick != null) {
			stopLongClick();
			mBrowser.startDrag();
		}
		
		if (event.getAction() == MotionEvent.ACTION_UP && currentLongClick != null) {
			stopLongClick();
			final int touchedIndex = this.pointToPosition((int) event.getX(), (int) event.getY());
			final View touchedView = this.getViewAtPosition(touchedIndex);
			final File touchedFile = ((FolderAdapter.RowDataHolder) touchedView.getTag()).file;
			mBrowser.openFile(touchedFile);
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
		return mBrowser.getFileSelectionStatus(fileUnderPointer);
	}
	
	
	//*********************************************************** Handle drag events that are in progress **********************************************
	
	
	private static final int STATE_RESTING = -4;
	private static final int STATE_FOLDER = -3;
	private static final int STATE_DOWN = -2;
	private static final int STATE_UP = -1;
	
	private int dragState = STATE_RESTING;
	private long timeOfLastStateTransition = System.currentTimeMillis();
	
	public void dragListener (float x, float y) {
		// Wait for animations to finish before doing stuff
		if (this.getAnimation() != null && !this.getAnimation().hasEnded()) {
			dragState = STATE_RESTING;
			timeOfLastStateTransition = System.currentTimeMillis();
			return;
		}
		
		final int indexUnderPointer = this.pointToPosition((int) x, (int) y);
		int newState = STATE_RESTING;
		
		if (y < this.getHeight() * SCROLL_REGION_MULTIPLIER && this.canScrollVertically(-1)) {								// Scroll up
			this.smoothScrollBy(-SCROLL_DISTANCE, SCROLL_DURATION);
			newState = STATE_UP;
		} else if (y > this.getHeight() - this.getHeight() * SCROLL_REGION_MULTIPLIER && this.canScrollVertically(1)) {		// Scroll down
			this.smoothScrollBy(SCROLL_DISTANCE, SCROLL_DURATION);
			newState = STATE_DOWN;
		} else if (indexUnderPointer == -1 || mAdapter.getItem(indexUnderPointer).isFile() ||								// Drop on this view
			mBrowser.getFileSelectionStatus(mAdapter.getItem(indexUnderPointer)) == true) {
			newState = STATE_FOLDER;
		} else {																											// Drop on folder under pointer
			newState = indexUnderPointer;
		}
		
		// Make state transition
		if (newState != dragState) {
			timeOfLastStateTransition = System.currentTimeMillis();
			dragState = newState;
			invalidate();
		}
		
		// Open folder if we've been hovering for DRAG_ACTION_TIMER milliseconds
		if (dragState >= 0 && System.currentTimeMillis()-timeOfLastStateTransition >= DRAG_ACTION_TIMER) {
			final File fileUnderDrag = mAdapter.getItem(dragState);
			mBrowser.openFile(fileUnderDrag);
			mAdapter.notifyDataSetChanged();
			invalidate();
		}
	}
	
	public File getDropTarget () {
		if (dragState < 0) {
			return folder;
		} else {
			return mAdapter.getItem(dragState);
		}
	}
	
	public void dragExitedListener () {
		dragState = STATE_RESTING;
		timeOfLastStateTransition = System.currentTimeMillis();
		mAdapter.notifyDataSetChanged();
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
		drawHighlights(canvas);
		
		if (mBrowser.getSelectedFolder().equals(folder) && mBrowser.dragInProgress() == false) {
			canvas.drawRect(new RectF(selectedRad, selectedRad, getWidth()-selectedRad, getHeight()-selectedRad), mSelectedPaint);
		}
	}
	
	private void drawHighlights (Canvas canvas) {
		Rect highlightRect;
		Shader highlightShader;
		Paint highlightPaint = new Paint();
		highlightPaint.setAntiAlias(true);
		
		if (dragState == STATE_UP) {
			highlightRect = new Rect(0, 0, this.getWidth(), (int)(this.getHeight()*SCROLL_REGION_MULTIPLIER));
			highlightShader = new LinearGradient(0, 0, 0, this.getHeight()*SCROLL_REGION_MULTIPLIER, SOLID_HIGHLIGHT, NO_COLOR, Shader.TileMode.CLAMP);
			highlightPaint.setShader(highlightShader);
			highlightPaint.setStyle(Paint.Style.FILL);
			
			canvas.drawRect(highlightRect, highlightPaint);
		} else if (dragState == STATE_DOWN) {
			highlightRect = new Rect(0, this.getHeight()-(int)(this.getHeight()*SCROLL_REGION_MULTIPLIER), this.getWidth(), this.getHeight());
			highlightShader = new LinearGradient(0, this.getHeight(), 0, this.getHeight() - this.getHeight()*SCROLL_REGION_MULTIPLIER, SOLID_HIGHLIGHT, NO_COLOR, Shader.TileMode.CLAMP);
			highlightPaint.setShader(highlightShader);
			highlightPaint.setStyle(Paint.Style.FILL);
			
			canvas.drawRect(highlightRect, highlightPaint);
		} else if (dragState == STATE_FOLDER) {
			highlightPaint.setColor(SOLID_HIGHLIGHT);
			highlightPaint.setStrokeWidth(6);
			highlightPaint.setStyle(Paint.Style.STROKE);
			highlightRect = new Rect(3, this.getScrollY()+3, getWidth()-3, this.getHeight()-3);
			canvas.drawRect(highlightRect, highlightPaint);
		} else if (dragState >= 0) {
			final File fileUnderDrag = mAdapter.getItem(dragState);
			final View v = getViewAtPosition(dragState);
			if (mBrowser.getDisplayStatus(fileUnderDrag) == false) {
				highlightRect = new Rect((int)(v.getRight() - v.getWidth()*DIRECTORY_UP_REGION_MULTIPLIER), v.getTop(), v.getRight(), v.getBottom());
				highlightShader = new LinearGradient(v.getRight(), v.getTop(), (v.getRight() - v.getWidth()*DIRECTORY_UP_REGION_MULTIPLIER), v.getTop(), SOLID_HIGHLIGHT, NO_COLOR, Shader.TileMode.CLAMP);
				highlightPaint.setShader(highlightShader);
				highlightPaint.setStyle(Paint.Style.FILL);
				canvas.drawRect(highlightRect, highlightPaint);
			}
			
			highlightPaint.setShader(null);
			highlightPaint.setColor(SOLID_HIGHLIGHT);
			highlightPaint.setStrokeWidth(4);
			highlightPaint.setStyle(Paint.Style.STROKE);
			highlightRect = new Rect(v.getLeft()+2, v.getTop()+2, v.getRight()-2, v.getBottom()-2);
			canvas.drawRect(highlightRect, highlightPaint);
		}
	}	

	
	//****************************************** LongClick timing code ************************************
	
	private void startLongClick (final File file) {
		stopLongClick();
		
		final Runnable longClickRunnable = new Runnable () {
			public void run() {
				((Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(30);
				watchingForDrag = mBrowser.toggleSelection(file);
				mAdapter.notifyDataSetChanged();
				stopLongClick();
			}
		};
		
		currentLongClick = new TimerTask () {
			@Override
			public void run() {
				mBrowser.runOnUiThread(longClickRunnable);
			}
		};
		
		mTimer.schedule(currentLongClick, ViewConfiguration.getLongPressTimeout());
	}
	
	private void stopLongClick () {
		if (currentLongClick != null) {
			currentLongClick.cancel();
			currentLongClick = null;
		}
	}

	
	//********************************************** change listeners ******************************************
	
	public void notifyDataSetChanged () {
		mAdapter.notifyDataSetChanged();
	}
	
	public void notifyFolderMutated () {
		mAdapter.notifyFolderMutated();
	}
	
	
	private class FolderAdapter extends ArrayAdapter<File> {
		private final Activity inflaterActivity;
		private final int mRowViewResourceId;
		
		private final Comparator<File> mComparator = new Comparator<File> () {
			public int compare(File lhs, File rhs) {
				if (lhs.isFile() && rhs.isDirectory()) {
					return 1;
				} else if (lhs.isDirectory() && rhs.isFile()) {
					return -1;
				}
				
				return lhs.compareTo(rhs);
			}
		};
		
		public FolderAdapter (Context newContext, int newRowViewResourceId) {
			super(newContext, newRowViewResourceId, new ArrayList<File>());
			this.notifyFolderMutated();
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
			
			// Change background color and indicators as appropriate
			if (mBrowser.getDisplayStatus(holder.file)) {
				convertView.findViewById(R.id.DirectoryViewRowBar).setVisibility(VISIBLE);
			} else {
				convertView.findViewById(R.id.DirectoryViewRowBar).setVisibility(INVISIBLE);
			}
			
			
			if (mBrowser.getFileSelectionStatus(holder.file) && mBrowser.dragInProgress()) {
				convertView.setBackgroundColor(Color.LTGRAY);
			} else if (mBrowser.getFileSelectionStatus(holder.file)) {
				convertView.setBackgroundColor(TRANS_HIGHLIGHT);
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
		
		public void notifyFolderMutated () {
			this.clear();
			File[] files = folder.listFiles(new FileFilter() {
				public boolean accept(File f) {
					return f.isDirectory() || f.getName().endsWith(".note");
				}
			});
			
			if (files == null) {
				Log.d("PEN", "Files == null in FolderAdapter");
				return;
			}
			
			Arrays.sort(files, mComparator);
			
			this.addAll(files);
			this.notifyDataSetChanged();
		}
	}
}