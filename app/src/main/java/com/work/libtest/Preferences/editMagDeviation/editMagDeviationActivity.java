package com.work.libtest.Preferences.editMagDeviation;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.work.libtest.MainActivity;
import com.work.libtest.Preferences.PreferencesActivity;
import com.work.libtest.R;

public class editMagDeviationActivity extends AppCompatActivity {

    EditText magneticDeviation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_mag_deviation);
        Toolbar toolbar = findViewById(R.id.toolbar);

        magneticDeviation = findViewById(R.id.magneticDeviationEdit);
//        magneticDeviation.setText(Double.toString(MainActivity.preferences.getMagneticMaximumDeviation()));

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void magneticDeviationSubmit(View v) {
//        MainActivity.preferences.setMagneticMaximumDeviation(Double.parseDouble(magneticDeviation.getText().toString()));
        Intent intent = new Intent(this, PreferencesActivity.class);
        startActivity(intent);
    }
}
