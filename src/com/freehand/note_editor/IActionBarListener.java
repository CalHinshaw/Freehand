package com.freehand.note_editor;

interface IActionBarListener {
	public enum Tool { PEN, STROKE_ERASER, SMOOTH_ERASER }
	
	public void setTool(Tool newTool, float size, int color);
	public void undo();
	public void redo();
}