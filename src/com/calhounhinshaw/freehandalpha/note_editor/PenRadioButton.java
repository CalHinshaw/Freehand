package com.calhounhinshaw.freehandalpha.note_editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RadioButton;

public class PenRadioButton extends RadioButton implements OnPenChangedListener {
	private int color = Color.BLACK;
	private float size = 6.5f;
	private Drawable mBackground;
	
	private float sampleX = 0;
	private float sampleY = 0;
	private Paint samplePaint = new Paint();
	
	private Rect selectedRect;
	private Paint selectedPaint = new Paint();
	
	private Paint pressedPaint = new Paint();
	
	private OnPenChangedListener mListener = null;
	private NewPenRequestListener mRequestListener = null;
	
	private boolean hasCheckedState = false;
	
	
	private OnClickListener mClickListener = new OnClickListener () {	
		public void onClick(View v) {
			if (hasCheckedState) {
				Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
				vibrator.vibrate(40);
			
				mRequestListener.requestNewPen(PenRadioButton.this, color, size);
			}
		}

	};
	
	
	
	
	
	public PenRadioButton (Context context) {
		super(context);
		
		init();
	}
	
	public PenRadioButton (Context context, AttributeSet attrs) {
		super(context, attrs);
		
		init();
	}
	
	public PenRadioButton (Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		init();
	}
	
	private void init() {
		mBackground = new AlphaPatternDrawable((int) (5*getContext().getResources().getDisplayMetrics().density));
		
		samplePaint.setColor(color);
		samplePaint.setStrokeWidth(size);
		samplePaint.setStrokeCap(Paint.Cap.ROUND);
		
		selectedPaint.setColor(0xFF33B5E5);
		selectedPaint.setStrokeWidth(6);
		selectedPaint.setStyle(Paint.Style.STROKE);
		selectedPaint.setStrokeJoin(Paint.Join.MITER);
		selectedPaint.setStrokeCap(Paint.Cap.BUTT);
		
		pressedPaint.setColor(0x600099CC);
		
		this.setOnClickListener(mClickListener);
	}
	
	@Override
	protected void onSizeChanged (int w, int h, int oldW, int oldH) {
		mBackground.setBounds(new Rect(0, 0, w, h));
		
		sampleX = (float) (w/2);
		sampleY = (float) (h/2);
		
		selectedRect = new Rect(3, 3, w-3, h-3);
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

	
	public void setListeners (OnPenChangedListener newListener, NewPenRequestListener newRequestListener) {
		mListener = newListener;
		mRequestListener = newRequestListener;
	}
	
	public void setPen (int newColor, float newSize) {
		color = newColor;
		size = newSize;
		
		samplePaint.setColor(color);
	}
	
	@Override
	public void toggle() {
		setChecked(true);
		
		if (mListener != null) {
			mListener.onPenChanged(color, size);
		}
	}
	
	
	public void onPenChanged(int newColor, float newSize) {
		setPen(newColor, newSize);
		
		toggle();
		invalidate();
		
		if (mListener != null) {
			mListener.onPenChanged(color, size);
		}
	}
	
	public int getColor() {
		return color;
	}
	
	public float getSize() {
		return size;
	}
	
	public boolean onTouchEvent (MotionEvent e) {
		if (e.getAction() == MotionEvent.ACTION_UP) {
			hasCheckedState = isChecked();
		} else {
			hasCheckedState = false;
		}
		
		return super.onTouchEvent(e);
	}
}