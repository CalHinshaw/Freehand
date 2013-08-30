package com.freehand.tutorial;

import com.calhounroberthinshaw.freehand.R;
import com.freehand.organizer.MainMenuActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.RelativeLayout.LayoutParams;

public class TutorialActivity extends Activity {
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setTitle("Freehand Tutorial");
        setContentView(R.layout.tutorial_layout);
        
        Gallery gallery = (Gallery) this.findViewById(R.id.tutorial_gallery);
        gallery.setAdapter(new ImageAdapter());

        Button skipButton = (Button) this.findViewById(R.id.skip_tutorial_button);
        skipButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				Intent startOrganizer = new Intent(getBaseContext(), MainMenuActivity.class);
				startActivity(startOrganizer);
			}
        	
        });
	}
	
	private class ImageAdapter extends BaseAdapter {
		private final Integer[] slides = { 	R.drawable.erase_button,
											R.drawable.folder };
		
		public int getCount() {
			return slides.length;
		}

		public Object getItem(int position) {
			return slides[position];
		}

		public long getItemId(int position) {
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView i = new ImageView(TutorialActivity.this);
			i.setImageResource(slides[position]);
			i.setLayoutParams(new Gallery.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			i.setScaleType(ImageView.ScaleType.FIT_CENTER);
			return i;
		}
		
	}
	
}