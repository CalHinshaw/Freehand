package com.freehand.organizer;

import com.calhounroberthinshaw.freehand.R;
import com.freehand.storage.INoteHierarchyItem;

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
import android.widget.TextView;

public class NewItemDialog extends DialogFragment {

	private String mTitleText;
	private String mMessageText;
	private String mDefaultInput;
	private String mPositiveButtonText;
	private String mNegativeButtonText;
	
	private NewItemFunctor mOnConfirmFunction;
	private INoteHierarchyItem mNoteHierarchyItem;
	
	public NewItemDialog (String newTitleText, String newMessageText, String newDefaultInput, String newPositiveButtonText, String newNegativeButtonText, INoteHierarchyItem newItem, NewItemFunctor newOnConfirmFunction) {
		mTitleText = newTitleText;
		mMessageText = newMessageText;
		mDefaultInput = newDefaultInput;
		mPositiveButtonText = newPositiveButtonText;
		mNegativeButtonText = newNegativeButtonText;
		mNoteHierarchyItem = newItem;
		mOnConfirmFunction = newOnConfirmFunction;
	}
	
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Set up view for dialog
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View contentView = inflater.inflate(R.layout.input_dialog_layout, null);
		((TextView) contentView.findViewById(R.id.input_dialog_message)).setText(mMessageText);
		final EditText input = (EditText) contentView.findViewById(R.id.input_dialog_EditText);
		input.setText(mDefaultInput);
		
		// Create onClickListener for the positive button
		OnClickListener positiveListener = new OnClickListener () {
			public void onClick(DialogInterface dialog, int which) {
				mOnConfirmFunction.function(mNoteHierarchyItem, input.getText().toString());
			}
		};
		
		// Create the dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_LIGHT);
		
		builder.setTitle(mTitleText)
			.setView(contentView)
			.setNegativeButton(mNegativeButtonText, null)
			.setPositiveButton(mPositiveButtonText, positiveListener);
			
		AlertDialog d = builder.create();
		
		// Show soft keyboard automatically
		d.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		
		return d;
	}
}