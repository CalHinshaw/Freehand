package com.calhounhinshaw.freehandalpha;

import java.io.File;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewAnimator;

public class NoteExplorer extends ViewAnimator{
	private File rootDirectory;
	
	public NoteExplorer(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void setRootDirectory (File newRootDirectory) {
		rootDirectory = newRootDirectory;
		this.addView(new DirectoryView(this.getContext(), rootDirectory, this));
	}
	
	public void openNewDirectory (File newDirectory) {
		this.addView(new DirectoryView(this.getContext(), newDirectory, this));
	}
	
	public boolean isInRootDirectory() {
		File currentDirectory = ((DirectoryView) this.getCurrentView()).getDirectory();
		
		if (rootDirectory.equals(currentDirectory)) {
			return true;
		} else {
			return false;
		}
	}
	
	public void moveUpDirectory () {
		Log.d("PEN", "moveUpDirectory called");
		
		if (isInRootDirectory()) {
			Log.d("PEN", "Can't move up directory, already in root.");
			return;
		}
		
		this.removeView(this.getCurrentView());
	}
	
	private class DirectoryView extends ListView {
		private NoteExplorer mExplorer;
		private ArrayAdapter<String> mAdapter;
		private File mDirectory;
		
		public DirectoryView(Context context, File newDirectory, NoteExplorer newExplorer) {
			super(context);
			mExplorer = newExplorer;
			mDirectory = newDirectory;
			
			mAdapter = new ArrayAdapter<String> (this.getContext(), android.R.layout.simple_expandable_list_item_1, mDirectory.list());
			this.setAdapter(mAdapter);
			this.setOnItemClickListener(DirectoryViewItemClickListener);
		}
		
		private OnItemClickListener DirectoryViewItemClickListener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				File clickedFile = new File(mDirectory, ((TextView)arg1).getText().toString());
				
				if (clickedFile.isDirectory()) {
					mExplorer.addView(new DirectoryView(mExplorer.getContext(), clickedFile, mExplorer));
					mExplorer.showNext();
				}
			}
		};
		
		public File getDirectory() {
			return mDirectory;
		}
		
	}
	
	
}