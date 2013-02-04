package com.calhounhinshaw.freehandalpha.note_editor;

class NoteEditorPresenter {
	private NoteView mNoteView;
	
	
	// The zoom scalar that all values going into and out of the note are multiplied by after translation by windowX and windowY
	private float zoomMultiplier = 1;
	
	// The x and y values of the upper left corner of the screen relative to the note data
	private float windowX = 0;
	private float windowY = 0;
	
	public void setNoteView (NoteView newNoteView) {
		mNoteView = newNoteView;
	}
	
	public void setPen (int newColor, float newSize) {
		if (mNoteView != null) {
			mNoteView.onPenChanged(newColor, newSize);
		}
	}
	
	public void setEraser () {
		mNoteView.onErase();
	}
	
	public void setSelector () {
		mNoteView.onSelect();
	}
	
	public void undo () {
		mNoteView.onUndo();
	}
	
	public void redo () {
		mNoteView.onRedo();
	}
	
	
	public void panZoom (float dx, float dy, float dz, float x, float y) {
		
	} 
}