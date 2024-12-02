package com.work.libtest;

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
import android.icu.util.Measure;
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
import android.text.format.DateFormat;
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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

import com.work.libtest.CalibrationHelper;

public class SensorActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE_NAME = "Device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "Device_address";
    public static final String EXTRA_DEVICE_CONNECTION_STATUS = "Connection_status";

    public static final String EXTRA_DEVICE_SERIAL_NUMBER = "Serial_number";
    public static final String EXTRA_DEVICE_DEVICE_ADDRESS = "Device_gathered_addresses";
    public static final String EXTRA_DEVICE_VERSION = "Device_firmware_version";
    public static final String EXTRA_PARENT_ACTIVITY = "Device_parent_activity";

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
    private TextView textRoll360;
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
    private TextView dev_record_number;

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

    private TextView accTemp;
    private TextView magTemp;

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

    private TextView shotFormat;

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
            setContentView(R.layout.activity_sensor);
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
            textDeviceNameAndAddress = findViewById(R.id.probe_info);                     //Get a reference to the TextView that will display the device name and address
            textDeviceStatus = findViewById(R.id.orientation_connection_Txt);                     //Get a reference to the TextView that will display the device name and address
            shotFormat = findViewById(R.id.dev_format_info);

            final Intent intent = getIntent();
            mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
            mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);

            textAccX = findViewById(R.id.accelerometer_x_data);
            textAccY = findViewById(R.id.accelerometer_y_data);
            textAccZ = findViewById(R.id.accelerometer_z_data);

            magTemp = findViewById(R.id.magnetometer_temp_data);
            accTemp = findViewById(R.id.accelerometer_temp_data);

            editBoxAverage = findViewById(R.id.maxDev_magX_data);

            textMagX = findViewById(R.id.magnetometer_x_data);
            textMagY = findViewById(R.id.magnetometer_y_data);
            textMagZ = findViewById(R.id.magnetometer_z_data);

            textRoll = findViewById(R.id.orientation_roll_data);
            textDip = findViewById(R.id.orientation_dip_data);
            textAz = findViewById(R.id.orientation_azimuth_data);
            textTempUc = findViewById(R.id.orientation_temperature_data); //idk if this is correct

            connectTimeoutHandler = new Handler(Looper.getMainLooper());
            connectionStatus = (TextView) findViewById(R.id.orientation_connection_Txt);
            connectionStatusImage = (ImageView) findViewById(R.id.orientation_connection_img);

            dev_record_number = (TextView) findViewById(R.id.dev_record_number_info);
            buttonLive = (Button) findViewById(R.id.shot_request_button);
            buttonLive.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Log.i(TAG, "PJH - processing Live Data button press");
                    if (buttonLive.getText() == "PAUSE"){
                        buttonLive.setText("LIVE DATA");
                        bleService.setProbeIdle();
                    }
                    else {
                        buttonLive.setText("PAUSE");
                        bleService.setProbeMode(2); //PROBE_MODE_ROLLING_SHOTS);
                    }
                }
            });
            initializeDisplay();
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown in onCreate in SensorActivity: " + e);
        }
    }

//    private StateApp stateApp;

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

            final Intent intent = getIntent();
            try {
                Log.e(TAG, intent.getStringExtra(EXTRA_PARENT_ACTIVITY) + "," + intent.getStringExtra(EXTRA_DEVICE_NAME) + "," + intent.getStringExtra(EXTRA_DEVICE_ADDRESS));
                stateApp = stateApp.RUNNING;
                bleDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
                bleDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);

                if (bleDeviceName != null) {
                    textDeviceNameAndAddress.setText(bleDeviceName);
                } else {
                    textDeviceNameAndAddress.setText(R.string.unknown_device);
                }
                if (bleDeviceAddress != null) {
                    textDeviceNameAndAddress.append(" - " + bleDeviceAddress);
                }

                        if (bleDeviceAddress == null) {
                            stateConnection = StateConnection.DISCONNECTED;
                        } else {
                            stateConnection = StateConnection.CONNECTING;
                            connectWithAddress(bleDeviceAddress);
                        }
                        updateConnectionState();

            } catch (Exception e) {
                Log.e(TAG, "Exception thrown in getting intent: " + e);
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
        bleService.setProbeIdle();
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
            Log.d(TAG, "exit sensor activity to main activity");
            Log.e(TAG, "Sensor name: " + mDeviceName + ", address: " + mDeviceAddress);
            Log.e(TAG, "Probe name: " + bleDeviceName + ", address: " + bleDeviceAddress);
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_DEVICE_NAME, bleDeviceName);
            intent.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, bleDeviceAddress);
//            intent.putExtra(MainActivity.EXTRA_DEVICE_CONNECTION_STATUS, mConnectionStatus);
            intent.putExtra(MainActivity.EXTRA_PARENT_ACTIVITY, "ProbeDetails");
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

                intentSaveData.putExtra(SaveData.EXTRA_DEVICE_NAME, mDeviceName);
                intentSaveData.putExtra(SaveData.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
                intentSaveData.putExtra(SaveData.EXTRA_DEVICE_SERIAL_NUMBER, lSerialNumber);
                intentSaveData.putExtra(SaveData.EXTRA_DEVICE_DEVICE_ADDRESS, lDeviceAddress);
                intentSaveData.putExtra(SaveData.EXTRA_DEVICE_VERSION, lFirmwareVersion);
                intentSaveData.putExtra(SaveData.EXTRA_DEVICE_CONNECTION_STATUS, mConnectionStatus);
                intentSaveData.putExtra(SaveData.EXTRA_PARENT_ACTIVITY, "Sensor");

                try {
                    if (SavedMeasurements != null) {
                        Log.d(TAG, "PRINTING PROBE DATA");
                        Log.d(TAG, SavedMeasurements.get(0).getName());
                    } else {
                        Log.e(TAG, "Probe Data is null!");
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Could not save correct data: " + e);
                }
                startActivity(intentSaveData);
            }
        }
        return true;
    }
    /**********************************************************************************************/

    static LinkedList<Measurement> SavedMeasurements = new LinkedList<>();

    private void saveData() {
        try {
            //need to add all current data to a measurement and save to a linkedList
            Date c = Calendar.getInstance().getTime();
            System.out.println("Current time => " + c);

            SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
            String formattedDate = df.format(c);

            SimpleDateFormat dfer = new SimpleDateFormat("h:mm:ss a");
            String time = dfer.format(Calendar.getInstance().getTime());


            Measurement currentData = new Measurement((String) dev_record_number.getText(), (String) formattedDate,
                    (String) time, (String) textTempUc.getText(), (String) null, (String) textDip.getText(),
                    (String) textRoll.getText(), (String) textAz.getText());
            //increase the value on the save menu button by 1
            MenuItem saveMenuItem = menu.findItem(R.id.sensor_save_button);
            number = saveMenuItem.getTitle().toString().replace("Save ", "");
            number = Integer.toString(Integer.valueOf(number) + 1);
            saveMenuItem.setTitle("Save " + number);
            SavedMeasurements.add(currentData);
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown saved sensor data: " + e);
        }
    }

    private boolean hold = false;

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
                        textDeviceNameAndAddress.setText(bleDeviceName);                        //Display the name
                    } else {
                        textDeviceNameAndAddress.setText(R.string.unknown_device);                     //or display "Unknown Device"
                    }
                    if (bleDeviceAddress != null) {                                             //See if there is an address
                        textDeviceNameAndAddress.append(" - " + bleDeviceAddress);              //Display the address
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
                    Log.d(TAG, "Received Intent  ACTION_BLE_CONFIG_READY");
                    progressBar.setVisibility(ProgressBar.INVISIBLE);

                    String verString = bleService.getFirmwareVersionString();
                    textDeviceStatus.setText(R.string.ready);
                    bleService.parseBinaryCalibration();
                    bleService.setNotifications(true);
                    haveSuitableProbeConnected = true;
                    break;
                }
                case BleService.ACTION_BLE_FETCH_CAL: {                                        //Have completed service discovery
                    Log.d(TAG, "PJH - Received Intent  ACTION_BLE_FETCH_CAL");
                    textDeviceStatus.setText("Fetching calibration");                            //Show "Discovering"
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                    break;
                }
                case BleService.ACTION_BLE_NEW_DATA_RECEIVED: {                                     //Have received data (characteristic notification) from BLE device
                    Log.i(TAG, "PJH - Received Intent ACTION_BLE_NEW_DATA_RECEIVED");
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
            textDeviceStatus.setText("Not Connected");  // PJH - hack - shouldn't be here!
            textAccX.setText("");
            textAccY.setText("");
            textAccZ.setText("");
            textAccMag.setText("");

            textMagX.setText("");
            textMagY.setText("");
            textMagZ.setText("");
            textMagMag.setText("");

            textRoll.setText("");
            textRoll360.setText("");
            textDip.setText("");
            textAz.setText("");
            textAzErr.setText("");


            textAlignCount.setText("0");
            textAlignAvgDip.setText("");
            textAlignAvgAz.setText("");

            textAcceptComment.setText("Select Location and press Start\n(in -50 tray, at 0 roll, az should be 283.26)");
            textAcceptDip.setText("");
            textAcceptAz.setText("");

            buttonAlignStart.setText("START");  // was having issues with first click not working
            buttonAcceptStart.setText("START");

            textAcceptResultAz.setText("");
            textAcceptResultDip.setText("");

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
            int count = toInteger(editBoxAverage.getText().toString());
            if (count < 1) {
                count = 1;
            }
            if (count > 120) {   // this value is set by ringBufferSize in bleService
                count = 120;
            }
            double newVal[] = bleService.getLatestBoreshot(count);
//            double newVal[] = bleService.getLatestCoreshot(count);


            recordCount = bleService.getSensorDataCount();
            dev_record_number.setText(String.format("%7.1f", newVal[0]));

            textAccX.setText(String.format("%7.4f", newVal[1]));
            textAccY.setText(String.format("%7.4f", newVal[2]));
            textAccZ.setText(String.format("%7.4f", newVal[3]));
            boolean accValid = true;
            double accMag = Math.sqrt(newVal[1]*newVal[1] + newVal[2]*newVal[2] + newVal[3]*newVal[3]);
            double magMag = Math.sqrt(newVal[4]*newVal[4] + newVal[5]*newVal[5] + newVal[6]*newVal[6]);
            textMagX.setText(String.format("%7.4f", newVal[4]));
            textMagY.setText(String.format("%7.4f", newVal[5]));
            textMagZ.setText(String.format("%7.4f", newVal[6]));
            shotFormat.setText(String.format("%7.1f", newVal[11]));
            textRoll.setText(String.format("%7.4f", newVal[7]));
//            textRoll360.setText(String.format("(%7.4f)", newVal[7]+180));
            textDip.setText(String.format("%7.4f", newVal[8]));
            textAz.setText(String.format("%7.4f", newVal[9]));
//            textAzErr.setText("");  // just in case we are out of accept mode - will this flicker?

//            textTempUc.setText(String.format("%7.4f", newVal[10]));

            //orientation_temperature_data accelerometer_temp_data magnetometer_temp_data
            textTempUc.setText(String.format("%7.4f", newVal[10]));
            accTemp.setText(String.format("%7.4f", newVal[10]));
            magTemp.setText(String.format("%7.4f", newVal[10]));

            if (newAlignCountRemaining > 0) {
                newAlignReadingDipSum += newVal[8];
                newAlignReadingAzSum += newVal[9];
                newAlignCountRemaining -= 1;
                if (newAlignCountRemaining == 0) {
                    if (switchRecord.isChecked()) {  // do we have a race condition here
                        bleService.stopRecordingSensorData();
                    }
                    alignDipTotal += newAlignReadingDipSum;
                    alignAzTotal += newAlignReadingAzSum;
                    alignCount += 1;
                }

                if (acceptShowLiveError) {
                    acceptCurrentLiveDipError = Math.abs(newVal[8] - acceptTestPointDip[acceptState - 1]);
                    acceptCurrentLiveRoll360Error = Math.abs(newVal[7] + 180 - acceptTestPointRoll[acceptState - 1]);
                    //textFirmwareVersion.setText(String.format("Acc DipErr=%3.2f RollErr=%3.2f", acceptCurrentLiveDipError, acceptCurrentLiveRoll360Error));// PJH TODO DEBUG HACK
                    double azIdeal = 0;
                    if ((acceptState >= 1) && (acceptState <= 4)) {
                        azIdeal = acceptIdeal50Az;
                    }
                    if ((acceptState >= 5) && (acceptState <= 8)) {
                        azIdeal = acceptIdeal60Az;
                    }
                    if ((acceptState >= 9) && (acceptState <= 12)) {
                        azIdeal = acceptIdeal30Az;
                    }
                    acceptCurrentLiveAzError = Math.abs(newVal[9] - azIdeal);
                    textAzErr.setText(String.format("(err:%7.4f)", acceptCurrentLiveAzError));
                } else {
                    acceptCurrentLiveDipError = 0;
                    acceptCurrentLiveRoll360Error = 0;
                    acceptCurrentLiveAzError = 0;
                }

                if (newAcceptCountRemaining > 0) {
                    newAcceptReadingDipSum += newVal[8];
                    newAcceptReadingAzSum += newVal[9];
                    newAcceptCountRemaining -= 1;
                    textAlignCountdown.setText(String.format("(%d)", newAcceptCountRemaining));  // yes, I know it is in the wrong area

                    if (acceptState > 0) {
                        textAcceptResultAz.setText("");
                        textAcceptResultDip.setText("");
                    }

                    if (newAcceptCountRemaining == 0) {
                        if (switchRecord.isChecked()) {  // do we have a race condition here
                            // we have recorded the last of the shots
                            bleService.stopRecordingSensorData();
                        }

                        if ((acceptState > 0) && (acceptState <= 12)) {
                            acceptDip[acceptState - 1] = newAcceptReadingDipSum / acceptSamplesPerReading;
                            acceptAz[acceptState - 1] = newAcceptReadingAzSum / acceptSamplesPerReading;
                            acceptRoll[acceptState - 1] = newVal[7];
                            acceptState += 1;
                        }
                        textAcceptDip.setText(String.format("%7.4f", (newAcceptReadingDipSum / acceptSamplesPerReading)));
                        textAcceptAz.setText(String.format("%7.4f", (newAcceptReadingAzSum / acceptSamplesPerReading)));

                        if (acceptState >= 13) {
                            // Ok, have taken all 12 readings
                            textAcceptComment.setText(String.format("Test complete"));
                            buttonAcceptStart.setText("START");

                            acceptShowLiveError = false;  // turn off error display and make sure everything back to normal

                            bleService.setProbeIdle();
                            // now generate results file
                            String nowDate = new SimpleDateFormat("yyyy-MM-dd_hh-mm", Locale.getDefault()).format(new Date());
                            String safeBleDeviceAddress = bleDeviceAddress.replace(':', '-');
                            String safeBleDeviceName = bleDeviceName.replace(':', '-');
                            String filename = String.format("AcceptTest_%s_%s_%s.csv", nowDate, safeBleDeviceAddress, safeBleDeviceName);
                            Log.i(TAG, String.format("PJH - Accept filename: %s", filename));
                            double sqAzDeltaSum = 0;
                            double sqDipDeltaSum = 0;
                            double err = 0;
                            for (int i = 0; i < 4; i++) {
                                err = acceptAz[i] - acceptIdeal50Az;
                                if (err > 180) {
                                    err -= 360;
                                }
                                if (err < -180) {
                                    err += 360;
                                }
                                sqAzDeltaSum += (err * err);

                                err = acceptDip[i] - acceptIdeal50Dip;
                                if (err > 180) {
                                    err -= 360;
                                }
                                if (err < -180) {
                                    err += 360;
                                }
                                sqDipDeltaSum += (err * err);

                            }
                            for (int i = 4; i < 8; i++) {
                                err = acceptAz[i] - acceptIdeal60Az;
                                if (err > 180) {
                                    err -= 360;
                                }
                                if (err < -180) {
                                    err += 360;
                                }
                                sqAzDeltaSum += (err * err);

                                err = acceptDip[i] - acceptIdeal60Dip;
                                if (err > 180) {
                                    err -= 360;
                                }
                                if (err < -180) {
                                    err += 360;
                                }
                                sqDipDeltaSum += (err * err);

                            }
                            for (int i = 8; i < 12; i++) {
                                err = acceptAz[i] - acceptIdeal30Az;
                                if (err > 180) {
                                    err -= 360;
                                }
                                if (err < -180) {
                                    err += 360;
                                }
                                sqAzDeltaSum += (err * err);

                                err = acceptDip[i] - acceptIdeal30Dip;
                                if (err > 180) {
                                    err -= 360;
                                }
                                if (err < -180) {
                                    err += 360;
                                }
                                sqDipDeltaSum += (err * err);

                            }
                            acceptRmsAz = Math.sqrt(sqAzDeltaSum / 12);
                            acceptRmsDip = Math.sqrt(sqDipDeltaSum / 12);

                            textAcceptResultAz.setText(String.format("%5.3f", acceptRmsAz));     // show the result (summary) of the test
                            textAcceptResultDip.setText(String.format("%5.3f", acceptRmsDip));
                            textAcceptComment.setText(String.format("Test complete\nPress Start to begin a new test."));
                            acceptState = 0;
                        } else {
                            // more readings to take...
                            textAcceptComment.setText(String.format("%dof12 Place probe in '%d' tray, adjust roll to %d degrees, step back and press 'Take Reading'",
                                    acceptState, acceptTestPointDip[acceptState - 1], acceptTestPointRoll[acceptState - 1]));
                        }
                    }
                }
            }
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
                final Intent bleScanActivityIntent = new Intent(SensorActivity.this, BleScanActivity.class); //Create Intent to start the BleScanActivity
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
                        textDeviceStatus.setText(R.string.waiting_to_connect);                             //Show "Connecting"
                        progressBar.setVisibility(ProgressBar.VISIBLE);                             //Show the circular progress bar
                        break;
                    }
                    case CONNECTED: {
                        textDeviceStatus.setText(R.string.interrogating_configuration);

                        progressBar.setVisibility(ProgressBar.INVISIBLE);                           //Hide the circular progress bar
                        break;
                    }
                    case DISCOVERING: {
                        //textDeviceStatus.setText(R.string.discovering);                            //Show "Discovering"
                        textDeviceStatus.setText(R.string.interrogating_features);                            //Show "Discovering"
                        progressBar.setVisibility(ProgressBar.VISIBLE);                             //Show the circular progress bar
                        break;
                    }
                    case DISCONNECTING: {
                        textDeviceStatus.setText(R.string.disconnecting);                          //Show "Disconnectiong"
                        progressBar.setVisibility(ProgressBar.INVISIBLE);                           //Hide the circular progress bar
                        break;
                    }
                    case DISCONNECTED:
                    default: {
                        stateConnection = StateConnection.DISCONNECTED;                             //Default, in case state is unknown
                        textDeviceStatus.setText(R.string.not_connected);                          //Show "Not Connected"
                        progressBar.setVisibility(ProgressBar.INVISIBLE);                           //Hide the circular progress bar
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
                textAlignCount.setText("0");
                textAlignAvgDip.setText("0");
                textAlignAvgAz.setText("0");

                alignSamplesPerReading = count;

                buttonAlignStart.setText("ABORT");
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