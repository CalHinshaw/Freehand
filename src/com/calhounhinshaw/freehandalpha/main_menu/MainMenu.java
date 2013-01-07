package com.calhounhinshaw.freehandalpha.main_menu;

import java.io.File;

import com.calhounhinshaw.freehandalpha.R;

import com.calhounhinshaw.freehandalpha.note_orginazion.INoteHierarchyItem;
import com.calhounhinshaw.freehandalpha.note_orginazion.NoteFileHierarchyItem;


import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.DragEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.widget.Button;
import android.widget.LinearLayout;

public class MainMenu extends Activity implements IActionBarListener {
	
	private NoteExplorer mExplorer;
	
	// itemsSelectedActionBar view references
	private LinearLayout itemsSelectedActionBar;
	private Button selectedCancelButton;
	private Button selectedDeleteButton;
	
	private OnClickListener cancelButtonOnClickListener = new OnClickListener() {
		public void onClick(View v) {
			// TODO implement cancleButtonOnClickListener	
		}
	};
	
	private OnClickListener deleteButtonOnClickListener = new OnClickListener() {
		public void onClick(View v) {
			// TODO implement deleteButtonOnClickListener	
		}
	};
	
	private OnDragListener cancelButtonDragListener = new OnDragListener () {
		public boolean onDrag(View v, DragEvent event) {
			// TODO implement cancelButtonDragListener
			return true;
		}
	};
	
	private OnDragListener deleteButtonDragListener = new OnDragListener () {
		public boolean onDrag(View v, DragEvent event) {
			// TODO implement deleteButtonDragListener
			return true;
		}
	};
	
	// defaultActionBar view references
	private LinearLayout defaultActionBar;
	private Button defaultNewNoteButton;
	private Button defaultNewFolderButton;
	private Button defaultShowAllNotesButton;
	
	private OnClickListener newNoteButtonOnClickListener = new OnClickListener() {
		public void onClick(View v) {
			// TODO implement newNoteButtonOnClickListener
		}
	};
	
	private OnClickListener newFolderButtonOnClickListener = new OnClickListener() {
		public void onClick(View v) {
			// TODO implement newFolderButtonOnClickListener
		}
	};
	
	private OnClickListener showAllNotesButtonOnClickListener = new OnClickListener() {
		public void onClick(View v) {
			// TODO implement showAllNotesButtonOnClickListener
		}
	};
	
	
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        
        // Set up the defaultActionBar
        defaultActionBar = (LinearLayout) findViewById(R.id.defaultActionBar);
        
        defaultNewNoteButton = (Button) findViewById(R.id.newNoteButton);
        defaultNewNoteButton.setOnClickListener(newNoteButtonOnClickListener);
        
        defaultNewFolderButton = (Button) findViewById(R.id.newFolderButton);
        defaultNewFolderButton.setOnClickListener(newFolderButtonOnClickListener);
        
        defaultShowAllNotesButton = (Button) findViewById(R.id.showAllNotesButton);
        defaultShowAllNotesButton.setOnClickListener(showAllNotesButtonOnClickListener);
        
        // Set up the itemsSelectedActionBar
        itemsSelectedActionBar = (LinearLayout) findViewById(R.id.itemsSelectedActionBar);
        
        selectedCancelButton = (Button) findViewById(R.id.cancelButton);
        selectedCancelButton.setOnClickListener(cancelButtonOnClickListener);
        selectedCancelButton.setOnDragListener(cancelButtonDragListener);
        
        selectedDeleteButton = (Button) findViewById(R.id.deleteButton);
        selectedDeleteButton.setOnClickListener(deleteButtonOnClickListener);
        selectedDeleteButton.setOnDragListener(deleteButtonDragListener);
        
        itemsSelectedActionBar.setVisibility(View.INVISIBLE);
        
        // Gather resources needed to create the NoteFileHierarchyItem that's getting passed to mExplorer
        File rootDirectory = Environment.getExternalStoragePublicDirectory("Freehand");
        Drawable defaultNoteDrawable = this.getResources().getDrawable(R.drawable.pencil);
        Drawable defaultFileDrawable = this.getResources().getDrawable(R.drawable.folder);
        
        INoteHierarchyItem rootItem = new NoteFileHierarchyItem(rootDirectory, defaultNoteDrawable, defaultFileDrawable);
        
        // Starts NoteExplorer in the app's root directory and set it's INoteHierarchyItem
        mExplorer = (NoteExplorer) findViewById(R.id.noteExplorer);
        mExplorer.setActionBarListener(this);
        mExplorer.setRootHierarchyItem(rootItem);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        return true;
    }
    
    // This method overrides the back button to let users navigate through folders more easily
    @Override
    public void onBackPressed() {
    	if(mExplorer.directoryHasSelected()) {			//first clear selections
    		mExplorer.clearDirectorySelections();
    		this.setDefaultActionBar();
    	} else if (mExplorer.isInRootDirectory()) {		// then close folders
    		super.onBackPressed();
    	} else {										// finally, close app
    		mExplorer.moveUpDirectory();
    	}
    }
    
    public void setItemsSelectedActionBar () {
    	itemsSelectedActionBar.setVisibility(View.VISIBLE);
    	defaultActionBar.setVisibility(View.INVISIBLE);
    }
    
    public void setDefaultActionBar () {
    	itemsSelectedActionBar.setVisibility(View.INVISIBLE);
    	defaultActionBar.setVisibility(View.VISIBLE);
    }
}