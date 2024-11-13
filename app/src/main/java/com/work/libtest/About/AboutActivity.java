////////////////////////////////////////////////////////////////////////////////
/*
* \file AboutActivity.java
* \brief information page about the app, currently displays minimal information
* \author Anna Pedersen
* \date Created: 07/06/2024
*
* About page of the app displaying static data defined in activity_about.xml
 */

package com.work.libtest.About;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

//import com.work.libtest.MainActivity;
import com.work.libtest.R;

public class AboutActivity extends AppCompatActivity {
    private String TAG = "ABOUT";

    TextView versionTxt;

    //Information to be passed in to the activity, stores a device and its connection status
    public static final String EXTRA_DEVICE_NAME = "Device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "Device_address";
    public static final String EXTRA_DEVICE_CONNECTION_STATUS = "Connection_status";

    //local probe information
    private String mDeviceName;
    private String mDeviceAddress;
    private String mDeviceConnectionStatus;

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
        setContentView(R.layout.activity_about);
        Toolbar toolbar = findViewById(R.id.toolbar);

        versionTxt = findViewById(R.id.versionTxt);
//        versionTxt.setText("Version " + MainActivity.preferences.getCurrentVersion());

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        mDeviceConnectionStatus = intent.getStringExtra(EXTRA_DEVICE_CONNECTION_STATUS);

        setSupportActionBar(toolbar);
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
        inflater.inflate(R.menu.menu_preferences, menu);
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
     * return to Main activity and pass back through the saved probe data
     */
    private void back() {
//        Intent intent = new Intent(this, MainActivity.class);
//        intent.putExtra(MainActivity.EXTRA_DEVICE_NAME, mDeviceName);
//        intent.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
//        intent.putExtra(MainActivity.EXTRA_CONNECTION_STATUS, mDeviceConnectionStatus);
//        startActivity(intent);
    }
}
