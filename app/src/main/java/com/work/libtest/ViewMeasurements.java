package com.work.libtest;

import android.content.Intent;
import android.icu.util.Measure;
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

import com.work.libtest.SurveyOptions.AllSurveyOptionsActivity;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
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

    public static ArrayList<ProbeData> fullProbeData = new ArrayList<ProbeData>();

    int numberOfMeasurements = 0;

    private MeasurementArrayAdapter measurementArrayAdapter;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_measurements);
        Toolbar toolbar = findViewById(R.id.toolbar);

        //WORKS
        LinkedList<Measurement> savedProbeData = TakeMeasurements.recordedShots;

        listView = (ListView) findViewById(R.id.listView);
        measurementArrayAdapter = new MeasurementArrayAdapter(getApplicationContext(), R.layout.listview_row_layout);
        listView.setAdapter(measurementArrayAdapter);

        for (int i = 0; i < savedProbeData.size(); i++) {
            DecimalFormat numberFormat = new DecimalFormat("#.0000");
            String measurementName = savedProbeData.get(i).getName();
            String date = savedProbeData.get(i).getDate();
            String time = savedProbeData.get(i).getTime();
            String depth = savedProbeData.get(i).getDepth();
            String roll = savedProbeData.get(i).getRoll();
            String dip = savedProbeData.get(i).getDip();
            String azimuth = savedProbeData.get(i).getAzimuth();
            String temp = savedProbeData.get(i).getTemp();
            Measurement measurement = new Measurement(measurementName, date, time, temp, depth, dip, roll, azimuth); // i feel like this is a bad way of doing things...
            measurementArrayAdapter.add(measurement);
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    Log.e(TAG, "Going to orientation activity on position: " + position);
                    goToOrientation(position);
                } catch (Exception e) {
                    Log.e(TAG, "Exception thrown: " + e);
                }
            }
        });

        setSupportActionBar(toolbar);
    }

    //Will need slight reworking
    private void goToOrientation(int position) {
        Intent intent = new Intent(this, OrientationActivity.class);
        intent.putExtra(OrientationActivity.EXTRA_PARENT_ACTIVITY, "MEASUREMENT");
        String positionToSend = String.valueOf(position);
        intent.putExtra(OrientationActivity.EXTRA_MEASUREMENT_DATA, positionToSend);
        startActivity(intent);
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

    private void removeAll() {
        LinkedList<Measurement> emptyList = new LinkedList<>();
        TakeMeasurements.recordedShots = emptyList;
        measurementArrayAdapter = new MeasurementArrayAdapter(getApplicationContext(), R.layout.listview_row_layout);
        listView.setAdapter(measurementArrayAdapter);
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


    private void fetch() {
//        for (int i = 0; i < probeData.size(); i++) {
//            Log.e(TAG, "PROBE DATA BEING PASSED: " + probeData.get(i).returnData());
//        }

        Intent saveIntent = new Intent(this, SaveData.class);
        saveIntent.putExtra(SaveData.EXTRA_DEVICE_NAME, mDeviceName);
        saveIntent.putExtra(SaveData.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
//        saveIntent.putExtra(SaveData.EXTRA_DEVICE_SERIAL_NUMBER, "lSerialNumber");
//        saveIntent.putExtra(SaveData.EXTRA_DEVICE_DEVICE_ADDRESS, "lDeviceAddress"); //seems stupid
//        saveIntent.putExtra(SaveData.EXTRA_DEVICE_VERSION, "lFirmwareVersion"); //seems stupid
//        saveIntent.putExtra(SaveData.EXTRA_DEVICE_CONNECTION_STATUS, mConnectionStatus);
        saveIntent.putExtra(SaveData.EXTRA_PARENT_ACTIVITY, "View");
//        try {
//            if (probeData != null) {
//                saveIntent.putParcelableArrayListExtra(SaveData.EXTRA_SAVED_DATA, (ArrayList<? extends Parcelable>) probeData);
//                Log.d(TAG, "PRINTING PROBE DATA");
//                Log.d(TAG, probeData.get(0).returnData());
//            } else {
//                Log.e(TAG, "Probe Data is null!");
//            }
//
//        } catch (Exception e) {
//            Log.e(TAG, "Could not save correct data: " + e);
//        }
        startActivity(saveIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();

//        final Intent intent = getIntent();
//        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
//        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
//        mConnectionStatus = intent.getStringExtra(EXTRA_DEVICE_CONNECTION_STATUS);
//        mNextDepth = intent.getStringExtra(EXTRA_NEXT_DEPTH);
//        mPrevDepth = intent.getStringExtra(EXTRA_PREV_DEPTH);
//        Log.d(TAG, "Device Name: " + mDeviceName + ", Device Address: " + mDeviceAddress);
//
//        rawProbeDataOg = TakeMeasurements.rawData;
//        Set<String> set = new LinkedHashSet<>(rawProbeDataOg);
//        rawProbeData = new ArrayList<>(set);
//        Log.e(TAG, "Raw probe Data: " + rawProbeData);
//        TakeMeasurements.rawData = (ArrayList<String>) rawProbeData;
//
//        timeData = TakeMeasurements.timeData;
//        Set<String> set2 = new LinkedHashSet<>(timeData2);
//        timeData = new ArrayList<>(set2);
//        TakeMeasurements.rawData = (ArrayList<String>)timeData;

//        dateData = TakeMeasurements.dateData;
//
//        try {
//            depthDataN = TakeMeasurements.depthData;
//            Log.e(TAG, "Depth data: " + depthDataN);
//        } catch (Exception e) {
//            Log.e(TAG, "Error setting depth data array");
//        }
//
//        try {
//            tempData = TakeMeasurements.tempData;
//            Log.e(TAG, "Temp data: " + tempData);
//        } catch (Exception e) {
//            Log.e(TAG, "Error setting full temp data array: " + e);
//        }
//
//        listView = (ListView) findViewById(R.id.listView);
//        measurementArrayAdapter = new MeasurementArrayAdapter(getApplicationContext(), R.layout.listview_row_layout);
//        listView.setAdapter(measurementArrayAdapter);
//
//        List<String[]> measurementList = readData();
//        for (String[] measurementDatas:measurementList) {
//            double[] displayData = new double[3];
//            displayData = formatData(measurementDatas[4]);
//
//            String measurementName = measurementDatas[0];
//            String date = measurementDatas[1];
//            String time = measurementDatas[2];
//            String temp = measurementDatas[3];
//            String nanotesla = measurementDatas[4];
//            String depth = measurementDatas[5];
////            String dip = measurementDatas[6];
//            String dip = String.valueOf(displayData[0]);
//            dipData.add(dip);
//            String roll = String.valueOf(displayData[1]);
//            rollData.add(roll);
////            String roll = measurementDatas[7];
//            String azimuth = String.valueOf(displayData[2]);
//            azimuthData.add(azimuth);
////            String azimuth = measurementDatas[8];
//
//            Measurement measurement = new Measurement(measurementName, date, time, temp, nanotesla, depth, dip, roll, azimuth);
//            measurementArrayAdapter.add(measurement);
//        }
//
//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                try {
//                    goToOrientation(position);
//                } catch (Exception e) {
//                    Log.e(TAG, "Exception thrown: " + e);
//                }
//            }
//        });
    }

//    public double[] formatData(String allData) { //return roll, dip, azimuth
//        //allData is the data seperated by : between bytes
//        int pSurveyNum = 0, pShotFormat = 0, pRecordNumber = 0, pHoleID = 0, pMeasurementNum = 0;
//        boolean pShowMeanValues = false, pFastMagOnly = false;
//        double pRoll = 0, pDip = 0, pAzimuth = 0, pOrientationTemp = 0, pAccX = 0, pAccY = 0, pAccZ = 0, pAccMagError = 0, pMagX = 0, pMagY = 0, pMagZ = 0, pMagTemp = 0, pAccTemp = 0, pAccelerometer = 0, pMaxMeanAccX = 0, pMaxMeanAccY = 0, pMaxMeanAccZ = 0, pMaxMeanMagX = 0, pMaxMeanMagY = 0, pMaxMeanMagZ = 0;
//        String pDate = "", pCompanyName = "", pOperatorName = "", pTime = "", pDepth = "", pProbeID = "";
//
////        pCompanyName = MainActivity.surveys.get(0).getSurveyOptions().getCompanyName(); //the 0 needs to be updated to the survey number later
////        pOperatorName = MainActivity.surveys.get(0).getSurveyOptions().getOperatorName();
//        if (mDeviceName != null) {
//            pProbeID = mDeviceName;
//        }
//
//        String[] shot_data = allData.split(":", 20);
//        String shot_format = shot_data[0];
//        Log.d(TAG, "shot format: " + shot_format);
//        pShotFormat = Integer.parseInt(shot_format);
//        String highByte = "", lowByte = "";
//        String core_ax_binary = "";
//        String core_ay_binary = "";
//        String core_az_binary = "";
//        String core_ax = "", core_ay = "", core_az = "";
//        String mag_x = "", mag_y = "", mag_z = "";
//        String mag_x_binary = "", mag_y_binary = "", mag_z_binary = "";
//        double oRoll = 0, oDip = 0, oAzimuth = 0;
//
//        switch (shot_format) {
//            case "1":
//                break;
//            case "2":
//                break;
//            case "3":
//                //FORMAT EVERYTHING
//
//                do {
//
//                    Log.e(TAG, "-------------ANNA---------------");
//                    Log.e(TAG, "All data: " + allData);
//                    String[] string_all_bore_shot = {"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0"};
//                    for (int i = 0; i < shot_data.length; i++) {
//                        int bore_value;
//                        try {
//                            bore_value = Integer.valueOf(shot_data[i]);
//                        } catch (Exception e) {
//                            bore_value = -2;
//                        }
//                        try {
//                            if (bore_value < 0) {
//                                String unsignedHex = String.format("%02X", bore_value & 0xff);
//                                string_all_bore_shot[i] = unsignedHex;
//                            } else {
//                                string_all_bore_shot[i] = Integer.toHexString(bore_value);
//                            }
//                        } catch (Exception e) {
//                            string_all_bore_shot[i] = "-1";
//                        }
//                    }
//                    if (string_all_bore_shot.length <= 20) {
//
//
//                        DecimalFormat numberFormat = new DecimalFormat("0.0000000");
//
//                        try {
//                            byte value1 = (byte) Integer.parseInt(string_all_bore_shot[1], 16);
//                            byte value2 = (byte) Integer.parseInt(string_all_bore_shot[2], 16);
////                    recordNumber = Integer.parseInt(numberFormat.format((int) twoBytesToValue(value2, value1)));
//                        } catch (Exception e) {
//                            Log.e(TAG, "EXCEPTION thrown in record number: " + e);
//                        }
////                                  next 2 bytes after probe temperature
//                        try {
//                            byte value1 = (byte) Integer.parseInt(string_all_bore_shot[3], 16);
//                            byte value2 = (byte) Integer.parseInt(string_all_bore_shot[4], 16);
//                            int shotProbeTempRaw = ((int) value1 << 8) + (((int) value2) & 0x00FF);
//                            double probeTempU = (double) shotProbeTempRaw / 256.0;
//                            double probeTemp = CalibrationHelper.temp_param[0] + (CalibrationHelper.temp_param[1] * probeTempU);
//
////                    tempData.add(numberFormat.format(probeTemp));
////                    depthData.add("20m"); //TODO - FIX
//                            pOrientationTemp = probeTemp;
//                            pAccTemp = probeTemp;
//                        } catch (Exception e) {
//                            Log.e(TAG, "Exception thrown in probe temperature: " + e);
//                        }
//
//                        double ux = 0;
//                        double uy = 0;
//                        double uz = 0;
//                        double[] calAcc;
//
//                        //TODO could probably turn this into a function for better code functionality
//                        try {
//                            byte value1 = (byte) Integer.parseInt(string_all_bore_shot[5], 16);
//                            byte value2 = (byte) Integer.parseInt(string_all_bore_shot[6], 16);
//                            int shotAccX = ((int) value1 << 8) + (((int) value2) & 0x00FF);
//                            ux = ((double) shotAccX) / 32.0 / 512.0;
//                        } catch (Exception e) {
//                            Log.e(TAG, "Exception thrown setting ux: " + e);
//                        }
//
//                        try {
//                            byte value1 = (byte) Integer.parseInt(string_all_bore_shot[7], 16);
//                            byte value2 = (byte) Integer.parseInt(string_all_bore_shot[8], 16);
//                            int shotAccY = ((int) value1 << 8) + (((int) value2) & 0x00FF);
//                            uy = ((double) shotAccY) / 32.0 / 512.0;
//                        } catch (Exception e) {
//                            Log.e(TAG, "Exception thrown setting uy: " + e);
//                        }
//
//                        try {
//                            byte value1 = (byte) Integer.parseInt(string_all_bore_shot[9], 16);
//                            byte value2 = (byte) Integer.parseInt(string_all_bore_shot[10], 16);
//                            int shotAccZ = ((int) value1 << 8) + (((int) value2) & 0x00FF);
//                            uz = ((double) shotAccZ) / 32.0 / 512.0;
//
//                        } catch (Exception e) {
//                            Log.e(TAG, "Exception thrown setting uz: " + e);
//                        }
//
//                        double umx = 0;
//                        double umy = 0;
//                        double umz = 0;
//                        double[] calMag;
//
//                        try {
//                            byte value1 = (byte) Integer.parseInt(string_all_bore_shot[11], 16);
//                            byte value2 = (byte) Integer.parseInt(string_all_bore_shot[12], 16);
//                            byte value3 = (byte) Integer.parseInt(string_all_bore_shot[13], 16);
//                            int shotMagX = ((((int) value1 << 8) + (((int) value2) & 0x00FF)) << 8) + (((int) value3) & 0x00FF);
//                            umx = ((double) shotMagX * 0.001);
//                        } catch (Exception e) {
//                            Log.e(TAG, "Exception thrown setting umx");
//                        }
//
//                        try {
//                            byte value1 = (byte) Integer.parseInt(string_all_bore_shot[14], 16);
//                            byte value2 = (byte) Integer.parseInt(string_all_bore_shot[15], 16);
//                            byte value3 = (byte) Integer.parseInt(string_all_bore_shot[16], 16);
//                            int shotMagY = ((((int) value1 << 8) + (((int) value2) & 0x00FF)) << 8) + (((int) value3) & 0x00FF);
//                            umy = ((double) shotMagY * 0.001);
//                        } catch (Exception e) {
//                            Log.e(TAG, "Exception thrown setting umx");
//                        }
//
//                        try {
//                            byte value1 = (byte) Integer.parseInt(string_all_bore_shot[17], 16);
//                            byte value2 = (byte) Integer.parseInt(string_all_bore_shot[18], 16);
//                            byte value3 = (byte) Integer.parseInt(string_all_bore_shot[19], 16);
//                            int shotMagZ = ((((int) value1 << 8) + (((int) value2) & 0x00FF)) << 8) + (((int) value3) & 0x00FF);
//                            umz = ((double) shotMagZ * 0.001);
//                        } catch (Exception e) {
//                            Log.e(TAG, "Exception thrown setting umx");
//                        }
//
//                        calAcc = CalibrationHelper.CalibrationHelp(CalibrationHelper.accelerationCalibration, ux, uy, uz);
//                        calMag = CalibrationHelper.CalibrationHelp(CalibrationHelper.magnetometerCalibration, umx, umy, umz);
//
//                        //Set the display values for x, y and z calibrated values
//                        double cx = calAcc[0];
//                        pAccX = cx;
//                        double cy = calAcc[1];
//                        pAccY = cy;
//                        double cz = calAcc[2];
//                        pAccZ = cz;
//
//                        double cmx = calMag[0];
//                        pMagX = cmx;
//                        double cmy = calMag[1];
//                        pMagY = cmy;
//                        double cmz = calMag[2];
//                        pMagZ = cmz;
//
//                        //Set roll and dip values
//                        double cal_roll_radian = Math.atan2(cy, cz);
//                        if (cal_roll_radian > Math.PI) {
//                            cal_roll_radian -= (2 * Math.PI);
//                        }
//                        if (cal_roll_radian < -Math.PI) {
//                            cal_roll_radian += (2 * Math.PI);
//                        }
//                        double cal_dip_radian = Math.atan2(-cx, Math.sqrt((cy * cy) + (cz * cz)));
//
//                        double den = (cmx * Math.cos(cal_dip_radian)) + (cmy * Math.sin(cal_dip_radian) * Math.sin(cal_roll_radian)) + (cmz * Math.sin(cal_dip_radian) * Math.cos(cal_roll_radian));
//                        double num = (cmy * Math.cos(cal_roll_radian)) - (cmz * Math.sin(cal_roll_radian));
//                        double cal_az_radian = Math.atan2(-num, den);
//                        if (cal_az_radian > Math.PI) {
//                            cal_az_radian -= (2 * Math.PI);
//                        }
//                        if (cal_az_radian < -Math.PI) {
//                            cal_az_radian += (2 * Math.PI);
//                        }
//                        if (cal_az_radian < 0) {
//                            cal_az_radian += (2 * Math.PI);
//                        }
//
//                        //convert to degrees :(
//                        double cal_roll_degree = cal_roll_radian * 180 / Math.PI;
//                        double cal_dip_degree = cal_dip_radian * 180 / Math.PI;
//                        double cal_az_degree = cal_az_radian * 180 / Math.PI;
//
//                        //display orientation data
//
//                        oRoll = Double.parseDouble(numberFormat.format(cal_roll_degree));
//                        pRoll = oRoll;
//                        oDip = Double.parseDouble(numberFormat.format(cal_dip_degree));
//                        pDip = oDip;
//                        oAzimuth = Double.parseDouble(numberFormat.format(cal_az_degree));
//                        pAzimuth = oAzimuth;
//
//                    }
//
//                } while(pDip > 0);
//
//                Log.e(TAG, "Roll: " + oRoll + " Dip" + oDip + " Azimuth" + oAzimuth);
//                break;
//        }
//        double[] returnArray = new double[3];
//        returnArray[0] = oDip;
//        returnArray[1] = oRoll;
//        returnArray[2] = oAzimuth;
//
//
//
//        try {
////            ProbeData newProbeData = new ProbeData(0, "Boreline", "Anna", "8034", -1, -1, "-1", "-1", 6, pRecordNumber, pRoll, pDip, pAzimuth, pOrientationTemp, pAccX, pAccY, pAccZ, Double.parseDouble(mag_x), Double.parseDouble(mag_y), Double.parseDouble(mag_z), pShowMeanValues, pMaxMeanAccX, pMaxMeanAccY, pMaxMeanAccZ, pMaxMeanMagX, pMaxMeanMagY, pMaxMeanMagZ);
//            /**
//             * TODO
//             * Survey num needs to be updated to the survey number run on probe on app
//             * Company name and operator name need to come from the form thingo
//             * probe ID needs to come from the probe name
//             * Hole ID needs to come from form
//             * Measurement number needs to come from takeMeasurements
//             * Depth and date need to come from take Measurements - we already have for the display so it is somewhere in the class
//             */
//            ProbeData newProbeData = new ProbeData(0, pCompanyName, pOperatorName, pProbeID, -1, -1, "-1", "-1,-1", 6, pRecordNumber, pRoll, pDip, pAzimuth, pOrientationTemp, pAccX,pAccY,pAccZ, pMagX, pMagY, pMagZ, false, -1, -1, -1, -1, -1, -1);
//
//            Log.e(TAG, "--------------------ANNA-------------------");
//            Log.e(TAG, "---------------NEW PROBE DATA ---------------");
//            Log.e(TAG, newProbeData.returnTitles());
//            Log.e(TAG, newProbeData.returnData());
//
//            boolean set = true;
//            for (int i = 0; i < probeData.size(); i++) {
//                if (probeData.get(i) == newProbeData) {
//                    set = false;
//                }
//            }
//            if (set) {
//                probeData.add(newProbeData);
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Exception Adding data lol" + e);
//        }
//        return returnArray;
//    }
//
//    private double calculateRoll(double ay, double az) {
//        try {
//            double roll = 0;
//            double oRoll = 0;
//            roll = Math.atan((ay / az));
//            roll = Math.toDegrees(roll);
//            DecimalFormat numberFormat = new DecimalFormat("#.00");
//            oRoll = Double.parseDouble(numberFormat.format(roll));
//            return oRoll;
//        } catch (Exception e) {
//            Log.e(TAG, "Exception thrown when calculating roll: " + e);
//            return -1;
//        }
//    }
//
//    private double calculateDip(double ax, double ay, double az) {
//        try {
//            double dip = 0, oDip = 0;
//            dip = Math.atan((-ax) / (Math.sqrt((ay * ay) + (az * az))));
//            dip = Math.toDegrees(dip);
//            DecimalFormat numberFormat = new DecimalFormat("#.00");
//            oDip = Double.parseDouble(numberFormat.format(dip));
//            return oDip;
//        } catch (Exception e) {
//            Log.e(TAG, "Exception thrown when calculating dip: " + e);
//            return -1;
//        }
//    }
//
//    private double calculateAzimuth(double mx, double my, double mz, double roll, double dip) {
//        try {
//            double azimuth = 0, oAzimuth = 0;
//            azimuth = Math.atan(((-my) * Math.cos(roll) + mz * Math.sin(roll)) / (mx * Math.cos(dip) + my * Math.sin(dip) + mz * Math.sin(dip) * Math.cos(dip)));
//            DecimalFormat numberFormat = new DecimalFormat("#.00");
//            oAzimuth = Double.parseDouble(numberFormat.format(azimuth));
//            return oAzimuth;
//        } catch (Exception e) {
//            Log.e(TAG, "Exception thrown when calculating azimuth: " + e);
//            return -1;
//        }
//    }
}