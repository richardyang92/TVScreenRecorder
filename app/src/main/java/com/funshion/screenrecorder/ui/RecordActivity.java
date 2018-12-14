package com.funshion.screenrecorder.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

import com.funshion.screenrecorder.R;

public class RecordActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.layout_record);
    }
}
