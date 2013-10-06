package com.freehand.editor.tool_bar;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;

public class PenRadioButton extends PreviousStateAwareRadioButton implements PenCreatorView.IPenChangedListener {
	private Drawable mBackground;
	private IActionBarListener mListener = null;
	
	private final float dipScale = getResources().getDisplayMetrics().density;
	
	private int color = Color.BLACK;
	private float size = 6.5f;
	
	private float sampleX = 0;
	private float sampleY = 0;
	private Paint samplePaint = new Paint();
	
	private Rect selectedRect;
	private Paint selectedPaint = new Paint();
	private Paint pressedPaint = new Paint();
	
	private AnchorWindow mPenCreator;
	private PenCreatorView mPenCreatorView;
	
	
	/**
	 * Listens for the second click that signals this button to open it's pen creator menu.
	 */
	private OnClickListener mClickListener = new OnClickListener () {
		public void onClick(View v) {
			if (PenRadioButton.this.mPenCreator.lastClosedByAnchorTouch() == true) {
				return;
			}
			
			if (previousStateWasChecked()) {
				Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
				vibrator.vibrate(40);
				PenRadioButton.this.mPenCreator.show();
			}
		}
	};
	
	private OnLongClickListener mLongClickListener = new OnLongClickListener () {
		public boolean onLongClick(View v) {
			PenRadioButton.this.setChecked(true);
			Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
			vibrator.vibrate(40);
			PenRadioButton.this.mPenCreator.show();
			
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
		mBackground = new AlphaPatternDrawable((int) (5*getContext().getResources().getDisplayMetrics().density));
		
		samplePaint.setColor(color);
		samplePaint.setStrokeWidth(size);
		samplePaint.setStrokeCap(Paint.Cap.ROUND);
		samplePaint.setAntiAlias(true);
		
		selectedPaint.setColor(0xFF33B5E5);
		selectedPaint.setStrokeWidth(4.0f);
		selectedPaint.setStyle(Paint.Style.STROKE);
		selectedPaint.setStrokeJoin(Paint.Join.MITER);
		selectedPaint.setStrokeCap(Paint.Cap.BUTT);
		selectedPaint.setAntiAlias(true);
		
		pressedPaint.setColor(0x600099CC);
		
		this.setOnClickListener(mClickListener);
		this.setOnLongClickListener(mLongClickListener);
		this.setOnCheckedChangeListener(mCheckListener);
		
		mPenCreatorView = new PenCreatorView(this.getContext(), this, color, size);
		mPenCreator = new AnchorWindow(this, mPenCreatorView, (int) (320 * getResources().getDisplayMetrics().density),
			(int) (320*PenCreatorView.HEIGHT_SCALAR * getResources().getDisplayMetrics().density));
	}
	
	/**
	 * Usually called after a PenRadioButton object is created to set it's initial pen configuration from the app's settings
	 * 
	 * @param newColor the new pen's color
	 * @param newSize the new pen's size
	 */
	public void setPen (int newColor, float newSize) {
		color = newColor;
		size = newSize;
		
		samplePaint.setColor(color);
		mPenCreatorView.setPen(color, size);
	}
	
	public void setListener (IActionBarListener newListener) {
		mListener = newListener;
	}
	
	@Override
	protected void onSizeChanged (int w, int h, int oldW, int oldH) {
		mBackground.setBounds(new Rect(0, 0, w, h));
		
		sampleX = (float) (w/2);
		sampleY = (float) (h/2);
		
		
		selectedRect = new Rect(2, 2, w-2, h-2);
	}
	
	@Override
	protected void onDraw (Canvas canvas) {
		mBackground.draw(canvas);
		canvas.drawCircle(sampleX, sampleY, size, samplePaint);
		
		if (isChecked()) {
			canvas.drawRect(selectedRect, selectedPaint);
		}
		
		if (isPressed()) {
			canvas.drawPaint(pressedPaint);
		}
	}
	
	public void onPenChanged(int newColor, float newSize) {
		setPen(newColor, newSize);
		
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
		return mPenCreator.isShowing();
	}
	
	public void closePenCreatorWindow() {
		mPenCreator.dismiss();
	}
}