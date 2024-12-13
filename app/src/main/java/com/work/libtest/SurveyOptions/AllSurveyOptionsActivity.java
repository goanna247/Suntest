package com.work.libtest.SurveyOptions;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.work.libtest.CoreMain;
import com.work.libtest.MainActivity;
import com.work.libtest.R;
import com.work.libtest.Survey;
import com.work.libtest.TakeMeasurements;

public class AllSurveyOptionsActivity extends AppCompatActivity {

    public String TAG = "Survey Options";

    private String mDeviceName;
    private String mDeviceAddress;

    public static final String EXTRA_DEVICE_NAME = "Device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "Device_address";
    public static final String EXTRA_MEASUREMENT_TYPE = "Continue_or_new";
    public static final String EXTRA_SURVEY_TICKET = "Survey_Ticket";

    public static final String EXTRA_PARENT = "";
    public static final String EXTRA_BLACK_NAME = "black_name";
    public static final String EXTRA_BLACK_ADDRESS = "black_address";
    public static final String EXTRA_WHITE_NAME = "white_name";
    public static final String EXTRA_WHITE_ADDRESS = "white_address";


    private int surveyTicket;

    int resumePosition = 128; //error code!

    EditText holeIDInput;
    EditText operatorName;
    EditText companyName;
    EditText initialDepth;
    EditText depthInterval;

    TextView errorMessage;

    private String mBlackName;
    private String mBlackAddress;
    private String mWhiteName;
    private String mWhiteAddress;
    private String mParentActivity;

    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_survey_options);
        Toolbar toolbar = findViewById(R.id.toolbar);

        holeIDInput = findViewById(R.id.holeIDInput);
        operatorName = findViewById(R.id.operatorNameInput);
        companyName = findViewById(R.id.companyNameInput);
        initialDepth = findViewById(R.id.initialDepthInput);
        depthInterval = findViewById(R.id.depthIntervalInput);

        errorMessage = findViewById(R.id.errorMessage);

        errorMessage.setVisibility(View.INVISIBLE);

        final Intent intent = getIntent();
        mParentActivity = intent.getStringExtra(EXTRA_PARENT);
        if (mParentActivity.equals("CoreMain")) {
            mBlackName = intent.getStringExtra(EXTRA_BLACK_NAME);
            mBlackAddress = intent.getStringExtra(EXTRA_BLACK_ADDRESS);
            mWhiteName = intent.getStringExtra(EXTRA_WHITE_NAME);
            mWhiteAddress = intent.getStringExtra(EXTRA_WHITE_ADDRESS);
        } else {
            mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
            mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        }
        try {
            surveyTicket = Integer.valueOf(intent.getStringExtra(EXTRA_SURVEY_TICKET));
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown receiving survey ticket: " + e);
        }

        Log.e(TAG, "PASSED IN VALUE: " + intent.getStringExtra(EXTRA_MEASUREMENT_TYPE));
        try {
            if (intent.getStringExtra(EXTRA_MEASUREMENT_TYPE).equals(null)) {
                if (!intent.getStringExtra(EXTRA_MEASUREMENT_TYPE).equals("NEW")) {
                    try {
                        resumePosition = Integer.valueOf(intent.getStringExtra(EXTRA_MEASUREMENT_TYPE));
                    } catch (Exception e) {
                        Log.e(TAG, "Cannot set position of measurement to be resumed: " + e);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown: " + e);
        }

        try {

            if (MainActivity.surveys.size() > 0) {
                Log.d(TAG, MainActivity.surveys.get(0).getSurveyOptions().toString());
                if (resumePosition != 128) {
                    holeIDInput.setText(MainActivity.surveys.get(resumePosition).getSurveyOptions().getHoleID());
                    operatorName.setText(MainActivity.surveys.get(resumePosition).getSurveyOptions().getOperatorName());
                    companyName.setText(MainActivity.surveys.get(resumePosition).getSurveyOptions().getCompanyName());
                    try {
                        initialDepth.setText((int) MainActivity.surveys.get(resumePosition).getSurveyOptions().getInitialDepth());
                        depthInterval.setText((int) MainActivity.surveys.get(resumePosition).getSurveyOptions().getDepthInterval());
                    } catch (Exception e) {
                        Log.e(TAG, "Exception thrown in getting the initial depth and depth interval: " + e);
                    }
                } else {
                    holeIDInput.setText(MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().getHoleID());
                    operatorName.setText(MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().getOperatorName());
                    companyName.setText(MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().getCompanyName());
                    try {
                        initialDepth.setText((int) MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().getInitialDepth());
                        depthInterval.setText((int) MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().getDepthInterval());
                    } catch (Exception e) {
                        Log.e(TAG, "Exception thrown in getting the initial depth and depth interval: " + e);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown in OnCreate in AllSurveyOptionsActivity: " + e);
        }
        setSupportActionBar(toolbar);
    }

    public void allSurveyOptionsSubmit(View v) {
        try {
            if (resumePosition != 128) {
                Log.d(TAG, "Hole ID: " + holeIDInput.getText().toString() + " Operator Name: " + operatorName.getText().toString() + " Company Name: " + companyName.getText().toString());//ADD BACK IN + " Initial depth: " + initialDepth.getText().toString() + " Depth Interval: " + depthInterval.getText().toString());
                if (MainActivity.surveySize > 0) {
                    MainActivity.surveys.get(resumePosition).getSurveyOptions().setHoleID(holeIDInput.getText().toString());
                    MainActivity.surveys.get(resumePosition).getSurveyOptions().setOperatorName(operatorName.getText().toString());
                    MainActivity.surveys.get(resumePosition).getSurveyOptions().setCompanyName(companyName.getText().toString());
                    try {
                        if (initialDepth.getText().toString() == null || initialDepth.getText().toString().equals("")) {
                            MainActivity.surveys.get(resumePosition).getSurveyOptions().setInitialDepth(0); //default value is 0 meters, TODO - let this be set in the preferences as a default
                        } else {
                            MainActivity.surveys.get(resumePosition).getSurveyOptions().setInitialDepth(Double.parseDouble(initialDepth.getText().toString()));
                        }
                        if (depthInterval.getText().toString() == null || depthInterval.getText().toString().equals("") ) {
                            MainActivity.surveys.get(resumePosition).getSurveyOptions().setDepthInterval(5); //default value is 5 meters per shot taken, TODO - let this be set in the preferences as a default
                        } else {
                            MainActivity.surveys.get(resumePosition).getSurveyOptions().setDepthInterval(Double.parseDouble(depthInterval.getText().toString()));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Exception in 1: " + e);
                    }
                } else {
                    SurveyOptions newSurveyOptions;
                    try {
                        if (initialDepth.getText().toString() == null || initialDepth.getText().toString().equals("") || depthInterval.getText().toString() == null || depthInterval.getText().toString().equals("")) {
                            newSurveyOptions = new SurveyOptions(holeIDInput.getText().toString(), operatorName.getText().toString(), companyName.getText().toString(), Double.parseDouble(initialDepth.getText().toString()), Double.parseDouble(depthInterval.getText().toString()));
                        } else {
                            newSurveyOptions = new SurveyOptions(holeIDInput.getText().toString(), operatorName.getText().toString(), companyName.getText().toString(), Double.parseDouble(initialDepth.getText().toString()), Double.parseDouble(depthInterval.getText().toString()));
                        }
                        Survey newSurvey = new Survey(newSurveyOptions);
                    } catch (Exception e) {
                        Log.e(TAG, "Exception in 2: " + e);
                    }

//                    MainActivity.surveys.add(newSurvey);
//                    MainActivity.surveySize++;
                }

                if (!holeIDInput.getText().toString().equals("") && !operatorName.getText().toString().equals("") && !companyName.getText().toString().equals("")) {// && !initialDepth.getText().toString().equals("") && !depthInterval.getText().toString().equals("")) {
                    //move to taking measurements
                    Intent measurements = new Intent(this, TakeMeasurements.class);
                    Log.e(TAG, "Name: " + mDeviceName + ", Address: " + mDeviceAddress);
                    measurements.putExtra(TakeMeasurements.EXTRA_DEVICE_NAME, mDeviceName);
                    measurements.putExtra(TakeMeasurements.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
                    measurements.putExtra(TakeMeasurements.EXTRA_DEVICE_CONNECTION_STATUS, "Connected");
                    measurements.putExtra(TakeMeasurements.EXTRA_SURVEY_TICKET, String.valueOf(surveyTicket));
                    startActivity(measurements);
                }
            } else {
                try {
//                    Log.d(TAG, "Hole ID: " + holeIDInput.getText().toString() + " Operator Name: " + operatorName.getText().toString() + " Company Name: " + companyName.getText().toString() + " Initial depth: " + initialDepth.getText().toString() + " Depth Interval: " + depthInterval.getText().toString());
                    if (MainActivity.surveySize > 0) {
                        MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().setHoleID(holeIDInput.getText().toString());
                        MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().setOperatorName(operatorName.getText().toString());
                        MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().setCompanyName(companyName.getText().toString());

                        if (initialDepth.getText().toString() == null || initialDepth.getText().toString().equals("")) {
                            MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().setInitialDepth(0); //default value is 0 meters, TODO - let this be set in the preferences as a default
                        } else {
                            MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().setInitialDepth(Double.parseDouble(initialDepth.getText().toString()));
                        }
                        if (depthInterval.getText().toString() == null || depthInterval.getText().toString().equals("") ) {
                            MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().setDepthInterval(5); //default value is 5 meters per shot taken, TODO - let this be set in the preferences as a default
                        } else {
                            MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().setDepthInterval(Double.parseDouble(depthInterval.getText().toString()));
                        }
    //                    MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().setInitialDepth(Double.parseDouble(initialDepth.getText().toString()));
    //                    MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().setDepthInterval(Double.parseDouble(depthInterval.getText().toString()));
                    } else {
                        SurveyOptions newSurveyOptions;
                        if (initialDepth.getText().toString() == null || initialDepth.getText().toString().equals("") || depthInterval.getText().toString() == null ||  depthInterval.getText().toString().equals("")) {
                            newSurveyOptions = new SurveyOptions(holeIDInput.getText().toString(), operatorName.getText().toString(), companyName.getText().toString(),0, 5);
                        } else {
                            newSurveyOptions = new SurveyOptions(holeIDInput.getText().toString(), operatorName.getText().toString(), companyName.getText().toString(), Double.parseDouble(initialDepth.getText().toString()), Double.parseDouble(depthInterval.getText().toString()));
                        }

                        Survey newSurvey = new Survey(newSurveyOptions);
                        MainActivity.surveys.add(newSurvey);
                        MainActivity.surveySize++;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception thrown in 3: " + e);
//                    MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().setInitialDepth(0);
//                    MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().setDepthInterval(5);
                }

                try {

                    if (!holeIDInput.getText().toString().equals("") && !operatorName.getText().toString().equals("") && !companyName.getText().toString().equals("")) {// && !initialDepth.getText().toString().equals("") && !depthInterval.getText().toString().equals("")) {
                        //move to taking measurements
                        if (resumePosition != 128) {
                            Intent measurements = new Intent(this, TakeMeasurements.class);
                            measurements.putExtra(TakeMeasurements.EXTRA_DEVICE_NAME, mDeviceName);
                            measurements.putExtra(TakeMeasurements.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
                            measurements.putExtra(TakeMeasurements.EXTRA_DEVICE_CONNECTION_STATUS, "Connected");
                            measurements.putExtra(TakeMeasurements.EXTRA_MEASUREMENT_TYPE, Integer.toString(resumePosition));
                            measurements.putExtra(TakeMeasurements.EXTRA_SURVEY_TICKET, String.valueOf(surveyTicket));
                            startActivity(measurements);
                        } else {
                            Intent measurements = new Intent(this, TakeMeasurements.class);
                            measurements.putExtra(TakeMeasurements.EXTRA_DEVICE_NAME, mDeviceName);
                            measurements.putExtra(TakeMeasurements.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
                            measurements.putExtra(TakeMeasurements.EXTRA_DEVICE_CONNECTION_STATUS, "Connected");
                            measurements.putExtra(TakeMeasurements.EXTRA_MEASUREMENT_TYPE, "NEW");
                            measurements.putExtra(TakeMeasurements.EXTRA_SURVEY_TICKET, String .valueOf(surveyTicket));
                            startActivity(measurements);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception thrown in 4: " + e);
                }
            }
        } catch (Exception e) {
            errorMessage.setText("Option error, ensure all types are correct");
            errorMessage.setVisibility(View.VISIBLE);
            Log.d(TAG, "survey options cannot be submitted: " + e);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        Log.d(TAG, "Device Name: " + mDeviceName + ", Device Address: " + mDeviceAddress);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_all_survey_options, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.back_button) {
            Log.d(TAG, "Exit all survey options activity to main activity");
            back();
            return true;
        }
        return true;
    }

    public void back() {
        if (mParentActivity.equals("CoreMain")) {
            Intent intent = new Intent(this, CoreMain.class);
            intent.putExtra(CoreMain.EXTRA_PARENT_ACTIVITY, "SurveyOptions");
            intent.putExtra(CoreMain.EXTRA_BLACK_DEVICE_NAME, mBlackName);
            intent.putExtra(CoreMain.EXTRA_BLACK_DEVICE_ADDRESS, mBlackAddress);
            intent.putExtra(CoreMain.EXTRA_WHITE_DEVICE_NAME, mWhiteName);
            intent.putExtra(CoreMain.EXTRA_WHITE_DEVICE_ADDRESS, mWhiteAddress);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            Log.d(TAG, "Device name: " + mDeviceName + ", Device Address: " + mDeviceAddress);
            intent.putExtra(MainActivity.EXTRA_DEVICE_NAME, mDeviceName);
            intent.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
            intent.putExtra(MainActivity.EXTRA_PARENT_ACTIVITY, "SurveyOptions");
            surveyTicket = -1; //error code

            try {
                TakeMeasurements.detailedRecordedShots = null;
                TakeMeasurements.recordedShots = null;
            } catch (Exception e) {
                Log.e(TAG, "Exception thrown in removing existing survey from short term: " + e);
            }

            startActivity(intent);
        }
    }
}