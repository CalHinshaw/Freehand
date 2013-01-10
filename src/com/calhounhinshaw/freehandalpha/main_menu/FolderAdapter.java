package com.calhounhinshaw.freehandalpha.main_menu;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.calhounhinshaw.freehandalpha.R;
import com.calhounhinshaw.freehandalpha.note_orginazion.IChangeListener;
import com.calhounhinshaw.freehandalpha.note_orginazion.INoteHierarchyItem;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

class FolderAdapter extends BaseAdapter implements IChangeListener {
	private static final int BLUE_HIGHLIGHT = 0x600099CC;
	
	private INoteHierarchyItem mFolder;
	private Context mContext;
	private int mRowViewResourceId;
	
	private Set<Integer> selectedItems = new TreeSet<Integer>();
	private boolean selectedItemsGreyed = false;
	
	public FolderAdapter (INoteHierarchyItem newFolder, int newRowViewResourceId, Context newContext) {
		mFolder = newFolder;
		mContext = newContext;
		mRowViewResourceId = newRowViewResourceId;
	}
	
	public int getCount() {
		return mFolder.getNumChildren();
	}

	public INoteHierarchyItem getItem(int position) {
		return mFolder.getChildAt(position);
	}

	public long getItemId(int position) {
		return -1;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		RowDataHolder holder;

		// If convertView is new initialize it
		if (convertView == null) {
			LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
			convertView = inflater.inflate(mRowViewResourceId, parent, false);
			
			// Set holder to convertView's sub-views for easier modification and faster retrieval
			holder = new RowDataHolder();
			holder.thumbnail = (ImageView) convertView.findViewById(R.id.DirectoryViewRowThumbnail);
			holder.name = (TextView) convertView.findViewById(R.id.DirectoryViewRowName);
			holder.dateModified = (TextView) convertView.findViewById(R.id.DirectoryViewRowDate);
			convertView.setTag(holder);
		} else {
			holder = (RowDataHolder) convertView.getTag();
		}
		
		// Set the content of convertView's sub-views
		holder.noteHierarchyItem = mFolder.getChildAt(position);
		holder.name.setText(holder.noteHierarchyItem.getName());
		holder.thumbnail.setImageDrawable(holder.noteHierarchyItem.getThumbnail());
		holder.dateModified.setText(new Date(holder.noteHierarchyItem.getDateModified()).toString());
		

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

	// simple class to make getView a bit clearer (and much faster)
	public static class RowDataHolder {
		ImageView thumbnail;
		TextView name;
		TextView dateModified;
		INoteHierarchyItem noteHierarchyItem;


		public RowDataHolder() {
		}
	}
	
	
	//****************************************** SELECTION METHODS ****************************************************8
	
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
			toReturn.add(mFolder.getChildAt(i));
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

	public void onChange() {
		this.notifyDataSetChanged();
	}
	
}