package com.calhounhinshaw.freehandalpha.note_editor;

class NoteEditorPresenter {
	private NoteView mNoteView;
	
	
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
	
}