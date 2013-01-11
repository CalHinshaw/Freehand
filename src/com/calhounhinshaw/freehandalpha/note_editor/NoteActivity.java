package com.calhounhinshaw.freehandalpha.note_editor;

import com.calhounhinshaw.freehandalpha.R;
import com.calhounhinshaw.freehandalpha.note_orginazion.INoteHierarchyItem;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class NoteActivity extends Activity implements NewPenRequestListener {
	private RelativeLayout mLayout;
	
	private NoteView mNoteView;
	private PenCreatorView mPenView;
	
	private RadioGroup mRadioGroup;
	private PenRadioButton penOne;
	private PenRadioButton penTwo;
	private PenRadioButton penThree;
	private PenRadioButton penFour;
	private PenRadioButton penFive;
	
	
	private Button undoButton;
	private Button redoButton;
	
	
	
	
	private OnClickListener eraseButtonListener = new OnClickListener () {
		public void onClick (View v) {
			mNoteView.onErase();
		}
	};
	
	private OnClickListener selectButtonListener = new OnClickListener () {
		public void onClick (View v) {
			mNoteView.onSelect();
		}
	};
	
	private OnClickListener undoButtonListener = new OnClickListener () {
		public void onClick (View v) {
			mNoteView.onUndo();
		}
	};
	
	private OnClickListener redoButtonListener = new OnClickListener () {
		public void onClick (View v) {
			mNoteView.onRedo();
		}
	};
	
	
	
	
	
	
	
	
	
	public void onCreate (Bundle savedInstanceState) {
		// "system" level stuff
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView (R.layout.note_activity);
		getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		
		// getting views from xml layout
		mLayout = (RelativeLayout) findViewById(R.id.note_activity_layout);
		mNoteView = (NoteView) findViewById(R.id.note);
		mRadioGroup = (RadioGroup) findViewById(R.id.penSelectorRadioGroup);
		
		RadioButton eraseButton = (RadioButton) findViewById(R.id.erase);
		eraseButton.setOnClickListener(eraseButtonListener);
		
		RadioButton selectButton = (RadioButton) findViewById(R.id.select);
		selectButton.setOnClickListener(selectButtonListener);
		
		undoButton = (Button) findViewById(R.id.undo);
		undoButton.setOnClickListener(undoButtonListener);
		
		redoButton = (Button) findViewById(R.id.redo);
		redoButton.setOnClickListener(redoButtonListener);
		
		
		// setting up the note view
		final Object oldData = getLastNonConfigurationInstance();
		if (oldData == null) {
			Bundle passedInfo = getIntent().getBundleExtra("note_hierarchy_item_bundle");
			if (passedInfo != null && passedInfo.get("note_hierarchy_item") != null) {
				mNoteView.openNote((INoteHierarchyItem) passedInfo.get("note_hierarchy_item"));
			}
		} else {
			mNoteView.openNote((Note) oldData);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		SharedPreferences mPrefs = getPreferences(MODE_PRIVATE);
		
		try {
			if (mPrefs.getBoolean("firstRun", true)) {
				setDefaultPrefs(mPrefs);
			}
			
			initPenButtons(mPrefs);
		} catch (Error e) {
			// error trapping should be improved
			setDefaultPrefs(mPrefs);
			initPenButtons(mPrefs);
		}

		if (penOne.isChecked()) {
			penOne.toggle();
		} else if (penTwo.isChecked()) {
			penTwo.toggle();
		} else if (penThree.isChecked()) {
			penThree.toggle();
		} else if (penFour.isChecked()) {
			penFour.toggle();
		} else if (penFive.isChecked()) {
			penFive.toggle();
		} else {
			penOne.toggle();
		}
	}
	

	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return mNoteView.getNote();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		mNoteView.saveNote();
		
		SharedPreferences mPrefs = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = mPrefs.edit();
		
		editor.clear();
		
		editor.putBoolean("firstRun", false);
		
		editor.putInt("PenOneColor", penOne.getColor());
		editor.putFloat("PenOneSize", penOne.getSize());
		
		editor.putInt("PenTwoColor", penTwo.getColor());
		editor.putFloat("PenTwoSize", penTwo.getSize());
		
		editor.putInt("PenThreeColor", penThree.getColor());
		editor.putFloat("PenThreeSize", penThree.getSize());
		
		editor.putInt("PenFourColor", penFour.getColor());
		editor.putFloat("PenFourSize", penFour.getSize());
		
		editor.putInt("PenFiveColor", penFive.getColor());
		editor.putFloat("PenFiveSize", penFive.getSize());
		
		editor.commit();
	}
	
	
	
	
	
	
	private void setDefaultPrefs(SharedPreferences mPrefs) {
		SharedPreferences.Editor editor = mPrefs.edit();
		
		editor.clear();
		
		editor.putInt("PenOneColor", Color.BLACK);
		editor.putFloat("PenOneSize", 6.5f);
		
		editor.putInt("PenTwoColor", Color.BLUE);
		editor.putFloat("PenTwoSize", 6.5f);
		
		editor.putInt("PenThreeColor", Color.RED);
		editor.putFloat("PenThreeSize", 6.5f);
		
		editor.putInt("PenFourColor", Color.GREEN);
		editor.putFloat("PenFourSize", 6.5f);
		
		editor.putInt("PenFiveColor", 0x70FFFF0A);
		editor.putFloat("PenFiveSize", 25.0f);
		
		editor.commit();
	}
	
	// Potential recursive problem here - if not an issue won't worry about it for now
	private void initPenButtons (SharedPreferences mPrefs) {
		
		penOne = (PenRadioButton) findViewById(R.id.pen1Button);
		penTwo = (PenRadioButton) findViewById(R.id.pen2Button);
		penThree = (PenRadioButton) findViewById(R.id.pen3Button);
		penFour = (PenRadioButton) findViewById(R.id.pen4Button);
		penFive = (PenRadioButton) findViewById(R.id.pen5Button);
		
		try {
			int tempColor;
			float tempSize;
			
			
			// Pen button one
			tempColor = mPrefs.getInt("PenOneColor", 0);
			tempSize = mPrefs.getFloat("PenOneSize", 0f);
			
			if (tempColor == 0 || tempSize == 0) {
				throw new Error();
			}
			
			penOne.setPen(tempColor, tempSize);
			penOne.setListeners(mNoteView, this);
			
			
			//Pen button two
			tempColor = mPrefs.getInt("PenTwoColor", 0);
			tempSize = mPrefs.getFloat("PenTwoSize", 0f);
			
			if (tempColor == 0 || tempSize == 0) {
				throw new Error();
			}
			
			penTwo.setPen(tempColor, tempSize);
			penTwo.setListeners(mNoteView, this);
			
			
			//Pen button three
			tempColor = mPrefs.getInt("PenThreeColor", 0);
			tempSize = mPrefs.getFloat("PenThreeSize", 0f);
			
			if (tempColor == 0 || tempSize == 0) {
				throw new Error();
			}
			
			penThree.setPen(tempColor, tempSize);
			penThree.setListeners(mNoteView, this);
			
			
			//Pen button four
			tempColor = mPrefs.getInt("PenFourColor", 0);
			tempSize = mPrefs.getFloat("PenFourSize", 0f);
			
			if (tempColor == 0 || tempSize == 0) {
				throw new Error();
			}
			
			penFour.setPen(tempColor, tempSize);
			penFour.setListeners(mNoteView, this);
			
			
			//Pen button five
			tempColor = mPrefs.getInt("PenFiveColor", 0);
			tempSize = mPrefs.getFloat("PenFiveSize", 0f);
			
			if (tempColor == 0 || tempSize == 0) {
				throw new Error();
			}
			
			penFive.setPen(tempColor, tempSize);
			penFive.setListeners(mNoteView, this);
			
		} catch (ClassCastException e) {
			throw e;
		}
	}

	public void requestNewPen(OnPenChangedListener listener, int startColor, float startSize) {
		if (mPenView != null && listener == mPenView.getListener()) {
			mLayout.removeView(mPenView);
			mPenView = null;
			return;
		}
		
		mLayout.removeView(mPenView);
		mPenView = null;
		
		int windowWidth = getWindowManager().getDefaultDisplay().getWidth();
		
		int buttonWidth = mRadioGroup.getWidth() / 7;
		int newPenViewWidth = mRadioGroup.getLeft() + 3*buttonWidth;
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(newPenViewWidth, (int) (newPenViewWidth * PenCreatorView.HEIGHT_SCALAR));
		params.addRule(RelativeLayout.BELOW, undoButton.getId());
		
		if (listener == penOne) {
			int leftMargin = 0;
			params.setMargins(leftMargin, 0, windowWidth-leftMargin-newPenViewWidth, 0);
		} else if (listener == penTwo) {
			int leftMargin = buttonWidth*1;
			params.setMargins(leftMargin, 0, windowWidth-leftMargin-newPenViewWidth, 0);
		} else if (listener == penThree) {
			int leftMargin = buttonWidth*2;
			params.setMargins(leftMargin, 0, windowWidth-leftMargin-newPenViewWidth, 0);
		} else if (listener == penFour) {
			int leftMargin = buttonWidth*3;
			params.setMargins(leftMargin, 0, windowWidth-leftMargin-newPenViewWidth, 0);
		} else if (listener == penFive) {
			int leftMargin = buttonWidth*4;
			params.setMargins(leftMargin, 0, windowWidth-leftMargin-newPenViewWidth, 0);
		} else {
			Log.d("PEN", "requestNewPen's listener must be one of the pen buttons. it is not. you fucked up.");
		}
		
		mPenView = new PenCreatorView(this, listener, startColor, startSize);
		
		mPenView.setLayoutParams(params);
		
		mLayout.addView(mPenView);
	}
	
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent e) {
		if (mPenView != null && e.getAction() == MotionEvent.ACTION_DOWN) {
			PenRadioButton button = (PenRadioButton) mPenView.getListener();
			Rect buttonRect = new Rect();
			button.getGlobalVisibleRect(buttonRect);
			
			Rect newPenCreatorRect = new Rect();
			
			mPenView.getGlobalVisibleRect(newPenCreatorRect);
			
			if (!newPenCreatorRect.contains((int) e.getX(), (int) e.getY()) && !buttonRect.contains((int) e.getX(), (int) e.getY())) {
				mLayout.removeView(mPenView);
				mPenView = null;
			}
		}
		
		Rect activitySize = new Rect();
		getWindow().getDecorView().getWindowVisibleDisplayFrame(activitySize);
		e.offsetLocation(0, -activitySize.top);
		
		return mLayout.dispatchTouchEvent(e);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu (Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.note_activity_menu, menu);
		return true;
	}
	
	
	@Override
	public boolean onOptionsItemSelected (MenuItem item) {
		switch (item.getItemId()) {
		case R.id.saveItem:
			mNoteView.saveNote();
			return true;
			
		case R.id.metadataItem:
			final Dialog dialog = new Dialog(NoteActivity.this);
	   		 dialog.setContentView(R.layout.new_note_name_dialog);
	   		 dialog.setTitle("Title");

	   		 Button button = (Button) dialog.findViewById(R.id.submit_new_note_name);
	   		 button.setOnClickListener(new OnClickListener() {
	   		     public void onClick(View v) {
	   		    	 EditText edit=(EditText)dialog.findViewById(R.id.new_note_name);
	   		         String text=edit.getText().toString();

	   		         dialog.dismiss();
	   		         mNoteView.changeMetadata(text + ".note");
	   		         mNoteView.saveNote();
	   		     }
	   		 });   
	   		 dialog.show();
	   		 return true;
	   		 
	   	default:
	   		Toast.makeText(this, "Coming Soon!", Toast.LENGTH_LONG).show();
	   		return super.onOptionsItemSelected(item);
		}
	}
}