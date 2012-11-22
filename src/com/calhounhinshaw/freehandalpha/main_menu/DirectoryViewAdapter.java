package com.calhounhinshaw.freehandalpha.main_menu;

import java.io.File;
import java.util.Date;

import com.calhounhinshaw.freehandalpha.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class DirectoryViewAdapter extends ArrayAdapter<File> {
	private Context mContext;
	private int mLayoutResourceId;
	private File mFiles[] = null;
	
	// Needed in getView
	private Drawable mFolderDrawable;
	private Drawable mDefaultNoteDrawable;

	public DirectoryViewAdapter(Context context, int layoutResourceId, File[] files, Drawable folderDrawable, Drawable defaultNoteDrawable) {
		super(context, layoutResourceId, files);

		mContext = context;
		mLayoutResourceId = layoutResourceId;
		mFiles = files;
		mFolderDrawable = folderDrawable;
		mDefaultNoteDrawable = defaultNoteDrawable;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		RowDataHolder holder = new RowDataHolder();
		
		// If convertView is new initialize it
		if (convertView == null) {
			LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
			convertView = inflater.inflate(mLayoutResourceId, parent, false);
		}
		
		// Set the tag to be the file this view represents so the view can be meaningfully interacted with by other classes
		convertView.setTag(mFiles[position]);
		
		// Temporarily set holder to convertView's sub-views for easier modification
		holder.imageView = (ImageView)convertView.findViewById(R.id.DirectoryViewRowThumbnail);
		holder.textView = (TextView)convertView.findViewById(R.id.DirectoryViewRowName);
			
		// Set the content of convertView's sub-views. Folders and notes get treated differently
		if (mFiles[position].isDirectory()) {
			holder.textView.setText(mFiles[position].getName());
			holder.imageView.setImageDrawable(mFolderDrawable);
		} else if (mFiles[position].isFile()) {
			String noteName = mFiles[position].getName().replace(".note", "");
			holder.textView.setText(noteName);
			holder.imageView.setImageDrawable(mDefaultNoteDrawable);
		}
		
		Date d = new Date(mFiles[position].lastModified());
		
		
		return convertView;
	}
	
	// simple class to make getView a bit clearer
	private static class RowDataHolder {
		ImageView imageView;
		TextView textView;
		
		public RowDataHolder() {}
	}
}