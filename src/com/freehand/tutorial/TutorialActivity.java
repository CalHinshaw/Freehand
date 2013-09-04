package com.freehand.tutorial;

import com.calhounroberthinshaw.freehand.R;
import com.freehand.organizer.MainMenuActivity;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageSwitcher;
import android.widget.ImageView;

public class TutorialActivity extends Activity {
	
	private ImageSwitcher mSwitcher;
	private final Integer[] slides = { 	R.drawable.erase_button, R.drawable.folder };
	private int currentSlideIndex = 0;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setTitle("Freehand Tutorial");
        setContentView(R.layout.tutorial_layout);
        
        Button skipButton = (Button) this.findViewById(R.id.skip_tutorial_button);
        skipButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent startOrganizer = new Intent(getBaseContext(), MainMenuActivity.class);
				if (MainMenuActivity.isRunning() == false) {
					startActivity(startOrganizer);
					TutorialActivity.this.finish();
				} else {
					TutorialActivity.this.onBackPressed();
				}
				
			}
        });
        
        Button prevButton = (Button) this.findViewById(R.id.tutorial_previous_button);
        prevButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				updateSlide(-1);
			}
        });
        
        Button nextButton = (Button) this.findViewById(R.id.tutorial_next_button);
        nextButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				updateSlide(1);
			}
        });
        
        mSwitcher = (ImageSwitcher) this.findViewById(R.id.tutorial_slideshow);
        updateSlide(0);
        
        getSharedPreferences("freehand", Context.MODE_PRIVATE).edit().putBoolean("tutorialShown", true).commit();
	}
	
	private void updateSlide (int delta) {
		currentSlideIndex += delta;
		if (currentSlideIndex < 0) {
			currentSlideIndex = 0;
		} else if (currentSlideIndex >= slides.length) {
			currentSlideIndex = slides.length-1;
		}
		
        ImageView newSlide = new ImageView(this);
        newSlide.setImageResource(slides[currentSlideIndex]);
        
        if (mSwitcher.getChildCount() > 0) {
        	mSwitcher.removeViewAt(0);
        }
        
        mSwitcher.addView(newSlide);
        mSwitcher.showNext();
        
	}
}