package com.freehand.editor.tool_bar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RadioButton;

/**
 * Because of the order Android dispatches touch events in, if you want to know the state of the button right before
 * it's checked state last changed you have to modify onTouchEvent. It gets called before setChecked, but the registered
 * OnTouchListener gets called after, so you need to extend RadioButton to do it. I hate Android sometimes.
 * @author cal
 *
 */
class PreviousStateAwareRadioButton extends RadioButton {
	private boolean previousStateChecked = false;
	
	public PreviousStateAwareRadioButton(Context context) {
		super(context);
	}
	
	public PreviousStateAwareRadioButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public boolean onTouchEvent (MotionEvent e) {
		previousStateChecked = isChecked();
		return super.onTouchEvent(e);
	}
	
	public boolean previousStateWasChecked () {
		return previousStateChecked;
	}
}