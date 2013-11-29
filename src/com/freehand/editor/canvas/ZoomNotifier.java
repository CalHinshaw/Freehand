package com.freehand.editor.canvas;

import java.util.Timer;
import java.util.TimerTask;

import com.calhounroberthinshaw.freehand.R;

import android.app.Activity;
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
	
	private final Timer hideTimer = new Timer(true);
	private TimerTask hideTask = null;
	
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
		final float density = mParentView.getResources().getDisplayMetrics().density;
		
		int[] location = new int[2];
		mParentView.getLocationOnScreen(location);
		
		mView.setText(Integer.toString((int) (newZoom*100.0)));
		mWindow.showAtLocation(mParentView, Gravity.CENTER_HORIZONTAL + Gravity.TOP, 0, location[1] + (int) (10*density));
		
		
		if (hideTask != null) {
			hideTask.cancel();
		}
		
		hideTask = new TimerTask() {
			@Override
			public void run() {
				if (mParentView.getContext() instanceof Activity) {
					((Activity) mParentView.getContext()).runOnUiThread(new Runnable() {
						public void run() {
							mWindow.dismiss();
						}
					});
				}
			}
		};
		
		hideTimer.schedule(hideTask, 2000);
	}
}