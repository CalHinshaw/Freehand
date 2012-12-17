package com.calhounhinshaw.freehandalpha.main_menu;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.view.View;
import android.view.View.DragShadowBuilder;

public class DirectoryViewDragShadowBuilder extends DragShadowBuilder {
	private final Integer numberOfFiles;
	private final Integer size;
	
	public DirectoryViewDragShadowBuilder(int numberOfFiles, int size) {
		this.numberOfFiles = numberOfFiles;
		this.size = size;
	}
	
	@Override
	public void onProvideShadowMetrics (Point shadowSize, Point touch) {
		shadowSize.x = size;
		shadowSize.y = size;
		
		touch.x = size - size/8;
		touch.y = size - size/8;
	}
	
	@Override
	public void onDrawShadow (Canvas canvas) {
		canvas.drawARGB(0x90, 0x88, 0x88, 0x88);
		
		Paint numberPaint = new Paint();
		
		canvas.drawText(numberOfFiles.toString(), 0, 0, numberPaint);
	}
}