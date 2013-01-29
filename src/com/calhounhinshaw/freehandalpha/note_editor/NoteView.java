package com.calhounhinshaw.freehandalpha.note_editor;

import java.util.LinkedList;
import java.util.Vector;

import com.calhounhinshaw.freehandalpha.note_orginazion.INoteHierarchyItem;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

public class NoteView extends SurfaceView implements SurfaceHolder.Callback, OnPenChangedListener {
	private NoteViewThread myThread;
	
	
	private static final int WAITING = 0;
	private static final int WORKING = 1;
	private static final int MOVING = 2;
	
	private static final int DRAWING = 0;
	private static final int ERASING = 1;
	private static final int SELECTING = 2;
	
	// The state of the view
	private int mTouchState = WAITING;
	private int mWorkState = DRAWING;
	
	// The variables the paint new strokes are being added to the note with are defined by
	private int currentPaintColor = Color.BLUE;
	private float currentPaintSize = 4.5F;
	
	// The ammount values on the screen are multiplied by to keep the data in the note consistant with the ammount of zoom
	private float zoomMultiplier = 1;
	
	// The x and y values of the upper left corner of the screen
	private float windowX = 0;
	private float windowY = 0;
	
	// The data that makes up the stroke currently being drawn
	private Vector<PointF> currentStroke = new Vector<PointF>();
	
	// the data for the note
	private Note mNote;
	
	// The set of variables needed to perform the move and zoom opperations
	private float initialX;
	private float initialY;
	private float initialZoom;
	private float initialDistance;
	
	private float eraserSizeOnScreen = 30.0F;
	private float oldX;
	private float oldY;
	
	private LinkedList<PointF[]> toAdd = new LinkedList<PointF[]>();
	
	private float lastScreenX = 0;
	private float lastScreenY = 0;
	
//************************************* Constructors ************************************************
	private void init() {
		getHolder().addCallback(this);
		myThread = new NoteViewThread (getHolder(), this);
		
		mNote = new Note();
	}

	public NoteView(Context context) {
		super(context);
		init();
	}
	
	public NoteView (Context context, AttributeSet attrs) {
		super (context, attrs);
		init();
	}
	
	public NoteView (Context context, AttributeSet attrs, int defStyle) {
		super (context, attrs, defStyle);
		init();
	}

//---------------------------------------------------------------------------------------	
	


// ******************************** SurfaceHolder.Callback methods *****************************************
	
// Sets windowWidth and windowHeight to the new value.
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		myThread.invalidate();
	}
	
	// Starts myThread;
	public void surfaceCreated(SurfaceHolder holder) {
		myThread.invalidate();
	}
	
	// Stops myThread in a safe way.
	public void surfaceDestroyed(SurfaceHolder holder) {
		boolean retry = true;
		//myThread.setRunning(false);	// Tell thread to stop looping.
		// Wait for thread to finish last pass (we know it's done when we can join it without throwing an exception).
		while (retry) {
		try {
			myThread.join();
			retry = false;
		} catch (InterruptedException e) {
			// This is fine, we will keep trying until it works.
			}
		}
	}
//-----------------------------------------------------------------------------------------


	
//******************************* Tool Changer Methods ************************************
	
	public void onPenChanged (int color, float size) {
		currentPaintColor = color;		
		currentPaintSize = size;
		mWorkState = DRAWING;
		mNote.cancleSelection();
		
		myThread.invalidate();
		
		currentStroke = new Vector<PointF>();
	}
	
	public void onErase () {
		mWorkState = ERASING;
		mNote.cancleSelection();
		myThread.invalidate();
		currentStroke = new Vector<PointF>();
	}
	
	public void onSelect() {
		mWorkState = SELECTING;
		mNote.cancleSelection();
		myThread.invalidate();
	}
	
	public void onUndo () {
		if (mNote.isSelection()) {
			mNote.cancleSelection();
			myThread.invalidate();
			currentStroke = new Vector<PointF>();
		} else {
			mNote.undo();
		}
		
		myThread.invalidate();
	}
	
	public void onRedo () {
		if (mNote.isSelection()) {
			mNote.cancleSelection();
			myThread.invalidate();
			currentStroke = new Vector<PointF>();
		} else {
			mNote.redo();
		}
		
		myThread.invalidate();
	}
	
	
	
	public void openNote (INoteHierarchyItem note) {
		mNote = new Note(note);
		
		myThread.invalidate();
	}
	
	public void openNote (Note newNote) {
		mNote = newNote;
		myThread.invalidate();
	}
	
	public void saveNote () {
		if (mNote.save()) {
			Toast.makeText(getContext(), "Save Successful", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(getContext(), "Save Failed", Toast.LENGTH_LONG).show();
		}
	}
	
	public void changeMetadata (String name) {
		mNote.changeName(name);
	}
	
	public Note getNote() {
		return mNote;
	}
//------------------------------------------------------------------------------------------

	public String getName () {
		return mNote.getName();
	}
	

//****************************** Touch Handeling Methods *********************************************

	public boolean onTouchEvent (MotionEvent event) {
				
		if (mTouchState == WAITING && event.getPointerCount() == 1) {
			mTouchState = WORKING;
		} else if ((mTouchState == WAITING || mTouchState == WORKING) && event.getPointerCount() == 2) {
			mTouchState = MOVING;
		}
		
		if (mTouchState == WORKING) {
			if (mWorkState == DRAWING)
				draw(event);
			else if (mWorkState == ERASING)
				erase(event);
			else if (mWorkState == SELECTING)
				select(event);
		} else if (mTouchState == MOVING) {
			if (mWorkState == SELECTING && mNote.isSelection())
				moveSelection(event);
			else
				move(event);
		}
		
		return true;
	}
	
	
	private void draw (MotionEvent event) {
		Rect boundingRect = new Rect((int) lastScreenX, (int) lastScreenY, (int) lastScreenX, (int) lastScreenY);;
		
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			synchronized (currentStroke) {
				currentStroke = new Vector<PointF>();
			}
			
			currentStroke.add(new PointF(windowX + event.getX()*zoomMultiplier, windowY + zoomMultiplier * event.getY()));
			
			lastScreenX = event.getX();
			lastScreenY = event.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			for (int i = 0; i < event.getHistorySize(); i++) {
				float actualX = (event.getHistoricalX(i)*zoomMultiplier) + windowX;
				float actualY = (event.getHistoricalY(i)*zoomMultiplier) + windowY;
				
				float lastX = (currentStroke.get(currentStroke.size()-1).x - windowX)/zoomMultiplier;
				float lastY = (currentStroke.get(currentStroke.size()-1).y - windowY)/zoomMultiplier;
				
				lastScreenX = event.getHistoricalX(i);
				lastScreenY = event.getHistoricalY(i);
				
				boundingRect.union((int) event.getHistoricalX(i), (int) event.getHistoricalY(i));
				
				if (event.getHistoricalX(i)-lastX >= 3 || event.getHistoricalX(i)-lastX <= -3 || event.getHistoricalY(i)-lastY >= 3 || event.getHistoricalY(i)-lastY <= -3) {
					currentStroke.add(new PointF(actualX, actualY));
				}
			}
			
			float actualX = (event.getX()*zoomMultiplier) + windowX;
			float actualY = (event.getY()*zoomMultiplier) + windowY;
			
			float lastX = (currentStroke.get(currentStroke.size()-1).x - windowX)/zoomMultiplier;
			float lastY = (currentStroke.get(currentStroke.size()-1).y - windowY)/zoomMultiplier;
			
			lastScreenX = event.getX();
			lastScreenY = event.getY();
			
			boundingRect.union((int) event.getX(), (int) event.getY());
			
			if (event.getX()-lastX >= 3 || event.getX()-lastX <= -3 || event.getY()-lastY >= 3 || event.getY()-lastY <= -3) {
				currentStroke.add(new PointF(actualX, actualY));
			}
			
			
			boundingRect.inset((int) (-1*currentPaintSize/zoomMultiplier) - 10, (int) (-1*currentPaintSize/zoomMultiplier) - 10);
			myThread.invalidate(boundingRect);
			
			
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:			
			currentStroke.add(new PointF(windowX + zoomMultiplier * event.getX(), windowY + zoomMultiplier * event.getY()));
			boundingRect.union((int) (windowX + zoomMultiplier * event.getX()), (int) (windowY + zoomMultiplier * event.getY()));
			
			if (currentStroke.size() ==2) {
				currentStroke.add(new PointF(windowX + zoomMultiplier * event.getX() + 0.001F, windowY + zoomMultiplier * event.getY() + 0.001F));
				boundingRect.union((int) event.getX(), (int) event.getY());
			}
			
			synchronized (toAdd) {
				toAdd.offer(currentStroke.toArray(new PointF[0]));
			}
			
			synchronized (currentStroke) {
				currentStroke = new Vector<PointF>();
			}

			
			mTouchState = WAITING;
			
			boundingRect.inset((int) (-1*currentPaintSize/zoomMultiplier) - 10, (int) (-1*currentPaintSize/zoomMultiplier) - 10);
			myThread.invalidate(boundingRect);
			
			break;
		}
	}
	
	private void erase(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mNote.initializeErasure();
			
			oldX = event.getX();
			oldY = event.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			mNote.eraseLineSegment(eraserSizeOnScreen*zoomMultiplier, windowX + zoomMultiplier *oldX, windowY + zoomMultiplier *oldY, windowX + zoomMultiplier * event.getX(), windowY + zoomMultiplier * event.getY());
			oldX = event.getX();
			oldY = event.getY();
			
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			mNote.eraseLineSegment(eraserSizeOnScreen*zoomMultiplier, windowX + zoomMultiplier *oldX, windowY + zoomMultiplier *oldY, windowX + zoomMultiplier * event.getX(), windowY + zoomMultiplier * event.getY());
			
			mNote.finilizeErasure();
			
			mTouchState = WAITING;
			break;
		}
		
		myThread.invalidate();
	}
	
	private void select(MotionEvent event) {
Rect boundingRect = new Rect((int) lastScreenX, (int) lastScreenY, (int) lastScreenX, (int) lastScreenY);;
		
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			synchronized (currentStroke) {
				currentStroke = new Vector<PointF>();
			}
			
			currentStroke.add(new PointF(windowX + event.getX()*zoomMultiplier, windowY + zoomMultiplier * event.getY()));
			break;
		case MotionEvent.ACTION_MOVE:
			for (int i = 0; i < event.getHistorySize(); i++) {
				float actualX = (event.getHistoricalX(i)*zoomMultiplier) + windowX;
				float actualY = (event.getHistoricalY(i)*zoomMultiplier) + windowY;
				
				float lastX = (currentStroke.get(currentStroke.size()-1).x - windowX)/zoomMultiplier;
				float lastY = (currentStroke.get(currentStroke.size()-1).y - windowY)/zoomMultiplier;
				
				lastScreenX = event.getHistoricalX(i);
				lastScreenY = event.getHistoricalY(i);
				
				boundingRect.union((int) event.getHistoricalX(i), (int) event.getHistoricalY(i));
				
				if (event.getHistoricalX(i)-lastX >= 3 || event.getHistoricalX(i)-lastX <= -3 || event.getHistoricalY(i)-lastY >= 3 || event.getHistoricalY(i)-lastY <= -3) {
					currentStroke.add(new PointF(actualX, actualY));
				}
			}
			
			boundingRect.inset((int) (-1*currentPaintSize/zoomMultiplier) - 10, (int) (-1*currentPaintSize/zoomMultiplier) - 10);
			myThread.invalidate(boundingRect);
			
			
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:			
			currentStroke.add(new PointF(windowX + zoomMultiplier * event.getX(), windowY + zoomMultiplier * event.getY()));
			
			if (currentStroke.size() ==2) {
				currentStroke.add(new PointF(windowX + zoomMultiplier * event.getX() + 0.001F, windowY + zoomMultiplier * event.getY() + 0.001F));
			}
			
			mNote.select(currentStroke.toArray(new PointF[0]));
			
			synchronized (currentStroke) {
				currentStroke = new Vector<PointF>();
			}

			mTouchState = WAITING;
			
			myThread.invalidate();
			
			break;
		}
	}
	
	private void moveSelection(MotionEvent event) {
		if (event.getPointerCount() == 2) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_POINTER_2_DOWN:
				mNote.initalizeMove();
				
				oldX = windowX + event.getX()*zoomMultiplier;
				oldY = windowY + event.getY()*zoomMultiplier;
				initialDistance = distance(event.getX(0), event.getX(1), event.getY(0), event.getY(1));
				break;
			case MotionEvent.ACTION_MOVE:
			case MotionEvent.ACTION_UP:
				float currentDistance = distance(event.getX(0), event.getX(1), event.getY(0), event.getY(1));
				float dZoom = currentDistance/initialDistance;
				float currentX = windowX + event.getX()*zoomMultiplier;
				float currentY = windowY + event.getY()*zoomMultiplier;
				mNote.moveSelected(currentX - oldX, currentY - oldY, dZoom, currentX, currentY);
				oldX = currentX;
				oldY = currentY;
				initialDistance = currentDistance;
				break;
			}
		}
		
		if (event.getAction() == MotionEvent.ACTION_UP) {
			mTouchState = WAITING;
			mNote.finalizeMove();
		}
		
		myThread.invalidate();
	}
	
	private void move (MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_POINTER_2_DOWN:
			if (event.getPointerCount() == 2) {
				initMove(event);
			}
			
			break;
		case MotionEvent.ACTION_MOVE:
		case MotionEvent.ACTION_UP:
			if (event.getPointerCount() == 2) {
				float newZoomMultiplier = initialZoom*(initialDistance/distance(event.getX(0), event.getX(1), event.getY(0), event.getY(1)));

				if (newZoomMultiplier >= 0.10f && newZoomMultiplier <= 10.0f) {
					zoomMultiplier = newZoomMultiplier;
				} else if (newZoomMultiplier < 0.10f) {
					zoomMultiplier = 0.10f;
					initMove(event);
				} else {
					zoomMultiplier = 10.0f;
					initMove(event);
				}
				
				
				float newWindowX = initialX - event.getX(0)*zoomMultiplier;
				float newWindowY = initialY - event.getY(0)*zoomMultiplier;
				
				if (newWindowX <= 1000000f && newWindowX >= -1000000f) {
					windowX = newWindowX;
				} else if (newWindowX > 1000000f) {
					windowX = 1000000f;
					initMove(event);
					Toast.makeText(getContext(), "Edge of canvas reached.", Toast.LENGTH_SHORT).show();
				} else {
					windowX = -1000000f;
					initMove(event);
					Toast.makeText(getContext(), "Edge of canvas reached.", Toast.LENGTH_SHORT).show();
				}
				
				if (newWindowY <= 1000000f && newWindowY >= -1000000f) {
					windowY = newWindowY;
				} else if (newWindowY > 1000000f) {
					windowY = 1000000f;
					initMove(event);
					Toast.makeText(getContext(), "Edge of canvas reached.", Toast.LENGTH_SHORT).show();
				} else {
					windowY = -1000000f;
					initMove(event);
					Toast.makeText(getContext(), "Edge of canvas reached.", Toast.LENGTH_SHORT).show();
				}
			}
			
			break;
		}
		
		if (event.getAction() == MotionEvent.ACTION_UP) {
			mTouchState = WAITING;
		}
		
		myThread.invalidate();
	}
	
	private void initMove (MotionEvent event) {
		initialX = windowX + event.getX(0)*zoomMultiplier;
		initialY = windowY + event.getY(0)*zoomMultiplier;
		initialZoom = zoomMultiplier;
		initialDistance = distance(event.getX(0), event.getX(1), event.getY(0), event.getY(1));
	}
	
	private float distance (float x1, float x2, float y1, float y2) {
		return (float)Math.sqrt(Math.pow((x1-x2), 2) + Math.pow((y1-y2), 2));
	}
	
//-------------------------------------------------------------------------------------------------------------
	
	
//********************************* onDraw Methods *************************************************************
	@Override
	public void onDraw (Canvas c) {
		mNote.drawNote(c, windowX, windowY, 1/zoomMultiplier);
		
		if (mWorkState != ERASING) {
			drawCurrentActivity(c);
		}
		
		while (toAdd.size()>0) {
			mNote.addDrawingStroke(currentPaintColor, currentPaintSize, toAdd.poll());
		}
	}
	
	private void drawCurrentActivity (Canvas c) {
		Path path = new Path();
		Paint tempPaint = new Paint();
		
		tempPaint.setStyle(Paint.Style.STROKE);
		tempPaint.setStrokeJoin(Paint.Join.ROUND);
		tempPaint.setStrokeCap(Paint.Cap.ROUND);
		tempPaint.setAntiAlias(true);
		
		
		synchronized (toAdd) {
			for (PointF[] points : toAdd) {
				if (points.length >= 2) {
					path.moveTo((points[0].x-windowX)/zoomMultiplier, (points[0].y-windowY)/zoomMultiplier);
					
					for (int i = 1; i < points.length; i++) {
						path.lineTo((points[i].x-windowX)/zoomMultiplier, (points[i].y-windowY)/zoomMultiplier);
					}
					
					tempPaint.setColor(currentPaintColor);
					tempPaint.setStrokeWidth(currentPaintSize/zoomMultiplier);
					c.drawPath(path, tempPaint);
				}
			}		
		}
		
		
		synchronized (currentStroke) {
			if (currentStroke.size() > 0) {
				path.moveTo((currentStroke.get(0).x - windowX)/zoomMultiplier, (currentStroke.get(0).y - windowY)/zoomMultiplier);
				
				for (int i = 1; i < currentStroke.size(); i++) {
					path.lineTo((currentStroke.get(i).x - windowX)/zoomMultiplier, (currentStroke.get(i).y - windowY)/zoomMultiplier);
				}
				
				if (mWorkState == DRAWING) {
					tempPaint.setColor(currentPaintColor);
					tempPaint.setStrokeWidth(currentPaintSize/zoomMultiplier);
				} else if(mWorkState == SELECTING) {
					tempPaint.setColor(Color.RED);
					tempPaint.setStrokeWidth(3.5F);
					tempPaint.setPathEffect(new DashPathEffect(new float[] {10, 20}, 0));
				}
				
				c.drawPath(path, tempPaint);
			}
		}
	}

//------------------------------------------------------------------------------------------------------------------
























//*********************** STOP!!!! THREAD RUNNING THIS VIEW IS BELOW - DON'T FUCK WITH IT **********************
	
	// Thread that draws NoteView to it's surface holder.
	private class NoteViewThread extends Thread {
		private SurfaceHolder mySurfaceHolder;	// The surface holder we are drawing myNoteView to.
		private NoteView myNoteView;			// The view that will run in this thread.
		
		private Canvas c = null;
		
		// CONSTRUCTOR: gets passed the SurfaceHolder and NoteView it will be running and
		// 	stores them in class variables.
		public NoteViewThread (SurfaceHolder tempSurfaceHolder, NoteView tempNoteView) {
			mySurfaceHolder = tempSurfaceHolder;
			myNoteView = tempNoteView;
		}

		public void invalidate() {
			c = null;
		
			try {
				c = mySurfaceHolder.lockCanvas(null);
				synchronized (mySurfaceHolder) {
					myNoteView.onDraw(c);	// Pass c to myNoteView to be drawn on
				}
			} finally {
				if (c != null) {
					mySurfaceHolder.unlockCanvasAndPost(c);
				}
			}
		}
		
		public void invalidate(Rect r) {
			c = null;
			
			try {
				c = mySurfaceHolder.lockCanvas(r);
				synchronized (mySurfaceHolder) {
					myNoteView.onDraw(c);	// Pass c to myNoteView to be drawn on
				}
			} finally {
				if (c != null) {
					mySurfaceHolder.unlockCanvasAndPost(c);
				}
			}
		}
		 		
	}
//--------------------------------------------------------------------------------------------------------------

}
	
