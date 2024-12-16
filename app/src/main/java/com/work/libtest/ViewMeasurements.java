package com.work.libtest;

import android.content.Intent;
import android.icu.util.Measure;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.work.libtest.SurveyOptions.AllSurveyOptionsActivity;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ViewMeasurements extends AppCompatActivity {
    //pass in device name, address, connection status, measurement array
    // https://javapapers.com/android/android-listview-custom-layout-tutorial/

    public String TAG = "View Measurements";
    public Menu menu;

    public String mDeviceName;
    public String mDeviceAddress;
    public String mConnectionStatus;

    public static final String EXTRA_DEVICE_NAME = "Device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "Device_address";
    public static final String EXTRA_DEVICE_CONNECTION_STATUS = "Connection_status";
    public static final String EXTRA_PROBE_MEASUREMENTS = "Measurements";
    public static final String EXTRA_SURVEY_TICKET = "Survey_Ticket";
    public static final String EXTRA_PARENT_ACTIVITY = "Parent_activity";
    private int surveyTicket;

    public static final String EXTRA_PREV_DEPTH = "Prev depth";
    public static final String EXTRA_NEXT_DEPTH = "Next depth";

    private String mPrevDepth;
    private String mNextDepth;

    private ListView mMeasurementsList;
    private String mMeasurementNumber;
    private String mMeausrementData;

    private ArrayList<String> rawProbeDataOg = new ArrayList<>();
    private ArrayList<String> timeData = new ArrayList<>();
    private ArrayList<String> dateData = new ArrayList<>();
    private ArrayList<String> tempData = new ArrayList<>();
    private ArrayList<String> depthDataN = new ArrayList<>();

    private ArrayList<String> rollData = new ArrayList<>();
    private ArrayList<String> dipData = new ArrayList<>();
    private ArrayList<String> azimuthData = new ArrayList<>();

    public static ArrayList<ProbeData> probeData = new ArrayList<ProbeData>();

    List<String> rawProbeData;
    List<String> timeData2;

    public static ArrayList<ProbeData> fullProbeData = new ArrayList<ProbeData>();

    int numberOfMeasurements = 0;

    private MeasurementArrayAdapter measurementArrayAdapter;
    private ListView listView;

    private String mParentActivity = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_measurements);
        Toolbar toolbar = findViewById(R.id.toolbar);

        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        Log.i(TAG, "Name: " + mDeviceName + ", Address: " + mDeviceAddress);
        try {
            surveyTicket = Integer.valueOf(intent.getStringExtra(EXTRA_SURVEY_TICKET));
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown receiving survey ticket: " + e);
        }

        mParentActivity = intent.getStringExtra(EXTRA_PARENT_ACTIVITY);


        LinkedList<Measurement> savedProbeData = new LinkedList<>();
        LinkedList<DetailedMeasurement> savedDetailedProbeData = new LinkedList<>();

        try {
            if (mParentActivity != null) {
                if (mParentActivity.equals("Resume")) { //if we are resuming a previous activity we need to load the stored values from Global using the ticket number
                    try {
                        savedDetailedProbeData = Globals.storedMeasurements.get(surveyTicket);
                        for (int i = 0; i < Globals.storedMeasurements.get(surveyTicket).size(); i++) {
                            savedProbeData.add(Globals.storedMeasurements.get(surveyTicket).get(i).getBasicMeasurement());
                        }
                        TakeMeasurements.recordedShots = savedProbeData;
                        TakeMeasurements.detailedRecordedShots = savedDetailedProbeData;

                    } catch (Exception e) {
                        Log.e(TAG, "Exception thrown in getting saved probe data: " + e);
                    }
                } else {
                    savedProbeData = TakeMeasurements.recordedShots;
                    savedDetailedProbeData = TakeMeasurements.detailedRecordedShots;
                }
            } else {
                savedProbeData = TakeMeasurements.recordedShots;
                savedDetailedProbeData = TakeMeasurements.detailedRecordedShots;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown in getting previous measurement data: " + e);
        }

        listView = (ListView) findViewById(R.id.listView);
        measurementArrayAdapter = new MeasurementArrayAdapter(getApplicationContext(), R.layout.listview_row_layout);
        listView.setAdapter(measurementArrayAdapter);

        for (int i = 0; i < savedProbeData.size(); i++) {
            DecimalFormat numberFormat = new DecimalFormat("#.0000");
//            String measurementName = savedProbeData.get(i).getName(); //needs to not be the record number but the measurement number
            String measurementName = savedDetailedProbeData.get(i).getDetailedName();
            String date = savedProbeData.get(i).getDate();
            String time = savedProbeData.get(i).getTime();
            String depth = savedProbeData.get(i).getDepth();
            String roll = savedProbeData.get(i).getRoll();
            String dip = savedProbeData.get(i).getDip();
            String azimuth = savedProbeData.get(i).getAzimuth();
            String temp = savedProbeData.get(i).getTemp();
            Measurement measurement = new Measurement(measurementName, date, time, temp, depth, dip, roll, azimuth); // i feel like this is a bad way of doing things...
            measurementArrayAdapter.add(measurement);
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    Log.e(TAG, "Going to orientation activity on position: " + position);
                    goToOrientation(position);
                } catch (Exception e) {
                    Log.e(TAG, "Exception thrown: " + e);
                }
            }
        });

        setSupportActionBar(toolbar);
    }

    //Will need slight reworking
    private void goToOrientation(int position) {
        Intent intent = new Intent(this, OrientationActivity.class);
        intent.putExtra(OrientationActivity.EXTRA_DEVICE_NAME, mDeviceName);
        intent.putExtra(OrientationActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(OrientationActivity.EXTRA_PARENT_ACTIVITY, "MEASUREMENT");
        String positionToSend = String.valueOf(position);
        intent.putExtra(OrientationActivity.EXTRA_MEASUREMENT_DATA, positionToSend);
        intent.putExtra(OrientationActivity.EXTRA_SURVEY_TICKET, surveyTicket);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_view, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.back_button) {
            Log.d(TAG, "back button pressed");
            back();
            return true;
        } else if (item.getItemId() == R.id.remove_button) {
            removeAll();
        } else if (item.getItemId() == R.id.fetch_button) {
            fetch();
        }
        return true;
    }

    private void removeAll() {
        LinkedList<Measurement> emptyList = new LinkedList<>();
        TakeMeasurements.recordedShots = emptyList;
        measurementArrayAdapter = new MeasurementArrayAdapter(getApplicationContext(), R.layout.listview_row_layout);
        listView.setAdapter(measurementArrayAdapter);
    }

    private void back() {
        Intent backIntent = new Intent(this, TakeMeasurements.class);

        backIntent.putExtra(TakeMeasurements.EXTRA_DEVICE_NAME, mDeviceName);
        backIntent.putExtra(TakeMeasurements.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        backIntent.putExtra(TakeMeasurements.EXTRA_DEVICE_CONNECTION_STATUS, mConnectionStatus);
        backIntent.putExtra(TakeMeasurements.EXTRA_PREV_DEPTH, mPrevDepth);
        backIntent.putExtra(TakeMeasurements.EXTRA_NEXT_DEPTH, mNextDepth);
        backIntent.putExtra(TakeMeasurements.EXTRA_SURVEY_TICKET, surveyTicket);
        startActivity(backIntent);
    }


    private void fetch() {
        Intent saveIntent = new Intent(this, SaveData.class);
        saveIntent.putExtra(SaveData.EXTRA_DEVICE_NAME, mDeviceName);
        saveIntent.putExtra(SaveData.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        saveIntent.putExtra(SaveData.EXTRA_PARENT_ACTIVITY, "View");
        startActivity(saveIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}