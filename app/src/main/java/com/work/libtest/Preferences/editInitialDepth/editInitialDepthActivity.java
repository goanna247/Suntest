package com.work.libtest.Preferences.editInitialDepth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.work.libtest.MainActivity;
import com.work.libtest.Preferences.PreferencesActivity;
import com.work.libtest.R;

public class editInitialDepthActivity extends AppCompatActivity {

    EditText initialDepthEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_initial_depth);
        Toolbar toolbar = findViewById(R.id.toolbar);

        initialDepthEdit = findViewById(R.id.magneticDeviationEdit);
        initialDepthEdit.setText(Double.toString(MainActivity.preferences.getInitialDepth()));


        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    //TODO input verification

    public void initalDepthSubmit(View v) {
        MainActivity.preferences.setInitialDepth(Double.parseDouble(initialDepthEdit.getText().toString()));
        Intent intent = new Intent(this, PreferencesActivity.class);
        startActivity(intent);
    }

}
