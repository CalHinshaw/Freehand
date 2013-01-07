package com.calhounhinshaw.freehandalpha.note_orginazion;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.Drawable;

public class NoteFileHierarchyItem implements INoteHierarchyItem{
	private final File mFile;
	private final Drawable defaultNoteDrawable;
	private final Drawable defaultFolderDrawable;
	
	public NoteFileHierarchyItem (File newFile, Drawable noteDrawable, Drawable folderDrawable) {
		mFile = newFile;
		defaultNoteDrawable = noteDrawable;
		defaultFolderDrawable = folderDrawable;
	}
	
	
	
	
	
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
	
	// Not ideal, but needed (as far as I can tell) for this.equals. Shouldn't be accessible by anyone, anyway, because
	// they should be using INoteHierarchyItem.
	public File getFile () {
		return mFile;
	}

	public List<INoteHierarchyItem> getChildren() {
		
		// Return empty ArrayList of Files if mFile isn't a directory
		if (!mFile.isDirectory()) {
			return new ArrayList<INoteHierarchyItem>(0);
		}
		
		// Get array of files in mFile
		File mFileContents[] = mFile.listFiles();
		
		List<INoteHierarchyItem> toReturn = new ArrayList<INoteHierarchyItem>(mFileContents.length);

		for (File f : mFileContents) {
			if (!f.isHidden()) {
				if (f.isDirectory() || (f.isFile() && f.getName().contains(".note"))) {
					toReturn.add(new NoteFileHierarchyItem(f, defaultNoteDrawable, defaultFolderDrawable));
				}
			}
		}

		return toReturn;
	}
	
	@Override
	public boolean equals (Object obj) {
		if (!this.getClass().equals(obj.getClass())) {
			return false;
		}
		
		NoteFileHierarchyItem toTest = (NoteFileHierarchyItem) obj;
		if (mFile.equals(toTest.getFile())) {
			return true;
		}
		
		return false;
	}
	
	public boolean isFolder () {
		if (mFile.isDirectory()) {
			return true;
		} else {
			return false;
		}
	}
}