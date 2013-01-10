package com.calhounhinshaw.freehandalpha.note_orginazion;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.graphics.drawable.Drawable;

public class NoteFileHierarchyItem implements INoteHierarchyItem {
	private final File mFile;
	private final Drawable defaultNoteDrawable;
	private final Drawable defaultFolderDrawable;
	private ArrayList<INoteHierarchyItem> mChildren = null;
	
	private INoteHierarchyItemSorter mSorter;
	private LinkedList<IChangeListener> mChangeListeners;
	
	// Used when this this note is moved or deleted to call NoteFileHierarchyItem.childrenModified();
	private NoteFileHierarchyItem mParent;
	
	public NoteFileHierarchyItem (File newFile, NoteFileHierarchyItem newParent, Drawable noteDrawable, Drawable folderDrawable) {
		mFile = newFile;
		mParent = newParent;
		defaultNoteDrawable = noteDrawable;
		defaultFolderDrawable = folderDrawable;
	}
	
	
	// ********************************* PUBLIC METHODS DEFINED BY INTERFACE *****************************
	
	public String getName() {
		return mFile.getName().replace(".note", "");
	}

	public long getDateModified() {
		return mFile.lastModified();
	}

	public Drawable getThumbnail() {
		if (mFile.isDirectory()) {
			return defaultFolderDrawable;
		} else {
			return defaultNoteDrawable;
		}
	}

	public boolean isFolder () {
		if (mFile.isDirectory()) {
			return true;
		} else {
			return false;
		}
	}
	

	public int getNumChildren() {
		// Make sure we've populated mChildren
		if (mChildren == null) {
			updateChildren();
		}
		
		return mChildren.size();
	}

	public INoteHierarchyItem getChildAt(int index) throws IndexOutOfBoundsException {
		// Make sure we've populated mChildren
		if (mChildren == null) {
			updateChildren();
		}

		return mChildren.get(index);
	}

	public boolean rename(String newName) {
		File newNameFile = new File(mFile.getParent(), newName);
		
		if (mFile.renameTo(newNameFile)) {
			return true;
		} else {
			return false;
		}
	}

	public boolean move(List<INoteHierarchyItem> destination) {
		// TODO Auto-generated method stub
		return false;
	}

	// Callbacks to parent will scale linearly with the number of children being deleted - I don't think it will be a problem but it might be.
	// This method doesn't deal with symlinks, but that shouldn't be an issue because they're a bitch to create on Android and if anyone knows how
	// to create one and decides to put it in my app's directory they can deal with the fall out themselves. That being said, I should add it at some
	// point in the future.
	public boolean delete() {
		updateChildren();
		
		if (isFolder()) {
			for (INoteHierarchyItem i : mChildren) {
				i.delete();
			}
		}
		
		if (!mFile.delete()) {
			notifyParentOfChange();
			return false;
		} else {
			notifyParentOfChange();
			return true;
		}
	}
	

	public void addChangeListener(IChangeListener toAdd) {
		mChangeListeners.add(toAdd);
	}

	public void removeChangeListener(IChangeListener toRemove) {
		mChangeListeners.remove(toRemove);
	}


	public INoteHierarchyItem addFolder(String folderName) {
		// TODO Auto-generated method stub
		return null;
	}

	public INoteHierarchyItem addNote(String noteName) {
		// TODO Auto-generated method stub
		return null;
	}


	public void setSorter(INoteHierarchyItemSorter newSorter) {
		mSorter = newSorter;
		updateChildren();
	}
	

	//*********************************** Public NoteFileHierarchyItem METHODS ***************************************
	public void childrenModified() {
		updateChildren();
		notifyChangeListeners();
	}
	
	
	//*********************************** INTERNAL HELPER METHODS ****************************************************
	
	// Update this object's internal list of children
	private void updateChildren() {
		
		// If this file is a note we have to set mChildren equal to an empty ArrayList by hand - File.listFiles returns null if the file isn't a directory
		if (!mFile.isDirectory()) {
			mChildren = new ArrayList<INoteHierarchyItem>(0);
		} else {
			mChildren = new ArrayList<INoteHierarchyItem>(mChildren.size());
			File fileContents[] = mFile.listFiles();
			
			for (File f : fileContents) {
				if (!f.isHidden()) {
					if (f.isDirectory() || (f.isFile() && f.getName().contains(".note"))) {
						mChildren.add(new NoteFileHierarchyItem(f, this, defaultNoteDrawable, defaultFolderDrawable));
					}
				}
			}
			
			// Use supplied sorter
			mChildren = mSorter.sort(mChildren);
		}
	}
	
	private void notifyChangeListeners() {
		for (IChangeListener l : mChangeListeners) {
			l.onChange();
		}
	}
	
	private void notifyParentOfChange () {
		if (mParent != null) {
			mParent.childrenModified();
		}
	}
}