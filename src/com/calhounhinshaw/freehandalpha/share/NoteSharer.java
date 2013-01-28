package com.calhounhinshaw.freehandalpha.share;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.calhounhinshaw.freehandalpha.note_editor.Note;
import com.calhounhinshaw.freehandalpha.note_orginazion.INoteHierarchyItem;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

public class NoteSharer extends AsyncTask<List<INoteHierarchyItem>, Integer, Intent> {
	
	
	
	
	
	
	
	
	@Override
	protected Intent doInBackground(List<INoteHierarchyItem>... params) {
		
		// Make sure we have a list of hierarchy items
		if (params.length < 1) {
			return null;
		}
		
		List<INoteHierarchyItem> itemsToShare = params[0];
		ArrayList<Uri> imageUris = new ArrayList<Uri>();
		
		File rootDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath().concat("temp/Freehand"));
		
		
		for (INoteHierarchyItem item : itemsToShare) {
			imageUris.addAll((new Note(item)).getJpegUris(rootDirectory));
		}
		
		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
		shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);
		shareIntent.setType("image/jpeg");
		
		return shareIntent;
	}
	
	
	
	
	
	
	
	
//	public static boolean shareNoteHierarchyItemsAsJPEG (List<INoteHierarchyItem> toShare, Context context) {
//		ArrayList<Note> notesToShare = new ArrayList<Note>(toShare.size());
//		
//		for (INoteHierarchyItem i : toShare) {
//			notesToShare.add(new Note(i));
//		}
//		
//		return shareNotesAsJPEG(notesToShare, context);
//	}
//	
//	public static boolean shareNotesAsJPEG (List<Note> toShare, Context context) {
//		ArrayList<String> names = new ArrayList<String>(toShare.size());
//		ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>(toShare.size());
//		
//		
//		for (Note n : toShare) {
//			Bitmap tempBitmap = null;
//			String tempString = null;
//			
//			// Add the name. If there's a duplicate append a number.
//			if (!names.contains(n.getName())) {
//				names.add(n.getName());
//			} else {
//				int duplicateCounter = 0;
//				
//				while(names.contains(n.getName())) {
//					duplicateCounter++;
//				}
//				
//				tempString = n.getName() + Integer.toString(duplicateCounter);
//				
//			}
//			
//			tempBitmap = n.getBitmap();
//			if (tempBitmap != null) {
//				names.add(tempString);
//				bitmaps.add(n.getBitmap());
//			}
//			
//		}
//		
//		if (bitmaps.size() > 0) {
//			try {
//				File tempDir = new File(Environment.getExternalStorageDirectory(), "Freehand");
//				tempDir.mkdirs();
//				
//				ArrayList<Uri> imageUris = new ArrayList<Uri>(toShare.size());
//				
//				for (int i = 0; i < toShare.size(); i++) {
//					File tempFile = new File(tempDir, names.get(i) + ".jpeg");
//					bitmaps.get(i).compress(CompressFormat.JPEG, 100, new FileOutputStream(tempFile));
//					imageUris.add(Uri.fromFile(tempFile));
//				}
//				
//				Intent shareIntent = new Intent();
//				shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
//				shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);
//				shareIntent.setType("image/jpeg");
//				context.startActivity(Intent.createChooser(shareIntent, "Share notes with..."));
//				
//				return true;
//			} catch (FileNotFoundException e) {
//				Log.d("PEN", "Share Failed");
//				e.printStackTrace();
//			}
//		} 
//		
//		return false;
//	}











}