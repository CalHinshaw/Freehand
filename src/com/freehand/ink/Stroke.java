package com.freehand.ink;

import com.freehand.misc.WrapList;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

public class Stroke {
	private final WrapList<Point> mPoly;
	
	private final Paint mPaint = new Paint();
	private final Path mPath = new Path();
	
	//private final Paint mDebugPaint = new Paint();

	/**
	 * Creates a new immutable Stroke object from the List<Point> object argument. polygon must have at least three points. If it doesn't
	 * this class' behavior will be unpredictable (and isn't my problem).
	 * 
	 * @param color The polygon's color
	 * @param polygon The points that define the polygon.
	 */
	public Stroke (int color, WrapList<Point> poly) {
		
		mPoly = poly;
		
		mPaint.setColor(color);
//		mPaint.setAlpha(0x50);
		mPaint.setStyle(Paint.Style.FILL);
		mPaint.setAntiAlias(true);
		
//		mDebugPaint.setColor(color);
//		mDebugPaint.setAlpha(0xFF);
//		mDebugPaint.setStyle(Paint.Style.STROKE);
//		mDebugPaint.setStrokeWidth(0);
//		mDebugPaint.setAntiAlias(true);
		
		mPath.setFillType(Path.FillType.WINDING);
		
		mPath.moveTo(mPoly.get(0).x, mPoly.get(0).y);
		for (int i = 1; i < mPoly.size(); i++) {
			mPath.lineTo(mPoly.get(i).x, mPoly.get(i).y);
		}
		mPath.lineTo(mPoly.get(0).x, mPoly.get(0).y);
	}
	
	public void draw (Canvas c) {
		c.drawPath(mPath, mPaint);
//		c.drawPath(mPath, mDebugPaint);
	}
}