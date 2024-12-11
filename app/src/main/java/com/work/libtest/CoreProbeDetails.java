////////////////////////////////////////////////////////////////////////////////
/**
 * \file ProbeDetails.java
 * \brief Activity to view base probe details
 * \author Anna Pedersen
 * \date Created: 07/06/2024
 */
package com.work.libtest;

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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CoreProbeDetails extends AppCompatActivity {

    public static final String EXTRA_DEVICE_NAME = "Device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "Device_address";
    public static final String EXTRA_DEVICE_CONNECTION_STATUS = "Connection_status";

    public static final String EXTRA_CONNECTION_STATUS = "CONNECTION_STATUS";
    public static final String EXTRA_PARENT_ACTIVITY = "Parent_Activity";

    public static final String EXTRA_DEVICE_SERIAL_NUMBER = "Serial_number";
    public static final String EXTRA_DEVICE_DEVICE_ADDRESS = "Device_gathered_addresses";
    public static final String EXTRA_DEVICE_VERSION = "Device_firmware_version";

    private static final int REQ_CODE_ENABLE_BT =     1;                                            //Codes to identify activities that return results such as enabling Bluetooth
    private static final int REQ_CODE_SCAN_ACTIVITY = 2;                                            //or scanning for bluetooth devices
    private static final int REQ_CODE_ACCESS_LOC1 =   3;                                            //or requesting location access.
    private static final int REQ_CODE_ACCESS_LOC2 =   4;                                            //or requesting location access a second time.
    private static final long CONNECT_TIMEOUT = 10000;                                        //Length of time in milliseconds to try to connect to a device

    private BleService bleService;                                                                  //Service that handles all interaction with the Bluetooth radio and remote device
    private ByteArrayOutputStream transparentUartData = new ByteArrayOutputStream();                //Stores all the incoming byte arrays received from BLE device in bleService
    private ShowAlertDialogs showAlert;                                                             //Object that creates and shows all the alert pop ups used in the app
    private Handler connectTimeoutHandler;                                                          //Handler to provide a time out if connection attempt takes too long
    public static String bleDeviceName;
    public static String bleDeviceAddress;

    private boolean haveSuitableProbeConnected = false;
    private enum StateConnection {DISCONNECTED, CONNECTING, DISCOVERING, CONNECTED, DISCONNECTING}  //States of the Bluetooth connection
    private CoreProbeDetails.StateConnection stateConnection;                                                        //State of Bluetooth connection
    private enum StateApp {STARTING_SERVICE, REQUEST_PERMISSION, ENABLING_BLUETOOTH, RUNNING}       //States of the app
    private CoreProbeDetails.StateApp stateApp;

//    private String mDeviceName;
//    private String mDeviceAddress;
//    private String mDeviceConnectionStatus;

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


    public int seconds;
    private long startTime = 0;

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            seconds = (int) (millis / 1000);

            if (bleDeviceName != null && bleDeviceAddress != null) {
                if (stateConnection == CoreProbeDetails.StateConnection.DISCONNECTED || stateConnection == CoreProbeDetails.StateConnection.DISCONNECTING) {
                    if ((bleDeviceName != null) && (bleDeviceAddress != null) /**&& bleService.isCalibrated()*/) {                                                //See if there is a device name
                        // attempt a reconnection
                        stateConnection = CoreProbeDetails.StateConnection.CONNECTING;                               //Got an address so we are going to start connecting
                        connectWithAddress(bleDeviceAddress);                                       //Initiate a connection
                    }

                    updateConnectionState();
                }
            }
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
//        updateConnectionState("Disconnected");
    }

    private TextView textDeviceNameAndAddress;
    private TextView textDeviceStatus;
    private ImageView blackProbeStatusImg;

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

        startTime = System.currentTimeMillis();
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.postDelayed(timerRunnable, 0);

        try {
            setContentView(R.layout.activity_core_probe_details);
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            showAlert = new ShowAlertDialogs(this);                                             //Create the object that will show alert dialogs
            Log.i(TAG, "PJH - ========== onCreate ============");    // just as a separator in hte logcat window (to distinguish this run from the previous run)
            stateConnection = CoreProbeDetails.StateConnection.DISCONNECTED;                                             //Initial stateConnection when app starts
            stateApp = CoreProbeDetails.StateApp.STARTING_SERVICE;                                                       //Are going to start the BleService service
            Log.i(TAG, "PJH - about to request FINE permission");
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { //Check whether we have location permission, required to scan
                stateApp = CoreProbeDetails.StateApp.REQUEST_PERMISSION;                                                 //Are requesting Location permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_CODE_ACCESS_LOC1); //Request fine location permission
            }
            if (stateApp == CoreProbeDetails.StateApp.STARTING_SERVICE) {                                                //Only start BleService if we already have location permission
                Intent bleServiceIntent = new Intent(this, BleService.class);             //Create Intent to start the BleService
                this.bindService(bleServiceIntent, bleServiceConnection, BIND_AUTO_CREATE);             //Create and bind the new service with bleServiceConnection object that handles service connect and disconnect
            }

            connectTimeoutHandler = new Handler(Looper.getMainLooper());                                //Create a handler for a delayed runnable that will stop the connection attempt after a timeout


            dataToBeRead = 0;

            final Bundle intent = getIntent().getExtras();
            bleDeviceName = intent.getString(EXTRA_DEVICE_NAME);
            bleDeviceAddress = intent.getString(EXTRA_DEVICE_ADDRESS);
            Log.d(TAG, "Device Name: " + bleDeviceName + ", Device Address: " + bleDeviceAddress);

            textDeviceNameAndAddress = (TextView) findViewById(R.id.status_probeNumber);
            textDeviceStatus = (TextView) findViewById(R.id.connectionStatusTxt);
            blackProbeStatusImg = (ImageView) findViewById(R.id.connectionStatusImg);
            serialNumber = (TextView) findViewById(R.id.info_SerialNumberTxt);
            deviceAddress = (TextView) findViewById(R.id.info_deviceAddressTxt_details);
            firmwareVersion = (TextView) findViewById(R.id.info_firmwareVersionTxt);
//            connectionDetails = (TextView) findViewById(R.id.connectionDetails);


        } catch (Exception e) {
            Log.e(TAG, "Exception caught in onCreate: " + e);
        }
    }


    /**
     * When the activity is resumed it needs to update any data passed in to the app as well as the probes connection status
     */
    @Override
    protected void onResume() {
        super.onResume();
        try {
            registerReceiver(bleServiceReceiver, bleServiceIntentFilter());                         //Register receiver to handles events fired by the BleService
            if (bleService != null && !bleService.isBluetoothRadioEnabled()) {                      //Check if Bluetooth radio was turned off while app was paused
                if (stateApp == CoreProbeDetails.StateApp.RUNNING) {                                                 //Check that app is running, to make sure service is connected
                    stateApp = CoreProbeDetails.StateApp.ENABLING_BLUETOOTH;                                         //Are going to request user to turn on Bluetooth
                    stateConnection = CoreProbeDetails.StateConnection.DISCONNECTED;                                 //Must be disconnected if Bluetooth is off
                    startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQ_CODE_ENABLE_BT); //Start the activity to ask the user to grant permission to enable Bluetooth
                    Log.i(TAG, "Requesting user to enable Bluetooth radio");
                }
            }
            final Intent intent = getIntent();
            try {
                String parentActivityValue = intent.getStringExtra(EXTRA_PARENT_ACTIVITY);
                Log.e(TAG, intent.getStringExtra(EXTRA_PARENT_ACTIVITY) + "," + intent.getStringExtra(EXTRA_DEVICE_NAME) + "," + intent.getStringExtra(EXTRA_DEVICE_ADDRESS));
                if (parentActivityValue != null) {
                    if (parentActivityValue.equals("SurveyOptions") || parentActivityValue.equals("ProbeDetails") || parentActivityValue.equals("TakeMeasurements") || parentActivityValue.equals("AllSurveyOptions") || parentActivityValue.equals("Preferences")) {
                        stateApp = CoreProbeDetails.StateApp.RUNNING;
                        bleDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
                        bleDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);

                        if (bleDeviceName != null) {
                            textDeviceNameAndAddress.setText(bleDeviceName); //COMEBACK
                        } else {
                            textDeviceNameAndAddress.setText(R.string.unknown_device);
                        }
                        if (bleDeviceAddress != null) {
                            textDeviceNameAndAddress.append(" - " + bleDeviceAddress);
                        }

                        if (bleDeviceAddress == null) {
                            stateConnection = CoreProbeDetails.StateConnection.DISCONNECTED;
                        } else {
                            stateConnection = CoreProbeDetails.StateConnection.CONNECTED;
                        }
                        updateConnectionState();
                    } else if (parentActivityValue.equals(Globals.ActivityName.AllSurveyOptions)) {
                        bleDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
                        bleDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
                    } else if (parentActivityValue.equals(Globals.ActivityName.ProbeDetails)) {
                        bleDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
                        bleDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
                    } else {
                        Log.e(TAG, "Impossible"); //error as these are the only activities which lead back to the main activity
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception thrown in getting intent: " + e);
            }

            if ((bleDeviceName != null) && (bleDeviceAddress != null) && bleService.isCalibrated()) {                                                //See if there is a device name
                stateConnection = CoreProbeDetails.StateConnection.CONNECTING;                               //Got an address so we are going to start connecting
                connectWithAddress(bleDeviceAddress);                                       //Initiate a connection
            }

            updateConnectionState();                                                                //Update the screen and menus
        } catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();                                                                          //Call superclass (AppCompatActivity) onDestroy method
        if (stateApp != CoreProbeDetails.StateApp.REQUEST_PERMISSION) {                                              //See if we got past the permission request
            unbindService(bleServiceConnection);                                                    //Unbind from the service handling Bluetooth
        }
    }

    private final ServiceConnection bleServiceConnection = new ServiceConnection() {                //Create new ServiceConnection interface to handle connection and disconnection

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {              //Service connects
            try {
                Log.i(TAG, "BleService connected");
                BleService.LocalBinder binder = (BleService.LocalBinder) service;                   //Get the Binder for the Service
                bleService = binder.getService();                                                   //Get a link to the Service from the Binder
                if (bleService.isBluetoothRadioEnabled()) {                                         //See if the Bluetooth radio is on
                    stateApp = CoreProbeDetails.StateApp.RUNNING;                                                    //Service is running and Bluetooth is enabled, app is now fully operational
                }
                else {                                                                              //Radio needs to be enabled
                    stateApp = CoreProbeDetails.StateApp.ENABLING_BLUETOOTH;                                         //Are requesting Bluetooth to be turned on
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

    // ----------------------------------------------------------------------------------------------------------------
    // Callback for Activities that return a result
    // We call BluetoothAdapter to turn on the Bluetooth radio and BleScanActivity to scan
    // and return the name and address of a Bluetooth LE device that the user chooses
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);                                    //Pass the activity result up to the parent method
        switch (requestCode) {                                                                      //See which Activity returned the result
            case REQ_CODE_ENABLE_BT: {
                if (resultCode == Activity.RESULT_OK) {                                             //User chose to enable Bluetooth
                    stateApp = CoreProbeDetails.StateApp.RUNNING;                                                    //Service is running and Bluetooth is enabled, app is now fully operational                     //Start the BleScanActivity to do a scan for devices
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
                    stateApp = CoreProbeDetails.StateApp.RUNNING;                                                    //Service is running and Bluetooth is enabled, app is fully operational
                    bleDeviceAddress = intent.getStringExtra(BleScanActivity.EXTRA_SCAN_ADDRESS);   //Get the address of the BLE device selected in the BleScanActivity
                    bleDeviceName = intent.getStringExtra(BleScanActivity.EXTRA_SCAN_NAME);         //Get the name of the BLE device selected in the BleScanActivity
                    if (bleDeviceName != null) {                                                //See if there is a device name
                        textDeviceNameAndAddress.setText(bleDeviceName);                        //Display the name
                    } else {
                        textDeviceNameAndAddress.setText("Unknown");                     //or display "Unknown Device"
                    }
                    if (bleDeviceAddress != null) {                                             //See if there is an address
                        textDeviceNameAndAddress.append(" - " + bleDeviceAddress);              //Display the address
                    }

                    if (bleDeviceAddress == null) {                                                 //Check whether we were given a device address
                        stateConnection = CoreProbeDetails.StateConnection.DISCONNECTED;                             //No device address so not connected and not going to connect
                    } else {
                        stateConnection = CoreProbeDetails.StateConnection.CONNECTING;                               //Got an address so we are going to start connecting
                        connectWithAddress(bleDeviceAddress);                                       //Initiate a connection
                    }
                } else {                                                                            //Did not get a valid result from the BleScanActivity
                    stateConnection = CoreProbeDetails.StateConnection.DISCONNECTED;                                 //No result so not connected and not going to connect
                }
                updateConnectionState();                                                            //Update the connection state on the screen and menus
                break;
            }
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Callback for permission requests (new feature of Android Marshmallow requires runtime permission requests)
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.i(TAG, "PJH - onRequestPermissionsResult");

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {                                 //See if location permission was granted
            Log.i(TAG, "PJH - Location permission granted");
            stateApp = CoreProbeDetails.StateApp.STARTING_SERVICE;                                                   //Are going to start the BleService service
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
            //Permission refused twice so send user to settings
            Log.i(TAG, "PJH - Location permission NOT granted");
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);               //Create Intent to open the app settings page
            Uri uri = Uri.fromParts("package", getPackageName(), null);            //Identify the package for the settings
            intent.setData(uri);                                                                    //Add the package to the Intent
            startActivity(intent);                                                                  //Start the settings activity
        }
    }

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

    // ----------------------------------------------------------------------------------------------------------------
    // BroadcastReceiver handles various Intents sent by the BleService service.
    private final BroadcastReceiver bleServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {                                     //Intent received
            final String action = intent.getAction();                                               //Get the action String from the Intent
            switch (action) {                                                                       //See which action was in the Intent
                case BleService.ACTION_BLE_CONNECTED: {                                             //Have connected to BLE device
                    Log.d(TAG, "Received Intent  ACTION_BLE_CONNECTED");
                    initializeDisplay();                                                            //Clear the temperature and accelerometer text and graphs
                    transparentUartData.reset();   // PJH remove                                                 //Also clear any buffered incoming data
                    stateConnection = CoreProbeDetails.StateConnection.DISCOVERING;                                  //BleService automatically starts service discovery after connecting
                    blackProbeStatusImg.setImageResource(R.drawable.ready);
                    updateConnectionState();                                                        //Update the screen and menus
                    break;
                }
                case BleService.ACTION_BLE_DISCONNECTED: {                                          //Have disconnected from BLE device
                    Log.d(TAG, "Received Intent ACTION_BLE_DISCONNECTED");
                    initializeDisplay();                                                            //Clear the temperature and accelerometer text and graphs
                    transparentUartData.reset();                                                    //Also clear any buffered incoming data
                    stateConnection = CoreProbeDetails.StateConnection.DISCONNECTED;
                    blackProbeStatusImg.setImageResource(R.drawable.unconnected);

                    // but we want to reconnect automatically - TODO add retry counter then give up
                    if ((bleDeviceName != null) && (bleDeviceAddress != null) && bleService.isCalibrated()) {                                                //See if there is a device name
                        // attempt a reconnection
                        stateConnection = CoreProbeDetails.StateConnection.CONNECTING;                               //Got an address so we are going to start connecting
                        connectWithAddress(bleDeviceAddress);                                       //Initiate a connection
                    }

                    updateConnectionState();                                                        //Update the screen and menus
                    break;
                }
                case BleService.ACTION_BLE_DISCOVERY_DONE: {                                        //Have completed service discovery
                    Log.d(TAG, "PJH - Received Intent  ACTION_BLE_DISCOVERY_DONE");
                    connectTimeoutHandler.removeCallbacks(abandonConnectionAttempt);                //Stop the connection timeout handler from calling the runnable to stop the connection attempt
                    blackProbeStatusImg.setImageResource(R.drawable.calibrating);
                    stateConnection = CoreProbeDetails.StateConnection.CONNECTED;                                    //Were already connected but showing discovering, not connected
                    updateConnectionState();                                                        //Update the screen and menus
                    Log.i(TAG, "PJH - about to request Ezy config");
                    bleService.requestEzyConfig();                                                         //Ask the BleService to connect to start interrogating the device for its configuration
                    break;
                }
                case BleService.ACTION_BLE_DISCOVERY_FAILED: {                                      //Service discovery failed to find the right service and characteristics
                    Log.d(TAG, "Received Intent  ACTION_BLE_DISCOVERY_FAILED");
                    blackProbeStatusImg.setImageResource(R.drawable.unconnected);
                    stateConnection = CoreProbeDetails.StateConnection.DISCONNECTING;                                //Were already connected but showing discovering, so are now disconnecting
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
                    Log.d(TAG, "Received Intent  ACTION_BLE_CONFIG_READY");
                    textDeviceStatus.setText(R.string.ready);
                    blackProbeStatusImg.setImageResource(R.drawable.ready);

                    try {
                        Globals.probeConnectedName = bleDeviceName;
                        Globals.probeConnectedAddress = bleDeviceAddress;
                        Log.e(TAG, "Name: " + Globals.probeConnectedName + ", Address: " + Globals.probeConnectedAddress);
                    } catch (Exception e) {
                        Log.e(TAG, "Exception thrown getting probe information from globals: " + e);
                    }

                    bleService.parseBinaryCalibration();   // process thr calibration data just retrieved
                    bleService.setNotifications(true);   // PJH - HACK - find place where this write doesn't kill something else
                    haveSuitableProbeConnected = true;   // this enables the test stuff
                    Globals.setNotification = true;
                    break;
                }
                case BleService.ACTION_BLE_FETCH_CAL: {                                        //Have completed service discovery
                    Log.d(TAG, "Received Intent  ACTION_BLE_FETCH_CAL");
                    textDeviceStatus.setText("Fetching calibration");                            //Show "Discovering"
                    blackProbeStatusImg.setImageResource(R.drawable.calibrating);
                    break;
                }
                case BleService.ACTION_BLE_NEW_DATA_RECEIVED: {                                     //Have received data (characteristic notification) from BLE device
                    Log.i(TAG, "Received Intent ACTION_BLE_NEW_DATA_RECEIVED");
                    break;
                }
                default: {
                    Log.w(TAG, "Received Intent with invalid action: " + action);
                }
            }
        }
    };

    private void initializeDisplay() {
        try {
            textDeviceStatus.setText("Not Connected");  // PJH - hack - shouldn't be here!
        } catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }

    private void startBleScanActivity() {
        try {
            bleService.invalidateCalibration();  // to force a full connect and reread of the calibration data
            if (stateApp == CoreProbeDetails.StateApp.RUNNING) {                                                     //Only do a scan if we got through startup (permission granted, service started, Bluetooth enabled)
                stateConnection = CoreProbeDetails.StateConnection.DISCONNECTING;                                    //Are disconnecting prior to doing a scan
                haveSuitableProbeConnected = false;
                bleService.disconnectBle();                                                         //Disconnect an existing Bluetooth connection or cancel a connection attempt
                final Intent bleScanActivityIntent = new Intent(CoreProbeDetails.this, BleScanActivity.class); //Create Intent to start the BleScanActivity
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

    private Runnable abandonConnectionAttempt = new Runnable() {
        @Override
        public void run() {
            try {
                if (stateConnection == CoreProbeDetails.StateConnection.CONNECTING) {                                //See if still trying to connect
                    stateConnection = CoreProbeDetails.StateConnection.DISCONNECTING;                                //Are now disconnecting
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
                        textDeviceStatus.setText(R.string.waiting_to_connect);                             //Show "Connecting"
                        blackProbeStatusImg.setImageResource(R.drawable.disconnecting);
                        break;
                    }
                    case CONNECTED: {
                        blackProbeStatusImg.setImageResource(R.drawable.ready);
                        textDeviceStatus.setText(R.string.ready);
                        break;
                    }
                    case DISCOVERING: {
                        textDeviceStatus.setText(R.string.interrogating_features);                            //Show "Discovering"
                        break;
                    }
                    case DISCONNECTING: {
                        textDeviceStatus.setText(R.string.disconnecting);                          //Show "Disconnectiong"
                        break;
                    }
                    case DISCONNECTED:
                    default: {
                        stateConnection = CoreProbeDetails.StateConnection.DISCONNECTED;                             //Default, in case state is unknown
                        textDeviceStatus.setText(R.string.not_connected);                          //Show "Not Connected"
                        break;
                    }
                }
                invalidateOptionsMenu();                                                            //Update the menu to reflect the connection state stateConnection
            }
        });
    }

    /**
     * when the activity is paused unregister the bluetooth receiver
     */
    @Override
    protected void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable); //Call superclass (AppCompatActivity) onPause method
        try {
            unregisterReceiver(bleServiceReceiver);                                                     //Unregister receiver that was registered in onResume()
        } catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }

    public void backProbeDetailClick(View view) {
        Intent intent = new Intent(this, CoreMain.class);
        intent.putExtra(CoreMain.EXTRA_BLACK_DEVICE_NAME, bleDeviceName);
        intent.putExtra(CoreMain.EXTRA_BLACK_DEVICE_ADDRESS, bleDeviceAddress);
        intent.putExtra(CoreMain.EXTRA_PARENT_ACTIVITY, "ProbeDetails");
        startActivity(intent);
    }

    public void backProbeDetailClick() {
        Intent intent = new Intent(this, CoreMain.class);
        Log.e(TAG, "Name: " + bleDeviceName + ", Address: " + bleDeviceAddress);
        intent.putExtra(CoreMain.EXTRA_BLACK_DEVICE_NAME, bleDeviceName);
        intent.putExtra(CoreMain.EXTRA_BLACK_DEVICE_ADDRESS, bleDeviceAddress);
        intent.putExtra(CoreMain.EXTRA_PARENT_ACTIVITY, "ProbeDetails");
        startActivity(intent);
    }

    public void showRealTimeOrientation(View view) {
        //TODO go to show orientation info and compass stuff
        Intent intent = new Intent(this, OrientationActivity.class);
        intent.putExtra(OrientationActivity.EXTRA_DEVICE_NAME, bleDeviceName);
        intent.putExtra(OrientationActivity.EXTRA_DEVICE_ADDRESS, bleDeviceAddress);
        intent.putExtra(OrientationActivity.EXTRA_PARENT_ACTIVITY, "MainProbeDetails");
        startActivity(intent);
    }

    //FIX!
    public void showRealTimeSensors(View view) {
        String saveNum = "0";
        Intent intent = new Intent(this, SensorActivity.class);
        intent.putExtra(SensorActivity.EXTRA_DEVICE_NAME, bleDeviceName);
        intent.putExtra(SensorActivity.EXTRA_DEVICE_ADDRESS, bleDeviceAddress);
        intent.putExtra(SensorActivity.EXTRA_DEVICE_SERIAL_NUMBER, lSerialNumber);
        intent.putExtra(SensorActivity.EXTRA_DEVICE_DEVICE_ADDRESS, lDeviceAddress);
        intent.putExtra(SensorActivity.EXTRA_DEVICE_VERSION, lFirmwareVersion);
        intent.putExtra(SensorActivity.EXTRA_SAVED_NUM, saveNum);
//        intent.putExtra(SensorActivity.EXTRA_PARENT_ACTIVITY, "Sensor");
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

    public void connectProbe(View view) {
        //run ble scan activity
        Log.e(TAG, "Connect Probe");
        try {
            bleService.invalidateCalibration();  // to force a full connect and reread of the calibration data
            if (stateApp == CoreProbeDetails.StateApp.RUNNING) {                                                     //Only do a scan if we got through startup (permission granted, service started, Bluetooth enabled)
                stateConnection = CoreProbeDetails.StateConnection.DISCONNECTING;                                    //Are disconnecting prior to doing a scan
                haveSuitableProbeConnected = false;
                bleService.disconnectBle();                                                         //Disconnect an existing Bluetooth connection or cancel a connection attempt
                final Intent bleScanActivityIntent = new Intent(CoreProbeDetails.this, bleScanCore.class); //Create Intent to start the BleScanActivity
                startActivityForResult(bleScanActivityIntent, REQ_CODE_SCAN_ACTIVITY);              //Start the BleScanActivity
            }
        } catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }
}