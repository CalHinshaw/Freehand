package com.freehand.note_editor;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;

class ToolBar extends LinearLayout {
	private static final int BUTTON_SIZE = 40;
	private static final int TARGET_PADDING = 5;
	
	public ToolBar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public ToolBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@Override
	protected void onLayout (boolean changed, int left, int top, int right, int bot) {
		final int useableWidth = right-left-TARGET_PADDING;
		
		final int buttonsToShow = useableWidth / (BUTTON_SIZE+TARGET_PADDING);
		Log.d("PEN", Integer.toString(buttonsToShow));
	}
}