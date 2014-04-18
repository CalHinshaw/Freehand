package com.freehand.organizer;

import java.io.File;

import com.android.vending.billing.IabHelper;
import com.android.vending.billing.IabResult;
import com.android.vending.billing.Inventory;
import com.android.vending.billing.Purchase;
import com.calhounroberthinshaw.freehand.R;

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

public class MainMenuActivity extends Activity {
	private static final long VIBRATE_DURATION = 50;
	private static boolean isRunning = false;
	
	private IabHelper iabHelper;
	private static final String SKU_PRO = "pro";
	private boolean hasProLisence = false;
	
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
        
        loadIAP();
        
        showLeavingBetaDialog();
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
    
    private void loadIAP () {
    	final String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwc8zJDwniUbdnqKiUtkMbrLomID1JaQ0cJLApsX7qbBGU" +
    			"+78bwUwZ7JlFtBF/ITxstrYXwiiFmB3HKbQaGdTWf2TOb7TK0ysitlcBzHBRYNvD6bsgxtaPwREwT1hql6z+cPrxISyXvRCsvf/T+u9gNGkEkkZeg1SMOL+" +
    			"ZhOViHCpzQbF6znNu10r771WDdOmv0Ta7W/Qi/icuKf8fKRWhgDh4AzVhmE2z34bGoAZEwkA7AxdedAgdnCe7R6MargdJS9EUh+5PqFsY5QhT115VUbBD2E" +
    			"W8kcqp6wlj3HuxGdfzevok7fNBQ8qaxSEeWGFCv8dYVLQ4WBnXqIIAA/OSQIDAQAB";
    	
    	iabHelper = new IabHelper(this, base64EncodedPublicKey);
    	
    	final IabHelper.QueryInventoryFinishedListener queryFinishedListener = new IabHelper.QueryInventoryFinishedListener() {
			@Override
			public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
				Log.d("PEN", "Query inventory finished.");
				
	            // Have we been disposed of in the meantime? If so, quit.
	            if (iabHelper == null) {
	            	Log.d("PEN", "Our IabHelper was disposed of before setup finished.");
	            	return;
	            }
	            
	            // Did it a fail? If so, quit.
	            if (result.isFailure()) {
	                Log.d("PEN", "Failed to query inventory: " + result);
	                return;
	            }
	            
	            Log.d("PEN", "Query inventory was successful.");
	            
	            Purchase proPurchase = inventory.getPurchase(SKU_PRO);
	            hasProLisence = (proPurchase != null);
	            Log.d("PEN", "User is " + (hasProLisence ? "PRO" : "NOT PRO"));
			}
		};
		
    	final IabHelper.OnIabSetupFinishedListener setupFinishedListener = new IabHelper.OnIabSetupFinishedListener() {
			@Override
			public void onIabSetupFinished(IabResult result) {
				Log.d("PEN", "IabHelper setup finished");
				
	            if (!result.isSuccess()) {
	                Log.d("PEN", "There was a problem setting up in-app billing: " + result);
	                return;
	            }
	
	            // Have we been disposed of in the meantime? If so, quit.
	            if (iabHelper == null) {
	            	Log.d("PEN", "Our IabHelper was disposed of before setup finished.");
	            	return;
	            }
	
	            // IAB is fully set up. Now, let's get an inventory of stuff we own.
	            Log.d("PEN", "Setup successful. Querying inventory.");
	            iabHelper.queryInventoryAsync(queryFinishedListener);
			}
    	};
    	
    	iabHelper.startSetup(setupFinishedListener);
    }
    
    public boolean getIsPro () {
    	return this.hasProLisence;
    }
    
    private void showLeavingBetaDialog () {
    	final SharedPreferences prefs = getPreferences(MODE_PRIVATE);
    	
    	// If we're not displaying the dialog return.
    	try {
    		if (prefs.getBoolean("show_leaving_beta_dialog", true) == false) return;
    	} catch (ClassCastException e) {
    		return;
    	}
    	
    	final String message = "Thanks to everyone who's given me feedback and reported bugs - Freehand wouldn't be where it is now without your support.\n\n" +
    			"On April 17, 2014 I'll be removing freehand from beta and adding a one time license purchase needed to create more than five notes " +
    			"(fewer than five notes is a free trial). The single license will unlock all of Freehand's local functionality forever - I will never use" +
    			" in app purchases to sell individual features or consumables. I might, however, add web services to Freehand that I will charge for, but " +
    			"they won't be necessary for or affect local usage and will never take away from the local functionality.\n\nMy day job has kept me from " +
    			"putting all of the time into Freehand I've wanted, so I'm hoping I can make enough off of freehand to be able to devote more time to it going " +
    			"forward. Some of the features your support will let me implement are ink smoothing, natural erasing, a paint bucket, text boxes, and images. " +
    			"Thanks again!\n\n-Cal Hinshaw";
    	
    	(new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT))
	        .setTitle("Freehand is Leaving Beta!")
	        .setMessage(message)
	        .setPositiveButton("Close", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	               dialog.cancel();
	            }
	        }).setNegativeButton("Don't Show Again", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	                prefs.edit().putBoolean("show_leaving_beta_dialog", false).commit();
	            }
	        }).show();
    }
}