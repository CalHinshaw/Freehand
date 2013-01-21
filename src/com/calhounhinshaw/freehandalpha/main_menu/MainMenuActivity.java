package com.calhounhinshaw.freehandalpha.main_menu;

import java.io.File;

import com.calhounroberthinshaw.freehand.R;

import com.calhounhinshaw.freehandalpha.note_editor.NoteActivity;
import com.calhounhinshaw.freehandalpha.note_orginazion.DefaultNoteSorter;
import com.calhounhinshaw.freehandalpha.note_orginazion.INoteHierarchyItem;
import com.calhounhinshaw.freehandalpha.note_orginazion.NoteFileHierarchyItem;


import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.DragEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.Window;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainMenuActivity extends Activity {
	private static final int ORANGE_HIGHLIGHT = 0xFFFFBB33;
	private static final long VIBRATE_DURATION = 50;
	
	private MainMenuPresenter mPresenter;
	
	// itemsSelectedActionBar view references
	private LinearLayout itemsSelectedActionBar;
	private Button selectedCancelButton;
	private Button selectedDeleteButton;
	private Button selectedShareButton;
	
	private OnClickListener cancelButtonOnClickListener = new OnClickListener() {
		public void onClick(View v) {
			mPresenter.clearSelections();
		}
	};
	
	private OnClickListener deleteButtonOnClickListener = new OnClickListener() {
		public void onClick(View v) {
			mPresenter.deleteWithConfirmation();
		}
	};
	
	private OnClickListener shareButtonOnClickListener = new OnClickListener() {
		public void onClick(View v) {
			mPresenter.shareSelectedItems();
		}
	};
	
	private OnDragListener cancelButtonDragListener = new OnDragListener () {
		public boolean onDrag(View v, DragEvent event) {
			switch(event.getAction()) {
				case DragEvent.ACTION_DROP:
					mPresenter.clearSelections();
					v.getBackground().setColorFilter(null);
					break;
					
				case DragEvent.ACTION_DRAG_ENTERED:
					v.getBackground().setColorFilter(new PorterDuffColorFilter(ORANGE_HIGHLIGHT, PorterDuff.Mode.ADD));
					break;
					
				case DragEvent.ACTION_DRAG_EXITED:
					v.getBackground().setColorFilter(null);
					break;
			}
			
			return true;
		}
	};
	
	private OnDragListener deleteButtonDragListener = new OnDragListener () {
		public boolean onDrag(View v, DragEvent event) {
			switch(event.getAction()) {
				case DragEvent.ACTION_DROP:
					mPresenter.deleteWithConfirmation();
					v.getBackground().setColorFilter(null);
					break;
					
				case DragEvent.ACTION_DRAG_ENTERED:
					v.getBackground().setColorFilter(new PorterDuffColorFilter(ORANGE_HIGHLIGHT, PorterDuff.Mode.ADD));
					((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(VIBRATE_DURATION);
					break;
					
				case DragEvent.ACTION_DRAG_EXITED:
					v.getBackground().setColorFilter(null);
					break;
			}
			
			return true;
		}
	};
	
	private OnDragListener shareButtonDragListener = new OnDragListener() {

		public boolean onDrag(View v, DragEvent event) {
			switch(event.getAction()) {
				case DragEvent.ACTION_DROP:
					mPresenter.shareSelectedItems();
					v.getBackground().setColorFilter(null);
					break;
					
				case DragEvent.ACTION_DRAG_ENTERED:
					v.getBackground().setColorFilter(new PorterDuffColorFilter(ORANGE_HIGHLIGHT, PorterDuff.Mode.ADD));
					break;
					
				case DragEvent.ACTION_DRAG_EXITED:
					v.getBackground().setColorFilter(null);
					break;
			}
			
			return true;
		}
		
	};
	
	
	
	// defaultActionBar view references
	private LinearLayout defaultActionBar;
	private Button defaultNewNoteButton;
	private Button defaultNewFolderButton;
	
	private OnClickListener newNoteButtonOnClickListener = new OnClickListener() {
		public void onClick(View v) {
			mPresenter.createNewNote();
		}
	};
	
	private OnClickListener newFolderButtonOnClickListener = new OnClickListener() {
		public void onClick(View v) {
			mPresenter.createNewFolder();
		}
	};
	
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main_menu);
        
        
        // Set up the defaultActionBar
        defaultActionBar = (LinearLayout) findViewById(R.id.defaultActionBar);
        
        defaultNewNoteButton = (Button) findViewById(R.id.newNoteButton);
        defaultNewNoteButton.setOnClickListener(newNoteButtonOnClickListener);
        
        defaultNewFolderButton = (Button) findViewById(R.id.newFolderButton);
        defaultNewFolderButton.setOnClickListener(newFolderButtonOnClickListener);
        
        // Set up the itemsSelectedActionBar
        itemsSelectedActionBar = (LinearLayout) findViewById(R.id.itemsSelectedActionBar);
        
        selectedCancelButton = (Button) findViewById(R.id.cancelButton);
        selectedCancelButton.setOnClickListener(cancelButtonOnClickListener);
        selectedCancelButton.setOnDragListener(cancelButtonDragListener);
        
        selectedShareButton = (Button) findViewById(R.id.shareButton);
        selectedShareButton.setOnClickListener(shareButtonOnClickListener);
        selectedShareButton.setOnDragListener(shareButtonDragListener);
        
        selectedDeleteButton = (Button) findViewById(R.id.deleteButton);
        selectedDeleteButton.setOnClickListener(deleteButtonOnClickListener);
        selectedDeleteButton.setOnDragListener(deleteButtonDragListener);
        
        itemsSelectedActionBar.setVisibility(View.INVISIBLE);
        
        // Gather resources needed to create the NoteFileHierarchyItem that's getting passed to mExplorer
        File rootDirectory = Environment.getExternalStoragePublicDirectory("Freehand");
        Drawable defaultNoteDrawable = this.getResources().getDrawable(R.drawable.pencil);
        Drawable defaultFileDrawable = this.getResources().getDrawable(R.drawable.folder);
        
        INoteHierarchyItem rootItem = new NoteFileHierarchyItem(rootDirectory, null);
        rootItem.setDefaultDrawables(defaultNoteDrawable, defaultFileDrawable);
        rootItem.setSorter(new DefaultNoteSorter());
        
        HorizontalScrollView scrollView = ((HorizontalScrollView) findViewById(R.id.scrollView));
        FolderBrowser browser = new FolderBrowser(this, scrollView);
        scrollView.addView(browser);
        
        mPresenter = new MainMenuPresenter(this, browser, rootItem);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        return true;
    }
    
    @Override
    public void onBackPressed() {
    	if (!mPresenter.clearSelections()) {
    		super.onBackPressed();
    	}
    }
    
    public void setItemsSelectedActionBarOn () {
    	itemsSelectedActionBar.setVisibility(View.VISIBLE);
    	defaultActionBar.setVisibility(View.INVISIBLE);
    }
    
    public void setDefaultActionBarOn () {
    	itemsSelectedActionBar.setVisibility(View.INVISIBLE);
    	defaultActionBar.setVisibility(View.VISIBLE);
    }
    
    
    //**************************** PRESENTER METHODS (part of the rewrite) **************************
    
    public void displayDialogFragment (DialogFragment toDisplay, String tag) {
    	toDisplay.show(this.getFragmentManager(), tag);
    }
    
    public void displayToast (String toastText) {
    	Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();
    }
    
	public void openNoteActivity (INoteHierarchyItem toOpen) {
		Intent i = new Intent(this, NoteActivity.class);
		i.putExtra("com.calhounhinshaw.freehandalpha.note_editor.INoteHierarchyItem", toOpen);
		i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		this.startActivity(i);
	}
}