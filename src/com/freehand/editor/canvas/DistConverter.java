package com.freehand.editor.canvas;

public abstract class DistConverter {
	public abstract float canvasToScreenDist(float canvasDist);
	public abstract float screenToCanvasDist(float screenDist);
}