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

public class OrientationActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE_NAME = "Device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "Device_address";
    public static final String EXTRA_DEVICE_CONNECTION_STATUS = "Connection_status";

    public static final String EXTRA_DEVICE_SERIAL_NUMBER = "Serial_number";
    public static final String EXTRA_DEVCE_DEVICE_ADDRESS = "Device_gathered_adresses";
    public static final String EXTRA_DEVICE_VERSION = "Device_firmare_version";

    public static final String EXTRA_PARENT_ACTIVITY = "Parent_activity";
    public static final String EXTRA_MEASUREMENT_ALL_DATA = "All_measurement_data";
    public static final String EXTRA_MEASUREMENT_DATA = "Relevent_measurement";

    public static final String EXTRA_NEXT_DEPTH = "next depth";
    public static final String EXTRA_PREV_DEPTH = "prev depth";

    public String mNextDepth;
    public String mPrevDepth;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    String parentActivity;
    String measurementData;
//    measurementData

    private boolean isCalibrated = false;
    int _probeMode = 0;
    int _calibrationIndex = 0;
    String _calibrationData = "";
    private int dataToBeRead = 0;

    private BluetoothGattCharacteristic CORE_SHOT_CHARACTERISTIC;
    private BluetoothGattCharacteristic BORE_SHOT_CHARACTERISTIC;

    private String mDeviceName;
    private String mDeviceAddress;
    private String mConnectionStatus;

    private Menu menu;

    private String TAG = "Orientation Activity";

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<>();
    private BluetoothLeService mBluetoothLeService;
    private Handler handler;

    private TextView rollTitle;
    private TextView dipTitle;
    private TextView azimuthTitle;

    private double mRoll = 0;
    private double mDip = 0;
    private double mAzimuth = 0;

    private TextView rollValue;
    private TextView dipValue;
    private TextView azimuthValue;

    private TextView measurementRollValue;
    private TextView measurementDipValue;
    private TextView measurementAzimuthValue;

    private int dRecordNumber = 0;

    private TextView connectionStatus;
    private ImageView connectionImg;

    private double coreAX = 0;
    private double coreAY = 0;
    private double coreAZ = 0;

    private String coreAXBinary, coreAYBinary, coreAZBinary, magXBinary, magYBinary, magZBinary;

    private double magX = 0;
    private double magY = 0;
    private double magZ = 0;

    private int shotFormat;

    private Queue<Operation> operations = new LinkedList<>();
    private Operation currentOp;


    String highByte, lowByte;

    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 0;
        public static final int MESSAGE_TOAST = 0;
    }

    /**
     * request an operation to be performed
     * @param operation
     */
    public synchronized void request(Operation operation) {
        Log.d(TAG, "Requestion Operation: " + operation.toString());
        operations.add(operation);
        if (currentOp == null) {
            currentOp = operations.poll();
            performOperation();
        } else {
            Log.e(TAG, "Current operation is null");
        }
    }

    /**
     * runs whenever an operation is completed
     */
    public synchronized void operationCompleted() {
        Log.d(TAG, "Operation complete, moving to next one");
        currentOp = null;
        if (operations.peek() != null) {
            currentOp = operations.poll();
            performOperation();
        } else {
            Log.d(TAG, "Queue empty");
        }
    }

    /**
     * code to perform the operation depending on its action (write,read,notify)
     */
    public void performOperation() {
        Log.d(TAG, "Performing operation");
        if (currentOp != null) {
            if (currentOp.getService() != null && currentOp.getCharacteristic() != null && currentOp.getAction() != 0) {
//                Log.e(TAG, "Current Op Service: " + currentOp.getService() + )
                switch (currentOp.getAction()) {
                    case OPERATION_WRITE:
//                        Log.e(TAG, "Writing to characteristic with service: " + currentOp.getService().getUuid().toString() + " and characteristic: " + currentOp.getCharacteristic().getUuid().toString());
//                        try {
//                            boolean write = mBluetoothLeService.writeToCalibrationIndex();
//                            if (write) {
//                                Log.e(TAG, "Written successfully");
//                                operationCompleted();
//                            }
//                        } catch (Exception e) {
//                            Log.e(TAG, "Exception thrown; " + e);
//                        }
                        break;
                    case OPERATION_READ:
                        try {
//                            Log.d(TAG, "Reading characteristic with service: " + currentOp.getService().getUuid().toString() + " and characteristic: " + currentOp.getCharacteristic().getUuid().toString());
                            mBluetoothLeService.readCharacteristic(currentOp.getCharacteristic());
                        } catch (Exception e) {
                            Log.e(TAG, "Cannot Read charateristic: " + e);
                        }
                        break;
                    case OPERATION_NOTIFY:
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

    /**
     * Clear queue to stop all future operations
     */
    public void resetQueue() {
        operations.clear();
        currentOp = null;
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        /**
         *
         * @param name The concrete component name of the service that has
         * been connected.
         *
         * @param service The IBinder of the Service's communication channel,
         * which you can now make calls on.
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Enable to initialise bluetooth");
                finish();
            }
            mBluetoothLeService.connect(mDeviceAddress);
        }

        /**
         *
         * @param name The concrete component name of the service whose
         * connection has been lost.
         */
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        /**
         *
         * @param context The Context in which the receiver is running.
         * @param intent The Intent being received.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                updateConnectionState("Connected");
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                updateConnectionState("Disconnected");
                mBluetoothLeService.connect(mDeviceAddress);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                if (intent.getStringExtra(BluetoothLeService.CORE_SHOT) != null) {

                    String coreShotData = intent.getStringExtra(BluetoothLeService.CORE_SHOT);
                    String[] shotData = coreShotData.split(":", 20);
                    shotFormat = Integer.parseInt(shotData[0]);

                    switch (shotFormat) {
                        case 1:
                            //first format(12): Format(1), Record Number(2), Probe Temperature(2), AX(2), AY(2), AZ(2), Accelerometer Temperature(1)
                            for (int i = 0; i < shotData.length; i++) {
                                if (i == 5) { //AX 1
                                    coreAXBinary = "";
                                    coreAX = Double.parseDouble(shotData[i]);
                                    int num = Integer.parseInt(shotData[i]);
                                    if (num < 0) {
                                        num = 128 + (128 + num);
                                    }
                                    String binaryOutput = Integer.toBinaryString(num);
                                    if (Integer.toBinaryString(num).length() < 8) {
                                        for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                            binaryOutput = "0" + binaryOutput;
                                        }
                                    }
                                    coreAXBinary = binaryOutput;
                                } else if (i == 6) { //AX 2
                                    int num = Integer.parseInt(shotData[i]);
                                    if (num < 0) {
                                        num = 128 + (128 + num);
                                    }
                                    String binaryOutput = Integer.toBinaryString(num);
                                    if (Integer.toBinaryString(num).length() < 8) {
                                        for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                            binaryOutput = "0" + binaryOutput;
                                        }
                                    }
                                    coreAXBinary = coreAXBinary + binaryOutput;

                                    if (coreAXBinary.length() > 16) {
                                        //TODO Error
                                    } else {
                                        if (coreAXBinary.charAt(0) == '1') {
                                            coreAXBinary = coreAXBinary.substring(1);
                                            coreAX = Integer.parseInt(coreAXBinary, 2);
                                            coreAX = coreAX * -1;
                                        } else {
                                            coreAX = Integer.parseInt(coreAXBinary, 2);
                                        }
                                    }

                                } else if (i == 7) { //AY 1
                                    coreAYBinary = "";
                                    coreAY = Double.parseDouble(shotData[i]);
                                    int num = Integer.parseInt(shotData[i]);
                                    if (num < 0) {
                                        num = 128 + (128 + num);
                                    }
                                    String binaryOutput = Integer.toBinaryString(num);
                                    if (Integer.toBinaryString(num).length() < 8) {
                                        for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                            binaryOutput = "0" + binaryOutput;
                                        }
                                    }
                                    coreAYBinary = binaryOutput;
                                } else if (i == 8) { //AY 2
                                    int num = Integer.parseInt(shotData[i]);
                                    if (num < 0) {
                                        num  = 128 + (128 + num);
                                    }
                                    String binaryOutput = Integer.toBinaryString(num);
                                    if (Integer.toBinaryString(num).length() < 8) {
                                        for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                            binaryOutput = "0" + binaryOutput;
                                        }
                                    }
                                    coreAYBinary = coreAYBinary + binaryOutput;

                                    if (coreAYBinary.length() > 16) {
                                        //TODO ERROR
                                    } else {
                                        if (coreAYBinary.charAt(0) == '1') {
                                            coreAYBinary = coreAYBinary.substring(1);
                                            coreAY = Integer.parseInt(coreAYBinary, 2);
                                            coreAY = coreAY * -1;
                                        } else {
                                            coreAY = Integer.parseInt(coreAYBinary, 2);
                                        }
                                    }
                                } else if (i == 9) { //AZ 1
                                    coreAZBinary = "";
                                    coreAZ = Double.parseDouble(shotData[i]);
                                    int num = Integer.parseInt(shotData[i]);
                                    if (num < 0) {
                                        num = 128 + (128 + num);
                                    }
                                    String binaryOutput = Integer.toBinaryString(num);
                                    if (Integer.toBinaryString(num).length() < 8) {
                                        for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                            binaryOutput = "0" + binaryOutput;
                                        }
                                    }
                                    coreAZBinary = binaryOutput;
                                    Log.e(TAG, "CoreAZ 1: " + binaryOutput);
                                } else if (i == 10) { //AZ 2
                                    int num = Integer.parseInt(shotData[i]);
                                    if (num < 0) {
                                        num  = 128 + (128 + num);
                                    }
                                    String binaryOutput = Integer.toBinaryString(num);
                                    if (Integer.toBinaryString(num).length() < 8) {
                                        for (int k = 8; k > Integer.toBinaryString(num).length(); k--) {
                                            binaryOutput = "0" + binaryOutput;
                                        }
                                    }
                                    coreAZBinary = coreAZBinary + binaryOutput;
                                    Log.e(TAG, "CoreAZ 2: " + binaryOutput);
                                    if (coreAZBinary.length() > 16) {
                                        //TODO ERROR
                                    } else {
                                        if (coreAZBinary.charAt(0) == '1') {
                                            coreAZBinary = coreAZBinary.substring(1);
                                            coreAZ = Integer.parseInt(coreAZBinary, 2);
                                            coreAZ = coreAZ * -1;
                                        } else {
                                            coreAZ = Integer.parseInt(coreAZBinary, 2);
                                        }
                                    }
                                    try {
                                        calculateRoll(Double.valueOf(coreAY), Double.valueOf(coreAZ));
                                        calculateDip(Double.valueOf(coreAX), Double.valueOf(coreAY), Double.valueOf(coreAZ));
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error calling calculate roll and dip functions: " + e);
                                    }
                                }
                            }
                            break;
                        case 2:
                            Log.e(TAG, "Shot format is 2");
                            break;
                        case 3:
                            Log.e(TAG, "Shot format is 3");
                            break;
                        default:
                            Log.e(TAG, "ERROR Shot format not valid for core shot");
                            break;
                    }

                } else if (intent.getStringExtra(BluetoothLeService.BORE_SHOT) != null) {
                    String boreShotData = intent.getStringExtra(BluetoothLeService.BORE_SHOT);
                    String[] bore_shot_data = boreShotData.split(":", 20);
                    shotFormat = Integer.parseInt(bore_shot_data[0]);

                    switch (shotFormat) {
                        case 1:
                            Log.e(TAG, "Bore shot format = 1");
                            break;
                        case 2:
                            Log.e(TAG, "Bore shot format = 2");
                            break;
                        case 3:
                            //third format(20) : Format(1), Record number(2), Probe temperature(2), AX(2), AY(2), AZ(2), MX(3), MY(3), MZ(3)
                           Log.e(TAG, "Bore shot format = 3");
                           //REHAUL

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
//                                dev_record_number.setText(String.valueOf(dRecordNumber));
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

//                                orientation_temperature_data.setText(numberFormat.format(probeTemp));
                                //TODO make so if the accelerometer or magnetometer are actually passing temp data it displays the corresponding value
//                                accelerometer_temp_data.setText(numberFormat.format(probeTemp));
//                                magnetometer_temp_data.setText(numberFormat.format(probeTemp));
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

                            Log.e(TAG, "---------------AJP---------------");
                            Log.e(TAG, "roll" + cal_roll_radian + ",dip" + cal_dip_radian + "azimuth" + cal_az_radian);

                            //display orientation data
                            rollValue.setText(String.valueOf(Double.parseDouble(numberFormat.format(cal_roll_degree))));
                            dipValue.setText(numberFormat.format(cal_dip_degree));
                            azimuthValue.setText(numberFormat.format(cal_az_degree));

//                            accelerometer_x_data.setText(numberFormat.format(cx));
//                            accelerometer_y_data.setText(numberFormat.format(cy));
//                            accelerometer_z_data.setText(numberFormat.format(cz));
//
//                            magnetometer_x_data.setText(numberFormat.format(cmx));
//                            magnetometer_y_data.setText(numberFormat.format(cmy));
//                            magnetometer_z_data.setText(numberFormat.format(cmz));

                            break;
                        default:
                            Log.e(TAG, "Bore shot format not recognised");
                            break;
                    }
                } else if (intent.getStringExtra(BluetoothLeService.PROBE_MODE) != null) {
                    Log.d(TAG, "Probe Mode is: " + intent.getStringExtra(BluetoothLeService.PROBE_MODE));
                    if (intent.getStringExtra(BluetoothLeService.PROBE_MODE).equals("2")) {
                        _probeMode = 2;
                        Log.d(TAG, "Probe Mode set to 2");
                    } else {
                        _probeMode = 0;
                        Log.d(TAG, "Probe mode set to 0");
                    }
                }
//                else if (intent.getStringExtra(BluetoothLeService.CALIBRATION_INDEX) != null) {
//                    Log.e(TAG, "Calibration index is: " + intent.getStringExtra(BluetoothLeService.CALIBRATION_INDEX));
//                    _calibrationIndex = Integer.parseInt(intent.getStringExtra(BluetoothLeService.CALIBRATION_INDEX));
//                } else if (intent.getStringExtra(BluetoothLeService.CALIBRATION_DATA) != null) {
//                    Log.e(TAG, "Calibration Data is: " + intent.getStringExtra(BluetoothLeService.CALIBRATION_DATA));
//                    _calibrationData = intent.getStringExtra(BluetoothLeService.CALIBRATION_DATA);
//                }
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
            } catch (IOException e){
                Log.e(TAG, "Error occured when creating an input stream: " + e);
            }

            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occured when creating output stream: " + e);
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
                    Log.d(TAG, "Input stream was disconnected: " + e);
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


    /**
     * calculate the probe roll value
     * @param ay
     * @param az
     */
    private void calculateRoll(double ay, double az) {
        try {
            double roll = 0;
            roll = Math.atan(ay / az);
            roll = Math.toDegrees(roll);
            DecimalFormat numberFormat = new DecimalFormat("#.00");
            mRoll = roll;
            rollValue.setText(numberFormat.format(mRoll) + " °");
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown when calculating roll: " + e);
        }
    }

    /**
     * Calculate the probe dip value
     * @param ax
     * @param ay
     * @param az
     */
    private void calculateDip(double ax, double ay, double az) {
        try {
            double dip = 0;
            dip = Math.atan((-ax) / Math.sqrt((ay * ay) + (az * az)));
            dip = Math.toDegrees(dip);
            DecimalFormat numberFormat = new DecimalFormat("#.00");
            mDip = dip;
            dipValue.setText(numberFormat.format(mDip) + " °");
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown when calculating dip: " + e);
        }
    }

    /**
     * Calculate the probe azimuth value
     * @param mx
     * @param my
     * @param mz
     * @param roll
     * @param dip
     */
    private void calculateAzimuth(double mx, double my, double mz, double roll, double dip) {
        try {
            Log.e(TAG, "AZIMUTH");
            double azimuth = 0;
            azimuth = Math.atan(((-my) * Math.cos(roll) + mz * Math.sin(roll)) / (mx * Math.cos(dip) + my * Math.sin(dip) + mz * Math.sin(dip) * Math.cos(dip)));
            DecimalFormat numberFormat = new DecimalFormat("#.00");
            mAzimuth = azimuth;
            azimuthValue.setText(numberFormat.format(mAzimuth) + " °");
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown when calculating azimuth");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    /**
     * runs when the activity resumes to reset any information passed in to the activity and to register the bluetooth
     */
    @Override
    protected void onResume() {
        super.onResume();

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVCE_DEVICE_ADDRESS);
        mConnectionStatus = intent.getStringExtra(EXTRA_DEVICE_CONNECTION_STATUS);
        mPrevDepth = intent.getStringExtra(EXTRA_PREV_DEPTH);
        mNextDepth = intent.getStringExtra(EXTRA_NEXT_DEPTH);
        try {
            if (mConnectionStatus.equals("Connected")) {
                updateConnectionState("Connected");
            } else {
                Log.e(TAG, "connection state disconnected upon starting activity");
                updateConnectionState("Disconnected");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown in onResume: " + e);
        }

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connection request result: " + result);
        }
    }

    /**
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     * setup the activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orientation);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        mConnectionStatus = intent.getStringExtra(EXTRA_DEVICE_CONNECTION_STATUS);
        mPrevDepth = intent.getStringExtra(EXTRA_PREV_DEPTH);
        mNextDepth = intent.getStringExtra(EXTRA_NEXT_DEPTH);

        parentActivity = intent.getStringExtra(EXTRA_PARENT_ACTIVITY);
        measurementData = intent.getStringExtra(EXTRA_MEASUREMENT_DATA);

        rollValue = (TextView) findViewById(R.id.probe_rollValue);
        dipValue = (TextView) findViewById(R.id.probe_dipValue);
        azimuthValue = (TextView) findViewById(R.id.probe_azimuthValue);

        measurementRollValue = (TextView) findViewById(R.id.measurement_rollValue);
        measurementDipValue = (TextView) findViewById(R.id.measurement_dipValue);
        measurementAzimuthValue = (TextView) findViewById(R.id.measurement_azimuthValue);

        connectionStatus = (TextView) findViewById(R.id.orientation_connectionStatus);
        connectionImg = (ImageView) findViewById(R.id.orientation_connectionImg);

        if (parentActivity.equals("MEASUREMENT")) {
            String[] splitData = measurementData.split(";", 20);
            measurementRollValue.setText(splitData[1]); //roll
            measurementDipValue.setText(splitData[2]); //dip
            measurementAzimuthValue.setText(splitData[3]); //azimuth
        } else {
            measurementRollValue.setText("Null"); //roll
            measurementDipValue.setText("Null"); //dip
            measurementAzimuthValue.setText("Null"); //azimuth
        }

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        try {
            if (mConnectionStatus.equals("Connected")) {
                updateConnectionState("Connected");
            } else {
                Log.e(TAG, "Connection status is disconnected upon activity start");
                updateConnectionState("Disconnected");
            }
        } catch (Exception e) {
            Log.e(TAG, "Connection status null");
        }

    }




    /**
     * unregister the bluetooth receiver when the app is paused
     */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    /**
     * Go through all gatt services that the phone can detect on the BLE device
     * @param gattServices
     */
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

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
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);

                Log.d(TAG, "Gatt characteristic is: " + gattCharacteristic.getUuid().toString());
                if (gattCharacteristic.getUuid() != null) {
                    if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.DEVICE_ID)) {
                        Operation getDeviceIDOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);
                        request(getDeviceIDOperation);
                    } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.DEVICE_ADDRESS)) {
                        Operation getDeviceAddressOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);
                        request(getDeviceAddressOperation);
                    } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.VERSION_MAJOR)) {
                        Operation getVersionMajorOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);
                        request(getVersionMajorOperation);
                    } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.VERSION_MINOR)) {
                        Operation getVersionMinorOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);
                        request(getVersionMinorOperation);
                    } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.PROBE_MODE)) {
                        Operation getProbeModeOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);
                        request(getProbeModeOperation);
                    }
//                    else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.CALIBRATION_INDEX)) {
//                        Log.e(TAG, "CALIIBRATION INDEX");
//                        Operation getCalibrationIndexOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);
//                        request(getCalibrationIndexOperation);
//                    }
//                    else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.CALIBRATION_DATA)) {
//                        Operation getCalibrationDataOperation = new Operation(gattService, gattCharacteristic, OPERATION_READ);
//                        request(getCalibrationDataOperation);
//                    }
                    else if (_probeMode == 2 && gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.CORE_SHOT)) {
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
                    }
                } else {
                    Log.e(TAG, "gatt characteristic uuid is null");
                }

            }
        }
    }

    private void updateConnectionState(final String resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (resourceId.equals("Connected")) {
                        Log.d(TAG, "Device Connected");
                        connectionStatus.setText("Connected");
                        connectionImg.setImageResource(R.drawable.ready);
                        mConnectionStatus = "Connected";

                    } else if (resourceId.equals("Disconnected")) {
                        Log.e(TAG, "Device Disconnected");
                        connectionStatus.setText("Disconnected");
                        connectionImg.setImageResource(R.drawable.unconnected);
                        mConnectionStatus = "Disconnected";

                        //Just doesnt work
                        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
                        if (mBluetoothLeService != null) {
                            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
                            Log.d(TAG, "Connection request result: " + result);
                            if (result) {
                                updateConnectionState("Connected");
                                try {
                                    shotRequest();
                                } catch (Exception e) {
                                    Log.e(TAG, "Exception thrown: " + e);
                                }
                            }
                        }
                    } else {
                        Log.e(TAG, "Error, no probe selected");
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Exception thrown: " + e);
                }
            }
        });
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
        inflater.inflate(R.menu.menu_orientation, menu);
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
            if (parentActivity.equals("MEASUREMENT")) {
                Intent intent = new Intent(this, ViewMeasurements.class);
                intent.putExtra(ViewMeasurements.EXTRA_DEVICE_NAME, mDeviceName);
                intent.putExtra(ViewMeasurements.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
                intent.putExtra(ViewMeasurements.EXTRA_DEVICE_CONNECTION_STATUS, mConnectionStatus);
                intent.putExtra(ViewMeasurements.EXTRA_NEXT_DEPTH, mNextDepth);
                intent.putExtra(ViewMeasurements.EXTRA_PREV_DEPTH, mPrevDepth);
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
//        switch (item.getItemId()) {
//            case R.id.back_button:
//                if (parentActivity.equals("MEASUREMENT")) {
//                    Intent intent = new Intent(this, ViewMeasurements.class);
//                    intent.putExtra(ViewMeasurements.EXTRA_DEVICE_NAME, mDeviceName);
//                    intent.putExtra(ViewMeasurements.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
//                    intent.putExtra(ViewMeasurements.EXTRA_DEVICE_CONNECTION_STATUS, mConnectionStatus);
//                    intent.putExtra(ViewMeasurements.EXTRA_NEXT_DEPTH, mNextDepth);
//                    intent.putExtra(ViewMeasurements.EXTRA_PREV_DEPTH, mPrevDepth);
//                    startActivity(intent);
//                } else {
//                    Intent intent = new Intent(this, ProbeDetails.class);
//                    intent.putExtra(ProbeDetails.EXTRA_DEVICE_NAME, mDeviceName);
//                    intent.putExtra(ProbeDetails.EXTRA_DEVICE_DEVICE_ADDRESS, mDeviceAddress);
//                    intent.putExtra(ProbeDetails.EXTRA_DEVICE_CONNECTION_STATUS, mConnectionStatus);
//                    startActivity(intent);
//                }
//                break;
//            case R.id.tools_button: //TODO
//                break;
//            case R.id.tools_setZero: //TODO
//                break;
//            case R.id.tools_clearZero: //TODO
//                break;
//            case R.id.sensors_button: //TODO
//                break;
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

    /**
     * Write a shot request to the probe to get real time data upon button press
     * @param view
     */
    public void shotRequest(View view) {
        //Probe Mode
        boolean status = mBluetoothLeService.writeToProbeMode(02);
        Log.e(TAG, "STATUS OF WRITE: " + status);
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            Log.e(TAG, "Could not sleep" + e);
        }
        if (status) {
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
        new CountDownTimer(700, 1) {
            public void onTick(long millisUntilFinished) {}
            public void onFinish() {
                dataToBeRead = 0;
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            }
        }.start();
    }

    /**
     * Write a shot request to the probe manually
     */
    public void shotRequest() {
        boolean status = mBluetoothLeService.writeToProbeMode(02);
        Log.e(TAG, "STATUS OF WRITE: " + status);
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            Log.e(TAG, "Could not sleep" + e);
        }
        if (status) {
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
        new CountDownTimer(700, 1) {
            public void onTick(long millisUntilFinished) {}
            public void onFinish() {
                dataToBeRead = 0;
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            }
        }.start();
    }

    /**
     * Reset the queue upon button press
     * @param view
     */
    public void resetQueueClick(View view) {
        resetQueue();
        Log.d(TAG, "Resetting the queue");
    }

    public static String removeLastChar(String s) {
        return (s == null || s.length() == 0)
                ? null
                : (s.substring(0, s.length() - 1));
    }
}