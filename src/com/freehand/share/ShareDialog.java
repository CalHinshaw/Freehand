package com.freehand.share;

import java.util.List;

import com.calhounroberthinshaw.freehand.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;

public class ShareDialog extends DialogFragment {
	private final Context context;
	private final List<Object> notes;
	
	public ShareDialog (final Context context, final List<Object> notes) {
		this.context = context;
		this.notes = notes;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Set up view for dialog
		final LayoutInflater inflater = getActivity().getLayoutInflater();
		final View contentView = inflater.inflate(R.layout.share_dialog_layout, null);
		
		// Create onClickListener for the positive button
		OnClickListener positiveListener = new OnClickListener () {
			@SuppressWarnings("unchecked")
			public void onClick(DialogInterface dialog, int which) {
				final RadioGroup g = (RadioGroup) contentView.findViewById(R.id.export_format);
				
				if (g.getCheckedRadioButtonId() == R.id.png) {
					new PngSharer(context).execute(notes);
				} else if (g.getCheckedRadioButtonId() == R.id.pdf) {
					new PdfSharer(context).execute(notes);
				}
			}
		};
		
		// Create the dialog
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_LIGHT);
		builder	.setView(contentView)
				.setTitle("Select Share Format")
				.setNegativeButton("Cancel", null)
				.setPositiveButton("Share", positiveListener);
			
		return builder.create();
	}
}