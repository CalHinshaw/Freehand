package com.calhounhinshaw.freehandalpha.main_menu;

import com.calhounroberthinshaw.freehand.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class InputDialog extends DialogFragment {

	private String mTitleText;
	private String mMessageText;
	private String mDefaultInput;
	private String mPositiveButtonText;
	private String mNegativeButtonText;
	
	private SingleStringFunctor mOnConfirmFunction;
	
	public InputDialog (String newTitleText, String newMessageText, String newDefaultInput, String newPositiveButtonText, String newNegativeButtonText, SingleStringFunctor newOnConfirmFunction) {
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
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View contentView = inflater.inflate(R.layout.input_dialog_layout, null);
		((TextView) contentView.findViewById(R.id.input_dialog_message)).setText(mMessageText);
		final EditText input = (EditText) contentView.findViewById(R.id.input_dialog_EditText);
		input.setText(mDefaultInput);
		
		// Create onClickListener for the positive button
		OnClickListener positiveListener = new OnClickListener () {
			public void onClick(DialogInterface dialog, int which) {
				mOnConfirmFunction.function(input.getText().toString());
			}
		};
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_LIGHT);
		
		builder.setTitle(mTitleText)
			.setView(contentView)
			.setNegativeButton(mNegativeButtonText, null)
			.setPositiveButton(mPositiveButtonText, positiveListener);
			
		
		return builder.create();
	}
}