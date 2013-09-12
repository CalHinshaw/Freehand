package com.freehand.note_editor;

import com.calhounroberthinshaw.freehand.R;

import android.content.Context;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;

class ActionBar extends LinearLayout {
	private IActionBarListener mListener = null;
	
	private final AnchorWindow mEraseMenuWindow;

	private final int buttonWidth = (int) (40 * getResources().getDisplayMetrics().density);
	private final int buttonMargin = (int) (4 * getResources().getDisplayMetrics().density);
	
	private final Button undoButton;
	private final Button redoButton;
	private final PreviousStateAwareRadioButton eraserButton;
	private final PreviousStateAwareRadioButton selectButton;
	private final PenRadioButton[] penButtons = new PenRadioButton[5];
	private final View menuButtonSpacer;
	private final Button menuButton;
	
	private float eraserSize = 6.0f;
	
	
	
	
	private CompoundButton.OnCheckedChangeListener eraseButtonCheckListener = new CompoundButton.OnCheckedChangeListener() {
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (isChecked == true) {
				mListener.setTool(IActionBarListener.Tool.STROKE_ERASER, eraserSize, 0);
			}
		}
	};
	
	private View.OnClickListener eraseButtonClickListener = new View.OnClickListener() {
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
	
	private OnLongClickListener eraseButtonLongClickListener = new OnLongClickListener () {
		public boolean onLongClick(View v) {
			eraserButton.setChecked(true);
			Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
			vibrator.vibrate(40);
			mEraseMenuWindow.show();
			
			return true;
		}
	};
	
	private CompoundButton.OnCheckedChangeListener selectButtonListener = new CompoundButton.OnCheckedChangeListener() {
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (isChecked == true) {
				mListener.setTool(IActionBarListener.Tool.STROKE_SELECTOR, 0, 0);
			}
		}
	};
	
	private OnClickListener undoButtonListener = new OnClickListener () {
		public void onClick (View v) {
			mListener.undo();
		}
	};
	
	private OnClickListener redoButtonListener = new OnClickListener () {
		public void onClick (View v) {
			mListener.redo();
		}
	};
	
	
	
	
	
	
	
	
	
	public ActionBar(Context context, AttributeSet attributes) {
		super(context, attributes);
		
		// All of the child view declarations happen in here so i can make the member variables that reference them final.
		// Thanks Java...
		
		final LinearLayout.LayoutParams llButtonParams = new LinearLayout.LayoutParams(buttonWidth, buttonWidth);
		llButtonParams.setMargins(0, 0, buttonMargin, 0);
		
		undoButton = new Button(this.getContext());
		undoButton.setBackgroundResource(R.drawable.undo_button_selector);
		undoButton.setPadding(0, 0, 0, 0);
		undoButton.setLayoutParams(llButtonParams);
		this.addView(undoButton);
		
		redoButton = new Button(this.getContext());
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
		
		selectButton = new PreviousStateAwareRadioButton(this.getContext());
		selectButton.setButtonDrawable(R.drawable.select_button_selector);
		selectButton.setLayoutParams(rgButtonParams);
		toolRadioGroup.addView(selectButton);
		
		eraserButton = new PreviousStateAwareRadioButton(this.getContext());
		eraserButton.setButtonDrawable(R.drawable.erase_button_selector);
		eraserButton.setLayoutParams(rgButtonParams);
		toolRadioGroup.addView(eraserButton);
		
		for (int i = 0; i < 5; i++) {
			penButtons[i] = new PenRadioButton(this.getContext());
			penButtons[i].setLayoutParams(rgButtonParams);
			toolRadioGroup.addView(penButtons[i]);
		}
		
		this.addView(toolRadioGroup);
		
		
		menuButtonSpacer = new View(this.getContext());
		menuButtonSpacer.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f));
		this.addView(menuButtonSpacer);
		
		menuButton = new Button(this.getContext());
		menuButton.setBackgroundResource(R.drawable.settings_button_selector);
		menuButton.setPadding(0, 0, 0, 0);
		menuButton.setLayoutParams(llButtonParams);
		this.addView(menuButton);
		
		
		LinearLayout eraseMenu = (LinearLayout) inflate(getContext(), R.layout.eraser_menu, null);
		mEraseMenuWindow = new AnchorWindow(eraserButton, eraseMenu, 450, LayoutParams.WRAP_CONTENT);
		
		final SizeSliderView eraseSizeSlider = (SizeSliderView) eraseMenu.findViewById(R.id.eraser_size_slider);
		eraseSizeSlider.setActionBarListener(mListener);
		
		mEraseMenuWindow.setDismissListener(new PopupWindow.OnDismissListener() {
			public void onDismiss() {
				eraserSize = eraseSizeSlider.getSize();
				mListener.setTool(IActionBarListener.Tool.STROKE_ERASER, eraserSize, 0);
			}
		});
		
		penButtons[0].setChecked(true);
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
	
	
	@Override
	protected void onSizeChanged (int w, int h, int oldW, int oldH) {
		final boolean hwMenuKey = ViewConfiguration.get(this.getContext()).hasPermanentMenuKey();
		adjustShowingViews(calcNumPenButtons(w, hwMenuKey), hwMenuKey);
	}
	
	private int calcNumPenButtons (final int w, final boolean hwMenuKey) {
		final float layoutWidth = w * getResources().getDisplayMetrics().density - buttonMargin;
		final int numButtons = (int) (layoutWidth/buttonWidth);
		return Math.min(5, numButtons - (hwMenuKey ? 4 : 5));
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