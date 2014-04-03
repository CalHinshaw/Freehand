package com.freehand.editor.tool_bar;


import com.calhounroberthinshaw.freehand.R;
import com.freehand.tutorial.TutorialPrefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.FillType;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;

public class PenRadioButton extends PreviousStateAwareRadioButton implements IPenChangedListener {
	private int backgroundColor;
	
	private IActionBarListener mListener = null;
	
	private final float dipScale = getResources().getDisplayMetrics().density;
	
	private int color = Color.BLACK;
	private float size = 6.5f;
	
	private float sampleX = 0;
	private float sampleY = 0;
	private Paint samplePaint = new Paint();
	
	private Drawable alphaPatternDrawable;
	private final Path erasePath = new Path();
	private final Paint erasePaint = new Paint();
	
	private Paint selectedPaint = new Paint();
	private Paint pressedPaint = new Paint();
	
	private PenCreator penCreator;
	
	
	/**
	 * Listens for the second click that signals this button to open it's pen creator menu.
	 */
	private OnClickListener mClickListener = new OnClickListener () {
		public void onClick(View v) {
			if (PenRadioButton.this.penCreator.lastClosedByAnchorTouch() == true) {
				return;
			}
			
			if (previousStateWasChecked()) {
				Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
				vibrator.vibrate(40);
				PenRadioButton.this.penCreator.show(color, size);
				setTutorialToOff();
			}
			
			triggerTutorial();
		}
	};
	
	private OnLongClickListener mLongClickListener = new OnLongClickListener () {
		public boolean onLongClick(View v) {
			PenRadioButton.this.setChecked(true);
			Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
			vibrator.vibrate(40);
			PenRadioButton.this.penCreator.show(color, size);
			setTutorialToOff();
			
			return true;
		}
	};
	
	private OnCheckedChangeListener mCheckListener = new OnCheckedChangeListener () {
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (isChecked == true && mListener != null) {
				mListener.setTool(IActionBarListener.Tool.PEN, size, color);
			}
		}
	};
	
	public PenRadioButton (Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public PenRadioButton (Context context) {
		super(context);
		init();
	}
	
	private void init () {
		alphaPatternDrawable = new AlphaPatternDrawable((int) (5*getContext().getResources().getDisplayMetrics().density));
		backgroundColor = this.getResources().getColor(R.color.dkgray);
		
		samplePaint.setColor(color);
		samplePaint.setStrokeWidth(size);
		samplePaint.setStrokeCap(Paint.Cap.ROUND);
		samplePaint.setAntiAlias(true);
		
		erasePath.setFillType(FillType.INVERSE_WINDING);
		
		erasePaint.setAntiAlias(true);
		erasePaint.setStyle(Style.FILL);
		erasePaint.setColor(backgroundColor);
		
		selectedPaint.setColor(0xFF33B5E5);
		selectedPaint.setStrokeWidth(3.0f*getContext().getResources().getDisplayMetrics().density);
		selectedPaint.setStyle(Paint.Style.STROKE);
		selectedPaint.setStrokeJoin(Paint.Join.MITER);
		selectedPaint.setStrokeCap(Paint.Cap.BUTT);
		selectedPaint.setAntiAlias(true);
		
		pressedPaint.setColor(0x600099CC);
		
		this.setOnClickListener(mClickListener);
		this.setOnLongClickListener(mLongClickListener);
		this.setOnCheckedChangeListener(mCheckListener);
		
		penCreator = new PenCreator (this, this);
	}
	
	/**#0x7
	 * Usually called after a PenRadioButton object is created to set it's initial pen configuration from the app's settings
	 * 
	 * @param newColor the new pen's color
	 * @param newSize the new pen's size
	 */
	public void setPen (int newColor, float newSize) {
		color = newColor;
		size = newSize;
		samplePaint.setColor(color);
		
		penCreator.setPen(newColor, newSize);
	}
	
	public void setListener (IActionBarListener newListener) {
		mListener = newListener;
	}
	
	@Override
	protected void onSizeChanged (int w, int h, int oldW, int oldH) {
		sampleX = w/2.0f;
		sampleY = h/2.0f;
		
		alphaPatternDrawable.setBounds(new Rect(0, 0, w, h));
	}
	
	
	
	@Override
	protected void onDraw (Canvas canvas) {
		final float sizeToDraw = penSizeToSampleRadius(size, getContext().getResources().getDisplayMetrics().density);
		
		if (Color.alpha(color) != 0xff) {
			alphaPatternDrawable.draw(canvas);
		}
		
		canvas.drawCircle(sampleX, sampleY, sizeToDraw, samplePaint);
		
		if (isPressed()) {
			canvas.drawPaint(pressedPaint);
		}
		
		erasePath.reset();
		erasePath.addCircle(sampleX, sampleY, sizeToDraw, Path.Direction.CCW);
		canvas.drawPath(erasePath, erasePaint);
		
		if (isChecked()) {
			canvas.drawCircle(sampleX, sampleY, sizeToDraw+selectedPaint.getStrokeWidth()/2.0f, selectedPaint);
		}
	}
	
	private static float penSizeToSampleRadius (final float size, final float density) {
		if (size < 10.0f) {
			return 5.0f*density;
		} else if (size < 20.0f) {
			return 9.0f*density;
		} else if (size < 30.0f) {
			return 13.0f*density;
		} else {
			return 17.0f*density;
		}
	}
	

	
	public void onPenChanged(int newColor, float newSize) {
		color = newColor;
		size = newSize;
		samplePaint.setColor(color);
		
		if (isChecked() == true && mListener != null) {
			mListener.setTool(IActionBarListener.Tool.PEN, size*dipScale, color);
		}
		
		invalidate();
	}
	
	public int getColor() {
		return color;
	}
	
	public float getSize() {
		return size;
	}
	
	public boolean getPenCreatorShowing () {
		return penCreator.isShowing();
	}
	
	public void closePenCreatorWindow() {
		penCreator.dismiss();
	}
	
	
	// *************************************** Tutorial Methods ************************************
	
	private void triggerTutorial() {
		final SharedPreferences prefs = TutorialPrefs.getPrefs();
		if (prefs == null) return;
		boolean used = prefs.getBoolean("tool_dropdown_used", false);
		if (used == false) {
			TutorialPrefs.toast("Tap button again to display dropdown menu");
		}
	}
	
	private void setTutorialToOff() {
		final SharedPreferences prefs = TutorialPrefs.getPrefs();
		if (prefs == null) return;
		if (prefs.getBoolean("tool_dropdown_used", false) == true) return;
		TutorialPrefs.toast("The eraser also has a dropdown menu");
		prefs.edit().putBoolean("tool_dropdown_used", true).apply();
	}
}