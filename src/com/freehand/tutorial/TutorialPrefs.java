package com.freehand.tutorial;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class TutorialPrefs {
	private static Context c;
	
	public static void setContext (Context context) {
		c = context;
		
		// Debug code
		//c.getSharedPreferences("tutorial", Context.MODE_PRIVATE).edit().clear().commit();
	}
	
	public static void clear () {
		c = null;
	}
	
	public static SharedPreferences getPrefs() {
		if (c == null) {
			return null;
		} else {
			return c.getSharedPreferences("tutorial", Context.MODE_PRIVATE);
		}
	}
	
	public static void toast(String text) {
		if (c != null) {
			for (int i = 0; i < 2; i++) {
				Toast toast = Toast.makeText(c, text, Toast.LENGTH_LONG);
				LinearLayout toastLayout = (LinearLayout) toast.getView();
				TextView toastText = (TextView) toastLayout.getChildAt(0);
				toastText.setTextSize(18);
				toast.show();
			}
		}
	}
}