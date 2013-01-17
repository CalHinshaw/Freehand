package com.calhounhinshaw.freehandalpha.main_menu;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.calhounroberthinshaw.freehand.R;
import com.calhounhinshaw.freehandalpha.note_orginazion.IChangeListener;
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

class FolderAdapter extends ArrayAdapter<INoteHierarchyItem> {
	private static final int BLUE_HIGHLIGHT = 0x600099CC;
	
	private Context mContext;
	private int mRowViewResourceId;
	
	private boolean selectedItemsGreyed = false;
	
	
	
	
	public FolderAdapter (Context newContext, int newRowViewResourceId) {
		super(newContext, newRowViewResourceId, new ArrayList<INoteHierarchyItem>());
		mContext = newContext;
		mRowViewResourceId = newRowViewResourceId;
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
		holder.noteHierarchyItem = this.getItem(position);
		holder.name.setText(holder.noteHierarchyItem.getName());
		holder.thumbnail.setImageDrawable(holder.noteHierarchyItem.getThumbnail());
		holder.dateModified.setText(new Date(holder.noteHierarchyItem.getDateModified()).toString());
		

		// Change background color as appropriate
		if (holder.noteHierarchyItem.isSelected() && selectedItemsGreyed) {
			convertView.setBackgroundColor(Color.LTGRAY);
		} else if (holder.noteHierarchyItem.isSelected()) {
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
	}
	
	public void updateContent (List<INoteHierarchyItem> newContent) {
		this.clear();
		this.addAll(newContent);
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
}