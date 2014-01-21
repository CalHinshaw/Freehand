package com.freehand.pdf;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import android.os.Environment;
import android.util.Log;


public class PdfDoc {
	private static final String charset = "US-ASCII";
	
	private final List<PdfPage> pages = new ArrayList<PdfPage>();
	
	public void moveTo (final float x, final float y) {
		append(Float.toString(x));
		append(" ");
		append(Float.toString(curPage().height-y));
		append(" m\n");
	}
	
	public void lineTo (final float x, final float y) {
		append(Float.toString(x));
		append(" ");
		append(Float.toString(curPage().height-y));
		append(" l\n");
	}
	
	public void fill () {
		append("f\n");
	}
	
	public void setColor (final int color) {
		append(Float.toString(Color.red(color)/255.0f) + " ");
		append(Float.toString(Color.green(color)/255.0f) + " ");
		append(Float.toString(Color.blue(color)/255.0f) + " scn\n");
		
		final float alpha = Color.alpha(color)/255.0f;
		if (getAlpha() != alpha) {
			append("/Gs"+Integer.toString(curPage().alphas.size()) + " gs\n");
			curPage().alphas.add(alpha);
		}
	}
	
	public void newPage (final int width, final int height) {
		pages.add(new PdfPage(height, width));
		append("/DeviceRGB cs\n");
	}
	
	public boolean writePdf (final File dest) {
		try {
			final DataOutputStream w = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(dest)));
			
			final List<Integer> objOffsets = new ArrayList<Integer>();
			final List<Integer> pageObjectNumbers = new ArrayList<Integer>();
			
			int pagesObj = 0;
			for (PdfPage page : pages) {
				pagesObj += 3 + page.alphas.size();
			}
			
			// Write document header
			w.write("%PDF-1.3\n\n".getBytes(charset));
			
			for (PdfPage page : pages) {
				// Write alpha objects
				for (Float alpha : page.alphas) {
					objOffsets.add(w.size());
					final String s = Integer.toString(objOffsets.size()-1) + " 0 obj\n<<\n/Type /ExtGState\n/ca " + alpha + "\n>>\nendobj\n";
					w.write(s.getBytes(charset));
				}
				
				// Write resource object
				objOffsets.add(w.size());
				w.write((Integer.toString(objOffsets.size()-1) + " 0 obj\n<<\n/ExtGState\n<<\n").getBytes(charset));
				final int start = objOffsets.size()-1-page.alphas.size();
				for (int i = 0; i < page.alphas.size(); i++) {
					w.write(("/Gs"+i+" "+Integer.toString(start+i)+" 0 R\n").getBytes(charset));
				}
				w.write(">>\n>>\nendobj\n".getBytes());
				
				// Write Page contents
				objOffsets.add(w.size());
				w.write((Integer.toString(objOffsets.size()-1)+" 0 obj\n<<\n/Length "+page.stream.length()+"\n>>\nstream\n").getBytes(charset));
				w.write(page.stream.toString().getBytes(charset));
				w.write("endstream\nendobj\n".getBytes(charset));
				
				// Write Page object
				objOffsets.add(w.size());
				pageObjectNumbers.add(objOffsets.size()-1);
				w.write((Integer.toString(objOffsets.size()-1) + " 0 obj\n<<\n/Type /Page\n/Parent "+Integer.toString(pagesObj)+" 0 R\n").getBytes(charset));
				w.write(("/MediaBox [0 0 " + Float.toString(page.width) + " " + Float.toString(page.height) + "]\n").getBytes(charset));
				w.write(("/Resources "+Integer.toString(objOffsets.size()-3)+" 0 R\n").getBytes(charset));
				w.write(("/Contents "+Integer.toString(objOffsets.size()-2)+" 0 R\n").getBytes(charset));
				w.write(">>\nendobj\n".getBytes());
			}
			
			// Write Pages object
			objOffsets.add(w.size());
			w.write((Integer.toString(objOffsets.size()-1) + " 0 obj\n<<\n/Type /Pages\n/Count " + pageObjectNumbers.size() + "\n/Kids [").getBytes(charset));
			for (int i : pageObjectNumbers) {
				w.write((i + " 0 R ").getBytes(charset));
			}
			w.write("]\nendobj\n".getBytes(charset));
			
			// Write Catalog object
			objOffsets.add(w.size());
			w.write((Integer.toString(objOffsets.size()-1) + " 0 obj\n<<\n/Type /Catalog\n/Pages " + Integer.toString(objOffsets.size()-2) + " 0 R\n>>\nendobj\n").getBytes(charset));
			
			// Write xref
			final int startXref = w.size();
			w.write(("xref\n0 " + Integer.toString(objOffsets.size())+"\n").getBytes(charset));
			for (int offset : objOffsets) {
				w.write((String.format("%010d", offset)).getBytes(charset));
				w.write(" 00000 n\n".getBytes(charset));
			}
			
			// Write Trailer
			w.write("trailer\n<<\n".getBytes(charset));
			w.write(("/Size "+objOffsets.size()+" 0 R\n").getBytes(charset));
			w.write(("/Root " + Integer.toString(objOffsets.size()-2)+" 0 R\n").getBytes(charset));
			w.write((">>\nstartxref\n" + startXref + "\n%%EOF").getBytes(charset));
			
			w.close();
		} catch (IOException e) {
			Log.d("PEN", "write failed");
			e.printStackTrace();
		}
		return true;
	}
	
	private void append(final String s) {
		pages.get(pages.size()-1).stream.append(s);
	}
	
	private float getAlpha () {
		 if (curPage().alphas.size() < 1) {
			 return 1.0f;
		 } else {
			 return curPage().alphas.get(curPage().alphas.size()-1);
		 }
	}
	
	private PdfPage curPage() {
		return pages.get(pages.size()-1);
	}
	
	
	private static class PdfPage {
		public final StringBuilder stream = new StringBuilder();
		public final List<Float> alphas = new ArrayList<Float>();
		public final float height;
		public final float width;
		
		public PdfPage (final float height, final float width) {
			this.height = height;
			this.width = width;
		}
	}
	
	
	
	public static void test(final PdfDoc d) {
		d.newPage(1000, 1000);
		d.moveTo(100, 100);
		d.lineTo(200, 200);
		d.lineTo(100, 200);
		d.lineTo(100, 100);
		d.setColor(Color.BLUE);
		d.fill();
		
		d.moveTo(150, 100);
		d.lineTo(250, 200);
		d.lineTo(150, 200);
		d.lineTo(150, 100);
		d.setColor(0x99FF0000);
		d.fill();
		
		
		d.writePdf(new File(Environment.getExternalStorageDirectory(), "test.pdf"));
	}
}