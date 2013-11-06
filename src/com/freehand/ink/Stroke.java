package com.freehand.ink;

import java.util.List;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

public class Stroke {
	private final List<Point> mPoly;
	
	private final Paint mPaint = new Paint();
	private final Path mPath = new Path();
	
	private RectF aabb = null;
	
	/**
	 * Creates a new immutable Stroke object from the WrapList<Point> object argument. Polygon must have at least three points. If it doesn't
	 * this class' behavior will be unpredictable (and isn't my problem).
	 * 
	 * @param color The polygon's color
	 * @param polygon The points that define the polygon.
	 */
	public Stroke (int color, List<Point> poly) {
		mPoly = poly;
		
		mPaint.setColor(color);
		mPaint.setStyle(Paint.Style.FILL);
		mPaint.setAntiAlias(true);
		
		mPath.setFillType(Path.FillType.WINDING);
		mPath.moveTo(mPoly.get(0).x, mPoly.get(0).y);
		for (int i = 1; i < mPoly.size(); i++) {
			mPath.lineTo(mPoly.get(i).x, mPoly.get(i).y);
		}
		mPath.lineTo(mPoly.get(0).x, mPoly.get(0).y);
	}
	
	public void draw (Canvas c) {
		c.drawPath(mPath, mPaint);
		
//		Paint paint = new Paint();
//		paint.setColor(0xA0FF0000);
//		
//		for (Point p : mPoly) {
//			c.drawCircle(p.x, p.y, 0.2f, paint);
//		}
	}
	
	/**
	 * @return this stroke's axis aligned bounding box
	 */
	public RectF getAABoundingBox () {
		if (aabb == null) {
			aabb = MiscPolyGeom.calcAABoundingBox(mPoly);
		}
		
		return aabb;
	}
	
	public List<Point> getPoly () {
		return mPoly;
	}
	
	public int getColor () {
		return mPaint.getColor();
	}
}