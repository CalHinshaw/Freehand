package com.calhounhinshaw.freehandalpha.main_menu;

import java.io.File;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class DirectoryView extends ListView {
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