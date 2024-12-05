package com.work.libtest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.ListView;
import android.widget.Toolbar;

public class ResumePrevious extends AppCompatActivity {

    public String TAG = "Resume Previous";
    public Menu menu;

    public String mDeviceName;
    public String mDeviceAddress;
    public String mConnectionStatus;

    public static final String EXTRA_DEVICE_NAME = "Device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "Device_address";
    public static final String EXTRA_DEVICE_CONNECTION_STATUS = "Connection_status";
    public static final String EXTRA_PROBE_MEASUREMENTS = "Measurements";
    
    private SurveyArrayAdapter surveyArrayAdapter;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resume_previous);
//        Toolbar toolbar = findViewById(R.id.toolbar);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        Log.i(TAG, "Name: " + mDeviceName + ", Address: " + mDeviceAddress);
    }
}