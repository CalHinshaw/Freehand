package com.freehand.note_editor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import android.util.Log;

import com.freehand.ink.Point;
import com.freehand.ink.Stroke;
import com.freehand.note_editor.tool.DistConverter;
import com.freehand.note_editor.tool.Pen;

public class Note {
	private final File noteFile;
	
	
	private ArrayList<Stroke> inkLayer = new ArrayList<Stroke>(5000);
	
	private LinkedList<List<Action>> undoQueue = new LinkedList<List<Action>>();
	private LinkedList<List<Action>> redoQueue = new LinkedList<List<Action>>();
	
	public Note (String notePath) {
		noteFile = new File(notePath);
		
		if (notePath.endsWith(".note") == false) { return; }
		if (noteFile.exists() == false) { return; }
		if (noteFile.isDirectory() == true) { return; }
		
		try {
			DataInputStream s = new DataInputStream(new BufferedInputStream(new FileInputStream(noteFile)));
			
			int formatVersion = s.readInt();
			
			if (formatVersion == 1) {
				readV1(s);
			} else if (formatVersion == 2) {
				
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