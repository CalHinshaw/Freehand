package com.freehand.organizer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import com.calhounroberthinshaw.freehand.R;
import com.freehand.editor.canvas.Note;
import com.freehand.editor.canvas.Note.PaperType;
import com.freehand.share.ShareDialog;
import com.freehand.tutorial.TutorialPrefs;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.Toast;

public class FolderBrowser extends HorizontalScrollView {
	private static final float DRAG_SCROLL_REGION_SCREEN_PERCENTAGE = 0.10f;
	private final int HIGHLIGHT;
	private static final int NO_COLOR = 0x00FFFFFF;
	private static final float MIN_FOLDER_WIDTH_DIP = 300;
	
	private MainMenuActivity mActivity;
	private final LinearLayout mLayout = new LinearLayout(this.getContext());
	private File root;
	
	private final Set<File> selections = new TreeSet<File>();
	private File selectedFolder;
	
	private int dividerWidth = 0;
	private int pxPerFolder = 0;
	private int foldersPerScreen = 0;
	
	private int indexToShow = -1;
	private int toScrollTo = -1;
	
	private Timer scrollTimer = new Timer(true);
	private TimerTask currentScroll;
	private boolean scrollInProgress;
	
	private boolean dragInProgress = false;
	private int mDragRegionWidth = 0;
	private int currentDragRegion = 0;

	private int dragStartWatcherView = -1;
	
	
	
	//****************************************** setup methods ****************************************************
	
	public FolderBrowser(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.setOverScrollMode(HorizontalScrollView.OVER_SCROLL_ALWAYS);
		this.setWillNotDraw(false);
		
		mDragRegionWidth = (int) (DRAG_SCROLL_REGION_SCREEN_PERCENTAGE * getResources().getDisplayMetrics().widthPixels * getResources().getDisplayMetrics().density);
		
		dividerWidth = (int) (2*getResources().getDisplayMetrics().density + 1);
		final PaintDrawable divider = new PaintDrawable(Color.DKGRAY);
		divider.setIntrinsicWidth(dividerWidth);
		mLayout.setDividerDrawable(divider);
		mLayout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE + LinearLayout.SHOW_DIVIDER_END);
		
		this.addView(mLayout);
		this.setBackgroundColor(Color.WHITE);
		HIGHLIGHT = getResources().getColor(R.color.solid_highlight);
	}
	

	
	@Override
	protected void onMeasure (final int width, final int height) {
		super.onMeasure(width, height);
		setFolderWidthFields(this.getMeasuredWidth());
		
		final int wSpec = MeasureSpec.makeMeasureSpec(pxPerFolder, MeasureSpec.EXACTLY);
		final int hSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY);
		for (int i = 0; i < mLayout.getChildCount(); i++) {
			mLayout.getChildAt(i).getLayoutParams().width = pxPerFolder;
			mLayout.getChildAt(i).measure(wSpec, hSpec);
		}
	}

	private void setFolderWidthFields (final int width) {
		final float scale = getResources().getDisplayMetrics().density;
		final float minFolderWidthPx = MIN_FOLDER_WIDTH_DIP * scale;
		final int numFoldersToShow = (int) (width/minFolderWidthPx);
		final int folderWidth = (int) (width/numFoldersToShow) - dividerWidth;
		
		pxPerFolder = folderWidth;
		foldersPerScreen = numFoldersToShow;
	}
	
	public void setRootDirectory (File root) {
		this.root = root;
		mLayout.removeAllViews();
		openFolder(root);
	}
	
	public void setMainMenuActivity (MainMenuActivity activity) {
		mActivity = activity;
	}
	
	public void runOnUiThread (Runnable toRun) {
		mActivity.runOnUiThread(toRun);
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
		
		// Update selectedFolder
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			FolderView under = getViewUnderPoint(event.getX()+getScrollX(), event.getY());
			if (under != null) {
				selectedFolder = under.folder;
				invalidateChildren();
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
			Log.d("PEN", "***********Error************************************************************");
		}
		
		return returnValue;
	}
	
	public void setDragWatcherView (FolderView v) {
		dragStartWatcherView = mLayout.indexOfChild(v);
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
			stopScroll();
			for (int i = 0; i < mLayout.getChildCount(); i++) {
				FolderView toTest = (FolderView) mLayout.getChildAt(i);
				if (pointInView(toTest, getScrollX() + event.getX(), getScrollY() + event.getY())) {
					this.moveSelectionsToDirectory(toTest.getDropTarget());
				}
				toTest.dragExitedListener();
			}
			return true;
		}
		
		// If the drag event leaves the FolderBrowser or ends without a drop make sure all of the highlights are cleared
		if (event.getAction() == DragEvent.ACTION_DRAG_EXITED || event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
			stopScroll();
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
		int newDragRegion = 0;
		if (event.getX() <= mDragRegionWidth) {
			newDragRegion = -1;
		} else if (event.getX() >= (this.getWidth() - mDragRegionWidth)) {
			newDragRegion = 1;
		}
			
		if (newDragRegion == -1 && currentDragRegion != -1) {
			currentDragRegion = -1;
			startScroll(-1);
			this.invalidate();
		} else if (newDragRegion == 1 && currentDragRegion != 1) {
			currentDragRegion = 1;
			startScroll(1);
			this.invalidate();
		} else if (newDragRegion == 0 && currentDragRegion != 0) {
			currentDragRegion = 0;
			stopScroll();
			this.invalidate();
		}
		
		return scrollInProgress;
	}
	
	@Override
	protected void onDraw (Canvas canvas) {
		super.onDraw(canvas);
		
		if (dragInProgress && scrollInProgress && currentDragRegion == -1) {
			Rect highlightRect = new Rect(this.getScrollX(), 0, this.getScrollX() + mDragRegionWidth, this.getHeight());
			Shader highlightShader = new LinearGradient(this.getScrollX(), 0, this.getScrollX() + mDragRegionWidth, 0, HIGHLIGHT, NO_COLOR, Shader.TileMode.CLAMP);
			Paint highlightPaint = new Paint();
			highlightPaint.setShader(highlightShader);
			
			canvas.drawRect(highlightRect, highlightPaint);
		} else if (dragInProgress && scrollInProgress && currentDragRegion == 1) {
			Rect highlightRect = new Rect(this.getScrollX() + this.getWidth() - mDragRegionWidth, 0, this.getScrollX() + this.getWidth(), this.getHeight());
			Shader highlightShader = new LinearGradient(this.getScrollX() + this.getWidth(), 0, this.getScrollX() + this.getWidth() - mDragRegionWidth, 0, HIGHLIGHT, NO_COLOR, Shader.TileMode.CLAMP);
			Paint highlightPaint = new Paint();
			highlightPaint.setShader(highlightShader);
			
			canvas.drawRect(highlightRect, highlightPaint);
		}
	}
	
	public boolean dragInProgress () {
		return dragInProgress;
	}
	
	//**************************************** selection management methods *************************************
	
	/**
	 * @return true if the file is now selected, false if it is no longer selected
	 */
	public boolean toggleSelection(File toToggle) {
		final int initSels = selections.size();
		if (selections.remove(toToggle) == false) {
			selections.add(toToggle);
		}
		
		if (initSels == 0 && selections.size() == 1 && userHasFolder()) {
			triggerTutorial();
		}
		
		selectionsChanged();
		return selections.contains(toToggle);
	}
	
	public void cancelSelections () {
		selections.clear();
		notifyChildrenOfDatasetChange();
		invalidateChildren();
		selectionsChanged();
	}
	
	public boolean getFileSelectionStatus (File toGet) {
		return selections.contains(toGet);
	}
	
	private void selectionsChanged () {
		mActivity.setActionBar(selections.size());
	}
	
	public int getNumSelections () {
		return selections.size();
	}
	
	
	
	//*************************************** organization mutation methods****************************************
	
	private void moveSelectionsToDirectory (File destination) {
		closeSelectedFolders();
		boolean showToast = false;
		for (File f : selections) {
			if (destination.getAbsolutePath().contains(f.getAbsolutePath())) {
				showToast = true;
			} else {
				f.renameTo(new File(destination, f.getName()));
			}
		}
		
		if (showToast == true) {
			Toast.makeText(getContext(), "Can't move a file into itself", Toast.LENGTH_SHORT).show();
		}
		
		notifyChildrenOfFolderMutation();
		cancelSelections();
		setTutorialToOff();
	}
	
	public void deleteSelections () {
		final DialogInterface.OnClickListener onConfirmDelete = new DialogInterface.OnClickListener () {
			public void onClick(DialogInterface dialog, int which) {
				closeSelectedFolders();
				
				int numFailures = 0;
				for (File f : selections) {
					if (recursivelyDelete(f) == false) {
						numFailures++;
					}
				}
				
				if (numFailures > 0) {
					Toast.makeText(getContext(), "Failed to delete " + Integer.toString(numFailures)+ " files, please try again.", Toast.LENGTH_SHORT).show();
				}
				cancelSelections();
				notifyChildrenOfFolderMutation();
			}
		};
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), AlertDialog.THEME_HOLO_LIGHT);
		builder.setTitle("Are you sure you want to delete the selected files?")
			.setNegativeButton("Cancel", null)
			.setPositiveButton("Delete", onConfirmDelete)
			.create()
			.show();
	}
	
	public void shareSelections () {
		final Set<String> toShare = new TreeSet<String>();
		for (File f : selections) {
			this.getNonDirectoryFilePaths(f, toShare);
		}
		
		if (toShare.size() == 0) {
			Toast.makeText(this.getContext(), "No notes selected to share. Please select some notes and try again.", Toast.LENGTH_LONG).show();
			return;
		}
		
		new ShareDialog(getContext(), new ArrayList<Object>(toShare)).show(mActivity.getFragmentManager(), "share");
	}
	
	public void createNewFolder (final String name) {
		File newFolder = new File(selectedFolder, name);
		if (newFolder.mkdirs()) {
			this.getViewDisplayingFile(selectedFolder).notifyFolderMutated();
			openFile(newFolder);
		} else {
			Toast.makeText(getContext(), "Failed to create new folder, please try again.", Toast.LENGTH_SHORT).show();
		}
	}
	
	public void createNewNote (final String name, final PaperType paperType) {
		final File newNote = new File(selectedFolder, name);
		
		if (newNote.getParentFile().isDirectory() == false) {
			if (newNote.getParentFile().mkdirs() == false) {
				Toast.makeText(getContext(), "Unable to create folder, please make sure your sd card is mounted and try again.", Toast.LENGTH_SHORT).show();
				return;
			}
		}
		
		if (Note.createEmptyNote(newNote, paperType) == false) {
			Toast.makeText(getContext(), "Failed to create new note, please try again.", Toast.LENGTH_SHORT).show();
			return;
		}
		
		this.getViewDisplayingFile(selectedFolder).notifyFolderMutated();
		openFile(newNote);
	}
	
	public void renameSelection () {
		final File selection = selections.iterator().next();
		String defaultInput = selection.getName().replace(".note", "");
		final TextInputDialog.onConfirmFn onFinish = new TextInputDialog.onConfirmFn () {
			@Override
			public void function(String s) {
				closeSelectedFolders();
				cancelSelections();
				selection.renameTo(new File(selection.getParentFile(), s + (selection.isDirectory()? "" : ".note")));
				getViewDisplayingFile(selection.getParentFile()).notifyFolderMutated();
			}
		};
		
		DialogFragment d = new TextInputDialog("Rename Selected Item", "Enter the new name of the file.", defaultInput, "Rename", "Cancel", onFinish);
		d.show(mActivity.getFragmentManager(), "Rename");
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
		Intent i = new Intent(this.getContext(), com.freehand.editor.NoteActivity.class);
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
		selectedFolder = folder;
		invalidateChildren();
	}
	
	
	// ****************************************** non-drag scrolling methods ****************************************
	
	@Override
	public void computeScroll () {
		super.computeScroll();
		
		if (toScrollTo != -1) {
			this.setScrollX(toScrollTo);
			toScrollTo = -1;
		}
		
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
	
	
	//*********************************************** misc methods ******************************************************
	
	public File getSelectedFolder () {
		return this.selectedFolder;
	}
	
	public int getNumNotes () {
		final Set<String> paths = new TreeSet<String>();
		getNonDirectoryFilePaths(this.root, paths);
		
		int count = 0;
		for (String path : paths) {
			if (path.endsWith(".note")) {
				count++;
			}
		}
		
		return count;
	}
	
	private FolderView getViewUnderPoint (final float x, final float y) {
		for(int i = 0; i < mLayout.getChildCount(); i++) {
			FolderView current = (FolderView) mLayout.getChildAt(i);
			if (x >= current.getLeft() && x <= current.getRight() && y >= current.getTop() && y <= current.getBottom()) {
				return current;
			}
		}
		return null;
	}
	
	private boolean pointInView (View v, float x, float y) {
		if (x >= v.getLeft() && x <= v.getRight() && y >= v.getTop() && y <= v.getBottom()) {
			return true;
		} else {
			return false;
		}
	}
	
	private FolderView getViewDisplayingFile (File toGet) {
		for(int i = 0; i < mLayout.getChildCount(); i++) {
			FolderView current = (FolderView) mLayout.getChildAt(i);
			if (current.folder.equals(toGet)) {
				return current;
			}
		}
		return null;
	}
	
	private void invalidateChildren () {
		for(int i = 0; i < mLayout.getChildCount(); i++) {
			final FolderView current = ((FolderView) mLayout.getChildAt(i));
			current.invalidate();
		}
	}
	
	private void notifyChildrenOfDatasetChange () {
		for(int i = 0; i < mLayout.getChildCount(); i++) {
			final FolderView current = ((FolderView) mLayout.getChildAt(i));
			current.notifyDataSetChanged();
		}
	}
	
	private void notifyChildrenOfFolderMutation () {
		for(int i = 0; i < mLayout.getChildCount(); i++) {
			final FolderView current = ((FolderView) mLayout.getChildAt(i));
			current.notifyFolderMutated();
		}
	}
	
	private boolean recursivelyDelete (File toDelete) {
		if (toDelete.isDirectory()) {
			final File[] files = toDelete.listFiles();
			if (files != null) {
				for (File f : files) {
					if (recursivelyDelete(f) == false) {
						return false;
					}
				}
			}
		}
		return toDelete.delete();
	}
	
	private void closeSelectedFolders() {
		toScrollTo = this.getScrollX();
		for (File f : selections) {
			int i = 0;
			for(; i < mLayout.getChildCount(); i++) {
				final FolderView current = ((FolderView) mLayout.getChildAt(i));
				if (f.equals(current.folder)) {
					while (i < mLayout.getChildCount()) {
						mLayout.removeViewAt(i);
					}
				}
			}
		}
	}
	
	private void getNonDirectoryFilePaths (File toGetFrom, Collection<String> toAddTo) {
		if (toGetFrom.isFile()) {
			toAddTo.add(toGetFrom.getAbsolutePath());
		} else {
			final File[] files = toGetFrom.listFiles();
			if (files != null) {
				for (File f : files) {
					getNonDirectoryFilePaths(f, toAddTo);
				}
			}
		}
	}
	
	private boolean userHasFolder () {
		final File[] files = root.listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory()) return true;
			}
		}
		return false;
	}
	
	
	
	//************************************** Persistent scrolling **********************************
	
	private void startScroll (final int direction) {
		stopScroll();
		if (this.canScrollHorizontally(direction) == false) { return; }
		
		final Runnable scrollRunnable = new Runnable () {
			public void run() {
				if (canScrollHorizontally(direction) == false) {
					stopScroll();
				}
				scrollBy(direction*2, 0);
			}
		};
		
		currentScroll = new TimerTask() {
			@Override
			public void run() {
				mActivity.runOnUiThread(scrollRunnable);
			}
		};
		
		scrollTimer.scheduleAtFixedRate(currentScroll, 0, 3);
		scrollInProgress = true;
	}
	
	private void stopScroll () {
		if (currentScroll != null) {
			currentScroll.cancel();
			scrollInProgress = false;
			invalidate();
		}
	}
	
	
	// *************************************** Tutorial Methods ************************************
	
	private void triggerTutorial() {
		final SharedPreferences prefs = TutorialPrefs.getPrefs();
		if (prefs == null) return;
		boolean used = prefs.getBoolean("drag_to_move_used", false);
		if (used == false) {
			TutorialPrefs.toast("Drag the selected items to move them to another folder");
		}
	}
	
	private void setTutorialToOff() {
		final SharedPreferences prefs = TutorialPrefs.getPrefs();
		if (prefs == null) return;
		if (prefs.getBoolean("drag_to_move_used", false) == true) return;
		TutorialPrefs.toast("You can also drag items onto Action Bar buttons");
		prefs.edit().putBoolean("drag_to_move_used", true).apply();
	}
}