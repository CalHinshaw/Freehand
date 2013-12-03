package com.freehand.editor;

import com.calhounroberthinshaw.freehand.R;
import com.freehand.editor.canvas.Note;
import com.freehand.editor.canvas.NoteView;
import com.freehand.editor.tool_bar.ActionBar;
import com.freehand.tutorial.TutorialPrefs;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;

public class NoteActivity extends Activity {
	private ActionBar mActionBar;
	private NoteView mNoteView;
	private Note mNote;
	
	private int checkedOnPause = -1;
	
	public void onCreate (Bundle savedInstanceState) {
		// "system" level stuff
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView (R.layout.note_activity);
		getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
		
		// getting views from xml layout
		mNoteView = (NoteView) findViewById(R.id.note);
		mActionBar = (ActionBar) findViewById(R.id.actionBar);
		
		// Load the previous object on a runtime change
		@SuppressWarnings("deprecation")
		final Object saved = getLastNonConfigurationInstance();
		
		if (saved == null) {
			String notePath = getIntent().getStringExtra("note_path");
			mNote = new Note(notePath);
		} else {
			final Object[] savedArr = (Object[]) saved;
			checkedOnPause = (Integer) savedArr[0];
			mNote = (Note) savedArr[1];
			mNoteView.setPos((float[]) savedArr[2]);
		}
		
		mNoteView.setNote(mNote);
		mNoteView.setUsingCapDrawing(getUsingCapDrawing());
		mNoteView.setPressureSensitivity(getPressureSensitivity());
		mNoteView.setZoomThreshold(getZoomThreshold());
		
		mActionBar.setActionBarListener(mNoteView);
		mActionBar.setNote(mNote);
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
	
	private float getZoomThreshold () {
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		String psString = mPrefs.getString("zoom_threshold", "20");
		int thresholdInt = Integer.parseInt(psString);
		
		if (thresholdInt < 0) {
			thresholdInt = 0;
		}
		
		return thresholdInt / 100.0f;
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
		
		TutorialPrefs.setContext(this);
	}
	

	
	@Override
	public Object onRetainNonConfigurationInstance() {
		final Object[] toRetain = {checkedOnPause, mNote, mNoteView.getPos()};
		return toRetain;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mNote.save();
		
		final int[] colors = new int[5];
		final float[] sizes = new float[5];
		mActionBar.getPens(colors, sizes);
		final SharedPreferences mPrefs = getPreferences(MODE_PRIVATE);
		savePenPrefs(mPrefs, colors, sizes);
		
		checkedOnPause = mActionBar.getCheckedButton();
		
		TutorialPrefs.clear();
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
		mActionBar.toggleHwMenu();
		return false;
	}
	
	@Override
	public void onBackPressed() {
		if (mActionBar.hasOpenWindows() == true) {
			mActionBar.closeWindows();
		} else {
			this.finish();
			overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
		}
	}
	
	@Override
	public void onStop () {
		mNoteView.finish();
		super.onStop();
	}
	
}