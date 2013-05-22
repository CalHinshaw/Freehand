package com.freehand.share;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.freehand.note_editor.Note;
import com.freehand.storage.INoteHierarchyItem;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

public class NoteSharer extends AsyncTask<List<INoteHierarchyItem>, Integer, Intent> {
	private ProgressUpdateFunction mUpdater;
	private Context mContext;
	
	public NoteSharer (ProgressUpdateFunction newUpdater, Context newContext) {
		mUpdater = newUpdater;
		mContext = newContext;
	}
	
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		mUpdater.updateProgress(0);
	}
	
	@Override
	protected Intent doInBackground(List<INoteHierarchyItem>... params) {
		
		// Make sure we have a list of hierarchy items
		if (params.length < 1) {
			return null;
		}
		
		List<INoteHierarchyItem> itemsToShare = params[0];
		int progressIncrement = (int) (100/itemsToShare.size());
		
		ArrayList<Uri> imageUris = new ArrayList<Uri>();
		
		File rootDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath().concat("/temp/Freehand"));
		
		for (int i = 0; i < itemsToShare.size(); i++) {
			List<Uri> newUris = (new Note(itemsToShare.get(i))).getJpegUris(rootDirectory);
			if (newUris != null) {
				imageUris.addAll(newUris);
			}
			
			this.publishProgress((i+1)*progressIncrement);
		}
		
		if (imageUris.isEmpty()) {
			return null;
		} else {
			Intent shareIntent = new Intent();
			shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
			shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);
			shareIntent.setType("image/jpeg");
			
			return shareIntent;
		}
		
	}
	
	@Override
	protected void onProgressUpdate(Integer... values) {
		if (values.length > 0) {
			mUpdater.updateProgress(values[0]);
		}
	}
	
	@Override
	protected void onPostExecute (Intent shareIntent) {
		if (shareIntent == null) {
			Toast.makeText(mContext, "Share failed.", Toast.LENGTH_LONG).show();
		} else {
			mContext.startActivity(Intent.createChooser(shareIntent, "Share notes with..."));
		}
		
		mUpdater.updateProgress(1000);
	}

}