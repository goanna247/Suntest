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

    int resumePosition = 128; //error code!

    EditText holeIDInput;
    EditText operatorName;
    EditText companyName;
    EditText initialDepth;
    EditText depthInterval;

    TextView errorMessage;

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
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
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


//        Log.d(TAG, "Device Name: " + mDeviceName + ", Device Address: " + mDeviceAddress);
//
//
////        if (MainActivity.surveys.size() > 0) {
//            Log.d(TAG, MainActivity.surveys.get(0).getSurveyOptions().toString());
//            if (resumePosition != 128) {
////                holeIDInput.setText(Integer.toString(MainActivity.surveys.get(resumePosition).getSurveyOptions().getHoleID()));
////                operatorName.setText(MainActivity.surveys.get(resumePosition).getSurveyOptions().getOperatorName());
////                companyName.setText(MainActivity.surveys.get(resumePosition).getSurveyOptions().getCompanyName());
//                try {
////                    initialDepth.setText((int) MainActivity.surveys.get(resumePosition).getSurveyOptions().getInitialDepth());
////                    depthInterval.setText((int) MainActivity.surveys.get(resumePosition).getSurveyOptions().getDepthInterval());
//                } catch (Exception e) {
//                    Log.e(TAG, "Exception thrown in getting the initial depth and depth interval: " + e);
//                }
//            } else {
//                holeIDInput.setText(Integer.toString(MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().getHoleID() + 1));
//                operatorName.setText(MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().getOperatorName());
//                companyName.setText(MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().getCompanyName());
//                try {
//                    initialDepth.setText((int) MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().getInitialDepth());
//                    depthInterval.setText((int) MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().getDepthInterval());
//                } catch (Exception e) {
//                    Log.e(TAG, "Exception thrown in getting the initial depth and depth interval: " + e);
//                }
//            }
//        }
//        setSupportActionBar(toolbar);
    }

    public void allSurveyOptionsSubmit(View v) {
        try {
            if (resumePosition != 128) {
                Log.d(TAG, "Hole ID: " + holeIDInput.getText().toString() + " Operator Name: " + operatorName.getText().toString() + " Company Name: " + companyName.getText().toString() + " Initial depth: " + initialDepth.getText().toString() + " Depth Interval: " + depthInterval.getText().toString());
//                if (MainActivity.surveySize > 0) {
//                    MainActivity.surveys.get(resumePosition).getSurveyOptions().setHoleID(Integer.valueOf(holeIDInput.getText().toString()));
//                    MainActivity.surveys.get(resumePosition).getSurveyOptions().setOperatorName(operatorName.getText().toString());
//                    MainActivity.surveys.get(resumePosition).getSurveyOptions().setCompanyName(companyName.getText().toString());
//                    MainActivity.surveys.get(resumePosition).getSurveyOptions().setInitialDepth(Double.parseDouble(initialDepth.getText().toString()));
//                    MainActivity.surveys.get(resumePosition).getSurveyOptions().setDepthInterval(Double.parseDouble(depthInterval.getText().toString()));
//                } else {
//                    SurveyOptions newSurveyOptions = new SurveyOptions(Integer.valueOf(holeIDInput.getText().toString()), operatorName.getText().toString(), companyName.getText().toString(), Double.parseDouble(initialDepth.getText().toString()), Double.parseDouble(depthInterval.getText().toString()));
//                    Survey newSurvey = new Survey(newSurveyOptions);
////                    MainActivity.surveys.add(newSurvey);
////                    MainActivity.surveySize++;
//                }

                if (!holeIDInput.getText().toString().equals("") && !operatorName.getText().toString().equals("") && !companyName.getText().toString().equals("") && !initialDepth.getText().toString().equals("") && !depthInterval.getText().toString().equals("")) {
                    //move to taking measurements
                    Intent measurements = new Intent(this, TakeMeasurements.class);
                    measurements.putExtra(TakeMeasurements.EXTRA_DEVICE_NAME, mDeviceName);
                    measurements.putExtra(TakeMeasurements.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
                    measurements.putExtra(TakeMeasurements.EXTRA_DEVICE_CONNECTION_STATUS, "Connected");
                    startActivity(measurements);
                }
            } else {
                Log.d(TAG, "Hole ID: " + holeIDInput.getText().toString() + " Operator Name: " + operatorName.getText().toString() + " Company Name: " + companyName.getText().toString() + " Initial depth: " + initialDepth.getText().toString() + " Depth Interval: " + depthInterval.getText().toString());
//                if (MainActivity.surveySize > 0) {
//                    MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().setHoleID(Integer.valueOf(holeIDInput.getText().toString()));
//                    MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().setOperatorName(operatorName.getText().toString());
//                    MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().setCompanyName(companyName.getText().toString());
//                    MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().setInitialDepth(Double.parseDouble(initialDepth.getText().toString()));
//                    MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().setDepthInterval(Double.parseDouble(depthInterval.getText().toString()));
//                } else {
//                    SurveyOptions newSurveyOptions = new SurveyOptions(Integer.valueOf(holeIDInput.getText().toString()), operatorName.getText().toString(), companyName.getText().toString(), Double.parseDouble(initialDepth.getText().toString()), Double.parseDouble(depthInterval.getText().toString()));
//                    Survey newSurvey = new Survey(newSurveyOptions);
//                    MainActivity.surveys.add(newSurvey);
//                    MainActivity.surveySize++;
//                }

                if (!holeIDInput.getText().toString().equals("") && !operatorName.getText().toString().equals("") && !companyName.getText().toString().equals("") && !initialDepth.getText().toString().equals("") && !depthInterval.getText().toString().equals("")) {
                    //move to taking measurements
                    if (resumePosition != 128) {
                        Intent measurements = new Intent(this, TakeMeasurements.class);
                        measurements.putExtra(TakeMeasurements.EXTRA_DEVICE_NAME, mDeviceName);
                        measurements.putExtra(TakeMeasurements.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
                        measurements.putExtra(TakeMeasurements.EXTRA_DEVICE_CONNECTION_STATUS, "Connected");
                        measurements.putExtra(TakeMeasurements.EXTRA_MEASUREMENT_TYPE, Integer.toString(resumePosition));
                        startActivity(measurements);
                    } else {
                        Intent measurements = new Intent(this, TakeMeasurements.class);
                        measurements.putExtra(TakeMeasurements.EXTRA_DEVICE_NAME, mDeviceName);
                        measurements.putExtra(TakeMeasurements.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
                        measurements.putExtra(TakeMeasurements.EXTRA_DEVICE_CONNECTION_STATUS, "Connected");
                        measurements.putExtra(TakeMeasurements.EXTRA_MEASUREMENT_TYPE, "NEW");
                        startActivity(measurements);
                    }
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
//        switch (item.getItemId()) {
//            case R.id.back_button:
//                Log.d(TAG, "Exit all survey options activity to main activity");
//                back();
//                return true;
//        }
        return true;
    }

    public void back() {
        Intent intent = new Intent(this, MainActivity.class);
        Log.d(TAG, "Device name: " + mDeviceName + ", Device Address: " + mDeviceAddress);
//        intent.putExtra(MainActivity.EXTRA_DEVICE_NAME, mDeviceName);
//        intent.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
//        intent.putExtra(MainActivity.EXTRA_CONNECTION_STATUS, "Connected");
        intent.putExtra(MainActivity.EXTRA_DEVICE_NAME, mDeviceName);
        intent.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(MainActivity.EXTRA_PARENT_ACTIVITY, "AllSurveyOptions");
        startActivity(intent);
    }
}