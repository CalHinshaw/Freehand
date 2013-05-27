package com.freehand.note_editor;

import android.content.Context;
import android.graphics.Point;
import android.view.View;

interface IViewOverlayHandler {
	public Context getContextForView();
	public void setOverlayView(View newOverlayView, View anchor);
}