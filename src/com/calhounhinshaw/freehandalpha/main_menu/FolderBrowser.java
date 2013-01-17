package com.calhounhinshaw.freehandalpha.main_menu;

import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.RelativeLayout;

public class FolderBrowser extends RelativeLayout {
	private static final float MIN_CHILD_WIDTH_DIP = 300;
	
	private final HorizontalScrollView mParentView;
	
	private int childWidth = -1;
	
	public FolderBrowser(Context context, HorizontalScrollView scrollView) {
		super(context);
		mParentView = scrollView;
	}
	
	public void updateViews (List<View> newViews) {
		if (childWidth == -1) {
			setChildWidth();
		}

		this.removeAllViews();
		
		for (int i = 0; i<newViews.size(); i++) {
			if (i == 0) {
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(childWidth, LayoutParams.MATCH_PARENT);
				params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				newViews.get(i).setLayoutParams(params);
			} else {
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(childWidth, LayoutParams.MATCH_PARENT);
				params.addRule(RelativeLayout.RIGHT_OF, this.getChildAt(i-1).getId());
				params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				newViews.get(i).setLayoutParams(params);
			}

			this.addView(newViews.get(i), this.getChildCount());
		}
	}
	
	
	
	
	
	private void setChildWidth () {
		final float parentWidthPx = mParentView.getWidth();
		
		if (parentWidthPx > 0) {
			
			final float scale = getResources().getDisplayMetrics().density;
			final float minChildWidthPx = MIN_CHILD_WIDTH_DIP * scale;
			
			
			float numFoldersToShow = parentWidthPx/minChildWidthPx;
			Log.d("PEN", "number of folders to show" + Float.toString(numFoldersToShow));
			childWidth = (int) (parentWidthPx/((int)numFoldersToShow));
		} else {
			childWidth = -1;
		}
		
	}
}