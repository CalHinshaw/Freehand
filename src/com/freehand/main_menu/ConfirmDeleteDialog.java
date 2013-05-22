package com.freehand.main_menu;

import java.util.List;

import com.freehand.storage.INoteHierarchyItem;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

public class ConfirmDeleteDialog extends DialogFragment {
	private String mTitleText;
	private String mPositiveButtonText;
	private String mNegativeButtonText;
	
	private MainMenuPresenter mPresenter;
	private List<INoteHierarchyItem> toDelete;
	
	public ConfirmDeleteDialog (String newTitleText, String newPositiveButtonText, String newNegativeButtonText, List<INoteHierarchyItem> newToDelete, MainMenuPresenter newPresenter) {
		mTitleText = newTitleText;
		mPositiveButtonText = newPositiveButtonText;
		mNegativeButtonText = newNegativeButtonText;
		toDelete = newToDelete;
		mPresenter = newPresenter;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		// Create onClickListener for the positive button
		OnClickListener positiveListener = new OnClickListener () {
			public void onClick(DialogInterface dialog, int which) {
				mPresenter.deleteWithoutConfirmation(toDelete);
			}
		};
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_LIGHT);
		
		builder.setTitle(mTitleText)
			.setNegativeButton(mNegativeButtonText, null)
			.setPositiveButton(mPositiveButtonText, positiveListener);
			
		return builder.create();
	}
}