package com.calhounhinshaw.freehandalpha.note_orginazion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DefaultNoteSorter implements INoteHierarchyItemSorter {

	public ArrayList<INoteHierarchyItem> sort(List<INoteHierarchyItem> toSort) {
		ArrayList<INoteHierarchyItem> directories = new ArrayList<INoteHierarchyItem>(toSort.size());
		ArrayList<INoteHierarchyItem> notes = new ArrayList<INoteHierarchyItem>(toSort.size());

		// Separate directories and notes into their respective ArrayLists
		for (INoteHierarchyItem item : toSort) {
			if (item.isFolder()) {
				directories.add(item);
			} else {
				notes.add(item);
			}
		}

		// Sort Directories alphabetically
		Collections.sort(directories, new Comparator<INoteHierarchyItem>() {
			public int compare(INoteHierarchyItem arg0, INoteHierarchyItem arg1) {
				return arg0.getName().compareTo(arg1.getName());
			}
		});

		// Sort Directories by date modified
		Collections.sort(notes, new Comparator<INoteHierarchyItem>() {
			public int compare(INoteHierarchyItem arg0, INoteHierarchyItem arg1) {
				return Long.valueOf(arg1.getDateModified()).compareTo(arg0.getDateModified());
			}
		});

		// Concatenate directories and notes (which are now sorted) into an array - kinda messy
		directories.addAll(notes);
		return directories;
		
	}
}