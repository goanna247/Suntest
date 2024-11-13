package com.work.libtest.Preferences.editMaxMovementDeviation;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.work.libtest.MainActivity;
import com.work.libtest.Preferences.PreferencesActivity;
import com.work.libtest.R;

public class editMaxMovementDeviationActivity extends AppCompatActivity {

    EditText maxDeviation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_max_movement_deviation);
        Toolbar toolbar = findViewById(R.id.toolbar);

        maxDeviation = findViewById(R.id.magneticDeviationEdit);
//        maxDeviation.setText(Double.toString(MainActivity.preferences.getMovementMaximumDeviation()));

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void movementDeviationSubmit(View v) {
//        MainActivity.preferences.setMovementMaximumDeviation(Double.parseDouble(maxDeviation.getText().toString()));
        Intent intent = new Intent(this, PreferencesActivity.class);
        startActivity(intent);
    }
}
