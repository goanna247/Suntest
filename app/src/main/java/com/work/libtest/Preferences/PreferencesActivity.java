////////////////////////////////////////////////////////////////////////////////
/*
 * \file PreferencesActivity.java
 * \brief Page to edit the preferences of the probe
 * \author Anna Pedersen
 * \date Created: 07/06/2024
 *
 * Links to a bunch of unimplemented pages :(
 */
package com.work.libtest.Preferences;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.work.libtest.MainActivity;
import com.work.libtest.Preferences.modeSelection.modeSelectionActivity;
import com.work.libtest.R;

public class PreferencesActivity extends AppCompatActivity {
    private static final String TAG = "preferences:";

    //Data passed in from Main activity to store information about the probe
    public static final String EXTRA_DEVICE_NAME = "Device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "Device_address";
    public static final String EXTRA_DEVICE_CONNECTION_STATUS = "Connection_status";

    //local probe information
    private String mDeviceName;
    private String mDeviceAddress;
    private String mDeviceConnectionStatus;

    //edit items within the view
    TextView preferences_operations_modeOptions;
    CheckBox preferences_operations_advancedOptions;
    CheckBox preferences_operations_multiShotOptions;
    CheckBox preferences_operations_depthTrackingOptions;
    TextView preferences_operations_initialDepthOptions;
    TextView preferences_operations_depthIntervalOptions;
    TextView preferences_alignment_rollOptions;
    TextView preferences_alignment_dipOptions;
    CheckBox preferences_movement_enableOptions;
    TextView preferences_movement_deviationOptions;
    CheckBox preferences_magnetic_enableOptions;
    TextView preferences_magnetic_magnitudeOptions;
    TextView preferences_magnetic_deviationOptions;
    CheckBox preferences_presentation_showDipOptions;
    CheckBox preferences_presentation_showRollOptions;
    CheckBox preferences_presentation_audioOptions;
    TextView preferences_about_versionOptions;

    private EditText initialDepthEdit;
    private EditText depthIntervalEdit;
    private EditText rollEdit;
    private EditText dipEdit;
    private EditText normalMagEdit;
    private EditText maxDevEdit;
    private EditText moveMaxDevEdit;


    private Menu menu;

    /**
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
        Toolbar toolbar = findViewById(R.id.toolbar);

        preferences_operations_modeOptions = findViewById(R.id.preferences_operations_modeOptions);
        preferences_operations_advancedOptions = findViewById(R.id.preferences_operations_advancedOptions);
        preferences_operations_multiShotOptions = findViewById(R.id.preferences_operations_multiShotOptions);
        preferences_operations_depthTrackingOptions = findViewById(R.id.preferences_operations_depthTrackingOptions);
        preferences_operations_initialDepthOptions = findViewById(R.id.preferences_operations_initialDepthOptions);
        preferences_operations_depthIntervalOptions = findViewById(R.id.preferences_operations_depthIntervalOptions);
        preferences_alignment_rollOptions = findViewById(R.id.accelerometer_x_data);
        preferences_alignment_dipOptions = findViewById(R.id.preferences_alignment_dipOptions);
        preferences_movement_enableOptions = findViewById(R.id.preferences_movement_enableOptions);
        preferences_movement_deviationOptions = findViewById(R.id.preferences_movement_deviationOptions);
        preferences_magnetic_enableOptions = findViewById(R.id.preferences_magnetic_enableOptions);
        preferences_magnetic_magnitudeOptions = findViewById(R.id.preferences_magnetic_magnitudeOptions);
        preferences_magnetic_deviationOptions = findViewById(R.id.preferences_magnetic_deviationOptions);
        preferences_presentation_showDipOptions = findViewById(R.id.preferences_presentation_showDipOptions);
        preferences_presentation_showRollOptions = findViewById(R.id.preferences_presentation_showRollOptions);
        preferences_presentation_audioOptions = findViewById(R.id.preferences_presentation_audioOptions);
        preferences_about_versionOptions = findViewById(R.id.preferences_about_versionOptions);

        initialDepthEdit = (EditText) findViewById(R.id.initalDepthEdit);
        depthIntervalEdit = (EditText) findViewById(R.id.depthIntervalEdit);
        rollEdit = (EditText) findViewById(R.id.rollEdit);
        dipEdit = (EditText) findViewById(R.id.dipEdit);
        normalMagEdit = (EditText) findViewById(R.id.normalMagEdit);
        maxDevEdit = (EditText) findViewById(R.id.maxDevEdit);
        moveMaxDevEdit = (EditText) findViewById(R.id.moveMaxDevEdit);

        preferences_operations_modeOptions.setText(MainActivity.preferences.getMode());
        Log.d(TAG, MainActivity.preferences.getMode());

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        mDeviceConnectionStatus = intent.getStringExtra(EXTRA_DEVICE_CONNECTION_STATUS);

        setSupportActionBar(toolbar);

        LoadSavedPreferences();
    }


    /**
     * Method to handle the click event for advanced options in preferences.
     * @param v The view associated with the click event.
     */
    public void preferences_operations_mode_click(View v) {
        Intent intent = new Intent(this, modeSelectionActivity.class);
        intent.putExtra(modeSelectionActivity.EXTRA_DEVICE_NAME, mDeviceName);
        intent.putExtra(modeSelectionActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(modeSelectionActivity.EXTRA_DEVICE_CONNECTION_STATUS, mDeviceConnectionStatus);
        startActivity(intent);
    }

    /**
     * Method to handle the click event for multi-shot options in preferences.
     * @param v The view associated with the click event.
     */
    public void preferences_operations_advanced_click(View v) {
        MainActivity.preferences.setAdvancedMode(preferences_operations_advancedOptions.isChecked());
    }

    /**
     * Method to handle the click event for depth tracking options in preferences.
     * @param v The view associated with the click event.
     */
    public void preferences_operations_multiShot_click(View v) {
        MainActivity.preferences.setMultiShotMode(preferences_operations_multiShotOptions.isChecked());
    }

    /**
     * Method to handle the click event for movement enable options in preferences.
     * @param v The view associated with the click event.
     */
    public void preferences_operations_depthTracking_click(View v) {
        MainActivity.preferences.setDepthTracking(preferences_operations_depthTrackingOptions.isChecked());
    }

//    public void preferences_operations_initialDepth_click(View v) {
//        Intent intent = new Intent(this, editInitialDepthActivity.class);
//        startActivity(intent);
//    }
//
//    public void preferences_operations_depthInterval_click(View v) {
//        Intent intent = new Intent(this, editDepthIntervalActivity.class);
//        startActivity(intent);
//    }


//    public void preferences_alignment_roll_click(View v) {
//        Intent intent = new Intent(this, editRollActivity.class);
//        startActivity(intent);
//    }
//
//    public void preferences_alignment_dip_click(View v) {
//        Intent intent = new Intent(this, editDipActivity.class);
//        startActivity(intent);
//    }

    /**
     * Method to handle the click event for magnetic field enable options in preferences.
     * @param v The view associated with the click event.
     */
    public void preferences_movement_enable_click(View v) {
        MainActivity.preferences.setMovementAlarmEnable(preferences_movement_enableOptions.isChecked());
    }

//    public void preferences_movement_deviation_click(View v) {
//        Intent intent = new Intent(this, editMaxMovementDeviationActivity.class);
//        startActivity(intent);
//    }

    public void preferences_magnetic_enable_click(View v) {
        MainActivity.preferences.setMagneticFieldAlarmEnable(preferences_magnetic_enableOptions.isChecked());
    }
//
//    public void preferences_magnetic_magnitude_click(View v) {
//        Intent intent = new Intent(this, editMagMagnitudeActivity.class);
//        startActivity(intent);
//    }
//
//    public void preferences_magnetic_deviation_click(View v) {
//        Intent intent = new Intent(this, editMagDeviationActivity.class);
//        startActivity(intent);
//    }

    /**
     * Method to handle the click event for showing dip options in preferences.
     * @param v The view associated with the click event.
     */
    public void preferences_presentation_showDip_click(View v) {
        MainActivity.preferences.setShowDip(preferences_presentation_showDipOptions.isChecked());
    }

    /**
     * Method to handle the click event for showing roll options in preferences.
     * @param v The view associated with the click event.
     */
    public void preferences_presentation_showRoll_click(View v) {
        MainActivity.preferences.setShowRoll(preferences_presentation_showRollOptions.isChecked());
    }

    /**
     * Method to handle the click event for audio alerts options in preferences.
     * @param v The view associated with the click event.
     */
    public void preferences_presentation_audio_click(View v) {
        MainActivity.preferences.setAudioAlerts(preferences_presentation_audioOptions.isChecked());
    }

//    public void preferences_files_email_click(View v) {
//        Intent intent = new Intent(this, emailFilesActivity.class);
//        startActivity(intent);
//    }
//
//    public void preferences_files_delete_click(View v) {
//        Intent intent = new Intent(this, deleteFilesActivity.class);
//        startActivity(intent);
//    }
//
//    public void preferences_about_version_click(View v) {
//        //the apple app doesn't link to the about page, but like it feels like it should
//        Intent intent = new Intent(this, AboutActivity.class);
//        startActivity(intent);
//    }

    /**
     * Method to load saved preferences.
     */
    public void LoadSavedPreferences() {
        preferences_operations_modeOptions.setText(MainActivity.preferences.getMode());
        preferences_operations_advancedOptions.setChecked(MainActivity.preferences.getAdvancedMode());
        preferences_operations_multiShotOptions.setChecked(MainActivity.preferences.getmultiShotMode());
        preferences_operations_depthTrackingOptions.setChecked(MainActivity.preferences.getDepthTracking());
        initialDepthEdit.setText(Double.toString(MainActivity.preferences.getInitialDepth()));
        depthIntervalEdit.setText(Double.toString(MainActivity.preferences.getDepthInterval()));

        rollEdit.setText(Double.toString(MainActivity.preferences.getRoll()));
        dipEdit.setText(Double.toString(MainActivity.preferences.getDip()));

        preferences_movement_enableOptions.setChecked(MainActivity.preferences.getMagneticEnable());
        moveMaxDevEdit.setText(Double.toString(MainActivity.preferences.getMovementMaximumDeviation()));

        preferences_magnetic_enableOptions.setChecked(MainActivity.preferences.getMagneticEnable());
        normalMagEdit.setText(Double.toString(MainActivity.preferences.getNominalMagnitude()));
        maxDevEdit.setText(Double.toString(MainActivity.preferences.getMagneticMaximumDeviation()));

        preferences_presentation_showDipOptions.setChecked(MainActivity.preferences.getShowDip());
        preferences_presentation_showRollOptions.setChecked(MainActivity.preferences.getShowRoll());
        preferences_presentation_audioOptions.setChecked(MainActivity.preferences.getAudioAlerts());

        preferences_about_versionOptions.setText(Double.toString(MainActivity.preferences.getCurrentVersion()));
    }

    /**
     * Method to handle the creation of the options menu.
     * @param menu The menu to be inflated.
     * @return True if the menu is successfully inflated, false otherwise.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_preferences, menu);
        this.menu = menu;
        return true;
    }

    /**
     * Method to handle options menu item selection.
     * @param item The menu item selected.
     * @return True if the item selection is handled successfully, false otherwise.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.sensor_back_button) {
            Log.d(TAG, "exit save data activity to go back to sensor activity");
            back();
            return true;
        }
//        switch (item.getItemId()) {
//            case R.id.sensor_back_button:
//                Log.d(TAG, "exit save data activity to go back to sensor activity");
//                back();
//                return true;
//        }
        return true;
    }

    /**
     * Method to handle going back to the previous activity.
     */
    private void back() {
        //save all the current data

        MainActivity.preferences.setAdvancedMode(preferences_operations_advancedOptions.isChecked());
        MainActivity.preferences.setMultiShotMode(preferences_operations_multiShotOptions.isChecked());
        MainActivity.preferences.setDepthTracking(preferences_operations_depthTrackingOptions.isChecked());
        MainActivity.preferences.setMovementAlarmEnable(preferences_movement_enableOptions.isChecked());
        MainActivity.preferences.setMagneticFieldAlarmEnable(preferences_magnetic_enableOptions.isChecked());
        MainActivity.preferences.setShowDip(preferences_presentation_showDipOptions.isChecked());
        MainActivity.preferences.setShowRoll(preferences_presentation_showRollOptions.isChecked());
        MainActivity.preferences.setAudioAlerts(preferences_presentation_audioOptions.isChecked());

        MainActivity.preferences.setDip(Double.parseDouble(dipEdit.getText().toString()));
        MainActivity.preferences.setRoll(Double.parseDouble(rollEdit.getText().toString()));
        MainActivity.preferences.setDepthInterval(Double.parseDouble(depthIntervalEdit.getText().toString()));
        MainActivity.preferences.setInitialDepth(Double.parseDouble(initialDepthEdit.getText().toString()));
        MainActivity.preferences.setNominalMagnitude(Double.parseDouble(normalMagEdit.getText().toString()));
        MainActivity.preferences.setMagneticMaximumDeviation(Double.parseDouble(maxDevEdit.getText().toString()));
        MainActivity.preferences.setMovementMaximumDeviation(Double.parseDouble(moveMaxDevEdit.getText().toString()));

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_DEVICE_NAME, mDeviceName);
        intent.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(MainActivity.EXTRA_CONNECTION_STATUS, mDeviceConnectionStatus);
        startActivity(intent);
    }
}
