package com.freehand.storage;

import java.util.ArrayList;
import java.util.List;

public interface INoteHierarchyItemSorter {
	public ArrayList<INoteHierarchyItem> sort (List<INoteHierarchyItem> toSort);
}