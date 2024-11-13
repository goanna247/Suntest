package com.work.libtest.Preferences.editDip;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.work.libtest.MainActivity;
import com.work.libtest.Preferences.PreferencesActivity;
import com.work.libtest.R;

public class editDipActivity extends AppCompatActivity {

    EditText dipEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_dip);
        Toolbar toolbar = findViewById(R.id.toolbar);

        dipEdit = findViewById(R.id.magneticDeviationEdit);
//        dipEdit.setText(Double.toString(MainActivity.preferences.getDip()));


        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void dipSubmit(View v) {
//        MainActivity.preferences.setDip(Double.parseDouble(dipEdit.getText().toString()));
        Intent intent = new Intent(this, PreferencesActivity.class);
        startActivity(intent);
    }
}
