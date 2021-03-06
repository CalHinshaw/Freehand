package com.freehand.editor.canvas;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import com.freehand.ink.Point;
import com.freehand.ink.Stroke;

public class Note {
	private File noteFile;
	private int backingFileVersion;
	
	private PaperType paperType = PaperType.WHITEBOARD;
	
	private ArrayList<Stroke> inkLayer = new ArrayList<Stroke>(5000);
	
	private LinkedList<List<Action>> undoQueue = new LinkedList<List<Action>>();
	private LinkedList<List<Action>> redoQueue = new LinkedList<List<Action>>();
	
	
	public Note (final String notePath) {
		noteFile = new File(notePath);
		
		if (!notePath.endsWith(".note") || !noteFile.exists() || noteFile.isDirectory()) {
			backingFileVersion = 3;
			return;
		}
		
		try {
			DataInputStream s = new DataInputStream(new BufferedInputStream(new FileInputStream(noteFile)));
			
			backingFileVersion = s.readInt();
			
			if (backingFileVersion == 1) {
				readV1(s);
			} else if (backingFileVersion == 2) {
				readV2(s);
			} else if (backingFileVersion == 3) {
				readV3(s);
			}
			
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Note () {
		noteFile = new File("/empty");
	}
	
	private void readV1 (DataInputStream s) throws IOException {
		
		ICanvScreenConverter conv = new ICanvScreenConverter () {
			public float canvToScreenDist(final float canvasDist) {
				return 0;
			}

			public float screenToCanvDist(final float screenDist) {
				return 0;
			}

			public Rect canvRectToScreenRect(RectF canvRect) {
				return null;
			}
		};
		
		// Ignore the background color field
		s.readInt();
		int numCompoundStrokes = s.readInt();
		
		for (int csNum = 0; csNum < numCompoundStrokes; csNum++) {
			int color = s.readInt();
			float size = s.readFloat()/2;
			
			// ignore rounds - too hard to fix...
			s.readBoolean();
			s.readBoolean();
			
			Pen pen = new Pen(this, conv, null, 0, color, size, true);
			
			int numSubStrokes = s.readInt();
			for (int ssNum = 0; ssNum < numSubStrokes; ssNum++) {
				int numPointsInSubstroke = s.readInt();
				// Used a hack to get round end caps that makes strokes of length less than 2 impossible - this is just in case...
				if (numPointsInSubstroke < 2) continue;
				
				pen.onMotionEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, s.readFloat(), s.readFloat(), 1, 1, 0, 1, 1, 0, 0));
				for (int sspNum = 1; sspNum < numPointsInSubstroke-1; sspNum++) {
					pen.onMotionEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, s.readFloat(), s.readFloat(), 1, 1, 0, 1, 1, 0, 0));
				}
				pen.onMotionEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, s.readFloat(), s.readFloat(), 1, 1, 0, 1, 1, 0, 0));
			}
		}
		
		undoQueue.clear();
	}
	
	private void readV2 (final DataInputStream s) throws IOException {
		int numStrokes = s.readInt();
		
		for (int i = 0; i < numStrokes; i++) {
			int color = s.readInt();
			int numPoints = s.readInt();
			List<Point> poly = new ArrayList<Point>(numPoints);
			for (int j = 0; j < numPoints; j++) {
				poly.add(new Point(s.readFloat(), s.readFloat()));
			}
			inkLayer.add(new Stroke(color, poly));
			
		}
	}
	
	private void readV3 (final DataInputStream s) throws IOException {
		paperType = PaperType.values()[s.readInt()];
		
		Log.d("PEN", paperType.toString());
		
		s.readInt();	// This is the background place holder
		
		readV2(s);
	}
	
	public boolean save () {
		if (noteFile.getPath().endsWith(".note") == false) { return false; }
		if (noteFile.isDirectory() == true) { return false; }
		
		try {
			File temp = new File(noteFile.getParentFile(), "temp_"+noteFile.getName());
			DataOutputStream s = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(temp)));
			s.writeInt(3);						// Version of .note file format
			s.writeInt(paperType.ordinal());	// PaperType
			s.writeInt(0);						// Background if it's ever used
			
			s.writeInt(inkLayer.size());
			
			for (Stroke stroke : inkLayer) {
				s.writeInt(stroke.getColor());
				
				s.writeInt(stroke.getPoly().size());
				for (Point p : stroke.getPoly()) {
					s.writeFloat(p.x);
					s.writeFloat(p.y);
				}
			}
			
			s.writeInt(0);						// Objects like images and text boxes will go here if I ever add them
			
			s.close();
			
			if (backingFileVersion == 3) {
				temp.renameTo(noteFile);
			} else {
				File rootDir = Environment.getExternalStorageDirectory();
				File backupDir = new File(rootDir, "Freehand Backups");
				backupDir.mkdirs();
				File backupFile = new File(backupDir, "backup_" + noteFile.getName());
				
				List<File> children = Arrays.asList(backupDir.listFiles());
				int num = 0;
				while (children.contains(backupFile)) {
					num++;
					backupFile = new File(backupDir, "backup_" + noteFile.getName().replace(".note", "") + " " + Integer.toString(num) + ".note");
				}
				
				String finalPath = noteFile.getPath();
				noteFile.renameTo(backupFile);
				temp.renameTo(new File(finalPath));
			}
			
			
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public String getPath () {
		return noteFile.getPath();
	}
	
	public boolean rename (String newName) {
		File newNameFile = new File(noteFile.getParent(), newName + ".note");
		
		if (noteFile.renameTo(newNameFile)) {
			noteFile = newNameFile;
			return true;
		} else {
			return false;
		}
	}
	
	
	public List<Stroke> getInkLayer () {
		return Collections.unmodifiableList(inkLayer);
	}
	
	public PaperType getPaperType () {
		return paperType;
	}
	
	public void performActions (List<Action> actions) {
		doActions(actions);
		undoQueue.push(actions);
		redoQueue.clear();
	}
	
	private void doActions (List<Action> actions) {
		for (Action a : actions) {
			if (a.isAddition == true) {
				inkLayer.add(a.index, a.stroke);
			} else {
				inkLayer.remove(a.index);
			}
		}
	}
	
	private void undoActions (List<Action> actions) {
		ListIterator<Action> iter = actions.listIterator(actions.size());
		
		while (iter.hasPrevious()) {
			Action a = iter.previous();
			
			if (a.isAddition == true) {
				inkLayer.remove(a.index);
			} else {
				inkLayer.add(a.index, a.stroke);
			}
		}
	}
	
	public void undo() {
		if (undoQueue.isEmpty() == true) {
			return;
		}
		
		List<Action> toUndo = undoQueue.pop();
		undoActions(toUndo);
		
		redoQueue.push(toUndo);
	}
	
	public void redo() {
		if (redoQueue.isEmpty() == true) {
			return;
		}
		
		List<Action> toRedo = redoQueue.pop();
		doActions(toRedo);
		undoQueue.push(toRedo);
	}
	
	
	public static boolean createEmptyNote (final File noteFile, final PaperType paperType) {
		noteFile.getParentFile().mkdirs();
		try {
			DataOutputStream s = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(noteFile)));
			
			s.writeInt(3);						// Version of .note file format
			s.writeInt(paperType.ordinal());	// PaperType
			s.writeInt(0);						// Background if it's ever used
			
			s.writeInt(0);						// The ink layer doesn't exist, so it's size is zero
			s.writeInt(0);						// We also don't have any objects
			
			s.close();
			
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	
	public static class Action {
		public final Stroke stroke;
		public final int index;
		public final boolean isAddition;
		
		public Action (Stroke stroke, int index, boolean isAddition) {
			this.stroke = stroke;
			this.index = index;
			this.isAddition = isAddition;
		}
	}
	
	public enum PaperType {
		WHITEBOARD		(Integer.MAX_VALUE, Integer.MIN_VALUE),
		VERTICAL_85X11	(830, 1080);
		
		private static final float Y_MAX = 250000.0f;
		
		public final int width;
		public final int height;
		public final float yMax;
		public final int numPages;
		PaperType(final int width, final int height) {
			this.width = width;
			this.height = height;
			this.numPages = (int) (2*Y_MAX / height);
			yMax = numPages*height / 2.0f;
		}
	}
	
}