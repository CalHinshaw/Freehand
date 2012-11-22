package com.calhounhinshaw.freehandalpha.main_menu;

import java.io.File;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ViewAnimator;

public class NoteExplorer extends ViewAnimator{
	private static final float ANIMATION_SPEED = 1.0f;
	private static final long ANIMATION_DURATION = 500;
	
	private Animation inFromLeftAnimation;
	private Animation inFromRightAnimation;
	private Animation outToLeftAnimation;
	private Animation outToRightAnimation;
	
	private File rootDirectory;
	
	public NoteExplorer(Context context, AttributeSet attrs) {
		super(context, attrs);

		setAnimations();
	}
	
	private void setAnimations () {
		inFromRightAnimation = new TranslateAnimation (
				Animation.RELATIVE_TO_PARENT, ANIMATION_SPEED,
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f);
		inFromRightAnimation.setDuration(ANIMATION_DURATION);
		
		inFromLeftAnimation = new TranslateAnimation (
				Animation.RELATIVE_TO_PARENT, -ANIMATION_SPEED,
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f);
		inFromLeftAnimation.setDuration(ANIMATION_DURATION);
		
		outToRightAnimation = new TranslateAnimation (
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, ANIMATION_SPEED,
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f);
		outToRightAnimation.setDuration(ANIMATION_DURATION);
		
		outToLeftAnimation = new TranslateAnimation (
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, -ANIMATION_SPEED,
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f);
		outToLeftAnimation.setDuration(ANIMATION_DURATION);
	}
	
	public void setRootDirectory (File newRootDirectory) {
		rootDirectory = newRootDirectory;
		rootDirectory.mkdirs();
		
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
		if (isInRootDirectory()) {
			return;
		}
		
		// Set the animations used for going up in the file structure
		this.setInAnimation(inFromLeftAnimation);
		this.setOutAnimation(outToRightAnimation);
		
		// Deletes view but makes sure animation runs
		View toDelete = this.getCurrentView();
		this.showPrevious();
		this.removeView(toDelete);
	}
	
	@Override
	public void addView(View child) {
		this.setInAnimation(inFromRightAnimation);
		this.setOutAnimation(outToLeftAnimation);
		
		super.addView(child);
	}
}