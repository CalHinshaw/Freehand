package com.calhounhinshaw.freehandalpha.share;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.calhounhinshaw.freehandalpha.note_editor.Note;
import com.calhounhinshaw.freehandalpha.note_orginazion.INoteHierarchyItem;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class Sharer {
	public static void shareNoteHierarchyItemsAsJPEG (List<INoteHierarchyItem> toShare, Context context) {
		ArrayList<Note> notesToShare = new ArrayList<Note>(toShare.size());
		
		for (INoteHierarchyItem i : toShare) {
			notesToShare.add(new Note(i));
		}
		
		shareNotesAsJPEG(notesToShare, context);
	}
	
	public static void shareNotesAsJPEG (List<Note> toShare, Context context) {
		ArrayList<String> names = new ArrayList<String>(toShare.size());
		ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>(toShare.size());
		
		
		for (Note n : toShare) {
			
			// Add the name. If there's a duplicate append a number.
			if (!names.contains(n.getName())) {
				names.add(n.getName());
			} else {
				int duplicateCounter = 0;
				
				while(names.contains(n.getName())) {
					duplicateCounter++;
				}
				
				names.add(n.getName() + Integer.toString(duplicateCounter));
			}
			
			bitmaps.add(n.getBitmap());
		}
		
		try {
			File tempDir = new File(Environment.getExternalStorageDirectory(), "Freehand");
			tempDir.mkdirs();
			
			ArrayList<Uri> imageUris = new ArrayList<Uri>(toShare.size());
			
			for (int i = 0; i < toShare.size(); i++) {
				File tempFile = new File(tempDir, names.get(i) + ".jpeg");
				bitmaps.get(i).compress(CompressFormat.JPEG, 100, new FileOutputStream(tempFile));
				imageUris.add(Uri.fromFile(tempFile));
			}
			
			Intent shareIntent = new Intent();
			shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
			shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);
			shareIntent.setType("image/jpeg");
			context.startActivity(Intent.createChooser(shareIntent, "Share notes with..."));
			
		} catch (FileNotFoundException e) {
			Log.d("PEN", "Share Failed");
			e.printStackTrace();
		}
	}
}