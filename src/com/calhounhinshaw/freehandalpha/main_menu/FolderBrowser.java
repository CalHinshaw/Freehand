package com.calhounhinshaw.freehandalpha.main_menu;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
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
		this.removeAllViews();
		for (int i = 0; i<newViews.size(); i++) {
			newViews.get(i).setId(i+1);
			
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
	
	@Override
	protected void dispatchDraw (Canvas canvas) {
		if (childWidth == -1) {
			setChildWidth();
		}
		
		super.dispatchDraw(canvas);
	}
	
	
	
	
	private void setChildWidth () {
		final float parentWidthPx = mParentView.getWidth();
		
		if (parentWidthPx > 0) {
			final float scale = getResources().getDisplayMetrics().density;
			final float minChildWidthPx = MIN_CHILD_WIDTH_DIP * scale;
			float numFoldersToShow = parentWidthPx/minChildWidthPx;
			childWidth = (int) (parentWidthPx/((int)numFoldersToShow));
			
			ArrayList<View> views = new ArrayList<View>(this.getChildCount());
			for (int i = 0; i < this.getChildCount(); i++) {
				views.add(this.getChildAt(i));
			}
			updateViews(views);
		} else {
			childWidth = -1;
		}
		
	}
}