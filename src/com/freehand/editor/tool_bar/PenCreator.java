package com.freehand.editor.tool_bar;

import com.calhounroberthinshaw.freehand.R;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.PopupWindow;

class PenCreator {
	private final AnchorWindow window;
	private final IPenChangedListener listener;
	private final View penCreatorView;
	
	public PenCreator (final View anchor, final IPenChangedListener listener) {
		this.listener = listener;
		penCreatorView = View.inflate(anchor.getContext(), R.layout.pen_creator_layout, null);
		window = new AnchorWindow(anchor, penCreatorView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	}
	
	public boolean lastClosedByAnchorTouch () {
		return window.lastClosedByAnchorTouch();
	}
	
	public void show () {
		window.show();
	}
	
	public boolean isShowing() {
		return window.isShowing();
	}
	
	public void dismiss() {
		Log.d("PEN", ""+penCreatorView.getWidth() + "   " + penCreatorView.getHeight());
		window.dismiss();
	}
	
	public void setDismissListener (final PopupWindow.OnDismissListener newListener) {
		window.setDismissListener(newListener);
	}
	
	public void setPen (final int color, final float size) {
		// TODO this stuff...
	}
}