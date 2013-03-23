package com.calhounhinshaw.freehandalpha.note_editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

class Stroke {
	private final List<Point> mPoly;
	
	private final Paint mPaint = new Paint();
	private final Path mPath = new Path();

	/**
	 * Creates a new immutable Stroke object from the List<Point> object argument. polygon must have at least three points. If it doesn't
	 * this class' behavior will be unpredictable (and isn't my problem).
	 * 
	 * @param color The polygon's color
	 * @param polygon The points that define the polygon.
	 */
	public Stroke (int color, ArrayList<Point> polygon) {
		
		mPoly = Collections.unmodifiableList(polygon);
		
		mPaint.setColor(color);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeWidth(0);
		mPaint.setAntiAlias(true);
		
		mPath.moveTo(mPoly.get(0).x, mPoly.get(0).y);
		for (int i = 1; i < mPoly.size(); i++) {
			mPath.lineTo(mPoly.get(i).x, mPoly.get(i).y);
		}
		mPath.lineTo(mPoly.get(0).x, mPoly.get(0).y);
	}
	
	public void draw (Canvas c) {
		c.drawPath(mPath, mPaint);
	}
}