package com.freehand.share;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.freehand.editor.canvas.Note;
import com.freehand.ink.Point;
import com.freehand.ink.Stroke;
import com.freehand.pdf.PdfDoc;

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

public class PdfSharer extends AsyncTask<List<Object>, Object, Intent> {
	private Context mContext;
	private ProgressDialog mDialog;
	
	public PdfSharer (final Context newContext) {
		mContext = newContext;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		
		mDialog = new ProgressDialog(this.mContext, ProgressDialog.THEME_HOLO_LIGHT);
		mDialog.setProgressNumberFormat(null);
		mDialog.setTitle("Share as PDFs");
		mDialog.setMessage("Converting note(s) to PDF, sorry for the wait.");
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
		
		final ArrayList<Uri> pdfUris = new ArrayList<Uri>();
		for (int i = 0; i < noteList.size(); i++) {
			Note toShare;
			if (noteList.get(i) instanceof Note) {
				toShare = (Note) notes[i];
			} else if (noteList.get(i) instanceof String) {
				toShare = new Note((String) noteList.get(i));
			} else {
				continue;
			}
			
			final Uri uri = saveNoteAsPdf(toShare, rootDirectory);
			if (uri != null) {
				pdfUris.add(uri);
			}
			
			onProgressUpdate(progressIncrement*i);
		}
		
		//Debug.stopMethodTracing();
		
		if (pdfUris.isEmpty()) {
			return null;
		} else {
			Intent shareIntent = new Intent();
			shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
			shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, pdfUris);
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

	
	private static Uri saveNoteAsPdf (final Note note, final File rootDir) {
		if (note.getInkLayer().isEmpty()) {
			return null;
		}
		
		final PdfDoc d = new PdfDoc();
		
		if (note.getPaperType() != Note.PaperType.WHITEBOARD) {
			final RectF page = new RectF();
			for (int i = 0; i < getLastPage(note); i++) {
				page.set(-note.getPaperType().width/2, (int) (-note.getPaperType().yMax + i * note.getPaperType().height),
					note.getPaperType().width/2, (int) (-note.getPaperType().yMax + (i+1) * note.getPaperType().height));
				d.newPage(note.getPaperType().width, note.getPaperType().height);
				
				for (Stroke s : note.getInkLayer()) {
					if (RectF.intersects(page, s.getAABoundingBox()) == false) continue;
					
					final int xOffset = note.getPaperType().width/2;
					final int yOffset = (int) (note.getPaperType().yMax - i * note.getPaperType().height);
					d.moveTo(xOffset+s.getPoly().get(0).x, yOffset+s.getPoly().get(0).y);
					for (Point p : s.getPoly()) { 
						d.lineTo(xOffset + p.x, yOffset + p.y);
					}
					d.setColor(s.getColor());
					d.fill();
				}
			}
		} else {
			final RectF aabb = getAABB(note);
			d.newPage((int) aabb.width(), (int) aabb.height());
			
			for (Stroke s : note.getInkLayer()) {
				d.moveTo(s.getPoly().get(0).x-aabb.left, s.getPoly().get(0).y-aabb.top);
				for (Point p : s.getPoly()) { 
					d.lineTo(p.x-aabb.left, p.y-aabb.top);
				}
				d.setColor(s.getColor());
				d.fill();
			}
		}
		
		final String noteName = (new File(note.getPath()).getName()).replace(".note", "");
		final File dest = new File(rootDir, noteName+".pdf");
		
		if (d.writePdf(dest)) {
			return Uri.fromFile(dest);
		} else {
			return null;
		}
	}
	
	private static int getLastPage (final Note note) {
		final Note.PaperType t = note.getPaperType();
		
		float maxY = -t.yMax-1000;
		for (Stroke s : note.getInkLayer()) {
			for (Point p : s.getPoly()) {
				if (p.y > maxY && p.x >= -t.width && p.x <= t.width) {
					maxY = p.y;
				}
			}
		}
		
		return (int) ((maxY+t.yMax)/t.height + 1.0f);
	}
	
	private static RectF getAABB (final Note note) {
		final RectF aabb = new RectF();
		for (Stroke s : note.getInkLayer()) {
			aabb.union(s.getAABoundingBox());
		}
		aabb.left -= 150;
		aabb.right += 150;
		aabb.top -= 150;
		aabb.bottom += 150;
		
		return aabb;
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