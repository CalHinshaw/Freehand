package com.freehand.preferences;

import android.R;
import android.app.Activity;
import android.os.Bundle;

public class PrefActivity extends Activity {
	@Override
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTitle("Settings");
		getFragmentManager().beginTransaction().replace(R.id.content, new PrefFragment()).commit();
	}
}