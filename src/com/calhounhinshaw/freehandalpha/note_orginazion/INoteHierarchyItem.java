package com.calhounhinshaw.freehandalpha.note_orginazion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.List;

import android.graphics.drawable.Drawable;

public interface INoteHierarchyItem {
	public String getName();
	public long getDateModified();
	public Drawable getThumbnail();
	public boolean isFolder();
	
	public int getNumChildren ();
	public INoteHierarchyItem getChildAt (int index) throws IndexOutOfBoundsException;
	public boolean containsItemName (String testContains);
	
	/**
	 * Rename this INoteHierarchyItem
	 * 
	 * @param newName
	 * @return true if successful, false if unsuccessful. If a change that cannot be reconciled has been made to the
	 * underlying representation since the last time it was updated, a renaming won't even be tried.
	 */
	public boolean rename (String newName);
	
	/**
	 * Delete this INoteHierarchyItem.
	 * Returns true if the deletion was successful, false if it was not. In the case of an unsuccessful deletion, the files successfully deleted WILL NOT be restored.
	 */
	public boolean delete();
	public boolean move (List<INoteHierarchyItem> destination);
	
	public INoteHierarchyItem addFolder (String folderName);
	public INoteHierarchyItem addNote (String noteName);
	
	public void addChangeListener (IChangeListener toAdd);
	public void removeChangeListener (IChangeListener toRemove);
	
	public void setSorter (INoteHierarchyItemSorter newSorter);
	
	public DataOutputStream getOutputStream();
	public DataInputStream getInputStream();
}