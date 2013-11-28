package com.freehand.editor.canvas;

import com.calhounroberthinshaw.freehand.R;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

class ZoomNotifier {
	private View mParentView;
	private PopupWindow mWindow;
	private TextView mView;
	
	public ZoomNotifier (final View parentView) {
		mParentView = parentView;
		LinearLayout layout = (LinearLayout) LayoutInflater.from(mParentView.getContext()).inflate(R.layout.pan_zoom_notifier_layout, null);
		mView = (TextView) layout.getChildAt(1);
		
		mWindow = new PopupWindow(mParentView.getContext());
		mWindow.setContentView(layout);
		mWindow.setWidth(LayoutParams.WRAP_CONTENT);
		mWindow.setHeight(LayoutParams.WRAP_CONTENT);
		mWindow.setBackgroundDrawable(new ColorDrawable(Color.LTGRAY));
	}
	
	public void update (final float newZoom) {
		mView.setText(Integer.toString((int) (newZoom*100.0)));
		mWindow.showAtLocation(mParentView, Gravity.CENTER_HORIZONTAL + Gravity.TOP, 0,
			(int) (mParentView.getTop() + 10*mParentView.getResources().getDisplayMetrics().density));
	}
}