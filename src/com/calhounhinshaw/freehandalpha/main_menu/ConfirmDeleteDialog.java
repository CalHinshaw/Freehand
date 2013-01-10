package com.calhounhinshaw.freehandalpha.main_menu;

import java.util.List;

import com.calhounhinshaw.freehandalpha.note_orginazion.INoteHierarchyItem;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.widget.Toast;

public class ConfirmDeleteDialog extends DialogFragment {
	private String mTitleText;
	private String mPositiveButtonText;
	private String mNegativeButtonText;
	
	private List<INoteHierarchyItem> toDelete;
	private Context toastContext;
	
	public ConfirmDeleteDialog (String newTitleText, String newPositiveButtonText, String newNegativeButtonText, List<INoteHierarchyItem> newToDelete, Context newContext) {
		mTitleText = newTitleText;
		mPositiveButtonText = newPositiveButtonText;
		mNegativeButtonText = newNegativeButtonText;
		toDelete = newToDelete;
		toastContext = newContext;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		// Create onClickListener for the positive button
		OnClickListener positiveListener = new OnClickListener () {
			public void onClick(DialogInterface dialog, int which) {
				for (INoteHierarchyItem i : toDelete) {
					if (!i.delete()) {
						Toast.makeText(toastContext, "Create new note failed. Please try again.", Toast.LENGTH_LONG).show();
					}
					
				}
			}
		};
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_LIGHT);
		
		builder.setTitle(mTitleText)
			.setNegativeButton(mNegativeButtonText, null)
			.setPositiveButton(mPositiveButtonText, positiveListener);
			
		return builder.create();
	}
}