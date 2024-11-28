////////////////////////////////////////////////////////////////////////////////
/**
 * \file InitalisePopupActivity.java
 * \brief Helper class for all Bluetooth Low energy connections
 * \author Anna Pedersen
 * \date Created: 07/06/2024
 *
 */
package com.work.libtest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.work.libtest.SurveyOptions.AllSurveyOptionsActivity;

public class InitalisePopupActivity extends AppCompatActivity {

    private String mDeviceName;
    private String mDeviceAddress;

    public static final String EXTRA_DEVICE_NAME = "Device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "Device_address";

    private Menu menu;

    private String TAG = "Initialise Pop-up Activity";

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
        setContentView(R.layout.activity_initalise_popup);
        Toolbar toolbar = findViewById(R.id.toolbar);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        Log.d(TAG, "Device Name: " + mDeviceName + ", Device Address: " + mDeviceAddress);

        setSupportActionBar(toolbar);
    }

    /**
     * When activity is resumed re-set probe details
     */
    @Override
    protected void onResume() {
        super.onResume();

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        Log.d(TAG, "Device Name: " + mDeviceName + ", Device Address: " + mDeviceAddress);
    }

    /**
     *
     * @param menu The options menu in which you place your items.
     *
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_initalise_popup, menu);
        this.menu = menu;
        return true;
    }

    /**
     *
     * @param item The menu item that was selected.
     *
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.back_button) {
            Log.d(TAG, "Exit initalise activity");
            back();
        }
        return true;
    }

    /**
     * go back to the Main Activity
     */
    public void back() {
        Intent intent = new Intent(this, MainActivity.class);
        Log.d(TAG, "Device name: " + mDeviceName + ", Device Address: " + mDeviceAddress);
        intent.putExtra(MainActivity.EXTRA_DEVICE_NAME, mDeviceName);
        intent.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(MainActivity.EXTRA_PARENT_ACTIVITY, "SurveyOptions");
        startActivity(intent);
    }


    /**
     * Start new survey by going to the survey options activity
     * @param v
     */
    public void startNewClick(View v) {
        //open survey preferences with initial depth and depth interval
        Intent intent = new Intent(this, AllSurveyOptionsActivity.class);
        intent.putExtra(AllSurveyOptionsActivity.EXTRA_DEVICE_NAME, mDeviceName);
        intent.putExtra(AllSurveyOptionsActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(AllSurveyOptionsActivity.EXTRA_MEASUREMENT_TYPE, "NEW");
        startActivity(intent);
    }

    /**
     * Open a previous survey - TODO
     *
     * TODO - NOT IMPLEMENTED
     */
    public void resumePrevClick(View v) {
//        Intent intent = new Intent(this, PreviousSurvey.class);
//        intent.putExtra(PreviousSurvey.EXTRA_DEVICE_NAME, mDeviceName);
//        intent.putExtra(PreviousSurvey.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
//        intent.putExtra(PreviousSurvey.EXTRA_MEASUREMENT_TYPE, "OLD");
//        startActivity(intent);
    }

}
