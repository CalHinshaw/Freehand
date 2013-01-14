package com.calhounhinshaw.freehandalpha.main_menu;

import java.util.List;

import android.app.DialogFragment;
import android.content.Intent;

import com.calhounhinshaw.freehandalpha.note_editor.NoteActivity;
import com.calhounhinshaw.freehandalpha.note_orginazion.INoteHierarchyItem;

/**
 * The presenter responsible for the MainMenu Activity.
 * 
 * @author Cal Hinshaw
 */
public class MainMenuPresenter {
	
	// The activity this presenter is responsible for. It is used to display Dialogs and Toasts.
	private final MainMenuActivity mActivity;
	
	// The NoteExplorer that holds the view stack. It is used to display FolderViews.
	private final NoteExplorer mExplorer;
	
	
	public MainMenuPresenter (MainMenuActivity activity, NoteExplorer explorer) {
		mActivity = activity;
		mExplorer = explorer;
	}
	
	
	//***************************************** Deletion Methods ****************************************
	
	/**
	 * Delete the INoteHierarchyItems passed in after presenting the user with a confirmation dialog.
	 * If something goes wrong during the file I/O the user will be informed by a toast.
	 * 
	 * @param toDelete will be deleted if the user confirms
	 */
	public void deleteWithConfirmation (List<INoteHierarchyItem> toDelete) {
		DialogFragment d = new ConfirmDeleteDialog("Confirm Delete?", "Delete", "Cancel", toDelete, this);
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
	
	public void createNewFolder (INoteHierarchyItem dest) {

			// Create the function that will be run when the user presses the Create Folder button
			NewItemFunctor newFolderFunction = new NewItemFunctor() {
				@Override
				public void function(INoteHierarchyItem destinationItem, String folderName) {
					createNewFolder(destinationItem, folderName);
				}
			};
			
			// Find the default input string - unnamed folder + the smallest unused natural number
			int i = 1;
			while (dest.containsItemName("unnamed folder " + Integer.toString(i))) {
				i++;
			}
			String defaultInput = "unnamed folder " + Integer.toString(i);
			
			// Create the dialog and pass it to activity to be run
			DialogFragment d = new NewItemDialog("Create New Folder", "Enter the name of the folder.", defaultInput, "Create Folder", "Cancel", dest, newFolderFunction);
			mActivity.displayDialogFragment(d, "New Folder");
	}
	
	public void createNewFolder (INoteHierarchyItem dest, String name) {
		INoteHierarchyItem newFolder = dest.addFolder(name);
		
		if (newFolder != null) {
			mExplorer.openFolder(newFolder);
		} else {
			mActivity.displayToast("Create new folder failed. Please try again.");
		}
	}
	
	//************************************** New Note Methods (who wants first class functions anyway?) ************************
	
	public void createNewNote (INoteHierarchyItem dest) {

		// Create the function that will be run when the user presses the Create Note button
		NewItemFunctor newNoteFunction = new NewItemFunctor() {
			@Override
			public void function(INoteHierarchyItem destinationItem, String folderName) {
				createNewNote(destinationItem, folderName);
			}
		};
		
		// Find the default input string - unnamed note + the smallest unused natural number
		int i = 1;
		while (dest.containsItemName("unnamed note " + Integer.toString(i))) {
			i++;
		}
		String defaultInput = "unnamed note " + Integer.toString(i);
		
		// Create the dialog and pass it to activity to be run
		DialogFragment d = new NewItemDialog("Create New Note", "Enter the name of the note.", defaultInput, "Create Note", "Cancel", dest, newNoteFunction);
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
	
	
	//*************************************** Open Note/Folder *******************************************************************
	
	public void openNote (INoteHierarchyItem toOpen) {
		mActivity.openNoteActivity(toOpen);
	}
	
	public void openFolder (INoteHierarchyItem toOpen) {
		mExplorer.openFolder(toOpen);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
}