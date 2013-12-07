package com.freehand.organizer;

import com.calhounroberthinshaw.freehand.R;
import com.freehand.editor.canvas.Note;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.RadioGroup;

public class NewNoteDialog extends DialogFragment {
	private final String mDefaultNoteName;
	private final onConfirmFn mOnConfirmFunction;
	
	public NewNoteDialog (String defaultNoteName, onConfirmFn onConfirmFunction) {
		mDefaultNoteName = defaultNoteName;
		mOnConfirmFunction = onConfirmFunction;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Set up view for dialog
		final LayoutInflater inflater = getActivity().getLayoutInflater();
		final View contentView = inflater.inflate(R.layout.new_note_dialog_layout, null);
		final EditText input = (EditText) contentView.findViewById(R.id.input_dialog_EditText);
		input.setText(mDefaultNoteName);
		
		// Create onClickListener for the positive button
		OnClickListener positiveListener = new OnClickListener () {
			public void onClick(DialogInterface dialog, int which) {
				final RadioGroup g = (RadioGroup) contentView.findViewById(R.id.paper_type);
				Note.PaperType paperType = Note.PaperType.WHITEBOARD;
				
				if (g.getCheckedRadioButtonId() == R.id.vert_85x11) {
					paperType = Note.PaperType.VERTICAL_85X11;
				}
				
				mOnConfirmFunction.function(input.getText().toString(), paperType);
			}
		};
		
		// Create the dialog
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_LIGHT);
		builder	.setView(contentView)
				.setNegativeButton("Cancel", null)
				.setPositiveButton("Create Note", positiveListener);
			
		final AlertDialog d = builder.create();
		d.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);	// Show soft keyboard automatically
		return d;
	}
	
	public static abstract class onConfirmFn {
		abstract public void function(String s, Note.PaperType t);
	}
}