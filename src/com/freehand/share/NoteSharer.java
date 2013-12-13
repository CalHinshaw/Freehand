package com.freehand.share;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.freehand.editor.canvas.Note;
import com.freehand.ink.MiscPolyGeom;
import com.freehand.ink.Point;
import com.freehand.ink.Stroke;
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
import android.os.Debug;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class NoteSharer extends AsyncTask<List<Object>, Integer, Intent> {
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
	protected Intent doInBackground(List<Object>... notes) {
		
		// Make sure we have a list file paths
		if (notes.length < 1) {
			return null;
		}
		
		final List<Object> noteList = notes[0];
		if (noteList.size() < 1) {
			return null;
		}
		
		final File rootDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath().concat("/temp/Freehand"));
		rootDirectory.mkdirs();
		
		final int progressIncrement = (int) (100/noteList.size());
		
		//Debug.startMethodTracing("share");
		
		final ArrayList<Uri> imageUris = new ArrayList<Uri>();
		for (int i = 0; i < noteList.size(); i++) {
			Note toShare;
			if (noteList.get(i) instanceof Note) {
				toShare = (Note) notes[i];
			} else if (noteList.get(i) instanceof String) {
				toShare = new Note((String) noteList.get(i));
			} else {
				continue;
			}
			
			imageUris.addAll(saveNoteAsPNGs(toShare, rootDirectory));
			this.publishProgress((i+1)*progressIncrement);
		}
		
		//Debug.stopMethodTracing();
		
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

	
	private static List<Uri> saveNoteAsPNGs (final Note note, final File rootDir) {
		List<Stroke> strokes = note.getInkLayer();
		
		if (strokes.isEmpty()) {
			return new ArrayList<Uri>(0);
		}
		
		List<Rect> rects;
		if (note.getPaperType() == Note.PaperType.VERTICAL_85X11) {
			rects = get85x11VertRects(strokes);
		} else {
			rects = getWhiteboardRects(strokes);
		}
		
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
				
				String noteName = (new File(note.getPath()).getName()).replace(".note", "");
				
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
	
	
	private static List<Rect> get85x11VertRects (final List<Stroke> strokes) {
		final Note.PaperType t = Note.PaperType.VERTICAL_85X11;
		
		final int w = Note.PaperType.VERTICAL_85X11.width / 2;
		final int h = Note.PaperType.VERTICAL_85X11.height;
		
		int lastPage = 0;
		
		pageLoop:
		for (int i = t.numPages; i > 0; i--) {
			ArrayList<Point> r = new ArrayList<Point>(4);
			r.add(new Point(-w, -t.yMax + (i-1)*h));
			r.add(new Point(w, -t.yMax + (i-1)*h));
			r.add(new Point(w, -t.yMax + i*h));
			r.add(new Point(-w, -t.yMax + i*h));
			
			for (Stroke s : strokes) {
				if (MiscPolyGeom.checkPolyIntersection(r, s.getPoly())) {
					lastPage = i;
					break pageLoop;
				}
			}
		}
		
		ArrayList<Rect> pageRects = new ArrayList<Rect>(lastPage);
		for (int i = 0; i < lastPage; i++) {
			pageRects.add(new Rect(-w, (int)-t.yMax + i*h, w, (int)-t.yMax + (i+1)*h));
		}
		
		return pageRects;
	}
	
	
	private static List<Rect> getWhiteboardRects (final List<Stroke> strokes) {
		RectF aabb = strokes.get(0).getAABoundingBox();
		for (int i = 1; i < strokes.size(); i++) {
			aabb.union(strokes.get(i).getAABoundingBox());
		}
		aabb.left -= 150;
		aabb.right += 150;
		aabb.top -= 150;
		aabb.bottom += 150;
		
		long maxMemory = (long) (Runtime.getRuntime().maxMemory() * 0.5f);			// Fifty percent of the maximum continuous memory the VM will try to allocate in bytes
		long noteMemory = (long) (2 * (aabb.width()+1) * (aabb.height()+1));		// The size of the bitmap required to display the entire note in bytes
		int numPngs = (int) ((noteMemory/maxMemory) + 1);							// The number of images we're going to save
		
		return getTiledRects(rectfToRect(aabb), numPngs);
	}
	
	
	private static ArrayList<Rect> getTiledRects (Rect boundingRect, int numJpegs) {
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