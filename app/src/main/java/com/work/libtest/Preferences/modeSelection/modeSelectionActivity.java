package com.work.libtest.Preferences.modeSelection;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.work.libtest.MainActivity;
import com.work.libtest.Preferences.PreferencesActivity;
import com.work.libtest.R;

public class modeSelectionActivity extends AppCompatActivity {
    private static final String TAG = "MODE SELECTION:";

    Spinner spinnerModes;
    ArrayAdapter<CharSequence> adapter;

    public static final String EXTRA_DEVICE_NAME = "Device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "Device_address";
    public static final String EXTRA_DEVICE_CONNECTION_STATUS = "Connection_status";

    private String mDeviceName;
    private String mDeviceAddress;
    private String mDeviceConnectionStatus;

    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode_selection);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        spinnerModes = findViewById(R.id.modeSelectionSpinner);
        adapter = ArrayAdapter.createFromResource(this, R.array.mode_selection_values, android.R.layout.simple_spinner_item);


        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerModes.setAdapter(adapter);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        mDeviceConnectionStatus = intent.getStringExtra(EXTRA_DEVICE_CONNECTION_STATUS);

    }


    public void onSubmit(View v) {
        if (spinnerModes.getSelectedItem().toString().equals("Core Orientation (Dual)")) {
//            MainActivity.preferences.setMode("Core Orientation (Dual)");

        } else if (spinnerModes.getSelectedItem().toString().equals("Bore Orientation (Single)")) {
//            MainActivity.preferences.setMode("Bore Orientation (Single)");

        } else {
            Log.d(TAG, "Error, selected item is not an option");
        }
        back();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_preferences, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.sensor_back_button) {
            Log.d(TAG, "exit save data activity to go back to sensor activity");
            back();
        }
//        switch (item.getItemId()) {
//            case R.id.sensor_back_button:
//                Log.d(TAG, "exit save data activity to go back to sensor activity");
//                back();
//                return true;
//        }
        return true;
    }

    private void back() {
        Intent intent = new Intent(this, PreferencesActivity.class);
        intent.putExtra(PreferencesActivity.EXTRA_DEVICE_NAME, mDeviceName);
        intent.putExtra(PreferencesActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(PreferencesActivity.EXTRA_DEVICE_CONNECTION_STATUS, mDeviceConnectionStatus);
        startActivity(intent);
    }
}
