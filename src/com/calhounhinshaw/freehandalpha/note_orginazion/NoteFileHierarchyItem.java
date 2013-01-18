package com.calhounhinshaw.freehandalpha.note_orginazion;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class NoteFileHierarchyItem implements INoteHierarchyItem {
	private File mFile;
	private Drawable defaultNoteDrawable = null;
	private Drawable defaultFolderDrawable = null;
	private ArrayList<INoteHierarchyItem> mChildren = null;
	private ArrayList<INoteHierarchyItem> mRecursiveChildren = null;
	
	private INoteHierarchyItemSorter mSorter;
	private LinkedList<IChangeListener> mChangeListeners = new LinkedList<IChangeListener>();
	
	private boolean isSelected = false;
	
	// Used when this this note is moved or deleted to call NoteFileHierarchyItem.childrenModified();
	private NoteFileHierarchyItem mParent = null;
	
	public NoteFileHierarchyItem (File newFile, NoteFileHierarchyItem newParent) {
		mFile = newFile;
		mParent = newParent;
	}
	
	
	// ********************************* PUBLIC METHODS DEFINED BY INTERFACE *****************************
	
	public synchronized String getName() {
		return mFile.getName().replace(".note", "");
	}

	public synchronized long getDateModified() {
		return mFile.lastModified();
	}

	public synchronized Drawable getThumbnail() {
		if (mFile.isDirectory()) {
			if (defaultFolderDrawable != null) {
				return defaultFolderDrawable;
			} else {
				return new ColorDrawable();
			}
			
		} else {
			if (defaultNoteDrawable != null) {
				return defaultNoteDrawable;
			} else {
				return new ColorDrawable();
			}
		}
	}

	public synchronized boolean isFolder () {
		if (mFile.isDirectory()) {
			return true;
		} else {
			return false;
		}
	}
	

	public synchronized int getNumChildren() {
		// Make sure we've populated mChildren
		updateChildren();
		
		return mChildren.size();
	}

	public synchronized INoteHierarchyItem getChildAt(int index) throws IndexOutOfBoundsException {
		// Make sure we've populated mChildren
		updateChildren();

		return mChildren.get(index);
	}
	

	public synchronized List<INoteHierarchyItem> getAllChildren() {
		updateChildren();
		
		return mChildren;
	}
	
	
	public synchronized boolean containsItemName(String testContains) {
		// Make sure we've populated mChildren
			updateChildren();
				
		for (INoteHierarchyItem i : mChildren) {
			if (testContains.equals(i.getName())) {
				return true;
			}
		}
		
		return false;
	}
	
	
	public synchronized boolean isSelected() {
		return isSelected;
	}

	public synchronized void setSelected(boolean newSelected) {
		isSelected = newSelected;
		notifyChangeListeners();
	}
	
	
	public synchronized int getRecursiveNumChildren() {
		if (mRecursiveChildren == null) {
			recursivelyUpdateChildren();
		}
		
		return mRecursiveChildren.size();
	}


	public synchronized INoteHierarchyItem getRecursiveChildAt(int index) throws IndexOutOfBoundsException {
		if (mRecursiveChildren == null) {
			recursivelyUpdateChildren();
		}
		
		return mRecursiveChildren.get(index);
	}
	
	public synchronized List<INoteHierarchyItem> getAllRecursiveChildren() {
		if (mRecursiveChildren == null) {
			recursivelyUpdateChildren();
		}
		
		return mRecursiveChildren;
	}
	

	public synchronized boolean rename(String newName) {
		Log.d("PEN", "Before:  " + mFile.getAbsolutePath());
		File newNameFile;
		
		if (!mFile.isDirectory()) {
			newNameFile = new File(mFile.getParent(), newName + ".note");
		} else {
			newNameFile = new File(mFile.getParent(), newName);
		}
		
		
		if (mFile.renameTo(newNameFile)) {
			mFile = newNameFile;
			notifyChangeListeners();
			notifyParentOfChange();
			return true;
		} else {
			return false;
		}
	}

	public synchronized boolean moveTo(INoteHierarchyItem destination) {
		File newParent = ((NoteFileHierarchyItem) destination).getFile();
		File newName = new File(newParent, mFile.getName());
		
		if(mFile.renameTo(newName)) {
			mFile = newName;
			mParent = (NoteFileHierarchyItem) destination;
			
			notifyChangeListeners();
			notifyParentOfChange();
			return true;
		} else {
			return false;
		}
	}

	// Callbacks to parent will scale linearly with the number of children being deleted - I don't think it will be a problem but it might be.
	// This method doesn't deal with symlinks, but that shouldn't be an issue because they're a bitch to create on Android and if anyone knows how
	// to create one and decides to put it in my app's directory they can deal with the fall out themselves. That being said, I should add it at some
	// point in the future.
	public synchronized boolean delete() {
		// Make sure we've populated mChildren
		if (mChildren == null) {
			updateChildren();
		}
		
		if (isFolder()) {
			for (INoteHierarchyItem i : mChildren) {
				i.delete();
			}
		}
		
		if (!mFile.delete()) {
			notifyChangeListeners();
			notifyParentOfChange();
			return false;
		} else {
			notifyChangeListeners();
			notifyParentOfChange();
			return true;
		}
	}
	

	public synchronized void addChangeListener(IChangeListener toAdd) {
		mChangeListeners.add(toAdd);
	}

	public synchronized void removeChangeListener(IChangeListener toRemove) {
		mChangeListeners.remove(toRemove);
	}
	
	public synchronized void clearChangeListeners() {
		mChangeListeners = new LinkedList<IChangeListener>();
	}


	public synchronized INoteHierarchyItem addFolder(String folderName) {
		File newFolder = new File(mFile, folderName);
		if (newFolder.mkdirs()) {
			updateChildren();
			notifyChangeListeners();
			notifyParentOfChange();
			
			NoteFileHierarchyItem newItem = new NoteFileHierarchyItem(newFolder, this);
			newItem.setDefaultDrawables(defaultNoteDrawable, defaultFolderDrawable);
			newItem.setSorter(mSorter);
			return newItem;
			
		} else {
			return null;
		}
		
		
	}

	public synchronized INoteHierarchyItem addNote(String noteName) {
		try {
			File newNote = new File(mFile, noteName + ".note");
			newNote.createNewFile();
			updateChildren();
			notifyChangeListeners();
			notifyParentOfChange();
			
			NoteFileHierarchyItem newItem = new NoteFileHierarchyItem(newNote, this);
			newItem.setDefaultDrawables(defaultNoteDrawable, defaultFolderDrawable);
			newItem.setSorter(mSorter);
			return newItem;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}


	public synchronized void setSorter(INoteHierarchyItemSorter newSorter) {
		mSorter = newSorter;
		updateChildren();
	}
	
	public synchronized void setDefaultDrawables (Drawable newNoteDrawable, Drawable newFolderDrawable) {
		defaultNoteDrawable = newNoteDrawable;
		defaultFolderDrawable = newFolderDrawable;
	}
	
	
	public synchronized DataOutputStream getOutputStream() throws IOException {
		return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(mFile)));
	}


	public synchronized DataInputStream getInputStream() throws IOException {
		return new DataInputStream(new BufferedInputStream(new FileInputStream(mFile)));
	}
	
	public synchronized void forceUpdate() {
		updateChildren();
	}
	
	
	// ********************************* Parcelable METHODS ****************************************
	
	public synchronized int describeContents() {
		return 0;
	}


	public synchronized void writeToParcel(Parcel dest, int flags) {
		Log.d("PEN", mFile.getAbsolutePath());
		
		dest.writeString(mFile.getAbsolutePath());
	}
	
	public static final Parcelable.Creator<INoteHierarchyItem> CREATOR = new Parcelable.Creator<INoteHierarchyItem>() {
		public INoteHierarchyItem createFromParcel(Parcel in) {
			String test = in.readString();
			Log.d("PEN", test);
			File newFile = new File(test);
			
			return new NoteFileHierarchyItem(newFile, null);
		}

		public INoteHierarchyItem[] newArray(int size) {
			return new INoteHierarchyItem[size];
		}
	};
	
	
	//*********************************** Public NoteFileHierarchyItem METHODS ***************************************
	public synchronized void childrenModified() {
		updateChildren();
		notifyChangeListeners();
	}
	
	/**
	 * Use this at your own risk. It should only really be used internally.
	 * 
	 * @return the file this NoteFileHierarchyItem is based on
	 */
	public synchronized File getFile() {
		return mFile;
	}
	
	//*********************************** INTERNAL HELPER METHODS ****************************************************
	
	// Update this object's internal list of children
	private synchronized void updateChildren() {
		
		// If this file is a note we have to set mChildren equal to an empty ArrayList by hand - File.listFiles returns null if the file isn't a directory
		if (!mFile.isDirectory()) {
			mChildren = new ArrayList<INoteHierarchyItem>(0);
		} else {
			mChildren = new ArrayList<INoteHierarchyItem>();
			File fileContents[] = mFile.listFiles();
			
			for (File f : fileContents) {
				if (!f.isHidden()) {
					if (f.isDirectory() || (f.isFile() && f.getName().contains(".note"))) {
						
						NoteFileHierarchyItem newItem = new NoteFileHierarchyItem(f, this);
						newItem.setDefaultDrawables(defaultNoteDrawable, defaultFolderDrawable);
						newItem.setSorter(mSorter);
						
						mChildren.add(newItem);
					}
				}
			}
			
			if (mSorter != null) {
				mChildren = mSorter.sort(mChildren);
			}
		}
	}
	
	private synchronized void recursivelyUpdateChildren () {
		if (!mFile.isDirectory()) {
			mRecursiveChildren = new ArrayList<INoteHierarchyItem>(0);
		} else {
			mRecursiveChildren = new ArrayList<INoteHierarchyItem>();
			LinkedList<File> children = recursiveList(mFile);
			
			for (File f : children) {
				if (!f.isHidden() && f.isFile() && f.getName().contains(".note")) {
					NoteFileHierarchyItem newItem = new NoteFileHierarchyItem(f, this);
					newItem.setDefaultDrawables(defaultNoteDrawable, defaultFolderDrawable);
					newItem.setSorter(mSorter);
					
					mRecursiveChildren.add(newItem);
				}
			}
			
			if (mSorter != null) {
				mRecursiveChildren = mSorter.sort(mRecursiveChildren);
			}
		}
	}
	
	private synchronized LinkedList<File> recursiveList (File toList) {
		LinkedList<File> toReturn = new LinkedList<File>();
		File[] directChildren = toList.listFiles();
		
		for (File f : directChildren) {
			if (f.isDirectory()) {
				toReturn.addAll(recursiveList(f));
			} else {
				toReturn.add(f);
			}
		}
		
		return toReturn;
	}
	
	private synchronized void notifyChangeListeners() {
		for (IChangeListener l : mChangeListeners) {
			l.onChange();
		}
	}
	
	private synchronized void notifyParentOfChange () {
		if (mParent != null) {
			mParent.childrenModified();
		}
	}
}