package com.freehand.note_editor;

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

public class PenRadioButton extends RadioButton implements PenCreatorView.IPenChangedListener {
	
	private IActionBarListener mListener = null;
	private IViewOverlayHandler mOverlayHandler = null;
	
	private int color = Color.BLACK;
	private float size = 6.5f;
	private Drawable mBackground;
	
	private float sampleX = 0;
	private float sampleY = 0;
	private Paint samplePaint = new Paint();
	
	private Rect selectedRect;
	private Paint selectedPaint = new Paint();
	
	private Paint pressedPaint = new Paint();
	
	private boolean hasCheckedState = false;
	
	/**
	 * Listens for the second click that signals this button to open it's pen creator menu.
	 */
	private OnClickListener mClickListener = new OnClickListener () {
		public void onClick(View v) {
			if (hasCheckedState) {
				Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
				vibrator.vibrate(40);

				PenCreatorView creatorView = new PenCreatorView(mOverlayHandler.getContextForView(), PenRadioButton.this, color, size);
				mOverlayHandler.setOverlayView(creatorView);
			}
		}
	};
	
	private OnLongClickListener mLongClickListener = new OnLongClickListener () {
		public boolean onLongClick(View v) {
			PenRadioButton.this.setChecked(true);
			
			Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
			vibrator.vibrate(40);
			
			PenCreatorView creatorView = new PenCreatorView(mOverlayHandler.getContextForView(), PenRadioButton.this, color, size);
			mOverlayHandler.setOverlayView(creatorView);
			
			return true;
		}
	};
	
	
	public PenRadioButton (Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mBackground = new AlphaPatternDrawable((int) (5*getContext().getResources().getDisplayMetrics().density));
		
		samplePaint.setColor(color);
		samplePaint.setStrokeWidth(size);
		samplePaint.setStrokeCap(Paint.Cap.ROUND);
		samplePaint.setAntiAlias(true);
		
		selectedPaint.setColor(0xFF33B5E5);
		selectedPaint.setStrokeWidth(6);
		selectedPaint.setStyle(Paint.Style.STROKE);
		selectedPaint.setStrokeJoin(Paint.Join.MITER);
		selectedPaint.setStrokeCap(Paint.Cap.BUTT);
		selectedPaint.setAntiAlias(true);
		
		pressedPaint.setColor(0x600099CC);
		
		this.setOnClickListener(mClickListener);
		this.setOnLongClickListener(mLongClickListener);
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
	}
	
	public void setListener (IActionBarListener newListener) {
		mListener = newListener;
	}
	
	public void setOverlayHandler (IViewOverlayHandler newOverlayHandler) {
		mOverlayHandler = newOverlayHandler;
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
	
	@Override
	public void setChecked (boolean checked) {
		if (checked == true && mListener != null) {
			mListener.setTool(IActionBarListener.Tool.PEN, size, color);
		}
		
		super.setChecked(checked);
	}
	
	public void onPenChanged(int newColor, float newSize) {
		setPen(newColor, newSize);
		
		if (mListener != null) {
			mListener.setTool(IActionBarListener.Tool.PEN, size, color);
		}
		
		invalidate();
	}
	
	public boolean onTouchEvent (MotionEvent e) {
		if (e.getAction() == MotionEvent.ACTION_UP) {
			hasCheckedState = isChecked();
		} else {
			hasCheckedState = false;
		}
		
		return super.onTouchEvent(e);
	}
	
	public int getColor() {
		return color;
	}
	
	public float getSize() {
		return size;
	}
}