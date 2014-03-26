package com.freehand.editor.tool_bar;


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
	
	private static final int TRACKER_COLOR = 0xff1c1c1c;
	
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
	
	
	//************************************ Slider/Selector Classes *************************************
	
	private static class SatValSelector extends View {
		private static final float INNER_TRACKER_RADIUS = 4.0f;
		private static final float OUTER_TRACKER_RADIUS = 5.0f;
		
		private final float mDensity;
		private final int borderSize;
		
		private Shader valShader;
		private final Paint shaderPaint = new Paint();
		private final Paint outerTrackerPaint = new Paint();
		private final Paint innerTrackerPaint = new Paint();
		
		private float xPos;
		private float yPos;
		
		public SatValSelector(Context context) {
			super(context);
			
			mDensity = getContext().getResources().getDisplayMetrics().density;
			borderSize = (int) (2.0f*mDensity + 1);
			
			shaderPaint.setAntiAlias(true);
			
			outerTrackerPaint.setAntiAlias(true);
			outerTrackerPaint.setStyle(Style.STROKE);
			outerTrackerPaint.setStrokeWidth(2f * mDensity);
			outerTrackerPaint.setColor(0xffdddddd);
			
			innerTrackerPaint.setAntiAlias(true);
			innerTrackerPaint.setStyle(Style.STROKE);
			innerTrackerPaint.setStrokeWidth(2f * mDensity);
			innerTrackerPaint.setColor(0xff000000);
		}
		
		@Override
		protected void onMeasure (final int widthMeasureSpec, final int heightMeasureSpec) {
			setMeasuredDimension(View.MeasureSpec.getSize(heightMeasureSpec), View.MeasureSpec.getSize(heightMeasureSpec));
		}
		
		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			
			valShader = new LinearGradient(0, borderSize, 0, this.getHeight()-borderSize, 
					0xffffffff, 0xff000000, TileMode.CLAMP);
		}
		
		public void draw (final Canvas c, final float hue) {
			// Make the saturation/value box
			final int rgb = Color.HSVToColor(new float[] {hue,1.0f,1.0f});
			final Shader satShader = new LinearGradient(borderSize, 0, this.getWidth()-borderSize, 0, 
					0xffffffff, rgb, TileMode.CLAMP);
			final Shader satValShader = new ComposeShader(valShader, satShader, PorterDuff.Mode.MULTIPLY);
			shaderPaint.setShader(satValShader);
			c.drawRect(borderSize, borderSize, this.getWidth()-borderSize, this.getHeight()-borderSize, shaderPaint);
			
			// Draw tracker
			c.drawCircle(xPos, yPos, INNER_TRACKER_RADIUS * mDensity, innerTrackerPaint);
			c.drawCircle(xPos, yPos, OUTER_TRACKER_RADIUS * mDensity, outerTrackerPaint);
		}
		
		@Override
		public boolean onTouchEvent (final MotionEvent e) {
			xPos = bind(e.getX(), borderSize, this.getWidth()-borderSize);
			yPos = bind(e.getY(), borderSize, this.getHeight()-borderSize);
			return true;
		}
		
		public float[] getSatAndVal () {
			final float width = getWidth()-2*borderSize;
			final float height = getHeight()-2*borderSize;
			return new float[] {1.0f/width*(xPos-borderSize), 1.0f-(1.0f/height*(yPos-borderSize))};
		}
	}
	
	private static class HueSelector extends View {
		private static final float TRACKER_OFFSET = 2.0f;
		
		private final float mDensity;
		private final int borderSize;
		
		private final Paint huePaint = new Paint();
		private final Paint trackerPaint = new Paint();
		
		private float yPos;
		
		
		public HueSelector(Context context) {
			super(context);
			
			mDensity = getContext().getResources().getDisplayMetrics().density;
			borderSize = (int) (2.0f*mDensity + 1);
			
			huePaint.setAntiAlias(true);
			
			trackerPaint.setAntiAlias(true);
			trackerPaint.setColor(TRACKER_COLOR);
			trackerPaint.setStyle(Style.STROKE);
			trackerPaint.setStrokeWidth(TRACKER_OFFSET * mDensity);
		}
		
		@Override
		protected void onMeasure (final int widthMeasureSpec, final int heightMeasureSpec) {
			setMeasuredDimension(View.MeasureSpec.getSize(heightMeasureSpec)/10, View.MeasureSpec.getSize(heightMeasureSpec));
		}
		
		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			
			huePaint.setShader(new LinearGradient(0, borderSize, 0, this.getHeight()-borderSize, buildHueColorArray(), null, TileMode.CLAMP));
		}
		
		public void draw (final Canvas c) {
			// Draw hues
			c.drawRect(borderSize, borderSize, this.getWidth()-borderSize, this.getHeight()-borderSize, huePaint);
			
			// Draw slider
			float trackerSize = TRACKER_OFFSET * mDensity;
			c.drawRoundRect(new RectF(0, getWidth(), yPos-trackerSize, yPos+trackerSize), trackerSize, trackerSize, trackerPaint);
		}
		
		@Override
		public boolean onTouchEvent (final MotionEvent e) {
			yPos = bind(e.getY(), borderSize, this.getHeight()-borderSize);
			return true;
		}
		
		public float getHue () {
			final float height = this.getHeight()-2*borderSize;
			return 360f - ((yPos-borderSize) * 360f / height);
		}
		
		private static int[] buildHueColorArray () {
			final int[] hues = new int[361];
			
			int count = 0;
			for(int i = hues.length-1; i >= 0; i--, count++){
				hues[count] = Color.HSVToColor(new float[]{i, 1f, 1f});
			}
			
			return hues;
		}
	}
	
	/**
	 * Ignores normal onDraw process - uses must call draw manually.
	 * Ignores normal onMeasure process - setMeasuredDimension called with args width and width/10.
	 * @author cal
	 */
	private static class AlphaSelector extends View {
		private final float mDensity;
		private final int borderSize;
		
		private AlphaPatternDrawable mAlphaPattern;
		private final Paint shaderPaint = new Paint();
		private final Paint labelPaint = new Paint();
		private final Paint trackerPaint = new Paint();
		
		private float xPos;
		
		public AlphaSelector(Context context) {
			super(context);
			mDensity = getContext().getResources().getDisplayMetrics().density;
			borderSize = (int) (2.0f*mDensity + 1);
			
			shaderPaint.setAntiAlias(true);
			
			labelPaint.setAntiAlias(true);
			labelPaint.setTextAlign(Paint.Align.CENTER);
			
			trackerPaint.setAntiAlias(true);
			trackerPaint.setColor(TRACKER_COLOR);
			trackerPaint.setStyle(Style.STROKE);
			trackerPaint.setStrokeWidth(2.0f * mDensity);
		}
		
		@Override
		protected void onMeasure (final int widthMeasureSpec, final int heightMeasureSpec) {
			final int width = View.MeasureSpec.getSize(widthMeasureSpec);
			setMeasuredDimension(width, width/10);
		}
		
		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			
			mAlphaPattern = new AlphaPatternDrawable((int) (5 * mDensity));
			mAlphaPattern.setBounds(borderSize, borderSize, this.getWidth()-borderSize, this.getHeight()-borderSize);
		}
		
		public void draw (final Canvas c, final int color) {
			// Draw the alpha pattern
			mAlphaPattern.draw(c);
			
			// Draw the shader that shows how transparent a colors looks at a given alpha on top of the alpha pattern
			final int acolor = color - 0xff000000;
			shaderPaint.setShader(new LinearGradient(borderSize, 0, this.getWidth()-borderSize, 0, color, acolor, TileMode.CLAMP));
			c.drawRect(borderSize, borderSize, this.getWidth()-borderSize, this.getHeight()-borderSize, shaderPaint);
			
			// Draw the transparency label on top of that stuff
			c.drawText("Transparency", this.getWidth()/2, this.getHeight()/2 + labelPaint.getTextSize()/2, labelPaint);
			
			// Draw the alpha tracker on top of everything else
			final float rectWidth = 2 * mDensity;
			final RectF r = new RectF();
			r.left = xPos - rectWidth;
			r.right = xPos + rectWidth;
			r.top =  0;
			r.bottom = this.getHeight();
			c.drawRoundRect(r, rectWidth, rectWidth, trackerPaint);
		}
		
		@Override
		public boolean onTouchEvent (final MotionEvent e) {
			xPos = bind(e.getX(), borderSize, this.getWidth()-borderSize);
			return true;
		}
		
		public int getSelectedAlpha () {
			return (int) ((xPos-borderSize)/(this.getWidth()-2*borderSize) * 255);
		}
	}
	
	private static class SizeSelector extends View {
		private final float mDensity;
		private final int borderSize;
		
		private final Path sizePath = new Path();
		
		private final Paint sizePaint = new Paint();
		private final Paint trackerPaint = new Paint();
		
		private float xPos;
		
		public SizeSelector (Context context) {
			super(context);
			mDensity = getContext().getResources().getDisplayMetrics().density;
			borderSize = (int) (2.0f*mDensity + 1);
			
			
			sizePaint.setAntiAlias(true);
			sizePaint.setStyle(Paint.Style.FILL);
			
			trackerPaint.setAntiAlias(true);
			trackerPaint.setColor(TRACKER_COLOR);
			trackerPaint.setStyle(Style.STROKE);
			trackerPaint.setStrokeWidth(2.0f * mDensity);
		}
		
		@Override
		protected void onMeasure (final int widthMeasureSpec, final int heightMeasureSpec) {
			final int width = View.MeasureSpec.getSize(widthMeasureSpec);
			setMeasuredDimension(width, width/10);
		}
		
		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			
			sizePath.reset();
			sizePath.moveTo(borderSize, borderSize);
			sizePath.lineTo(getWidth()-borderSize, getHeight()/2);
			sizePath.lineTo(borderSize, getHeight()-borderSize);
		}
		
		public void draw (final Canvas c, final int color) {
			// Draw the size slider
			sizePaint.setColor(color);
			c.drawPath(sizePath, sizePaint);
			
			// Draw the size tracker
			final float rectWidth = 2 * mDensity;
			final RectF r = new RectF();
			r.left = xPos - rectWidth;
			r.right = xPos + rectWidth;
			r.top =  0;
			r.bottom = this.getHeight();
			c.drawRoundRect(r, rectWidth, rectWidth, trackerPaint);
		}
		
		@Override
		public boolean onTouchEvent (final MotionEvent e) {
			xPos = bind(e.getX(), borderSize, this.getWidth()-borderSize);
			return true;
		}
		
		public float getSelectedSize () {
			final float h = getHeight()-2*borderSize;
			final float w = getWidth()-2*borderSize;
			return h-(xPos*h/w);
		}
	}
	
	private static class PenDisplay extends View {

		public PenDisplay(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
		}
		
	}
	
	
	private static float bind (final float input, final float floor, final float ceiling) {
		if (input < floor) return floor;
		if (input > ceiling) return ceiling;
		return input;
	}
}