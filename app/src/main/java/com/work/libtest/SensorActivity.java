package com.work.libtest;

import static com.work.libtest.Operation.OPERATION_NOTIFY;
import static com.work.libtest.Operation.OPERATION_READ;
import static com.work.libtest.Operation.OPERATION_WRITE;
import static com.work.libtest.TakeMeasurements.shotWriteType;

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
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.work.libtest.CalibrationHelper;

/*
NEXT TIME ON THE BACHELOR

Anna finally figures out how to read the calibration data after a successful fling with calibration index
But she will need to make it write to get ALL of calibration data.

She also needs to figure out how to record shots when the probe isnt connected to the phone. DIFFICULT!

THATS ALL FOR NOW!

THIS TIME ON THE BACHELOR
Anna struggles to read the calibration data for each changing calibration index, but at least we can read a few values!
Will she ever return any actual proper data>! Who knows???

For now all she can do is go figure out how to actually make the main function of the app work.
 */



public class SensorActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE_NAME = "Device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "Device_address";
    public static final String EXTRA_DEVICE_CONNECTION_STATUS = "Connection_status";

    public static final String EXTRA_DEVICE_SERIAL_NUMBER = "Serial_number";
    public static final String EXTRA_DEVICE_DEVICE_ADDRESS = "Device_gathered_addresses";
    public static final String EXTRA_DEVICE_VERSION = "Device_firmware_version";

    public static final String EXTRA_SAVED_NUM = "Number of data points saved";

    private String mDeviceName;
    private String mDeviceAddress;
    private String mConnectionStatus;

    String number;

    boolean writtenCalibrationIndex = false;

    private String TAG = "Sensor Activity";

    private int dataToBeRead = 0;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private Handler handler;

    private BluetoothLeService mBluetoothLeService;
    private BluetoothGatt mBluetoothGatt;

    //Probe status info
    private TextView probeNumber;
    private TextView connectionStatus;
    private ImageView connectionStatusImage;

    //dev tools, TODO delete before roll out / hide
    private TextView dev_shot_format;
    private TextView dev_record_number;

    private int dShotFormat = 1; //error number
    private int dRecordNumber = 0;
    private double dProbeTemp = 0;
    private double dProbeAccX = 0;
    private double dProbeAccY = 0;
    private double dProbeAccZ = 0;

    private double dProbeMagX = 0;
    private double dProbeMagY = 0;
    private double dProbeMagZ = 0;

    //Orientation
    private TextView orientation_roll_data;
    private TextView orientation_dip_data;
    private TextView orientation_azimuth_data;
    private TextView orientation_temperature_data;

    private double oRoll = 0;
    private double oDip = 0;
    private double oAzimuth = 0;
    private double oTemp = 0;

    //Accelerometer
    private TextView accelerometer_x_data;
    private TextView accelerometer_y_data;
    private TextView accelerometer_z_data;
    private TextView accelerometer_temp_data;
    private TextView accelerometer_magError_data;

    private double accX = 0;
    private double accY = 0;
    private double accZ = 0;
    private double accTemp = 0;
    private double accMagError = 0;

    //magnetometer
    private TextView magnetometer_x_data;
    private TextView magnetometer_y_data;
    private TextView magnetometer_z_data;
    private TextView magnetometer_temp_data;

    private double magX = 0;
    private double magY = 0;
    private double magZ = 0;
    private double magTemp = 0;

    //Maximum deviations
    private TextView maxDev_title;
    private CheckBox maxDev_mean_check;
//    private boolean maxDev_mean_check_data = false;
    private TextView maxDev_acc_data;
    private TextView maxDev_mag_data;
    private TextView maxDev_accX_data;
    private TextView maxDev_accY_data;
    private TextView maxDev_accZ_data;
    private TextView maxDev_magX_data;
    private TextView maxDev_magY_data;
    private TextView maxDev_magZ_data;

    private boolean mdMeanChecked = false;
    private double mdAcc = 0;
    private double mdMag = 0;
    private double mdAccX = 0;
    private double mdAccY = 0;
    private double mdAccZ = 0;
    private double mdMagX = 0;
    private double mdMagY = 0;
    private double mdMagZ = 0;

    private ArrayList<Byte> calibrationDataArray = new ArrayList<>();

    //options
    private CheckBox fastMag_data;
//    private boolean fastMag_data_var = false;
    private boolean fastMagChecked = false;

    //sensor relay service
    private TextView relayOptions_title;
    private TextView relayOptions_info;
    private CheckBox relayOptions_check;

    //holds last known info
    private String lSerialNumber; //the l stands for looser
    private String lDeviceAddress;
    private String lFirmwareVersion;

    private String lCalibrationDate;

    private Queue<Operation> operations = new LinkedList<>();
    private Operation currentOp;
    private static boolean operationOngoing = false;

    private Menu menu;
    int _probeMode = 0;
    public static ArrayList<Survey> surveys = new ArrayList<Survey>();
    public static ArrayList<ProbeData> probeData = new ArrayList<ProbeData>();

    private static ArrayList<Double> accXData = new ArrayList<>();
    private static ArrayList<Double> accYData = new ArrayList<>();
    private static ArrayList<Double> accZData = new ArrayList<>();

    private static ArrayList<Double> magXData = new ArrayList<>();
    private static ArrayList<Double> magYData = new ArrayList<>();
    private static ArrayList<Double> magZData = new ArrayList<>();

    private BluetoothGattCharacteristic CORE_SHOT_CHARACTERISTIC;
    private BluetoothGattCharacteristic BORE_SHOT_CHARACTERISTIC;

    private BluetoothGattCharacteristic CALIBRATION_INDEX_CHARACTERISTIC;
    private BluetoothGattCharacteristic CALIBRATION_DATA_CHARACTERISTIC;

    String shot_format = "", record_number = "", probe_temp = "", core_ax = "", core_ay = "", core_az = "", acc_temp = "";
    String record_number_binary = "", core_ax_binary = "", core_ay_binary, core_az_binary, mag_x_binary = "", mag_y_binary = "", mag_z_binary = "";
    String mag_x = "", mag_y = "", mag_z = "";

    String highByte, lowByte;

    boolean hold = false;
    boolean calibrationContinueCollection = true;
    public static int calibrationIndexValue = 00;
    boolean readyToContinue = true;

    int numSaved = 0;

    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;
    }

    //Queue managers
    public synchronized void request(Operation operation) {
        Log.d(TAG, "requesting operation: " + operation.toString());
        try {
            operations.add(operation);
            if (currentOp == null) {
                currentOp = operations.poll();
                performOperation();
            } else {
                Log.e(TAG, "current operation is null");
//                if (operations.peek() != null) {
//                    currentOp = operations.poll();
//                    performOperation();
//                }
            }
//            try {
//                currentOp = operations.poll();
//                performOperation();
//            } catch (Exception e) {
//                Log.e(TAG, "Exeption thrown attemping to perform operation: " + e);
//            }
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
            Log.d(TAG, "Queue empty");
        }
    }

    public void performOperation() {
        Log.d(TAG, "Performing operation");
        if (currentOp != null) {
            operationOngoing = true;
            Log.e(TAG, "Current performing option on service: " + currentOp.getService().getUuid().toString() + " with characteristic: " + currentOp.getCharacteristic().getUuid().toString());
            if (currentOp.getService() != null && currentOp.getCharacteristic() != null && currentOp.getAction() != 0) {
                switch (currentOp.getAction()) {
                    case OPERATION_WRITE:
                        Log.e(TAG, "WRITING?!");
                        //TODO write to the character
                        break;
                    case OPERATION_READ:
                        Log.e(TAG, "READING");
                        //read the characteristic
                        Log.d(TAG, "Reading characteristic with service: " + currentOp.getService().getUuid().toString() + " and characteristic: " + currentOp.getCharacteristic().getUuid().toString());
                        mBluetoothLeService.readCharacteristic(currentOp.getCharacteristic());
                        break;
                    case OPERATION_NOTIFY:
                        Log.e(TAG, "NOTIFY");
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
            } else {
                Log.e(TAG, "The service, characterisitic or action is null");
            }
        } else {
            Log.e(TAG, "CurrentOp is null");
        }
    }

    public void resetQueue() {
        operations.clear();
        currentOp = null;
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
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

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)){
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
                        Log.d(TAG, "OTHER DATA !?" + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                        //THIS is returning calibration data
                    } else if (intent.getStringExtra(BluetoothLeService.SERIAL_NUMBER) != null) {
                        Log.d(TAG, "Device Serial number is: " + intent.getStringExtra(BluetoothLeService.SERIAL_NUMBER).toString());
                    } else if (intent.getStringExtra(BluetoothLeService.DEVICE_ADDRESS) != null) {
                        Log.d(TAG, "Device Address is: " + intent.getStringExtra(BluetoothLeService.DEVICE_ADDRESS).toString());
                    } else if (intent.getStringExtra(BluetoothLeService.MAJOR_FIRMWARE_VERSION) != null) {
                        Log.d(TAG, "Major firmware version is: " + intent.getStringExtra(BluetoothLeService.MAJOR_FIRMWARE_VERSION));
                    } else if (intent.getStringExtra(BluetoothLeService.MINOR_FIRMWARE_VERSION) != null) {
                        Log.d(TAG, "Minor firmware version is: " + intent.getStringExtra(BluetoothLeService.MINOR_FIRMWARE_VERSION));
                    } else if (intent.getStringExtra(BluetoothLeService.CALIBRATION_INDEX) != null) {
                        Log.d(TAG, "Calibration index is: " + intent.getStringExtra(BluetoothLeService.CALIBRATION_INDEX));
                    } else if (intent.getStringExtra(BluetoothLeService.CALIBRATION_DATA) != null) {
                        Log.d(TAG, "Calibration data is: " + intent.getStringExtra(BluetoothLeService.CALIBRATION_DATA));
                        //Save calibration data
                        String calibrationData = intent.getStringExtra(BluetoothLeService.CALIBRATION_DATA);
                        boolean addData = false;
                        for (int i = 0; i < calibrationData.length(); i++) {
                            if (calibrationData.toString().charAt(i) != 'F') {
                                addData = true;
                            }
                        }
                        if (addData) {
                            for (int i = 0; i < calibrationData.length(); i++) {
                                calibrationDataArray.add((byte) calibrationData.toString().charAt(i));
                            }
                            calibrationContinueCollection = true;
                            calibrationIndexValue++;
                        } else {
                            calibrationContinueCollection = false;
                            Log.e(TAG, "FINISHED COLLECTING CALA data: ");
                            for (int i = 0; i < calibrationDataArray.size(); i++) {
                                Log.d(TAG, "Num: " + i + " is: " + calibrationDataArray.get(i));
                            }
                        }
                        readyToContinue = true;

                    } else if (intent.getStringExtra(BluetoothLeService.CORE_SHOT) != null) {
                        String coreShotData = intent.getStringExtra(BluetoothLeService.CORE_SHOT);
                        Log.e(TAG, "CORE DATA: " + coreShotData);
                        String[] core_shot_data = coreShotData.split(":", 20); //split the data up into bytes
                        shot_format = core_shot_data[0];
                        Log.e(TAG, "SHOT FORMAT: " + shot_format);

                        if (!hold) {
                            switch (shot_format) {
                                case "1":
                                    //first format(12): Format(1), Record number(2), Probe temperature(2), AX(2), AY(2), AZ(2), Accelerometer Temperature(1)
                                    String a = "", b = "";
                                    for (int i = 0; i < core_shot_data.length; i++) {
                                        if (i == 0) {
                                            dShotFormat = Integer.valueOf(shot_format);
                                            dev_shot_format.setText(shot_format);
                                        } else if (i == 1) {
                                            try {
                                                highByte = core_shot_data[i];
//                                                Log.e(TAG, "high byte: " + highByte);
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
//                                                Log.e(TAG, "low byte: " + lowByte);
                                                if (Integer.valueOf(lowByte) > 0 && Integer.valueOf(lowByte) < 128) {
                                                    value = value + Integer.valueOf(lowByte);
                                                } else if (Integer.valueOf(lowByte) < 1 && Integer.valueOf(lowByte) >= -128) {
                                                    value = 128 + (128 + Integer.valueOf(lowByte));
                                                }
//                                                Log.e(TAG, "final record number: " + value);
                                                dev_record_number.setText(Integer.toString(value));
                                                dRecordNumber = value;
                                            } catch (Exception e) {
                                                Log.e(TAG, "Exception thrown in low byte: " + e);
                                            }
                                        } else if (i == 3) {
                                            probe_temp = core_shot_data[i];
                                        } else if (i == 4) {
                                            //ISSUE the documentation says that temperature should be divided by 256 to be read correct, but this seems to work so idk
                                            probe_temp = probe_temp + "." + core_shot_data[i]; //as it is a fixed point value with 8 binary places
                                            orientation_temperature_data.setText(probe_temp);
                                            oTemp = Double.parseDouble(probe_temp);
                                        } else if (i == 5) { //AX 1
                                            core_ax_binary = "";
                                            core_ax = core_shot_data[i];
                                            int num = Integer.parseInt(core_shot_data[i]);
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
                                            int num = Integer.parseInt(core_shot_data[i]);
                                            if (num < 0) {
                                                num  = 128 + (128 + num);
                                            }
//                                            Log.e(TAG, "record number byte 1 in integer form is: " + num);
                                            String binaryOutput = Integer.toBinaryString(num);
                                            if (Integer.toBinaryString(num).length() < 8) {
                                                for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                                    binaryOutput = "0" + binaryOutput;
                                                }
                                            }
                                            core_ax_binary = core_ax_binary + binaryOutput;
//                                            Log.e(TAG, "Core AX Binary: " + core_ax_binary);

                                            if (core_ax_binary.length() > 16) {
//                                                Log.e(TAG, "Error, AX binary longer than 16 bits");
                                                accelerometer_x_data.setText("Error");
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
                                            int num = Integer.parseInt(core_shot_data[i]);
                                            if (num < 0) {
                                                num  = 128 + (128 + num);
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
                                                accelerometer_y_data.setText("Error");
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
                                            int num = Integer.parseInt(core_shot_data[i]);
                                            if (num < 0) {
                                                num  = 128 + (128 + num);
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
                                                accelerometer_z_data.setText("Error");
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
//                                            Log.d(TAG, "Accelerometer temperature: " + acc_temp);
                                            if (accelerometer_temp_data.equals("-128")) {
                                                accelerometer_temp_data.setText("Not Avaliable");
                                            } else {
                                                accelerometer_temp_data.setText(acc_temp);
                                                accTemp = Double.parseDouble(acc_temp);
                                            }
                                        }
                                    }
                                    break;
                                case "2":
                                    //second format(19) : Format(1), Record number(2), Probe temperature(2), AX(2), AY(2), AZ(2), Accelerometer Temperature(1), MX (2), MY (2), MZ (2), Magnetometer Temperature (1)
                                    Log.e(TAG, "SHOT FORMAT 2");
                                    break;
                                case "3":
                                    Log.e(TAG, "SHOT FORMAT 3");
                                    //third format(20) : Format(1), Record number(2), Probe temperature(2), AX(2), AY(2), AZ(2), MX(3), MY(3), MZ(3)

                                    break;
                                case "4":
                                    break;
                                case "5":
                                    break;
                                case "6":
                                    break;
                                default:
                                    Log.e(TAG, "ERROR: Shot format not valid: " + shot_format);
                                    break;
                            }
                        }
                    } else if (intent.getStringExtra(BluetoothLeService.BORE_SHOT) != null) {
                        Log.d(TAG, "Bore shot raw data is: " + intent.getStringExtra(BluetoothLeService.BORE_SHOT));
                        String boreShotData = intent.getStringExtra(BluetoothLeService.BORE_SHOT);
                        String[] bore_shot_data = boreShotData.split(":", 20); //split the data up into bytes
//                        Log.e(TAG, "DATA: " + bore_shot_data);
                        shot_format = bore_shot_data[0];
                        Log.e(TAG, "SHOT FORMAT: " + shot_format);

//                        if (!hold) {
                            switch (shot_format) {
//                                case "1":
////                                {
////                                    //first format(12): Format(1), Record number(2), Probe temperature(2), AX(2), AY(2), AZ(2), Accelerometer Temperature(1)
////                                    String a = "", b = "";
////                                    for (int i = 0; i < core_shot_data.length; i++) {
////                                        if (i == 0) {
////                                            dShotFormat = Integer.valueOf(shot_format);
////                                            dev_shot_format.setText(shot_format);
////                                        } else if (i == 1) {
////                                            try {
////                                                highByte = core_shot_data[i];
//////                                                Log.e(TAG, "high byte: " + highByte);
////                                            } catch (Exception e) {
////                                                Log.e(TAG, "Exception thrown in high byte: " + e);
////                                            }
////                                        } else if (i == 2) {
////                                            try {
////                                                int value = 0;
////                                                if (Integer.valueOf(highByte) > 0 && Integer.valueOf(highByte) < 128) {
////                                                    value = (128 * Integer.valueOf(highByte));
////                                                } else if (Integer.valueOf(highByte) < 0 && Integer.valueOf(highByte) >= -128) {
////                                                    value = (128 * (128 + Integer.valueOf(highByte))) + (128 * 127);
////                                                }
////                                                lowByte = core_shot_data[i];
//////                                                Log.e(TAG, "low byte: " + lowByte);
////                                                if (Integer.valueOf(lowByte) > 0 && Integer.valueOf(lowByte) < 128) {
////                                                    value = value + Integer.valueOf(lowByte);
////                                                } else if (Integer.valueOf(lowByte) < 1 && Integer.valueOf(lowByte) >= -128) {
////                                                    value = 128 + (128 + Integer.valueOf(lowByte));
////                                                }
//////                                                Log.e(TAG, "final record number: " + value);
////                                                dev_record_number.setText(Integer.toString(value));
////                                                dRecordNumber = value;
////                                            } catch (Exception e) {
////                                                Log.e(TAG, "Exception thrown in low byte: " + e);
////                                            }
////                                        } else if (i == 3) {
////                                            probe_temp = core_shot_data[i];
////                                        } else if (i == 4) {
////                                            //ISSUE the documentation says that temperature should be divided by 256 to be read correct, but this seems to work so idk
////                                            probe_temp = probe_temp + "." + core_shot_data[i]; //as it is a fixed point value with 8 binary places
////                                            orientation_temperature_data.setText(probe_temp);
////                                            oTemp = Double.parseDouble(probe_temp);
////                                        } else if (i == 5) { //AX 1
////                                            core_ax_binary = "";
////                                            core_ax = core_shot_data[i];
////                                            int num = Integer.parseInt(core_shot_data[i]);
////                                            if (num < 0) {
////                                                //need to convert from -128 - 0 to a positive number
////                                                num = 128 + (128 + num);
////                                            }
//////                                            Log.e(TAG, "record number byte 1 in integer form is: " + num); //Seems not helpful
////                                            String binaryOutput = Integer.toBinaryString(num);
////                                            if (Integer.toBinaryString(num).length() < 8) {
////                                                for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
////                                                    binaryOutput = "0" + binaryOutput;
////                                                }
////                                            }
////                                            core_ax_binary = binaryOutput;
//////                                            Log.e(TAG, "binary core_ax: " + binaryOutput);
////                                        } else if (i == 6) { //AX 2
////                                            int num = Integer.parseInt(core_shot_data[i]);
////                                            if (num < 0) {
////                                                num  = 128 + (128 + num);
////                                            }
//////                                            Log.e(TAG, "record number byte 1 in integer form is: " + num);
////                                            String binaryOutput = Integer.toBinaryString(num);
////                                            if (Integer.toBinaryString(num).length() < 8) {
////                                                for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
////                                                    binaryOutput = "0" + binaryOutput;
////                                                }
////                                            }
////                                            core_ax_binary = core_ax_binary + binaryOutput;
//////                                            Log.e(TAG, "Core AX Binary: " + core_ax_binary);
////
////                                            if (core_ax_binary.length() > 16) {
//////                                                Log.e(TAG, "Error, AX binary longer than 16 bits");
////                                                accelerometer_x_data.setText("Error");
////                                            } else {
////                                                if (core_ax_binary.charAt(0) == '1') {
////                                                    core_ax_binary = core_ax_binary.substring(1);
////                                                    core_ax = Integer.toString(Integer.parseInt(core_ax_binary, 2));
////                                                    core_ax = Integer.toString(Integer.valueOf(core_ax) * -1);
////                                                    setUncalibratedX(core_ax);
////                                                    calculateMeanMaxAccX();
////
////                                                } else {
////                                                    core_ax = Integer.toString(Integer.parseInt(core_ax_binary, 2));
////                                                    setUncalibratedX(core_ax);
////                                                    calculateMeanMaxAccX();
////                                                }
////                                            }
////                                        } else if (i == 7) { //AY 1
////                                            core_ay_binary = "";
////                                            core_ay = core_shot_data[i];
////                                            int num = Integer.parseInt(core_shot_data[i]);
////                                            if (num < 0) {
////                                                //need to convert from -128 - 0 to a positive number
////                                                num = 128 + (128 + num);
////                                            }
//////                                            Log.e(TAG, "record number byte 1 in integer form is: " + num); //Seems not helpful
////                                            String binaryOutput = Integer.toBinaryString(num);
////                                            if (Integer.toBinaryString(num).length() < 8) {
////                                                for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
////                                                    binaryOutput = "0" + binaryOutput;
////                                                }
////                                            }
////                                            core_ay_binary = binaryOutput;
//////                                            Log.e(TAG, "binary core_ay: " + binaryOutput);
////                                        } else if (i == 8) { //AY 2
////                                            int num = Integer.parseInt(core_shot_data[i]);
////                                            if (num < 0) {
////                                                num  = 128 + (128 + num);
////                                            }
//////                                            Log.e(TAG, "record number byte 1 in integer form is: " + num);
////                                            String binaryOutput = Integer.toBinaryString(num);
////                                            if (Integer.toBinaryString(num).length() < 8) {
////                                                for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
////                                                    binaryOutput = "0" + binaryOutput;
////                                                }
////                                            }
////                                            core_ay_binary = core_ay_binary + binaryOutput;
//////                                            Log.e(TAG, "Core AY Binary: " + core_ay_binary);
////
////                                            if (core_ay_binary.length() > 16) {
//////                                                Log.e(TAG, "Error, AY binary longer than 16 bits");
////                                                accelerometer_y_data.setText("Error");
////                                            } else {
////                                                if (core_ay_binary.charAt(0) == '1') {
////                                                    core_ay_binary = core_ay_binary.substring(1);
////                                                    core_ay = Integer.toString(Integer.parseInt(core_ay_binary, 2));
////                                                    core_ay = Integer.toString(Integer.valueOf(core_ay) * -1);
////                                                    setUncalibratedY(core_ay);
////                                                    calculateMeanMaxAccY();
////                                                } else {
////                                                    core_ay = Integer.toString(Integer.parseInt(core_ay_binary, 2));
////                                                    setUncalibratedY(core_ay);
////                                                    calculateMeanMaxAccY();
////
////                                                }
////                                            }
////                                        } else if (i == 9) { //AZ 1
////                                            core_az_binary = "";
////                                            core_az = core_shot_data[i];
////                                            int num = Integer.parseInt(core_shot_data[i]);
////                                            if (num < 0) {
////                                                //need to convert from -128 - 0 to a positive number
////                                                num = 128 + (128 + num);
////                                            }
//////                                            Log.e(TAG, "record number byte 1 in integer form is: " + num); //Seems not helpful
////                                            String binaryOutput = Integer.toBinaryString(num);
////                                            if (Integer.toBinaryString(num).length() < 8) {
////                                                for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
////                                                    binaryOutput = "0" + binaryOutput;
////                                                }
////                                            }
////                                            core_az_binary = binaryOutput;
//////                                            Log.e(TAG, "binary core_az: " + binaryOutput);
////                                        } else if (i == 10) { //AZ 2
////                                            int num = Integer.parseInt(core_shot_data[i]);
////                                            if (num < 0) {
////                                                num  = 128 + (128 + num);
////                                            }
////
//////                                            Log.e(TAG, "record number byte 1 in integer form is: " + num);
////                                            String binaryOutput = Integer.toBinaryString(num);
////                                            if (Integer.toBinaryString(num).length() < 8) {
////                                                for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
////                                                    binaryOutput = "0" + binaryOutput;
////                                                }
////                                            }
////                                            core_az_binary = core_az_binary + binaryOutput;
//////                                            Log.e(TAG, "Core AY Binary: " + core_az_binary);
////
////                                            if (core_az_binary.length() > 16) {
//////                                                Log.e(TAG, "Error, AY binary longer than 16 bits");
////                                                accelerometer_z_data.setText("Error");
////                                            } else {
////                                                if (core_az_binary.charAt(0) == '1') {
////                                                    core_az_binary = core_az_binary.substring(1);
////                                                    core_az = Integer.toString(Integer.parseInt(core_az_binary, 2));
////                                                    core_az = Integer.toString(Integer.valueOf(core_az) * -1);
////                                                    setUncalibratedZ(core_az);
////                                                    calculateMeanMaxZ();
////
////                                                } else {
////                                                    core_az = Integer.toString(Integer.parseInt(core_az_binary, 2));
////                                                    setUncalibratedZ(core_az);
////                                                    calculateMeanMaxZ();
////                                                }
////                                            }
////                                            try {
////                                                calculateRoll(Double.valueOf(core_ay), Double.valueOf(core_az));
////                                                calculateDip(Double.valueOf(core_ax), Double.valueOf(core_ay), Double.valueOf(core_az));
////                                            } catch (Exception e) {
////                                                Log.e(TAG, "Error calling calculate roll and dip functions: " + e);
////                                            }
////
////                                        } else if (i == 11) { // Temperature
////                                            acc_temp = core_shot_data[i];
//////                                            Log.d(TAG, "Accelerometer temperature: " + acc_temp);
////                                            if (accelerometer_temp_data.equals("-128")) {
////                                                accelerometer_temp_data.setText("Not Avaliable");
////                                            } else {
////                                                accelerometer_temp_data.setText(acc_temp);
////                                                accTemp = Double.parseDouble(acc_temp);
////                                            }
////                                        }
////                                    }
////                                }
//                                    break;
//                                case "2":
//                                    //second format(19) : Format(1), Record number(2), Probe temperature(2), AX(2), AY(2), AZ(2), Accelerometer Temperature(1), MX (2), MY (2), MZ (2), Magnetometer Temperature (1)
//                                    Log.e(TAG, "SHOT FORMAT 2");
//                                    break;
                                case "3":
                                    Log.e(TAG, "SHOT FORMAT 3");
//                                    //third format(20) : Format(1), Record number(2), Probe temperature(2), AX(2), AY(2), AZ(2), MX(3), MY(3), MZ(3)
//
//                                    dShotFormat = Integer.valueOf(shot_format);
                                    dev_shot_format.setText(shot_format);
//
                                    Log.e(TAG, "ANNA ----------------------------------");
                                    byte[] char_all_bore_shot = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                                    String[] string_all_bore_shot = {"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0"};
                                    for (int i = 0; i < bore_shot_data.length; i++) {
////                                        Log.e(TAG, "Shot " + i + ": " + bore_shot_data[i]);
                                        int bore_value;
                                        try {
                                            bore_value = Integer.valueOf(bore_shot_data[i]);
                                        } catch (Exception e) {
                                            bore_value = -2;
                                        }
                                        try {
//                                            char_all_bore_shot[i] = Byte.parseByte(Integer.toHexString(bore_value));
                                            if (bore_value < 0) {
                                                String unsignedHex = String.format("%02X", bore_value & 0xff);
                                                string_all_bore_shot[i] = unsignedHex;
//                                                char_all_bore_shot[i] = (byte) Integer.parseInt(string_all_bore_shot[i], 16);//Byte.parseByte();
//                                                char_all_bore_shot[i] = (byte) (Integer.parseInt(string_all_bore_shot[i], 16) & 0xFF);
                                            } else {
                                                string_all_bore_shot[i] = Integer.toHexString(bore_value);
                                            }
                                        } catch (Exception e) {
                                            string_all_bore_shot[i] = "-1";
                                        }
                                    }

                                    //Format all the displayed numbers so they arent incredibly long
                                    DecimalFormat numberFormat = new DecimalFormat("0.0000000");

//                                  //first 2 bytes after shot type is the record number
                                    try {
                                        byte value1 = (byte) Integer.parseInt(string_all_bore_shot[1], 16);
                                        byte value2 = (byte) Integer.parseInt(string_all_bore_shot[2], 16);
                                        dRecordNumber = (int) twoBytesToValue(value2, value1);
                                        numberFormat.format(dRecordNumber);
                                        dev_record_number.setText(String.valueOf(dRecordNumber));
                                    } catch (Exception e) {
                                        Log.e(TAG, "EXCEPTION thrown in record number: " + e);
                                    }

//                                  next 2 bytes after probe temperature
                                    try {
                                        byte value1 = (byte) Integer.parseInt(string_all_bore_shot[3], 16);
                                        byte value2 = (byte) Integer.parseInt(string_all_bore_shot[4], 16);
                                        int shotProbeTempRaw = ((int)value1 << 8) + (((int)value2) & 0x00FF);
                                        double probeTempU = (double)shotProbeTempRaw/256.0;
                                        double probeTemp = CalibrationHelper.temp_param[0] + (CalibrationHelper.temp_param[1] * probeTempU);

                                        orientation_temperature_data.setText(numberFormat.format(probeTemp));
                                        //TODO make so if the accelerometer or magnetometer are actually passing temp data it displays the corresponding value
                                        accelerometer_temp_data.setText(numberFormat.format(probeTemp));
                                        magnetometer_temp_data.setText(numberFormat.format(probeTemp));
                                    } catch (Exception e) {
                                        Log.e(TAG, "Exception thrown in probe temperature: " + e);
                                    }

                                    double ux = 0;
                                    double uy = 0;
                                    double uz = 0;
                                    double[] calAcc;

                                    //TODO could probably turn this into a function for better code functionality
                                    try {
                                        byte value1 = (byte) Integer.parseInt(string_all_bore_shot[5], 16);
                                        byte value2 = (byte) Integer.parseInt(string_all_bore_shot[6], 16);
                                        int shotAccX = ((int)value1 << 8) + (((int)value2) & 0x00FF);
                                        ux = ((double)shotAccX)/32.0/512.0;
                                    } catch (Exception e) {
                                        Log.e(TAG, "Exception thrown setting ux: " + e);
                                    }

                                    try {
                                        byte value1 = (byte) Integer.parseInt(string_all_bore_shot[7], 16);
                                        byte value2 = (byte) Integer.parseInt(string_all_bore_shot[8], 16);
                                        int shotAccY = ((int)value1 << 8) + (((int)value2) & 0x00FF);
                                        uy = ((double)shotAccY)/32.0/512.0;
                                    } catch (Exception e) {
                                        Log.e(TAG, "Exception thrown setting uy: " + e);
                                    }

                                    try {
                                        byte value1 = (byte) Integer.parseInt(string_all_bore_shot[9], 16);
                                        byte value2 = (byte) Integer.parseInt(string_all_bore_shot[10], 16);
                                        int shotAccZ = ((int)value1 << 8) + (((int)value2) & 0x00FF);
                                        uz = ((double)shotAccZ)/32.0/512.0;

                                    } catch (Exception e) {
                                        Log.e(TAG, "Exception thrown setting uz: " + e);
                                    }

                                    double umx = 0;
                                    double umy = 0;
                                    double umz = 0;
                                    double[] calMag;

                                    try {
                                        byte value1 = (byte) Integer.parseInt(string_all_bore_shot[11], 16);
                                        byte value2 = (byte) Integer.parseInt(string_all_bore_shot[12], 16);
                                        byte value3 = (byte) Integer.parseInt(string_all_bore_shot[13], 16);
                                        int shotMagX = ((((int)value1 << 8) + (((int)value2) & 0x00FF) ) << 8) + (((int)value3) & 0x00FF);
                                        umx = ((double)shotMagX *0.001);
                                    } catch (Exception e) {
                                        Log.e(TAG, "Exception thrown setting umx");
                                    }

                                    try {
                                        byte value1 = (byte) Integer.parseInt(string_all_bore_shot[14], 16);
                                        byte value2 = (byte) Integer.parseInt(string_all_bore_shot[15], 16);
                                        byte value3 = (byte) Integer.parseInt(string_all_bore_shot[16], 16);
                                        int shotMagY = ((((int)value1 << 8) + (((int)value2) & 0x00FF) ) << 8) + (((int)value3) & 0x00FF);
                                        umy = ((double)shotMagY *0.001);
                                    } catch (Exception e) {
                                        Log.e(TAG, "Exception thrown setting umx");
                                    }

                                    try {
                                        byte value1 = (byte) Integer.parseInt(string_all_bore_shot[17], 16);
                                        byte value2 = (byte) Integer.parseInt(string_all_bore_shot[18], 16);
                                        byte value3 = (byte) Integer.parseInt(string_all_bore_shot[19], 16);
                                        int shotMagZ = ((((int)value1 << 8) + (((int)value2) & 0x00FF) ) << 8) + (((int)value3) & 0x00FF);
                                        umz = ((double)shotMagZ *0.001);
                                    } catch (Exception e) {
                                        Log.e(TAG, "Exception thrown setting umx");
                                    }

                                    calAcc = CalibrationHelper.CalibrationHelp(CalibrationHelper.accelerationCalibration, ux, uy, uz);
                                    calMag = CalibrationHelper.CalibrationHelp(CalibrationHelper.magnetometerCalibration, umx, umy, umz);

                                    //Set the display values for x, y and z calibrated values
                                    double cx = calAcc[0];
                                    double cy = calAcc[1];
                                    double cz = calAcc[2];

                                    double cmx = calMag[0];
                                    double cmy = calMag[1];
                                    double cmz = calMag[2];

                                    //Set roll and dip values
                                    double cal_roll_radian = Math.atan2(cy, cz);
                                    if (cal_roll_radian > Math.PI) { cal_roll_radian -= (2 * Math.PI); }
                                    if (cal_roll_radian < -Math.PI) { cal_roll_radian += (2*Math.PI); }
                                    double cal_dip_radian = Math.atan2(-cx, Math.sqrt((cy*cy)+(cz*cz)));

                                    double den = (cmx * Math.cos(cal_dip_radian)) + (cmy * Math.sin(cal_dip_radian) * Math.sin(cal_roll_radian)) + (cmz * Math.sin(cal_dip_radian) * Math.cos(cal_roll_radian));
                                    double num = (cmy * Math.cos(cal_roll_radian)) - (cmz * Math.sin(cal_roll_radian));
                                    double cal_az_radian = Math.atan2(-num, den);
                                    if (cal_az_radian > Math.PI) { cal_az_radian -= (2*Math.PI); }
                                    if (cal_az_radian < -Math.PI) { cal_az_radian += (2*Math.PI); }
                                    if (cal_az_radian < 0) { cal_az_radian += (2*Math.PI); }

                                    //convert to degrees :(
                                    double cal_roll_degree = cal_roll_radian*180/Math.PI;
                                    double cal_dip_degree = cal_dip_radian*180/Math.PI;
                                    double cal_az_degree = cal_az_radian*180/Math.PI;

                                    //display orientation data
                                    orientation_roll_data.setText(numberFormat.format(cal_roll_degree));
                                    orientation_dip_data.setText(numberFormat.format(cal_dip_degree));
                                    orientation_azimuth_data.setText(numberFormat.format(cal_az_degree));

                                    accelerometer_x_data.setText(numberFormat.format(cx));
                                    accelerometer_y_data.setText(numberFormat.format(cy));
                                    accelerometer_z_data.setText(numberFormat.format(cz));

                                    magnetometer_x_data.setText(numberFormat.format(cmx));
                                    magnetometer_y_data.setText(numberFormat.format(cmy));
                                    magnetometer_z_data.setText(numberFormat.format(cmz));

                                    break;
                                default:
                                    Log.e(TAG, "ERROR: Shot format not valid: " + shot_format);
                                    break;
                            }
//                        }
                    } else if (intent.getStringExtra(BluetoothLeService.PROBE_MODE) != null) {
                        Log.d(TAG, "Probe Mode is: " + intent.getStringExtra(BluetoothLeService.PROBE_MODE));
                        if (intent.getStringExtra(BluetoothLeService.PROBE_MODE).equals("2")) {
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

    public static short toInt16(byte[] bytes, int index) {
        return (short) ((bytes[index + 1] << 8) | (bytes[index] & 0xFF));
    }

    private double twoBytesToValue(byte value1, byte value2) {
        byte[] dataArray = new byte[] {value1, value2};
        int i1 = toInt16(dataArray, 0);
        return i1;
    }

    private void clearUI() { }

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
                Log.e(TAG, "Error occurred in sending data", e);
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
        setContentView(R.layout.activity_sensor);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        shotWriteType = 2;

        dataToBeRead = 0;

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        mConnectionStatus = intent.getStringExtra(EXTRA_DEVICE_CONNECTION_STATUS);
        Log.d(TAG, "Device Name: " + mDeviceName + ", Device Address: " + mDeviceAddress);

        try {
            numSaved = Integer.valueOf(intent.getStringExtra(EXTRA_SAVED_NUM));
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown in writing to save button title: " + e);
        }

        try {
            MenuItem saveMenuItem = menu.findItem(R.id.sensor_save_button);
            saveMenuItem.setTitle("Save! ");
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown in writing menu data: " + e);
        }

        connectionStatus = (TextView) findViewById(R.id.orientation_connection_Txt);
        connectionStatusImage = (ImageView) findViewById(R.id.orientation_connection_img);

        dev_shot_format = (TextView) findViewById(R.id.dev_format_info);
        dev_record_number = (TextView) findViewById(R.id.dev_record_number_info);

        orientation_roll_data = (TextView) findViewById(R.id.orientation_roll_data);
        orientation_dip_data = (TextView) findViewById(R.id.orientation_dip_data);
        orientation_azimuth_data = (TextView) findViewById(R.id.orientation_azimuth_data);
        orientation_temperature_data = (TextView) findViewById(R.id.orientation_temperature_data);

        accelerometer_x_data = (TextView) findViewById(R.id.accelerometer_x_data);
        accelerometer_y_data = (TextView) findViewById(R.id.accelerometer_y_data);
        accelerometer_z_data = (TextView) findViewById(R.id.accelerometer_z_data);
        accelerometer_temp_data = (TextView) findViewById(R.id.accelerometer_temp_data);
        accelerometer_magError_data = (TextView) findViewById(R.id.accelerometer_magError_data);

        magnetometer_x_data = (TextView) findViewById(R.id.magnetometer_x_data);
        magnetometer_y_data = (TextView) findViewById(R.id.magnetometer_y_data);
        magnetometer_z_data = (TextView) findViewById(R.id.magnetometer_z_data);
        magnetometer_temp_data = (TextView) findViewById(R.id.magnetometer_temp_data);

        maxDev_title = (TextView) findViewById(R.id.maxDev_title);
        maxDev_mean_check = (CheckBox) findViewById(R.id.maxDev_mean_check);
        maxDev_acc_data = (TextView) findViewById(R.id.maxDev_acc_data);
        maxDev_mag_data = (TextView) findViewById(R.id.maxDev_mag_data);
        maxDev_accX_data = (TextView) findViewById(R.id.maxDev_accX_data);
        maxDev_accY_data = (TextView) findViewById(R.id.maxDev_accY_data);
        maxDev_accZ_data = (TextView) findViewById(R.id.maxDev_accZ_data);
        maxDev_magX_data = (TextView) findViewById(R.id.maxDev_magX_data);
        maxDev_magY_data = (TextView) findViewById(R.id.maxDev_magY_data);
        maxDev_magZ_data = (TextView) findViewById(R.id.maxDev_magZ_data);

        fastMag_data = (CheckBox) findViewById(R.id.fastMag_data);

        relayOptions_title = (TextView) findViewById(R.id.relayOptions_title);
        relayOptions_info = (TextView) findViewById(R.id.relayOptions_info);
        relayOptions_check = (CheckBox) findViewById(R.id.relayOptions_check);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        if (mConnectionStatus.equals("Connected")) {
            updateConnectionState("Connected");
        } else {
            Log.e(TAG, "Device disconnected");
            updateConnectionState("Disconnected");
        }

        if (maxDev_mean_check.isChecked()) {
            maxDev_title.setText("MEAN VALUES");
        } else {
            maxDev_title.setText("MAXIMUM DEVIATIONS");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        dataToBeRead = 0;

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        mConnectionStatus = intent.getStringExtra(EXTRA_DEVICE_CONNECTION_STATUS);

        numSaved = Integer.valueOf(intent.getStringExtra(EXTRA_SAVED_NUM));
//        MenuItem saveMenuItem = menu.findItem(R.id.sensor_save_button);
//        saveMenuItem.setTitle("Save " + numSaved);

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
    }

    private boolean displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return false;

        String uuid = null;
        String unknownServiceString = "Unknown service";
        String unknownCharaString = "Unknown characteristics";
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicsData = new ArrayList<>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

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
                    if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.DEVICE_ID)) {
                        Log.e(TAG, "DEVICE ID");
                        Operation getDeviceIDOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);
                        request(getDeviceIDOperation);
                    } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.DEVICE_ADDRESS)) {
                        Log.e(TAG, "DEVICE ADDRESS");
                        Operation getDeviceAddressOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);
                        request(getDeviceAddressOperation);
                    } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.VERSION_MAJOR)) {
                        Log.e(TAG, "VERSION MAJOR");
                        Operation getVersionMajorOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);
                        request(getVersionMajorOperation);
                    } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.VERSION_MINOR)) {
                        Log.e(TAG, "VERSION MINOR");
                        Operation getVersionMinorOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);
                        request(getVersionMinorOperation);
                    } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.PROBE_MODE)) {
                        Log.e(TAG, "PROBE MODE");
                        Operation getProbeModeOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);
                        request(getProbeModeOperation);
                    } else if (_probeMode == 2 && gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.CORE_SHOT)) {
                        Log.e(TAG, "Reading core shot");
                        CORE_SHOT_CHARACTERISTIC = gattCharacteristic;
                        Operation getCoreShotOperation = new Operation(gattService, gattCharacteristic, OPERATION_NOTIFY);
                        request(getCoreShotOperation);
                    } else if (_probeMode == 2 && gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.BORE_SHOT)) {
                        Log.e(TAG, "Reading bore shot");
                        BORE_SHOT_CHARACTERISTIC = gattCharacteristic;
                        Operation getCoreShotOperation = new Operation(gattService, gattCharacteristic, OPERATION_NOTIFY);
                        try {
                            request(getCoreShotOperation);
                        } catch (Exception e) {
                            Log.e(TAG, "Request for BORE Shot failed!");
                        }
                    } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.CALIBRATION_INDEX)) {
                        Log.d(TAG, "Reading calibration index");
                        CALIBRATION_INDEX_CHARACTERISTIC = gattCharacteristic;
                        Operation getCalibrationIndex = new Operation(gattService, gattCharacteristic, OPERATION_READ);
                        try {
                            request(getCalibrationIndex);
                        } catch (Exception e) {
                            Log.e(TAG, "Request for calibration index failed" + e);
                        }
                    } else if (writtenCalibrationIndex && gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.CALIBRATION_DATA)) {
                        Log.d(TAG, "Reading Calibration Data");
                        CALIBRATION_DATA_CHARACTERISTIC = gattCharacteristic;
                        Operation getCalibrationData = new Operation(gattService, gattCharacteristic, OPERATION_READ);
                        try {
                            request(getCalibrationData);
                        } catch (Exception e) {
                            Log.e(TAG, "Request for calibration data failed" + e);
                        }
                    }
//                    else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.CALIBRATION_INDEX)) {
//                        Log.d(TAG, "READING CALIBRATION INDEX NEW");
//                        CALIBRATION_INDEX_CHARACTERISTIC = gattCharacteristic;
//                        Operation getCalibrationIndex = new Operation(gattService, gattCharacteristic, OPERATION_READ);
//                        try {
//                            request(getCalibrationIndex); //ISSUE
//                        } catch (Exception e) {
//                            Log.e(TAG, "Request for calibration index failed" + e);
//                        }
//                    } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.CALIBRATION_DATA)) {
//                        Log.d(TAG, "READING CALIBRATION DATA NEW");
//                        CALIBRATION_DATA_CHARACTERISTIC = gattCharacteristic;
//                        Operation getCalibrationData = new Operation(gattService, gattCharacteristic, OPERATION_READ);
//                        try {
////                            request(getCalibrationData); //ISSUE
//                        } catch (Exception e) {
//                            Log.e(TAG, "Request for calibration data failed");
//                        }
//                    }
                } else {
                    Log.e(TAG, "gatt characteristic uuid is null");
                }

            }
        }
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    private void updateConnectionState(final String resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //setText to resource ID to display connection status
                if (resourceId.equals("Connected")) {
                    Log.d(TAG, "DEVICE CONNECTED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    connectionStatus.setText("Connected");
                    connectionStatusImage.setImageResource(R.drawable.ready);
                    mConnectionStatus = "Connected";
                } else if (resourceId.equals("Disconnected")) {
                    Log.d(TAG, "DEVICE DISCONNECTED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    connectionStatus.setText("Disconnected");
                    connectionStatusImage.setImageResource(R.drawable.unconnected);

                    mConnectionStatus = "Disconnected";
                    registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
                    if (mBluetoothLeService != null) {
                        final boolean result = mBluetoothLeService.connect(mDeviceAddress);
                        Log.d(TAG, "Connection request result=" + result);
                        if (result) {
                            updateConnectionState("Connected");
                        }
                    }
                } else {
//                    connectionStatus.setText("No probe selected");
                    Log.e(TAG, "Error no probe selected");
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_sensor, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /**
         * Fun fact, you actually have to do it like this you cant use a switch case
         * hence this is not bad programming practice
         */
        if (item.getItemId() == R.id.sensor_back_button) {
            Log.d(TAG, "exit sensor activity to probe details");
            Intent intent = new Intent(this, ProbeDetails.class);
            intent.putExtra(ProbeDetails.EXTRA_DEVICE_NAME, mDeviceName);
            intent.putExtra(ProbeDetails.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
            intent.putExtra(ProbeDetails.EXTRA_DEVICE_CONNECTION_STATUS, mConnectionStatus);
            startActivity(intent);
        } else if (item.getItemId() == R.id.sensor_save_button) {
            saveData();
        } else if (item.getItemId() == R.id.sensor_hold_button) {
            holdButton();
        } else if (item.getItemId() == R.id.sensor_options_button) {

        } else if (item.getItemId() == R.id.sensor_tools_autoSaveOn) {

        } else if (item.getItemId() == R.id.sensor_tools_deleteDataFile) {

        } else if (item.getItemId() == R.id.sensor_tools_newAutoFile) {

        } else if (item.getItemId() == R.id.sensor_tools_saveData) {
            if (Integer.valueOf(number) != 0) {
                Intent intentSaveData = new Intent(this, SaveData.class);
//                intentSaveData.putExtra("PROBE DATA", probeData);

                intentSaveData.putExtra(SaveData.EXTRA_DEVICE_NAME, mDeviceName);
                intentSaveData.putExtra(SaveData.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
                intentSaveData.putExtra(SaveData.EXTRA_DEVICE_SERIAL_NUMBER, lSerialNumber);
                intentSaveData.putExtra(SaveData.EXTRA_DEVICE_DEVICE_ADDRESS, lDeviceAddress);
                intentSaveData.putExtra(SaveData.EXTRA_DEVICE_VERSION, lFirmwareVersion);
                intentSaveData.putExtra(SaveData.EXTRA_DEVICE_CONNECTION_STATUS, mConnectionStatus);

                try {
                    if (probeData != null) {
                        intentSaveData.putParcelableArrayListExtra(SaveData.EXTRA_SAVED_DATA, (ArrayList<? extends Parcelable>) probeData);
                        Log.d(TAG, "PRINTING PROBE DATA");
                        Log.d(TAG, probeData.get(0).returnData());
                    } else {
                        Log.e(TAG, "Probe Data is null!");
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Could not save correct data: " + e);
                }
                startActivity(intentSaveData);
            }
        }
//        switch (item.getItemId()) {
//            case R.id.sensor_back_button:
//                Log.d(TAG, "exit sensor activity to probe details");
//                Intent intent = new Intent(this, ProbeDetails.class);
//                intent.putExtra(ProbeDetails.EXTRA_DEVICE_NAME, mDeviceName);
//                intent.putExtra(ProbeDetails.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
//                intent.putExtra(ProbeDetails.EXTRA_DEVICE_CONNECTION_STATUS, mConnectionStatus);
//                startActivity(intent);
//                return true;
//            case R.id.sensor_save_button:
//                saveData();
//                return true;
//            case R.id.sensor_hold_button:
//                holdButton();
//                return true;
//            case R.id.sensor_options_button:
//                return true;
//            case R.id.sensor_tools_autoSaveOn:
//                return true;
//            case R.id.sensor_tools_deleteDataFile:
//                return true;
//            case R.id.sensor_tools_newAutoFile:
//                return true;
//            case R.id.sensor_tools_saveData:
//                if (Integer.valueOf(number) != 0) {
//                    Intent intentSaveData = new Intent(this, SaveData.class);
////                intentSaveData.putExtra("PROBE DATA", probeData);
//
//                    intentSaveData.putExtra(SaveData.EXTRA_DEVICE_NAME, mDeviceName);
//                    intentSaveData.putExtra(SaveData.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
//                    intentSaveData.putExtra(SaveData.EXTRA_DEVICE_SERIAL_NUMBER, lSerialNumber);
//                    intentSaveData.putExtra(SaveData.EXTRA_DEVICE_DEVICE_ADDRESS, lDeviceAddress);
//                    intentSaveData.putExtra(SaveData.EXTRA_DEVICE_VERSION, lFirmwareVersion);
//                    intentSaveData.putExtra(SaveData.EXTRA_DEVICE_CONNECTION_STATUS, mConnectionStatus);
//
//                    try {
//                        if (probeData != null) {
//                            intentSaveData.putParcelableArrayListExtra(SaveData.EXTRA_SAVED_DATA, (ArrayList<? extends Parcelable>) probeData);
//                            Log.d(TAG, "PRINTING PROBE DATA");
//                            Log.d(TAG, probeData.get(0).returnData());
//                        } else {
//                            Log.e(TAG, "Probe Data is null!");
//                        }
//
//                    } catch (Exception e) {
//                        Log.e(TAG, "Could not save correct data: " + e);
//                    }
//                    startActivity(intentSaveData);
//                }
//                return true;
//        }
        return true;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public void probeModeChange(View view) {
        boolean status = mBluetoothLeService.writeToProbeMode(02);
        Log.e(TAG, "STATUS OF WRITE: " + status);
    }

    public void calibrationRequest(View view) {
         //wait till ready continue does not equal false
        while (calibrationContinueCollection) {
            while (!readyToContinue) {}
            Log.e(TAG, "Ready to continue: " + readyToContinue);
            readyToContinue = false;
            boolean status = mBluetoothLeService.writeToCalibrationIndex((byte) 00);
            Log.e(TAG, "STATUS OF CALIBRATION WRITE: " + status);
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                Log.e(TAG, "Could not sleep" + e);
            }
            if (status) {
                dataToBeRead = 0;
                writtenCalibrationIndex = true;
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
                if (currentOp == null) {
                    Log.e(TAG, "2nd");
                    dataToBeRead = 0;
                    displayGattServices(mBluetoothLeService.getSupportedGattServices());

                } else {
                    Log.e(TAG, "Current Op isnt null, " + currentOp.getService().getUuid().toString() + " : " + currentOp.getCharacteristic().getUuid().toString());
                }
            } else {
                updateConnectionState("Disconnected");
            }
            new CountDownTimer(700, 1) { //definetly inefficicent, but if it works dont touch it lol

                public void onTick(long millisUntilFinished) {
//               mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
                }

                public void onFinish() {
                    dataToBeRead = 0;
                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
                    if (calibrationContinueCollection) {
                        Log.e(TAG, "Recursive call");
                        calibrationRequest(null);
                    }
                }
            }.start();



//            if (status) {
//                //written successfully to the probe now need to get the data back
//
//            }

        }
    }

    public void shotRequest(View view) {
        shotWriteType = 02;
         boolean status = mBluetoothLeService.writeToProbeMode(02);
         Log.e(TAG, "STATUS OF WRITE: " + status);
         try {
             Thread.sleep(1000);
         } catch (Exception e) {
             Log.e(TAG, "Could not sleep" + e);
         }
         if (status) { //status == true, so successfully read from probe
             dataToBeRead = 0;
             displayGattServices(mBluetoothLeService.getSupportedGattServices());

             if (currentOp == null) {
                 Log.e(TAG, "2nd");
                 dataToBeRead = 0;
                 displayGattServices(mBluetoothLeService.getSupportedGattServices());
             }
         } else {
             updateConnectionState("Disconnected");
         }
         new CountDownTimer(700, 1) { //definitely inefficient, but if it works don't touch it lol

             public void onTick(long millisUntilFinished) {
//               mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
             }

             public void onFinish() {
                 dataToBeRead = 0;
                 displayGattServices(mBluetoothLeService.getSupportedGattServices());
             }
         }.start();
    }

    public void resetQueueClick(View view) {
        resetQueue();
        Log.d(TAG, "Resetting the queue");
    }

    private void setUncalibratedX(String core_ax_value) {
        double uncalibratedAccX = 0;
//        Log.d(TAG, "Core AX value: " + core_ax_value);
        uncalibratedAccX = ((double)Integer.valueOf(core_ax_value) / (double)0x7FFF);
        DecimalFormat numberFormat = new DecimalFormat("0.00000");
        try {
            accelerometer_x_data.setText(numberFormat.format(uncalibratedAccX) + " G");
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
            accelerometer_y_data.setText(numberFormat.format(uncalibratedAccY) + " G");
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
            accelerometer_z_data.setText(numberFormat.format(uncalibratedAccZ) + " G");
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
            double roll = 0;
            roll = Math.atan((ay / az));
            roll = Math.toDegrees(roll);
            DecimalFormat numberFormat = new DecimalFormat("#.00");
            orientation_roll_data.setText(numberFormat.format(roll) + " °");
            oRoll = Double.parseDouble(numberFormat.format(roll));
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown when calculating roll: " + e);
        }
    }

    private void calculateDip(double ax, double ay, double az) {
        try {
//            Log.d(TAG, "calculating dip from ax: " + ax + " ay: " + ay +" az: " + az);
            double dip = 0;
            dip = Math.atan((-ax) / (Math.sqrt((ay * ay) + (az * az))));
            dip = Math.toDegrees(dip);
            DecimalFormat numberFormat = new DecimalFormat("#.00");
            orientation_dip_data.setText(numberFormat.format(dip) + " °");
            oDip = Double.parseDouble(numberFormat.format(dip));
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown when calculating dip: " + e);
        }
    }

    private void calculateAzimuth(double mx, double my, double mz, double roll, double dip) {
        try {
            Log.d(TAG, "Calculating azimuth");
            double azimuth = 0;
            azimuth = Math.atan(((-my) * Math.cos(roll) + mz * Math.sin(roll)) / (mx * Math.cos(dip) + my * Math.sin(dip) + mz * Math.sin(dip) * Math.cos(dip)));
            DecimalFormat numberFormat = new DecimalFormat("#.00");
            orientation_azimuth_data.setText(numberFormat.format(azimuth));
            oAzimuth = Double.parseDouble(numberFormat.format(azimuth));
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown when calculating azimuth: " + e);
        }
    }

    private void holdButton() {
        MenuItem holdMenuItem = menu.findItem(R.id.sensor_hold_button);
        if (hold) {
            hold = false;
            holdMenuItem.setTitle("Hold");
        } else {
            hold = true;
            holdMenuItem.setTitle("Resume");
        }
    }

    private void saveData() {
        try {
            ProbeData newData = null;
            switch (dev_shot_format.getText().toString()) {
                case "1":
                     newData = new ProbeData(0, "11:30,11-12-23" , 1, dRecordNumber, oRoll, oDip, oTemp, accX, accY,
                            accZ, accTemp, accMagError, mdMeanChecked,
                            mdAccX, mdAccY, mdAccZ, fastMagChecked);
                    break;
                case "2":
                    //TODO
                    break;
                case "3":
//                    newData = new ProbeData(0, "11:30,11-12-23",)
                    break;
                case "4":
                    break;
                default:
                    Log.e(TAG, "Error Shot format invalid");
                    break;
            }
            MenuItem saveMenuItem = menu.findItem(R.id.sensor_save_button);

            number = saveMenuItem.getTitle().toString().replace("Save ", "");
            number = Integer.toString(Integer.valueOf(number) + 1);
            saveMenuItem.setTitle("Save " + number);
            probeData.add(newData);
        } catch (Exception e) {
            Log.e(TAG, "SAVE Exception thrown: " + e);
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

        if (maxDev_mean_check.isChecked()) {
            meanMaxValue = maxValue - meanMaxValue;
        }
//        Log.e(TAG, "MAX / MEAN OF ACC X is: " + meanMaxValue);
        maxDev_accX_data.setText(Double.toString(meanMaxValue));
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

        if (maxDev_mean_check.isChecked()) {
            meanMaxValue = maxValue - meanMaxValue;
        }
//        Log.e(TAG, "MAX / MEAN OF ACC Y is: " + meanMaxValue);
        maxDev_accY_data.setText(Double.toString(meanMaxValue));
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

        if (maxDev_mean_check.isChecked()) {
            meanMaxValue = maxValue - meanMaxValue;
        }
//        Log.e(TAG, "MAX / MEAN OF ACC Z is: " + meanMaxValue);
        maxDev_accZ_data.setText(Double.toString(meanMaxValue));
    }

//    public void setUncalibratedMagX(String mag_x_value) {
//        double uncalibratedMagX = 0;
//        uncalibratedMagX = ((double)Integer.valueOf(mag_x_value) / (double)0x7FFF);
//        DecimalFormat numberFormat = new DecimalFormat("0.00000");
//        try {
//            magnetometer_x_data.setText(uncalibratedMagX + " µT");
//            magX = Double.parseDouble(Double.toString(uncalibratedMagX));
//        } catch (Exception e) {
//            Log.e(TAG, "Error setting uncalibrated mag x: " + e);
//        }
//        mag_x = Double.toString(uncalibratedMagX);
//        magXData.add(Double.parseDouble(mag_x));
//    }
//
//    public void calculateMeanMaxMagX() {
//        double meanMaxValue = 0;
//        double maxValue = 0;
//
//        for (int i = 0; i < magXData.size(); i++ ) {
//            meanMaxValue = meanMaxValue + magXData.get(i);
//            if (magXData.get(i) > maxValue) {
//                maxValue = magXData.get(i);
//            }
//        }
//        meanMaxValue = meanMaxValue / magXData.size();
//
//        if (maxDev_mean_check.isChecked()) {
//            meanMaxValue = maxValue - meanMaxValue;
//        }
//        maxDev_magX_data.setText(Double.toString(meanMaxValue));
//    }
//
//    public void setUncalibratedMagY(String mag_y_value) {
//        double uncalibratedMagY = 0;
//        uncalibratedMagY = ((double)Integer.valueOf(mag_y_value) / (double)0x7FFF);
//        DecimalFormat numberFormat = new DecimalFormat("0.00000");
//        try {
//            magnetometer_y_data.setText(uncalibratedMagY + " µT");
//            magY = Double.parseDouble(Double.toString(uncalibratedMagY));
//        } catch (Exception e) {
//            Log.e(TAG, "Error setting uncalibrated mag y: " + e);
//        }
//        mag_y = Double.toString(uncalibratedMagY);
//        magYData.add(Double.parseDouble(mag_y));
//    }

//    public void calculateMeanMaxMagY() {
//        double meanMaxValue = 0;
//        double maxValue = 0;
//
//        for (int i = 0; i < magYData.size(); i++ ) {
//            meanMaxValue = meanMaxValue + magYData.get(i);
//            if (magYData.get(i) > maxValue) {
//                maxValue = magYData.get(i);
//            }
//        }
//        meanMaxValue = meanMaxValue / magYData.size();
//
//        if (maxDev_mean_check.isChecked()) {
//            meanMaxValue = maxValue - meanMaxValue;
//        }
//        maxDev_magY_data.setText(Double.toString(meanMaxValue));
//    }

//    private void setUncalibratedMagZ(String mag_z_value) {
//        Log.e(TAG, "YASSSS");
//        double uncalibratedMagZ = 0;
//        uncalibratedMagZ = ((double)Integer.valueOf(mag_z_value) / (double)0x7FFF);
//        DecimalFormat numberFormat = new DecimalFormat("0.00000");
//        Log.e(TAG, "YESSSS");
//
//        try {
//            Log.e(TAG, "SETTING Z VELU");
//            magnetometer_z_data.setText(uncalibratedMagZ + " µT");
//            magZ = Double.parseDouble(Double.toString(uncalibratedMagZ));
//        } catch (Exception e) {
//            Log.e(TAG, "Error setting uncalibrated mag z: " + e);
//        }
//        mag_z = Double.toString(uncalibratedMagZ);
//        magZData.add(Double.parseDouble(mag_z));
//    }

//    private void calculateMeanMaxMagZ() {
//        double meanMaxValue = 0;
//        double maxValue = 0;
//
//        for (int i = 0; i < magZData.size(); i++ ) {
//            meanMaxValue = meanMaxValue + magZData.get(i);
//            if (magZData.get(i) > maxValue) {
//                maxValue = magZData.get(i);
//            }
//        }
//        meanMaxValue = meanMaxValue / magZData.size();
//
//        if (maxDev_mean_check.isChecked()) {
//            meanMaxValue = maxValue - meanMaxValue;
//        }
//        maxDev_magZ_data.setText(Double.toString(meanMaxValue));
//    }
}