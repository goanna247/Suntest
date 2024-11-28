package com.work.libtest.Preferences.editRoll;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.work.libtest.MainActivity;
import com.work.libtest.Preferences.Preferences;
import com.work.libtest.Preferences.PreferencesActivity;
import com.work.libtest.R;

public class editRollActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE_NAME = "Device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "Device_address";

    EditText rollEdit;

    String mDeviceName = "";
    String mDeviceAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_roll);
        Toolbar toolbar = findViewById(R.id.toolbar);

        rollEdit = findViewById(R.id.magneticDeviationEdit);
//        rollEdit.setText(Double.toString(MainActivity.preferences.getRoll()));

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void rollSubmit(View v) {
//        MainActivity.preferences.setRoll(Double.parseDouble(rollEdit.getText().toString()));
        Intent intent = new Intent(this, PreferencesActivity.class);
        intent.putExtra(PreferencesActivity.EXTRA_DEVICE_NAME, mDeviceName);
        intent.putExtra(PreferencesActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        startActivity(intent);
    }
}
