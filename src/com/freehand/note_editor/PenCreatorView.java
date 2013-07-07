package com.freehand.note_editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Shader.TileMode;
import android.view.MotionEvent;
import android.view.View;


class PenCreatorView extends View {
	
	public static final float HEIGHT_SCALAR = 1.144f;
	
    /**
	 * The width in pixels of the border 
	 * surrounding all color panels.
	 */
	private final static float	BORDER_WIDTH_PX = 1;

	private float 		PANEL_SPACING = 10f;	
	/**
	 * The radius in dp of the color palette tracker circle.
	 */
	private float 		PALETTE_CIRCLE_TRACKER_RADIUS = 5f;
	/**
	 * The dp which the tracker of the hue or alpha panel
	 * will extend outside of its bounds.
	 */
	private float		RECTANGLE_TRACKER_OFFSET = 2f;
	
	
	private float 		mDensity = 1f;
	
	private Paint 		mSatValPaint;
	private Paint		mSatValTrackerPaint;
	
	private Paint		mHuePaint;
	private Paint		mRectTrackerPaint;
	
	private Paint		mAlphaPaint;
	private Paint		mAlphaTextPaint;
	
	private Paint		mBorderPaint;
		
	private Shader		mValShader;
	private Shader		mSatShader;
	private Shader		mHueShader;
	private Shader		mAlphaShader;
	
	private int			mAlpha = 0xff;
	private float		mHue = 178f;
	private float 		mSat = 178f;
	private float 		mVal = 178f;
	private float		mSize = 4f;
	
	private String		mAlphaSliderText = "Alpha";	
	private int 		mSliderTrackerColor = 0xff1c1c1c;
	private int 		mBorderColor = 0xff6E6E6E;
	private boolean		mShowAlphaPanel = true;  
			
	private RectF	mSatValRect;
	private RectF 	mHueRect;
	private RectF	mAlphaRect;
	private RectF	mSizeRect;
	
	
	private int width;
	
	private AlphaPatternDrawable	mAlphaPattern;
	
	private Point	mStartTouchPoint = null;
	
	private IPenChangedListener mListener = null;
	
	public PenCreatorView(Context context, IPenChangedListener newListener, int newColor, float newSize) {
		super(context);
		init();
		
		mListener = newListener;
		this.setPen(newColor, newSize);
	}
		
	public void setOnPenChangedListener (IPenChangedListener temp) {
		mListener = temp;
	}
	
	private void init () {
		mDensity = getContext().getResources().getDisplayMetrics().density;
		PALETTE_CIRCLE_TRACKER_RADIUS *= mDensity;		
		RECTANGLE_TRACKER_OFFSET *= mDensity;
		PANEL_SPACING = PANEL_SPACING * mDensity;
		
		initPaintTools();
	}
	
	private void initPaintTools(){
		
		mSatValPaint = new Paint();
		mSatValTrackerPaint = new Paint();
		mHuePaint = new Paint();
		mRectTrackerPaint = new Paint();
		mAlphaPaint = new Paint();
		mAlphaTextPaint = new Paint();
		mBorderPaint = new Paint();
		
		
		mSatValTrackerPaint.setStyle(Style.STROKE);
		mSatValTrackerPaint.setStrokeWidth(2f * mDensity);
		mSatValTrackerPaint.setAntiAlias(true);
		
		mRectTrackerPaint.setColor(mSliderTrackerColor);
		mRectTrackerPaint.setStyle(Style.STROKE);
		mRectTrackerPaint.setStrokeWidth(2f * mDensity);
		mRectTrackerPaint.setAntiAlias(true);
		
		mAlphaTextPaint.setColor(0xff1c1c1c);
		mAlphaTextPaint.setTextSize(14f * mDensity);
		mAlphaTextPaint.setAntiAlias(true);
		mAlphaTextPaint.setTextAlign(Align.CENTER);
		mAlphaTextPaint.setFakeBoldText(true);
	
		
	}
	
	private int[] buildHueColorArray(){
		
		int[] hue = new int[361];
		
		int count = 0;
		for(int i = hue.length -1; i >= 0; i--, count++){
			hue[count] = Color.HSVToColor(new float[]{i, 1f, 1f});
		}
		
		return hue;
	}
	
	
	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawColor(Color.DKGRAY);
		
		
		drawSatValPanel(canvas);	
		drawHuePanel(canvas);
		drawAlphaPanel(canvas);
		drawSizePanel(canvas);
	}
	
	private void drawSatValPanel(Canvas canvas) {
		
		final RectF	rect = mSatValRect;
		
		if(BORDER_WIDTH_PX > 0){			
			mBorderPaint.setColor(mBorderColor);
			//canvas.drawRect(mDrawingRect.left, mDrawingRect.top, rect.right + BORDER_WIDTH_PX, rect.bottom + BORDER_WIDTH_PX, mBorderPaint);		
		}
			
		if (mValShader == null) {
			mValShader = new LinearGradient(rect.left, rect.top, rect.left, rect.bottom, 
					0xffffffff, 0xff000000, TileMode.CLAMP);
		}
		
		int rgb = Color.HSVToColor(new float[]{mHue,1f,1f});
	
		mSatShader = new LinearGradient(rect.left, rect.top, rect.right, rect.top, 
				0xffffffff, rgb, TileMode.CLAMP);
		ComposeShader mShader = new ComposeShader(mValShader, mSatShader, PorterDuff.Mode.MULTIPLY);
		mSatValPaint.setShader(mShader);
		
		canvas.drawRect(rect, mSatValPaint);
	
		Point p = satValToPoint(mSat, mVal);
			
		mSatValTrackerPaint.setColor(0xff000000);
		canvas.drawCircle(p.x, p.y, PALETTE_CIRCLE_TRACKER_RADIUS - 1f * mDensity, mSatValTrackerPaint);
				
		mSatValTrackerPaint.setColor(0xffdddddd);
		canvas.drawCircle(p.x, p.y, PALETTE_CIRCLE_TRACKER_RADIUS, mSatValTrackerPaint);
	}

	private void drawHuePanel(Canvas canvas){
	
		final RectF rect = mHueRect;
		
		if(BORDER_WIDTH_PX > 0){
			mBorderPaint.setColor(mBorderColor);
			canvas.drawRect(rect.left - BORDER_WIDTH_PX, 
					rect.top - BORDER_WIDTH_PX, 
					rect.right + BORDER_WIDTH_PX, 
					rect.bottom + BORDER_WIDTH_PX, 
					mBorderPaint);		
		}

		if (mHueShader == null) {
			mHueShader = new LinearGradient(rect.left, rect.top, rect.left, rect.bottom, buildHueColorArray(), null, TileMode.CLAMP);
			mHuePaint.setShader(mHueShader);
		}
	
		canvas.drawRect(rect, mHuePaint);
		
		float rectHeight = 4 * mDensity / 2;
				
		Point p = hueToPoint(mHue);
				
		RectF r = new RectF();
		r.left = rect.left - RECTANGLE_TRACKER_OFFSET;
		r.right = rect.right + RECTANGLE_TRACKER_OFFSET;
		r.top = p.y - rectHeight;
		r.bottom = p.y + rectHeight;
		
		
		canvas.drawRoundRect(r, 2, 2, mRectTrackerPaint);
		
	}
	
	private void drawAlphaPanel(Canvas canvas){
		
		if(!mShowAlphaPanel || mAlphaRect == null || mAlphaPattern == null) return;
		
		final RectF rect = mAlphaRect;
		
		if(BORDER_WIDTH_PX > 0){
			mBorderPaint.setColor(mBorderColor);
			canvas.drawRect(rect.left - BORDER_WIDTH_PX, 
					rect.top - BORDER_WIDTH_PX, 
					rect.right + BORDER_WIDTH_PX, 
					rect.bottom + BORDER_WIDTH_PX, 
					mBorderPaint);		
		}
		
		
		mAlphaPattern.draw(canvas);
		
		float[] hsv = new float[]{mHue,mSat,mVal};
		int color = Color.HSVToColor(hsv);
		int acolor = Color.HSVToColor(0, hsv);
		
		mAlphaShader = new LinearGradient(rect.left, rect.top, rect.right, rect.top, 
				color, acolor, TileMode.CLAMP);
		
		
		mAlphaPaint.setShader(mAlphaShader);
		
		canvas.drawRect(rect, mAlphaPaint);
		
		if(mAlphaSliderText != null && mAlphaSliderText!= ""){
			canvas.drawText(mAlphaSliderText, rect.centerX(), rect.centerY() + 4 * mDensity, mAlphaTextPaint);
		}
		
		float rectWidth = 4 * mDensity / 2;
		
		Point p = alphaToPoint(mAlpha);
				
		RectF r = new RectF();
		r.left = p.x - rectWidth;
		r.right = p.x + rectWidth;
		r.top = rect.top - RECTANGLE_TRACKER_OFFSET;
		r.bottom = rect.bottom + RECTANGLE_TRACKER_OFFSET;
		
		canvas.drawRoundRect(r, 2, 2, mRectTrackerPaint);
		
	}
	
	
	private void drawSizePanel(Canvas canvas) {
		float[] hsv = new float[]{mHue,mSat,mVal};
		int color = Color.HSVToColor(hsv);
		Paint sizePaint = new Paint();
		sizePaint.setAntiAlias(true);
		sizePaint.setColor(color);
		sizePaint.setStyle(Paint.Style.FILL);
		
		Path sizePath = new Path();
		sizePath.moveTo(mSizeRect.left, mSizeRect.top);
		sizePath.lineTo(mSizeRect.left, mSizeRect.bottom);
		sizePath.lineTo(mSizeRect.right, (mSizeRect.top + mSizeRect.bottom)/2);
		
		canvas.drawPath(sizePath, sizePaint);
		
		
		float rectWidth = 4 * mDensity / 2;
		
		Point p = sizeToPoint(mSize);
				
		RectF r = new RectF();
		r.left = p.x - rectWidth;
		r.right = p.x + rectWidth;
		r.top = mSizeRect.top - RECTANGLE_TRACKER_OFFSET;
		r.bottom = mSizeRect.bottom + RECTANGLE_TRACKER_OFFSET;
		
		canvas.drawRoundRect(r, 2, 2, mRectTrackerPaint);
	}
	

	
	
	private Point hueToPoint(float hue){
		
		final RectF rect = mHueRect;
		final float height = rect.height();
		
		Point p = new Point();
			
		p.y = (int) (height - (hue * height / 360f) + rect.top);
		p.x = (int) rect.left;
		
		return p;		
	}
	
	private Point satValToPoint(float sat, float val){
		
		final RectF rect = mSatValRect;
		final float height = rect.height();
		final float width = rect.width();
		
		Point p = new Point();
		
		p.x = (int) (sat * width + rect.left);
		p.y = (int) ((1f - val) * height + rect.top);
		
		return p;
	}
	
	private Point alphaToPoint(int alpha){
		
		final RectF rect = mAlphaRect;
		final float width = rect.width();
		
		Point p = new Point();
		
		p.x = (int) (width - (alpha * width / 0xff) + rect.left);
		p.y = (int) rect.top;
		
		return p;
	
	}
	
	private Point sizeToPoint(float size) {
		final RectF rect = mSizeRect;
		final float width = rect.width();
		
		Point p = new Point();
		
		p.x = (int) ((width*(rect.height()-size))/rect.height() + rect.left);
		p.y = (int) rect.top;
		
		return p;
	}
	
	private float[] pointToSatVal(float x, float y){
	
		final RectF rect = mSatValRect;
		float[] result = new float[2];
		
		float width = rect.width();
		float height = rect.height();
		
		if (x < rect.left) {
			x = 0f;
		} else if (x > rect.right) {
			x = width;
		} else {
			x = x - rect.left;
		}
				
		if (y < rect.top) {
			y = 0f;
		} else if (y > rect.bottom) {
			y = height;
		} else {
			y = y - rect.top;
		}
			
		result[0] = 1.f / width * x;
		result[1] = 1.f - (1.f / height * y);
		
		return result;	
	}
	
	private float pointToHue(float y){		
		if (y < mHueRect.top){
			y = 0f;
		} else if (y > mHueRect.bottom) {
			y = mHueRect.height();
		} else {
			y = y - mHueRect.top;
		}
		
		return 360f - (y * 360f / mHueRect.height());
	}
	
	private int pointToAlpha(int x){
		
		final RectF rect = mAlphaRect;
		final int width = (int) rect.width();
		
		if (x < rect.left) {
			x = 0;
		} else if (x > rect.right) {
			x = width;
		} else {
			x = x - (int)rect.left;
		}
		
		return 0xff - (x * 0xff / width);
	}
	
	private float pointToSize (float x) {
		x -= mSizeRect.left;
		
		if (x < 0) {
			x = 0;
		} else if (x > mSizeRect.width()) {
			x = mSizeRect.width();
		}
		
		float s = mSizeRect.height() - (x*mSizeRect.height() / mSizeRect.width());
		if (s < 0.1f) { s = 0.1f; }
		return s;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		boolean update = false;
				
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mStartTouchPoint = new Point((int)event.getX(), (int)event.getY());
			update = moveTrackersIfNeeded(event);
			break;		
		case MotionEvent.ACTION_MOVE:
			update = moveTrackersIfNeeded(event);
			break;
		case MotionEvent.ACTION_UP:	
			update = moveTrackersIfNeeded(event);
			mStartTouchPoint = null;
			break;
		}
		
		if(update){
			invalidate();
			if (mListener != null) {
				mListener.onPenChanged(Color.HSVToColor(mAlpha, new float[] {mHue, mSat, mVal}), mSize);
			}
		}
		
	
		return true;
	}
		
	private boolean moveTrackersIfNeeded(MotionEvent event){
		
		if(mStartTouchPoint == null) return false;
		
		boolean update = false;
		
		int startX = mStartTouchPoint.x;
		int startY = mStartTouchPoint.y;
		
		
		if(mHueRect.contains(startX, startY)){
			
			mHue = pointToHue(event.getY());
						
			update = true;
		}
		else if(mSatValRect.contains(startX, startY)){
			float[] result = pointToSatVal(event.getX(), event.getY());
			
			mSat = result[0];
			mVal = result[1];

			update = true;
		}
		else if(mAlphaRect != null && mAlphaRect.contains(startX, startY)){
			
			mAlpha = pointToAlpha((int)event.getX());
			
			update = true;
		} else if(mSizeRect != null && mSizeRect.contains(startX, startY)) {
			
			mSize = pointToSize(event.getX());
			
			update = true;
		}
		
		return update;
	}
	

	
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		
		width = w;
		
		setUpHueRect();
		setUpSatValRect();
		setUpAlphaRect();
		setUpSizeRect();
	}
	
	private void setUpSatValRect(){
		
		float left = 0.02f * width;
		float top = 0.02f * width;
		float bottom = 0.864f * width;
		float right = 0.864f * width;
		
		mSatValRect = new RectF(left,top, right, bottom);
	}
	
	private void setUpHueRect(){

		float left = 0.884f * width;
		float top = 0.02f * width;
		float bottom = 0.864f * width;
		float right = 0.980f * width;
		
		mHueRect = new RectF(left, top, right, bottom);
	}

	private void setUpAlphaRect(){
		
		float left = 0.02f * width;
		float top = 0.894f * width;
		float bottom = 0.994f * width;
		float right = 0.980f * width;
		
		mAlphaRect = new RectF(left, top, right, bottom);	
		
	
		mAlphaPattern = new AlphaPatternDrawable((int) (5 * mDensity));
		mAlphaPattern.setBounds(Math.round(mAlphaRect.left), Math
				.round(mAlphaRect.top), Math.round(mAlphaRect.right), Math
				.round(mAlphaRect.bottom));

	}
	
	private void setUpSizeRect() {
		
		float left = 0.02f * width;
		float top = 1.024f * width;
		float bottom = 1.124f * width;
		float right = 0.980f * width;
		
		mSizeRect = new RectF(left, top, right, bottom);
		
	}
	
	public IPenChangedListener getListener () {
		return mListener;
	}
	
	public void setPen(int newColor, float newSize) {
		float[] newHSV = {0,0,0};
		Color.colorToHSV(newColor, newHSV);
		mHue = newHSV[0];
		mSat = newHSV[1];
		mVal = newHSV[2];
		mAlpha = Color.alpha(newColor);
		mSize = newSize;
	}
	
	
	public interface IPenChangedListener {
		public void onPenChanged(int color, float size);
	}
}