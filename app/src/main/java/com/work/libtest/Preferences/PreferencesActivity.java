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
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.chip.ChipGroup;
import com.work.libtest.CoreMain;
import com.work.libtest.Globals;
import com.work.libtest.MainActivity;
import com.work.libtest.Preferences.modeSelection.modeSelectionActivity;
import com.work.libtest.R;

public class PreferencesActivity extends AppCompatActivity {
    private static final String TAG = "preferences:";

    //Data passed in from Main activity to store information about the probe
    public static final String EXTRA_DEVICE_NAME = "Device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "Device_address";
    public static final String EXTRA_DEVICE_CONNECTION_STATUS = "Connection_status";

    public static final String EXTRA_BLACK_DEVICE_NAME = "Device_name";
    public static final String EXTRA_BLACK_DEVICE_ADDRESS = "Device_address";

    public static final String EXTRA_WHITE_DEVICE_NAME = "Device_name";
    public static final String EXTRA_WHITE_DEVICE_ADDRESS = "Device_address";

    public static final String EXTRA_PARENT_ACTIVITY = "Parent_Activity";

    //local probe information
    private String mDeviceName;
    private String mDeviceAddress;
    private String mDeviceConnectionStatus;

    //Black probes
    private String mDeviceNameBlack;
    private String mDeviceAddressBlack;

    //White probes
    private String mDeviceNameWhite;
    private String mDeviceAddressWhite;

    private String parentActivity;

    //edit items within the view
    TextView preferences_mode;
    CheckBox preferences_mode_check;

    TextView preferences_rollOptions;
    CheckBox preferences_rollOptions_check;

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

        preferences_mode = findViewById(R.id.preferences_mode);
        preferences_mode_check = findViewById(R.id.preferences_mode_check);

        preferences_rollOptions = findViewById(R.id.preferences_roll);
        preferences_rollOptions_check = findViewById(R.id.preferences_roll_check);

        final Intent intent = getIntent();
        parentActivity = intent.getStringExtra(EXTRA_PARENT_ACTIVITY);
        if (parentActivity.equals("Core")) {
            mDeviceNameBlack = intent.getStringExtra(EXTRA_BLACK_DEVICE_NAME);
            mDeviceAddressBlack = intent.getStringExtra(EXTRA_BLACK_DEVICE_ADDRESS);

            mDeviceNameWhite = intent.getStringExtra(EXTRA_WHITE_DEVICE_NAME);
            mDeviceAddressWhite = intent.getStringExtra(EXTRA_WHITE_DEVICE_ADDRESS);
        } else if (parentActivity.equals("Bore")) {
            mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
            mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
            mDeviceConnectionStatus = intent.getStringExtra(EXTRA_DEVICE_CONNECTION_STATUS);
        }


        setSupportActionBar(toolbar);

        preferences_mode_check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    preferences_mode.setText("Bore Orientation (Single)");
                } else {
                    preferences_mode.setText("Core Orientation (Dual)");
                }
            }
        });

        preferences_rollOptions_check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    preferences_rollOptions.setText("0 to 360");
                } else {
                    preferences_rollOptions.setText("-180 to 180");
                }
            }
        });

        loadPreferences();
    }

    private void loadPreferences() {
        boolean bore = Globals.simplePreferences.getBoreMode();
        boolean rollOptions = Globals.simplePreferences.getRollMode();

        preferences_mode_check.setChecked(bore);
        if (bore) {
            preferences_mode.setText("Bore Orientation (Single)");
        } else {
            preferences_mode.setText("Core Orientation (Dual)");
        }

        preferences_rollOptions_check.setChecked(rollOptions);
        if (rollOptions) {
            preferences_rollOptions.setText("0 to 360");
        } else {
            preferences_rollOptions.setText("-180 to 180");
        }
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
        return true;
    }



    /**
     * Method to handle going back to the previous activity.
     */
    private void back() {
        //save all the current data
        SimplePreferences newPreferences = new SimplePreferences(preferences_mode_check.isChecked(), preferences_rollOptions_check.isChecked());
        Globals.simplePreferences = newPreferences;

        if (preferences_mode_check.isChecked()) { //Bore
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_DEVICE_NAME, mDeviceName);
            intent.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
            intent.putExtra(MainActivity.EXTRA_PARENT_ACTIVITY, "Preferences");
            startActivity(intent);
        } else { //Core
            Intent intent = new Intent(this, CoreMain.class);
            intent.putExtra(CoreMain.EXTRA_BLACK_DEVICE_NAME, mDeviceNameBlack);
            intent.putExtra(CoreMain.EXTRA_BLACK_DEVICE_ADDRESS, mDeviceAddressBlack);
            intent.putExtra(CoreMain.EXTRA_WHITE_DEVICE_NAME, mDeviceNameWhite);
            intent.putExtra(CoreMain.EXTRA_WHITE_DEVICE_ADDRESS, mDeviceAddressWhite);
            intent.putExtra(CoreMain.EXTRA_PARENT_ACTIVITY, "Preferences");
            startActivity(intent);
        }


    }
}
