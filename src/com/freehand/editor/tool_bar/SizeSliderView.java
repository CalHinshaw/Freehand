package com.freehand.editor.tool_bar;

import com.calhounroberthinshaw.freehand.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

class SizeSliderView extends View {
	private static final int BUFFER = 4;
	private static final float MAX_SIZE = 40.0f;
	private static final float MIN_SIZE = 0.1f;
	
	private float screenDensity = 1f;
	
	private Path trianglePath;
	private Paint trianglePaint;
	
	private float lastSliderX = 0f;
	private Paint sliderPaint;
	
	private IActionBarListener mListener;

	
	public SizeSliderView (final Context context) {
		super(context);
		init();
	}
	
	public SizeSliderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	private void init () {
		screenDensity = getContext().getResources().getDisplayMetrics().density;
		
		trianglePaint = new Paint();
		trianglePaint.setColor(getResources().getColor(R.color.ltgray));
		trianglePaint.setStyle(Paint.Style.FILL);
		trianglePaint.setAntiAlias(true);
		
		sliderPaint = new Paint();
		sliderPaint.setColor(Color.BLACK);
		sliderPaint.setStyle(Paint.Style.STROKE);
		sliderPaint.setStrokeWidth(2f*screenDensity);
		sliderPaint.setAntiAlias(true);
	}
	
	@Override
	protected void onDraw (Canvas c) {
		if (trianglePath == null) {
			trianglePath = new Path();
			trianglePath.moveTo(getWidth()-BUFFER, getHeight()/2);
			trianglePath.lineTo(BUFFER, BUFFER);
			trianglePath.lineTo(BUFFER, getHeight()-BUFFER);
			trianglePath.lineTo(getWidth()-BUFFER, getHeight()/2);
		}
		
		c.drawPath(trianglePath, trianglePaint);
		
		float sliderOffset = (BUFFER/2) * screenDensity;
		
		RectF sliderRect = new RectF(lastSliderX-sliderOffset, sliderOffset, lastSliderX+sliderOffset, getHeight()-sliderOffset);
		c.drawRoundRect(sliderRect, 2*screenDensity, 2*screenDensity, sliderPaint);
	}
	
	public void setActionBarListener (IActionBarListener newListener) {
		mListener = newListener;
	}
	
	@Override
	public boolean onTouchEvent (MotionEvent e) {
		lastSliderX = e.getX();
		
		if (lastSliderX < BUFFER) {
			lastSliderX = BUFFER;
		} else if (lastSliderX > getWidth()-BUFFER) {
			lastSliderX = getWidth()-BUFFER;
		}
		
		if (mListener != null) {
			mListener.setTool(IActionBarListener.Tool.STROKE_ERASER, getSize(), 0);
		}
		
		invalidate();
		return true;
	}
	
	public float getSize () {
		float size = (1 - (lastSliderX-BUFFER)/(getWidth()-2*BUFFER)) * MAX_SIZE;
		
		if (size < MIN_SIZE) {
			size = MIN_SIZE;
		}
		
		return size;
	}
	
	@Override
	protected void onSizeChanged (int w, int h, int oldW, int oldH) {
		lastSliderX = w/2;
		super.onSizeChanged(w, h, oldW, oldH);
	}
	
}