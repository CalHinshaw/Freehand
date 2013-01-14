package com.calhounhinshaw.freehandalpha.note_editor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import com.calhounhinshaw.freehandalpha.note_orginazion.INoteHierarchyItem;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Environment;
import android.util.Log;

public class Note {
//*********************************** Member Variables ********************************************
	
	private static final int SAVE_FORMAT_VERSION = 1;
	
	private List<Stroke> imageList = new LinkedList<Stroke>();
	private List<Stroke> stableList;
	
	private LinkedList<Temporal> undoStack = new LinkedList<Temporal>();
	private LinkedList<Temporal> redoStack = new LinkedList<Temporal>();
	
	private int backgroundColor = Color.WHITE;
	
	private boolean isSelection = false;
	
	private INoteHierarchyItem mFile;
//-------------------------------------------------------------------------------------------------
	
	
	
//*********************************** Constructors ***********************************************
	public Note () {
		backgroundColor = Color.WHITE;
	}
	
	// Opens a note from the SD card
	public Note(INoteHierarchyItem newFile) {
		mFile = newFile;
		
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState())) {
			try {
				DataInputStream s = mFile.getInputStream();
				
				int formatVersion = s.readInt();
				
				backgroundColor = s.readInt();
				
				if (formatVersion == 1) {
					readV1(s);
				} else {
					backgroundColor = Color.WHITE;
				}
				
				s.close();
			} catch (IOException e) {
				Log.d("ERROR", e.toString());
			}
		}
	}
	
	private void readV1 (DataInputStream s) throws IOException {
		int numberOfStrokes = s.readInt();
		
		for (int i = 0; i<numberOfStrokes; i++) {
			imageList.add(new Stroke(s));
		}
	}
//----------------------------------------------------------------------------------------------------
	
	
	public boolean save() {
		Log.d("PEN", "saving...");
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			Log.d("PEN", "media mounted");
			try {
				DataOutputStream s = mFile.getOutputStream();
				
				s.writeInt(SAVE_FORMAT_VERSION);
				
				s.writeInt(backgroundColor);
				
				s.writeInt(imageList.size());
				
				for (Stroke stroke : imageList) {
					stroke.strokeToByteStream(s);
				}
				
				s.close();
				
				return true;
			} catch (IOException e) {
				Log.d("PEN", "save failed:  " + e.toString());
				return false;
			}
		}
		
		return false;
	}
	
	
	public synchronized void changeName (String newName) {
		mFile.rename(newName);
	}
	
	public synchronized String getName () {
		return mFile.getName();
	}
	
	
	private synchronized List<Stroke> getImageListCopy() {
		List<Stroke> tempList = new LinkedList<Stroke>();
		
		if (imageList.size() == 0) {
			return new LinkedList<Stroke>();
		} else {
			for (Stroke s : imageList) {
				tempList.add(s.deepCopy());
			}
			
			return tempList;
		}
	}

	
	
	
	
//************************************* Stroke Addition Methods ************************************
	public synchronized void addDrawingStroke (int tempColor, float tempSize, PointF[] tempStroke) {
		undoStack.push(new Temporal(getImageListCopy()));
		imageList.add(new Stroke(tempColor, tempSize, tempStroke));

		invalidateRedoStack();
	}
//--------------------------------------------------------------------------------------------------
	
	
	
//*************************************** Erasing Methods ***********************************************
	public synchronized void eraseLineSegment (float eraserSize, float x1, float y1, float x2, float y2) {
		Iterator<Stroke> iter = imageList.iterator();

		while (iter.hasNext()) {
			Stroke temp = iter.next();
			
			if (temp.eraseLineSegment(eraserSize, x1, y1, x2, y2)) {
				iter.remove();
			}
		}
		
		invalidateRedoStack();
	}

	public synchronized void initializeErasure() {
		stableList = getImageListCopy();
	}
	
	public synchronized void finilizeErasure() {
		if(!imageList.equals(stableList))
			undoStack.push(new Temporal(stableList));
		
		stableList = new LinkedList<Stroke> ();
	}
//-------------------------------------------------------------------------------------------------------
	
	
	
//******************************************* Selector Methods ********************************************
	public synchronized void select (PointF[] selectionPolygon) {
		isSelection = false;
		
		for (Stroke stroke : imageList) {
			if (stroke.selectStroke(selectionPolygon)) {
				isSelection = true;
			}
		}
	}
	
	public synchronized void moveSelected (float dx, float dy, float dZoom, float xAnchor, float yAnchor) {
		TwoReturns<Boolean, Stroke> temp;
		Iterator<Stroke> i = imageList.iterator();
		
		List<Stroke> toAdd = new LinkedList<Stroke>();
		
		while (i.hasNext()) {
			Stroke tempStroke = i.next();
			temp = tempStroke.shiftStroke(dx, dy, dZoom, xAnchor, yAnchor);
			
			if(!temp.getFirst() && temp.getSecond() != null) {
				//whole stroke shifted
				i.remove();
				toAdd.add(temp.getSecond());
			} else if (temp.getFirst() && temp.getSecond() != null) {
				//part of stroke shifted
				toAdd.add(temp.getSecond());
			}
		}
		
		imageList.addAll(toAdd);
		
		invalidateRedoStack();
	}
	
	public synchronized void cancleSelection() {
		for (Stroke s : imageList) {
			s.clearStroke();
		}
		isSelection = false;
	}
	
	public synchronized boolean isSelection() {
		return isSelection;
	}
	
	public synchronized void initalizeMove() {
		stableList = getImageListCopy();
	}
	
	public synchronized void finalizeMove() {
		undoStack.push(new Temporal(stableList));
		stableList = new LinkedList<Stroke> ();
	}
//---------------------------------------------------------------------------------------------------------
	
	
	
//******************************************* Undo and Redo Methods *****************************************
	public synchronized void undo() {
		if (undoStack.size() > 0) {
			imageList = undoStack.pop().apply(imageList, redoStack);
		}
	}
	
	public synchronized void redo() {
		if (redoStack.size() > 0) {
			imageList = redoStack.pop().apply(imageList, undoStack);
		}
	}
	
	private synchronized void invalidateRedoStack() {
		redoStack = new LinkedList<Temporal>();
	}
//-----------------------------------------------------------------------------------------------------------
	
	
	
//******************************************* Drawing Methods ***********************************************
	
	// drawNote draws all of the FINALIZED parts of the note to the given canvas. It is not the responsibility
	// of this method to draw what the user is currently doing but has not been submitted to the note - that is
	// the responsibility of the class that calls this method.
	public synchronized void drawNote (Canvas window, float windowX, float windowY, float windowZoom) {
		if (window != null) {
			window.drawColor(backgroundColor);
			
			for (Stroke stroke : imageList) {
				stroke.drawStroke(window, windowX, windowY, windowZoom, backgroundColor);
			}
		}
	}
	
	
	/**
	 * @return a bitmap representation of the note or null if the note is empty
	 */
	public synchronized Bitmap getBitmap () {
		if (imageList != null && imageList.size() > 0 && imageList.get(0) != null) {
			RectF boundingRect = imageList.get(0).getBoundingRect();
			RectF r;
			
			for (Stroke s : imageList) {
				r = s.getBoundingRect();
				
				if (r.left < boundingRect.left) {
					boundingRect.left = r.left;
				} else if (r.right > boundingRect.right) {
					boundingRect.right = r.right;
				}
				
				if (r.top < boundingRect.top) {
					boundingRect.top = r.top;
				} else if (r.bottom > boundingRect.bottom) {
					boundingRect.bottom = r.bottom;
				}
			}
			
			// Give the image a border
			boundingRect.left -= 100;
			boundingRect.right += 100;
			boundingRect.top -= 100;
			boundingRect.bottom += 100;
			
			Bitmap toDrawOn = Bitmap.createBitmap((int) boundingRect.width(), (int) boundingRect.height(), Bitmap.Config.ARGB_8888);
			
			Canvas c = new Canvas(toDrawOn);
			drawNote(c, boundingRect.left, boundingRect.top, 1);
			
			return toDrawOn;
		} else {
			return null;
		}
	}
	
	
//-----------------------------------------------------------------------------------------------------------
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
//****************************************************** Nested Classes ******************************************
	private class Stroke {
		private Paint mPaint = new Paint();
		
		private float size;
		private int color;
		
		private boolean beginWithRound = true;
		private boolean endWithRound = true;
		
		private List<PointF[]> stroke = new ArrayList<PointF[]>(1);
		
		private Set<Integer> selectedIndexes = new TreeSet<Integer>();
		
		
		
		
		
		
		public Stroke (int tempColor, float tempSize, PointF[] tempStroke) {
			size = tempSize;
			color = tempColor;

			mPaint.setColor(tempColor);
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeJoin(Paint.Join.ROUND);
			mPaint.setStrokeCap(Paint.Cap.BUTT);
			mPaint.setAntiAlias(true);

			stroke.add(tempStroke);
		}
		
		public Stroke (DataInputStream s) {
			try {
				color = s.readInt();
				size = s.readFloat();
				beginWithRound = s.readBoolean();
				endWithRound = s.readBoolean();
				
				int numberOfSubStrokes = s.readInt();
				
				for (int i = 0; i<numberOfSubStrokes; i++) {
					LinkedList<PointF> temp = new LinkedList<PointF>();
					
					int numberOfPointArrs = s.readInt();
					for (int j = 0; j<numberOfPointArrs; j++) {
						temp.add(new PointF(s.readFloat(), s.readFloat()));
					}
					
					stroke.add(temp.toArray(new PointF[0]));
				}	
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			mPaint.setColor(color);
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeJoin(Paint.Join.ROUND);
			mPaint.setStrokeCap(Paint.Cap.BUTT);
			mPaint.setAntiAlias(true);
		}
		
		private Stroke (int tempColor, float tempSize, List<PointF[]> list, boolean begin, boolean end, boolean selected) {
			size = tempSize;
			color = tempColor;

			mPaint.setColor(tempColor);
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeJoin(Paint.Join.ROUND);
			mPaint.setStrokeCap(Paint.Cap.BUTT);
			mPaint.setAntiAlias(true);
			
			beginWithRound = begin;
			endWithRound = end;
			
			stroke = list;
			
			if(selected) {
				for (int i = 0; i < list.size(); i++) {
					selectedIndexes.add(i);
				}
			}
		}

		
		public void strokeToByteStream (DataOutputStream s) {
			try {
				s.writeInt(color);
				s.writeFloat(size);
				s.writeBoolean(beginWithRound);
				s.writeBoolean(endWithRound);
				
				s.writeInt(stroke.size());
				
				for (PointF[] points : stroke) {
					s.writeInt(points.length);
					
					for (PointF point : points) {
						s.writeFloat(point.x);
						s.writeFloat(point.y);
					}
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		public Stroke deepCopy() {
			List <PointF[]> newList = new ArrayList<PointF[]>(stroke.size());
			for (int i = 0; i< stroke.size(); i++) {
				PointF[] temp = new PointF[stroke.get(i).length];
				for (int j = 0; j < stroke.get(i).length; j++) {
					temp[j] = new PointF(stroke.get(i)[j].x, stroke.get(i)[j].y);
				}
				newList.add(temp);
			}
			
			return new Stroke(color, size, newList, beginWithRound, endWithRound, false);
		}

				
		public boolean selectStroke (PointF[] selectionPolygon) {
			clearStroke();
			
			for (int i = 0; i < stroke.size(); i++) {
				if (!selectedIndexes.contains(i)) {
					if (polylineInPolygon(stroke.get(i), selectionPolygon)) {
						selectedIndexes.add(i);
					}
				}
			}

			if (selectedIndexes.size() != 0) {
				return true;
			} else {
				return false;
			}
		}

		private boolean polylineInPolygon (PointF[] polyline, PointF[] polygon) {
			for (PointF point : polyline) {
				if (pointInPolygon(point, polygon)) {
					return true;
				}
			}
			
			for (int i = 1; i<polyline.length; i++) {
				if (lineSegmentIntersectsPolygon(polyline[i-1], polyline[i], polygon)) {
					return true;
				}
			}
			
			return false;
		}
		
		
		private boolean lineSegmentIntersectsPolygon(PointF p1, PointF p2, PointF[] polygon) {
			for (int i = 1; i<polygon.length; i++) {
				if (isSegmentIntersection(p1, p2, polygon[i-1], polygon[i])) {
					return true;
				}
			}
			
			return false;
		}
		
		
		
		private boolean pointInPolygon (PointF point, PointF[] selectionPolygon) {
			// -1 if not valid, else 0 if below and 1 if above
			int ptState = -1;
			
			int intersections = 0;
			
			for (PointF selPt : selectionPolygon) {
				if (selPt.x < point.x) {
					ptState = -1;
				} else {
					if (ptState == -1) {
						if (selPt.y >= point.y) {
							ptState = 1;
						} else {
							ptState = 0;
						}
					} else if ((selPt.y >= point.y) && ptState == 0) {
						intersections++;
						ptState = 1;
					} else if ((selPt.y < point.y) && ptState == 1) {
						intersections++;
						ptState = 0;
					}
					
				}
			}
			
			if (selectionPolygon[0].x < point.x) {
				ptState = -1;
			} else {
				if (ptState == -1) {
					if (selectionPolygon[0].y >= point.y) {
						ptState = 1;
					} else {
						ptState = 0;
					}
				} else if ((selectionPolygon[0].y >= point.y) && ptState == 0) {
					intersections++;
					ptState = 1;
				} else if ((selectionPolygon[0].y < point.y) && ptState == 1) {
					intersections++;
					ptState = 0;
				}
				
			}
			
			if (intersections >=1 && intersections%2 == 1) {
				return true;
			} else {
				return false;
			}
		}
		
		public void clearStroke () {
			selectedIndexes = new TreeSet<Integer>();
		}
		
		// boolean in return is true if the current stroke should be left where it is
		public TwoReturns<Boolean, Stroke> shiftStroke (float dx, float dy, float dZoom, float xAnchor, float yAnchor) {
			if (selectedIndexes.size() == 0) {
				
				return new TwoReturns<Boolean, Stroke>(true, null);
				
			} else if (selectedIndexes.size() == stroke.size()) {
				size *= dZoom;
				
				for (PointF[] points : stroke) {
					for (int i = 0; i < points.length; i++) {
						points[i].x += dx;
						points[i].y += dy;

						points[i].x = (points[i].x - xAnchor) * dZoom + xAnchor;
						points[i].y = (points[i].y - yAnchor) * dZoom + yAnchor;
					}
				}
			
				return new TwoReturns<Boolean, Stroke>(false, this);
				
			} else {
				float newSize = size * dZoom;
				List<PointF[]> temp = new ArrayList<PointF[]>();
				
				int removalCounter = 0;
				
				for (int i : selectedIndexes) {
					temp.add(stroke.remove(i-removalCounter++));
				}
				
				for (PointF[] points : temp) {
					for (int i = 0; i < points.length; i++) {
						points[i].x += dx;
						points[i].y += dy;

						points[i].x = (points[i].x - xAnchor) * dZoom + xAnchor;
						points[i].y = (points[i].y - yAnchor) * dZoom + yAnchor;
					}
				}
				
				if (selectedIndexes.contains(0) && selectedIndexes.contains(stroke.size()-1)) {
					selectedIndexes = new TreeSet<Integer>();
					return new TwoReturns<Boolean, Stroke>(true, new Stroke(color, newSize, temp, false, false, true));
				} else if (selectedIndexes.contains(0)) {
					selectedIndexes = new TreeSet<Integer>();
					return new TwoReturns<Boolean, Stroke>(true, new Stroke(color, newSize, temp, beginWithRound, false, true));
				} else if (selectedIndexes.contains(stroke.size()-1)) {
					selectedIndexes = new TreeSet<Integer>();
					return new TwoReturns<Boolean, Stroke>(true, new Stroke(color, newSize, temp, false, endWithRound, true));
				} else {
					selectedIndexes = new TreeSet<Integer>();
					return new TwoReturns<Boolean, Stroke>(true, new Stroke(color, newSize, temp, beginWithRound, endWithRound, true));
				}
			}
		}
		
		
		/**
		 * @return the bounding rectangle if the stroke contains points or null if it's empty
		 */
		public RectF getBoundingRect() {
			if (stroke != null && stroke.size() > 0 && stroke.get(0) != null && stroke.get(0).length >0 && stroke.get(0)[0] != null) {
				RectF boundingRect = new RectF(stroke.get(0)[0].x, stroke.get(0)[0].y, stroke.get(0)[0].x, stroke.get(0)[0].y);
					
					for (PointF[] points : stroke) {
						for (PointF p : points) {
							if (p.x < boundingRect.left) {
								boundingRect.left = p.x;
							} else if (p.x > boundingRect.right) {
								boundingRect.right = p.x;
							}
							
							if (p.y < boundingRect.top) {
								boundingRect.top = p.y;
							} else if (p.y > boundingRect.bottom) {
								boundingRect.bottom = p.y;
							}
						}
					}
					
					return boundingRect;
			} else {
				return null;
			}
			
		}
		
		
		
		
		
		public void drawStroke (Canvas c, float windowX, float windowY, float zoomMultiplier, int backgroundColor) {
			Path normalPath = new Path();
			Path selectedPath = new Path();
			mPaint.setStrokeWidth(size*zoomMultiplier);
			

			if (beginWithRound) {
				if (selectedIndexes.contains(0))
					addCapToPath (selectedPath, (stroke.get(0)[0].x-windowX) * zoomMultiplier, (stroke.get(0)[0].y -windowY) * zoomMultiplier);
				else
					addCapToPath (normalPath, (stroke.get(0)[0].x-windowX) * zoomMultiplier, (stroke.get(0)[0].y -windowY) * zoomMultiplier);
			}
			
			for (int i = 0; i < stroke.size(); i++) {
				if (selectedIndexes.contains(i)) {
					selectedPath.moveTo((stroke.get(i)[0].x-windowX) * zoomMultiplier, (stroke.get(i)[0].y-windowY) * zoomMultiplier);
					
					for (int j = 1; j<stroke.get(i).length; j++) {
						selectedPath.lineTo((stroke.get(i)[j].x-windowX) * zoomMultiplier, (stroke.get(i)[j].y - windowY) * zoomMultiplier);
					}
				} else {
					normalPath.moveTo((stroke.get(i)[0].x-windowX) * zoomMultiplier, (stroke.get(i)[0].y-windowY) * zoomMultiplier);
					
					for (int j = 1; j<stroke.get(i).length; j++) {
						normalPath.lineTo((stroke.get(i)[j].x-windowX) * zoomMultiplier, (stroke.get(i)[j].y - windowY) * zoomMultiplier);
					}
				}
			}
			
			if (endWithRound) {
				if (selectedIndexes.contains(stroke.size() - 1))
					addCapToPath(selectedPath, (stroke.get(stroke.size() - 1)[stroke.get(stroke.size() - 1).length - 1].x-windowX) * zoomMultiplier, (stroke.get(stroke.size() - 1)[stroke.get(stroke.size() - 1).length - 1].y -windowY) * zoomMultiplier);
				else
					addCapToPath(normalPath, (stroke.get(stroke.size() - 1)[stroke.get(stroke.size() - 1).length - 1].x-windowX) * zoomMultiplier, (stroke.get(stroke.size() - 1)[stroke.get(stroke.size() - 1).length - 1].y -windowY) * zoomMultiplier);
			}
			
			
			c.drawPath(normalPath, mPaint);
			
			if (selectedIndexes.size() != 0) {
				mPaint.setShadowLayer(20, 0, 0, invertColor(backgroundColor));
				c.drawPath(selectedPath, mPaint);
				mPaint.setShadowLayer(0, 0, 0, invertColor(backgroundColor));
			}
		}
	
		private void addCapToPath (Path p, float x, float y) {
			p.moveTo(x, y);
			p.lineTo(x + 0.001F, y);
			p.lineTo(x, y + 0.001F);
			p.lineTo(x - 0.001F, y);
			p.lineTo(x, y - 0.001F);
			p.lineTo(x + 0.001F, y);
		}
	
		private int invertColor (int color) {
			int r = 255 - Color.red(color);
			int b = 255 - Color.blue(color);
			int g = 255 - Color.green(color);
			
			return Color.rgb(r, g, b);
		}
		
		
		
		
		public boolean eraseLineSegment(float eraserSize, float x1, float y1, float x2, float y2) {
			float minDistSquared = eraserSize*eraserSize*0.25F + size*size*0.1875F;
			Set<Integer> touchingSegments;
			
			
			int index = 0;
			
			while (index < stroke.size()) {
				touchingSegments = new TreeSet<Integer>();
				PointF[] currentPath = stroke.get(index);
				
				for (int i = 1; i < currentPath.length; i++) {
					if (segmentDistSquared (x1, y1, x2, y2, currentPath[i-1].x, currentPath[i-1].y, currentPath[i].x, currentPath[i].y) <= minDistSquared) {
						touchingSegments.add(i-1);
						touchingSegments.add(i);
					}
				}
				
				if (touchingSegments.size() > 0) {
					if (index == 0 && touchingSegments.contains(0))
						beginWithRound = false;
					
					int segmentStart = 0;
					
					for (int touchedIndex : touchingSegments) {
						
						if (touchedIndex == segmentStart) {
						} else if (touchedIndex == segmentStart+1) {
							segmentStart++;
						} else {
							if (touchedIndex - segmentStart >=2) {
								stroke.add(index++, Arrays.copyOfRange(currentPath, segmentStart, touchedIndex+1));
							}
							segmentStart = touchedIndex;
						}
					}
					
					
					if (index == stroke.size()-1 && touchingSegments.contains(currentPath.length - 1) ){
						endWithRound = false;
					} else if (currentPath.length - segmentStart >= 2) {
						stroke.add(index++, Arrays.copyOfRange(currentPath, segmentStart, currentPath.length));
					}
					
					stroke.remove(index--);
				}
				
				index++;
			}	
			
			if (stroke.size() == 0) {
				return true;
			} else {
				return false;
			}
		}
	
		// Erase goes in first, existing stroke goes in second
		private float segmentDistSquared (float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
			if (x3 == x4) {
				x4 += 0.001f;
			}
			
			if (y3 == y4) {
				y4 += 0.001f;
			}
			
			float min = Float.MAX_VALUE;
			float denominator = (y4-y3)*(x2-x1)-(x4-x3)*(y2-y1);

			if (denominator == 0) {
				denominator+= 0.001f;
			}

			float Ta = ((x4-x3)*(y1-y3)-(y4-y3)*(x1-x3))/denominator;
			float Tb = ((x2-x1)*(y1-y3)-(y2-y1)*(x1-x3))/denominator;

			if (Ta <= 1 && Ta >= 0 && Tb <= 1 && Tb >= 0) {
				return 0;     //segments intersect
			}


			float temp;

			if (Ta<0) {
				temp = pointLineSegmentDistSquared(x3, y3, x4, y4, x1, y1);
				if (temp<min) {
					min = temp;
				}
			} else if (Ta>1) {
				temp = pointLineSegmentDistSquared(x3, y3, x4, y4, x2, y2);
				if (temp<min) {
					min = temp;
				}
			}

			if (Tb<0) {
				temp = pointLineSegmentDistSquared(x1, y1, x2, y2, x3, y3);
				if (temp<min) {
					min = temp;
				}
			} else if (Tb>1) {
				temp = pointLineSegmentDistSquared(x1, y1, x2, y2, x4, y4);
				if (temp<min) {
					min = temp;
				}
			}

			return min;
		}

		// Point 3 is the point
		private float pointLineSegmentDistSquared (float x1, float y1, float x2, float y2, float x3, float y3) {
			if (x1==x2 && y1==y2) {
				return distSquared (x1, y1, x3, y3);
			}

			float u = ((x3-x1)*(x2-x1) + (y3-y1)*(y2-y1))/((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));

			if (u<0) {
				return distSquared(x1, y1, x3, y3);
			} else if (u>1) {
				return distSquared (x2, y2, x3, y3);
			} else {
				float x = x1 + u*(x2-x1);
				float y = y1 + u*(y2-y1);
				return distSquared (x, y, x3, y3);
			}
		}

		private float distSquared (float x1, float y1, float x2, float y2) {
			return (float) (x2-x1)*(x2-x1) + (y2-y1)*(y2-y1);
		}
		
		
		
		// There are severe performance issues, so this is as optomized (unreadible) as possible
		private boolean isSegmentIntersection (PointF a, PointF b, PointF c, PointF d) {
			boolean aHigher = (a.y < b.y);
			boolean aLefter = (a.x < b.x);
			boolean cHigher = (c.y < d.y);
			boolean cLefter = (c.x < d.x);
			
			if (  !(  ( (aHigher ? a.y : b.y) <= (cHigher ? d.y : c.y) ) && ( (aHigher ? b.y : a.y) >= (cHigher ? c.y : d.y) )  )  ) {
				return false;
			}
			
			if (  !(  ( (cLefter ? c.x : d.x) <= (aLefter ? b.x : a.x) ) && ( (cLefter ? d.x : c.x) >= (aLefter ? a.x : b.x) )  )  ) {
				return false;
			}
			
			float denominator = (d.y-c.y)*(b.x-a.x)-(d.x-c.x)*(b.y-a.y);

			if (denominator == 0) {
				denominator+= 0.001f;
			}

			float Ta = ((d.x-c.x)*(a.y-c.y)-(d.y-c.y)*(a.x-c.x))/denominator;
			float Tb = ((b.x-a.x)*(a.y-c.y)-(b.y-a.y)*(a.x-c.x))/denominator;

			if (Ta <= 1 && Ta >= 0 && Tb <= 1 && Tb >= 0) {
				return true;
			}
			
			return false;
		}
	}
	

	
	
	
	
	
	
	
	
	
	
	private class TwoReturns <T, S> {
		private final T first;
		private final S second;
		
		public TwoReturns (T tFirst, S tSecond) {
			first = tFirst;
			second = tSecond;
		}
		
		public T getFirst() {
			return first;
		}
		
		public S getSecond() {
			return second;
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	private class Temporal {
		List<Stroke> undoData;
		
		public Temporal (List<Stroke> temp) {
			undoData = temp;
		}
		
		public List<Stroke> apply(List<Stroke> image, LinkedList<Temporal> otherStack) {
			otherStack.push(new Temporal(image));
			return undoData;
		}
	}
	
	

	
	
	
	
	
	
	

//---------------------------------------------------------------------------------------------------------------------
}