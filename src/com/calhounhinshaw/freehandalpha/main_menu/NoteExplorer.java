package com.calhounhinshaw.freehandalpha.main_menu;

import com.calhounroberthinshaw.freehand.R;
import com.calhounhinshaw.freehandalpha.note_orginazion.INoteHierarchyItem;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ViewAnimator;

public class NoteExplorer extends ViewAnimator{
	private Animation inFromLeftAnimation;
	private Animation inFromRightAnimation;
	private Animation outToLeftAnimation;
	private Animation outToRightAnimation;
	
	private INoteHierarchyItem mRootFolder;
	private IActionBarListener mActionBarListener;
	
	public NoteExplorer(Context context, AttributeSet attrs) {
		super(context, attrs);

		inFromRightAnimation = AnimationUtils.loadAnimation(this.getContext(), R.anim.in_from_right);
		inFromLeftAnimation = AnimationUtils.loadAnimation(this.getContext(), R.anim.in_from_left);
		outToRightAnimation = AnimationUtils.loadAnimation(this.getContext(), R.anim.out_to_right);
		outToLeftAnimation = AnimationUtils.loadAnimation(this.getContext(), R.anim.out_to_left);
	}
	
	// setActionBarListener MUST BE CALLED BEFORE THIS!!!!!!
	public void setRootHierarchyItem (INoteHierarchyItem newFolder) {
		mRootFolder = newFolder;
		
		this.addView(new FolderView(this.getContext(), mRootFolder, this, mActionBarListener));
	}
	
	public void setActionBarListener (IActionBarListener newListener) {
		mActionBarListener = newListener;
	}
	
	public void openNewFolder (INoteHierarchyItem newFolder) {
		this.addView(new FolderView(this.getContext(), newFolder, this, mActionBarListener));
	}
	
	public boolean isInRootDirectory() {
		INoteHierarchyItem currentFolder = ((FolderView) this.getCurrentView()).getNoteHierarchyItem();
		
		if (mRootFolder.equals(currentFolder)) {
			return true;
		} else {
			return false;
		}
	}
	
	public void moveUpDirectory () {
		if (isInRootDirectory()) {
			return;
		}
		
		// Set the animations used for going up in the file structure
		this.setInAnimation(inFromLeftAnimation);
		this.setOutAnimation(outToRightAnimation);
		
		// Deletes view but makes sure animation runs
		View toDelete = this.getCurrentView();
		this.getChildAt(this.getDisplayedChild()-1).setVerticalScrollBarEnabled(false);
		this.showPrevious();
		this.removeView(toDelete);
		this.getCurrentView().setVerticalScrollBarEnabled(true);
		
		forceUpdate();
	}
	
	@Override
	public void addView(View child) {
		this.setInAnimation(inFromRightAnimation);
		this.setOutAnimation(outToLeftAnimation);
		
		child.setVerticalScrollBarEnabled(false);
		super.addView(child);
		child.setVerticalScrollBarEnabled(true);
	}
	
	public boolean directoryHasSelected () {
		return ((FolderView) this.getCurrentView()).adapterHasSelections();
	}
	
	public void clearDirectorySelections () {
		((FolderView) this.getCurrentView()).clearAdapterSelections();
		
	}
		
	public void deleteSelectedItems () {
		((FolderView) this.getCurrentView()).deleteSelectedItems();
	}
	
	public void newNote() {
		((FolderView) this.getCurrentView()).newNote();
	}
	
	public void newFolder() {
		((FolderView) this.getCurrentView()).newFolder();
	}
	
	public void forceUpdate() {
		((FolderView) this.getCurrentView()).forceUpdate();
	}
}