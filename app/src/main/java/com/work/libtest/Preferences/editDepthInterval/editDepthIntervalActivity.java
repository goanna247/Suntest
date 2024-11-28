package com.work.libtest.Preferences.editDepthInterval;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.work.libtest.MainActivity;
import com.work.libtest.Preferences.PreferencesActivity;
import com.work.libtest.R;

public class editDepthIntervalActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE_NAME = "Device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "Device_address";

    EditText depthIntervalEdit;

    String mDeviceName = "";
    String mDeviceAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_depth_interval);
        Toolbar toolbar = findViewById(R.id.toolbar);

        depthIntervalEdit = findViewById(R.id.magneticDeviationEdit);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);

//        depthIntervalEdit.setText(Double.toString(MainActivity.preferences.getDepthInterval()));

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void depthIntervalSubmit(View v) {
//        MainActivity.preferences.setDepthInterval(Double.parseDouble(depthIntervalEdit.getText().toString()));
        Intent intent = new Intent(this, PreferencesActivity.class);
        startActivity(intent);
    }

}
