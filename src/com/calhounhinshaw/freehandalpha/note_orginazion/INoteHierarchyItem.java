package com.calhounhinshaw.freehandalpha.note_orginazion;

import java.util.List;

import android.graphics.drawable.Drawable;

public interface INoteHierarchyItem {
	public String getName();
	public long getDateModified();
	public Drawable getThumbnail();
	public boolean isFolder();
	
	public List<INoteHierarchyItem> getChildren ();
}