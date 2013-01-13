package com.calhounhinshaw.freehandalpha.share;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class Sharer {
	public static void shareAsJPEG (List<Bitmap> toShare, List<String> names, Context context) {
		try {
			File tempDir = new File(Environment.getExternalStorageDirectory(), "Freehand");
			tempDir.mkdirs();
			
			ArrayList<Uri> imageUris = new ArrayList<Uri>(toShare.size());
			
			for (int i = 0; i < toShare.size(); i++) {
				File tempFile = new File(tempDir, names.get(i) + ".jpeg");
				toShare.get(i).compress(CompressFormat.JPEG, 100, new FileOutputStream(tempFile));
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