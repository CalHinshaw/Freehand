package com.freehand.share;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.freehand.editor.canvas.Note;
import com.freehand.ink.Point;
import com.freehand.ink.Stroke;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class NoteSharer extends AsyncTask<List<Object>, Object, Intent> {
	private static final float TARGET_RATIO = 11.0f/8.5f;
	
	private Context mContext;
	private ProgressDialog mDialog;
	
	public NoteSharer (final Context newContext) {
		mContext = newContext;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		
		mDialog = new ProgressDialog(this.mContext, ProgressDialog.THEME_HOLO_LIGHT);
		mDialog.setProgressNumberFormat(null);
		mDialog.setTitle("Preparing to Share");
		mDialog.setMessage("Preparing notes for sharing.");
		mDialog.setIndeterminate(false);
		mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mDialog.setCancelable(true);
		mDialog.setCanceledOnTouchOutside(true);
		mDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel Sharing", new DialogInterface.OnClickListener () {
			public void onClick(DialogInterface dialog, int which) {
				mDialog.cancel();
			}
		});
		
		mDialog.setOnCancelListener(new OnCancelListener () {
			public void onCancel(DialogInterface dialog) {
				cancel(true);
			}
		});
		
		mDialog.show();
		mDialog.setProgress(0);
	}
	
	@Override
	protected Intent doInBackground(List<Object>... notes) {
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			Log.d("PEN", "Media not mounted");
			return null;
		}
		
		// Make sure we have a list of notes
		if (notes.length < 1) {
			return null;
		}
		
		final List<Object> noteList = notes[0];
		if (noteList.size() < 1) {
			return null;
		}
		
		final File rootDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath().concat("/temp/Freehand"));
		rootDirectory.mkdirs();
		
		final float progressIncrement = 100.0f/noteList.size();
		
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
			
			imageUris.addAll(saveNoteAsPNGs(toShare, rootDirectory, progressIncrement*i, progressIncrement));
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
	protected void onProgressUpdate(Object... values) {
		if (values.length > 0) {
			if (values[0] instanceof Float) {
				mDialog.setProgress(((Float) values[0]).intValue());
			} else if (values[0] instanceof String) {
				mDialog.setMessage((String) values[0]);
			}
		}
	}
	
	@Override
	protected void onPostExecute (Intent shareIntent) {
		if (shareIntent == null) {
			Toast.makeText(mContext, "Share failed. Please make sure your external storage is mounted and try again.", Toast.LENGTH_LONG).show();
		} else {
			mContext.startActivity(Intent.createChooser(shareIntent, "Share notes with..."));
		}
		
		mDialog.dismiss();
	}

	
	private List<Uri> saveNoteAsPNGs (final Note note, final File rootDir, final float startProgress, final float progressShare) {
		List<Stroke> strokes = note.getInkLayer();
		
		if (strokes.isEmpty()) {
			return new ArrayList<Uri>(0);
		}
		
		List<Rect> rects;
		Bitmap bmp;
		Canvas c;
		float canvasScale = 1.0f;
		if (note.getPaperType() == Note.PaperType.VERTICAL_85X11) {
			rects = get85x11VertRects(strokes);
			bmp = Bitmap.createBitmap(rects.get(0).width()*2, rects.get(0).height()*2, Bitmap.Config.ARGB_8888);
			c = new Canvas(bmp);
			canvasScale = 2.0f;
		} else {
			rects = getWhiteboardRects(strokes);
			bmp = Bitmap.createBitmap(rects.get(0).width(), rects.get(0).height(), Bitmap.Config.RGB_565);
			c = new Canvas(bmp);
		}
		
		if (rects.size() > 200) {
			final String baseMessage = "Sorry, but " + new File(note.getPath()).getName().replace(".note", "") +
				" is probably too large to share. We'll try, but it might take hours to convert the whole thing to PNGs.";
			final String suggestionMessage = "Notes are easier to share if they're small and all of the content is close together.";
			this.publishProgress(baseMessage + "\n\n" + suggestionMessage);
		} else if (rects.size() > 20) {
			final String baseMessage = new File(note.getPath()).getName().replace(".note", "") + " is very large and may take a while to share. Please be patient.";
			final String sizeMessage = "It's being broken up into " + Integer.toString(rects.size()) + " PNGs.";
			this.publishProgress(baseMessage + "\n\n" + sizeMessage);
		}
		
		final float progressIncrement = progressShare/rects.size();
		
		ArrayList<Uri> uris = new ArrayList<Uri>(rects.size());
		try {
			final Matrix m = new Matrix();
			for (int i = 0; i < rects.size(); i++) {
				m.setTranslate(-rects.get(i).left, -rects.get(i).top);
				m.postScale(canvasScale, canvasScale);
				c.setMatrix(m);
				
				c.drawColor(Color.WHITE);
				for (Stroke s : strokes) {
					s.draw(c);
				}
				
				String noteName = (new File(note.getPath()).getName()).replace(".note", "");
				
				File target = new File(rootDir, noteName + " " + Integer.toString(i+1) + ".png");
				FileOutputStream outStream = new FileOutputStream(target);
				bmp.compress(CompressFormat.PNG, 100, outStream);
				uris.add(Uri.fromFile(target));
				
				this.publishProgress(startProgress + (i+1)*progressIncrement);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		bmp.recycle();
		this.publishProgress("Preparing notes for sharing.");
		return uris;
	}
	
	
	private static List<Rect> get85x11VertRects (final List<Stroke> strokes) {
		final Note.PaperType t = Note.PaperType.VERTICAL_85X11;
		
		final int w = Note.PaperType.VERTICAL_85X11.width / 2;
		final int h = Note.PaperType.VERTICAL_85X11.height;
		
		float maxY = -t.yMax-1000;
		Log.d("PEN", Float.toString(maxY));
		for (Stroke s : strokes) {
			for (Point p : s.getPoly()) {
				if (p.y > maxY && p.x >= -w && p.x <= w) {
					maxY = p.y;
				}
			}
		}
		
		int lastPage = (int) ((maxY+t.yMax)/t.height + 1.0f);
		
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