package com.freehand.organizer;

import java.io.File;

import com.android.vending.billing.IabHelper;
import com.android.vending.billing.IabResult;
import com.android.vending.billing.Purchase;
import com.calhounroberthinshaw.freehand.R;

import com.freehand.billing.FreehandIabHelper;
import com.freehand.editor.canvas.Note;
import com.freehand.preferences.PrefActivity;
import com.freehand.tutorial.TutorialPrefs;

import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.DragEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

public class MainMenuActivity extends Activity {
	private static final long VIBRATE_DURATION = 50;
	
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
			if (!FreehandIabHelper.getProStatus(MainMenuActivity.this) && mBrowser.getNumNotes() >= 5) {
				showProDialog();
				return;
			}
			
			final File targetDir = mBrowser.getSelectedFolder();
			
			// Find the default input string - unnamed note + the smallest unused natural number
			int i = 1;
			while (directoryContainsName(targetDir, "unnamed note " + Integer.toString(i) + ".note")) {
				i++;
			}
			final String defaultInput = "unnamed note " + Integer.toString(i);
			
			final NewNoteDialog.onConfirmFn onConfirm = new NewNoteDialog.onConfirmFn () {
				@Override
				public void function(String s, Note.PaperType t) {
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
					mBrowser.createNewNote(newNoteName, t);
				}
			};
			
			DialogFragment d = new NewNoteDialog(defaultInput, onConfirm);
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
			
			final TextInputDialog.onConfirmFn onFinish = new TextInputDialog.onConfirmFn () {
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
					mBrowser.createNewFolder(newNoteName);
				}
			};
			
			DialogFragment d = new TextInputDialog("Create New Folder", "Enter the name of the folder you want to create.", defaultInput, "Create Folder", "Cancel", onFinish);
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
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.organizer_layout);
        
        FreehandIabHelper.updatePurchases(this);
        
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
        
        showFreeTrialDialog();
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
    protected void onActivityResult (final int requestCode, final int resultCode, final Intent data) {
    	if (FreehandIabHelper.handleActivityResult(requestCode, resultCode, data) == false) {
    		Log.d("PEN", "purchase NOT handled");
    		super.onActivityResult(requestCode, resultCode, data);
    	}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        return true;
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
    
	private void showProDialog () {
		final DialogInterface.OnClickListener getProListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
		    	final IabHelper.OnIabPurchaseFinishedListener listener = new IabHelper.OnIabPurchaseFinishedListener() {
					@Override
					public void onIabPurchaseFinished(IabResult result, Purchase info) {
						if (FreehandIabHelper.getProStatus(MainMenuActivity.this) == true) {
							Toast.makeText(MainMenuActivity.this, "Thanks for buying Pro, your support really helps!", Toast.LENGTH_LONG).show();
							newNoteButtonOnClickListener.onClick(null);
						}
					}
				};
				
				FreehandIabHelper.buyPro(MainMenuActivity.this, listener);
			}
		};
		
		final AlertDialog.Builder b = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT);
		
		b.setTitle("Get Freehand Pro for 50% off!")
		 .setMessage("You've exceded the free trial's cap on the number of notes you can have. If you want to make a new note please " +
		 		"either delete one of your existing notes and try again or get Freehand Pro.\n\nFreehand pro removes the cap on the number " +
		 		"of notes you have, and guarantees access to all on-device features added in the future without any further purchases." +
		 		"\n\nIf you have any questions about Pro please email me at calhinshaw@gmail.com!")
		  .setPositiveButton("Get Pro!", getProListener)
		  .setNegativeButton("Cancel", null)
		  .create().show();
	}
    
    private void showFreeTrialDialog () {
    	final SharedPreferences prefs = getPreferences(MODE_PRIVATE);
    	
    	// If we're not displaying the dialog return.
    	try {
    		if (prefs.getBoolean("show_trial_info_dialog_2", true) == false) return;
    	} catch (ClassCastException e) { /* intentionally blank */ }
    	
    	if (FreehandIabHelper.getProStatus(this) == true) {
    		return;
    	}
    	
    	final String message = "Welcome to Freehand's free trial! It has all the functionality of the full version " +
    			"but caps the number of notes you can have at five. The Pro license, currently on sale for 50% off, removes the cap.\n" +
    			"More details about Pro are in the settings menu under \"About Pro\".\n\nPlease feel free to contact me at calhinshaw@gmail.com!";
    	
    	(new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT))
	        .setTitle("Welcome to Freehand!")
	        .setMessage(message)
	        .setPositiveButton("Close", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	               dialog.cancel();
	            }
	        }).setNegativeButton("Don't Show Again", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	                prefs.edit().putBoolean("show_trial_info_dialog_2", false).commit();
	            }
	        }).show();
    }
}