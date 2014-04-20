package com.freehand.preferences;

import android.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class PrefActivity extends Activity {
	@Override
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTitle("Settings");
		getFragmentManager().beginTransaction().replace(R.id.content, new PrefFragment(), "frag").commit();
	}
	
	
	@Override
    protected void onActivityResult (final int requestCode, final int resultCode, final Intent data) {
    	getFragmentManager().findFragmentByTag("frag").onActivityResult(requestCode, resultCode, data);
    	super.onActivityResult(requestCode, resultCode, data);
    }
}