package com.e.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pd = new ProgressDialog(MainActivity.this);
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        pd.getWindow().setGravity(Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL);
        TextView tv = new TextView(this);
        tv.setTextColor(Color.BLACK);
        tv.setTextSize(20);
        tv.setText("Waiting...");
        pd.setCustomTitle(tv);
        pd.setIndeterminate(false);
        pd.show();
    }
}
