////////////////////////////////////////////////////////////////////////////////
/**
 * \file OrientationActivity.java
 * \brief Orientation page to view a recorded measurement vs real time updates from the probe
 * \author Anna Pedersen
 * \date Created: 07/06/2024
 *
 */
package com.work.libtest;

import static com.work.libtest.CalibrationHelper.binCalData;
import static com.work.libtest.Operation.OPERATION_NOTIFY;
import static com.work.libtest.Operation.OPERATION_READ;
import static com.work.libtest.Operation.OPERATION_WRITE;

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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.TimeZone;

import com.work.libtest.CalibrationHelper;

//public class OrientationActivity extends AppCompatActivity {
//
//    public static final String EXTRA_DEVICE_NAME = "Device_name";
//    public static final String EXTRA_DEVICE_ADDRESS = "Device_address";
//    public static final String EXTRA_DEVICE_CONNECTION_STATUS = "Connection_status";
//
//    public static final String EXTRA_DEVICE_SERIAL_NUMBER = "Serial_number";
//    public static final String EXTRA_DEVCE_DEVICE_ADDRESS = "Device_gathered_adresses";
//    public static final String EXTRA_DEVICE_VERSION = "Device_firmare_version";
//
//    public static final String EXTRA_PARENT_ACTIVITY = "Parent_activity";
//    public static final String EXTRA_MEASUREMENT_ALL_DATA = "All_measurement_data";
//    public static final String EXTRA_MEASUREMENT_DATA = "Relevent_measurement";
//
//    public static final String EXTRA_NEXT_DEPTH = "next depth";
//    public static final String EXTRA_PREV_DEPTH = "prev depth";
//
//    public String mNextDepth;
//    public String mPrevDepth;
//
//    private final String LIST_NAME = "NAME";
//    private final String LIST_UUID = "UUID";
//
//    String parentActivity;
//    String measurementData;
////    measurementData
//
//    private boolean isCalibrated = false;
//    int _probeMode = 0;
//    int _calibrationIndex = 0;
//    String _calibrationData = "";
//    private int dataToBeRead = 0;
//
//    private BluetoothGattCharacteristic CORE_SHOT_CHARACTERISTIC;
//    private BluetoothGattCharacteristic BORE_SHOT_CHARACTERISTIC;
//
//    private String mDeviceName;
//    private String mDeviceAddress;
//    private String mConnectionStatus;
//
//    private Menu menu;
//
//    private String TAG = "Orientation Activity";
//
//    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<>();
//    private BluetoothLeService mBluetoothLeService;
//    private Handler handler;
//
//    private TextView rollTitle;
//    private TextView dipTitle;
//    private TextView azimuthTitle;
//
//    private double mRoll = 0;
//    private double mDip = 0;
//    private double mAzimuth = 0;
//
//    private TextView rollValue;
//    private TextView dipValue;
//    private TextView azimuthValue;
//
//    private TextView measurementRollValue;
//    private TextView measurementDipValue;
//    private TextView measurementAzimuthValue;
//
//    private int dRecordNumber = 0;
//
//    private TextView connectionStatus;
//    private ImageView connectionImg;
//
//    private double coreAX = 0;
//    private double coreAY = 0;
//    private double coreAZ = 0;
//
//    private String coreAXBinary, coreAYBinary, coreAZBinary, magXBinary, magYBinary, magZBinary;
//
//    private double magX = 0;
//    private double magY = 0;
//    private double magZ = 0;
//
//    private int shotFormat;
//
//    private Queue<Operation> operations = new LinkedList<>();
//    private Operation currentOp;
//
//
//    String highByte, lowByte;
//
//    private interface MessageConstants {
//        public static final int MESSAGE_READ = 0;
//        public static final int MESSAGE_WRITE = 0;
//        public static final int MESSAGE_TOAST = 0;
//    }
//
//    /**
//     * request an operation to be performed
//     * @param operation
//     */
//    public synchronized void request(Operation operation) {
//        Log.d(TAG, "Requestion Operation: " + operation.toString());
//        operations.add(operation);
//        if (currentOp == null) {
//            currentOp = operations.poll();
//            performOperation();
//        } else {
//            Log.e(TAG, "Current operation is null");
//        }
//    }
//
//    /**
//     * runs whenever an operation is completed
//     */
//    public synchronized void operationCompleted() {
//        Log.d(TAG, "Operation complete, moving to next one");
//        currentOp = null;
//        if (operations.peek() != null) {
//            currentOp = operations.poll();
//            performOperation();
//        } else {
//            Log.d(TAG, "Queue empty");
//        }
//    }
//
//    /**
//     * code to perform the operation depending on its action (write,read,notify)
//     */
//    public void performOperation() {
//        Log.d(TAG, "Performing operation");
//        if (currentOp != null) {
//            if (currentOp.getService() != null && currentOp.getCharacteristic() != null && currentOp.getAction() != 0) {
////                Log.e(TAG, "Current Op Service: " + currentOp.getService() + )
//                switch (currentOp.getAction()) {
//                    case OPERATION_WRITE:
////                        Log.e(TAG, "Writing to characteristic with service: " + currentOp.getService().getUuid().toString() + " and characteristic: " + currentOp.getCharacteristic().getUuid().toString());
////                        try {
////                            boolean write = mBluetoothLeService.writeToCalibrationIndex();
////                            if (write) {
////                                Log.e(TAG, "Written successfully");
////                                operationCompleted();
////                            }
////                        } catch (Exception e) {
////                            Log.e(TAG, "Exception thrown; " + e);
////                        }
//                        break;
//                    case OPERATION_READ:
//                        try {
////                            Log.d(TAG, "Reading characteristic with service: " + currentOp.getService().getUuid().toString() + " and characteristic: " + currentOp.getCharacteristic().getUuid().toString());
//                            mBluetoothLeService.readCharacteristic(currentOp.getCharacteristic());
//                        } catch (Exception e) {
//                            Log.e(TAG, "Cannot Read charateristic: " + e);
//                        }
//                        break;
//                    case OPERATION_NOTIFY:
//                        try {
//                            mBluetoothLeService.setCharacteristicNotification(currentOp.getCharacteristic(), true);
//                        } catch (Exception e) {
//                            Log.e(TAG, "CANT SET CHARACTERISTIC TO NOTIFY" + e);
//                        }
//                        break;
//                    default:
//                        Log.e(TAG, "Action not valid");
//                        break;
//                }
//            }
//        }
//    }
//
//    /**
//     * Clear queue to stop all future operations
//     */
//    public void resetQueue() {
//        operations.clear();
//        currentOp = null;
//    }
//
//    private final ServiceConnection mServiceConnection = new ServiceConnection() {
//        /**
//         *
//         * @param name The concrete component name of the service that has
//         * been connected.
//         *
//         * @param service The IBinder of the Service's communication channel,
//         * which you can now make calls on.
//         */
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
//            if (!mBluetoothLeService.initialize()) {
//                Log.e(TAG, "Enable to initialise bluetooth");
//                finish();
//            }
//            mBluetoothLeService.connect(mDeviceAddress);
//        }
//
//        /**
//         *
//         * @param name The concrete component name of the service whose
//         * connection has been lost.
//         */
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//            mBluetoothLeService = null;
//        }
//    };
//
//    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
//        /**
//         *
//         * @param context The Context in which the receiver is running.
//         * @param intent The Intent being received.
//         */
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            final String action = intent.getAction();
//            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
//                updateConnectionState("Connected");
//                invalidateOptionsMenu();
//            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
//                updateConnectionState("Disconnected");
//                mBluetoothLeService.connect(mDeviceAddress);
//            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
//                displayGattServices(mBluetoothLeService.getSupportedGattServices());
//            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//                if (intent.getStringExtra(BluetoothLeService.CORE_SHOT) != null) {
//
//                    String coreShotData = intent.getStringExtra(BluetoothLeService.CORE_SHOT);
//                    String[] shotData = coreShotData.split(":", 20);
//                    shotFormat = Integer.parseInt(shotData[0]);
//
//                    switch (shotFormat) {
//                        case 1:
//                            //first format(12): Format(1), Record Number(2), Probe Temperature(2), AX(2), AY(2), AZ(2), Accelerometer Temperature(1)
//                            for (int i = 0; i < shotData.length; i++) {
//                                if (i == 5) { //AX 1
//                                    coreAXBinary = "";
//                                    coreAX = Double.parseDouble(shotData[i]);
//                                    int num = Integer.parseInt(shotData[i]);
//                                    if (num < 0) {
//                                        num = 128 + (128 + num);
//                                    }
//                                    String binaryOutput = Integer.toBinaryString(num);
//                                    if (Integer.toBinaryString(num).length() < 8) {
//                                        for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
//                                            binaryOutput = "0" + binaryOutput;
//                                        }
//                                    }
//                                    coreAXBinary = binaryOutput;
//                                } else if (i == 6) { //AX 2
//                                    int num = Integer.parseInt(shotData[i]);
//                                    if (num < 0) {
//                                        num = 128 + (128 + num);
//                                    }
//                                    String binaryOutput = Integer.toBinaryString(num);
//                                    if (Integer.toBinaryString(num).length() < 8) {
//                                        for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
//                                            binaryOutput = "0" + binaryOutput;
//                                        }
//                                    }
//                                    coreAXBinary = coreAXBinary + binaryOutput;
//
//                                    if (coreAXBinary.length() > 16) {
//                                        //TODO Error
//                                    } else {
//                                        if (coreAXBinary.charAt(0) == '1') {
//                                            coreAXBinary = coreAXBinary.substring(1);
//                                            coreAX = Integer.parseInt(coreAXBinary, 2);
//                                            coreAX = coreAX * -1;
//                                        } else {
//                                            coreAX = Integer.parseInt(coreAXBinary, 2);
//                                        }
//                                    }
//
//                                } else if (i == 7) { //AY 1
//                                    coreAYBinary = "";
//                                    coreAY = Double.parseDouble(shotData[i]);
//                                    int num = Integer.parseInt(shotData[i]);
//                                    if (num < 0) {
//                                        num = 128 + (128 + num);
//                                    }
//                                    String binaryOutput = Integer.toBinaryString(num);
//                                    if (Integer.toBinaryString(num).length() < 8) {
//                                        for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
//                                            binaryOutput = "0" + binaryOutput;
//                                        }
//                                    }
//                                    coreAYBinary = binaryOutput;
//                                } else if (i == 8) { //AY 2
//                                    int num = Integer.parseInt(shotData[i]);
//                                    if (num < 0) {
//                                        num  = 128 + (128 + num);
//                                    }
//                                    String binaryOutput = Integer.toBinaryString(num);
//                                    if (Integer.toBinaryString(num).length() < 8) {
//                                        for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
//                                            binaryOutput = "0" + binaryOutput;
//                                        }
//                                    }
//                                    coreAYBinary = coreAYBinary + binaryOutput;
//
//                                    if (coreAYBinary.length() > 16) {
//                                        //TODO ERROR
//                                    } else {
//                                        if (coreAYBinary.charAt(0) == '1') {
//                                            coreAYBinary = coreAYBinary.substring(1);
//                                            coreAY = Integer.parseInt(coreAYBinary, 2);
//                                            coreAY = coreAY * -1;
//                                        } else {
//                                            coreAY = Integer.parseInt(coreAYBinary, 2);
//                                        }
//                                    }
//                                } else if (i == 9) { //AZ 1
//                                    coreAZBinary = "";
//                                    coreAZ = Double.parseDouble(shotData[i]);
//                                    int num = Integer.parseInt(shotData[i]);
//                                    if (num < 0) {
//                                        num = 128 + (128 + num);
//                                    }
//                                    String binaryOutput = Integer.toBinaryString(num);
//                                    if (Integer.toBinaryString(num).length() < 8) {
//                                        for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
//                                            binaryOutput = "0" + binaryOutput;
//                                        }
//                                    }
//                                    coreAZBinary = binaryOutput;
//                                    Log.e(TAG, "CoreAZ 1: " + binaryOutput);
//                                } else if (i == 10) { //AZ 2
//                                    int num = Integer.parseInt(shotData[i]);
//                                    if (num < 0) {
//                                        num  = 128 + (128 + num);
//                                    }
//                                    String binaryOutput = Integer.toBinaryString(num);
//                                    if (Integer.toBinaryString(num).length() < 8) {
//                                        for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
//                                            binaryOutput = "0" + binaryOutput;
//                                        }
//                                    }
//                                    coreAZBinary = coreAZBinary + binaryOutput;
//                                    Log.e(TAG, "CoreAZ 2: " + binaryOutput);
//                                    if (coreAZBinary.length() > 16) {
//                                        //TODO ERROR
//                                    } else {
//                                        if (coreAZBinary.charAt(0) == '1') {
//                                            coreAZBinary = coreAZBinary.substring(1);
//                                            coreAZ = Integer.parseInt(coreAZBinary, 2);
//                                            coreAZ = coreAZ * -1;
//                                        } else {
//                                            coreAZ = Integer.parseInt(coreAZBinary, 2);
//                                        }
//                                    }
//                                    try {
//                                        calculateRoll(Double.valueOf(coreAY), Double.valueOf(coreAZ));
//                                        calculateDip(Double.valueOf(coreAX), Double.valueOf(coreAY), Double.valueOf(coreAZ));
//                                    } catch (Exception e) {
//                                        Log.e(TAG, "Error calling calculate roll and dip functions: " + e);
//                                    }
//                                }
//                            }
//                            break;
//                        case 2:
//                            Log.e(TAG, "Shot format is 2");
//                            break;
//                        case 3:
//                            Log.e(TAG, "Shot format is 3");
//                            break;
//                        default:
//                            Log.e(TAG, "ERROR Shot format not valid for core shot");
//                            break;
//                    }
//
//                } else if (intent.getStringExtra(BluetoothLeService.BORE_SHOT) != null) {
//                    String boreShotData = intent.getStringExtra(BluetoothLeService.BORE_SHOT);
//                    String[] bore_shot_data = boreShotData.split(":", 20);
//                    shotFormat = Integer.parseInt(bore_shot_data[0]);
//
//                    switch (shotFormat) {
//                        case 1:
//                            Log.e(TAG, "Bore shot format = 1");
//                            break;
//                        case 2:
//                            Log.e(TAG, "Bore shot format = 2");
//                            break;
//                        case 3:
//                            //third format(20) : Format(1), Record number(2), Probe temperature(2), AX(2), AY(2), AZ(2), MX(3), MY(3), MZ(3)
//                           Log.e(TAG, "Bore shot format = 3");
//                           //REHAUL
//
////
//                            Log.e(TAG, "ANNA ----------------------------------");
//                            byte[] char_all_bore_shot = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
//                            String[] string_all_bore_shot = {"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0"};
//                            for (int i = 0; i < bore_shot_data.length; i++) {
//////                                        Log.e(TAG, "Shot " + i + ": " + bore_shot_data[i]);
//                                int bore_value;
//                                try {
//                                    bore_value = Integer.valueOf(bore_shot_data[i]);
//                                } catch (Exception e) {
//                                    bore_value = -2;
//                                }
//                                try {
////                                            char_all_bore_shot[i] = Byte.parseByte(Integer.toHexString(bore_value));
//                                    if (bore_value < 0) {
//                                        String unsignedHex = String.format("%02X", bore_value & 0xff);
//                                        string_all_bore_shot[i] = unsignedHex;
////                                                char_all_bore_shot[i] = (byte) Integer.parseInt(string_all_bore_shot[i], 16);//Byte.parseByte();
////                                                char_all_bore_shot[i] = (byte) (Integer.parseInt(string_all_bore_shot[i], 16) & 0xFF);
//                                    } else {
//                                        string_all_bore_shot[i] = Integer.toHexString(bore_value);
//                                    }
//                                } catch (Exception e) {
//                                    string_all_bore_shot[i] = "-1";
//                                }
//                            }
//
//                            //Format all the displayed numbers so they arent incredibly long
//                            DecimalFormat numberFormat = new DecimalFormat("0.0000000");
//
////                                  //first 2 bytes after shot type is the record number
//                            try {
//                                byte value1 = (byte) Integer.parseInt(string_all_bore_shot[1], 16);
//                                byte value2 = (byte) Integer.parseInt(string_all_bore_shot[2], 16);
//                                dRecordNumber = (int) twoBytesToValue(value2, value1);
//                                numberFormat.format(dRecordNumber);
////                                dev_record_number.setText(String.valueOf(dRecordNumber));
//                            } catch (Exception e) {
//                                Log.e(TAG, "EXCEPTION thrown in record number: " + e);
//                            }
//
////                                  next 2 bytes after probe temperature
//                            try {
//                                byte value1 = (byte) Integer.parseInt(string_all_bore_shot[3], 16);
//                                byte value2 = (byte) Integer.parseInt(string_all_bore_shot[4], 16);
//                                int shotProbeTempRaw = ((int)value1 << 8) + (((int)value2) & 0x00FF);
//                                double probeTempU = (double)shotProbeTempRaw/256.0;
//                                double probeTemp = CalibrationHelper.temp_param[0] + (CalibrationHelper.temp_param[1] * probeTempU);
//
////                                orientation_temperature_data.setText(numberFormat.format(probeTemp));
//                                //TODO make so if the accelerometer or magnetometer are actually passing temp data it displays the corresponding value
////                                accelerometer_temp_data.setText(numberFormat.format(probeTemp));
////                                magnetometer_temp_data.setText(numberFormat.format(probeTemp));
//                            } catch (Exception e) {
//                                Log.e(TAG, "Exception thrown in probe temperature: " + e);
//                            }
//
//                            double ux = 0;
//                            double uy = 0;
//                            double uz = 0;
//                            double[] calAcc;
//
//                            //TODO could probably turn this into a function for better code functionality
//                            try {
//                                byte value1 = (byte) Integer.parseInt(string_all_bore_shot[5], 16);
//                                byte value2 = (byte) Integer.parseInt(string_all_bore_shot[6], 16);
//                                int shotAccX = ((int)value1 << 8) + (((int)value2) & 0x00FF);
//                                ux = ((double)shotAccX)/32.0/512.0;
//                            } catch (Exception e) {
//                                Log.e(TAG, "Exception thrown setting ux: " + e);
//                            }
//
//                            try {
//                                byte value1 = (byte) Integer.parseInt(string_all_bore_shot[7], 16);
//                                byte value2 = (byte) Integer.parseInt(string_all_bore_shot[8], 16);
//                                int shotAccY = ((int)value1 << 8) + (((int)value2) & 0x00FF);
//                                uy = ((double)shotAccY)/32.0/512.0;
//                            } catch (Exception e) {
//                                Log.e(TAG, "Exception thrown setting uy: " + e);
//                            }
//
//                            try {
//                                byte value1 = (byte) Integer.parseInt(string_all_bore_shot[9], 16);
//                                byte value2 = (byte) Integer.parseInt(string_all_bore_shot[10], 16);
//                                int shotAccZ = ((int)value1 << 8) + (((int)value2) & 0x00FF);
//                                uz = ((double)shotAccZ)/32.0/512.0;
//
//                            } catch (Exception e) {
//                                Log.e(TAG, "Exception thrown setting uz: " + e);
//                            }
//
//                            double umx = 0;
//                            double umy = 0;
//                            double umz = 0;
//                            double[] calMag;
//
//                            try {
//                                byte value1 = (byte) Integer.parseInt(string_all_bore_shot[11], 16);
//                                byte value2 = (byte) Integer.parseInt(string_all_bore_shot[12], 16);
//                                byte value3 = (byte) Integer.parseInt(string_all_bore_shot[13], 16);
//                                int shotMagX = ((((int)value1 << 8) + (((int)value2) & 0x00FF) ) << 8) + (((int)value3) & 0x00FF);
//                                umx = ((double)shotMagX *0.001);
//                            } catch (Exception e) {
//                                Log.e(TAG, "Exception thrown setting umx");
//                            }
//
//                            try {
//                                byte value1 = (byte) Integer.parseInt(string_all_bore_shot[14], 16);
//                                byte value2 = (byte) Integer.parseInt(string_all_bore_shot[15], 16);
//                                byte value3 = (byte) Integer.parseInt(string_all_bore_shot[16], 16);
//                                int shotMagY = ((((int)value1 << 8) + (((int)value2) & 0x00FF) ) << 8) + (((int)value3) & 0x00FF);
//                                umy = ((double)shotMagY *0.001);
//                            } catch (Exception e) {
//                                Log.e(TAG, "Exception thrown setting umx");
//                            }
//
//                            try {
//                                byte value1 = (byte) Integer.parseInt(string_all_bore_shot[17], 16);
//                                byte value2 = (byte) Integer.parseInt(string_all_bore_shot[18], 16);
//                                byte value3 = (byte) Integer.parseInt(string_all_bore_shot[19], 16);
//                                int shotMagZ = ((((int)value1 << 8) + (((int)value2) & 0x00FF) ) << 8) + (((int)value3) & 0x00FF);
//                                umz = ((double)shotMagZ *0.001);
//                            } catch (Exception e) {
//                                Log.e(TAG, "Exception thrown setting umx");
//                            }
//
//                            calAcc = CalibrationHelper.CalibrationHelp(CalibrationHelper.accelerationCalibration, ux, uy, uz);
//                            calMag = CalibrationHelper.CalibrationHelp(CalibrationHelper.magnetometerCalibration, umx, umy, umz);
//
//                            //Set the display values for x, y and z calibrated values
//                            double cx = calAcc[0];
//                            double cy = calAcc[1];
//                            double cz = calAcc[2];
//
//                            double cmx = calMag[0];
//                            double cmy = calMag[1];
//                            double cmz = calMag[2];
//
//                            //Set roll and dip values
//                            double cal_roll_radian = Math.atan2(cy, cz);
//                            if (cal_roll_radian > Math.PI) { cal_roll_radian -= (2 * Math.PI); }
//                            if (cal_roll_radian < -Math.PI) { cal_roll_radian += (2*Math.PI); }
//                            double cal_dip_radian = Math.atan2(-cx, Math.sqrt((cy*cy)+(cz*cz)));
//
//                            double den = (cmx * Math.cos(cal_dip_radian)) + (cmy * Math.sin(cal_dip_radian) * Math.sin(cal_roll_radian)) + (cmz * Math.sin(cal_dip_radian) * Math.cos(cal_roll_radian));
//                            double num = (cmy * Math.cos(cal_roll_radian)) - (cmz * Math.sin(cal_roll_radian));
//                            double cal_az_radian = Math.atan2(-num, den);
//                            if (cal_az_radian > Math.PI) { cal_az_radian -= (2*Math.PI); }
//                            if (cal_az_radian < -Math.PI) { cal_az_radian += (2*Math.PI); }
//                            if (cal_az_radian < 0) { cal_az_radian += (2*Math.PI); }
//
//                            //convert to degrees :(
//                            double cal_roll_degree = cal_roll_radian*180/Math.PI;
//                            double cal_dip_degree = cal_dip_radian*180/Math.PI;
//                            double cal_az_degree = cal_az_radian*180/Math.PI;
//
//                            Log.e(TAG, "---------------AJP---------------");
//                            Log.e(TAG, "roll" + cal_roll_radian + ",dip" + cal_dip_radian + "azimuth" + cal_az_radian);
//
//                            //display orientation data
//                            rollValue.setText(String.valueOf(Double.parseDouble(numberFormat.format(cal_roll_degree))));
//                            dipValue.setText(numberFormat.format(cal_dip_degree));
//                            azimuthValue.setText(numberFormat.format(cal_az_degree));
//
////                            accelerometer_x_data.setText(numberFormat.format(cx));
////                            accelerometer_y_data.setText(numberFormat.format(cy));
////                            accelerometer_z_data.setText(numberFormat.format(cz));
////
////                            magnetometer_x_data.setText(numberFormat.format(cmx));
////                            magnetometer_y_data.setText(numberFormat.format(cmy));
////                            magnetometer_z_data.setText(numberFormat.format(cmz));
//
//                            break;
//                        default:
//                            Log.e(TAG, "Bore shot format not recognised");
//                            break;
//                    }
//                } else if (intent.getStringExtra(BluetoothLeService.PROBE_MODE) != null) {
//                    Log.d(TAG, "Probe Mode is: " + intent.getStringExtra(BluetoothLeService.PROBE_MODE));
//                    if (intent.getStringExtra(BluetoothLeService.PROBE_MODE).equals("2")) {
//                        _probeMode = 2;
//                        Log.d(TAG, "Probe Mode set to 2");
//                    } else {
//                        _probeMode = 0;
//                        Log.d(TAG, "Probe mode set to 0");
//                    }
//                }
////                else if (intent.getStringExtra(BluetoothLeService.CALIBRATION_INDEX) != null) {
////                    Log.e(TAG, "Calibration index is: " + intent.getStringExtra(BluetoothLeService.CALIBRATION_INDEX));
////                    _calibrationIndex = Integer.parseInt(intent.getStringExtra(BluetoothLeService.CALIBRATION_INDEX));
////                } else if (intent.getStringExtra(BluetoothLeService.CALIBRATION_DATA) != null) {
////                    Log.e(TAG, "Calibration Data is: " + intent.getStringExtra(BluetoothLeService.CALIBRATION_DATA));
////                    _calibrationData = intent.getStringExtra(BluetoothLeService.CALIBRATION_DATA);
////                }
//                operationCompleted();
//            } else if (intent != null) {
//                operationCompleted();
//            }
//        }
//    };
//
//    public static short toInt16(byte[] bytes, int index) {
//        return (short) ((bytes[index + 1] << 8) | (bytes[index] & 0xFF));
//    }
//
//    private double twoBytesToValue(byte value1, byte value2) {
//        byte[] dataArray = new byte[] {value1, value2};
//        int i1 = toInt16(dataArray, 0);
//        return i1;
//    }
//
//    private class ConnectedThread extends Thread {
//        private final BluetoothSocket mmSocket;
//        private final InputStream mmInStream;
//        private final OutputStream mmOutStream;
//        private byte[] mmBuffer;
//
//        public ConnectedThread(BluetoothSocket socket) {
//            mmSocket = socket;
//            InputStream tmpIn = null;
//            OutputStream tmpOut = null;
//
//            try {
//                tmpIn = socket.getInputStream();
//            } catch (IOException e){
//                Log.e(TAG, "Error occured when creating an input stream: " + e);
//            }
//
//            try {
//                tmpOut = socket.getOutputStream();
//            } catch (IOException e) {
//                Log.e(TAG, "Error occured when creating output stream: " + e);
//            }
//
//            mmInStream = tmpIn;
//            mmOutStream = tmpOut;
//        }
//
//
//        public void run() {
//            mmBuffer = new byte[1024];
//            int numBytes;
//
//            while (true) {
//                try {
//                    numBytes = mmInStream.read(mmBuffer);
//                    Message readMsg = handler.obtainMessage(
//                            MessageConstants.MESSAGE_READ, numBytes, -1, mmBuffer
//                    );
//                    readMsg.sendToTarget();
//                } catch (IOException e) {
//                    Log.d(TAG, "Input stream was disconnected: " + e);
//                    break;
//                }
//            }
//        }
//
//        public void write(byte[] bytes) {
//            try {
//                mmOutStream.write(bytes);
//                Message writtenMsg = handler.obtainMessage(
//                        MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer
//                );
//                writtenMsg.sendToTarget();
//            } catch (IOException e) {
//                Log.e(TAG, "Error occured in sending data", e);
//                Message writeErrorMsg = handler.obtainMessage(MessageConstants.MESSAGE_TOAST);
//                Bundle bundle = new Bundle();
//                bundle.putString("toast", "couldn't send data to the other device");
//                writeErrorMsg.setData(bundle);
//                handler.sendMessage(writeErrorMsg);
//            }
//        }
//
//        public void cancel() {
//            try {
//                mmSocket.close();
//            } catch (IOException e) {
//                Log.e(TAG, "Could not close the connect socket");
//            }
//        }
//    }
//
//
//    /**
//     * calculate the probe roll value
//     * @param ay
//     * @param az
//     */
//    private void calculateRoll(double ay, double az) {
//        try {
//            double roll = 0;
//            roll = Math.atan(ay / az);
//            roll = Math.toDegrees(roll);
//            DecimalFormat numberFormat = new DecimalFormat("#.00");
//            mRoll = roll;
//            rollValue.setText(numberFormat.format(mRoll) + " °");
//        } catch (Exception e) {
//            Log.e(TAG, "Exception thrown when calculating roll: " + e);
//        }
//    }
//
//    /**
//     * Calculate the probe dip value
//     * @param ax
//     * @param ay
//     * @param az
//     */
//    private void calculateDip(double ax, double ay, double az) {
//        try {
//            double dip = 0;
//            dip = Math.atan((-ax) / Math.sqrt((ay * ay) + (az * az)));
//            dip = Math.toDegrees(dip);
//            DecimalFormat numberFormat = new DecimalFormat("#.00");
//            mDip = dip;
//            dipValue.setText(numberFormat.format(mDip) + " °");
//        } catch (Exception e) {
//            Log.e(TAG, "Exception thrown when calculating dip: " + e);
//        }
//    }
//
//    /**
//     * Calculate the probe azimuth value
//     * @param mx
//     * @param my
//     * @param mz
//     * @param roll
//     * @param dip
//     */
//    private void calculateAzimuth(double mx, double my, double mz, double roll, double dip) {
//        try {
//            Log.e(TAG, "AZIMUTH");
//            double azimuth = 0;
//            azimuth = Math.atan(((-my) * Math.cos(roll) + mz * Math.sin(roll)) / (mx * Math.cos(dip) + my * Math.sin(dip) + mz * Math.sin(dip) * Math.cos(dip)));
//            DecimalFormat numberFormat = new DecimalFormat("#.00");
//            mAzimuth = azimuth;
//            azimuthValue.setText(numberFormat.format(mAzimuth) + " °");
//        } catch (Exception e) {
//            Log.e(TAG, "Exception thrown when calculating azimuth");
//        }
//    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//
//    }
//
//    /**
//     * runs when the activity resumes to reset any information passed in to the activity and to register the bluetooth
//     */
//    @Override
//    protected void onResume() {
//        super.onResume();
//
//        final Intent intent = getIntent();
//        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
//        mDeviceAddress = intent.getStringExtra(EXTRA_DEVCE_DEVICE_ADDRESS);
//        mConnectionStatus = intent.getStringExtra(EXTRA_DEVICE_CONNECTION_STATUS);
//        mPrevDepth = intent.getStringExtra(EXTRA_PREV_DEPTH);
//        mNextDepth = intent.getStringExtra(EXTRA_NEXT_DEPTH);
//        try {
//            if (mConnectionStatus.equals("Connected")) {
//                updateConnectionState("Connected");
//            } else {
//                Log.e(TAG, "connection state disconnected upon starting activity");
//                updateConnectionState("Disconnected");
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Exception thrown in onResume: " + e);
//        }
//
//        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
//        if (mBluetoothLeService != null) {
//            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
//            Log.d(TAG, "Connection request result: " + result);
//        }
//    }
//
//    /**
//     *
//     * @param savedInstanceState If the activity is being re-initialized after
//     *     previously being shut down then this Bundle contains the data it most
//     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
//     *
//     * setup the activity
//     */
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_orientation);
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//
//        final Intent intent = getIntent();
//        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
//        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
//        mConnectionStatus = intent.getStringExtra(EXTRA_DEVICE_CONNECTION_STATUS);
//        mPrevDepth = intent.getStringExtra(EXTRA_PREV_DEPTH);
//        mNextDepth = intent.getStringExtra(EXTRA_NEXT_DEPTH);
//
//        parentActivity = intent.getStringExtra(EXTRA_PARENT_ACTIVITY);
//        measurementData = intent.getStringExtra(EXTRA_MEASUREMENT_DATA);
//
//        rollValue = (TextView) findViewById(R.id.probe_rollValue);
//        dipValue = (TextView) findViewById(R.id.probe_dipValue);
//        azimuthValue = (TextView) findViewById(R.id.probe_azimuthValue);
//
//        measurementRollValue = (TextView) findViewById(R.id.measurement_rollValue);
//        measurementDipValue = (TextView) findViewById(R.id.measurement_dipValue);
//        measurementAzimuthValue = (TextView) findViewById(R.id.measurement_azimuthValue);
//
//        connectionStatus = (TextView) findViewById(R.id.orientation_connectionStatus);
//        connectionImg = (ImageView) findViewById(R.id.orientation_connectionImg);
//
//        if (parentActivity.equals("MEASUREMENT")) {
//            String[] splitData = measurementData.split(";", 20);
//            measurementRollValue.setText(splitData[1]); //roll
//            measurementDipValue.setText(splitData[2]); //dip
//            measurementAzimuthValue.setText(splitData[3]); //azimuth
//        } else {
//            measurementRollValue.setText("Null"); //roll
//            measurementDipValue.setText("Null"); //dip
//            measurementAzimuthValue.setText("Null"); //azimuth
//        }
//
//        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
//        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
//
//        try {
//            if (mConnectionStatus.equals("Connected")) {
//                updateConnectionState("Connected");
//            } else {
//                Log.e(TAG, "Connection status is disconnected upon activity start");
//                updateConnectionState("Disconnected");
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Connection status null");
//        }
//
//    }
//
//
//
//
//    /**
//     * unregister the bluetooth receiver when the app is paused
//     */
//    @Override
//    protected void onPause() {
//        super.onPause();
//        unregisterReceiver(mGattUpdateReceiver);
//    }
//
//    /**
//     * Go through all gatt services that the phone can detect on the BLE device
//     * @param gattServices
//     */
//    private void displayGattServices(List<BluetoothGattService> gattServices) {
//        if (gattServices == null) return;
//
//        String uuid = null;
//        String unknownServiceString = "Unknown service";
//        String unknownCharaString = "Unknown characteristics";
//        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<>();
//        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicsData = new ArrayList<>();
//        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
//
//        for (BluetoothGattService gattService : gattServices) {
//            HashMap<String, String> currentServiceData = new HashMap<>();
//            uuid = gattService.getUuid().toString();
//            currentServiceData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
//            currentServiceData.put(LIST_UUID, uuid);
//            gattServiceData.add(currentServiceData);
//
//            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<>();
//            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
//            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<>();
//
//            Log.d(TAG, "Gatt service is: " + gattService.getUuid().toString());
//            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
//                charas.add(gattCharacteristic);
//                HashMap<String, String> currentCharaData = new HashMap<>();
//                uuid = gattCharacteristic.getUuid().toString();
//                currentCharaData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
//                currentCharaData.put(LIST_UUID, uuid);
//                gattCharacteristicGroupData.add(currentCharaData);
//
//                Log.d(TAG, "Gatt characteristic is: " + gattCharacteristic.getUuid().toString());
//                if (gattCharacteristic.getUuid() != null) {
//                    if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.DEVICE_ID)) {
//                        Operation getDeviceIDOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);
//                        request(getDeviceIDOperation);
//                    } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.DEVICE_ADDRESS)) {
//                        Operation getDeviceAddressOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);
//                        request(getDeviceAddressOperation);
//                    } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.VERSION_MAJOR)) {
//                        Operation getVersionMajorOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);
//                        request(getVersionMajorOperation);
//                    } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.VERSION_MINOR)) {
//                        Operation getVersionMinorOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);
//                        request(getVersionMinorOperation);
//                    } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.PROBE_MODE)) {
//                        Operation getProbeModeOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);
//                        request(getProbeModeOperation);
//                    }
////                    else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.CALIBRATION_INDEX)) {
////                        Log.e(TAG, "CALIIBRATION INDEX");
////                        Operation getCalibrationIndexOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);
////                        request(getCalibrationIndexOperation);
////                    }
////                    else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.CALIBRATION_DATA)) {
////                        Operation getCalibrationDataOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);
////                        request(getCalibrationDataOperation);
////                    }
//                    else if (_probeMode == 2 && gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.CORE_SHOT)) {
//                        Log.e(TAG, "Reading core shot");
//                        CORE_SHOT_CHARACTERISTIC = gattCharacteristic;
//                        Operation getCoreShotOperation = new Operation(gattService, gattCharacteristic, OPERATION_NOTIFY);
//                        request(getCoreShotOperation);
//                    } else if (_probeMode == 2 && gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.BORE_SHOT)) {
//                        Log.e(TAG, "Reading bore shot");
//                        BORE_SHOT_CHARACTERISTIC = gattCharacteristic;
//                        Operation getCoreShotOperation = new Operation(gattService, gattCharacteristic, OPERATION_NOTIFY);
//                        try {
//                            request(getCoreShotOperation);
//                        } catch (Exception e) {
//                            Log.e(TAG, "Request for BORE Shot failed!");
//                        }
//                    }
//                } else {
//                    Log.e(TAG, "gatt characteristic uuid is null");
//                }
//
//            }
//        }
//    }
//
//    private void updateConnectionState(final String resourceId) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    if (resourceId.equals("Connected")) {
//                        Log.d(TAG, "Device Connected");
//                        connectionStatus.setText("Connected");
//                        connectionImg.setImageResource(R.drawable.ready);
//                        mConnectionStatus = "Connected";
//
//                    } else if (resourceId.equals("Disconnected")) {
//                        Log.e(TAG, "Device Disconnected");
//                        connectionStatus.setText("Disconnected");
//                        connectionImg.setImageResource(R.drawable.unconnected);
//                        mConnectionStatus = "Disconnected";
//
//                        //Just doesnt work
//                        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
//                        if (mBluetoothLeService != null) {
//                            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
//                            Log.d(TAG, "Connection request result: " + result);
//                            if (result) {
//                                updateConnectionState("Connected");
//                                try {
//                                    shotRequest();
//                                } catch (Exception e) {
//                                    Log.e(TAG, "Exception thrown: " + e);
//                                }
//                            }
//                        }
//                    } else {
//                        Log.e(TAG, "Error, no probe selected");
//                    }
//                } catch (Exception e) {
//                    Log.d(TAG, "Exception thrown: " + e);
//                }
//            }
//        });
//    }
//
//    /**
//     *
//     * @param menu The options menu in which you place your items.
//     *
//     * @return
//     */
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.menu_orientation, menu);
//        this.menu = menu;
//        return true;
//    }
//
//    /**
//     *
//     * @param item The menu item that was selected.
//     *
//     * @return
//     */
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == R.id.back_button) {
//            if (parentActivity.equals("MEASUREMENT")) {
//                Intent intent = new Intent(this, ViewMeasurements.class);
//                intent.putExtra(ViewMeasurements.EXTRA_DEVICE_NAME, mDeviceName);
//                intent.putExtra(ViewMeasurements.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
//                intent.putExtra(ViewMeasurements.EXTRA_DEVICE_CONNECTION_STATUS, mConnectionStatus);
//                intent.putExtra(ViewMeasurements.EXTRA_NEXT_DEPTH, mNextDepth);
//                intent.putExtra(ViewMeasurements.EXTRA_PREV_DEPTH, mPrevDepth);
//                startActivity(intent);
//            } else {
//                Intent intent = new Intent(this, ProbeDetails.class);
//                intent.putExtra(ProbeDetails.EXTRA_DEVICE_NAME, mDeviceName);
//                intent.putExtra(ProbeDetails.EXTRA_DEVICE_DEVICE_ADDRESS, mDeviceAddress);
//                intent.putExtra(ProbeDetails.EXTRA_DEVICE_CONNECTION_STATUS, mConnectionStatus);
//                startActivity(intent);
//            }
//        } else if (item.getItemId() == R.id.tools_button) {
//
//        } else if (item.getItemId() == R.id.tools_setZero) {
//
//        } else if (item.getItemId() == R.id.tools_clearZero) {
//
//        } else if (item.getItemId() == R.id.sensors_button) {
//
//        }
////        switch (item.getItemId()) {
////            case R.id.back_button:
////                if (parentActivity.equals("MEASUREMENT")) {
////                    Intent intent = new Intent(this, ViewMeasurements.class);
////                    intent.putExtra(ViewMeasurements.EXTRA_DEVICE_NAME, mDeviceName);
////                    intent.putExtra(ViewMeasurements.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
////                    intent.putExtra(ViewMeasurements.EXTRA_DEVICE_CONNECTION_STATUS, mConnectionStatus);
////                    intent.putExtra(ViewMeasurements.EXTRA_NEXT_DEPTH, mNextDepth);
////                    intent.putExtra(ViewMeasurements.EXTRA_PREV_DEPTH, mPrevDepth);
////                    startActivity(intent);
////                } else {
////                    Intent intent = new Intent(this, ProbeDetails.class);
////                    intent.putExtra(ProbeDetails.EXTRA_DEVICE_NAME, mDeviceName);
////                    intent.putExtra(ProbeDetails.EXTRA_DEVICE_DEVICE_ADDRESS, mDeviceAddress);
////                    intent.putExtra(ProbeDetails.EXTRA_DEVICE_CONNECTION_STATUS, mConnectionStatus);
////                    startActivity(intent);
////                }
////                break;
////            case R.id.tools_button: //TODO
////                break;
////            case R.id.tools_setZero: //TODO
////                break;
////            case R.id.tools_clearZero: //TODO
////                break;
////            case R.id.sensors_button: //TODO
////                break;
////        }
//        return true;
//    }
//
//    private static IntentFilter makeGattUpdateIntentFilter() {
//        final IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
//        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
//        return intentFilter;
//    }
//
//    /**
//     * Write a shot request to the probe to get real time data upon button press
//     * @param view
//     */
//    public void shotRequest(View view) {
//        //Probe Mode
//        boolean status = mBluetoothLeService.writeToProbeMode(02);
//        Log.e(TAG, "STATUS OF WRITE: " + status);
//        try {
//            Thread.sleep(1000);
//        } catch (Exception e) {
//            Log.e(TAG, "Could not sleep" + e);
//        }
//        if (status) {
//            dataToBeRead = 0;
//            displayGattServices(mBluetoothLeService.getSupportedGattServices());
//
//            if (currentOp == null) {
//                Log.e(TAG, "2nd");
//                dataToBeRead = 0;
//                displayGattServices(mBluetoothLeService.getSupportedGattServices());
//            }
//        } else {
//            updateConnectionState("Disconnected");
//        }
//        new CountDownTimer(700, 1) {
//            public void onTick(long millisUntilFinished) {}
//            public void onFinish() {
//                dataToBeRead = 0;
//                displayGattServices(mBluetoothLeService.getSupportedGattServices());
//            }
//        }.start();
//    }
//
//    /**
//     * Write a shot request to the probe manually
//     */
//    public void shotRequest() {
//        boolean status = mBluetoothLeService.writeToProbeMode(02);
//        Log.e(TAG, "STATUS OF WRITE: " + status);
//        try {
//            Thread.sleep(1000);
//        } catch (Exception e) {
//            Log.e(TAG, "Could not sleep" + e);
//        }
//        if (status) {
//            dataToBeRead = 0;
//            displayGattServices(mBluetoothLeService.getSupportedGattServices());
//
//            if (currentOp == null) {
//                Log.e(TAG, "2nd");
//                dataToBeRead = 0;
//                displayGattServices(mBluetoothLeService.getSupportedGattServices());
//            }
//        } else {
//            updateConnectionState("Disconnected");
//        }
//        new CountDownTimer(700, 1) {
//            public void onTick(long millisUntilFinished) {}
//            public void onFinish() {
//                dataToBeRead = 0;
//                displayGattServices(mBluetoothLeService.getSupportedGattServices());
//            }
//        }.start();
//    }
//
//    /**
//     * Reset the queue upon button press
//     * @param view
//     */
//    public void resetQueueClick(View view) {
//        resetQueue();
//        Log.d(TAG, "Resetting the queue");
//    }
//
//    public static String removeLastChar(String s) {
//        return (s == null || s.length() == 0)
//                ? null
//                : (s.substring(0, s.length() - 1));
//    }
//}

import static com.work.libtest.Operation.OPERATION_NOTIFY;
import static com.work.libtest.Operation.OPERATION_READ;
import static com.work.libtest.Operation.OPERATION_WRITE;
//import static com.work.libtest.TakeMeasurements.shotWriteType;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
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
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

import com.work.libtest.CalibrationHelper;

public class OrientationActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE_NAME = "Device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "Device_address";
    public static final String EXTRA_DEVICE_CONNECTION_STATUS = "Connection_status";
    public static final String EXTRA_DEVCE_DEVICE_ADDRESS = "not relevant";
    public static final String EXTRA_PARENT_ACTIVITY = "null";
    public static final String EXTRA_MEASUREMENT_DATA = "null";
    public static final String EXTRA_NEXT_DEPTH = "null";
    public static final String EXTRA_PREV_DEPTH = "null";

    public static final String EXTRA_DEVICE_SERIAL_NUMBER = "Serial_number";
    public static final String EXTRA_DEVICE_VERSION = "Device_firmware_version";

    private static final int REQ_CODE_ENABLE_BT = 1;
    private static final int REQ_CODE_SCAN_ACTIVITY = 2;
    private static final int REQ_CODE_ACCESS_LOC1 = 3;
    private static final int REQ_CODE_ACCESS_LOC2 = 4;
    private static final long CONNECT_TIMEOUT = 10000;

    private ProgressBar progressBar;
    private BleService bleService;
    private ByteArrayOutputStream transparentUartData = new ByteArrayOutputStream();
    private ShowAlertDialogs showAlert;
    private Handler connectTimeoutHandler;
    private String bleDeviceName, bleDeviceAddress;

    private boolean haveSuitableProbeConnected = false;

    private enum StateConnection {DISCONNECTED, CONNECTING, DISCOVERING, CONNECTED, DISCONNECTING}
    private StateConnection stateConnection;
    private enum StateApp {STARTING_SERVICE, REQUEST_PERMISSION, ENABLING_BLUETOOTH, RUNNING}
    private StateApp stateApp;

    public static final String EXTRA_SAVED_NUM = "Number of data points saved";

    private String mDeviceName;
    private String mDeviceAddress;
    private String mConnectionStatus;

    String number;
    String parentActivity;

    boolean writtenCalibrationIndex = false;

    private String TAG = "Sensor Activity";

    private TextView probeNumber;
    private TextView connectionStatus;
    private ImageView connectionStatusImage;

    private TextView textDeviceNameAndAddress;                                                      //To show device and status information on the screen
    private TextView textDeviceStatus;                                                      //To show device and status information on the screen
    private TextView labelFirmwareVersion;
    private TextView textFirmwareVersion;                                                      //To show device and status information on the screen
    private TextView labelCalibratedDate;                                                      //To show device and status information on the screen
    private TextView textCalibratedDate;                                                      //To show device and status information on the screen
    private Button buttonReconnect;

    private boolean forceUncalibratedMode = false;  // user can tap CalibrationDate label and disable calibration

    private TextView labelAcc, labelMag, labelAverage;
    private TextView textAccX, textAccY, textAccZ, textAccMag;
    private TextView textMagX, textMagY, textMagZ, textMagMag;
    private TextView labelRoll, labelDip, labelAz;
    private TextView textRoll, textDip, textAz, textAzErr;
    private TextView probeRoll, probeDip, probeAz;
//    private TextView textRoll360;
    private TextView textTempUc;
    private TextView labelAveraging;
    private EditText editBoxAverage;
    private Button buttonLive, buttonRecord;
    private Switch switchRecord;

    private TextView labelDebug1, labelDebug2, labelInterval;
    private EditText editBoxDebug1, editBoxDebug2;
    private TextView textInterval;

    private TextView labelAcceptDip, labelAcceptAz;
    private TextView textAlignCount, textAlignAvgDip, textAlignAvgAz, textAlignCountdown;
    private Button buttonAlignStart, buttonAlignTakeReading, buttonAlignUpdate;
    private TextView textAcceptLocation, textAcceptComment;
    private TextView textAcceptDip, textAcceptAz;
    private Button buttonAcceptStart, buttonAcceptTakeReading;
    private TextView textAcceptResultAz, textAcceptResultDip;

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
    private static ArrayList<Survey> surveys = new ArrayList<Survey>();
    private static ArrayList<ProbeData> probeData = new ArrayList<ProbeData>();

    private static ArrayList<Double> accXData = new ArrayList<>();
    private static ArrayList<Double> accYData = new ArrayList<>();
    private static ArrayList<Double> accZData = new ArrayList<>();

    private static ArrayList<Double> magXData = new ArrayList<>();
    private static ArrayList<Double> magYData = new ArrayList<>();
    private static ArrayList<Double> magZData = new ArrayList<>();

    int recordCount = 0;
    int numSaved = 0;
    private static int acceptSamplesPerReading = 5; //10; //5;   // currenlt overriden by average edit box
    private double newAcceptReadingDipSum = 0;  // actually sum of all the dip readings
    private double newAcceptReadingAzSum = 0;  // actually sum of all the az readings
    private int newAcceptCountRemaining = 0;
    private double newAlignReadingDipSum = 0;  // actually sum of all the dip readings
    private double newAlignReadingAzSum = 0;  // actually sum of all the dip readings
    private int newAlignCountRemaining = 0;  // when non zero triggers actions in processNewReading
    private static int alignSamplesPerReading = 5; //10; //5;  // now explicitly set to 10 in AlignStartButton OnClick handler
    private int alignCount = 0;
    private double alignDipTotal = 0;
    private double alignAzTotal = 0;private boolean acceptShowLiveError = false;
    private double  acceptCurrentLiveDipError = 0;
    private double  acceptCurrentLiveAzError = 0;
    private double  acceptCurrentLiveRoll360Error = 0;
    private static final int acceptAcceptableLiveError = 4;
    private double acceptDip[] = new double[12];
    private double acceptAz[] = new double[12];
    private double acceptRoll[] = new double[12];
    private double acceptRmsDip = 0;
    private double acceptRmsAz = 0;

    private String acceptLocation = "None";
    private boolean acceptIdealModified = false;  // to indicate if align has updated these values (they are no longer the normal values
    private double acceptIdeal50Az = 0;
    private double acceptIdeal50Dip = 0;
    private double acceptIdeal60Az = 0;
    private double acceptIdeal60Dip = 0;
    private double acceptIdeal30Az = 0;
    private int acceptState = 0;
    private double acceptIdeal30Dip = 0;
    int acceptTestPointDip[]  = { -50, -50, -50, -50, -60, -60, -60, -60, -30, -30, -30, -30 };
    int acceptTestPointRoll[] = { 355,  80, 170, 260, 355,  80, 170, 260, 355,  80, 170, 260 };

    int probePosition;

    /******************************************************************************************
     * Methods for handling the life cycle of the activity
     */

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_orientation);
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            showAlert = new ShowAlertDialogs(this);


            Log.i(TAG, "==========SENSOR ACTIVITY ON CREATE==============");
            stateConnection = StateConnection.DISCONNECTED;
            stateApp = StateApp.STARTING_SERVICE;
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { //Check whether we have location permission, required to scan
                stateApp = StateApp.REQUEST_PERMISSION;                                                 //Are requesting Location permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_CODE_ACCESS_LOC1); //Request fine location permission
            }
            if (stateApp == StateApp.STARTING_SERVICE) {                                                //Only start BleService if we already have location permission
                Intent bleServiceIntent = new Intent(this, BleService.class);             //Create Intent to start the BleService
                this.bindService(bleServiceIntent, bleServiceConnection, BIND_AUTO_CREATE);             //Create and bind the new service with bleServiceConnection object that handles service connect and disconnect
            }
            textRoll = findViewById(R.id.probe_rollValue);
            textDip = findViewById(R.id.probe_dipValue);
            textAz = findViewById(R.id.probe_azimuthValue);

            probeRoll = findViewById(R.id.measurement_rollValue);
            probeDip = findViewById(R.id.measurement_dipValue);
            probeAz = findViewById(R.id.measurement_azimuthValue);


            final Intent intent = getIntent();
            parentActivity = intent.getStringExtra(EXTRA_PARENT_ACTIVITY);
            Log.i(TAG, "Parent activity of orientation activity is: " + parentActivity);
            //check if the parent activity is the probe details page or the view measurement page
            //if the activity has come from the view measurement activity then we have to display the data that was clicked on when opening the activity
            //TODO - @ANNA - add after view measurement page is less mucky
            if (parentActivity != null) {
                if (parentActivity.equals("MEASUREMENT")) {
                    probePosition = Integer.parseInt(intent.getStringExtra(EXTRA_MEASUREMENT_DATA));
                    probeRoll.setText(String.valueOf(TakeMeasurements.savedProbeData.get(probePosition)[7]));
                    probeDip.setText(String.valueOf(TakeMeasurements.savedProbeData.get(probePosition)[8]));
                    probeAz.setText(String.valueOf(TakeMeasurements.savedProbeData.get(probePosition)[9]));
                } else {

                }
            }

            connectTimeoutHandler = new Handler(Looper.getMainLooper());

            buttonLive = (Button) findViewById(R.id.get_data);
            buttonLive.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Log.i(TAG, "PJH - processing Live Data button press");
                    if (buttonLive.getText() == "PAUSE"){
                        bleService.setProbeIdle();
                    }
                    else {
                        bleService.setProbeMode(2);
                    }
                }
            });
            initializeDisplay();
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown in onCreate in Orientation Activity: " + e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            registerReceiver(bleServiceReceiver, bleServiceIntentFilter());                         //Register receiver to handles events fired by the BleService
            if (bleService != null && !bleService.isBluetoothRadioEnabled()) {                      //Check if Bluetooth radio was turned off while app was paused
                if (stateApp == StateApp.RUNNING) {                                                 //Check that app is running, to make sure service is connected
                    stateApp = StateApp.ENABLING_BLUETOOTH;                                         //Are going to request user to turn on Bluetooth
                    stateConnection = StateConnection.DISCONNECTED;                                 //Must be disconnected if Bluetooth is off
                    startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQ_CODE_ENABLE_BT); //Start the activity to ask the user to grant permission to enable Bluetooth
                    Log.i(TAG, "Requesting user to enable Bluetooth radio");
                }
            }
            if ((bleDeviceName != null) && (bleDeviceAddress != null) && bleService.isCalibrated()) {                                                //See if there is a device name
                // attempt a reconnection
                stateConnection = StateConnection.CONNECTING;                               //Got an address so we are going to start connecting
                connectWithAddress(bleDeviceAddress);                                       //Initiate a connection
            }

            updateConnectionState();                                                                //Update the screen and menus
        } catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();                                                                            //Call superclass (AppCompatActivity) onPause method
        try {
            unregisterReceiver(bleServiceReceiver);                                                     //Unregister receiver that was registered in onResume()
        } catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }

    @Override
    public void onStop() {
        super.onStop();                                                                             //Call superclass (AppCompatActivity) onStop method
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();                                                                          //Call superclass (AppCompatActivity) onDestroy method
        bleService.setProbeIdle();

        if (stateApp != StateApp.REQUEST_PERMISSION) {                                              //See if we got past the permission request
            unbindService(bleServiceConnection);                                                    //Unbind from the service handling Bluetooth
        }
    }

    /**********************************************************************************************/

    /********************************************************************************************
     * Methods for handling menu creation and operation
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_orientation, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /**
         * Fun fact, you actually have to do it like this you cant use a switch case
         * hence this is not bad programming practice
         */
        if (item.getItemId() == R.id.back_button) {
            if (parentActivity.equals("Measurement")) { //this should really be a global variable or smth
                Intent intent = new Intent(this, ViewMeasurements.class);
                intent.putExtra(ViewMeasurements.EXTRA_DEVICE_NAME, mDeviceName);
                intent.putExtra(ViewMeasurements.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
                intent.putExtra(ViewMeasurements.EXTRA_DEVICE_CONNECTION_STATUS, mConnectionStatus);
//                intent.putExtra(ViewMeasurements.EXTRA_NEXT_DEPTH, mNextDepth);
//                intent.putExtra(ViewMeasurements.EXTRA_PREV_DEPTH, mPrevDepth);
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, ProbeDetails.class);
                intent.putExtra(ProbeDetails.EXTRA_DEVICE_NAME, mDeviceName);
                intent.putExtra(ProbeDetails.EXTRA_DEVICE_DEVICE_ADDRESS, mDeviceAddress);
                intent.putExtra(ProbeDetails.EXTRA_DEVICE_CONNECTION_STATUS, mConnectionStatus);
                startActivity(intent);
            }
        } else if (item.getItemId() == R.id.tools_button) {

        } else if (item.getItemId() == R.id.tools_setZero) {

        } else if (item.getItemId() == R.id.tools_clearZero) {

        } else if (item.getItemId() == R.id.sensors_button) {

        }
        return true;
    }
    /**********************************************************************************************/

    /**********************************************************************************************
     * Callback methods for handling Service connection events and Activity result events
     */

    private final ServiceConnection bleServiceConnection = new ServiceConnection() {                //Create new ServiceConnection interface to handle connection and disconnection

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {              //Service connects
            try {
                Log.i(TAG, "BleService connected");
                BleService.LocalBinder binder = (BleService.LocalBinder) service;                   //Get the Binder for the Service
                bleService = binder.getService();                                                   //Get a link to the Service from the Binder
                if (bleService.isBluetoothRadioEnabled()) {                                         //See if the Bluetooth radio is on
                    stateApp = StateApp.RUNNING;                                                    //Service is running and Bluetooth is enabled, app is now fully operational
                }
                else {                                                                              //Radio needs to be enabled
                    stateApp = StateApp.ENABLING_BLUETOOTH;                                         //Are requesting Bluetooth to be turned on
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);     //Create an Intent asking the user to grant permission to enable Bluetooth
                    startActivityForResult(enableBtIntent, REQ_CODE_ENABLE_BT);                     //Send the Intent to start the Activity that will return a result based on user input
                    Log.i(TAG, "Requesting user to turn on Bluetooth");
                }
            } catch (Exception e) {
                Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {                            //BleService disconnects - should never happen
            Log.i(TAG, "BleService disconnected");
            bleService = null;                                                                      //Not bound to BleService
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);                                    //Pass the activity result up to the parent method
        switch (requestCode) {                                                                      //See which Activity returned the result
            case REQ_CODE_ENABLE_BT: {
                if (resultCode == Activity.RESULT_OK) {                                             //User chose to enable Bluetooth
                    stateApp = StateApp.RUNNING;                                                    //Service is running and Bluetooth is enabled, app is now fully operational
                } else {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);     //User chose not to enable Bluetooth so create an Intent to ask again
                    startActivityForResult(enableBtIntent, REQ_CODE_ENABLE_BT);                     //Send the Intent to start the activity that will return a result based on user input
                    Log.i(TAG, "Requesting user to turn on Bluetooth again");
                }
                break;
            }
            case REQ_CODE_SCAN_ACTIVITY: {
                showAlert.dismiss();
                if (resultCode == Activity.RESULT_OK) {                                             //User chose a Bluetooth device to connect
                    stateApp = StateApp.RUNNING;                                                    //Service is running and Bluetooth is enabled, app is fully operational
                    bleDeviceAddress = intent.getStringExtra(BleScanActivity.EXTRA_SCAN_ADDRESS);   //Get the address of the BLE device selected in the BleScanActivity
                    bleDeviceName = intent.getStringExtra(BleScanActivity.EXTRA_SCAN_NAME);         //Get the name of the BLE device selected in the BleScanActivity

                    if (bleDeviceName != null) {                                                //See if there is a device name
//                        textDeviceNameAndAddress.setText(bleDeviceName);                        //Display the name
                    } else {
//                        textDeviceNameAndAddress.setText(R.string.unknown_device);                     //or display "Unknown Device"
                    }
                    if (bleDeviceAddress != null) {                                             //See if there is an address
//                        textDeviceNameAndAddress.append(" - " + bleDeviceAddress);              //Display the address
                    }

                    if (bleDeviceAddress == null) {                                                 //Check whether we were given a device address
                        stateConnection = StateConnection.DISCONNECTED;                             //No device address so not connected and not going to connect
                    } else {
                        stateConnection = StateConnection.CONNECTING;                               //Got an address so we are going to start connecting
                        connectWithAddress(bleDeviceAddress);                                       //Initiate a connection
                    }
                } else {                                                                            //Did not get a valid result from the BleScanActivity
                    stateConnection = StateConnection.DISCONNECTED;                                 //No result so not connected and not going to connect
                }
                updateConnectionState();                                                            //Update the connection state on the screen and menus
                break;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.i(TAG, "PJH - onRequestPermissionsResult");

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {                                 //See if location permission was granted
            Log.i(TAG, "PJH - Location permission granted");
            stateApp = StateApp.STARTING_SERVICE;                                                   //Are going to start the BleService service
            Intent bleServiceIntent = new Intent(this, BleService.class);             //Create Intent to start the BleService
            this.bindService(bleServiceIntent, bleServiceConnection, BIND_AUTO_CREATE);             //Create and bind the new service to bleServiceConnection object that handles service connect and disconnect
        }
        else if (requestCode == REQ_CODE_ACCESS_LOC1) {                                             //Not granted so see if first refusal and need to ask again
            showAlert.showLocationPermissionDialog(new Runnable() {                                 //Show the AlertDialog that scan cannot be performed without permission
                @TargetApi(Build.VERSION_CODES.M)
                @Override
                public void run() {                                                                 //Runnable to execute when Continue button pressed
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_CODE_ACCESS_LOC2); //Ask for location permission again
                }
            });
        }
        else {
            Log.i(TAG, "PJH - Location permission NOT granted");
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);               //Create Intent to open the app settings page
            Uri uri = Uri.fromParts("package", getPackageName(), null);            //Identify the package for the settings
            intent.setData(uri);                                                                    //Add the package to the Intent
            startActivity(intent);                                                                  //Start the settings activity
        }
    }

    /******************************************************************************************************************
     * Methods for handling Intents.
     */

    private static IntentFilter bleServiceIntentFilter() {                                          //Method to create and return an IntentFilter
        final IntentFilter intentFilter = new IntentFilter();                                       //Create a new IntentFilter
        intentFilter.addAction(BleService.ACTION_BLE_CONNECTED);                                    //Add filter for receiving an Intent from BleService announcing a new connection
        intentFilter.addAction(BleService.ACTION_BLE_DISCONNECTED);                                 //Add filter for receiving an Intent from BleService announcing a disconnection
        intentFilter.addAction(BleService.ACTION_BLE_DISCOVERY_DONE);                               //Add filter for receiving an Intent from BleService announcing a service discovery
        intentFilter.addAction(BleService.ACTION_BLE_DISCOVERY_FAILED);                             //Add filter for receiving an Intent from BleService announcing failure of service discovery
        intentFilter.addAction(BleService.ACTION_BLE_NEW_DATA_RECEIVED);                            //Add filter for receiving an Intent from BleService announcing new data received
        intentFilter.addAction(BleService.ACTION_BLE_CONFIG_READY);                            //Add filter for receiving an Intent from BleService announcing new data received
        intentFilter.addAction(BleService.ACTION_BLE_FETCH_CAL);  // PJH - just to update display
        return intentFilter;                                                                        //Return the new IntentFilter
    }

    private final BroadcastReceiver bleServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {                                     //Intent received
            final String action = intent.getAction();                                               //Get the action String from the Intent
            switch (action) {                                                                       //See which action was in the Intent
                case BleService.ACTION_BLE_CONNECTED: {                                             //Have connected to BLE device
                    Log.d(TAG, "Received Intent  ACTION_BLE_CONNECTED");
                    initializeDisplay();                                                            //Clear the temperature and accelerometer text and graphs
                    transparentUartData.reset();   // PJH remove                                                 //Also clear any buffered incoming data
                    stateConnection = StateConnection.DISCOVERING;                                  //BleService automatically starts service discovery after connecting
                    updateConnectionState();                                                        //Update the screen and menus
                    break;
                }
                case BleService.ACTION_BLE_DISCONNECTED: {                                          //Have disconnected from BLE device
                    Log.d(TAG, "Received Intent ACTION_BLE_DISCONNECTED");
                    initializeDisplay();                                                            //Clear the temperature and accelerometer text and graphs
                    transparentUartData.reset();                                                    //Also clear any buffered incoming data
                    stateConnection = StateConnection.DISCONNECTED;                                 //Are disconnected
                    if ((bleDeviceName != null) && (bleDeviceAddress != null) && bleService.isCalibrated()) {                                                //See if there is a device name
                        // attempt a reconnection
                        stateConnection = StateConnection.CONNECTING;                               //Got an address so we are going to start connecting
                        connectWithAddress(bleDeviceAddress);                                       //Initiate a connection
                    }
                    updateConnectionState();                                                        //Update the screen and menus
                    break;
                }
                case BleService.ACTION_BLE_DISCOVERY_DONE: {                                        //Have completed service discovery
                    Log.d(TAG, "PJH - Received Intent  ACTION_BLE_DISCOVERY_DONE");
                    connectTimeoutHandler.removeCallbacks(abandonConnectionAttempt);                //Stop the connection timeout handler from calling the runnable to stop the connection attempt
                    stateConnection = StateConnection.CONNECTED;                                    //Were already connected but showing discovering, not connected
                    updateConnectionState();                                                        //Update the screen and menus
                    Log.i(TAG, "PJH - about to request Ezy config");
                    bleService.requestEzyConfig();                                                         //Ask the BleService to connect to start interrogating the device for its configuration
                    break;
                }
                case BleService.ACTION_BLE_DISCOVERY_FAILED: {                                      //Service discovery failed to find the right service and characteristics
                    Log.d(TAG, "Received Intent  ACTION_BLE_DISCOVERY_FAILED");
                    stateConnection = StateConnection.DISCONNECTING;                                //Were already connected but showing discovering, so are now disconnecting
                    connectTimeoutHandler.removeCallbacks(abandonConnectionAttempt);                //Stop the connection timeout handler from calling the runnable to stop the connection attempt
                    bleService.disconnectBle();                                                     //Ask the BleService to disconnect from the Bluetooth device
                    updateConnectionState();                                                        //Update the screen and menus
                    showAlert.showFaultyDeviceDialog(new Runnable() {                               //Show the AlertDialog for a faulty device
                        @Override
                        public void run() {                                                         //Runnable to execute if OK button pressed
                            startBleScanActivity();                                                 //Launch the BleScanActivity to scan for BLE devices
                        }
                    });
                    break;
                }
                case BleService.ACTION_BLE_CONFIG_READY: {                                             //Have read all the Ezy parameters from BLE device
                    bleService.parseBinaryCalibration();
                    bleService.setNotifications(true);
                    haveSuitableProbeConnected = true;
                    break;
                }
                case BleService.ACTION_BLE_FETCH_CAL: {                                        //Have completed service discovery
                    break;
                }
                case BleService.ACTION_BLE_NEW_DATA_RECEIVED: {                                     //Have received data (characteristic notification) from BLE device
                    processNewReading();
                    break;
                }
                default: {
                    Log.w(TAG, "Received Intent with invalid action: " + action);
                }
            }
        }
    };

    /******************************************************************************************************************
     * Method for processing incoming data and updating the display
     */

    private void initializeDisplay() {
        try {
            textRoll.setText("Roll Value");
            textDip.setText("Dip Value");
            textAz.setText("Azimuth Value");
        } catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }

    public static Integer toInteger(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return (int) Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }


    // a new shot has been received, so retrieve and display it
    private void processNewReading() {
        try {
            int count = 1;
            double newVal[] = bleService.getLatestBoreshot(count);

            textRoll.setText(String.format("%7.4f", newVal[7]));
            textDip.setText(String.format("%7.4f", newVal[8]));
            textAz.setText(String.format("%7.4f", newVal[9]));

        } catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }

    /******************************************************************************************************************
     * Methods for scanning, connecting, and showing event driven dialogs
     */

    private void startBleScanActivity() {
        try {
            bleService.invalidateCalibration();  // to force a full connect and reread of the calibration data
            if (stateApp == StateApp.RUNNING) {                                                     //Only do a scan if we got through startup (permission granted, service started, Bluetooth enabled)
                stateConnection = StateConnection.DISCONNECTING;                                    //Are disconnecting prior to doing a scan
                haveSuitableProbeConnected = false;
                bleService.disconnectBle();                                                         //Disconnect an existing Bluetooth connection or cancel a connection attempt
                final Intent bleScanActivityIntent = new Intent(OrientationActivity.this, BleScanActivity.class); //Create Intent to start the BleScanActivity
                startActivityForResult(bleScanActivityIntent, REQ_CODE_SCAN_ACTIVITY);              //Start the BleScanActivity
            }
        } catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Attempt to connect to a Bluetooth device given its address and time out after CONNECT_TIMEOUT milliseconds
    private void connectWithAddress(String address) {
        try {
            updateConnectionState();                                                                //Update the screen and menus (stateConnection is either CONNECTING or AUTO_CONNECT
            connectTimeoutHandler.postDelayed(abandonConnectionAttempt, CONNECT_TIMEOUT);           //Start a delayed runnable to time out if connection does not occur
            bleService.connectBle(address);                                                         //Ask the BleService to connect to the device
        } catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Runnable used by the connectTimeoutHandler to stop the connection attempt
    private Runnable abandonConnectionAttempt = new Runnable() {
        @Override
        public void run() {
            try {
                if (stateConnection == StateConnection.CONNECTING) {                                //See if still trying to connect
                    stateConnection = StateConnection.DISCONNECTING;                                //Are now disconnecting
                    bleService.disconnectBle();                                                     //Stop the Bluetooth connection attempt in progress
                    updateConnectionState();                                                        //Update the screen and menus
                    showAlert.showFailedToConnectDialog(new Runnable() {                            //Show the AlertDialog for a connection attempt that failed
                        @Override
                        public void run() {                                                         //Runnable to execute if OK button pressed
                            startBleScanActivity();                                                 //Launch the BleScanActivity to scan for BLE devices
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
            }
        }
    };

    /******************************************************************************************************************
     * Methods for updating connection state on the screen
     */

    // ----------------------------------------------------------------------------------------------------------------
    // Update the text showing what Bluetooth device is connected, connecting, discovering, disconnecting, or not connected
    private void updateConnectionState() {
        runOnUiThread(new Runnable() {                                                              //Always do display updates on UI thread
            @Override
            public void run() {
                switch (stateConnection) {
                    case CONNECTING: {
//                        textDeviceStatus.setText(R.string.waiting_to_connect);                             //Show "Connecting"
//                        progressBar.setVisibility(ProgressBar.VISIBLE);                             //Show the circular progress bar
                        break;
                    }
                    case CONNECTED: {
//                        textDeviceStatus.setText(R.string.interrogating_configuration);

//                        progressBar.setVisibility(ProgressBar.INVISIBLE);                           //Hide the circular progress bar
                        break;
                    }
                    case DISCOVERING: {
                        //textDeviceStatus.setText(R.string.discovering);                            //Show "Discovering"
//                        textDeviceStatus.setText(R.string.interrogating_features);                            //Show "Discovering"
//                        progressBar.setVisibility(ProgressBar.VISIBLE);                             //Show the circular progress bar
                        break;
                    }
                    case DISCONNECTING: {
//                        textDeviceStatus.setText(R.string.disconnecting);                          //Show "Disconnectiong"
//                        progressBar.setVisibility(ProgressBar.INVISIBLE);                           //Hide the circular progress bar
                        break;
                    }
                    case DISCONNECTED:
                    default: {
                        stateConnection = StateConnection.DISCONNECTED;                             //Default, in case state is unknown
//                        textDeviceStatus.setText(R.string.not_connected);                          //Show "Not Connected"
//                        progressBar.setVisibility(ProgressBar.INVISIBLE);                           //Hide the circular progress bar
                        break;
                    }
                }
                invalidateOptionsMenu();                                                            //Update the menu to reflect the connection state stateConnection
            }
        });
    }


    // alignInit is called when Align Start button is pressed,
    // so it also has to handle the alternate abort functionality
    private void initAlign(View v, int count) {
        if ((haveSuitableProbeConnected) && (acceptState == 0)) {
            Log.i(TAG, "PJH - processing Align Start Data button press");
//            if (buttonAlignStart.getText() == "START"){
            // PJH TODO - need to stop LIVE DATA or accept
            alignCount = 0;
            alignDipTotal = 0;
            alignAzTotal = 0;
//            textAlignCount.setText("0");
//            textAlignAvgDip.setText("0");
//            textAlignAvgAz.setText("0");

            alignSamplesPerReading = count;

//            buttonAlignStart.setText("ABORT");
            bleService.setProbeMode(2); //PROBE_MODE_ROLLING_SHOTS);  // TODO - this is wrong for corecam

            if (switchRecord.isChecked()) {
                // sensor data recording is enabled
                bleService.initRecordingSensorData();
            }
//            }
//            else {    // must be abort
//                buttonAlignStart.setText("START");
//                bleService.setProbeIdle();
//                textAlignCountdown.setText("");
//            }
        }
    }

}