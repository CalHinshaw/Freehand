package com.freehand.organizer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.view.View.DragShadowBuilder;

public class NoteMovementDragShadowBuilder extends DragShadowBuilder {
	private final Integer numberOfFiles;
	private final Integer size;
	
	public NoteMovementDragShadowBuilder(int numberOfFiles, int size) {
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
		numberPaint.setColor(Color.BLACK);
		numberPaint.setAntiAlias(true);
		numberPaint.setTextSize((int)(size*.5));
		canvas.drawText(numberOfFiles.toString(), size/4, size/2, numberPaint);
	}
}