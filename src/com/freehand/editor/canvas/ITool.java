package com.freehand.editor.canvas;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.MotionEvent;

interface ITool {
	public boolean onMotionEvent(MotionEvent e);
	public RectF getDirtyRect();
	public void draw(Canvas c);
	
	public void undo();
	public void redo();
}