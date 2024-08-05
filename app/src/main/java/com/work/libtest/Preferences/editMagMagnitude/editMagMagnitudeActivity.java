package com.work.libtest.Preferences.editMagMagnitude;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.work.libtest.MainActivity;
import com.work.libtest.Preferences.PreferencesActivity;
import com.work.libtest.R;

public class editMagMagnitudeActivity extends AppCompatActivity {

    EditText magMagnitudeEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_mag_magnitude);
        Toolbar toolbar = findViewById(R.id.toolbar);

        magMagnitudeEdit = findViewById(R.id.magneticDeviationEdit);
        magMagnitudeEdit.setText(Double.toString(MainActivity.preferences.getNominalMagnitude()));

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void magneticMagnitudeSubmit(View v) {
        MainActivity.preferences.setNominalMagnitude(Double.parseDouble(magMagnitudeEdit.getText().toString()));
        Intent intent = new Intent(this, PreferencesActivity.class);
        startActivity(intent);
    }

}
