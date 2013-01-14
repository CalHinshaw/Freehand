package com.calhounhinshaw.freehandalpha.note_orginazion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import android.graphics.drawable.Drawable;
import android.os.Parcelable;

/**
 * Don't mix and match different implementations for INoteHierarchyItem!
 * @author cal
 *
 */
public interface INoteHierarchyItem extends Parcelable {
	public String getName();
	public long getDateModified();
	public Drawable getThumbnail();
	public boolean isFolder();
	
	public int getNumChildren ();
	public INoteHierarchyItem getChildAt (int index) throws IndexOutOfBoundsException;
	public boolean containsItemName (String testContains);
	
	public int getRecursiveNumChildren ();
	public INoteHierarchyItem getRecursiveChildAt (int index) throws IndexOutOfBoundsException;
	public List<INoteHierarchyItem> getAllRecursiveChildren();
	
	
	/**
	 * Rename this INoteHierarchyItem
	 * 
	 * @param newName The name getName will return in the future
	 * @return true if successful, false if unsuccessful
	 */
	public boolean rename (String newName);
	
	/**
	 * Delete this INoteHierarchyItem.
	 * Returns true if the deletion was successful, false if it was not. In the case of an unsuccessful deletion, the files successfully deleted WILL NOT be restored.
	 */
	public boolean delete();
	public boolean moveTo (INoteHierarchyItem destination);
	
	/**
	 * returns null if it failed to add the folder of the given name
	 */
	public INoteHierarchyItem addFolder (String folderName);
	
	/**
	 * returns null if it failed to add the note of the given name
	 */
	public INoteHierarchyItem addNote (String noteName);
	
	public void addChangeListener (IChangeListener toAdd);
	public void removeChangeListener (IChangeListener toRemove);
	public void clearChangeListeners ();
	
	public void setSorter (INoteHierarchyItemSorter newSorter);
	public void setDefaultDrawables (Drawable defaultNoteDrawable, Drawable defaultFolderDrawable);
	
	public DataOutputStream getOutputStream() throws IOException;
	public DataInputStream getInputStream() throws IOException;
	
	public void forceUpdate ();
}