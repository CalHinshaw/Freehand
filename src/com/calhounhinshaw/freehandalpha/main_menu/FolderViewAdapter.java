package com.calhounhinshaw.freehandalpha.main_menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.calhounhinshaw.freehandalpha.R;
import com.calhounhinshaw.freehandalpha.note_orginazion.INoteHierarchyItem;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FolderViewAdapter extends ArrayAdapter<INoteHierarchyItem> {
	private static final int BLUE_HIGHLIGHT = 0x600099CC;

	private Context mContext;
	private int mLayoutResourceId;
	private INoteHierarchyItem[] mItems;
	private Set<Integer> selectedItems = new TreeSet<Integer>();
	private boolean selectedItemsGreyed = false;

	public FolderViewAdapter(Context context, int layoutResourceId, INoteHierarchyItem[] items) {
		super(context, layoutResourceId, items);

		mContext = context;
		mLayoutResourceId = layoutResourceId;
		mItems = sortItems(items);
	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		RowDataHolder holder;

		// If convertView is new initialize it
		if (convertView == null) {
			LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
			convertView = inflater.inflate(mLayoutResourceId, parent, false);
			
			// Set holder to convertView's sub-views for easier modification and faster retrieval
			holder = new RowDataHolder();
			holder.thumbnail = (ImageView) convertView.findViewById(R.id.DirectoryViewRowThumbnail);
			holder.name = (TextView) convertView.findViewById(R.id.DirectoryViewRowName);
			holder.dateModified = (TextView) convertView.findViewById(R.id.DirectoryViewRowDate);
			convertView.setTag(holder);
		} else {
			holder = (RowDataHolder) convertView.getTag();
		}

		// Find last modified date of file
		Date modDate = new Date(mItems[position].getDateModified());

		// Set the content of convertView's sub-views
		holder.name.setText(mItems[position].getName());
		holder.thumbnail.setImageDrawable(mItems[position].getThumbnail());
		holder.dateModified.setText(modDate.toString());
		holder.noteHierarchyItem = mItems[position];

		// Change background color as appropriate
		if (selectedItems.contains(position) && selectedItemsGreyed) {
			convertView.setBackgroundColor(Color.LTGRAY);
		} else if (selectedItems.contains(position)) {
			convertView.setBackgroundColor(BLUE_HIGHLIGHT);
		} else {
			convertView.setBackgroundColor(0x0000000000);
		}

		return convertView;
	}

	// simple class to make getView a bit clearer
	public static class RowDataHolder {
		ImageView thumbnail;
		TextView name;
		TextView dateModified;
		INoteHierarchyItem noteHierarchyItem;


		public RowDataHolder() {
		}
	}


	// Sorts the files in the adapter. Directories are at the top in alphabetical order and notes are below them ordered by the date they were last modified on.
	private INoteHierarchyItem[] sortItems(INoteHierarchyItem[] unsorted) {
		ArrayList<INoteHierarchyItem> directories = new ArrayList<INoteHierarchyItem>(unsorted.length);
		ArrayList<INoteHierarchyItem> notes = new ArrayList<INoteHierarchyItem>(unsorted.length);

		// Separate directories and notes into their respective ArrayLists
		for (INoteHierarchyItem item : unsorted) {
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
		return directories.toArray(new INoteHierarchyItem[0]);
	}


	public void addSelection(int position) {
		selectedItems.add(position);
	}


	public void clearSelections() {
		selectedItems.clear();
		this.notifyDataSetChanged();
	}


	public boolean hasSelections() {
		return !selectedItems.isEmpty();
	}


	public List<INoteHierarchyItem> getSelections() {
		List<INoteHierarchyItem> toReturn = new LinkedList<INoteHierarchyItem>();
		
		for (int i : selectedItems) {
			toReturn.add(mItems[i]);
		}

		return toReturn;
	}
	
	public boolean isSelected (int position) {
		return selectedItems.contains(position);
	}
	
	public void removeSelection (int position) {
		selectedItems.remove(position);
		this.notifyDataSetChanged();
	}
	
	public void greySelections() {
		selectedItemsGreyed = true;
		this.notifyDataSetChanged();
	}


	public void ungreySelections() {
		selectedItemsGreyed = false;
		this.notifyDataSetChanged();
	}


	@Override
	public INoteHierarchyItem getItem(int position) {
		return mItems[position];
	}
}