package com.calhounhinshaw.freehandalpha.main_menu;

import java.io.File;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
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
	
}