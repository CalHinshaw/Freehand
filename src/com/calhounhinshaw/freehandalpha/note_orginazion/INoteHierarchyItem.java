package com.calhounhinshaw.freehandalpha.note_orginazion;

import java.util.List;

import android.graphics.drawable.Drawable;

public interface INoteHierarchyItem {
	public String getName();
	public long getDateModified();
	public Drawable getThumbnail();
	public boolean isFolder();
	
	public int getNumChildren ();
	public INoteHierarchyItem getChildAt (int index);
	
	// return false if something in the directory being modified has been modified in a conflicting way
	public boolean moveNoteHierarchyItems (List<INoteHierarchyItem> toMove);
	public boolean deleteChildAtIndex (int toDelete);
	public boolean renameItemAtIndex (int index, String newName);
	
	public INoteHierarchyItem addFolder (String folderName);
	public INoteHierarchyItem addNote (String noteName);
	
	public void addChangeListener (IChangeListener toAdd);
	public void removeChangeListener (IChangeListener toRemove);
	
	public void setSorter (INoteHierarchyItemSorter newSorter);
}