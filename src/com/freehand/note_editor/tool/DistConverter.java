package com.freehand.note_editor.tool;

public abstract class DistConverter {
	public abstract float canvasToScreenDist(float canvasDist);
	public abstract float screenToCanvasDist(float screenDist);
}