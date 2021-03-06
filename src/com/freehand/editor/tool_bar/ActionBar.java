package com.freehand.editor.tool_bar;

import java.util.ArrayList;
import java.util.List;

import com.calhounroberthinshaw.freehand.R;
import com.freehand.editor.canvas.Note;
import com.freehand.share.ShareDialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.Toast;

public class ActionBar extends LinearLayout {
	private float density;
	
	private IActionBarListener mListener = null;
	private Note mNote;

	private final int buttonWidth = (int) (40 * getResources().getDisplayMetrics().density);
	private final int buttonMargin = (int) (4 * getResources().getDisplayMetrics().density);
	
	private final Button undoButton = new Button(this.getContext());
	private final Button redoButton = new Button(this.getContext());
	private final PreviousStateAwareRadioButton eraserButton = new PreviousStateAwareRadioButton(this.getContext());
	private final PreviousStateAwareRadioButton selectButton = new PreviousStateAwareRadioButton(this.getContext());
	private final PenRadioButton[] penButtons = new PenRadioButton[8];
	private final View menuButtonSpacer = new View(this.getContext());
	private final Button menuButton = new Button(this.getContext());
	
	private final AnchorWindow mEraseMenuWindow;
	private float eraserSize = 6.0f;
	
	private final PopupWindow mMenuWindow;
	private final LinearLayout menuLayout = new LinearLayout(this.getContext());
	private final Button saveButton = new Button(new ContextThemeWrapper(getContext(), R.style.TextButton));
	private final Button shareButton = new Button(new ContextThemeWrapper(getContext(), R.style.TextButton));
	private final Button renameButton = new Button(new ContextThemeWrapper(getContext(), R.style.TextButton));
	
	
	
	private final CompoundButton.OnCheckedChangeListener eraseButtonCheckListener = new CompoundButton.OnCheckedChangeListener() {
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (isChecked == true) {
				mListener.setTool(IActionBarListener.Tool.STROKE_ERASER, eraserSize, 0);
			}
		}
	};
	
	private final View.OnClickListener eraseButtonClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			if (mEraseMenuWindow.lastClosedByAnchorTouch() == true) {
				return;
			}
			
			if (eraserButton.previousStateWasChecked()) {
				Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
				vibrator.vibrate(40);
				mEraseMenuWindow.show();
			}
		}
	};
	
	private final OnLongClickListener eraseButtonLongClickListener = new OnLongClickListener () {
		public boolean onLongClick(View v) {
			eraserButton.setChecked(true);
			Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
			vibrator.vibrate(40);
			mEraseMenuWindow.show();
			
			return true;
		}
	};
	
	private final CompoundButton.OnCheckedChangeListener selectButtonListener = new CompoundButton.OnCheckedChangeListener() {
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (isChecked == true) {
				mListener.setTool(IActionBarListener.Tool.STROKE_SELECTOR, 0, 0);
			}
		}
	};
	
	private final OnClickListener undoButtonListener = new OnClickListener () {
		public void onClick (View v) {
			mListener.undo();
		}
	};
	
	private final OnClickListener redoButtonListener = new OnClickListener () {
		public void onClick (View v) {
			mListener.redo();
		}
	};
	
	private final OnClickListener menuButtonListener = new OnClickListener () {
		public void onClick (View v) {
			mMenuWindow.showAtLocation(ActionBar.this, Gravity.TOP + Gravity.RIGHT, 0, ActionBar.this.getHeight() + 2 * buttonMargin);
		}
	};
	
	private final OnClickListener saveButtonListener = new OnClickListener () {
		public void onClick(View v) {
			if (mNote.save() == true) {
				Toast.makeText(getContext(), "Save successful!", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getContext(), "Save failed, please try again.", Toast.LENGTH_LONG).show();
			}
			mMenuWindow.dismiss();
		}
	};
	
	private final OnClickListener shareButtonListener = new OnClickListener () {
		public void onClick(View v) {
			mNote.save();
			List<Object> toShare = new ArrayList<Object>(1);
			toShare.add(mNote.getPath());
			
			if (getContext() instanceof Activity) {
				new ShareDialog(getContext(), new ArrayList<Object>(toShare)).show(((Activity) getContext()).getFragmentManager(), "share");
			}
			
			mMenuWindow.dismiss();
		}
	};
	
	private final OnClickListener renameButtonListener = new OnClickListener () {
		public void onClick(View v) {
			final Dialog dialog = new Dialog(getContext());
	   		 dialog.setContentView(R.layout.new_note_name_dialog);
	   		 dialog.setTitle("Rename Note");

	   		 Button button = (Button) dialog.findViewById(R.id.submit_new_note_name);
	   		 button.setOnClickListener(new OnClickListener() {
	   		     public void onClick(View v) {
	   		    	 EditText edit = (EditText)dialog.findViewById(R.id.new_note_name);
	   		    	 String text = edit.getText().toString();
	   		    	 dialog.dismiss();
	   		    	 mNote.rename(text);
	   		    }
	   		});   
	   		dialog.show();
	   		mMenuWindow.dismiss();
		}
	};
	
	
	
	
	
	
	
	@SuppressWarnings("deprecation")
	public ActionBar(Context context, AttributeSet attributes) {
		super(context, attributes);
		
		density = this.getContext().getResources().getDisplayMetrics().density;
		
		initBarButtons();
		initMenuButtons();
		
		LinearLayout eraseMenu = (LinearLayout) inflate(getContext(), R.layout.eraser_menu, null);
		mEraseMenuWindow = new AnchorWindow(eraserButton, eraseMenu, (int) (320 * getResources().getDisplayMetrics().density), LayoutParams.WRAP_CONTENT);
		
		final SizeSliderView eraseSizeSlider = (SizeSliderView) eraseMenu.findViewById(R.id.eraser_size_slider);
		eraseSizeSlider.setActionBarListener(mListener);
		
		mEraseMenuWindow.setDismissListener(new PopupWindow.OnDismissListener() {
			public void onDismiss() {
				eraserSize = eraseSizeSlider.getSize();
				mListener.setTool(IActionBarListener.Tool.STROKE_ERASER, eraserSize, 0);
			}
		});
		
		mMenuWindow = new PopupWindow(menuLayout, LayoutParams.WRAP_CONTENT,  LayoutParams.WRAP_CONTENT);
		mMenuWindow.setOutsideTouchable(true);
		mMenuWindow.setBackgroundDrawable(new BitmapDrawable());	// PopupWindow doesn't close on outside touch without background...
	}
	
	private void initBarButtons () {
		final LinearLayout.LayoutParams llButtonParams = new LinearLayout.LayoutParams(buttonWidth, buttonWidth);
		llButtonParams.setMargins(0, 0, buttonMargin, 0);
		
		undoButton.setBackgroundResource(R.drawable.undo_button_selector);
		undoButton.setPadding(0, 0, 0, 0);
		undoButton.setLayoutParams(llButtonParams);
		this.addView(undoButton);
		
		redoButton.setBackgroundResource(R.drawable.redo_button_selector);
		redoButton.setPadding(0, 0, 0, 0);
		redoButton.setLayoutParams(llButtonParams);
		this.addView(redoButton);
		
		
		// Tool radio group
		LayoutParams radioGroupParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		radioGroupParams.setMargins(0, 0, 0, 0);
		
		final RadioGroup toolRadioGroup = new RadioGroup(this.getContext());
		toolRadioGroup.setOrientation(RadioGroup.HORIZONTAL);
		toolRadioGroup.setLayoutParams(radioGroupParams);
		toolRadioGroup.setPadding(0, 0, 0, 0);
		
		final RadioGroup.LayoutParams rgButtonParams = new RadioGroup.LayoutParams(buttonWidth, buttonWidth);
		rgButtonParams.setMargins(0, 0, buttonMargin, 0);
		
		selectButton.setButtonDrawable(R.drawable.select_button_selector);
		selectButton.setLayoutParams(rgButtonParams);
		toolRadioGroup.addView(selectButton);
		
		eraserButton.setButtonDrawable(R.drawable.erase_button_selector);
		eraserButton.setLayoutParams(rgButtonParams);
		toolRadioGroup.addView(eraserButton);
		
		for (int i = 0; i < 8; i++) {
			penButtons[i] = new PenRadioButton(this.getContext());
			penButtons[i].setLayoutParams(rgButtonParams);
			toolRadioGroup.addView(penButtons[i]);
		}
		
		this.addView(toolRadioGroup);
		
		
		menuButtonSpacer.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f));
		this.addView(menuButtonSpacer);
		
		menuButton.setBackgroundResource(R.drawable.ic_action_overflow);
		menuButton.setPadding(0, 0, 0, 0);
		menuButton.setLayoutParams(llButtonParams);
		menuButton.setOnClickListener(menuButtonListener);
		this.addView(menuButton);
	}
	
	private void initMenuButtons () {
		final int dividerHeight = (int) (2.0f*density);
		
		menuLayout.setOrientation(LinearLayout.VERTICAL);
		menuLayout.setBackgroundColor(Color.DKGRAY);
		menuLayout.setPadding(dividerHeight, 0, dividerHeight, 0);
		
		
		final PaintDrawable divider = new PaintDrawable(Color.WHITE);
		divider.setIntrinsicHeight(dividerHeight);
		menuLayout.setDividerDrawable(divider);
		menuLayout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
		
		saveButton.setText("Save");
		saveButton.setTextSize(20);
		saveButton.setTextColor(this.getResources().getColorStateList(R.color.text_button_color));
		saveButton.setGravity(Gravity.LEFT);
		saveButton.setPadding((int) (10.0f*density), (int) (2.0f*density), (int) (2.0f*density), (int) (10.0f*density));
		saveButton.setBackgroundColor(this.getResources().getColor(R.color.clear));
		menuLayout.addView(saveButton);
		
		shareButton.setText("Share");
		shareButton.setTextSize(20);
		shareButton.setTextColor(this.getResources().getColorStateList(R.color.text_button_color));
		shareButton.setGravity(Gravity.LEFT);
		shareButton.setPadding((int) (10.0f*density), (int) (2.0f*density), (int) (2.0f*density), (int) (10.0f*density));
		shareButton.setBackgroundColor(this.getResources().getColor(R.color.clear));
		menuLayout.addView(shareButton);
		
		renameButton.setText("Rename");
		renameButton.setTextSize(20);
		renameButton.setTextColor(this.getResources().getColorStateList(R.color.text_button_color));
		renameButton.setGravity(Gravity.LEFT);
		renameButton.setPadding((int) (10.0f*density), (int) (2.0f*density), (int) (2.0f*density), (int) (10.0f*density));
		renameButton.setBackgroundColor(this.getResources().getColor(R.color.clear));
		menuLayout.addView(renameButton);
	}
	
	
	public void setActionBarListener (final IActionBarListener newListener) {
		mListener = newListener;
		
		undoButton.setOnClickListener(undoButtonListener);
		redoButton.setOnClickListener(redoButtonListener);
		
		eraserButton.setOnCheckedChangeListener(eraseButtonCheckListener);
		eraserButton.setOnClickListener(eraseButtonClickListener);
		eraserButton.setOnLongClickListener(eraseButtonLongClickListener);
		
		selectButton.setOnCheckedChangeListener(selectButtonListener);
		
		for (PenRadioButton b : penButtons) {
			b.setListener(newListener);
		}
	}
	
	public void setNote (Note newNote) {
		mNote = newNote;
		
		saveButton.setOnClickListener(saveButtonListener);
		shareButton.setOnClickListener(shareButtonListener);
		renameButton.setOnClickListener(renameButtonListener);
	}
	
	public void setPens (final int colors[], final float sizes[]) {
		for (int i = 0; i < penButtons.length; i++) {
			penButtons[i].setPen(colors[i], sizes[i]);
		}
	}
	
	public void getPens (final int colors[], final float sizes[]) {
		for (int i = 0; i < penButtons.length; i++) {
			colors[i] = penButtons[i].getColor();
			sizes[i] = penButtons[i].getSize();
		}
	}
	
	public void setEraserSize (final float size) {
		eraserSize = size;
	}
	
	public float getEraserSize () {
		return eraserSize;
	}
	
	public int getCheckedButton () {
		if (eraserButton.isChecked() == true) {
			return 0;
		} else if (selectButton.isChecked() == true) {
			return 1;
		} else {
			for (int i = 0; i < penButtons.length; i++) {
				if (penButtons[i].isChecked() == true) {
					return i + 2;
				}
			}
		}
		
		return 2;
	}
	
	public void setCheckedButton (final int checked) {
		if (checked == 0) {
			eraserButton.setChecked(true);
		} else if (checked == 1) {
			selectButton.setChecked(true);
		} else if (checked >= 2 && checked <= 6) {
			penButtons[checked-2].setChecked(true);
		} else {
			penButtons[0].setChecked(true);
		}
	}
	
	public void toggleHwMenu () {
		if (mMenuWindow.isShowing() == true) {
			mMenuWindow.dismiss();
		} else {
			mMenuWindow.showAtLocation(this, Gravity.BOTTOM + Gravity.CENTER_HORIZONTAL, 0, 0);
		}
	}
	
	public boolean hasOpenWindows () {
		if (mMenuWindow.isShowing()) {
			return true;
		}
		
		if (mEraseMenuWindow.isShowing()) {
			return true;
		}
		
		for (PenRadioButton b : penButtons) {
			if (b.getPenCreatorShowing()) {
				return true;
			}
		}
		
		return false;
	}
	
	public void closeWindows () {
		mMenuWindow.dismiss();
		mEraseMenuWindow.dismiss();
		
		for (PenRadioButton b : penButtons) {
			b.closePenCreatorWindow();
		}
	}
	
	
	@Override
	protected void onSizeChanged (int w, int h, int oldW, int oldH) {
		final boolean hwMenuKey = ViewConfiguration.get(this.getContext()).hasPermanentMenuKey();
		adjustShowingViews(calcNumPenButtons(w, hwMenuKey), hwMenuKey);
	}
	
	private int calcNumPenButtons (final int w, final boolean hwMenuKey) {
		final float buttonGroupWidth = w - 4*buttonWidth - 2*buttonMargin;
		final int numButtons = (int) (buttonGroupWidth/(buttonWidth+buttonMargin));
		return Math.min(8, numButtons - (hwMenuKey ? 0 : 1));
	}
	
	private void adjustShowingViews (final int numPens, final boolean hwMenuKey) {
		if (hwMenuKey == false) {
			menuButton.setVisibility(View.VISIBLE);
		} else {
			menuButton.setVisibility(View.GONE);
		}
		
		int i = 0;
		for (; i < numPens; i++) {
			penButtons[i].setVisibility(View.VISIBLE);
		}
		for (; i <5; i++) {
			penButtons[i].setVisibility(View.GONE);
		}
	}
}