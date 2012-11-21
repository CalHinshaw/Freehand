package com.calhounhinshaw.freehandalpha.main_menu;

import com.calhounhinshaw.freehandalpha.R;

import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;

public class MainMenu extends Activity {
	
	NoteExplorer mExplorer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        
        mExplorer = (NoteExplorer) findViewById(R.id.noteExplorer);
        mExplorer.setRootDirectory(Environment.getExternalStoragePublicDirectory("Freehand"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        return true;
    }
    
    @Override
    public void onBackPressed() {
    	if (mExplorer.isInRootDirectory()) {
    		super.onBackPressed();
    	} else {
    		mExplorer.moveUpDirectory();
    	}
    }
}