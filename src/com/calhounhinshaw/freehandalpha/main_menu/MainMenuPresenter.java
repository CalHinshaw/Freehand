package com.calhounhinshaw.freehandalpha.main_menu;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import android.app.DialogFragment;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

import com.calhounhinshaw.freehandalpha.note_orginazion.IChangeListener;
import com.calhounhinshaw.freehandalpha.note_orginazion.INoteHierarchyItem;
import com.calhounhinshaw.freehandalpha.share.Sharer;

/**
 * The presenter responsible for the MainMenu Activity.
 * 
 * @author Cal Hinshaw
 */
public class MainMenuPresenter {
	
	// The activity this presenter is responsible for. It is used to display Dialogs and Toasts.
	private final MainMenuActivity mActivity;
	
	// The NoteExplorer that holds the view stack. It is used to display FolderViews.
	private final FolderBrowser mBrowser;
	
	private ArrayList<FolderViewContainer> openFolderViews = new ArrayList<FolderViewContainer>(10);
	private TreeSet<INoteHierarchyItem> selectedItems = new TreeSet<INoteHierarchyItem>();
	private FolderViewContainer selectedContainer = null;
	
	
	public MainMenuPresenter (MainMenuActivity activity, FolderBrowser browser, INoteHierarchyItem root) {
		mActivity = activity;
		mBrowser = browser;
		
		FolderView newView = this.openFolder(root, null);
		this.setSelectedFolderView(newView);
	}
	
	
	//***************************************** Deletion Methods ****************************************
	
	/**
	 * Delete the INoteHierarchyItems passed in after presenting the user with a confirmation dialog.
	 * If something goes wrong during the file I/O the user will be informed by a toast.
	 * 
	 * @param toDelete will be deleted if the user confirms
	 */
	public void deleteWithConfirmation () {
		DialogFragment d = new ConfirmDeleteDialog("Confirm Delete?", "Delete", "Cancel", new ArrayList<INoteHierarchyItem>(selectedItems), this);
		mActivity.displayDialogFragment(d, "delete");
	}
	
	/**
	 * Deletes the passed INoteHierarchyItems. If something goes wrong displays a Toast telling the user via this presenter's MainMenuActivity.
	 * @param toDelete
	 */
	public void deleteWithoutConfirmation (List<INoteHierarchyItem> toDelete) {
		// If failureToast is true at the end of the method we will display a toast to the user.
		boolean failureToast = false;
		
		// Iterate through toDelete deleting the items. If the deletion fails set failureToast equal to true.
		for (INoteHierarchyItem i : toDelete) {
			if (i.delete() == false) {
				failureToast = true;
			}
		}
		
		if (failureToast == true) {
			mActivity.displayToast("Deletion failed. Please try again.");
		}
		
		this.clearSelections();
	}
	
	
	//************************************** New Folder Methods *********************************************
	
	public void createNewFolder () {
		
		if (selectedContainer == null) {
			mActivity.displayToast("Please select a folder and try again.");
			return;
		}
		
		INoteHierarchyItem toCreateIn = selectedContainer.hierarchyItem;
		
		if (toCreateIn == null) {
			mActivity.displayToast("Please select a folder and try again.");
			return;
		}

		// Create the function that will be run when the user presses the Create Folder button
		NewItemFunctor newFolderFunction = new NewItemFunctor() {
			@Override
			public void function(INoteHierarchyItem destinationItem, String folderName) {
				createNewFolder(destinationItem, folderName);
			}
		};
		
		// Find the default input string - unnamed folder + the smallest unused natural number
		int i = 1;
		while (toCreateIn.containsItemName("unnamed folder " + Integer.toString(i))) {
			i++;
		}
		String defaultInput = "unnamed folder " + Integer.toString(i);
		
		// Create the dialog and pass it to activity to be run
		DialogFragment d = new NewItemDialog("Create New Folder", "Enter the name of the folder.", defaultInput, "Create Folder", "Cancel", toCreateIn, newFolderFunction);
		mActivity.displayDialogFragment(d, "New Folder");
	}
	
	public void createNewFolder (INoteHierarchyItem dest, String name) {
		INoteHierarchyItem newFolder = dest.addFolder(name);
		
		if (newFolder != null) {
			FolderView newView = this.openFolder(newFolder, selectedContainer.folderView);
			this.setSelectedFolderView(newView);
		} else {
			mActivity.displayToast("Create new folder failed. Please try again.");
		}
	}
	
	//************************************** New Note Methods (who wants first class functions anyway?) ************************
	
	public void createNewNote () {
		if (selectedContainer == null) {
			mActivity.displayToast("Please select a folder and try again.");
			return;
		}
		
		INoteHierarchyItem toCreateIn = selectedContainer.hierarchyItem;
		 if (toCreateIn == null) {
			 mActivity.displayToast("Please select a folder and try again.");
			 return;
		 }
		 
		// Create the function that will be run when the user presses the Create Note button
		NewItemFunctor newNoteFunction = new NewItemFunctor() {
			@Override
			public void function(INoteHierarchyItem destinationItem, String folderName) {
				createNewNote(destinationItem, folderName);
			}
		};
		
		// Find the default input string - unnamed note + the smallest unused natural number
		int i = 1;
		while (toCreateIn.containsItemName("unnamed note " + Integer.toString(i))) {
			i++;
		}
		String defaultInput = "unnamed note " + Integer.toString(i);
		
		// Create the dialog and pass it to activity to be run
		DialogFragment d = new NewItemDialog("Create New Note", "Enter the name of the note.", defaultInput, "Create Note", "Cancel", toCreateIn, newNoteFunction);
		mActivity.displayDialogFragment(d, "New Note");
	}
	
	public void createNewNote (INoteHierarchyItem dest, String name) {
		INoteHierarchyItem newNote = dest.addNote(name);
		
		if (newNote != null) {
			openNote(newNote);
		} else {
			mActivity.displayToast("Create new note failed. Please try again.");
		}
	}
	
	
	//***************************************** Move Methods ***********************************************************
	
	public void moveTo (FolderView moveTarget) {
		INoteHierarchyItem moveDest = this.getContainerFromView(moveTarget).hierarchyItem;
		
		if (moveDest == null) {
			 Log.d("PEN", "moveTo called, moveDest null");
			 return;
		}

		boolean moveFailed = false;
		
		for (INoteHierarchyItem i : selectedItems) {
			if (!i.moveTo(moveDest)){
				moveFailed = true;
			}
		}
		
		for (FolderViewContainer c: openFolderViews) {
			c.updateChildren();
		}
		
		if (moveFailed == true) {
			mActivity.displayToast("Move failed. Please try again.");
		}
		
		this.clearSelections();
	}
	
	//*************************************** HierarchyItem management methods *******************************************************************
	
	public void openNote (HierarchyWrapper toOpen) {
		openNote(toOpen.hierarchyItem);
	}
	
	private void openNote (INoteHierarchyItem toOpen) {
		mActivity.openNoteActivity(toOpen);
	}
	
	public FolderView openFolder (HierarchyWrapper toOpen, FolderView parent) {
		return openFolder(toOpen.hierarchyItem, parent);
	}
	
	private FolderView openFolder (INoteHierarchyItem toOpen, FolderView parent) {
		// If the HierarchyItem is already open make sure it's displayed on screen but don't change openFolderViews or the mBrowser's
		// internal HashSet
		for (FolderViewContainer c : openFolderViews) {
			if (c.hierarchyItem.equals(toOpen)) {
				mBrowser.requestShow(c.folderView);
				return c.folderView;
			}
		}
		
		// Set up the new FolderView
		FolderView newFolderView = new FolderView(mActivity, this);
		FolderViewContainer newContainer = new FolderViewContainer(toOpen, newFolderView);
		newContainer.updateChildren();
		
		// Update openFolderViews
		int i = 0;
		boolean inserted = false;
		for (; i < openFolderViews.size(); i++) {
			openFolderViews.get(i).updateChildren();
			if(openFolderViews.get(i).folderView == parent) {
				openFolderViews.add(i+=1, newContainer);
				inserted = true;
				break;
			}
		}
		
		if (inserted == true) {
			i++;
			
			for (int j = openFolderViews.size()-1; j >= i; j--) {
				openFolderViews.remove(j);
			}
		} else {
			openFolderViews.clear();
			openFolderViews.add(newContainer);
		}
		
		// Produce ArrayList of Views
		List<View> toUpdateWith = new ArrayList<View>(openFolderViews.size());
		for (int j = 0; j < openFolderViews.size(); j++) {
			toUpdateWith.add(openFolderViews.get(j).folderView);
		}
		
		mBrowser.requestUpdateViews(toUpdateWith);
		
		return newFolderView;
	}

		
	//*************************************** Misc Methods (largely used for decoupling the views while re-architecting the main menu) **********************************
	

	public boolean testInRootDirectory() {
		return true;
	}
	
	private FolderViewContainer getContainerFromView (FolderView toGet) {
		// Get the hierarchy item that backs the FolderView that called this
		FolderViewContainer container = null;
		
		for (FolderViewContainer c : openFolderViews) {
			if (c.folderView == toGet) {
				container = c;
				break;
			}
		}
		
		return container;
	}
	
	
	public void setSelectedFolderView (FolderView nowSelected) {
		for (FolderViewContainer c : openFolderViews) {
			if (c.folderView == nowSelected) {
				c.folderView.setSelectedState(true);
				selectedContainer = c;
			} else {
				c.folderView.setSelectedState(false);
			}
		}
		
		if (selectedContainer != null && selectedContainer.folderView != nowSelected) {
			selectedContainer = null;
		}
	}
	
	public void dragStarted(FolderView calledFrom) {
		calledFrom.startDrag(selectedItems.size());
	}
	
	
	public void shareSelectedItems() {
		LinkedList<INoteHierarchyItem> toShare = new LinkedList<INoteHierarchyItem>();
		
		for (INoteHierarchyItem i : selectedItems) {
			if (i.isFolder()) {
				toShare.addAll(i.getAllRecursiveChildren());
			} else {
				toShare.add(i);
			}
		}
		
		if (toShare.size() == 1) {
			if (Sharer.shareNoteHierarchyItemsAsJPEG(toShare, mActivity) == false) {
				mActivity.displayToast("This note is too big to share, sorry for the inconvenience. I'm adding support for bigger notes in the next update.");
			}
		} else {
			mActivity.displayToast("You can only share one note at a time right now. Sharing multiple notes coming soon!");
		}
	}
	
	/**
	 * Handles back button events.
	 * @return true if the event was handled, false if it wasn't
	 */
	public boolean backButtonHandler () {
		boolean returnValue = false;
		
		if (selectedItems.isEmpty() == false) {
			this.clearSelections();
			returnValue = true;
		}
		
		return returnValue;
	}
	
	private boolean openFoldersContainsHierarchyItem (INoteHierarchyItem toTest) {
		
		for (FolderViewContainer c : openFolderViews) {
			if (c.hierarchyItem.equals(toTest)) {
				return true;
			}
		}
		
		return false;
	}
	
	// ******************************************** Selection methods ****************************************
	
	public void addSelection (HierarchyWrapper toSelect) {
		selectedItems.add(toSelect.hierarchyItem);
		selectionChangedMaintenence();

	}
	
	public void removeSelection (HierarchyWrapper toRemove) {
		selectedItems.remove(toRemove.hierarchyItem);
		selectionChangedMaintenence();
	}
	
	public boolean clearSelections () {
		boolean toReturn = !selectedItems.isEmpty();
		selectedItems.clear();
		selectionChangedMaintenence();
		return toReturn;
	}
	
	private void selectionChangedMaintenence() {
		for (FolderViewContainer c :openFolderViews) {
			c.updateChildren();
		}
		
		if (selectedItems.isEmpty()) {
			mActivity.setDefaultActionBarOn();
		} else {
			mActivity.setItemsSelectedActionBarOn();
		}
	}
	
	//********************************************* Helper classes ********************************************
	
	private class FolderViewContainer implements IChangeListener {
		public final INoteHierarchyItem hierarchyItem;
		public final FolderView folderView;
		public List<INoteHierarchyItem> children;
		
		public FolderViewContainer (INoteHierarchyItem newHierarchyItem, FolderView newFolderView) {
			hierarchyItem = newHierarchyItem;
			folderView = newFolderView;
			
			hierarchyItem.addChangeListener(this);
		}
		
		public synchronized void updateChildren() {
			children = hierarchyItem.getAllChildren();
			
			ArrayList<HierarchyWrapper> toUpdateWith = new ArrayList<HierarchyWrapper>(children.size());
			for (INoteHierarchyItem i : children) {
				toUpdateWith.add(new HierarchyWrapper(i, selectedItems.contains(i), openFoldersContainsHierarchyItem(i)));
			}
			
			folderView.updateContent(toUpdateWith);
		}

		public synchronized void onChange() {
			updateChildren();
		}
		
	}
	
	
	public class HierarchyWrapper {
		private final INoteHierarchyItem hierarchyItem;
		
		public final String name;
		public final Drawable thumbnail;
		public final long lastModified;
		public final boolean isFolder;
		public final boolean isSelected;
		public final boolean isOpen;
		
		public HierarchyWrapper (INoteHierarchyItem item, boolean selectionStatus, boolean openStatus) {
			hierarchyItem = item;
			
			name = item.getName();
			thumbnail = item.getThumbnail();
			lastModified = item.getDateModified();
			isFolder = item.isFolder();
			isSelected = selectionStatus;
			isOpen = openStatus;
		}
	}
	
	
}