package com.freehand.organizer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.Button;

public class HighlightButton extends Button {
	private static final int ORANGE_HIGHLIGHT = 0xFFFFBB33;
	
	private boolean drawHighlight = false;
	
	private Rect highlightRect = new Rect();
	private final Paint highlightPaint = new Paint();
	
	public HighlightButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		highlightPaint.setAntiAlias(true);
		highlightPaint.setColor(ORANGE_HIGHLIGHT);
		highlightPaint.setStrokeWidth(6);
		highlightPaint.setStyle(Paint.Style.STROKE);
	}
	
	public void setHighlight (boolean newValue) {
		drawHighlight = newValue;
		invalidate();
	}
	
	@Override
	public void onDraw (Canvas c) {
		super.onDraw(c);
		if (drawHighlight == true) {
			c.drawRect(highlightRect, highlightPaint);
		}
	}
	
	@Override
	protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		highlightRect.set(3, 3, right-left-3, bottom-top-3);
	}
}