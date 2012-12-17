package com.calhounhinshaw.freehandalpha.main_menu;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.calhounhinshaw.freehandalpha.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class DirectoryViewAdapter extends ArrayAdapter<File> {
	private static final int BLUE_HIGHLIGHT = 0x600099CC;
	
	private Context mContext;
	private int mLayoutResourceId;
	private File mFiles[] = null;
	private Set<Integer> selectedItems = new TreeSet<Integer>();
	private boolean selectedItemsGreyed = false;
	
	// Needed in getView
	private Drawable mFolderDrawable;
	private Drawable mDefaultNoteDrawable;

	public DirectoryViewAdapter(Context context, int layoutResourceId, File[] files, Drawable folderDrawable, Drawable defaultNoteDrawable) {
		super(context, layoutResourceId, files);
		
		mContext = context;
		mLayoutResourceId = layoutResourceId;
		mFiles = sortFiles(files);
		mFolderDrawable = folderDrawable;
		mDefaultNoteDrawable = defaultNoteDrawable;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		RowDataHolder holder = new RowDataHolder();
		
		// If convertView is new initialize it
		if (convertView == null) {
			LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
			convertView = inflater.inflate(mLayoutResourceId, parent, false);
		}
		
		// Set the tag to be the file this view represents so the view can be meaningfully interacted with by other classes
		convertView.setTag(mFiles[position]);
		
		// Temporarily set holder to convertView's sub-views for easier modification
		holder.thumbnail = (ImageView)convertView.findViewById(R.id.DirectoryViewRowThumbnail);
		holder.name = (TextView)convertView.findViewById(R.id.DirectoryViewRowName);
		holder.dateModified = (TextView)convertView.findViewById(R.id.DirectoryViewRowDate);
		
		// Find last modified date of file
		Date modDate = new Date(mFiles[position].lastModified());
		
		// Set the content of convertView's sub-views. Folders and notes get treated differently
		if (mFiles[position].isDirectory()) {
			holder.name.setText(mFiles[position].getName());
			holder.thumbnail.setImageDrawable(mFolderDrawable);
			holder.dateModified.setText(modDate.toString());
		} else if (mFiles[position].isFile()) {
			String noteName = mFiles[position].getName().replace(".note", "");
			holder.name.setText(noteName);
			holder.thumbnail.setImageDrawable(mDefaultNoteDrawable);
			holder.dateModified.setText(modDate.toString());
		}
		
		// Change background color as appropriate
		if (selectedItems.contains(position) && selectedItemsGreyed) {
			convertView.setBackgroundColor(Color.LTGRAY);
		} else if (selectedItems.contains(position)) {
			convertView.setBackgroundColor(BLUE_HIGHLIGHT);
		} else {
			convertView.setBackgroundColor(Color.WHITE);
		}
		
		return convertView;
	}
	
	// simple class to make getView a bit clearer
	private static class RowDataHolder {
		ImageView thumbnail;
		TextView name;
		TextView dateModified;
		
		public RowDataHolder() {}
	}
	
	// Sorts the files in the adapter. Directories are at the top in alphabetical order and notes are below them ordered by the date they were last modified on.
	private File[] sortFiles (File[] unsorted) {
		ArrayList<File> directories = new ArrayList<File>(unsorted.length);
		ArrayList<File> notes = new ArrayList<File>(unsorted.length);
		
		// Separate directories and notes into their respective ArraLists
		for (File f : unsorted) {
			if (f.isDirectory()) {
				directories.add(f);
			} else {
				notes.add(f);
			}
		}
		
		// Sort Directories alphabetically
		Collections.sort(directories, new Comparator<File>(){
			public int compare(File arg0, File arg1) {
				return arg0.getName().compareTo(arg1.getName());
			}
		});
		
		// Sort Directories by date modified
		Collections.sort(notes, new Comparator<File>() {
			public int compare(File arg0, File arg1) {
				return Long.valueOf(arg1.lastModified()).compareTo(arg0.lastModified());
			}
		});
		
		// Concatenate directories and notes (which are now sorted) into an array - kinda messy
		directories.addAll(notes);
		return directories.toArray(new File[0]);
	}
	
	public void addSelection (int position) {
		selectedItems.add(position);
	}
	
	public void clearSelections() {
		selectedItems.clear();
		this.notifyDataSetChanged();
	}
	
	public boolean hasSelections() {
		return !selectedItems.isEmpty();
	}
	
	public File[] getSelections () {
		Integer[] selected = selectedItems.toArray(new Integer[1]);
		File[] files = new File[selected.length];
		
		for (int i = 0; i < selected.length; i++) {
			files[i] = mFiles[selected[i]];
		}
		
		return files;
	}
	
	public void greySelections () {
		selectedItemsGreyed = true;
		this.notifyDataSetChanged();
	}
	
	public void ungreySelections () {
		selectedItemsGreyed = false;
		this.notifyDataSetChanged();
	}
}