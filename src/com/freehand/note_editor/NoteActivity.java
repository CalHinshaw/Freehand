package com.freehand.note_editor;

import java.util.ArrayList;
import com.calhounroberthinshaw.freehand.R;
import com.freehand.share.NoteSharer;
import com.freehand.share.ProgressUpdateFunction;
import com.freehand.storage.INoteHierarchyItem;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class NoteActivity extends Activity implements IViewOverlayHandler {
	private NoteEditorController mPresenter;
	
	private NoteView mNoteView;
	
	private PopupWindow popup = null;
	private Rect popupRect = new Rect();
	private View anchorView = null;
	
	private RadioGroup mRadioGroup;
	private RadioButton mEraseButton;
	private RadioButton mSelectButton;
	private ArrayList<PenRadioButton> penButtons = new ArrayList<PenRadioButton>(5);
	
	private Button undoButton;
	private Button redoButton;
	
	private CompoundButton.OnCheckedChangeListener eraseButtonListener = new CompoundButton.OnCheckedChangeListener() {
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (isChecked == true) {
				mPresenter.setTool(IActionBarListener.Tool.STROKE_ERASER, 6, 0);
			}
		}
	};
	
	private CompoundButton.OnCheckedChangeListener selectButtonListener = new CompoundButton.OnCheckedChangeListener() {
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (isChecked == true) {
				//mPresenter.setSelector();
			}
		}
	};
	
	private OnClickListener undoButtonListener = new OnClickListener () {
		public void onClick (View v) {
			mPresenter.undo();
		}
	};
	
	private OnClickListener redoButtonListener = new OnClickListener () {
		public void onClick (View v) {
			mPresenter.redo();
		}
	};
	
	
	public void onCreate (Bundle savedInstanceState) {
		// "system" level stuff
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView (R.layout.note_activity);
		getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		
		overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
		
		// getting views from xml layout
		mNoteView = (NoteView) findViewById(R.id.note);
		mRadioGroup = (RadioGroup) findViewById(R.id.penSelectorRadioGroup);
		
		mEraseButton = (RadioButton) findViewById(R.id.erase);
		mEraseButton.setOnCheckedChangeListener(eraseButtonListener);
		
		mSelectButton = (RadioButton) findViewById(R.id.select);
		mSelectButton.setOnCheckedChangeListener(selectButtonListener);
		
		undoButton = (Button) findViewById(R.id.undo);
		undoButton.setOnClickListener(undoButtonListener);
		
		redoButton = (Button) findViewById(R.id.redo);
		redoButton.setOnClickListener(redoButtonListener);
		
		mPresenter = new NoteEditorController();		
		mNoteView.setListener(mPresenter);
		
		// setting up the note view
		final Object oldData = getLastNonConfigurationInstance();
		if (oldData == null) {
			Parcelable noteItem = getIntent().getParcelableExtra("com.calhounhinshaw.freehandalpha.note_editor.INoteHierarchyItem");
			
//			if (noteItem != null) {
//				mPresenter.openNote((INoteHierarchyItem) noteItem);
//			}
		} else {
			mPresenter = ((NoteEditorController) oldData);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		SharedPreferences mPrefs = getPreferences(MODE_PRIVATE);
		
		if (initPenButtons(mPrefs) == false) {
			setDefaultPrefs(mPrefs);
			initPenButtons(mPrefs);
		}
		
		// Reselect the correct item from the radio group
		if (mEraseButton.isChecked()) {
			mEraseButton.setChecked(true);
		} else if (mSelectButton.isChecked()){
			mSelectButton.setChecked(true);
		} else {
			for (PenRadioButton button : penButtons) {
				if (button.isChecked()) {
					button.setChecked(true);
				}
			}
		}
		
		// If none of the radio buttons are selected check the first pen
		if (mRadioGroup.getCheckedRadioButtonId() == -1) {
			penButtons.get(0).setChecked(true);
		}
	}
	

	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return mPresenter;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		//mPresenter.saveNote();
		
		SharedPreferences mPrefs = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = mPrefs.edit();
		
		editor.clear();
		
		for (int i = 0; i < penButtons.size(); i++) {
			editor.putInt("Pen" + Integer.toString(i) + "Color", penButtons.get(i).getColor());
			editor.putFloat("Pen" + Integer.toString(i) + "Size", penButtons.get(i).getSize());
		}
		
		editor.commit();
	}
	
	private void setDefaultPrefs(SharedPreferences mPrefs) {
		Log.d("PEN", "Setting default preferences");
		
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
	
	private boolean initPenButtons (SharedPreferences mPrefs) {
		penButtons.clear();
		penButtons.add(0,(PenRadioButton) findViewById(R.id.pen1Button));
		penButtons.add(1,(PenRadioButton) findViewById(R.id.pen2Button));
		penButtons.add(2,(PenRadioButton) findViewById(R.id.pen3Button));
		penButtons.add(3,(PenRadioButton) findViewById(R.id.pen4Button));
		penButtons.add(4,(PenRadioButton) findViewById(R.id.pen5Button));
		
		try {
			int tempColor;
			float tempSize;
			
			for (int i = 0; i < penButtons.size(); i++) {
				tempColor = mPrefs.getInt("Pen" + Integer.toString(i) + "Color", 0);
				tempSize = mPrefs.getFloat("Pen" + Integer.toString(i) + "Size", 0f);
				
				if (tempColor == 0 || tempSize == 0) {
					Log.d("PEN", "pen SharedPreferences empty");
					return false;
				}
				
				penButtons.get(i).setListener(mPresenter);
				penButtons.get(i).setPen(tempColor, tempSize);
				penButtons.get(i).setOverlayHandler(this);
			}
			
			return true;
		} catch (ClassCastException e) {
			Log.d("PEN", "Something is wrong with one of the PenRadioButton's SharedPreferences entries.");
			return false;
		}
	}

//	public void requestNewPen (IPenChangedListener listener, int startColor, float startSize) {
//		if (mPenView != null && listener == mPenView.getListener()) {
//			mLayout.removeView(mPenView);
//			mPenView = null;
//			return;
//		}
//		
//		mLayout.removeView(mPenView);
//		mPenView = null;
//		
//		int windowWidth = getWindowManager().getDefaultDisplay().getWidth();
//		
//		int buttonWidth = mRadioGroup.getWidth() / 7;
//		int newPenViewWidth = mRadioGroup.getLeft() + 3*buttonWidth;
//		
//		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(newPenViewWidth, (int) (newPenViewWidth * PenCreatorView.HEIGHT_SCALAR));
//		params.addRule(RelativeLayout.BELOW, undoButton.getId());
//		
//		for (int i = 0; i < penButtons.size(); i++) {
//			if (listener == penButtons.get(i)) {
//				int leftMargin = i * buttonWidth;
//				params.setMargins(leftMargin, 0, windowWidth-leftMargin-newPenViewWidth, 0);
//				break;
//			}
//		}
//		
//		mPenView = new PenCreatorView(this, listener, startColor, startSize);
//		
//		mPenView.setLayoutParams(params);
//		
//		mLayout.addView(mPenView);
//	}
	
	public void setOverlayView(View newOverlayView, View anchor) {
		
		if (popup != null && anchorView != null && anchor == anchorView) {
			popup.dismiss();
			popup = null;
			anchorView = null;
			return;
		}
		
		if (popup == null) {
			popup = new PopupWindow(this);
		} else if (popup.isShowing()) {
			popup.dismiss();
		}
		
		anchorView = anchor;
		
		popup.setContentView(newOverlayView);
		popup.setWidth(500);
		popup.setHeight(1000);
		popup.showAsDropDown(anchor);
		
		newOverlayView.getGlobalVisibleRect(popupRect);
		
		
	}
	
	public Context getContextForView() {
		return this;
	}
	
	// Overriding this to close overlayViews when the users touches outside of them
	@Override
	public boolean dispatchTouchEvent(MotionEvent e) {
		if (popup != null && popup.isShowing() && e.getAction() == MotionEvent.ACTION_DOWN) {
			if (popupRect.contains((int) e.getX(), (int) e.getY()) == false) {
				popup.dismiss();
				
//				Rect noteRect = new Rect();
//				mNoteView.getGlobalVisibleRect(noteRect);
//				if (noteRect.contains((int) e.getX(), (int) e.getY())) {
//					return true;
//				}
			}
		}
		
		return super.dispatchTouchEvent(e);
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
			//mPresenter.saveNote();
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
	   		         //mPresenter.rename(text);
	   		     }
	   		 });   
	   		 dialog.show();
	   		 return true;
	   		 
	   		 //TODO sharing
	   		 
//		case R.id.shareItem:
//			ArrayList<INoteHierarchyItem> toShare = new ArrayList<INoteHierarchyItem>(1);
//			toShare.add(mNoteView.getNote().getHierarchyItem());
//
//			final ProgressDialog progressDialog = new ProgressDialog(this, ProgressDialog.THEME_HOLO_LIGHT);
//			progressDialog.setProgressNumberFormat(null);
//			progressDialog.setTitle("Preparing to Share");
//			progressDialog.setMessage("Large notes take longer to share, please be patient.");
//			progressDialog.setIndeterminate(false);
//			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//			
//			ProgressUpdateFunction updater = new ProgressUpdateFunction() {
//				@Override
//				public void updateProgress(int percentageComplete) {
//					if (percentageComplete > 100) {
//						progressDialog.dismiss();
//					} else {
//						if (progressDialog.isShowing() == false) {
//							progressDialog.show();
//						}
//						
//						progressDialog.setProgress(percentageComplete);
//					}
//					
//				}
//			};
//			
//			new NoteSharer(updater, this).execute(toShare);
//
//			return true;
//	   		 
	   	default:
	   		Toast.makeText(this, "Coming Soon!", Toast.LENGTH_LONG).show();
	   		return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onBackPressed() {
		if (popup != null) {
			popup.dismiss();
			return;
		}
		
		this.finish();
		overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
		super.onBackPressed();
	}


}