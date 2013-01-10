package com.calhounhinshaw.freehandalpha.note_orginazion;

import java.util.ArrayList;
import java.util.List;

public interface INoteHierarchyItemSorter {
	public ArrayList<INoteHierarchyItem> sort (List<INoteHierarchyItem> toSort);
}