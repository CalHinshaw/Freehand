package com.freehand.organizer;

import java.io.File;

import com.calhounroberthinshaw.freehand.R;

import com.freehand.preferences.PrefActivity;
import com.freehand.tutorial.TutorialActivity;
import com.freehand.tutorial.TutorialPrefs;

import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.view.DragEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

public class MainMenuActivity extends Activity {
	private static final long VIBRATE_DURATION = 50;
	private static boolean isRunning = false;
	
	private FolderBrowser mBrowser;
	
	// itemsSelectedActionBar view references
	private ViewGroup itemsSelectedActionBar;
	private HighlightButton selectedCancelButton;
	private HighlightButton selectedDeleteButton;
	private HighlightButton selectedShareButton;
	private HighlightButton selectedRenameButton;
	
	private OnClickListener cancelButtonOnClickListener = new OnClickListener() {
		public void onClick(View v) {
			mBrowser.cancelSelections();
		}
	};
	
	private OnClickListener deleteButtonOnClickListener = new OnClickListener() {
		public void onClick(View v) {
			mBrowser.deleteSelections();
		}
	};
	
	private OnClickListener shareButtonOnClickListener = new OnClickListener() {
		public void onClick(View v) {
			mBrowser.shareSelections();
		}
	};
	
	private OnClickListener renameButtonOnClickListener = new OnClickListener() {
		public void onClick(View v) {
			mBrowser.renameSelection();
		}
	};
	
	private OnDragListener cancelButtonDragListener = new OnDragListener () {
		public boolean onDrag(View v, DragEvent event) {
			switch(event.getAction()) {
				case DragEvent.ACTION_DROP:
					selectedCancelButton.setHighlight(false);
					mBrowser.cancelSelections();
					break;
					
				case DragEvent.ACTION_DRAG_ENTERED:
					selectedCancelButton.setHighlight(true);
					break;
					
				case DragEvent.ACTION_DRAG_EXITED:
					selectedCancelButton.setHighlight(false);
					break;
			}
			
			return true;
		}
	};
	
	private OnDragListener deleteButtonDragListener = new OnDragListener () {
		public boolean onDrag(View v, DragEvent event) {
			switch(event.getAction()) {
				case DragEvent.ACTION_DROP:
					mBrowser.deleteSelections();
					selectedDeleteButton.setHighlight(false);
					break;
					
				case DragEvent.ACTION_DRAG_ENTERED:
					selectedDeleteButton.setHighlight(true);
					((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(VIBRATE_DURATION);
					break;
					
				case DragEvent.ACTION_DRAG_EXITED:
					selectedDeleteButton.setHighlight(false);
					break;
			}
			
			return true;
		}
	};
	
	private OnDragListener shareButtonDragListener = new OnDragListener() {
		public boolean onDrag(View v, DragEvent event) {
			switch(event.getAction()) {
				case DragEvent.ACTION_DROP:
					mBrowser.shareSelections();
					selectedShareButton.setHighlight(false);
					break;
					
				case DragEvent.ACTION_DRAG_ENTERED:
					selectedShareButton.setHighlight(true);
					break;
					
				case DragEvent.ACTION_DRAG_EXITED:
					selectedShareButton.setHighlight(false);
					break;
			}
			
			return true;
		}
	};
	
	private OnDragListener renameButtonDragListener = new OnDragListener () {
		public boolean onDrag(View v, DragEvent event) {
			switch(event.getAction()) {
				case DragEvent.ACTION_DROP:
					mBrowser.renameSelection();
					selectedRenameButton.setHighlight(false);
					break;
					
				case DragEvent.ACTION_DRAG_ENTERED:
					selectedRenameButton.setHighlight(true);
					break;
					
				case DragEvent.ACTION_DRAG_EXITED:
					selectedRenameButton.setHighlight(false);
					break;
			}
			
			return true;
		}
	};
	
	
	// defaultActionBar view references
	private ViewGroup defaultActionBar;
	private Button defaultNewNoteButton;
	private Button defaultNewFolderButton;
	
	private OnClickListener newNoteButtonOnClickListener = new OnClickListener() {
		public void onClick(View v) {
			final File targetDir = mBrowser.getSelectedFolder();
			
			// Find the default input string - unnamed note + the smallest unused natural number
			int i = 1;
			while (directoryContainsName(targetDir, "unnamed note " + Integer.toString(i) + ".note")) {
				i++;
			}
			final String defaultInput = "unnamed note " + Integer.toString(i);
			
			final NewItemFn onFinish = new NewItemFn() {
				@Override
				public void function(String s) {
					String newNoteName;
					if (directoryContainsName(targetDir, s+".note")) {
						int j = 1;
						while (directoryContainsName(targetDir, s + Integer.toString(j) + ".note")) {
							j++;
						}
						newNoteName = s + Integer.toString(j)+".note";
					} else {
						newNoteName = s + ".note";
					}
					mBrowser.createNewFile(newNoteName, false);
				}
			};
			
			DialogFragment d = new NewItemDialog("Create New Note", "Enter the name of the note you want to create.", defaultInput, "Create Note", "Cancel", onFinish);
			d.show(MainMenuActivity.this.getFragmentManager(), "New Note");
		}
	};
	
	private OnClickListener newFolderButtonOnClickListener = new OnClickListener() {
		public void onClick(View v) {
			final File targetDir = mBrowser.getSelectedFolder();
			int i = 1;
			while (directoryContainsName(targetDir, "unnamed folder " + Integer.toString(i))) {
				i++;
			}
			final String defaultInput = "unnamed folder " + Integer.toString(i);
			
			final NewItemFn onFinish = new NewItemFn() {
				@Override
				public void function(String s) {
					String newNoteName;
					if (directoryContainsName(targetDir, s)) {
						int j = 1;
						while (directoryContainsName(targetDir, s + Integer.toString(j))) {
							j++;
						}
						newNoteName = s + Integer.toString(j);
					} else {
						newNoteName = s;
					}
					mBrowser.createNewFile(newNoteName, true);
				}
			};
			
			DialogFragment d = new NewItemDialog("Create New Folder", "Enter the name of the folder you want to create.", defaultInput, "Create Folder", "Cancel", onFinish);
			d.show(MainMenuActivity.this.getFragmentManager(), "New Note");
		}
	};
	
	private boolean directoryContainsName (File dir, String name) {
		final String[] names = dir.list();
		
		if (names == null) { return false; }
		
		for (String s : names) {
			if (s.equals(name)) {
				return true;
			}
		}
		return false;
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
    	if (getSharedPreferences("freehand", Context.MODE_PRIVATE).contains("tutorialShown") == false) {
			startActivity(new Intent(getBaseContext(), TutorialActivity.class));
    	}
    	
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.organizer_layout);
        
        // Set up the defaultActionBar
        defaultActionBar = (ViewGroup) findViewById(R.id.defaultActionBar);
        
        defaultNewNoteButton = (Button) findViewById(R.id.newNoteButton);
        defaultNewNoteButton.setOnClickListener(newNoteButtonOnClickListener);
        
        defaultNewFolderButton = (Button) findViewById(R.id.newFolderButton);
        defaultNewFolderButton.setOnClickListener(newFolderButtonOnClickListener);
        
        // Set up the itemsSelectedActionBar
        itemsSelectedActionBar = (ViewGroup) findViewById(R.id.itemsSelectedActionBar);
        
        selectedCancelButton = (HighlightButton) findViewById(R.id.cancelButton);
        selectedCancelButton.setOnClickListener(cancelButtonOnClickListener);
        selectedCancelButton.setOnDragListener(cancelButtonDragListener);
        
        selectedShareButton = (HighlightButton) findViewById(R.id.shareButton);
        selectedShareButton.setOnClickListener(shareButtonOnClickListener);
        selectedShareButton.setOnDragListener(shareButtonDragListener);
        
        selectedDeleteButton = (HighlightButton) findViewById(R.id.deleteButton);
        selectedDeleteButton.setOnClickListener(deleteButtonOnClickListener);
        selectedDeleteButton.setOnDragListener(deleteButtonDragListener);
        
        selectedRenameButton = (HighlightButton) findViewById(R.id.renameButton);
        selectedRenameButton.setOnClickListener(renameButtonOnClickListener);
        selectedRenameButton.setOnDragListener(renameButtonDragListener);
        
        itemsSelectedActionBar.setVisibility(View.INVISIBLE);
        
        
        
        Button prefButton = (Button) findViewById(R.id.preferences);
        prefButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent prefActivity = new Intent(getBaseContext(), PrefActivity.class);
				startActivity(prefActivity);
			}
        });
        
        mBrowser = ((FolderBrowser) findViewById(R.id.scrollView));
        final File rootDirectory = Environment.getExternalStoragePublicDirectory("Freehand");
        mBrowser.setRootDirectory(rootDirectory);
        mBrowser.setMainMenuActivity(this);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	TutorialPrefs.setContext(this);
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	TutorialPrefs.clear();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        return true;
    }
    
    @Override
    public void onBackPressed() {
    	super.onBackPressed();
    	//TODO
    }
    
    public void setActionBar (int numSelections) {
    	if (numSelections == 0) {
    		itemsSelectedActionBar.setVisibility(View.GONE);
        	defaultActionBar.setVisibility(View.VISIBLE);
    	} else if (numSelections == 1) {
    		itemsSelectedActionBar.setVisibility(View.VISIBLE);
        	defaultActionBar.setVisibility(View.GONE);
        	selectedRenameButton.setVisibility(View.VISIBLE);
    	} else {
    		itemsSelectedActionBar.setVisibility(View.VISIBLE);
        	defaultActionBar.setVisibility(View.GONE);
        	selectedRenameButton.setVisibility(View.GONE);
    	}
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	isRunning = true;
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	isRunning = false;
    }
    
    public static boolean isRunning () {
    	return isRunning;
    }
}