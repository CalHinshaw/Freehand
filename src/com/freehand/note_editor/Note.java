package com.freehand.note_editor;

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
import android.os.Environment;
import android.util.Log;

import com.freehand.ink.Point;
import com.freehand.ink.Stroke;
import com.freehand.misc.WrapList;
import com.freehand.note_editor.tool.DistConverter;
import com.freehand.note_editor.tool.Pen;

public class Note {
	private File noteFile;
	private int backingFileVersion;
	
	
	private ArrayList<Stroke> inkLayer = new ArrayList<Stroke>(5000);
	
	private LinkedList<List<Action>> undoQueue = new LinkedList<List<Action>>();
	private LinkedList<List<Action>> redoQueue = new LinkedList<List<Action>>();
	
	public Note (String notePath) {
		noteFile = new File(notePath);
		
		if (!notePath.endsWith(".note") || !noteFile.exists() || noteFile.isDirectory()) {
			backingFileVersion = 2;
			return;
		}
		
		try {
			DataInputStream s = new DataInputStream(new BufferedInputStream(new FileInputStream(noteFile)));
			
			int formatVersion = s.readInt();
			backingFileVersion = formatVersion;
			
			if (formatVersion == 1) {
				readV1(s);
			} else if (formatVersion == 2) {
				readV2(s);
			}
			
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void readV1 (DataInputStream s) throws IOException {
		
		DistConverter conv = new DistConverter () {
			@Override
			public float canvasToScreenDist(float canvasDist) {
				return 0;
			}

			@Override
			public float screenToCanvasDist(float screenDist) {
				return 0;
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
			
			Pen pen = new Pen(this, conv, 0, color, size);
			
			int numSubStrokes = s.readInt();
			for (int ssNum = 0; ssNum < numSubStrokes; ssNum++) {
				int numPointsInSubstroke = s.readInt();
				Log.d("PEN", Integer.toString(numPointsInSubstroke));
				pen.startPointerEvent();
				for (int sspNum = 0; sspNum < numPointsInSubstroke; sspNum++) {
					pen.continuePointerEvent(new Point(s.readFloat(), s.readFloat()), 1000, 1);
				}
				pen.finishPointerEvent();
			}
		}
		
		undoQueue.clear();
	}
	
	private void readV2 (DataInputStream s) throws IOException {
		int numStrokes = s.readInt();
		
		for (int i = 0; i < numStrokes; i++) {
			int color = s.readInt();
			int numPoints = s.readInt();
			WrapList<Point> poly = new WrapList<Point>(numPoints);
			for (int j = 0; j < numPoints; j++) {
				poly.add(new Point(s.readFloat(), s.readFloat()));
			}
			inkLayer.add(new Stroke(color, poly));
			
		}
	}
	
	public boolean save () {
		if (noteFile.getPath().endsWith(".note") == false) { return false; }
		if (noteFile.isDirectory() == true) { return false; }
		
		try {
			File temp = new File(noteFile.getParentFile(), "temp_"+noteFile.getName());
			DataOutputStream s = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(temp)));
			s.writeInt(2);
			s.writeInt(inkLayer.size());
			
			for (Stroke stroke : inkLayer) {
				s.writeInt(stroke.getColor());
				
				s.writeInt(stroke.getPoly().size());
				for (Point p : stroke.getPoly()) {
					s.writeFloat(p.x);
					s.writeFloat(p.y);
				}
			}
			
			s.close();
			
			if (backingFileVersion == 2) {
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
	
}