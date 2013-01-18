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
	private int currentFolderViewID = 0;
	
	private final INoteHierarchyItem mRoot;
	
	private TreeSet<INoteHierarchyItem> selectedItems = new TreeSet<INoteHierarchyItem>();
	
	
	
	public MainMenuPresenter (MainMenuActivity activity, FolderBrowser browser, INoteHierarchyItem newRoot) {
		mActivity = activity;
		mBrowser = browser;
		mRoot = newRoot;
		
		this.openFolder(mRoot, -1);
	}
	
	
	//***************************************** Deletion Methods ****************************************
	
	/**
	 * Delete the INoteHierarchyItems passed in after presenting the user with a confirmation dialog.
	 * If something goes wrong during the file I/O the user will be informed by a toast.
	 * 
	 * @param toDelete will be deleted if the user confirms
	 */
	public void deleteWithConfirmation () {
		DialogFragment d = new ConfirmDeleteDialog("Confirm Delete?", "Delete", "Cancel", this.getSelections(), this);
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
	}
	
	
	//************************************** New Folder Methods *********************************************
	
	public void createNewFolder () {
		INoteHierarchyItem toCreateIn = this.getSelectedContainer().hierarchyItem;
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
			this.openFolder(newFolder, this.getSelectedContainer().ID);
		} else {
			mActivity.displayToast("Create new folder failed. Please try again.");
		}
	}
	
	//************************************** New Note Methods (who wants first class functions anyway?) ************************
	
	public void createNewNote () {
		INoteHierarchyItem toCreateIn = this.getSelectedContainer().hierarchyItem;
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
	
	public void moveTo (int callerID) {
		INoteHierarchyItem moveDest = this.getContainerFromID(callerID).hierarchyItem;
		 if (moveDest == null) {
			 return;
		 }

		boolean moveFailed = false;
		
		for (INoteHierarchyItem i : this.getSelections()) {
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
	}
	
	//*************************************** HierarchyItem management methods *******************************************************************
	
	public void openNote (HierarchyWrapper toOpen) {
		openNote(toOpen.hierarchyItem);
	}
	
	private void openNote (INoteHierarchyItem toOpen) {
		mActivity.openNoteActivity(toOpen);
	}
	
	public void openFolder (HierarchyWrapper toOpen, int parentID) {
		openFolder(toOpen.hierarchyItem, parentID);
	}
	
	private void openFolder (INoteHierarchyItem toOpen, int parentID) {
		currentFolderViewID += 1;
		
		// Set up the new FolderView
		FolderView newFolderView = new FolderView(mActivity, this);
		newFolderView.setId(currentFolderViewID);
		FolderViewContainer newContainer = new FolderViewContainer(newFolderView.getId(), toOpen, newFolderView);
		newContainer.updateChildren();
		newContainer.updateChildren();
		
		// Update openFolderViews
		int i = 0;
		boolean inserted = false;
		for (; i < openFolderViews.size(); i++) {
			if(openFolderViews.get(i).ID == parentID) {
				openFolderViews.add(i+=1, newContainer);
				inserted = true;
				break;
			}
		}
		
		if (inserted == true) {
			i++;
			for (; i < openFolderViews.size();) {
				openFolderViews.remove(i);
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
		
		mBrowser.updateViews(toUpdateWith);
		this.setSelectedFolderView(currentFolderViewID);
	}
	
	public void closeCurrentFolder() {
		//openFolderViews.removeLast();
	}

		
	//*************************************** Misc Methods (largely used for decoupling the views while re-architecting the main menu) **********************************
	

	public boolean testInRootDirectory() {
		return true;
	}
	
	public void turnDefaultActionBarOn () {
		mActivity.setDefaultActionBarOn();
	}
	
	public void turnItemsSelectedActionBarOn () {
		mActivity.setItemsSelectedActionBarOn();
	}
	
	private FolderViewContainer getContainerFromID (int callerID) {
		// Get the hierarchy item that backs the FolderView that called this
		FolderViewContainer container = null;
		
		for (FolderViewContainer c : openFolderViews) {
			if (c.ID == callerID) {
				container = c;
				break;
			}
		}
		
		return container;
	}
	
	private FolderViewContainer getSelectedContainer () {
		for (FolderViewContainer c : openFolderViews) {
			if (c.folderView.getSelectedState() == true) {
				return c;
			}
		}
		
		return null;
	}
	
	public void setSelectedFolderView (int selectedID) {
		for (FolderViewContainer c : openFolderViews) {
			if (c.ID == selectedID) {
				c.folderView.setSelectedState(true);
			} else {
				c.folderView.setSelectedState(false);
			}
			
		}
	}
	
	public void dragStarted(int callerID) {
		FolderView calledFrom = getContainerFromID(callerID).folderView;
		
		calledFrom.startDrag(this.getSelections().size());
	}
	
	
	// For share, which needs to be moved in here
	public List<INoteHierarchyItem> getSelections() {
		ArrayList<INoteHierarchyItem> selected = new ArrayList<INoteHierarchyItem>();
		for (FolderViewContainer c : openFolderViews) {
			for (INoteHierarchyItem i : c.children) {
				if (selectedItems.contains(i)) {
					selected.add(i);
				}
			}
		}
		return selected;
	}
	
	
	public void shareSelectedItems() {
		Log.d("PEN", "shareSelected in FolderView called");
		List<INoteHierarchyItem> selections = this.getSelections();
		LinkedList<INoteHierarchyItem> toShare = new LinkedList<INoteHierarchyItem>();
		
		for (INoteHierarchyItem i : selections) {
			if (i.isFolder()) {
				toShare.addAll(i.getAllRecursiveChildren());
			} else {
				toShare.add(i);
			}
		}
		
		if (toShare.size() == 1) {
			Sharer.shareNoteHierarchyItemsAsJPEG(toShare, mActivity);
		} else {
			mActivity.displayToast("You can only share one note at a time right now. Sharing multiple notes coming soon!");
		}
	}
	
	
	// ******************************************** Selection methods ****************************************
	
	public void addSelection (HierarchyWrapper toSelect) {
		selectedItems.add(toSelect.hierarchyItem);
	}
	
	public void removeSelection (HierarchyWrapper toRemove) {
		selectedItems.remove(toRemove.hierarchyItem);
	}
	
	public void clearSelections () {
		selectedItems.clear();
	}
	
	//********************************************* Helper classes ********************************************
	
	private class FolderViewContainer implements IChangeListener {
		public final int ID;
		public final INoteHierarchyItem hierarchyItem;
		public final FolderView folderView;
		
		public List<INoteHierarchyItem> children;
		
		public FolderViewContainer (int newID, INoteHierarchyItem newHierarchyItem, FolderView newFolderView) {
			ID = newID;
			hierarchyItem = newHierarchyItem;
			folderView = newFolderView;
			
			hierarchyItem.addChangeListener(this);
		}
		
		public synchronized void updateChildren() {
			children = hierarchyItem.getAllChildren();
			
			ArrayList<HierarchyWrapper> toUpdateWith = new ArrayList<HierarchyWrapper>(children.size());
			for (INoteHierarchyItem i : children) {
				toUpdateWith.add(new HierarchyWrapper(i, selectedItems.contains(i)));
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
		
		public HierarchyWrapper (INoteHierarchyItem item, boolean selectionStatus) {
			hierarchyItem = item;
			
			name = item.getName();
			thumbnail = item.getThumbnail();
			lastModified = item.getDateModified();
			isFolder = item.isFolder();
			isSelected = selectionStatus;
		}
	}
	
	
}