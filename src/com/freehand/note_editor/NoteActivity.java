package com.freehand.note_editor;

import java.util.ArrayList;
import com.calhounroberthinshaw.freehand.R;
import com.freehand.share.NoteSharer;
import com.freehand.share.ProgressUpdateFunction;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class NoteActivity extends Activity {
	private NoteEditorController mPresenter;
	private ActionBar mActionBar;
	private NoteView mNoteView;
	
	private int checkedOnPause = -1;
	
	public void onCreate (Bundle savedInstanceState) {
		// "system" level stuff
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView (R.layout.note_activity);
		getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
		
		// Load the previous object on a runtime change
		@SuppressWarnings("deprecation")
		final Object saved = getLastNonConfigurationInstance();
		
		
		
		if (saved == null) {
			String notePath = getIntent().getStringExtra("note_path");
			Note note = new Note(notePath);
			mPresenter = new NoteEditorController(note, getPressureSensitivity());
			
		} else {
			final Object[] savedArr = (Object[]) saved;
			checkedOnPause = (Integer) savedArr[0];
			mPresenter = (NoteEditorController) savedArr[1];
		}
		
		// getting views from xml layout
		mNoteView = (NoteView) findViewById(R.id.note);
		mActionBar = (ActionBar) findViewById(R.id.actionBar);
		
		
		mNoteView.setListener(mPresenter);
		mNoteView.setUsingCapDrawing(getUsingCapDrawing());
		mPresenter.setNoteView(mNoteView);
		
		mActionBar.setActionBarListener(mPresenter);
	}
	
	private float getPressureSensitivity() {
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		String psString = mPrefs.getString("pressure_sensitivity", "40");
		int psInt = Integer.parseInt(psString);
		
		if (psInt < 0) {
			psInt = 0;
		} else if (psInt > 100) {
			psInt = 100;
		}
		
		float psFloat = psInt/100.0f;
		return psFloat;
	}
	
	private boolean getUsingCapDrawing () {
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		return mPrefs.getBoolean("capacitive_drawing", true);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		final SharedPreferences mPrefs = getPreferences(MODE_PRIVATE);
		final int[] colors = new int[5];
		final float[] sizes = new float[5];
		if (readPenPrefs(mPrefs, colors, sizes) == false) {
			initPenPrefs(mPrefs);
			readPenPrefs(mPrefs, colors, sizes);
		}
		mActionBar.setPens(colors, sizes);
		
		mActionBar.setCheckedButton(checkedOnPause);
	}
	

	
	@Override
	public Object onRetainNonConfigurationInstance() {
		mPresenter.setNoteView(null);
		final Object[] toRetain = {checkedOnPause, mPresenter};
		return toRetain;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mPresenter.saveNote();
		
		final int[] colors = new int[5];
		final float[] sizes = new float[5];
		mActionBar.getPens(colors, sizes);
		final SharedPreferences mPrefs = getPreferences(MODE_PRIVATE);
		savePenPrefs(mPrefs, colors, sizes);
		
		Log.d("PEN", "before:  " + Integer.toString(checkedOnPause));
		checkedOnPause = mActionBar.getCheckedButton();
		Log.d("PEN", "after:  " + Integer.toString(checkedOnPause));
	}
	
	private void initPenPrefs (SharedPreferences mPrefs) {
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.clear();
		
		editor.putInt("Pen0Color", Color.BLACK);
		editor.putFloat("Pen0Size", 6.5f);
		
		editor.putInt("Pen1Color", Color.BLUE);
		editor.putFloat("Pen1Size", 6.5f);
		
		editor.putInt("Pen2Color", Color.RED);
		editor.putFloat("Pen2Size", 6.5f);
		
		editor.putInt("Pen3Color", Color.GREEN);
		editor.putFloat("Pen3Size", 6.5f);
		
		editor.putInt("Pen4Color", 0x70FFFF0A);
		editor.putFloat("Pen4Size", 25.0f);
		
		editor.commit();
	}
	
	private boolean readPenPrefs (final SharedPreferences mPrefs, final int colors[], final float sizes[]) {
		try {
			for (int i = 0; i < colors.length; i++) {
				colors[i] = mPrefs.getInt("Pen" + Integer.toString(i) + "Color", 0);
				sizes[i] = mPrefs.getFloat("Pen" + Integer.toString(i) + "Size", 0f);
			}
		} catch (ClassCastException e) {
			Log.d("PEN", "Something is wrong with one of the PenRadioButton's SharedPreferences entries.");
			return false;
		}
		
		// Checking to make sure the preferences aren't empty
		for (int i = 0; i < colors.length; i++) {
			if (colors[i] != 0 || sizes[i] != 0) {
				return true;
			}
		}
		
		return false;
	}
	
	private void savePenPrefs (final SharedPreferences mPrefs, final int colors[], final float sizes[]) {
		final SharedPreferences.Editor editor = mPrefs.edit();
		editor.clear();
		
		for (int i = 0; i < colors.length; i++) {
			editor.putInt("Pen" + Integer.toString(i) + "Color", colors[i]);
			editor.putFloat("Pen" + Integer.toString(i) + "Size", sizes[i]);
		}
		
		editor.commit();
	}
	
	
	@Override
	public boolean onCreateOptionsMenu (Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.note_activity_menu, menu);
		return true;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean onOptionsItemSelected (MenuItem item) {
		switch (item.getItemId()) {
		case R.id.saveItem:
			mPresenter.saveNote();
			return true;
			
		case R.id.renameItem:
			final Dialog dialog = new Dialog(NoteActivity.this);
	   		 dialog.setContentView(R.layout.new_note_name_dialog);
	   		 dialog.setTitle("Title");

	   		 Button button = (Button) dialog.findViewById(R.id.submit_new_note_name);
	   		 button.setOnClickListener(new OnClickListener() {
	   		     public void onClick(View v) {
	   		    	 EditText edit=(EditText)dialog.findViewById(R.id.new_note_name);
	   		         String text=edit.getText().toString();

	   		         dialog.dismiss();
	   		         mPresenter.renameNote(text);
	   		     }
	   		 });   
	   		 dialog.show();
	   		 return true;
	   		 
		case R.id.shareItem:
			mPresenter.saveNote();
			ArrayList<String> toShare = new ArrayList<String>(1);
			toShare.add(mPresenter.getNote().getPath());

			final ProgressDialog progressDialog = new ProgressDialog(this, ProgressDialog.THEME_HOLO_LIGHT);
			progressDialog.setProgressNumberFormat(null);
			progressDialog.setTitle("Preparing to Share");
			progressDialog.setMessage("Large notes take longer to share, please be patient.");
			progressDialog.setIndeterminate(false);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			
			ProgressUpdateFunction updater = new ProgressUpdateFunction() {
				@Override
				public void updateProgress(int percentageComplete) {
					if (percentageComplete > 100) {
						progressDialog.dismiss();
					} else {
						if (progressDialog.isShowing() == false) {
							progressDialog.show();
						}
						
						progressDialog.setProgress(percentageComplete);
					}
					
				}
			};
			
			new NoteSharer(updater, this).execute(toShare);

			return true;
	   		 
	   	default:
	   		Toast.makeText(this, "Coming Soon!", Toast.LENGTH_LONG).show();
	   		return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onBackPressed() {
		this.finish();
		overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
		super.onBackPressed();
	}


}