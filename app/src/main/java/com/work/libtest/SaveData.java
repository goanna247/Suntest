package com.work.libtest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.ui.AppBarConfiguration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;


public class SaveData extends AppCompatActivity {
    private String TAG = "Save Data";

    public static final String EXTRA_DEVICE_NAME = "Device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "Device_address";
    public static final String EXTRA_DEVICE_CONNECTION_STATUS = "Connection_status";

    public static final String EXTRA_DEVICE_SERIAL_NUMBER = "Serial_number";
    public static final String EXTRA_DEVICE_DEVICE_ADDRESS = "Device_gathered_addresses";
    public static final String EXTRA_DEVICE_VERSION = "Device_firmware_version";

    public static final String EXTRA_SAVED_DATA = "Data_to_be_saved";
    public static final String EXTRA_EXTRA_SAVED_DATA = "Extra_weird_data";

    public static final String EXTRA_PARENT_ACTIVITY = "Parent_activity"; //either View or Sensor

    private AppBarConfiguration appBarConfiguration;

    private Menu menu;

    private String parentActivity;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_data);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        mDeviceConnectionStatus = intent.getStringExtra(EXTRA_DEVICE_CONNECTION_STATUS);
        parentActivity = intent.getStringExtra(EXTRA_PARENT_ACTIVITY);
//        lSerialNumber = intent.getStringExtra(EXTRA_DEVICE_SERIAL_NUMBER);
//        lFirmwareVersion = intent.getStringExtra(EXTRA_DEVICE_VERSION);
//        lDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_DEVICE_ADDRESS);

        probeData = getIntent().getParcelableArrayListExtra(EXTRA_SAVED_DATA);
        if (probeData != null) {
            if (probeData.size() != 0) {
                Log.d(TAG, probeData.get(0).returnData());
            } else {
                Log.e(TAG, "Probe Data size is 0");
            }
        } else {
            Log.e(TAG, "Probe data is null");
        }

        exportAllData = (CheckBox) findViewById(R.id.export_data_check);
        errorMessage = (TextView) findViewById(R.id.save_error_msg);
        fileNameEdit = (EditText) findViewById(R.id.file_name_edit);
        saveNumberEdit = (EditText) findViewById(R.id.editTextNumber);
        exportTitles = (CheckBox) findViewById(R.id.export_titles_check);

        exportTitlesTitle = (TextView) findViewById(R.id.export_titles);
        exportAllDataTitle = (TextView) findViewById(R.id.export_data_title);
        saveNumberTitle = (TextView) findViewById(R.id.save_number_title);

        errorMessage.setText("");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
        }

        if (parentActivity.equals("View")) {
            exportAllData.setVisibility(View.INVISIBLE);
            saveNumberEdit.setVisibility(View.INVISIBLE);
            exportTitles.setVisibility(View.INVISIBLE);
            exportTitlesTitle.setVisibility(View.INVISIBLE);
            exportAllDataTitle.setVisibility(View.INVISIBLE);
            saveNumberTitle.setVisibility(View.INVISIBLE);
        }
    }

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

    @Override
    protected void onResume() {
        super.onResume();
        ArrayList<ProbeData> probeData = (ArrayList<ProbeData>) getIntent().getSerializableExtra(EXTRA_SAVED_DATA);
        final Intent intent = getIntent();
        parentActivity = intent.getStringExtra(EXTRA_PARENT_ACTIVITY);
        if (parentActivity.equals("View")) {
            exportAllData.setVisibility(View.INVISIBLE);
            saveNumberEdit.setVisibility(View.INVISIBLE);
            exportTitles.setVisibility(View.INVISIBLE);
            exportTitlesTitle.setVisibility(View.INVISIBLE);
            exportAllDataTitle.setVisibility(View.INVISIBLE);
            saveNumberTitle.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_save_data, menu);
        this.menu = menu;
        return true;
    }

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

    public void saveDataPressed(View view) {
        String content = "";
        int recordToBeSaved;

        if (probeData != null) {
            for (int i = 0; i < probeData.size(); i++) {
                Log.d(TAG, probeData.get(i).returnData());
            }
        } else {
            Log.e(TAG, "ERROR PROBE DATA NULL");
        }

        if (parentActivity.equals("View")) {
            try {
                content = probeData.get(0).returnTitles();
            } catch (Exception e) {
                Log.e(TAG, "Exception thrown attempting to return titles: " + e);
            }
        } else {
            if (exportTitles.isChecked()) {
                try {
                    content = probeData.get(0).returnTitles();
                } catch (Exception e) {
                    Log.e(TAG, "Exception thrown attempting to return titles: " + e);
                }
            }
        }

        if (parentActivity.equals("View")) {
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
            e.printStackTrace();
        }
    }

    public void back() {
        if (parentActivity.equals("View")) {
            Intent intent = new Intent(this, ViewMeasurements.class);
            intent.putExtra(ViewMeasurements.EXTRA_DEVICE_NAME, mDeviceName);
            intent.putExtra(ViewMeasurements.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
            intent.putExtra(ViewMeasurements.EXTRA_DEVICE_CONNECTION_STATUS, mDeviceConnectionStatus);

            ProbeDataStorage.arrayListNum++;
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_DEVICE_NAME, mDeviceName);
            intent.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
            intent.putExtra(MainActivity.EXTRA_CONNECTION_STATUS, mDeviceConnectionStatus);

            ProbeDataStorage.arrayListNum++;
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






















