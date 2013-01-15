package com.calhounhinshaw.freehandalpha.main_menu;

import com.calhounroberthinshaw.freehand.R;
import com.calhounhinshaw.freehandalpha.note_orginazion.INoteHierarchyItem;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
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
	private MainMenuPresenter mPresenter;
	
	public NoteExplorer(Context context, AttributeSet attrs) {
		super(context, attrs);

		inFromRightAnimation = AnimationUtils.loadAnimation(this.getContext(), R.anim.in_from_right);
		inFromLeftAnimation = AnimationUtils.loadAnimation(this.getContext(), R.anim.in_from_left);
		outToRightAnimation = AnimationUtils.loadAnimation(this.getContext(), R.anim.out_to_right);
		outToLeftAnimation = AnimationUtils.loadAnimation(this.getContext(), R.anim.out_to_left);
	}
	
	public void setPresenter (MainMenuPresenter newPresenter) {
		mPresenter = newPresenter;
	}
	
	public void setRootHierarchyItem (INoteHierarchyItem newFolder) {
		mRootFolder = newFolder;
	}
	
	

	
	public boolean isInRootDirectory() {
		return false;
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
	}
	
	public void openFolder (FolderView toAdd) {
		this.addView(toAdd);
		this.showNext();
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
	
	public void shareSelected() {
		Log.d("PEN", "shareSelected in NoteExplorer called");
		((FolderView) this.getCurrentView()).shareSelected();
	}
}