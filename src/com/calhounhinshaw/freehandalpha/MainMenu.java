package com.calhounhinshaw.freehandalpha;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class MainMenu extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        return true;
    }
}
