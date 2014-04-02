package com.freehand.editor.tool_bar;

import com.calhounroberthinshaw.freehand.R;

import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.PopupWindow;

class PenCreator {
	private final AnchorWindow window;
	private final PenCreatorView penCreatorView;
	
	public PenCreator (final View anchor, final IPenChangedListener listener) {
		penCreatorView = (PenCreatorView) View.inflate(anchor.getContext(), R.layout.pen_creator_layout, null);
		penCreatorView.setListener(listener);
		
		window = new AnchorWindow(anchor, penCreatorView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	}
	
	public boolean lastClosedByAnchorTouch () {
		return window.lastClosedByAnchorTouch();
	}
	
	public void show (final int color, final float size) {
		window.show();
		penCreatorView.setPen(color, size);
	}
	
	public boolean isShowing() {
		return window.isShowing();
	}
	
	public void dismiss() {
		window.dismiss();
	}
	
	public void setDismissListener (final PopupWindow.OnDismissListener newListener) {
		window.setDismissListener(newListener);
	}
	
	public void setPen (final int color, final float size) {
		penCreatorView.setPen(color, size);
	}
}