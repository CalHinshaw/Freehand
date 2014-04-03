package com.freehand.editor.tool_bar;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.Drawable;


public class AlphaPatternDrawable extends Drawable {
	
	private int mRectangleSize = 10;

	private Paint mPaintWhite = new Paint();
	private Paint mPaintGray = new Paint();

	private int numRectanglesHorizontal;
	private int numRectanglesVertical;

	private Bitmap mBitmap;
	
	int height = 0;
	int width = 0;
	
	public AlphaPatternDrawable(int rectangleSize) {
		mRectangleSize = rectangleSize;
		mPaintWhite.setColor(0xffffffff);
		mPaintGray.setColor(0xffcbcbcb);
	}

	@Override
	public void draw(Canvas canvas) {
		canvas.drawBitmap(mBitmap, null, getBounds(), null);
	}

	@Override
	public int getOpacity() {
		return 0;
	}

	@Override
	public void setAlpha(int alpha) {
		throw new UnsupportedOperationException("Alpha is not supported by this drawwable.");
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		throw new UnsupportedOperationException("ColorFilter is not supported by this drawwable.");
	}

	@Override
	protected void onBoundsChange(Rect bounds) {
		super.onBoundsChange(bounds);

		height = bounds.height();
		width = bounds.width();

		numRectanglesHorizontal = (int) Math.ceil((width / mRectangleSize));
		numRectanglesVertical = (int) Math.ceil(height / mRectangleSize);

		generatePatternBitmap();

	}
	
	/**
	 * Generate the checker board bitmap and cache it.
	 */
	private void generatePatternBitmap() {
		
		if(getBounds().width() <= 0 || getBounds().height() <= 0){
			return;
		}
		
		mBitmap = Bitmap.createBitmap(getBounds().width(), getBounds().height(), Config.ARGB_8888);			
		Canvas canvas = new Canvas(mBitmap);
		
		Rect r = new Rect();
		boolean verticalStartWhite = true;
		for (int i = 0; i <= numRectanglesVertical; i++) {

			boolean isWhite = verticalStartWhite;
			for (int j = 0; j <= numRectanglesHorizontal; j++) {

				r.top = i * mRectangleSize;
				r.left = j * mRectangleSize;
				r.bottom = r.top + mRectangleSize;
				r.right = r.left + mRectangleSize;
				
				canvas.drawRect(r, isWhite ? mPaintWhite : mPaintGray);

				isWhite = !isWhite;
			}

			verticalStartWhite = !verticalStartWhite;
		}
	}
	
	/**
	 * Untested - if it doesn't work you should fix it.
	 */
	public static Bitmap generatePatternBitmap(final int width, final int height, final int squareSize) {
		final Paint whitePaint = new Paint(0xffffffff);
		whitePaint.setAntiAlias(true);
		final Paint grayPaint = new Paint(0xffcbcbcb);
		grayPaint.setAntiAlias(true);
		
		
		final Bitmap b = Bitmap.createBitmap(width, height, Config.ARGB_8888);			
		final Canvas canvas = new Canvas(b);
		
		final int numHorizSquares = (int) Math.ceil((width / squareSize));
		final int numVertSquares = (int) Math.ceil(height / squareSize);
		
		final Rect r = new Rect();
		boolean verticalStartWhite = true;
		for (int i = 0; i <= numVertSquares; i++) {
			boolean isWhite = verticalStartWhite;
			for (int j = 0; j <= numHorizSquares; j++) {
				r.top = i * squareSize;
				r.left = j * squareSize;
				r.bottom = r.top + squareSize;
				r.right = r.left + squareSize;
				canvas.drawRect(r, isWhite ? whitePaint : grayPaint);

				isWhite = !isWhite;
			}

			verticalStartWhite = !verticalStartWhite;
		}
		
		return b;
	}
}