package com.freehand.organizer;

import com.calhounroberthinshaw.freehand.R;

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

	private final String mTitleText;
	private final String mMessageText;
	private final String mDefaultInput;
	private final String mPositiveButtonText;
	private final String mNegativeButtonText;
	
	private final NewItemFn mOnConfirmFunction;
	
	public NewItemDialog (String newTitleText, String newMessageText, String newDefaultInput, String newPositiveButtonText, String newNegativeButtonText, NewItemFn newOnConfirmFunction) {
		mTitleText = newTitleText;
		mMessageText = newMessageText;
		mDefaultInput = newDefaultInput;
		mPositiveButtonText = newPositiveButtonText;
		mNegativeButtonText = newNegativeButtonText;
		mOnConfirmFunction = newOnConfirmFunction;
	}
	
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Set up view for dialog
		final LayoutInflater inflater = getActivity().getLayoutInflater();
		final View contentView = inflater.inflate(R.layout.input_dialog_layout, null);
		final EditText input = (EditText) contentView.findViewById(R.id.input_dialog_EditText);
		input.setText(mDefaultInput);
		((TextView) contentView.findViewById(R.id.input_dialog_message)).setText(mMessageText);
		
		// Create onClickListener for the positive button
		OnClickListener positiveListener = new OnClickListener () {
			public void onClick(DialogInterface dialog, int which) {
				mOnConfirmFunction.function(input.getText().toString());
			}
		};
		
		// Create the dialog
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_LIGHT);
		builder	.setTitle(mTitleText)
				.setView(contentView)
				.setNegativeButton(mNegativeButtonText, null)
				.setPositiveButton(mPositiveButtonText, positiveListener);
			
		final AlertDialog d = builder.create();
		d.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);	// Show soft keyboard automatically
		return d;
	}
}