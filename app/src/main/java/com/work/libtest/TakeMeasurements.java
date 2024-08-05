package com.work.libtest;

/*
NEXT TIME ON THE BACHELOR
Anna finishes being able to select a single record then uses that to record offline!
She also finally gets the fucking calibration working, returning ALL the calibration data and adding it to a variable 
 */

import static com.work.libtest.Operation.OPERATION_NOTIFY;
import static com.work.libtest.Operation.OPERATION_READ;
import static com.work.libtest.Operation.OPERATION_WRITE;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class TakeMeasurements extends AppCompatActivity {
    public String TAG = "Take Measurements";
    public Menu menu;

    public static int shotWriteType = 00;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private String mDeviceName;
    private String mDeviceAddress;
    private String mConnectionStatus;
    private String mPrevDepth;
    private String mNextDepth;

    public static final String EXTRA_DEVICE_NAME = "Device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "Device_address";
    public static final String EXTRA_DEVICE_CONNECTION_STATUS = "Connection_status";
    public static final String EXTRA_MEASUREMENT_TYPE = "Resume_or_new";
    public static final String EXTRA_PREV_DEPTH = "prev depth";
    public static final String EXTRA_NEXT_DEPTH = "next depth";

    int resumePosition = 128; //error code

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<>();
    private boolean mConnected = true;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private Handler handler;

    private BluetoothLeService mBluetoothLeService;
    private BluetoothGatt mBluetoothGatt;

    //probe info
    private TextView connectionStatus;
    private ImageView connectionStatusImg;

    private ImageView collectionNumImg;

    private ConstraintLayout loadingSection;

    private int probeNumber;
    private int shotFormat = 4;
    private int recordNumber = 0; //TODO probably not needed here
    private double roll = 0;
    private double dip = 0;
    private final double azimuth = 0;
    private double accX = 0;
    private double accY = 0;
    private double accZ = 0;
    private double accTemp = 0;
    private final double accMagError = 0;

    private double shotInterval = 0; //TODO

    boolean settingProbeMode = false;

    int dataToBeRead;
    private double magX = 0;
    private double magY = 0;
    private double magZ = 0;
    private final double magTemp = 0;

    private final boolean mdMeanChecked = false;
    private final double mdAcc = 0;
    private final double mdMag = 0;
    private double mdAccX = 0;
    private double mdAccY = 0;
    private double mdAccZ = 0;
    private final boolean fastMagChecked = false;

    public static int shotRequestNum = 1; //pretty sure this indexes from 1 but who knows

    private final Queue<Operation> operations = new LinkedList<>();
    private Operation currentOp;
    private static boolean operationOngoing = false;

    int _probeMode = 0;
    public static ArrayList<ProbeData> probeData = new ArrayList<>();
    public static ArrayList<ProbeData> subProbeData = new ArrayList<>();

    private static final ArrayList<Double> accXData = new ArrayList<>();
    private static final ArrayList<Double> accYData = new ArrayList<>();
    private static final ArrayList<Double> accZData = new ArrayList<>();

    private static final ArrayList<Double> magXData = new ArrayList<>();
    private static final ArrayList<Double> magYData = new ArrayList<>();
    private static final ArrayList<Double> magZData = new ArrayList<>();

    public static ArrayList<String> rawData = new ArrayList<>();

    private static final ArrayList<Integer> shotsToCollect = new ArrayList<>();


    public static ArrayList<String> dateData = new ArrayList<String>();
    public static ArrayList<String> timeData = new ArrayList<String>();
    public static ArrayList<String> depthData = new ArrayList<String>();
    public static ArrayList<String> tempData = new ArrayList<>();

    public static ArrayList<ProbeData> exportProbeData = new ArrayList<>();


    private BluetoothGattCharacteristic CORE_SHOT_CHARACTERISTIC;
    private BluetoothGattCharacteristic BORE_SHOT_CHARACTERISTIC;

    String shot_format = "", record_number = "", probe_temp = "", core_ax = "", core_ay = "", core_az = "", acc_temp = "";
    String record_number_binary = "", core_ax_binary = "", core_ay_binary, core_az_binary, mag_x_binary = "", mag_y_binary = "", mag_z_binary = "";
    String mag_x = "", mag_y = "", mag_z = ""; String shot_interval = "";

    String highByte, lowByte;
    boolean hold = false;
    int numSaved = 0;

    private Button directionButton;
    private Button takeMeasurement;
    private TextView prevDepth;
    private TextView nextDepth;
//    private CheckBox exportAllData;
    private Button withdrawButton;

    private Button setProbeOn;

    double depthInterval = 0;
    double initialDepth = 0;
    int holeID = 0;
    String operatorName = "";
    String companyName = "";

    int measurementNum = 0;
    int dataCollected = 0;

    boolean collectingData = false;
    boolean collect = false;
    boolean secondCollect = false;
    boolean finished = false;
    boolean returning = false;
    boolean viewMeasurements = false;

    private long startTime = 0; //for timer
    public int seconds;
    private int starttime = 0;


    private interface MessageConstants {
        int MESSAGE_READ = 0;
        int MESSAGE_WRITE = 1;
        int MESSAGE_TOAST = 2;
    }

    //TIMER NOT WORKING
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            seconds = (int) (millis / 1000);

            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!mBluetoothAdapter.isEnabled()) {
                updateConnectionState("Disconnected");
            }

            if (connectionStatus.equals("Disconnected")) {
                updateConnectionState("Disconnected");
            }

            if (collectingData && (seconds - starttime <= 15)) {
                int timePassed = seconds - starttime;
                switch (timePassed) {
                    case 0:
                        collectionNumImg.setImageResource(R.drawable.s0);
                        break;
                    case 1:
                        collectionNumImg.setImageResource(R.drawable.s1);
                        break;
                    case 2:
                        collectionNumImg.setImageResource(R.drawable.s2);
                        break;
                    case 3:
                        collectionNumImg.setImageResource(R.drawable.s3);
                        break;
                    case 4:
                        collectionNumImg.setImageResource(R.drawable.s4);
                        break;
                    case 5:
                        collectionNumImg.setImageResource(R.drawable.s5);
                        break;
                    case 6:
                        collectionNumImg.setImageResource(R.drawable.s6);
                        break;
                    case 7:
                        collectionNumImg.setImageResource(R.drawable.s7);
                        break;
                    case 8:
                        collectionNumImg.setImageResource(R.drawable.s8);
                        break;
                    case 9:
                        collectionNumImg.setImageResource(R.drawable.s9);
                        break;
                    case 10:
                        collectionNumImg.setImageResource(R.drawable.s10);
                        break;
                    case 11:
                        collectionNumImg.setImageResource(R.drawable.s11);
                        break;
                    case 12:
                        collectionNumImg.setImageResource(R.drawable.s12);
                        break;
                    case 13:
                        collectionNumImg.setImageResource(R.drawable.s13);
                        break;
                    case 14:
                        collectionNumImg.setImageResource(R.drawable.s14);
                        measurementNum++;
                        takeMeasurement.setText("TAKE MEASUREMENT " + measurementNum);
                        Log.e(TAG, "MEASUREMENT TAKEN");
                        takeMeasurement.setText("TAKE MEASUREMENT " + measurementNum);
                        if (directionButton.getText().equals("IN")) {
                            takeMeasurement.setText("TAKE MEASUREMENT " + measurementNum);
                            double nextDepthNum = Double.parseDouble(nextDepth.getText().toString().replace("NEXT DEPTH: ", ""));
                            prevDepth.setText("PREV DEPTH: " + (nextDepthNum));
                            nextDepth.setText("NEXT DEPTH: " + (nextDepthNum + depthInterval));
                        } else if (directionButton.getText().equals("OUT")) {
                            takeMeasurement.setText("TAKE MEASUREMENT " + measurementNum);
                            double nextDepthNum = Double.parseDouble(nextDepth.getText().toString().replace("NEXT DEPTH: ", ""));
                            prevDepth.setText("PREV DEPTH: " + (nextDepthNum));
                            nextDepth.setText("NEXT DEPTH: " + (nextDepthNum - depthInterval));
                        } else {
                            Log.e(TAG, "ERROR, direction button has invalid text");
                        }
                        break;
                    case 15:
                        collectionNumImg.setImageResource(R.drawable.s15);

                        break;
                }
            } else {
                collectingData = false;
//                Log.e(TAG, "Collected data");
            }

            timerHandler.postDelayed(this, 1000);
        }
    };

    //Queue managers
    public synchronized void request(Operation operation) {
        Log.d(TAG, "requesting operation: " + operation.toString());
        Log.d(TAG, "Current Time: " + seconds);
        try {
            operations.add(operation);
            if (currentOp == null) {
                currentOp = operations.poll();
                performOperation();
            } else {
                Log.e(TAG, "current operation is not null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error thrown while requesting an operation: " + e);
        }

    }

    public synchronized void operationCompleted() {
        Log.d(TAG, "Operation completed, moving to next one");
        currentOp = null;
        if (operations.peek() != null) {
            currentOp = operations.poll();
            performOperation();
        } else {
            if (returning) {
                finished = true;
            }
            Log.d(TAG, "Queue empty");
            settingProbeMode = false;
            if (secondCollect) {
                new CountDownTimer(1000, 1000) {
                    public void onFinish() {
                        loadingSection.setVisibility(View.VISIBLE);
                        settingProbeMode = true;
                        boolean status2 = mBluetoothLeService.writeToProbeMode(01);
                        Log.e(TAG, "STATUS OF WRITE2: " + status2);
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            Log.e(TAG, "Could not sleep" + e);
                        }
                        if (status2) {
                            dataToBeRead = 0;
                            displayGattServices(mBluetoothLeService.getSupportedGattServices());
                        } else {
                            try {
                                if (mConnectionStatus.equals("Connected")) {
                                    updateConnectionState("Connected");
                                } else {
                                    Log.e(TAG, "Device disconnected");
                                    updateConnectionState("Disconnected");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error setting connection state: " + e);
                            }

                        }
                        new CountDownTimer(700, 1) { //definetly inefficicent, but if it works dont touch it lol

                            public void onTick(long millisUntilFinished) {
//                mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
                            }

                            public void onFinish() {
//                    dataToBeRead = 0;
//                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
                            }
                        }.start();
                        collect = true;
                        starttime = seconds;
                        secondCollect = false;
                    }

                    public void onTick(long millisUntilFinished) {
                        // millisUntilFinished    The amount of time until finished.
                    }
                }.start();

            }
            if (collect) {
                loadingSection.setVisibility(View.INVISIBLE);
                setProbeOn.setText("PROBE ON");
            } else {
                setProbeOn.setText("SET PROBE ON");
            }
        }
    }

    public void performOperation() {
        Log.d(TAG, "Performing operation: " + currentOp.getCharacteristic().getUuid().toString());
        if (currentOp != null) {
            operationOngoing = true;
            Log.e(TAG, "Current performing option on service: " + currentOp.getService().getUuid().toString() + " with characteristic: " + currentOp.getCharacteristic().getUuid().toString());
            if (currentOp.getService() != null && currentOp.getCharacteristic() != null && currentOp.getAction() != 0) {
                switch (currentOp.getAction()) {
                    case OPERATION_WRITE:
                        Log.e(TAG, "Writing?!");
                        //TODO write to the character
                        Log.d(TAG, "Attempting to write to a characteristic: " + currentOp.getCharacteristic().getUuid().toString());
                        mBluetoothLeService.writeData(currentOp.getCharacteristic());
                        break;
                    case OPERATION_READ:
                        Log.d(TAG, "Reading characteristic with service: " + currentOp.getService().getUuid().toString() + " and characteristic: " + currentOp.getCharacteristic().getUuid().toString());
                        mBluetoothLeService.readCharacteristic(currentOp.getCharacteristic());
                        break;
                    case OPERATION_NOTIFY:
                        //make the character notify
                        try {
                            mBluetoothLeService.setCharacteristicNotification(currentOp.getCharacteristic(), true);

                        } catch (Exception e) {
                            Log.e(TAG, "CANT SET CHARACTERISTIC TO NOTIFY" + e);
                        }
                        break;
                    default:
                        Log.e(TAG, "Action not valid");
                        break;
                }
            }
        }
    }

    public void resetQueue() {
        operations.clear();
        currentOp = null;
    }

    public void clear(View view) {
//        resetQueue();
        try {
            settingProbeMode = true;
            writeProbe(01);
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown: " + e);
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Enable to initalise bluetooth");
                finish();
            }
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState("Connected");
                Log.d(TAG, "Device Connected");
                invalidateOptionsMenu();
                mConnectionStatus = "Connected";

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState("Disconnected");
                Log.d(TAG, "Device Disconnected");
                clearUI();
                mBluetoothLeService.connect(mDeviceAddress);
                mConnectionStatus = "Disconnected";

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                //Task show information
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//                Log.d(TAG, intent.getStringExtra())

                if (intent.getStringExtra(BluetoothLeService.EXTRA_DATA) != null) {
                    Log.d(TAG, "OTHER DATA !? uwu: " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                } else if (intent.getStringExtra(BluetoothLeService.SERIAL_NUMBER) != null) {
                    Log.d(TAG, "Device Serial number is: " + intent.getStringExtra(BluetoothLeService.SERIAL_NUMBER));
                } else if (intent.getStringExtra(BluetoothLeService.DEVICE_ADDRESS) != null) {
                    Log.d(TAG, "Device Address is: " + intent.getStringExtra(BluetoothLeService.DEVICE_ADDRESS));
                } else if (intent.getStringExtra(BluetoothLeService.MAJOR_FIRMWARE_VERSION) != null) {
                    Log.d(TAG, "Major firmware version is: " + intent.getStringExtra(BluetoothLeService.MAJOR_FIRMWARE_VERSION));
                } else if (intent.getStringExtra(BluetoothLeService.MINOR_FIRMWARE_VERSION) != null) {
                    Log.d(TAG, "Minor firmware version is: " + intent.getStringExtra(BluetoothLeService.MINOR_FIRMWARE_VERSION));
                } else if (intent.getStringExtra(BluetoothLeService.RECORD_COUNT) != null) {
                    Log.d(TAG, "Record count is: " + intent.getStringExtra(BluetoothLeService.RECORD_COUNT));
                } else if (intent.getStringExtra(BluetoothLeService.SHOT_INTERVAL) != null) {
                    Log.d(TAG, "Shot interval is: " + intent.getStringExtra(BluetoothLeService.SHOT_INTERVAL));
                    shotInterval = Double.parseDouble(intent.getStringExtra(BluetoothLeService.SHOT_INTERVAL));
                } else if (intent.getStringExtra(BluetoothLeService.SHOT_REQUEST) != null) {
                    Log.d(TAG, "Shot request is: " + intent.getStringExtra(BluetoothLeService.SHOT_REQUEST));
                } else if (intent.getStringExtra(BluetoothLeService.CORE_SHOT) != null) { //needs to be fixed to be the same as bore shot
                    Log.e(TAG, "CORE SHOT");
                    String coreShotData = intent.getStringExtra(BluetoothLeService.CORE_SHOT);
                    String[] core_shot_data = coreShotData.split(":", 20); //split the data up into bytes
                    shot_format = core_shot_data[0];
                    Log.e(TAG, String.valueOf(core_shot_data));

                    if (!hold) {
//                        collectingData = true;
                        switch (shot_format) {
                            case "1":
                                //first format(12): Format(1), Record number(2), Probe temperature(2), AX(2), AY(2), AZ(2), Accelerometer Temperature(1)
                                Log.e(TAG, "SHOT FORMAT 1: CORE SHOT");
                                String a = "", b = "";
                                for (int i = 0; i < core_shot_data.length; i++) {
                                    if (i == 0) {
                                        shotFormat = Integer.valueOf(shot_format);
//                                        dev_shot_format.setText(shot_format);
                                    } else if (i == 1) {
                                        try {
                                            highByte = core_shot_data[i];
//                                            Log.e(TAG, "high byte: " + highByte);
                                        } catch (Exception e) {
                                            Log.e(TAG, "Exception thrown in high byte: " + e);
                                        }
                                    } else if (i == 2) {
                                        try {
                                            int value = 0;
                                            if (Integer.valueOf(highByte) > 0 && Integer.valueOf(highByte) < 128) {
                                                value = (128 * Integer.valueOf(highByte));
                                            } else if (Integer.valueOf(highByte) < 0 && Integer.valueOf(highByte) >= -128) {
                                                value = (128 * (128 + Integer.valueOf(highByte))) + (128 * 127);
                                            }
                                            lowByte = core_shot_data[i];
//                                            Log.e(TAG, "low byte: " + lowByte);
                                            if (Integer.valueOf(lowByte) > 0 && Integer.valueOf(lowByte) < 128) {
                                                value = value + Integer.valueOf(lowByte);
                                            } else if (Integer.valueOf(lowByte) < 1 && Integer.valueOf(lowByte) >= -128) {
                                                value = 128 + (128 + Integer.valueOf(lowByte));
                                            }
//                                            Log.e(TAG, "final record number: " + value);
//                                            dev_record_number.setText(Integer.toString(value));
                                            recordNumber = value;
                                        } catch (Exception e) {
                                            Log.e(TAG, "Exception thrown in low byte: " + e);
                                        }
                                    } else if (i == 3) {
                                        probe_temp = core_shot_data[i];
                                        tempData.add(probe_temp);
                                        Log.e(TAG, "Adding: " + nextDepth.getText().toString() + " to depth data");
//                                        depthData.add(nextDepth.getText().toString());
                                    } else if (i == 4) {
                                        //ISSUE the documentation says that temperature should be divided by 256 to be read correct, but this seems to work so idk
                                        probe_temp = probe_temp + "." + core_shot_data[i]; //as it is a fixed point value with 8 binary places
//                                        orientation_temperature_data.setText(probe_temp);
                                        accTemp = Double.parseDouble(probe_temp);
                                    } else if (i == 5) { //AX 1
                                        core_ax_binary = "";
                                        core_ax = core_shot_data[i];
                                        int num = Integer.parseInt(core_shot_data[i]);
                                        if (num < 0) {
                                            //need to convert from -128 - 0 to a positive number
                                            num = 128 + (128 + num);
                                        }
//                                        Log.e(TAG, "record number byte 1 in integer form is: " + num); //Seems not helpful
                                        String binaryOutput = Integer.toBinaryString(num);
                                        if (Integer.toBinaryString(num).length() < 8) {
                                            for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                                binaryOutput = "0" + binaryOutput;
                                            }
                                        }
                                        core_ax_binary = binaryOutput;
//                                        Log.e(TAG, "binary core_ax: " + binaryOutput);
                                    } else if (i == 6) { //AX 2
                                        int num = Integer.parseInt(core_shot_data[i]);
                                        if (num < 0) {
                                            num = 128 + (128 + num);
                                        }
//                                        Log.e(TAG, "record number byte 1 in integer form is: " + num);
                                        String binaryOutput = Integer.toBinaryString(num);
                                        if (Integer.toBinaryString(num).length() < 8) {
                                            for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                                binaryOutput = "0" + binaryOutput;
                                            }
                                        }
                                        core_ax_binary = core_ax_binary + binaryOutput;
//                                        Log.e(TAG, "Core AX Binary: " + core_ax_binary);

                                        if (core_ax_binary.length() > 16) {
                                            Log.e(TAG, "Error, AX binary longer than 16 bits");
//                                            accelerometer_x_data.setText("Error");
                                        } else {
                                            if (core_ax_binary.charAt(0) == '1') {
                                                core_ax_binary = core_ax_binary.substring(1);
                                                core_ax = Integer.toString(Integer.parseInt(core_ax_binary, 2));
                                                core_ax = Integer.toString(Integer.valueOf(core_ax) * -1);
                                                setUncalibratedX(core_ax);
                                                calculateMeanMaxAccX();

                                            } else {
                                                core_ax = Integer.toString(Integer.parseInt(core_ax_binary, 2));
                                                setUncalibratedX(core_ax);
                                                calculateMeanMaxAccX();
                                            }
                                        }
                                    } else if (i == 7) { //AY 1
                                        core_ay_binary = "";
                                        core_ay = core_shot_data[i];
                                        int num = Integer.parseInt(core_shot_data[i]);
                                        if (num < 0) {
                                            //need to convert from -128 - 0 to a positive number
                                            num = 128 + (128 + num);
                                        }
//                                        Log.e(TAG, "record number byte 1 in integer form is: " + num); //Seems not helpful
                                        String binaryOutput = Integer.toBinaryString(num);
                                        if (Integer.toBinaryString(num).length() < 8) {
                                            for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                                binaryOutput = "0" + binaryOutput;
                                            }
                                        }
                                        core_ay_binary = binaryOutput;
//                                        Log.e(TAG, "binary core_ay: " + binaryOutput);
                                    } else if (i == 8) { //AY 2
                                        int num = Integer.parseInt(core_shot_data[i]);
                                        if (num < 0) {
                                            num = 128 + (128 + num);
                                        }
//                                        Log.e(TAG, "record number byte 1 in integer form is: " + num);
                                        String binaryOutput = Integer.toBinaryString(num);
                                        if (Integer.toBinaryString(num).length() < 8) {
                                            for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                                binaryOutput = "0" + binaryOutput;
                                            }
                                        }
                                        core_ay_binary = core_ay_binary + binaryOutput;
//                                        Log.e(TAG, "Core AY Binary: " + core_ay_binary);

                                        if (core_ay_binary.length() > 16) {
                                            Log.e(TAG, "Error, AY binary longer than 16 bits");
//                                            accelerometer_y_data.setText("Error");
                                        } else {
                                            if (core_ay_binary.charAt(0) == '1') {
                                                core_ay_binary = core_ay_binary.substring(1);
                                                core_ay = Integer.toString(Integer.parseInt(core_ay_binary, 2));
                                                core_ay = Integer.toString(Integer.valueOf(core_ay) * -1);
                                                setUncalibratedY(core_ay);
                                                calculateMeanMaxAccY();
                                            } else {
                                                core_ay = Integer.toString(Integer.parseInt(core_ay_binary, 2));
                                                setUncalibratedY(core_ay);
                                                calculateMeanMaxAccY();
                                            }
                                        }
                                    } else if (i == 9) { //AZ 1
                                        core_az_binary = "";
                                        core_az = core_shot_data[i];
                                        int num = Integer.parseInt(core_shot_data[i]);
                                        if (num < 0) {
                                            //need to convert from -128 - 0 to a positive number
                                            num = 128 + (128 + num);
                                        }
//                                        Log.e(TAG, "record number byte 1 in integer form is: " + num); //Seems not helpful
                                        String binaryOutput = Integer.toBinaryString(num);
                                        if (Integer.toBinaryString(num).length() < 8) {
                                            for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                                binaryOutput = "0" + binaryOutput;
                                            }
                                        }
                                        core_az_binary = binaryOutput;
//                                        Log.e(TAG, "binary core_az: " + binaryOutput);
                                    } else if (i == 10) { //AZ 2
                                        int num = Integer.parseInt(core_shot_data[i]);
                                        if (num < 0) {
                                            num = 128 + (128 + num);
                                        }

//                                        Log.e(TAG, "record number byte 1 in integer form is: " + num);
                                        String binaryOutput = Integer.toBinaryString(num);
                                        if (Integer.toBinaryString(num).length() < 8) {
                                            for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                                binaryOutput = "0" + binaryOutput;
                                            }
                                        }
                                        core_az_binary = core_az_binary + binaryOutput;
//                                        Log.e(TAG, "Core AY Binary: " + core_az_binary);

                                        if (core_az_binary.length() > 16) {
                                            Log.e(TAG, "Error, AY binary longer than 16 bits");
//                                            accelerometer_z_data.setText("Error");
                                        } else {
                                            if (core_az_binary.charAt(0) == '1') {
                                                core_az_binary = core_az_binary.substring(1);
                                                core_az = Integer.toString(Integer.parseInt(core_az_binary, 2));
                                                core_az = Integer.toString(Integer.valueOf(core_az) * -1);
                                                setUncalibratedZ(core_az);
                                                calculateMeanMaxZ();
                                            } else {
                                                core_az = Integer.toString(Integer.parseInt(core_az_binary, 2));
                                                setUncalibratedZ(core_az);
                                                calculateMeanMaxZ();
                                            }
                                        }
                                        try {
                                            calculateRoll(Double.valueOf(core_ay), Double.valueOf(core_az));
                                            calculateDip(Double.valueOf(core_ax), Double.valueOf(core_ay), Double.valueOf(core_az));
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error calling calculate roll and dip functions: " + e);
                                        }
                                    } else if (i == 11) { // Temperature
                                        acc_temp = core_shot_data[i];
//                                        Log.d(TAG, "Accelerometer temperature: " + acc_temp);
                                        if (accTemp == -128) {
//                                            accelerometer_temp_data.setText("Not Avaliable");
                                            accTemp = 0;
                                        } else {
//                                            accelerometer_temp_data.setText(acc_temp);
                                            accTemp = Double.parseDouble(acc_temp);
                                        }
                                    }
                                }

                                Log.e(TAG, "Company Name: " + MainActivity.surveys.get(0).getSurveyOptions().getCompanyName());
                                if (collect) {
                                    Log.e(TAG, "DATA COLLECTED: " + dataCollected);
                                    if (dataCollected <= 3) {
                                        double nextDepthNum = Double.parseDouble(nextDepth.getText().toString().replace("NEXT DEPTH: ", ""));
                                        String date_time = String.valueOf(Calendar.getInstance().getTime());
                                        Log.e(TAG, "DATE : " + date_time);
                                        ProbeData newData = new ProbeData(ProbeDataStorage.arrayListNum, companyName, operatorName, mDeviceName, holeID, (measurementNum + 1), Double.toString(nextDepthNum), date_time,null, (shotFormat + 3), recordNumber, roll, dip, accTemp, accX, accY,
                                                accZ, accTemp, accMagError, mdMeanChecked,
                                                mdAccX, mdAccY, mdAccZ, fastMagChecked);
//                                        if (exportAllData.isChecked()) {
//                                            probeData.add(newData);
//                                            ProbeDataStorage.probeDataTotal.add(newData);
//                                        } else { //take an average of the 3 values collected
//                                            subProbeData.add(newData);
//                                            if (dataCollected == 2) {
//                                                int shot__format = 0;
//                                                int record__number = 0;
//                                                double roll_ = 0;
//                                                double dip_ = 0;
//                                                double orientation_temp = 0;
//                                                double acc_x = 0;
//                                                double acc_y = 0;
//                                                double acc_z = 0;
//                                                double acctemp = 0;
//                                                double acc_mag_error = 0;
//                                                boolean show_mean_values = false;
//                                                double max_mean_accX = 0;
//                                                double max_mean_accY = 0;
//                                                double max_mean_accZ = 0;
//                                                boolean fast_mag_only = false;
//                                                if (subProbeData.get(0) != null) {
//                                                    for (int i = 0; i < subProbeData.size(); i++) {
//                                                        roll_ = roll_ + subProbeData.get(i).getRoll();
//                                                        dip_ = dip_ + subProbeData.get(i).getDip();
//                                                        orientation_temp = orientation_temp + subProbeData.get(i).getOrientation_temp();
//                                                        acc_x = acc_x + subProbeData.get(i).getAcc_x();
//                                                        acc_y = acc_y + subProbeData.get(i).getAcc_y();
//                                                        acc_z = acc_z + subProbeData.get(i).getAcc_z();
//                                                        acctemp = acctemp + subProbeData.get(i).getAcc_temp();
//                                                        acc_mag_error = acc_mag_error + subProbeData.get(i).getAcc_mag_error();
//                                                        max_mean_accX = max_mean_accX + subProbeData.get(i).getMax_mean_accX();
//                                                        max_mean_accY = max_mean_accY + subProbeData.get(i).getMax_mean_accY();
//                                                        max_mean_accZ = max_mean_accZ + subProbeData.get(i).getMax_mean_accZ();
//                                                    }
//                                                    shot__format = 4;
//                                                    record__number = subProbeData.get(0).getRecordNumber();
//                                                    roll_ = roll_ / 3;
//                                                    dip_ = dip_ / 3;
//                                                    orientation_temp = orientation_temp / 3;
//                                                    acc_x = acc_x / 3;
//                                                    acc_y = acc_y / 3;
//                                                    acc_z = acc_z / 3;
//                                                    acctemp = acctemp / 3;
//                                                    acc_mag_error = acc_mag_error / 3;
//                                                    show_mean_values = subProbeData.get(0).getShow_mean_values();
//                                                    max_mean_accX = max_mean_accX / 3;
//                                                    max_mean_accY = max_mean_accY / 3;
//                                                    max_mean_accZ = max_mean_accZ / 3;
//                                                    fast_mag_only = subProbeData.get(0).getFast_mag_only();
//                                                    try {
//                                                        double nextDepthNum1 = Double.parseDouble(nextDepth.getText().toString().replace("NEXT DEPTH: ", ""));
//                                                        int survey_num = ProbeDataStorage.probeDataTotal.size();
//                                                        String dateTime = String.valueOf(Calendar.getInstance().getTime());
//                                                        Log.e(TAG, "DATE : " + dateTime);
//                                                        ProbeData new_data = new ProbeData(ProbeDataStorage.arrayListNum, companyName, operatorName, mDeviceName, holeID, (measurementNum + 1), Double.toString(nextDepthNum1), dateTime, shot__format, record__number, roll_, dip_, orientation_temp, acc_x, acc_y, acc_z, acctemp, acc_mag_error,
//                                                                show_mean_values, max_mean_accX, max_mean_accY, max_mean_accZ, fast_mag_only);
//                                                        probeData.add(new_data);
//                                                        ProbeDataStorage.probeDataTotal.add(new_data);
//                                                    } catch (Exception e) {
//                                                        Log.e(TAG, "CANNOT ADD AVERAGE OF PROBE DATA: " + e);
//                                                    }
//                                                }
//                                                subProbeData.removeAll(subProbeData);
//                                            }
                                        }
                                        dataCollected++;
//                                    }
//                                    if (dataCollected == 0) {
//                                        collectionNumImg.setImageResource(R.drawable.old_s0);
//                                    } else if (dataCollected == 1) {
//                                        collectionNumImg.setImageResource(R.drawable.old_s1);
//                                        dataCollected++;
//                                    } else if (dataCollected == 2) {
//                                        collectionNumImg.setImageResource(R.drawable.old_s2);
//
//                                    } else if (dataCollected == 3) {
//                                        collectionNumImg.setImageResource(R.drawable.old_s3);
//

//                                        collect = false;
//                                        measurementNum++;
//                                        dataCollected = 0;

//                                        collectionNumImg.setImageResource(R.drawable.s0);
                                    }


                                    //DO SHIT WITH THE INFO!!!!
//                                }
                                //TODO ISSUE TASK FIXME UNTESTED
                                break;
                            case "2":
                                //second format(19) : Format(1), Record number(2), Probe temperature(2), AX(2), AY(2), AZ(2), Accelerometer Temperature(1), MX (2), MY (2), MZ (2), Magnetometer Temperature (1)
                                Log.e(TAG, "SHOT FORMAT 2: CORE SHOT");
                                break;
                            case "3":
                                //third format(20) : Format(1), Record number(2), Probe temperature(2), AX(2), AY(2), AZ(2), MX(3), MY(3), MZ(3)
                                Log.e(TAG, "SHOT FORMAT 3: CORE SHOT");
                                break;
                            case "4":
                                Log.e(TAG, "SHOT FORMAT 4: CORE SHOT");
                                break;
                            case "5":
                                Log.e(TAG, "SHOT FORMAT 5: CORE SHOT");

                                break;
                            default:
                                Log.e(TAG, "ERROR: Shot format not valid: " + shot_format);
                                break;
                        }
                    }

                } else if (intent.getStringExtra(BluetoothLeService.BORE_SHOT) != null) {
                    String currentRawBoreMeasurement = intent.getStringExtra(BluetoothLeService.BORE_SHOT);
                    Log.d(TAG, "Bore shot raw data is: " + intent.getStringExtra(BluetoothLeService.BORE_SHOT));
                    String boreShotData = intent.getStringExtra(BluetoothLeService.BORE_SHOT);
                    String[] bore_shot_data = boreShotData.split(":", 20); //split the data up into bytes
                    shot_format = bore_shot_data[0];
                    Log.e(TAG, "SHOT FORMAT: " + shot_format);
                    if (!shot_format.equals("0")) {
                        rawData.add(intent.getStringExtra(BluetoothLeService.BORE_SHOT));
                        Log.e(TAG, "Adding: " + nextDepth.getText().toString() + " to depth data");
                        depthData.add(prevDepth.getText().toString().replace("PREV DEPTH: ", ""));
                    }

                    if (!hold) {
                        switch (shot_format) {
                            case "1":
                                break;
                            case "2":
                                //second format(19) : Format(1), Record number(2), Probe temperature(2), AX(2), AY(2), AZ(2), Accelerometer Temperature(1), MX (2), MY (2), MZ (2), Magnetometer Temperature (1)
                                break;
                            case "3":
                                //third format(20) : Format(1), Record number(2), Probe temperature(2), AX(2), AY(2), AZ(2), MX(3), MY(3), MZ(3)
                                String a = "", b = "";

                                try {
                                    for (int i = 0; i < bore_shot_data.length; i++) {
                                        if (i == 0) { //format
                                            shotFormat = Integer.valueOf(shot_format);
                                        } else if (i == 1) { // record number 1
                                            try {
                                                highByte = bore_shot_data[i];
                                            } catch (Exception e) {
                                                Log.e(TAG, "Exception thrown in high byte: " + e);
                                            }
                                        } else if (i == 2) { //record number 2
                                            try {
                                                int value = 0;
                                                if (Integer.valueOf(highByte) > 0 && Integer.valueOf(highByte) < 128) {
                                                    value = (128 * Integer.valueOf(highByte));
                                                } else if (Integer.valueOf(highByte) < 0 && Integer.valueOf(highByte) >= -128) {
                                                    value = (128 * (128 + Integer.valueOf(highByte))) + (128 * 127);
                                                }
                                                lowByte = bore_shot_data[i];
                                                if (Integer.valueOf(lowByte) > 0 && Integer.valueOf(lowByte) < 128) {
                                                    value = value + Integer.valueOf(lowByte);
                                                } else if (Integer.valueOf(lowByte) < 1 && Integer.valueOf(lowByte) >= -128) {
                                                    value = 128 + (128 + Integer.valueOf(lowByte));
                                                }
                                                recordNumber = value;
                                            } catch (Exception e) {
                                                Log.e(TAG, "Exception thrown in low byte: " + e);
                                            }
                                        } else if (i == 3) { //probe temp 1
                                            probe_temp = bore_shot_data[i];
                                            Log.e(TAG, "FORMAT 3 Probe temp 1: " + probe_temp);

                                            double temp = Double.parseDouble(bore_shot_data[i]);
                                            if (temp < 0) {
                                                temp = temp * -1;
                                            }
                                            tempData.add(String.valueOf(temp));
                                            Log.e(TAG, "Adding: " + nextDepth.getText().toString() + " to depth data");
//                                            depthData.add(nextDepth.getText().toString());
                                        } else if (i == 4) { //probe temp 2 ISSUE -> BROKEN
//                                            probe_temp = probe_temp + "." + bore_shot_data[i]; //as it is a fixed point value with 8 binary places
//                                            Log.e(TAG, "FORMAT 3 Probe temp 2: " + probe_temp);
//                                            accTemp = Double.parseDouble(probe_temp);
//                                            tempData.add(String.valueOf(accTemp));


                                        } else if (i == 5) { //AX 1
                                            core_ax_binary = "";
                                            core_ax = bore_shot_data[i];
                                            int num = Integer.parseInt(bore_shot_data[i]);
                                            if (num < 0) {
                                                //need to convert from -128 - 0 to a positive number
                                                num = 128 + (128 + num);
                                            }
//                                            Log.e(TAG, "record number byte 1 in integer form is: " + num); //Seems not helpful
                                            String binaryOutput = Integer.toBinaryString(num);
                                            if (Integer.toBinaryString(num).length() < 8) {
                                                for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                                    binaryOutput = "0" + binaryOutput;
                                                }
                                            }
                                            core_ax_binary = binaryOutput;
//                                            Log.e(TAG, "binary core_ax: " + binaryOutput);
                                        } else if (i == 6) { //AX 2
                                            int num = Integer.parseInt(bore_shot_data[i]);
                                            if (num < 0) {
                                                num = 128 + (128 + num);
                                            }
//                                            Log.e(TAG, "record number byte 1 in integer form is: " + num);
                                            String binaryOutput = Integer.toBinaryString(num);
                                            if (Integer.toBinaryString(num).length() < 8) {
                                                for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                                    binaryOutput = "0" + binaryOutput;
                                                }
                                            }
                                            core_ax_binary = core_ax_binary + binaryOutput;

                                            if (core_ax_binary.length() > 16) {
                                                Log.e(TAG, "Error, AX binary longer than 16 bits");
                                            } else {
                                                if (core_ax_binary.charAt(0) == '1') {
                                                    core_ax_binary = core_ax_binary.substring(1);
                                                    core_ax = Integer.toString(Integer.parseInt(core_ax_binary, 2));
                                                    core_ax = Integer.toString(Integer.valueOf(core_ax) * -1);
                                                    setUncalibratedX(core_ax);
                                                    calculateMeanMaxAccX();

                                                } else {
                                                    core_ax = Integer.toString(Integer.parseInt(core_ax_binary, 2));
                                                    setUncalibratedX(core_ax);
                                                    calculateMeanMaxAccX();
                                                }
                                            }
                                        } else if (i == 7) { //AY 1
                                            core_ay_binary = "";
                                            core_ay = bore_shot_data[i];
                                            int num = Integer.parseInt(bore_shot_data[i]);
                                            if (num < 0) {
                                                //need to convert from -128 - 0 to a positive number
                                                num = 128 + (128 + num);
                                            }
//                                            Log.e(TAG, "record number byte 1 in integer form is: " + num); //Seems not helpful
                                            String binaryOutput = Integer.toBinaryString(num);
                                            if (Integer.toBinaryString(num).length() < 8) {
                                                for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                                    binaryOutput = "0" + binaryOutput;
                                                }
                                            }
                                            core_ay_binary = binaryOutput;
//                                            Log.e(TAG, "binary core_ay: " + binaryOutput);
                                        } else if (i == 8) { //AY 2
                                            int num = Integer.parseInt(bore_shot_data[i]);
                                            if (num < 0) {
                                                num = 128 + (128 + num);
                                            }
//                                            Log.e(TAG, "record number byte 1 in integer form is: " + num);
                                            String binaryOutput = Integer.toBinaryString(num);
                                            if (Integer.toBinaryString(num).length() < 8) {
                                                for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                                    binaryOutput = "0" + binaryOutput;
                                                }
                                            }
                                            core_ay_binary = core_ay_binary + binaryOutput;
//                                            Log.e(TAG, "Core AY Binary: " + core_ay_binary);

                                            if (core_ay_binary.length() > 16) {
//                                                Log.e(TAG, "Error, AY binary longer than 16 bits");
//                                                accelerometer_y_data.setText("Error");
                                            } else {
                                                if (core_ay_binary.charAt(0) == '1') {
                                                    core_ay_binary = core_ay_binary.substring(1);
                                                    core_ay = Integer.toString(Integer.parseInt(core_ay_binary, 2));
                                                    core_ay = Integer.toString(Integer.valueOf(core_ay) * -1);
                                                    setUncalibratedY(core_ay);
                                                    calculateMeanMaxAccY();
                                                } else {
                                                    core_ay = Integer.toString(Integer.parseInt(core_ay_binary, 2));
                                                    setUncalibratedY(core_ay);
                                                    calculateMeanMaxAccY();

                                                }
                                            }
                                        } else if (i == 9) { //AZ 1
                                            core_az_binary = "";
                                            core_az = bore_shot_data[i];
                                            int num = Integer.parseInt(bore_shot_data[i]);
                                            if (num < 0) {
                                                //need to convert from -128 - 0 to a positive number
                                                num = 128 + (128 + num);
                                            }
//                                            Log.e(TAG, "record number byte 1 in integer form is: " + num); //Seems not helpful
                                            String binaryOutput = Integer.toBinaryString(num);
                                            if (Integer.toBinaryString(num).length() < 8) {
                                                for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                                    binaryOutput = "0" + binaryOutput;
                                                }
                                            }
                                            core_az_binary = binaryOutput;
//                                            Log.e(TAG, "binary core_az: " + binaryOutput);
                                        } else if (i == 10) { //AZ 2
                                            int num = Integer.parseInt(bore_shot_data[i]);
                                            if (num < 0) {
                                                num = 128 + (128 + num);
                                            }

//                                            Log.e(TAG, "record number byte 1 in integer form is: " + num);
                                            String binaryOutput = Integer.toBinaryString(num);
                                            if (Integer.toBinaryString(num).length() < 8) {
                                                for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                                    binaryOutput = "0" + binaryOutput;
                                                }
                                            }
                                            core_az_binary = core_az_binary + binaryOutput;
//                                            Log.e(TAG, "Core AY Binary: " + core_az_binary);

                                            if (core_az_binary.length() > 16) {
//                                                Log.e(TAG, "Error, AY binary longer than 16 bits");
//                                                accelerometer_z_data.setText("Error");
                                            } else {
                                                if (core_az_binary.charAt(0) == '1') {
                                                    core_az_binary = core_az_binary.substring(1);
                                                    core_az = Integer.toString(Integer.parseInt(core_az_binary, 2));
                                                    core_az = Integer.toString(Integer.valueOf(core_az) * -1);
                                                    setUncalibratedZ(core_az);
                                                    calculateMeanMaxZ();

                                                } else {
                                                    core_az = Integer.toString(Integer.parseInt(core_az_binary, 2));
                                                    setUncalibratedZ(core_az);
                                                    calculateMeanMaxZ();
                                                }
                                            }
                                            try {
                                                calculateRoll(Double.valueOf(core_ay), Double.valueOf(core_az));
                                                calculateDip(Double.valueOf(core_ax), Double.valueOf(core_ay), Double.valueOf(core_az));
                                            } catch (Exception e) {
                                                Log.e(TAG, "Error calling calculate roll and dip functions: " + e);
                                            }

                                        } else if (i == 11) { // MX 1
                                            try {
                                                mag_x = bore_shot_data[i];
                                                int num = Integer.parseInt(bore_shot_data[i]);
                                                if (num < 0) {
                                                    num = 128 + (128 + num);
                                                }
                                                String binaryOutput = Integer.toBinaryString(num);
                                                if (Integer.toBinaryString(num).length() < 8) {
                                                    for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                                        binaryOutput = "0" + binaryOutput;
                                                    }
                                                }
                                                mag_x_binary = binaryOutput;
                                            } catch (Exception e) {
                                                Log.e(TAG, "Exception thrown MX1: " + e);
                                            }
                                        } else if (i == 12) { //MX 2
                                            try {
                                                int num = Integer.parseInt(bore_shot_data[i]);
                                                if (num < 0) {
                                                    num = 128 + (128 + num);
                                                }

    //                                              Log.e(TAG, "record number byte 1 in integer form is: " + num);
                                                String binaryOutput = Integer.toBinaryString(num);
                                                if (Integer.toBinaryString(num).length() < 8) {
                                                    for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                                        binaryOutput = "0" + binaryOutput;
                                                    }
                                                }
                                                mag_x_binary = mag_x_binary + binaryOutput;

                                            } catch (Exception e) {
                                                Log.e(TAG, "Exception thrown at MX2: " + e);
                                            }

                                        } else if (i == 13) { //MX 3
                                            try {

                                                int num = Integer.parseInt(bore_shot_data[i]);
                                                if (num < 0) {
                                                    num = 128 + (128 + num);
                                                }

                                                String binaryOutput = Integer.toBinaryString(num);
                                                if (Integer.toBinaryString(num).length() < 8) {
                                                    for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                                        binaryOutput = "0" + binaryOutput;
                                                    }
                                                }
                                                mag_x_binary = mag_x_binary + binaryOutput;

                                                if (mag_x_binary.length() != 24) {
                                                    Log.e(TAG, "ERROR, Mag x binary length not 24 bytes");
                                                } else {
                                                    if (mag_x_binary.charAt(0) == '1') {
                                                        mag_x_binary = mag_x_binary.substring(1);
                                                        mag_x = Integer.toString(Integer.parseInt(mag_x_binary, 2));
                                                        mag_x = Integer.toString(Integer.valueOf(mag_x) * -1);
                                                        setUncalibratedMagX(mag_x);
                                                        calculateMeanMaxMagX();
                                                    } else {
                                                        mag_x = Integer.toString(Integer.parseInt(mag_x_binary, 2));
                                                        setUncalibratedMagX(mag_x);
                                                        calculateMeanMaxMagX();
                                                    }
                                                }
                                            } catch (Exception e) {
                                                Log.e(TAG, "Exception thrown MX3: " + e);
                                            }
                                        } else if (i == 14) { //MY 1
                                            try {
                                                mag_y = bore_shot_data[i];
                                                int num = Integer.parseInt(bore_shot_data[i]);
                                                if (num < 0) {
                                                    num = 128 + (128 + num);
                                                }
                                                String binaryOutput = Integer.toBinaryString(num);
                                                if (Integer.toBinaryString(num).length() < 8) {
                                                    for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                                        binaryOutput = "0" + binaryOutput;
                                                    }
                                                }
                                                mag_y_binary = binaryOutput;
                                            } catch (Exception e) {
                                                Log.e(TAG, "Exception throw MY1: " + e);
                                            }
                                        } else if (i == 15) { //MY 2
                                            try {
                                                int num = Integer.parseInt(bore_shot_data[i]);
                                                if (num < 0) {
                                                    num = 128 + (128 + num);
                                                }

    //                                              Log.e(TAG, "record number byte 1 in integer form is: " + num);
                                                String binaryOutput = Integer.toBinaryString(num);
                                                if (Integer.toBinaryString(num).length() < 8) {
                                                    for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                                        binaryOutput = "0" + binaryOutput;
                                                    }
                                                }
                                                mag_y_binary = mag_y_binary + binaryOutput;
                                            } catch (Exception e) {
                                                Log.e(TAG, "Exception thrown MY2: " + e);
                                            }
                                        } else if (i == 16) { //MY 3
                                            try {

                                                int num = Integer.parseInt(bore_shot_data[i]);
                                                if (num < 0) {
                                                    num = 128 + (128 + num);
                                                }

                                                String binaryOutput = Integer.toBinaryString(num);
                                                if (Integer.toBinaryString(num).length() < 8) {
                                                    for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                                        binaryOutput = "0" + binaryOutput;
                                                    }
                                                }
                                                mag_y_binary = mag_y_binary + binaryOutput;

                                                if (mag_y_binary.length() != 24) {
                                                    Log.e(TAG, "ERROR, Mag x binary length not 24 bytes");
                                                } else {
                                                    if (mag_y_binary.charAt(0) == '1') {
                                                        mag_y_binary = mag_y_binary.substring(1);
                                                        mag_y = Integer.toString(Integer.parseInt(mag_y_binary, 2));
                                                        mag_y = Integer.toString(Integer.valueOf(mag_y) * -1);
                                                        setUncalibratedMagY(mag_y);
                                                        calculateMeanMaxMagY();
                                                    } else {
                                                        mag_y = Integer.toString(Integer.parseInt(mag_y_binary, 2));
                                                        setUncalibratedMagY(mag_y);
                                                        calculateMeanMaxMagY();
                                                    }
                                                }
                                            } catch (Exception e) {
                                                Log.e(TAG, "Exception thrown MY3: " + e);
                                            }
                                        } else if (i == 17) { //MZ 1
                                            try {
                                                mag_z = bore_shot_data[i];
                                                int num = Integer.parseInt(bore_shot_data[i]);
                                                if (num < 0) {
                                                    num = 128 + (128 + num);
                                                }
                                                String binaryOutput = Integer.toBinaryString(num);
                                                if (Integer.toBinaryString(num).length() < 8) {
                                                    for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                                        binaryOutput = "0" + binaryOutput;
                                                    }
                                                }
                                                mag_z_binary = binaryOutput;
                                            } catch (Exception e) {
                                                Log.e(TAG, "Exception thrown in MZ1: " + e);
                                            }
                                        } else if (i == 18) { //MZ 2
                                            try {
                                                int num = Integer.parseInt(bore_shot_data[i]);
                                                if (num < 0) {
                                                    num = 128 + (128 + num);
                                                }
    //                                              Log.e(TAG, "record number byte 1 in integer form is: " + num);
                                                String binaryOutput = Integer.toBinaryString(num);
                                                if (Integer.toBinaryString(num).length() < 8) {
                                                    for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                                        binaryOutput = "0" + binaryOutput;
                                                    }
                                                }
                                                mag_z_binary = mag_z_binary + binaryOutput;
                                            } catch (Exception e) {
                                                Log.e(TAG, "Exception thrown in MZ2: " + e);
                                            }
                                        } else if (i == 19) { //MZ 3
                                            try {
                                                String number = bore_shot_data[i].replace(":", "");
//                                                depthData.add(prevDepth.getText().toString().replace("PREV DEPTH: ", ""));
                                                int num = Integer.parseInt(number);
                                                if (num < 0) {
                                                    num = 128 + (128 + num);
                                                }
                                                try {
                                                    String binaryOutput = Integer.toBinaryString(num);
                                                    if (Integer.toBinaryString(num).length() < 8) {
                                                        for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                                            binaryOutput = "0" + binaryOutput;
                                                        }
                                                    }
                                                    mag_z_binary = mag_z_binary + binaryOutput;
                                                } catch (Exception e) {
                                                    Log.e(TAG, "MZ3 first: " + e);
                                                }

                                                try {
                                                    if (mag_z_binary.length() != 24) {
                                                        Log.e(TAG, "ERROR, Mag x binary length not 24 bytes");
                                                    } else {
                                                        if (mag_z_binary.charAt(0) == '1') {
                                                            mag_z_binary = mag_z_binary.substring(1);
                                                            mag_z = Integer.toString(Integer.parseInt(mag_z_binary, 2));
                                                            mag_z = Integer.toString(Integer.valueOf(mag_z) * -1);
                                                            setUncalibratedMagZ(mag_z);
                                                            calculateMeanMaxMagZ();
                                                        } else {
                                                            mag_z = Integer.toString(Integer.parseInt(mag_z_binary, 2));
                                                            setUncalibratedMagZ(mag_z);
                                                            calculateMeanMaxMagZ();
                                                            Log.e(TAG, "YASSSS");
                                                        }
                                                    }
                                                } catch (Exception e) {
                                                    Log.e(TAG, "Exception thrown MZ3: " + e);
                                                }
                                            } catch (Exception e) {
                                                Log.e(TAG, "Exception thrown in MZ3: " + e);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Exception thrown getting data: " + e);
                                }

                                Log.e(TAG, "Company Name: " + MainActivity.surveys.get(0).getSurveyOptions().getCompanyName());
                                double nextDepthNum = Double.parseDouble(nextDepth.getText().toString().replace("NEXT DEPTH: ", ""));
                                //Date and time have to come from whenever you press the record button
                                ProbeData newData = new ProbeData(ProbeDataStorage.arrayListNum, companyName, operatorName, mDeviceName, holeID, (measurementNum + 1), Double.toString(nextDepthNum), dateData.get(0), timeData.get(0), (shotFormat), recordNumber, roll, dip, accTemp, accX, accY,
                                        accZ, accTemp, accMagError, false,
                                        mdAccX, mdAccY, mdAccZ, false); //TODO only some of this data is correct
                                Log.e(TAG, "ADDED SOME NEW DATA: " + newData.returnData());
                                probeData.add(newData);
//                                depthData.add(String.valueOf(nextDepthNum));
                                Log.e(TAG, "Adding: " + nextDepth.getText().toString() + " to depth data");
                                break;
                            case "4":
                                Log.d(TAG, "CASE 4");
                                break;
                            case "5":
                                Log.d(TAG, "CASE 5");
                                break;
                            case "6":
                                Log.d(TAG, "CASE 6");
                                break;
                            default:
                                Log.e(TAG, "ERROR: Shot format not valid: " + shot_format);
                                break;
                        }
                    }
                } else if (intent.getStringExtra(BluetoothLeService.PROBE_MODE) != null) {
                    Log.d(TAG, "Probe Mode is: " + intent.getStringExtra(BluetoothLeService.PROBE_MODE));
                    if (intent.getStringExtra(BluetoothLeService.PROBE_MODE).equals("1")) {
                        _probeMode = 1;
                        Log.d(TAG, "Probe mode set to 1");
                    } else if (intent.getStringExtra(BluetoothLeService.PROBE_MODE).equals("2")) {
                        _probeMode = 2;
                        Log.d(TAG, "Probe mode set to 2");
                    } else {
                        _probeMode = 0;
                        Log.d(TAG, "Probe mode set to 0");
                    }
                } else {

                }
                operationCompleted();

            } else if (intent != null) {
                operationCompleted();
            }
        }
    };

    private void clearUI() {

    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occured when creating an input stream", e);
            }

            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occured when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes;

            while (true) {
                try {
                    numBytes = mmInStream.read(mmBuffer);
                    Message readMsg = handler.obtainMessage(
                            MessageConstants.MESSAGE_READ, numBytes, -1, mmBuffer
                    );
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                Message writtenMsg = handler.obtainMessage(
                        MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer
                );
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occured in sending data", e);
                Message writeErrorMsg = handler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast", "couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                handler.sendMessage(writeErrorMsg);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket");
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateConnectionState("Disconnected");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_measurements);
        Toolbar toolbar = findViewById(R.id.toolbar);

//        shotWriteType = 2;

        probeData.clear();
        dataToBeRead = 0;

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        mConnectionStatus = intent.getStringExtra(EXTRA_DEVICE_CONNECTION_STATUS);
        mPrevDepth = intent.getStringExtra(EXTRA_PREV_DEPTH);
        mNextDepth = intent.getStringExtra(EXTRA_NEXT_DEPTH);
        Log.d(TAG, "Device Name: " + mDeviceName + ", Device Address: " + mDeviceAddress);

        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);



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

        try {
            if (resumePosition != 128) {
                record_number = String.valueOf(ProbeDataStorage.probeDataTotal.get(resumePosition));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error assigning record number: " + e);
        }


        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        connectionStatus = findViewById(R.id.connection_status_text);
        connectionStatusImg = findViewById(R.id.connection_status_img);
        directionButton = findViewById(R.id.direction_button);
        takeMeasurement = findViewById(R.id.take_measurement_button);
        prevDepth = findViewById(R.id.previous_depth_txt);
        nextDepth = findViewById(R.id.next_depth_txt);
        collectionNumImg = findViewById(R.id.collection_num);
//        exportAllData = findViewById(R.id.exportAllData);
        loadingSection = findViewById(R.id.loading);
        setProbeOn = findViewById(R.id.probeOn);
        withdrawButton = findViewById(R.id.withdraw_button);

        loadingSection.setVisibility(View.INVISIBLE);

        takeMeasurement.setText("TAKE MEASUREMENT " + rawData.size());

        try {
//            if (mPrevDepth.length() > 0) {
//                prevDepth.setText(mPrevDepth);
//                nextDepth.setText(mNextDepth);
//
//            } else {
//                Log.e(TAG, "IS NULL");
//            }
            Log.d(TAG, "Prev depth: " + mPrevDepth);
        } catch (Exception e) {
            Log.e(TAG, "Error setting previous depth: " + e);
        }

//        prevDepth.setText("PREV DEPTH: " + (nextDepthNum));
//        nextDepth.setText("NEXT DEPTH: " + (nextDepthNum + depthInterval));

        try {
            if (mConnectionStatus.equals("Connected")) {
                updateConnectionState("Connected");
            } else {
                Log.e(TAG, "Device disconnected");
                updateConnectionState("Disconnected");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting connection state: " + e);
        }


            if (resumePosition != 128) {
                try {
                    holeID = MainActivity.surveys.get(resumePosition).getSurveyOptions().getHoleID();
                    operatorName = MainActivity.surveys.get(resumePosition).getSurveyOptions().getOperatorName();
                    companyName = MainActivity.surveys.get(resumePosition).getSurveyOptions().getCompanyName();
                    initialDepth = MainActivity.surveys.get(resumePosition).getSurveyOptions().getInitialDepth();
                    depthInterval = MainActivity.surveys.get(resumePosition).getSurveyOptions().getDepthInterval();
                } catch (Exception e) {
                    Log.e(TAG, "Exception thrown in on create 1: " + e);
                }
            } else {
                try {
                    holeID = MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().getHoleID();
                    operatorName = MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().getOperatorName();
                    companyName = MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().getCompanyName();
                    initialDepth = MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().getInitialDepth();
                    depthInterval = MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().getDepthInterval();
                } catch (Exception e) {
                    Log.e(TAG, "Exception thrown in on create 2: " + e);
                }
            }


        takeMeasurement.setText("TAKE MEASUREMENT " + measurementNum);
        nextDepth.setText("NEXT DEPTH: " + (initialDepth + depthInterval * measurementNum));


        setSupportActionBar(toolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();

        probeData.clear();

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        mConnectionStatus = intent.getStringExtra(EXTRA_DEVICE_CONNECTION_STATUS);
        mPrevDepth = intent.getStringExtra(EXTRA_PREV_DEPTH);
        mNextDepth = intent.getStringExtra(EXTRA_NEXT_DEPTH);
        Log.d(TAG, "Device Name: " + mDeviceName + ", Device Address: " + mDeviceAddress);

        if (mConnectionStatus.equals("Connected")) {
            updateConnectionState("Connected");
        } else {
            Log.e(TAG, "Device disconnected");
            updateConnectionState("Disconnected");
        }

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connection request result = " + result);
        }

        if (resumePosition != 128) {
            try {
                holeID = MainActivity.surveys.get(resumePosition).getSurveyOptions().getHoleID();
                operatorName = MainActivity.surveys.get(resumePosition).getSurveyOptions().getOperatorName();
                companyName = MainActivity.surveys.get(resumePosition).getSurveyOptions().getCompanyName();
                initialDepth = MainActivity.surveys.get(resumePosition).getSurveyOptions().getInitialDepth();
                depthInterval = MainActivity.surveys.get(resumePosition).getSurveyOptions().getDepthInterval();
            } catch (Exception e) {
                Log.e(TAG, "Exception thrown in on resume 1: " + e);
            }
        } else {
            try {
                holeID = MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().getHoleID();
                operatorName = MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().getOperatorName();
                companyName = MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().getCompanyName();
                initialDepth = MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().getInitialDepth();
                depthInterval = MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().getDepthInterval();
            } catch (Exception e) {
                Log.e(TAG, "Exception thrown in on resume 2: " + e);
            }
        }

        try {
            if (mPrevDepth.length() > 0) {
                prevDepth.setText(mPrevDepth);
                nextDepth.setText(mNextDepth);
            } else {
                Log.e(TAG, "IS NULL");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting previous depth: " + e);
        }

        takeMeasurement.setText("TAKE MEASUREMENT " + rawData.size());
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        String uuid = null;
        String unknownServiceString = "Unknown service";
        String unknownCharaString = "Unknown characteristics";
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicsData = new ArrayList<>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        //HERE
//        settingProbeMode
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<>();

            Log.d(TAG, "Gatt service is: " + gattService.getUuid().toString());
            //look for the device ID
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
//                Log.e(TAG, "Setting probe mode: " + settingProbeMode);
                if (settingProbeMode) {
                    if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.PROBE_MODE)) {
                        charas.add(gattCharacteristic);
                        HashMap<String, String> currentCharaData = new HashMap<>();
                        uuid = gattCharacteristic.getUuid().toString();
                        currentCharaData.put(
                                LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString)
                        );
                        currentCharaData.put(LIST_UUID, uuid);
                        gattCharacteristicGroupData.add(currentCharaData);


                        Log.d(TAG, "Gatt characteristic is: " + gattCharacteristic.getUuid().toString());
                        if (gattCharacteristic.getUuid() != null) {
                            if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.PROBE_MODE) && _probeMode != 1) {
                                Log.e(TAG, "In probe mode, current mode: " + _probeMode);
                                Operation getProbeModeOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);

                                try {
                                    request(getProbeModeOperation);
                                } catch (Exception e) {
                                    Log.e(TAG, "Exception thrown for requesting operation");
                                }

                            }
                        } else {
                            Log.e(TAG, "gatt characteristic uuid is null");
                        }

                    }else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.CORE_SHOT)) {
                        Log.e(TAG, "Notifying core shot");
                        CORE_SHOT_CHARACTERISTIC = gattCharacteristic;
                        Operation getCoreShotNotifyOperation = new Operation(gattService, gattCharacteristic, OPERATION_NOTIFY);

                        try {
                            request(getCoreShotNotifyOperation);
                        } catch (Exception e) {
                            Log.e(TAG, "Exception thrown for requesting operation");
                        }
                    } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.BORE_SHOT)) {
                        Log.e(TAG, "Notifying bore shot");
                        BORE_SHOT_CHARACTERISTIC = gattCharacteristic;
                        Operation getBoreShotNotifyOperation = new Operation(gattService, gattCharacteristic, OPERATION_NOTIFY);

                        try {
                            request(getBoreShotNotifyOperation);
                        } catch (Exception e) {
                            Log.e(TAG, "Exception thrown for requesting operation");
                        }
                    } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.SHOT_REQUEST)) {
                        Log.e(TAG, "Writing to shot request");
                        Operation getShotRequestOperation = new Operation(gattService, gattCharacteristic, OPERATION_WRITE);
                        try {
                            request(getShotRequestOperation);
                        } catch (Exception e) {
                            Log.e(TAG, "Exception thrown for requesting operation");
                        }

                    }
                } else {
                    charas.add(gattCharacteristic);
                    HashMap<String, String> currentCharaData = new HashMap<>();
                    uuid = gattCharacteristic.getUuid().toString();
                    currentCharaData.put(
                            LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString)
                    );
                    currentCharaData.put(LIST_UUID, uuid);
                    gattCharacteristicGroupData.add(currentCharaData);

                    Log.d(TAG, "Gatt characteristic is: " + gattCharacteristic.getUuid().toString());
                    if (gattCharacteristic.getUuid() != null) {
                        if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.DEVICE_ID)) { //FFF1
                            Log.e(TAG, "Attempting to get device ID");
                            Operation getDeviceIDOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);

                            try {
                                request(getDeviceIDOperation);
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown for requesting operation");
                            }
                        } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.DEVICE_ADDRESS)) { //FFF2
                            Log.e(TAG, "Attempting to get device address");
                            Operation getDeviceAddressOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);

                            try {
                                request(getDeviceAddressOperation);
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown for requesting operation");
                            }
                        } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.VERSION_MAJOR)) {
                            Log.e(TAG, "Attempting to get version number (major)");
                            Operation getVersionMajorOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);

                            try {
                                request(getVersionMajorOperation);
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown for requesting operation");
                            }
                        } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.RECORD_COUNT)) {
                            Log.e(TAG, "Attempting to get record count");
                            Operation getRecordCount = new Operation(gattService, gattCharacteristic, OPERATION_READ);
                            try {
                                request(getRecordCount);
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown attempting to get record count");
                            }
                        } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.VERSION_MINOR)) {
                            Log.e(TAG, "Attempting to get version number (minor)");
                            Operation getVersionMinorOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);

                            try {
                                request(getVersionMinorOperation);
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown for requesting operation");
                            }
                        } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.PROBE_MODE) && _probeMode != 1) {
                            Log.e(TAG, "In probe mode, current mode: " + _probeMode);
                            Operation getProbeModeOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);

                            try {
                                request(getProbeModeOperation);
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown for requesting operation");
                            }

                        } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.SHOT_INTERVAL)) {
                            Log.e(TAG, "GETTING SHOT INTERVAL");
                            Operation getShotInteralOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);

                            try {
                                request(getShotInteralOperation);
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown for requesting operation");
                            }
                        } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.CORE_SHOT)) {
                            Log.e(TAG, "Reading core shot");
                            CORE_SHOT_CHARACTERISTIC = gattCharacteristic;
                            Operation getCoreShotOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);

                            try {
                                request(getCoreShotOperation);
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown for requesting operation");
                            }

                            Log.e(TAG, "Notifying core shot");
                            CORE_SHOT_CHARACTERISTIC = gattCharacteristic;
                            Operation getCoreShotNotifyOperation = new Operation(gattService, gattCharacteristic, OPERATION_NOTIFY);

                            try {
                                request(getCoreShotNotifyOperation);
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown for requesting operation");
                            }
                        } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.BORE_SHOT)) {
                            Log.e(TAG, "Reading bore shot");
                            BORE_SHOT_CHARACTERISTIC = gattCharacteristic;
                            Operation getBoreShotOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);

                            try {
                                request(getBoreShotOperation);
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown for requesting operation");
                            }

                            Log.e(TAG, "Notifying bore shot");
                            BORE_SHOT_CHARACTERISTIC = gattCharacteristic;
                            Operation getBoreShotNotifyOperation = new Operation(gattService, gattCharacteristic, OPERATION_NOTIFY);

                            try {
                                request(getBoreShotNotifyOperation);
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown for requesting operation");
                            }
                        } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.SHOT_REQUEST)) {
                            Log.e(TAG, "Writing to shot request");
                            Operation getShotRequestOperation = new Operation(gattService, gattCharacteristic, OPERATION_WRITE);
                            try {
                                request(getShotRequestOperation);
                            } catch (Exception e) {
                                Log.e(TAG, "Exception thrown for requesting operation");
                            }

                        }
                    } else {
                        Log.e(TAG, "gatt characteristic uuid is null");
                    }

                }
                }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void updateConnectionState(final String resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //setText to resource ID to display connection status
                if (resourceId.equals("Connected")) {
                    Log.d(TAG, "DEVICE CONNECTED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    connectionStatus.setText("Connected");
                    connectionStatusImg.setImageResource(R.drawable.ready);
                    mConnectionStatus = "Connected";

                } else if (resourceId.equals("Disconnected")) {
                    Log.d(TAG, "DEVICE DISCONNECTED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    connectionStatus.setText("Disconnected");
                    connectionStatusImg.setImageResource(R.drawable.unconnected);
                    mConnectionStatus = "Disconnected";
                    registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
                    if (mBluetoothLeService != null) {
                        final boolean result = mBluetoothLeService.connect(mDeviceAddress);
                        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
                        Log.d(TAG, "Connection request result=" + result);
                    }
                } else {
//                    connectionStatus.setText("No probe selected");
                    Log.e(TAG, "Error no probe selected");
                }
            }
        });
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_take_measurements, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.back_button) {
            Log.d(TAG, "back button pressed, going back to main acitivity");
            back();
            return true;
        }
        return true;
    }

    public void back() {
//        if (!collectingData) {
            Intent intent = new Intent(this, MainActivity.class);
            Log.d(TAG, "Device name: " + mDeviceName + ", Device Address: " + mDeviceAddress);
            intent.putExtra(MainActivity.EXTRA_DEVICE_NAME, mDeviceName);
            intent.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
            startActivity(intent);
//        }
    }

    public void measurementClick(View view) {
        if (!collectingData && collect) {
            Log.d(TAG, "Take measurement at: " + seconds);
            shotsToCollect.add(seconds); //Get current time and add it to the list of shots to be collected later
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy G");
            String currentDate = sdf.format(new Date());
            dateData.add(currentDate);
            SimpleDateFormat stf = new SimpleDateFormat("HH:mm:ss z");
            String currentTime = stf.format(new Date());
            timeData.add(currentTime);
            Log.e(TAG, "Date: " + currentDate + ", Time: " + currentTime);

            //wait 15 seconds, just don't do anything
            starttime = seconds;
            collectingData = true;
            Log.e(TAG, "Start time = " + starttime );
        }
    }

    public void writeProbe(int mode) {
        if (!collectingData && !collect) {
            loadingSection.setVisibility(View.VISIBLE);
            boolean status = false;
            do {
                status = mBluetoothLeService.writeToProbeMode(00);
                Log.e(TAG, "STATUS OF WRITE: " + status);
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    Log.e(TAG, "Could not sleep" + e);
                }
                if (status) {
                    dataToBeRead = 0;
                    displayGattServices(mBluetoothLeService.getSupportedGattServices());

                    //                if (currentOp == null) {
                    //                    Log.e(TAG, "2nd");
                    //                    dataToBeRead = 0;
                    //                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
                    //                }
                } else {
                    try {
                        if (mConnectionStatus.equals("Connected")) {
                            updateConnectionState("Connected");
                        } else {
                            Log.e(TAG, "Device disconnected");
                            updateConnectionState("Disconnected");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error setting connection state: " + e);
                    }

                }
                new CountDownTimer(700, 1) { //definetly inefficicent, but if it works dont touch it lol

                    public void onTick(long millisUntilFinished) {
                        //                mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
                    }

                    public void onFinish() {
                        //                    dataToBeRead = 0;
                        //                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
                    }
                }.start();

                new CountDownTimer(3000, 1) { //definetly inefficicent, but if it works dont touch it lol

                    public void onTick(long millisUntilFinished) {
                        //                mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
                    }

                    public void onFinish() {
                        //                    dataToBeRead = 0;
                        //                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
                    }
                }.start();

            } while (!status);
            }else{
                //            collectingData = false;
            }


            secondCollect = true;

            Log.e(TAG, "Start-time is: " + starttime);

    }

    public void withdrawClick(View view) {
        Log.e(TAG, "WITHDRAW PROBE");
        if (mConnectionStatus.equals("Connected")) {
            if (!viewMeasurements) {
                //end! Save data page
                try {
                    if (!collectingData) {
                        int recordsCollected = shotsToCollect.size();
                        Log.e(TAG, "Shots collected size: " + recordsCollected);
                        for (int i = 0; i < recordsCollected; i++) {
                            finished = false;
                            returning = true;
                            if (!finished) {
                                int firstByte = 0, secondByte = 0;
                                int record = shotsToCollect.get(i);
                                Log.e(TAG, "Record collected: " + record);
    //                        shotsToCollect.remove(0); //PROBLEM: we dont actually need to remove it bc we are getting the value using i
                                record = record / 10;
                                if (record <= 255) {
                                    firstByte = record;
                                    secondByte = 00;
                                } else if (record > 255 && record >= 510) {
                                    firstByte = -(255 - (record - 255));
                                    secondByte = 00;
                                } else {
                                    secondByte = record / 255;
                                    record = record - (255 * secondByte);
                                    if (record <= 255) {
                                        firstByte = record;
                                    } else if (record > 255 && record >= 510) {
                                        firstByte = -(255 - (record - 255));
                                    }
                                }
                                resetQueue();
                                Log.e(TAG, "Attempting to collect shot information by writing to shot request and reading a bore/core notification");

                                boolean status = false;
                                do {
                                    status = mBluetoothLeService.writeToShotRequest(firstByte, secondByte);
                                    Log.e(TAG, "STATUS OF WRITE: " + status);
                                    try {
                                        Thread.sleep(1000);
                                    } catch (Exception e) {
                                        Log.e(TAG, "Could not sleep");
                                    }

                                    new CountDownTimer(700, 1) { //efficiency? never heard of her
                                        public void onTick(long millisUntilFinished) {
                                            //                      mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
                                        }

                                        public void onFinish() {
                                            //                          dataToBeRead = 0;
//                                                                      displayGattServices(mBluetoothLeService.getSupportedGattServices());
                                        }
                                    }.start();
                                } while (!status);
                            }
                        }
                        //EXPORT DATA - change the text to View Measurements and display how many measurements taken
                        withdrawButton.setText("View Measurements[" + Integer.valueOf(shotsToCollect.size()) + "]");
                        try {
                            Log.e(TAG, "Num of measurements: " + Integer.valueOf(shotsToCollect.size()));
                        } catch (Exception e) {
                            Log.e(TAG, "Exception thrown getting the number of measurements: " + e);
                        }
                        viewMeasurements = true;
                    } else {
                        Log.e(TAG, "Error still collecting data");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Someone fucked up " + e); //Keeps throwing exceptions
                }
            } else {
                // go to page to view measurements TODO HERE
                Intent intent = new Intent(this, ViewMeasurements.class);
                intent.putExtra(ViewMeasurements.EXTRA_DEVICE_NAME, mDeviceName);
                intent.putExtra(ViewMeasurements.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
                intent.putExtra(ViewMeasurements.EXTRA_DEVICE_CONNECTION_STATUS, mConnectionStatus);
                intent.putExtra(ViewMeasurements.EXTRA_PREV_DEPTH, mPrevDepth);
                intent.putExtra(ViewMeasurements.EXTRA_NEXT_DEPTH, mNextDepth);
                Log.e(TAG, "Depth Data: " + depthData);
                startActivity(intent);

            }
        } else {
            Log.e(TAG, "NOT CONNECTED");
        }
    }

    public void changeDirection(View view) {
        if (!collectingData) {
            if (directionButton.getText().toString().equals("IN")) {
                directionButton.setText("OUT");
                double nextDepthNum = Double.parseDouble(nextDepth.getText().toString().replace("NEXT DEPTH: ", ""));
                nextDepth.setText("NEXT DEPTH: " + (nextDepthNum - (depthInterval * 2)));
            } else {
                directionButton.setText("IN");
                double nextDepthNum = Double.parseDouble(nextDepth.getText().toString().replace("NEXT DEPTH: ", ""));
                nextDepth.setText("NEXT DEPTH: " + (nextDepthNum + depthInterval * 2));
            }
        }
    }
    private void setUncalibratedX(String core_ax_value) {
        double uncalibratedAccX = 0;
//        Log.d(TAG, "Core AX value: " + core_ax_value);
        uncalibratedAccX = ((double)Integer.valueOf(core_ax_value) / (double)0x7FFF);
        DecimalFormat numberFormat = new DecimalFormat("0.00000");
        try {
//            accelerometer_x_data.setText(numberFormat.format(uncalibratedAccX) + " G");
            accX = Double.parseDouble(numberFormat.format(uncalibratedAccX));
        } catch (Exception e) {
            Log.e(TAG, "Error setting uncalibrated x: " + e);
        }
        core_ax = Double.toString(uncalibratedAccX); //Bad
        accXData.add(Double.parseDouble(core_ax));
    }

    private void setUncalibratedY(String core_ay_value) {
        double uncalibratedAccY = 0;
//        Log.d(TAG, "Core AY value: " + core_ay_value);
        uncalibratedAccY = ((double)Integer.valueOf(core_ay_value) / (double)0x7FFF);
        DecimalFormat numberFormat = new DecimalFormat("0.00000");
        try {
//            accelerometer_y_data.setText(numberFormat.format(uncalibratedAccY) + " G");
            accY = Double.parseDouble(numberFormat.format(uncalibratedAccY));
        } catch (Exception e) {
            Log.e(TAG, "Error setting uncalibrated y: " + e);
        }
        core_ay = Double.toString(uncalibratedAccY); //Bad
        accYData.add(Double.parseDouble(core_ay));

    }

    private void setUncalibratedZ(String core_az_value) {
        double uncalibratedAccZ = 0;
//        Log.d(TAG, "Core AZ value: " + core_az_value);
        uncalibratedAccZ = ((double)Integer.valueOf(core_az_value) / (double)0x7FFF);
        DecimalFormat numberFormat = new DecimalFormat("0.00000");
        try {
//            accelerometer_z_data.setText(numberFormat.format(uncalibratedAccZ) + " G");
            accZ = Double.parseDouble(numberFormat.format(uncalibratedAccZ));
        } catch (Exception e) {
            Log.e(TAG, "Error setting uncalibrated z: " + e);
        }
        core_az = Double.toString(uncalibratedAccZ); //Bad
        accZData.add(Double.parseDouble(core_az));
    }

    //ISSUE pretty sure this is returning incorrect results
    private void calculateRoll(double ay, double az) {
        try {
//            Log.d(TAG, "calculating roll from ay: " + ay + " az: " + az);
            double _roll = 0;
            _roll = Math.atan((ay / az));
            _roll = Math.toDegrees(_roll);
            DecimalFormat numberFormat = new DecimalFormat("#.00");
//            orientation_roll_data.setText(numberFormat.format(roll) + " °");
            roll = Double.parseDouble(numberFormat.format(_roll));
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown when calculating roll: " + e);
        }
    }

    private void calculateDip(double ax, double ay, double az) {
        try {
//            Log.d(TAG, "calculating dip from ax: " + ax + " ay: " + ay +" az: " + az);
            double _dip = 0;
            _dip = Math.atan((-ax) / (Math.sqrt((ay * ay) + (az * az))));
            _dip = Math.toDegrees(_dip);
            DecimalFormat numberFormat = new DecimalFormat("#.00");
//            orientation_dip_data.setText(numberFormat.format(dip) + " °");
            dip = Double.parseDouble(numberFormat.format(_dip));
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown when calculating dip: " + e);
        }
    }

    private void calculateMeanMaxAccX() {
        double meanMaxValue = 0;
        double maxValue = 0;

        for (int i = 0; i < accXData.size(); i++) {
            meanMaxValue = meanMaxValue + accXData.get(i);
            if (accXData.get(i) > maxValue) {
                maxValue = accXData.get(i);
            }
        }

        meanMaxValue = meanMaxValue / accXData.size();

        if (false) {
            meanMaxValue = maxValue - meanMaxValue;
        }
        Log.e(TAG, "MAX / MEAN OF ACC X is: " + meanMaxValue);
//        maxDev_accX_data.setText(Double.toString(meanMaxValue));
        mdAccX = meanMaxValue;
    }

    private void calculateMeanMaxAccY() {
        double meanMaxValue = 0;
        double maxValue = 0;

        for (int i = 0; i < accYData.size(); i++) {
            meanMaxValue = meanMaxValue + accYData.get(i);
            if (accYData.get(i) > maxValue) {
                maxValue = accYData.get(i);
            }
        }

        meanMaxValue = meanMaxValue / accYData.size();

        if (false) { //todo change this to a preference
            meanMaxValue = maxValue - meanMaxValue;
        }
        Log.e(TAG, "MAX / MEAN OF ACC Y is: " + meanMaxValue);
//        maxDev_accY_data.setText(Double.toString(meanMaxValue));
        mdAccY = meanMaxValue;
    }

    private void calculateMeanMaxZ() { //i LOVE inconsistent naming schemes!
        double meanMaxValue = 0;
        double maxValue = 0;

        for (int i = 0; i < accZData.size(); i++) {
            meanMaxValue = meanMaxValue + accZData.get(i);
            if (accZData.get(i) > maxValue) {
                maxValue = accZData.get(i);
            }
        }

        meanMaxValue = meanMaxValue / accZData.size();

        if (false) { //todo change this to a preference
            meanMaxValue = maxValue - meanMaxValue;
        }
        Log.e(TAG, "MAX / MEAN OF ACC Z is: " + meanMaxValue);
//        maxDev_accZ_data.setText(Double.toString(meanMaxValue));
        mdAccZ = meanMaxValue;
    }

    public void setUncalibratedMagX(String mag_x_value) {
        double uncalibratedMagX = 0;
        uncalibratedMagX = ((double)Integer.valueOf(mag_x_value) / (double)0x7FFF);
        DecimalFormat numberFormat = new DecimalFormat("0.00000");
        try {
//            magnetometer_x_data.setText(uncalibratedMagX + " µT");
            magX = Double.parseDouble(Double.toString(uncalibratedMagX));
        } catch (Exception e) {
            Log.e(TAG, "Error setting uncalibrated mag x: " + e);
        }
        mag_x = Double.toString(uncalibratedMagX);
        magXData.add(Double.parseDouble(mag_x));
    }

    public void calculateMeanMaxMagX() {
        double meanMaxValue = 0;
        double maxValue = 0;

        for (int i = 0; i < magXData.size(); i++ ) {
            meanMaxValue = meanMaxValue + magXData.get(i);
            if (magXData.get(i) > maxValue) {
                maxValue = magXData.get(i);
            }
        }
        meanMaxValue = meanMaxValue / magXData.size();

        if (false) {
            meanMaxValue = maxValue - meanMaxValue;
        }
//        maxDev_magX_data.setText(Double.toString(meanMaxValue));
    }

    public void setUncalibratedMagY(String mag_y_value) {
        double uncalibratedMagY = 0;
        uncalibratedMagY = ((double)Integer.valueOf(mag_y_value) / (double)0x7FFF);
        DecimalFormat numberFormat = new DecimalFormat("0.00000");
        try {
//            magnetometer_y_data.setText(uncalibratedMagY + " µT");
            magY = Double.parseDouble(Double.toString(uncalibratedMagY));
        } catch (Exception e) {
            Log.e(TAG, "Error setting uncalibrated mag y: " + e);
        }
        mag_y = Double.toString(uncalibratedMagY);
        magYData.add(Double.parseDouble(mag_y));
    }

    public void calculateMeanMaxMagY() {
        double meanMaxValue = 0;
        double maxValue = 0;

        for (int i = 0; i < magYData.size(); i++ ) {
            meanMaxValue = meanMaxValue + magYData.get(i);
            if (magYData.get(i) > maxValue) {
                maxValue = magYData.get(i);
            }
        }
        meanMaxValue = meanMaxValue / magYData.size();

        if (false) {
            meanMaxValue = maxValue - meanMaxValue;
        }
//        maxDev_magY_data.setText(Double.toString(meanMaxValue));
    }

    private void setUncalibratedMagZ(String mag_z_value) {
        Log.e(TAG, "YASSSS");
        double uncalibratedMagZ = 0;
        uncalibratedMagZ = ((double)Integer.valueOf(mag_z_value) / (double)0x7FFF);
        DecimalFormat numberFormat = new DecimalFormat("0.00000");
        Log.e(TAG, "YESSSS");

        try {
            Log.e(TAG, "SETTING Z VELU");
//            magnetometer_z_data.setText(uncalibratedMagZ + " µT");
            magZ = Double.parseDouble(Double.toString(uncalibratedMagZ));
        } catch (Exception e) {
            Log.e(TAG, "Error setting uncalibrated mag z: " + e);
        }
        mag_z = Double.toString(uncalibratedMagZ);
        magZData.add(Double.parseDouble(mag_z));
    }

    private void calculateMeanMaxMagZ() {
        double meanMaxValue = 0;
        double maxValue = 0;

        for (int i = 0; i < magZData.size(); i++ ) {
            meanMaxValue = meanMaxValue + magZData.get(i);
            if (magZData.get(i) > maxValue) {
                maxValue = magZData.get(i);
            }
        }
        meanMaxValue = meanMaxValue / magZData.size();

        if (false) {
            meanMaxValue = maxValue - meanMaxValue;
        }
//        maxDev_magZ_data.setText(Double.toString(meanMaxValue));

    }
}