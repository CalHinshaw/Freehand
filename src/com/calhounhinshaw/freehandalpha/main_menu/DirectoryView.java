package com.calhounhinshaw.freehandalpha.main_menu;

import java.io.File;
import java.util.LinkedList;
import com.calhounhinshaw.freehandalpha.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class DirectoryView extends ListView {
	private NoteExplorer mExplorer;
	private DirectoryViewAdapter mAdapter;
	private File mDirectory;
	
	// These get passed to DirectoryViewAdapters when they're created
	private Drawable folderDrawable;
	private Drawable defaultNoteDrawable;
	
	public DirectoryView(Context context, File newDirectory, NoteExplorer newExplorer) {
		super(context);
		mExplorer = newExplorer;
		mDirectory = newDirectory;
		
		folderDrawable = this.getContext().getResources().getDrawable(R.drawable.folder);
		defaultNoteDrawable = this.getContext().getResources().getDrawable(R.drawable.pencil);
		
		File filesInDir[] = mDirectory.listFiles();
		LinkedList<File> validFilesInDir = new LinkedList<File>();
		
		// Remove files in the directory that are hidden or aren't .note files
		for (File f : filesInDir) {
			if (f.isDirectory() && !f.isHidden()) {
				validFilesInDir.add(f);
			} else if (f.isFile() && !f.isHidden()) {
				if (f.getName().contains(".note")) {
					validFilesInDir.add(f);
				}
			}
		}
		
		// Create the adapter for this list view using the cleaned list of files, validFilesInDir
		mAdapter = new DirectoryViewAdapter(this.getContext(), R.layout.directoryview_row, validFilesInDir.toArray(new File[0]), folderDrawable, defaultNoteDrawable);
		
		this.setAdapter(mAdapter);
		this.setOnItemClickListener(DirectoryViewItemClickListener);
	}
	
	// Defines behavior of view elements of this ListView when clicked
	private OnItemClickListener DirectoryViewItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
			File clickedFile = (File) arg1.getTag();	// know arg1's tag is a file because of how it's created in DirectoryViewAdapter.getView
			
			// Clicking on directory opens it
			if (clickedFile.isDirectory()) {
				mExplorer.addView(new DirectoryView(mExplorer.getContext(), clickedFile, mExplorer));
				mExplorer.showNext();
			}
			
			//TODO: implement click on file (opens the note)
		}
	};
	
	public File getDirectory() {
		return mDirectory;
	}
	
}