package com.freehand.note_editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

class SizeSliderView extends View {
	private static final int BUFFER = 4;
	
	private Path trianglePath;
	private Paint trianglePaint;

	public SizeSliderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		Log.d("PEN", "created");
		
		trianglePaint = new Paint();
		trianglePaint.setColor(Color.RED);
		trianglePaint.setStyle(Paint.Style.FILL);
		trianglePaint.setAntiAlias(true);
	}
	
	@Override
	protected void onDraw (Canvas c) {
		Log.d("PEN", "drawing");
		if (trianglePath == null) {
			trianglePath = new Path();
			trianglePath.moveTo(getWidth()-BUFFER, getHeight()/2);
			trianglePath.lineTo(BUFFER, BUFFER);
			trianglePath.lineTo(BUFFER, getHeight()-BUFFER);
			trianglePath.lineTo(getWidth()-BUFFER, getHeight()/2);
		}
		
		c.drawPath(trianglePath, trianglePaint);
	}
}