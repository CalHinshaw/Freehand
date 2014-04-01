package com.freehand.editor.tool_bar;

import com.calhounroberthinshaw.freehand.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Paint.Style;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

public class PenCreatorView extends LinearLayout {
	private final static int TRACKER_COLOR = 0xff1c1c1c;
	
	private SatValSelector satValSelector;
	private HueSelector hueSelector;
	private AlphaSelector alphaSelector;
	private SizeSelector sizeSelector;
	
	public PenCreatorView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		
		satValSelector = (SatValSelector) this.findViewById(R.id.sat_val_selector);
		hueSelector = (HueSelector) this.findViewById(R.id.hue_selector);
		alphaSelector = (AlphaSelector) this.findViewById(R.id.alpha_selector);
		sizeSelector = (SizeSelector) this.findViewById(R.id.size_selector);
	}

	@Override
	protected void onDraw (final Canvas c) {
		c.drawColor(R.color.dkgray);
		
		final float hue = hueSelector.getHue();
		final int color = Color.HSVToColor(new float[] {hue, satValSelector.getSat(), satValSelector.getVal()});
		satValSelector.setHue(hue);
		alphaSelector.setColor(color);
		sizeSelector.setColor(color);
		
		super.onDraw(c);
	}
	
	@Override
	public boolean dispatchTouchEvent (final MotionEvent e) {
		final boolean toReturn = super.dispatchTouchEvent(e);
		invalidate();
		return toReturn;
	}

	
	
	
	
	
	
	
	
	
	public static class SatValSelector extends View {
		private static final float INNER_TRACKER_RADIUS = 4.0f;
		private static final float OUTER_TRACKER_RADIUS = 5.0f;
		
		private final float mDensity;
		private final int borderSize;
		
		private Shader valShader;
		private final Paint shaderPaint = new Paint();
		private final Paint outerTrackerPaint = new Paint();
		private final Paint innerTrackerPaint = new Paint();
		
		private float sat;
		private float val;
		
		private float hue;
		
		public SatValSelector(final Context context, final AttributeSet attrs) {
			super(context, attrs);
			
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
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			
			valShader = new LinearGradient(0, borderSize, 0, this.getHeight()-borderSize, 
					0xffffffff, 0xff000000, TileMode.CLAMP);
		}
		
		public void setHue (final float hue) {
			this.hue = hue;
		}
		
		@Override
		public void onDraw (final Canvas c) {
			// Make the saturation/value box
			final int rgb = Color.HSVToColor(new float[] {hue,1.0f,1.0f});
			final Shader satShader = new LinearGradient(borderSize, 0, this.getWidth()-borderSize, 0, 
					0xffffffff, rgb, TileMode.CLAMP);
			final Shader satValShader = new ComposeShader(valShader, satShader, PorterDuff.Mode.MULTIPLY);
			shaderPaint.setShader(satValShader);
			c.drawRect(borderSize, borderSize, this.getWidth()-borderSize, this.getHeight()-borderSize, shaderPaint);
			
			// Draw tracker
			final float xPos = satToXPos(sat);
			final float yPos = valToYPos(val);
			c.drawCircle(xPos, yPos, INNER_TRACKER_RADIUS * mDensity, innerTrackerPaint);
			c.drawCircle(xPos, yPos, OUTER_TRACKER_RADIUS * mDensity, outerTrackerPaint);
		}
		
		@Override
		public boolean onTouchEvent (final MotionEvent e) {
			sat = xPosToSat(e.getX());
			val = yPosToVal(e.getY());
			if (this.getParent() instanceof View) {
				((View) this.getParent()).invalidate();
			}
			return true;
		}
		
		public float getSat () {
			return sat;
		}
		
		public float getVal () {
			return val;
		}
		
		private float xPosToSat (final float xPos) {
			final float boundXPos = bind(xPos, borderSize, this.getWidth()-borderSize);
			final float width = getWidth()-2*borderSize;
			return (boundXPos-borderSize)/width;
		}
		
		private float yPosToVal (final float yPos) {
			final float boundYPos = bind(yPos, borderSize, this.getHeight()-borderSize);
			final float height = getHeight()-2*borderSize;
			return 1.0f-(boundYPos-borderSize)/height;
		}
		
		private float satToXPos (final float sat) {
			final float width = getWidth()-2*borderSize;
			return sat*width + borderSize;
		}
		
		private float valToYPos (final float val) {
			final float height = getHeight()-2*borderSize;
			return (1-val)*height + borderSize;
		}
	}
	
	
	
	public static class HueSelector extends View {
		private static final float TRACKER_OFFSET = 2.0f;
		
		private final float mDensity;
		private final int borderSize;
		
		private final Paint huePaint = new Paint();
		private final Paint trackerPaint = new Paint();
		
		private float hue;
		
		
		public HueSelector(final Context context, final AttributeSet attrs) {
			super(context, attrs);
			
			mDensity = getContext().getResources().getDisplayMetrics().density;
			borderSize = (int) (2.0f*mDensity + 1);
			
			huePaint.setAntiAlias(true);
			
			trackerPaint.setAntiAlias(true);
			trackerPaint.setColor(TRACKER_COLOR);
			trackerPaint.setStyle(Style.STROKE);
			trackerPaint.setStrokeWidth(TRACKER_OFFSET * mDensity);
		}
		
		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			
			huePaint.setShader(new LinearGradient(0, borderSize, 0, this.getHeight()-borderSize, buildHueColorArray(), null, TileMode.CLAMP));
		}
		
		@Override
		public void onDraw (final Canvas c) {
			// Draw hues
			c.drawRect(borderSize, borderSize, this.getWidth()-borderSize, this.getHeight()-borderSize, huePaint);
			
			// Draw slider
			final float yPos = hueToYPos(hue);
			final float trackerSize = TRACKER_OFFSET * mDensity;
			c.drawRoundRect(new RectF(0, yPos-trackerSize, getWidth(), yPos+trackerSize), trackerSize, trackerSize, trackerPaint);
		}
		
		@Override
		public boolean onTouchEvent (final MotionEvent e) {
			hue = yPosToHue(e.getY());
			if (this.getParent() instanceof View) {
				((View) this.getParent()).invalidate();
			}
			return true;
		}
		
		public float getHue () {
			return hue;
		}
		
		private float yPosToHue (final float yPos) {
			final float boundYPos = bind(yPos, borderSize, this.getHeight()-borderSize);
			final float height = this.getHeight()-2*borderSize;
			return 360f - ((boundYPos-borderSize) * 360f / height);
		}
		
		private float hueToYPos (final float hue) {
			final float height = this.getHeight()-2*borderSize;
			return (height*(360.0f-hue)/360.0f) + borderSize;
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
	public static class AlphaSelector extends View {
		private final float mDensity;
		private final int borderSize;
		
		private AlphaPatternDrawable mAlphaPattern;
		private final Paint shaderPaint = new Paint();
		private final Paint labelPaint = new Paint();
		private final Paint trackerPaint = new Paint();
		
		private int alpha;
		
		private int color;
		
		public AlphaSelector(final Context context, final AttributeSet attrs) {
			super(context, attrs);
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
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			
			mAlphaPattern = new AlphaPatternDrawable((int) (5 * mDensity));
			mAlphaPattern.setBounds(borderSize, borderSize, this.getWidth()-borderSize, this.getHeight()-borderSize);
		}
		
		public void setColor (final int color) {
			Log.d("PEN", "setColor");
			this.color = color;
		}
		
		@Override
		public void onDraw (final Canvas c) {
			// Draw the alpha pattern
			mAlphaPattern.draw(c);
			
			// Draw the shader that shows how transparent a colors looks at a given alpha on top of the alpha pattern
			final int acolor = color - 0xff000000;
			shaderPaint.setShader(new LinearGradient(borderSize, 0, this.getWidth()-borderSize, 0, color, acolor, TileMode.CLAMP));
			c.drawRect(borderSize, borderSize, this.getWidth()-borderSize, this.getHeight()-borderSize, shaderPaint);
			
			// Draw the transparency label on top of that stuff
			c.drawText("Transparency", this.getWidth()/2, this.getHeight()/2 + labelPaint.getTextSize()/2, labelPaint);
			
			// Draw the alpha tracker on top of everything else
			final float xPos = alphaToXPos(alpha);
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
			alpha = xPosToAlpha(e.getX());
			if (this.getParent() instanceof View) {
				((View) this.getParent()).invalidate();
			}
			return true;
		}
		
		public int getSelectedAlpha () {
			return alpha;
		}
		
		private int xPosToAlpha (final float xPos) {
			final float boundXPos = bind(xPos, borderSize, this.getWidth()-borderSize);
			final float width = this.getWidth()-2*borderSize;
			return (int) ((boundXPos-borderSize)*255.0f/width);
		}
		
		private float alphaToXPos (final int alpha) {
			final float width = this.getWidth()-2*borderSize;
			return alpha * width / 255.0f + borderSize;
		}
	}
	
	
	
	public static class SizeSelector extends View {
		private static final float MAX_SIZE = 40.0f;
		
		private final float mDensity;
		private final int borderSize;
		
		private final Path sizePath = new Path();
		
		private final Paint sizePaint = new Paint();
		private final Paint trackerPaint = new Paint();
		
		private float size;
		
		private int color;
		
		public SizeSelector (final Context context) {
			super(context);
			mDensity = getContext().getResources().getDisplayMetrics().density;
			borderSize = (int) (2.0f*mDensity + 1);
			initPaint();
		}
		
		public SizeSelector (final Context context, final AttributeSet attrs) {
			super(context, attrs);
			mDensity = getContext().getResources().getDisplayMetrics().density;
			borderSize = (int) (2.0f*mDensity + 1);
			initPaint();
		}
		
		private void initPaint () {
			sizePaint.setAntiAlias(true);
			sizePaint.setStyle(Paint.Style.FILL);
			
			trackerPaint.setAntiAlias(true);
			trackerPaint.setColor(TRACKER_COLOR);
			trackerPaint.setStyle(Style.STROKE);
			trackerPaint.setStrokeWidth(2.0f * mDensity);
		}
		
		public void setSize (final float size) {
			this.size = size;
		}
		
		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			
			sizePath.reset();
			sizePath.moveTo(borderSize, borderSize);
			sizePath.lineTo(getWidth()-borderSize, getHeight()/2);
			sizePath.lineTo(borderSize, getHeight()-borderSize);
		}
		
		public void setColor (final int color) {
			this.color = color;
		}
		
		@Override
		public void onDraw (final Canvas c) {
			// Draw the size slider
			sizePaint.setColor(color);
			c.drawPath(sizePath, sizePaint);
			
			// Draw the size tracker
			final float xPos = sizeToXPos(size);
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
			size = xPosToSize(e.getX());
			if (this.getParent() instanceof View) {
				((View) this.getParent()).invalidate();
			}
			return true;
		}
		
		public float getSelectedSize () {
			return size;
		}
		
		public float sizeToXPos (final float size) {
			final float width = getWidth()-2*borderSize;
			return width * (MAX_SIZE-size) / MAX_SIZE;
		}
		
		public float xPosToSize (final float xPos) {
			final float boundXPos = bind(xPos, borderSize, getWidth()-borderSize);
			final float width = getWidth()-2*borderSize;
			return Math.max(MAX_SIZE-(boundXPos*MAX_SIZE/width), 0.1f);
		}
	}
	
	
	
	public static class PenDisplay extends View {

		public PenDisplay(final Context context, final AttributeSet attrs) {
			super(context, attrs);
			// TODO Auto-generated constructor stub
		}
		
	}
	
	
	private static float bind (final float input, final float floor, final float ceiling) {
		if (input < floor) return floor;
		if (input > ceiling) return ceiling;
		return input;
	}
}