////////////////////////////////////////////////////////////////////////////////
/**
 * \file ProbeDetails.java
 * \brief Activity to view base probe details
 * \author Anna Pedersen
 * \date Created: 07/06/2024
 *
 */
package com.work.libtest;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProbeDetails extends AppCompatActivity {

    public static final String EXTRA_DEVICE_NAME = "Device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "Device_address";
    public static final String EXTRA_DEVICE_CONNECTION_STATUS = "Connection_status";

    public static final String EXTRA_DEVICE_SERIAL_NUMBER = "Serial_number";
    public static final String EXTRA_DEVICE_DEVICE_ADDRESS = "Device_gathered_addresses";
    public static final String EXTRA_DEVICE_VERSION = "Device_firmware_version";

    private String mDeviceName;
    private String mDeviceAddress;
    private String mDeviceConnectionStatus;

    private String TAG = "Probe Details";

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

    //Probe info
    private TextView serialNumber;
    private TextView deviceAddress;
    private TextView firmwareVersion;

    //Bluetooth Connection
    private TextView connectionDetails;

    //Setup and Tools
    private TextView calibrationDate;

    //debugging
    private TextView errorCodes;


    //holds last known info
    private String lSerialNumber; //the l stands for looser
    private String lDeviceAddress;
    private String lFirmwareVersion;

    private String lCalibrationDate;


    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;
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
                Log.e(TAG, "Enable to initalise bluetooth");
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
                mConnected = true;
                updateConnectionState("Connected");
                Log.d(TAG, "Device Connected");
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)){
                mConnected = false;
                updateConnectionState("Disconnected");
                Log.d(TAG, "Device Disconnected");
                clearUI();
                mBluetoothLeService.connect(mDeviceAddress);

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                //Task show information
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                if (intent.getStringExtra(BluetoothLeService.EXTRA_DATA) != null) {
                    errorCodes.setText(intent.getStringExtra(BluetoothLeService.EXTRA_DATA).toString());
                }
                if (intent.getStringExtra(BluetoothLeService.SERIAL_NUMBER) != null) {
                    Log.d(TAG, "Device Serial number is: " + intent.getStringExtra(BluetoothLeService.SERIAL_NUMBER).toString());
                    serialNumber.setText(intent.getStringExtra(BluetoothLeService.SERIAL_NUMBER).toString());
                }
                if (intent.getStringExtra(BluetoothLeService.DEVICE_ADDRESS) != null) {
                    Log.d(TAG, "Device Address is: " + intent.getStringExtra(BluetoothLeService.DEVICE_ADDRESS).toString());
                    deviceAddress.setText(intent.getStringExtra(BluetoothLeService.DEVICE_ADDRESS).toString());
                }
                if (intent.getStringExtra(BluetoothLeService.MAJOR_FIRMWARE_VERSION) != null) {
                    Log.d(TAG, "Major firmware version is: " + intent.getStringExtra(BluetoothLeService.MAJOR_FIRMWARE_VERSION));
                    firmwareVersion.setText(intent.getStringExtra(BluetoothLeService.MAJOR_FIRMWARE_VERSION).toString());
                }

                if (intent.getStringExtra(BluetoothLeService.MINOR_FIRMWARE_VERSION) != null) {
                    Log.d(TAG, "Minor firmware version is: " + intent.getStringExtra(BluetoothLeService.MINOR_FIRMWARE_VERSION));
                    firmwareVersion.append(".");
                    firmwareVersion.append(intent.getStringExtra(BluetoothLeService.MINOR_FIRMWARE_VERSION).toString());
                }

                //read characteristics again, this time of the next thing to read.

                dataToBeRead++;
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            }
        }
    };

    /**
     * Clear interface
     */
    private void clearUI() {}

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
//        updateConnectionState("Disconnected");
    }

    /**
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_probe_details);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dataToBeRead = 0;

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        mDeviceConnectionStatus = intent.getStringExtra(EXTRA_DEVICE_CONNECTION_STATUS);
        Log.d(TAG, "Device Name: " + mDeviceName + ", Device Address: " + mDeviceAddress + " Device Connection status: " + mDeviceConnectionStatus);

        probeNumber = (TextView) findViewById(R.id.status_probeNumber);
        probeNumber.setText(mDeviceName);
        connectionStatus = (TextView) findViewById(R.id.connectionStatusTxt);
        connectionStatusImage = (ImageView) findViewById(R.id.connectionStatusImg);
//        serialNumber = (TextView) findViewById(R.id.info_SerialNumberTxt);
        serialNumber = (TextView) findViewById(R.id.info_SerialNumberTxt);
        deviceAddress = (TextView) findViewById(R.id.info_deviceAddressTxt_details);
        firmwareVersion = (TextView) findViewById(R.id.info_firmwareVersionTxt);
        connectionDetails = (TextView) findViewById(R.id.connectionDetails);
        calibrationDate = (TextView) findViewById(R.id.setup_calibrationDate);


        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        try {
            if (mDeviceConnectionStatus != null) {
                Log.wtf(TAG, mDeviceConnectionStatus);
                if (mDeviceConnectionStatus.equals("Connected")) {
                    updateConnectionState("Connected");

                } else {
                    Log.e(TAG, "Device disconnected");
                    updateConnectionState("Disconnected");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown: " + e);
        }
    }


    /**
     * When the activity is resumed it needs to update any data passed in to the app as well as the probes connection status
     */
    @Override
    protected void onResume() {
        super.onResume();

        dataToBeRead = 0;

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        mDeviceConnectionStatus = intent.getStringExtra(EXTRA_DEVICE_CONNECTION_STATUS);
        Log.d(TAG, "Device Name: " + mDeviceName + ", Device Address: " + mDeviceAddress + " Device Connection status: " + mDeviceConnectionStatus);

        try {
            if (mDeviceConnectionStatus.equals("Connected")) {
                updateConnectionState("Connected");
            } else {
                updateConnectionState("Disconnected");
                Log.e(TAG, "Device disconnected");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown: " + e);
        }

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connection request result = " + result);
        }
    }

    /**
     * Display all charactersitics in each service that the app can see from the probe
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

                //Could probs chuck these in an array an interate through them that way.
                if (dataToBeRead == 0) { //Serial Number
                    if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.DEVICE_ID)) {
                        mBluetoothLeService.readCharacteristic(gattCharacteristic);
                    }
                } else if (dataToBeRead == 1) { //Device Address
                    if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.DEVICE_ADDRESS)) {
                        Log.d(TAG, "Found Device Address characteristic");
                        mBluetoothLeService.readCharacteristic(gattCharacteristic);
                    }
                } else if (dataToBeRead == 2) { //Version Major
                    if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.VERSION_MAJOR)) {
                        Log.d(TAG, "Found Version major characteristic");
                        mBluetoothLeService.readCharacteristic(gattCharacteristic);
                    }
                } else if (dataToBeRead == 3) { //Version Minor
                    if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.VERSION_MINOR)) {
                        Log.d(TAG, "Found Version Minor Characteristic");
                        mBluetoothLeService.readCharacteristic(gattCharacteristic);
                    }
                } else if (dataToBeRead == 4) { //Calibration Date
                    //TODO Calibration Date
                }
            }
        }
    }

    /**
     * when the activity is paused unregister the bluetooth receiver
     */
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
                    mDeviceConnectionStatus = "Connected";
                    if (mBluetoothLeService != null) {
                        displayGattServices(mBluetoothLeService.getSupportedGattServices());
                    }

                } else if (resourceId.equals("Disconnected")) {
                    Log.d(TAG, "DEVICE DISCONNECTED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    connectionStatus.setText("Disconnected");
                    connectionStatusImage.setImageResource(R.drawable.unconnected);

                    mDeviceConnectionStatus = "Disconnected";
                    registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
                    if (mBluetoothLeService != null) {
                        final boolean result = mBluetoothLeService.connect(mDeviceAddress);
                        Log.d(TAG, "Connection request result=" + result);
                    }
                } else {
                    connectionStatus.setText("No probe selected");
                    Log.e(TAG, "Error no probe selected");
                }
                //TODO Make a reconnecting /loading status as well
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            //TASK display the data

        }
    }

    public void refresh(View view) {
        if (mBluetoothLeService != null) {
            displayGattServices(mBluetoothLeService.getSupportedGattServices());
        }
    }

    public void backProbeDetailClick(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_DEVICE_NAME, mDeviceName);
        intent.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(MainActivity.EXTRA_PARENT_ACTIVITY, "ProbeDetails");
        startActivity(intent);
    }
    public void backProbeDetailClick() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_DEVICE_NAME, mDeviceName);
        intent.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(MainActivity.EXTRA_PARENT_ACTIVITY, "ProbeDetails");
        Log.e(TAG, "CONNECTION STATUS: " + mDeviceConnectionStatus);
        startActivity(intent);
    }

    public void showRealTimeOrientation(View view) {
        //TODO go to show orientation info and compass stuff
        Intent intent = new Intent(this, OrientationActivity.class);
        intent.putExtra(OrientationActivity.EXTRA_DEVICE_NAME, mDeviceName);
        intent.putExtra(OrientationActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(OrientationActivity.EXTRA_PARENT_ACTIVITY, "ProbeDetails");
        startActivity(intent);
    }

    public void showRealTimeSensors(View view) {
        //TODO show sensor information

        String saveNum = "0";
        Intent intent = new Intent(this, SensorActivity.class);
        intent.putExtra(SensorActivity.EXTRA_DEVICE_NAME, mDeviceName);
        intent.putExtra(SensorActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(SensorActivity.EXTRA_DEVICE_SERIAL_NUMBER, lSerialNumber);
        intent.putExtra(SensorActivity.EXTRA_DEVICE_DEVICE_ADDRESS, lDeviceAddress);
        intent.putExtra(SensorActivity.EXTRA_DEVICE_VERSION, lFirmwareVersion);
        intent.putExtra(SensorActivity.EXTRA_DEVICE_CONNECTION_STATUS, mDeviceConnectionStatus);
        intent.putExtra(SensorActivity.EXTRA_SAVED_NUM, saveNum);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_probe_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.back_button) {
            backProbeDetailClick();
        }
//        switch (item.getItemId()) {
//            case R.id.back_button:
////                onBackPressed();
//                backProbeDetailClick();
//                break;
//        }
        //TASK probs put the back button here lol
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

    public void preferences_movement_deviation_click(View view) {
        //Force the reconnection
        Log.d(TAG, "Forcing device " + mDeviceName + " to reconnect");
        mBluetoothLeService.connect(mDeviceAddress);
        updateConnectionState("Connected");
    }
}