package com.freehand.share;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.freehand.ink.Stroke;
import com.freehand.note_editor.Note;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

public class NoteSharer extends AsyncTask<List<String>, Integer, Intent> {
	private static final float TARGET_RATIO = 11.0f/8.5f;
	
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
	protected Intent doInBackground(List<String>... params) {
		
		// Make sure we have a list file paths
		if (params.length < 1) {
			return null;
		}
		
		File rootDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath().concat("/temp/Freehand"));
		rootDirectory.mkdirs();
		
		// Convert all of the Notes to PNG images and put the file Uris into imageUris
		ArrayList<Uri> imageUris = new ArrayList<Uri>();
		List<String> notePaths = params[0];
		
		int progressIncrement = 100;
		if (notePaths.size() > 0) {
			progressIncrement = (int) (100/notePaths.size());
		}
		
		for (int i = 0; i < notePaths.size(); i++) {
			imageUris.addAll(saveNoteAsPNGs(notePaths.get(i), rootDirectory));
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

	
	private static List<Uri> saveNoteAsPNGs (String notePath, File rootDir) {
		Note current = new Note(notePath);
		List<Stroke> strokes = current.getInkLayer();
		
		if (strokes.isEmpty()) {
			return new ArrayList<Uri>(0);
		}
		
		RectF aabb = strokes.get(0).getAABoundingBox();
		for (int i = 1; i < strokes.size(); i++) {
			aabb.union(strokes.get(i).getAABoundingBox());
		}
		aabb.left -= 150;
		aabb.right += 150;
		aabb.top -= 150;
		aabb.bottom += 150;
		
		long maxMemory = (long) (Runtime.getRuntime().maxMemory() * 0.5f);			// Eighty percent of the maximum continuous memory the VM will try to allocate in bytes
		long noteMemory = (long) (2 * (aabb.width()+1) * (aabb.height()+1));		// The size of the bitmap required to display the entire note in bytes
		int numPngs = (int) ((noteMemory/maxMemory) + 1);							// The number of images we're going to save
		
		ArrayList<Rect> rects = getSubRects(rectfToRect(aabb), numPngs);
		Bitmap bmp = Bitmap.createBitmap(rects.get(0).width(), rects.get(0).height(), Bitmap.Config.RGB_565);
		
		ArrayList<Uri> uris = new ArrayList<Uri>(rects.size());
		try {
			for (int i = 0; i < rects.size(); i++) {
				Canvas c = new Canvas(bmp);
				c.translate(-rects.get(i).left, -rects.get(i).top);
				c.drawColor(Color.WHITE);
				for (Stroke s : strokes) {
					s.draw(c);
				}
				
				String noteName = (new File(notePath).getName()).replace(".note", "");
				
				File target = new File(rootDir, noteName + " " + Integer.toString(i+1) + ".png");
				FileOutputStream outStream = new FileOutputStream(target);
				bmp.compress(CompressFormat.PNG, 100, outStream);
				uris.add(Uri.fromFile(target));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return uris;
	}
	
	
	private static ArrayList<Rect> getSubRects (Rect boundingRect, int numJpegs) {
		int hCells = 1;
		int vCells = 1;
		
		while (hCells * vCells < numJpegs) {
			float width = boundingRect.width()/(hCells);
			float height = boundingRect.height()/(vCells);
			float ratio = width/height;
			
			if (ratio > TARGET_RATIO) {
				hCells += 1;
			} else {
				vCells += 1;
			}
		}
		
		int cellWidth = (int) (boundingRect.width()/hCells + 1);
		int cellHeight = (int) (boundingRect.height()/vCells +1);
		
		ArrayList<Rect> subRects = new ArrayList<Rect>(hCells*vCells);
		
		for (int h = 1; h <= hCells; h++) {
			for (int v = 1; v <= vCells; v++) {
				Rect toAdd = new Rect((h-1)*cellWidth+boundingRect.left, (v-1)*cellHeight+boundingRect.top, h*cellWidth+boundingRect.left, v*cellHeight+boundingRect.top);
				subRects.add(toAdd);
			}
		}
		
		return subRects;
	}
	
	private static Rect rectfToRect (RectF rectf) {
		Rect rect = new Rect();
		
		rect.left = (int) rectf.left;
		rect.right = (int) (rectf.right+1);
		rect.top = (int) rectf.top;
		rect.bottom = (int) (rectf.bottom+1);
		
		return rect;
	}
	
}