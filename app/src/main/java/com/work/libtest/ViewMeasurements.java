package com.work.libtest;

import android.content.Intent;
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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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


    int numberOfMeasurements = 0;

    private MeasurementArrayAdapter measurementArrayAdapter;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_measurements);
        Toolbar toolbar = findViewById(R.id.toolbar);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        mConnectionStatus = intent.getStringExtra(EXTRA_DEVICE_CONNECTION_STATUS);
        mNextDepth = intent.getStringExtra(EXTRA_NEXT_DEPTH);
        mPrevDepth = intent.getStringExtra(EXTRA_PREV_DEPTH);
        Log.d(TAG, "Device name: " + mDeviceName + ", Device address: " + mDeviceAddress);

        rawProbeDataOg = TakeMeasurements.rawData;
        Set<String> set = new LinkedHashSet<>(rawProbeDataOg);
        rawProbeData = new ArrayList<>(set);
        Log.e(TAG, "Raw probe Data: " + rawProbeData);
        TakeMeasurements.rawData = (ArrayList<String>) rawProbeData;

        timeData = TakeMeasurements.timeData;
//        Set<String> set2 = new LinkedHashSet<>(timeData2);
//        timeData = new ArrayList<>(set2);
//        TakeMeasurements.rawData = (ArrayList<String>)timeData;

        dateData = TakeMeasurements.dateData;

        try {
            depthDataN = TakeMeasurements.depthData;
            Log.e(TAG, "Depth data: " + depthDataN);
        } catch (Exception e) {
            Log.e(TAG, "Error setting depth data array");
        }

        try {
            tempData = TakeMeasurements.tempData;
            Log.e(TAG, "Temp data: " + tempData);
        } catch (Exception e) {
            Log.e(TAG, "Error setting full temp data array: " + e);
        }

        listView = (ListView) findViewById(R.id.listView);
        measurementArrayAdapter = new MeasurementArrayAdapter(getApplicationContext(), R.layout.listview_row_layout);
        listView.setAdapter(measurementArrayAdapter);

        List<String[]> measurementList = readData();
        for (String[] measurementDatas:measurementList) {
            double[] displayData = new double[3];
            displayData = formatData(measurementDatas[4]);

            String measurementName = measurementDatas[0];
            String date = measurementDatas[1];
            String time = measurementDatas[2];
            String temp = measurementDatas[3];
            String nanotesla = measurementDatas[4];
            String depth = measurementDatas[5];
//            String dip = measurementDatas[6];
            String dip = String.valueOf(displayData[0]);
            dipData.add(dip);
            String roll = String.valueOf(displayData[1]);
            rollData.add(roll);
//            String roll = measurementDatas[7];
            String azimuth = String.valueOf(displayData[2]);
            azimuthData.add(azimuth);
//            String azimuth = measurementDatas[8];

            Measurement measurement = new Measurement(measurementName, date, time, temp, nanotesla, depth, dip, roll, azimuth);
            measurementArrayAdapter.add(measurement);
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    goToOrientation(position);
                } catch (Exception e) {
                    Log.e(TAG, "Exception thrown: " + e);
                }
            }
        });


        setSupportActionBar(toolbar);
    }

    private void goToOrientation(int position) {
        Intent intent = new Intent(this, OrientationActivity.class);
        Log.e(TAG, "Going to orientation activity for data: " + rawProbeData.get(position));
        try {
            String selectedData = rawProbeData.get(position) + ";" + rollData.get(position) + ";" + dipData.get(position) + ";" + azimuthData.get(position);

            intent.putExtra(OrientationActivity.EXTRA_DEVICE_NAME, mDeviceName);
            intent.putExtra(OrientationActivity.EXTRA_DEVCE_DEVICE_ADDRESS, mDeviceAddress);
            intent.putExtra(OrientationActivity.EXTRA_DEVICE_CONNECTION_STATUS, "Connected");
            intent.putExtra(OrientationActivity.EXTRA_PARENT_ACTIVITY, "MEASUREMENT");
//            intent.putExtra(OrientationActivity.EXTRA_MEASUREMENT_ALL_DATA, rawProbeData);
            intent.putExtra(OrientationActivity.EXTRA_MEASUREMENT_DATA, selectedData);
            intent.putExtra(OrientationActivity.EXTRA_NEXT_DEPTH, mNextDepth);
            intent.putExtra(OrientationActivity.EXTRA_PREV_DEPTH, mPrevDepth);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown in loading orientation activity");
        }
    }


    public List<String[]> readData() {
        List<String[]> resultList = new ArrayList<String[]>();

        for (int i = 0; i < rawProbeData.size(); i++) {
            String[] measurement1 = new String[9];
            measurement1[0] = "Measurement: " + String.valueOf(i);
            try {
                measurement1[1] = dateData.get(i);
            } catch (Exception e) {
                Log.e(TAG, "Exception thrown setting date data: " + e);
            }
            try {
                measurement1[2] = timeData.get(i);
            } catch (Exception e) {
                Log.e(TAG, "Exception thrown while setting time data: " + e);
            }
            try {
                measurement1[3] = tempData.get(i);
            } catch (Exception e) {
                measurement1[3] = "NULL";
                Log.e(TAG, "Error getting the temp data: " + e);
            }
            try {
                measurement1[4] = rawProbeData.get(i);
            } catch (Exception e) {
                measurement1[4] = "NULL";
                Log.e(TAG, "Exception thrown attempting to set raw probe data: " + e);
            }
            try {
                measurement1[5] = depthDataN.get(i);
            } catch (Exception e) {
                measurement1[5] = "NULL";
                Log.e(TAG, "Error getting the depth data: " + e);
            }
            measurement1[6] = "Blank";
            measurement1[7] = "Blank";
            measurement1[8] = "Blank";
            resultList.add(measurement1);
        }
        return resultList;
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

    private void back() {
        Intent backIntent = new Intent(this, TakeMeasurements.class);
        backIntent.putExtra(TakeMeasurements.EXTRA_DEVICE_NAME, mDeviceName);
        backIntent.putExtra(TakeMeasurements.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        backIntent.putExtra(TakeMeasurements.EXTRA_DEVICE_CONNECTION_STATUS, mConnectionStatus);
        backIntent.putExtra(TakeMeasurements.EXTRA_PREV_DEPTH, mPrevDepth);
        backIntent.putExtra(TakeMeasurements.EXTRA_NEXT_DEPTH, mNextDepth);
        startActivity(backIntent);
    }

    private void removeAll() {

    }

    private void fetch() {
        Intent saveIntent = new Intent(this, SaveData.class);
        saveIntent.putExtra(SaveData.EXTRA_DEVICE_NAME, mDeviceName);
        saveIntent.putExtra(SaveData.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        saveIntent.putExtra(SaveData.EXTRA_DEVICE_SERIAL_NUMBER, "lSerialNumber");
        saveIntent.putExtra(SaveData.EXTRA_DEVICE_DEVICE_ADDRESS, "lDeviceAddress"); //seems stupid
        saveIntent.putExtra(SaveData.EXTRA_DEVICE_VERSION, "lFirmwareVersion"); //seems stupid
        saveIntent.putExtra(SaveData.EXTRA_DEVICE_CONNECTION_STATUS, mConnectionStatus);
        saveIntent.putExtra(SaveData.EXTRA_PARENT_ACTIVITY, "View");
        try {
            if (probeData != null) {
                saveIntent.putParcelableArrayListExtra(SaveData.EXTRA_SAVED_DATA, (ArrayList<? extends Parcelable>) probeData);
                Log.d(TAG, "PRINTING PROBE DATA");
                Log.d(TAG, probeData.get(0).returnData());
            } else {
                Log.e(TAG, "Probe Data is null!");
            }

        } catch (Exception e) {
            Log.e(TAG, "Could not save correct data: " + e);
        }
        //->then pass an array in a weird manner which probably is a headache
        startActivity(saveIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        mConnectionStatus = intent.getStringExtra(EXTRA_DEVICE_CONNECTION_STATUS);
        mNextDepth = intent.getStringExtra(EXTRA_NEXT_DEPTH);
        mPrevDepth = intent.getStringExtra(EXTRA_PREV_DEPTH);
        Log.d(TAG, "Device Name: " + mDeviceName + ", Device Address: " + mDeviceAddress);
    }

    public double[] formatData(String allData) { //return roll, dip, azimuth
        //allData is the data seperated by : between bytes
        int pSurveyNum = 0, pShotFormat = 0, pRecordNumber = 0, pHoleID = 0, pMeasurementNum = 0;
        boolean pShowMeanValues = false, pFastMagOnly = false;
        double pRoll = 0, pDip = 0, pAzimuth = 0, pOrientationTemp = 0, pAccX = 0, pAccY = 0, pAccZ = 0, pAccMagError = 0, pMagX = 0, pMagY = 0, pMagZ = 0, pMagTemp = 0, pAccTemp = 0, pAccelerometer = 0, pMaxMeanAccX = 0, pMaxMeanAccY = 0, pMaxMeanAccZ = 0, pMaxMeanMagX = 0, pMaxMeanMagY = 0, pMaxMeanMagZ = 0;
        String pDate = "", pCompanyName = "", pOperatorName = "", pTime = "", pDepth = "", pProbeID = "";

        String[] shot_data = allData.split(":", 20);
        String shot_format = shot_data[0];
        Log.d(TAG, "shot format: " + shot_format);
        pShotFormat = Integer.parseInt(shot_format);
        String highByte = "", lowByte = "";
        String core_ax_binary = "";
        String core_ay_binary = "";
        String core_az_binary = "";
        String core_ax = "", core_ay = "", core_az = "";
        String mag_x = "", mag_y = "", mag_z = "";
        String mag_x_binary = "", mag_y_binary = "", mag_z_binary = "";
        double oRoll = 0, oDip = 0, oAzimuth = 0;

        switch (shot_format) {
            case "1":
                break;
            case "2":
                break;
            case "3":
                for (int i = 0; i < shot_data.length; i++) {
                    switch (i) { //the 2nd least efficient way to do this!
                        case 0:
                            //shot format -> not needed to display a measurement
                            break;
                        case 1:
                            try {
                                highByte = shot_data[i];
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown in high byte: " + e);
                            }
                            break;
                        case 2:
                            try {
                                int value = 0;
                                if (Integer.valueOf(highByte) > 0 && Integer.valueOf(highByte) < 128) {
                                    value = (128 * Integer.valueOf(highByte));
                                } else if (Integer.valueOf(highByte) < 0 && Integer.valueOf(highByte) >= -128) {
                                    value = (128 * (128 + Integer.valueOf(highByte))) + (128 * 127);
                                }
                                lowByte = shot_data[i];
                                if (Integer.valueOf(lowByte) > 0 && Integer.valueOf(lowByte) < 128) {
                                    value = value + Integer.valueOf(lowByte);
                                } else if (Integer.valueOf(lowByte) < 1 && Integer.valueOf(lowByte) >= -128) {
                                    value = 128 + (128 + Integer.valueOf(lowByte));
                                }
                                //Record number = value, probs dont need anywhere here
                                pRecordNumber = value;
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown in low byte: " + e);
                            }
                            break;
                        case 3: //probe temp 1
//                            setTemp(shot_data[i]); //FIXME
                            double temp = Double.parseDouble(shot_data[i]);
                            if (temp < 0) {
                                temp = temp * -1;
                            }
                            tempData.add(String.valueOf(temp));
                            pAccTemp = temp;
                            break;
                        case 4:
                            //Currently not working
                            break;
                        case 5:
                            try {
                                core_ax_binary = "";
                                core_ax = shot_data[i];
                                int num = Integer.parseInt(shot_data[i]);
                                if (num < 0) {
                                    num = 128 + (128 + num);
                                }
                                String binaryOutput = Integer.toBinaryString(num);
                                if (Integer.toBinaryString(num).length() < 8) {
                                    for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                        binaryOutput = "0" + binaryOutput;
                                    }
                                }
                                core_ax_binary = binaryOutput;
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown: " + e);
                            }
                            break;
                        case 6:
                            try {

                                int num2 = Integer.parseInt(shot_data[i]);
                                if (num2 < 0) {
                                    num2 = 128 + (128 + num2);
                                }
                                String binaryOutput2 = Integer.toBinaryString(num2);
                                if (Integer.toBinaryString(num2).length() < 8) {
                                    for (int k = 8; k > Integer.toBinaryString(num2).length(); k--) {
                                        binaryOutput2 = "0" + binaryOutput2;
                                    }
                                }
                                core_ax_binary = core_ax_binary + binaryOutput2;

                                if (core_ax_binary.length() > 16) {
                                    Log.e(TAG, "Error wrong length");
                                } else {
                                    if (core_ax_binary.charAt(0) == '1') {
                                        core_ax_binary = core_ax_binary.substring(1);
                                        core_ax = Integer.toString(Integer.parseInt(core_ax_binary, 2));
                                        core_ax = Integer.toString(Integer.valueOf(core_ax) * -1);
                                        //Set measurement value
                                        pAccX = Double.parseDouble(core_ax);

                                    } else {
                                        core_ax = Integer.toString(Integer.parseInt(core_ax_binary, 2));
                                        //Set measurement value
                                        pAccX = Double.parseDouble(core_ax);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown: " + e);
                            }
                            break;
                        case 7:
                            try {
                                core_ay_binary = "";
                                core_ay = shot_data[i];
                                int num3 = Integer.parseInt(shot_data[i]);
                                if (num3 < 0) {
                                    //need to convert from -128 - 0 to a positive number
                                    num3 = 128 + (128 + num3);
                                }
    //                                            Log.e(TAG, "record number byte 1 in integer form is: " + num); //Seems not helpful
                                String binaryOutput3 = Integer.toBinaryString(num3);
                                if (Integer.toBinaryString(num3).length() < 8) {
                                    for (int k = 8; k > Integer.toBinaryString(num3).length(); k--) {
                                        binaryOutput3 = "0" + binaryOutput3;
                                    }
                                }
                                core_ay_binary = binaryOutput3;
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown: " + e);
                            }
                            break;
                        case 8:
                            try {

                                int num4 = Integer.parseInt(shot_data[i]);
                                if (num4 < 0) {
                                    num4 = 128 + (128 + num4);
                                }
                                String binaryOutput4 = Integer.toBinaryString(num4);
                                if (Integer.toBinaryString(num4).length() < 8) {
                                    for (int k = 8; k > Integer.toBinaryString(num4).length(); k--) {
                                        binaryOutput4 = "0" + binaryOutput4;
                                    }
                                }
                                core_ay_binary = core_ay_binary + binaryOutput4;

                                if (core_ay_binary.length() > 16) {
                                    Log.e(TAG, "Error, AY binary longer than 16 bits");
                                } else {
                                    if (core_ay_binary.charAt(0) == '1') {
                                        core_ay_binary = core_ay_binary.substring(1);
                                        core_ay = Integer.toString(Integer.parseInt(core_ay_binary, 2));
                                        core_ay = Integer.toString(Integer.valueOf(core_ay) * -1);
                                        //Set AY value
                                        pAccY = Double.parseDouble(core_ay);
                                    } else {
                                        core_ay = Integer.toString(Integer.parseInt(core_ay_binary, 2));
                                        //set AY value
                                        pAccY = Double.parseDouble(core_ay);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown: " + e);
                            }
                            break;
                        case 9:
                            try {
                                core_az_binary = "";
                                core_az = shot_data[i];
                                int num5 = Integer.parseInt(shot_data[i]);
                                if (num5 < 0) {
                                    //need to convert from -128 - 0 to a positive number
                                    num5 = 128 + (128 + num5);
                                }
                                String binaryOutput5 = Integer.toBinaryString(num5);
                                if (Integer.toBinaryString(num5).length() < 8) {
                                    for (int k = 8; k > Integer.toBinaryString(num5).length(); k--) {
                                        binaryOutput5 = "0" + binaryOutput5;
                                    }
                                }
                                core_az_binary = binaryOutput5;
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown: " + e);
                            }
                            break;
                        case 10:
                            try {

                                int num6 = Integer.parseInt(shot_data[i]);
                                if (num6 < 0) {
                                    num6 = 128 + (128 + num6);
                                }

                                String binaryOutput6 = Integer.toBinaryString(num6);
                                if (Integer.toBinaryString(num6).length() < 8) {
                                    for (int k = 8; k > Integer.toBinaryString(num6).length(); k--) {
                                        binaryOutput6 = "0" + binaryOutput6;
                                    }
                                }
                                core_az_binary = core_az_binary + binaryOutput6;

                                if (core_az_binary.length() > 16) {
                                    Log.e(TAG, "Error, AY binary longer than 16 bits");
                                } else {
                                    if (core_az_binary.charAt(0) == '1') {
                                        core_az_binary = core_az_binary.substring(1);
                                        core_az = Integer.toString(Integer.parseInt(core_az_binary, 2));
                                        core_az = Integer.toString(Integer.valueOf(core_az) * -1);
                                        //set AZ
                                        pAccZ = Double.parseDouble(core_az);
                                    } else {
                                        core_az = Integer.toString(Integer.parseInt(core_az_binary, 2));
                                        //set AZ
                                        pAccZ = Double.parseDouble(core_az);
                                    }
                                }
                                //calc roll and dip
                                oRoll = calculateRoll(Integer.valueOf(core_ay), Integer.valueOf(core_az));
                                oDip = calculateDip(Integer.valueOf(core_ax), Integer.valueOf(core_ay), Integer.valueOf(core_az));
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown: " + e);
                            }
                            break;
                        case 11:
                            try {
                                mag_x = shot_data[i];
                                int num7 = Integer.parseInt(shot_data[i]);
                                if (num7 < 0) {
                                    num7 = 128 + (128 + num7);
                                }
                                String binaryOutput7 = Integer.toBinaryString(num7);
                                if (Integer.toBinaryString(num7).length() < 8) {
                                    for (int k = 8; k > Integer.toBinaryString(num7).length(); k--) {
                                        binaryOutput7 = "0" + binaryOutput7;
                                    }
                                }
                                mag_x_binary = binaryOutput7;

                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown: " + e);
                            }
                            break;
                        case 12:
                            try {
                                int num8 = Integer.parseInt(shot_data[i]);
                                if (num8 < 0) {
                                    num8  = 128 + (128 + num8);
                                }

                                String binaryOutput8 = Integer.toBinaryString(num8);
                                if (Integer.toBinaryString(num8).length() < 8) {
                                    for (int k = 8; k > Integer.toBinaryString(num8).length(); k--) {
                                        binaryOutput8 = "0" + binaryOutput8;
                                    }
                                }
                                mag_x_binary = mag_x_binary + binaryOutput8;
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown: " + e);
                            }
                            break;
                        case 13:
                            try {

                                int num9 = Integer.parseInt(shot_data[i]);
                                if (num9 < 0) {
                                    num9  = 128 + (128 + num9);
                                }

                                String binaryOutput9 = Integer.toBinaryString(num9);
                                if (Integer.toBinaryString(num9).length() < 8) {
                                    for (int k = 8; k > Integer.toBinaryString(num9).length(); k--) {
                                        binaryOutput9 = "0" + binaryOutput9;
                                    }
                                }
                                mag_x_binary = mag_x_binary + binaryOutput9;

                                if (mag_x_binary.length() != 24) {
                                    Log.e(TAG, "ERROR, Mag x binary length not 24 bytes");
                                } else {
                                    if (mag_x_binary.charAt(0) == '1') {
                                        mag_x_binary = mag_x_binary.substring(1);
                                        mag_x = Integer.toString(Integer.parseInt(mag_x_binary, 2));
                                        mag_x = Integer.toString(Integer.valueOf(mag_x) * -1);
                                        //set mag x
                                        pMagX = Double.parseDouble(mag_x);
                                    } else {
                                        mag_x = Integer.toString(Integer.parseInt(mag_x_binary, 2));
                                        //set mag x
                                        pMagX = Double.parseDouble(mag_x);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown: " + e);
                            }

                            break;
                        case 14:
                            try {
                                mag_y = shot_data[i];
                                int num10 = Integer.parseInt(shot_data[i]);
                                if (num10 < 0) {
                                    num10 = 128 + (128 + num10);
                                }
                                String binaryOutput10 = Integer.toBinaryString(num10);
                                if (Integer.toBinaryString(num10).length() < 8) {
                                    for (int k = 8; k > Integer.toBinaryString(num10).length(); k--) {
                                        binaryOutput10 = "0" + binaryOutput10;
                                    }
                                }
                                mag_y_binary = binaryOutput10;
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown: " + e);
                            }
                            break;
                        case 15:
                            try {
                                int num11 = Integer.parseInt(shot_data[i]);
                                if (num11 < 0) {
                                    num11  = 128 + (128 + num11);
                                }
                                String binaryOutput11 = Integer.toBinaryString(num11);
                                if (Integer.toBinaryString(num11).length() < 8) {
                                    for (int k = 8; k > Integer.toBinaryString(num11).length(); k--) {
                                        binaryOutput11 = "0" + binaryOutput11;
                                    }
                                }
                                mag_y_binary = mag_y_binary + binaryOutput11;
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown: " + e);
                            }
                            break;
                        case 16:
                            try {

                                int num12 = Integer.parseInt(shot_data[i]);
                                if (num12 < 0) {
                                    num12  = 128 + (128 + num12);
                                }
                                String binaryOutput12 = Integer.toBinaryString(num12);
                                if (Integer.toBinaryString(num12).length() < 8) {
                                    for (int k = 8; k > Integer.toBinaryString(num12).length(); k--) {
                                        binaryOutput12 = "0" + binaryOutput12;
                                    }
                                }
                                mag_y_binary = mag_y_binary + binaryOutput12;

                                if (mag_y_binary.length() != 24) {
                                    Log.e(TAG, "ERROR, Mag x binary length not 24 bytes");
                                } else {
                                    if (mag_y_binary.charAt(0) == '1') {
                                        mag_y_binary = mag_y_binary.substring(1);
                                        mag_y = Integer.toString(Integer.parseInt(mag_y_binary, 2));
                                        mag_y = Integer.toString(Integer.valueOf(mag_y) * -1);
                                        //set mag y
                                        pMagY = Double.parseDouble(mag_y);
                                    } else {
                                        mag_y = Integer.toString(Integer.parseInt(mag_y_binary, 2));
                                        //set mag y
                                        pMagY = Double.parseDouble(mag_y);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown: " + e);
                            }
                            break;
                        case 17:
                            try {
                                mag_z = shot_data[i];
                                int num13 = Integer.parseInt(shot_data[i]);
                                if (num13 < 0) {
                                    num13 = 128 + (128 + num13);
                                }
                                String binaryOutput13 = Integer.toBinaryString(num13);
                                if (Integer.toBinaryString(num13).length() < 8) {
                                    for (int k = 8; k > Integer.toBinaryString(num13).length(); k--) {
                                        binaryOutput13 = "0" + binaryOutput13;
                                    }
                                }
                                mag_z_binary = binaryOutput13;
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown: " + e);
                            }
                            break;
                        case 18:
                            try {
                                int num14 = Integer.parseInt(shot_data[i]);
                                if (num14 < 0) {
                                    num14  = 128 + (128 + num14);
                                }

                                String binaryOutput14 = Integer.toBinaryString(num14);
                                if (Integer.toBinaryString(num14).length() < 8) {
                                    for (int k = 8; k > Integer.toBinaryString(num14).length(); k--) {
                                        binaryOutput14 = "0" + binaryOutput14;
                                    }
                                }
                                mag_z_binary = mag_z_binary + binaryOutput14;
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown: " + e);
                            }
                            break;
                        case 19:
                            try {
                                String number = shot_data[i].replace(":", "");
                                int num15 = Integer.parseInt(number);
                                if (num15 < 0) {
                                    num15  = 128 + (128 + num15);
                                }

                                String binaryOutput15 = Integer.toBinaryString(num15);
                                if (Integer.toBinaryString(num15).length() < 8) {
                                    for (int k = 8; k > Integer.toBinaryString(num15).length(); k--) {
                                        binaryOutput15 = "0" + binaryOutput15;
                                    }
                                }
                                mag_z_binary = mag_z_binary + binaryOutput15;

                                if (mag_z_binary.length() != 24) {
                                    Log.e(TAG, "ERROR, Mag x binary length not 24 bytes");
                                } else {
                                    if (mag_z_binary.charAt(0) == '1') {
                                        mag_z_binary = mag_z_binary.substring(1);
                                        mag_z = Integer.toString(Integer.parseInt(mag_z_binary, 2));
                                        mag_z = Integer.toString(Integer.valueOf(mag_z) * -1);
                                        //set mag z
                                        pMagZ = Double.parseDouble(mag_z);
                                        oAzimuth = calculateAzimuth(Double.valueOf(mag_x), Double.valueOf(mag_y), Double.valueOf(mag_z), oRoll, oDip);
                                        pAzimuth = oAzimuth;
                                        Log.e(TAG, "Azimuth: " + pAzimuth);
                                    } else {
                                        mag_z = Integer.toString(Integer.parseInt(mag_z_binary, 2));
                                        //set mag z and calculate azimuth
                                        pMagZ = Double.parseDouble(mag_z);
                                        oAzimuth = calculateAzimuth(Double.valueOf(mag_x), Double.valueOf(mag_y), Double.valueOf(mag_z), oRoll, oDip);
                                        pAzimuth = oAzimuth;
                                        Log.e(TAG, "Azimuth: " + pAzimuth);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown: " + e);
                            }
                            break;

                    }
                }
                break;
        }
        double[] returnArray = new double[3];
        returnArray[0] = oDip;
        returnArray[1] = oRoll;
        returnArray[2] = oAzimuth;


        ProbeData newProbeData = new ProbeData(pSurveyNum, pCompanyName, pOperatorName, pProbeID, pHoleID, pMeasurementNum, pDepth, pDate, pTime, pShotFormat, pRecordNumber, pRoll, pDip, pOrientationTemp, pAccX, pAccY, pAccZ, pAccTemp, pAccMagError, pShowMeanValues, pMaxMeanAccX, pMaxMeanAccY, pMaxMeanAccZ, pFastMagOnly);
        boolean set = true;
        for (int i = 0; i < probeData.size(); i++) {
            if (probeData.get(i) == newProbeData) {
                set = false;
            }
        }
        if (set) {
            probeData.add(newProbeData);
        }
        return returnArray;
    }

    private double calculateRoll(double ay, double az) {
        try {
            double roll = 0;
            double oRoll = 0;
            roll = Math.atan((ay / az));
            roll = Math.toDegrees(roll);
            DecimalFormat numberFormat = new DecimalFormat("#.00");
            oRoll = Double.parseDouble(numberFormat.format(roll));
            return oRoll;
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown when calculating roll: " + e);
            return -1;
        }
    }

    private double calculateDip(double ax, double ay, double az) {
        try {
            double dip = 0, oDip = 0;
            dip = Math.atan((-ax) / (Math.sqrt((ay * ay) + (az * az))));
            dip = Math.toDegrees(dip);
            DecimalFormat numberFormat = new DecimalFormat("#.00");
            oDip = Double.parseDouble(numberFormat.format(dip));
            return oDip;
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown when calculating dip: " + e);
            return -1;
        }
    }

    private double calculateAzimuth(double mx, double my, double mz, double roll, double dip) {
        try {
            double azimuth = 0, oAzimuth = 0;
            azimuth = Math.atan(((-my) * Math.cos(roll) + mz * Math.sin(roll)) / (mx * Math.cos(dip) + my * Math.sin(dip) + mz * Math.sin(dip) * Math.cos(dip)));
            DecimalFormat numberFormat = new DecimalFormat("#.00");
            oAzimuth = Double.parseDouble(numberFormat.format(azimuth));
            return oAzimuth;
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown when calculating azimuth: " + e);
            return -1;
        }
    }
}