package com.freehand.editor.tool_bar;

import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.PopupWindow;

class AnchorWindow {
	private View mAnchor;
	private View mContent;
	
	private PopupWindow mWindow = new PopupWindow();
	
	private boolean closedByAnchorTouch = false;
	
	@SuppressWarnings("deprecation")
	public AnchorWindow (View anchorView, View contentView, int width, int height) {
		mAnchor = anchorView;
		mContent = contentView;
		
		// Set up out PopupWindow
		mWindow.setWidth(width);
		mWindow.setHeight(height);
		mWindow.setContentView(contentView);
		mWindow.setOutsideTouchable(true);
		mWindow.setBackgroundDrawable(new BitmapDrawable());
		
		mWindow.setTouchInterceptor(new OnTouchListener () {
			public boolean onTouch(View v, MotionEvent e) {
				
				// Calculate the bounding rectangle of the view we're anchoring to
				Rect anchorRect = new Rect();
				mAnchor.getGlobalVisibleRect(anchorRect);
				int offsetArr[] = new int[2];
				mContent.getLocationOnScreen(offsetArr);
				
				anchorRect.left -= offsetArr[0];
				anchorRect.right -= offsetArr[0];
				anchorRect.top -= offsetArr[1];
				anchorRect.bottom -= offsetArr[1];
				
				if (anchorRect.contains((int) e.getX(), (int) e.getY())) {
					closedByAnchorTouch = true;
				} else {
					closedByAnchorTouch = false;
				}

				return false;
			}
		});
	}
	
	public boolean lastClosedByAnchorTouch () {
		boolean toReturn = closedByAnchorTouch;
		closedByAnchorTouch = false;
		return toReturn;
	}
	
	public void show () {
		mWindow.showAsDropDown(mAnchor);
	}
	
	public void setDismissListener (PopupWindow.OnDismissListener newListener) {
		mWindow.setOnDismissListener(newListener);
	}
}