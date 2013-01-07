package com.calhounhinshaw.freehandalpha.main_menu;

import java.io.File;

import com.calhounhinshaw.freehandalpha.R;

import com.calhounhinshaw.freehandalpha.note_orginazion.INoteHierarchyItem;
import com.calhounhinshaw.freehandalpha.note_orginazion.NoteFileHierarchyItem;


import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.Menu;

public class MainMenu extends Activity {
	
	NoteExplorer mExplorer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        
        // Gather resources needed to create the NoteFileHierarchyItem that's getting passed to mExplorer
        File rootDirectory = Environment.getExternalStoragePublicDirectory("Freehand");
        Drawable defaultNoteDrawable = this.getResources().getDrawable(R.drawable.pencil);
        Drawable defaultFileDrawable = this.getResources().getDrawable(R.drawable.folder);
        
        INoteHierarchyItem rootItem = new NoteFileHierarchyItem(rootDirectory, defaultNoteDrawable, defaultFileDrawable);
        
        // Starts NoteExplorer in the app's root directory and set it's INoteHierarchyItem
        mExplorer = (NoteExplorer) findViewById(R.id.noteExplorer);
        mExplorer.setRootHierarchyItem(rootItem);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        return true;
    }
    
    // This method overrides the back button to let users navigate through folders more easily
    @Override
    public void onBackPressed() {
    	if(mExplorer.directoryHasSelected()) {			//first clear selections
    		mExplorer.clearDirectorySelections();
    	} else if (mExplorer.isInRootDirectory()) {		// then close folders
    		super.onBackPressed();
    	} else {										// finally, close app
    		mExplorer.moveUpDirectory();
    	}
    }
}