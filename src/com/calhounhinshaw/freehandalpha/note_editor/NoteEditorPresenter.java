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
	
}