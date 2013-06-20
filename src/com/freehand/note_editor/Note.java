package com.freehand.note_editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.freehand.ink.Stroke;

public class Note {
	private ArrayList<Stroke> inkLayer = new ArrayList<Stroke>(5000);
	
	public List<Stroke> getInkLayer () {
		return Collections.unmodifiableList(inkLayer);
	}
	
	public void performActions (List<Action> actions) {
		for (Action a : actions) {
			if (a.isAddition == true) {
				inkLayer.add(a.index, a.stroke);
			} else {
				inkLayer.remove(a.index);
			}
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
	
}