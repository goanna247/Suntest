/**
 * \file SaveData.java
 * \brief Activity for saving probe shots into a csv file to be saved on the device
 * \author Anna Pedersen
 * \date Updated: 31/08/2024
 *
 * Currently needs to be updated to actually write data to file after the calibration
 * changes.
 */

package com.work.libtest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
//import androidx.navigation.ui.AppBarConfiguration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;


public class SaveData extends AppCompatActivity {
    private String TAG = "Save Data";

    public static File dir = new File(new File(Environment.getExternalStorageDirectory(), "bleh"), "bleh");

    public static final String EXTRA_DEVICE_NAME = "Device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "Device_address";
    public static final String EXTRA_DEVICE_CONNECTION_STATUS = "Connection_status";

    public static final String EXTRA_DEVICE_SERIAL_NUMBER = "Serial_number";
    public static final String EXTRA_DEVICE_DEVICE_ADDRESS = "Device_gathered_addresses";
    public static final String EXTRA_DEVICE_VERSION = "Device_firmware_version";

    public static final String EXTRA_SAVED_DATA = "Data_to_be_saved";
    public static final String EXTRA_EXTRA_SAVED_DATA = "Extra_weird_data";

    public static final String EXTRA_PARENT_ACTIVITY = "Parent_activity"; //either View or Sensor

    private Menu menu;

    private String parentActivity = "View";

    private CheckBox exportAllData;
    private TextView errorMessage;
    private EditText fileNameEdit;
    private EditText saveNumberEdit;
    private CheckBox exportTitles;

    private TextView exportTitlesTitle;
    private TextView exportAllDataTitle;
    private TextView saveNumberTitle;

    private String mDeviceName;
    private String mDeviceAddress;
    private String mDeviceConnectionStatus;
    private String lDeviceAddress;
    private String lSerialNumber;
    private String lFirmwareVersion;

    private int saveNum;

    public static ArrayList<ProbeData> probeData = new ArrayList<ProbeData>();
    public static LinkedList<Measurement> viewProbeData = new LinkedList<>();
    public static LinkedList<DetailedMeasurement> detailedViewProbeData = new LinkedList<>();

    @Override
    protected void onDestroy() {
        super.onDestroy();

        probeData = new ArrayList<>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * Link the layout view to the java class and set the toolbar as the basic toolbar
         */
        setContentView(R.layout.activity_save_data);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /**
         * Get all data being passed in from the location the activity is called
         * needs the name, address, status and parent activity in order to function
         */
        final Intent intent = getIntent();
        try {
            mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
            mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
            mDeviceConnectionStatus = intent.getStringExtra(EXTRA_DEVICE_CONNECTION_STATUS);
            parentActivity = intent.getStringExtra(EXTRA_PARENT_ACTIVITY);
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown gathering intent from SaveData: " + e);
        }

        /**
         * Link items on the view to objects in the java class so they are usable
         */
        exportAllData = (CheckBox) findViewById(R.id.export_data_check);
        errorMessage = (TextView) findViewById(R.id.save_error_msg);
        fileNameEdit = (EditText) findViewById(R.id.file_name_edit);
        saveNumberEdit = (EditText) findViewById(R.id.editTextNumber);
        exportTitles = (CheckBox) findViewById(R.id.export_titles_check);

        exportTitlesTitle = (TextView) findViewById(R.id.export_titles);
        exportAllDataTitle = (TextView) findViewById(R.id.export_data_title);
        saveNumberTitle = (TextView) findViewById(R.id.save_number_title);

        errorMessage.setText("");
    }

    /**
     *
     * @param requestCode The request code passed in {@link #requestPermissions(
     * android.app.Activity, String[], int)}
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
     *     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
     *
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1000:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission granted!");
                } else {
                    Log.d(TAG, "Permission not granted!");
                }
        }
    }

    LinkedList<Measurement> sensorSavedData = new LinkedList<>();

    @Override
    protected void onResume() {
        super.onResume();

        /**
         * If the activity came from is a survey, ensure only the file name option
         * is visible as we dont want them to just pick a single measurement and we always want to
         * display the titles.
         */
        if (parentActivity.equals("View")) {
            exportAllData.setVisibility(View.INVISIBLE);
            saveNumberEdit.setVisibility(View.INVISIBLE);
            exportTitles.setVisibility(View.INVISIBLE);
            exportTitlesTitle.setVisibility(View.INVISIBLE);
            exportAllDataTitle.setVisibility(View.INVISIBLE);
            saveNumberTitle.setVisibility(View.INVISIBLE);

            viewProbeData = TakeMeasurements.recordedShots;
            detailedViewProbeData = TakeMeasurements.detailedRecordedShots;
        } else if (parentActivity.equals("Sensor")){
            sensorSavedData = SensorActivity.SavedMeasurements;
            for (int i = 0; i < sensorSavedData.size(); i++) {
                Log.i(TAG, "Measurement record number: " + sensorSavedData.get(i).getName());
            }

        } else {
            /**
             * get all the probe data passed into the activity and log for backend checks
             */
            probeData = (ArrayList<ProbeData>) getIntent().getSerializableExtra(EXTRA_SAVED_DATA);
            for (int i = 0; i < probeData.size(); i++) {
                Log.e(TAG, "Probe Data " + i + " : " + probeData.get(i).returnData() + probeData.get(i).returnTitles());
            }

            /**
             * If there is no data log this to a debugger
             */
            if (probeData.size() == 0) {
                Log.e(TAG, "EMPTY DATA");
            }

        }
    }

    /**
     * Create activity menu on page
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_save_data, menu);
        this.menu = menu;
        return true;
    }

    /**
     * deals with menu items being selected
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
     * When the user presses the Save Data button on the page, save the data
     */
    public void saveDataPressed(View view) {
        String content = ""; //initialise content to be saved as empty
        int recordToBeSaved;

        /**
         * If we have come from taking a survey measurement then return titles
         * by default, if not check whether the checkbox is ticked.
         */
        if (parentActivity.equals("View")) {
            //Get the measurment data from the takeMeasurements/viewmeasurement activity, check validity then save locally
            LinkedList<Measurement> gatheredMeasurements = null;
            LinkedList<DetailedMeasurement> detailedGatheredMeasurements = null;

            try {
                gatheredMeasurements = TakeMeasurements.recordedShots; //maybe it would be better to get from viewMeasurements ¯\_(ツ)_/¯
                detailedGatheredMeasurements = TakeMeasurements.detailedRecordedShots;

            } catch (Exception e) {
                Log.e(TAG, "Exception thrown collecting data: " + e);
            }

            try {
                content = content + "Borecam - Precision Instrumentation\n";
                content = content + "Company Name " + detailedGatheredMeasurements.get(0).getCompanyName() + "\n";
                content = content + "Operator Name " + detailedGatheredMeasurements.get(0).getOperatorID() + "\n\n";


                content = content + "Probe ID,Hole,Measurement,Depth,Date,Time,DIntegrity,Azimuth,Dip,Roll,TotMag,Temperature\n"; //headings from measurement.java

                for (int i = 0; i < gatheredMeasurements.size(); i++) {
                    content = content + detailedGatheredMeasurements.get(i).getProbeID();
                    content = content + "," + detailedGatheredMeasurements.get(i).getHoleID();
                    content = content + "," + detailedGatheredMeasurements.get(i).getDetailedName();
                    content = content + "," +  gatheredMeasurements.get(i).getDepth();
                    content = content + "," +  gatheredMeasurements.get(i).getDate();
                    content = content + "," +  gatheredMeasurements.get(i).getTime();
                    content = content + "," +  detailedGatheredMeasurements.get(i).getDIntegrity();
                    content = content + "," +  gatheredMeasurements.get(i).getAzimuth();
                    content = content + "," +  gatheredMeasurements.get(i).getDip();
                    content = content + "," +  gatheredMeasurements.get(i).getRoll();
                    content = content + "," + detailedGatheredMeasurements.get(i).getTotMag();
                    content = content + "," +  gatheredMeasurements.get(i).getTemp();
                    content = content + "\n\n";
                }

                content = content + "DIntegrity Legend:\n";
                content = content + "D*                   Dip outside expected range\n";
                content = content + "M*                   Total magnetic field outside excpected range\n";
                content = content + "A*                   Azimuth outside expected range\n";
                content = content + "#                    Probe outside operating temperature\n";
                content = content + "!                    Probe moving during survey shot\n";


            } catch (Exception e) {
                Log.e(TAG, "Exception thrown in adding data to file to be saved: " + e);
            }

            try {
                saveTextAsFile(content); //save all content collected
            } catch (Exception e) {
                Log.e(TAG, "failed to save: " + e);
            }
        } else if (parentActivity.equals("Sensor")) {
            //gathered measurments = sensorSavedData
            content = ""; //clear content in case there is anything left after the last save attempt (ie, if the app crashes)
            if (exportTitles.isChecked()) {
                content = content + "Record Name,Date,Time,Temp,Depth,Dip,Roll,Azimuth\n"; //headings from measurement.java
            }
            if (exportAllData.isChecked()) {
                //export all of the data
                if (sensorSavedData != null && sensorSavedData.size() > 0) {
                    for (int i = 0; i < sensorSavedData.size(); i++) {
                        content = content + sensorSavedData.get(i).getName();
                        content = content + "," +  sensorSavedData.get(i).getDate();
                        content = content + "," +  sensorSavedData.get(i).getTime();
                        content = content + "," +  sensorSavedData.get(i).getTemp();
                        content = content + "," +  sensorSavedData.get(i).getDepth();
                        content = content + "," +  sensorSavedData.get(i).getDip();
                        content = content + "," +  sensorSavedData.get(i).getRoll();
                        content = content + "," +  sensorSavedData.get(i).getAzimuth();
                        content = content + "\n";
                    }
                }
            } else {
                //export a single value
                if (!saveNumberEdit.getText().toString().equals("")) {
                    errorMessage.setText(" ");
                    recordToBeSaved = Integer.valueOf(saveNumberEdit.getText().toString());
                    content = content + sensorSavedData.get(recordToBeSaved).getName();
                    content = content + "," +  sensorSavedData.get(recordToBeSaved).getDate();
                    content = content + "," +  sensorSavedData.get(recordToBeSaved).getTime();
                    content = content + "," +  sensorSavedData.get(recordToBeSaved).getTemp();
                    content = content + "," +  sensorSavedData.get(recordToBeSaved).getDepth();
                    content = content + "," +  sensorSavedData.get(recordToBeSaved).getDip();
                    content = content + "," +  sensorSavedData.get(recordToBeSaved).getRoll();
                    content = content + "," +  sensorSavedData.get(recordToBeSaved).getAzimuth();
                    content = content + "\n";
                    saveTextAsFile(content);
                } else {
                    errorMessage.setText("Please enter a number");
                }
            }
            saveTextAsFile(content);
        } else {
            /**
             * Check data that is being saved is correct
             */
            if (probeData != null) {
                for (int i = 0; i < probeData.size(); i++) {
                    Log.d(TAG, probeData.get(i).returnData());
                }
            } else {
                Log.e(TAG, "ERROR PROBE DATA NULL");
            }

            if (exportTitles.isChecked()) {
                try {
                    content = probeData.get(0).returnTitles();
                } catch (Exception e) {
                    Log.e(TAG, "Exception thrown attempting to return titles: " + e);
                }
            }

            if (exportAllData.isChecked()) {
                //export all of the files
                if (probeData != null) {
                    for (int i = 0; i < probeData.size(); i++) { //pass these values in seperately lol
                        content = content + "\n" + probeData.get(i).returnData();
                    }
                } else {
                    Log.e(TAG, "ERROR PROBE DATA NULL");
                }

                try {
                    saveTextAsFile(content);
                } catch (Exception e) {
                    Log.e(TAG, "failed to save: " + e);
                }
            } else {
                //export a single value
                if (!saveNumberEdit.getText().toString().equals("")) {
                    errorMessage.setText(" ");
                    recordToBeSaved = Integer.valueOf(saveNumberEdit.getText().toString());
                    content = content + probeData.get(recordToBeSaved).returnData();
                    saveTextAsFile(content);
                } else {
                    errorMessage.setText("Please enter a number");
                }
            }
        }
    }

    public void saveTextAsFile(String content) {
        String filename;
        Log.e(TAG, "SAVING TEXT AS FILE");
        Log.e(TAG, "CONTENT: " + content);

        askForPermissions();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                createDir();
            }
        }

        try {
            if (fileNameEdit.getText().equals("")) {
                filename = Calendar.getInstance().getTime().toString();
            } else {
                filename = fileNameEdit.getText().toString();
            }

            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + filename + ".csv");

            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
            Log.d(TAG, "Written to file");

            back();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown: " + e);
            e.printStackTrace();
        }
    }

    public void askForPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
                return;
            }
            createDir();
        }
    }

    public void createDir(){
        if (!dir.exists()){
            dir.mkdirs();
        }
    }

    public void back() {
        if (parentActivity.equals("View")) {
            Intent intent = new Intent(this, ViewMeasurements.class);
            intent.putExtra(ViewMeasurements.EXTRA_DEVICE_NAME, mDeviceName);
            intent.putExtra(ViewMeasurements.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
            intent.putExtra(ViewMeasurements.EXTRA_DEVICE_CONNECTION_STATUS, mDeviceConnectionStatus);

            ProbeDataStorage.arrayListNum++;
            probeData = null;
            startActivity(intent);
        } else if (parentActivity.equals("Sensor")) {
            Intent intent = new Intent(this, SensorActivity.class);
            intent.putExtra(ViewMeasurements.EXTRA_DEVICE_NAME, mDeviceName);
            intent.putExtra(ViewMeasurements.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
            intent.putExtra(ViewMeasurements.EXTRA_DEVICE_CONNECTION_STATUS, mDeviceConnectionStatus);
            Log.e(TAG, "Name: " + mDeviceName + ", Address: " + mDeviceAddress);
            SensorActivity.SavedMeasurements = new LinkedList<>();
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_DEVICE_NAME, mDeviceName);
            intent.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
            intent.putExtra(MainActivity.EXTRA_CONNECTION_STATUS, mDeviceConnectionStatus);
            Log.e(TAG, "Name: " + mDeviceName + ", Address: " + mDeviceAddress);

            ProbeDataStorage.arrayListNum++;
            probeData = null;
            startActivity(intent);
        }
//        Intent intent = new Intent(this, SensorActivity.class);
//        intent.putExtra(SensorActivity.EXTRA_DEVICE_NAME, mDeviceName);
//        intent.putExtra(SensorActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
//        intent.putExtra(SensorActivity.EXTRA_DEVICE_SERIAL_NUMBER, lSerialNumber);
//        intent.putExtra(SensorActivity.EXTRA_DEVICE_DEVICE_ADDRESS, lDeviceAddress);
//        intent.putExtra(SensorActivity.EXTRA_DEVICE_VERSION, lFirmwareVersion);
//        intent.putExtra(SensorActivity.EXTRA_DEVICE_CONNECTION_STATUS, mDeviceConnectionStatus);
//
//        intent.putExtra(SensorActivity.EXTRA_SAVED_NUM, "8");
//        ProbeDataStorage.arrayListNum++;
//        startActivity(intent);
    }
}






















