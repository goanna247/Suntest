package com.work.libtest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ResetSurveyActivity extends AppCompatActivity {

    /**
     * Creates a kind-of popup asking the user whether they would like to reset the survey
     * If in the single mode ask the user whether they would like to reset
     * If in the double mode ask the user which they would like to reset
     */

    private static final String TAG = "Reset Survey Activity";

    public static final String EXTRA_DEVICE_NAME = "Device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "Device_address";

    public static final String EXTRA_PREFERENCES_MODE = "Mode";

    private String mMode;
    private String mDeviceName;
    private String mDeviceAddress;

    private TextView modeInfo;
    private TextView displayText;

    private Button whiteProbeReset;
    private Button blackProbeReset;
    private Button singleProbeReset;

    private Button backReset;

    @Override
    protected void onResume() {
        super.onResume();

        modeInfo.setText(EXTRA_PREFERENCES_MODE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_survey);

        mMode = EXTRA_PREFERENCES_MODE;
        mDeviceAddress = EXTRA_DEVICE_ADDRESS;
        mDeviceName = EXTRA_DEVICE_NAME;

        modeInfo = (TextView) findViewById(R.id.reset_survey_modeInfo);
        displayText = (TextView) findViewById(R.id.reset_survey_displayText);

        whiteProbeReset = (Button) findViewById(R.id.reset_survey_white_probe);
        blackProbeReset = (Button) findViewById(R.id.reset_survey_black_probe);
        singleProbeReset = (Button) findViewById(R.id.reset_survey_single_confirm);

        modeInfo.setText(mMode);

        if (mMode == "Dual") {
            singleProbeReset.setVisibility(View.GONE);
            whiteProbeReset.setVisibility(View.VISIBLE);
            blackProbeReset.setVisibility(View.VISIBLE);
        } else if (mMode == "Single") {
            singleProbeReset.setVisibility(View.VISIBLE);
            whiteProbeReset.setVisibility(View.GONE);
            blackProbeReset.setVisibility(View.GONE);
        } else {
            Log.e(TAG, "Error, mode incorrectly set");
        }
    }

    /**
     * Buttons
     */


    public void backReset(View view) {
        Intent intent = new Intent(this, MainActivity.class);
//        intent.putExtra(MainActivity.EXTRA_DEVICE_NAME, mDeviceName);
//        intent.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        startActivity(intent);
    }

    public void blackProbeReset(View view) {
        //TODO reset all saved measurements for the black probe, and deselect whatever probe has been being used
    }

    public void whiteProbeReset(View view) {
        //TODO reset all saved measurements for the white probe, and deselect whatever probe has been being used
    }

    public void confirmReset(View view) {
        //TODO reset all saved measurements and deselect probe.
    }
}