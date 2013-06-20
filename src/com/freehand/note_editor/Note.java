package com.freehand.note_editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import android.util.Log;

import com.freehand.ink.Stroke;

public class Note {
	private ArrayList<Stroke> inkLayer = new ArrayList<Stroke>(5000);
	
	private LinkedList<List<Action>> undoQueue = new LinkedList<List<Action>>();
	private LinkedList<List<Action>> redoQueue = new LinkedList<List<Action>>();
	
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