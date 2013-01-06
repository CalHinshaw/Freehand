package com.calhounhinshaw.freehandalpha.note_orginazion;

import java.util.List;

import android.graphics.drawable.Drawable;

interface INoteHierarchyItem {
	public String getName();
	public long getDateModified();
	public Drawable getThumbnail();
	
	public List<INoteHierarchyItem> getChildren ();
}