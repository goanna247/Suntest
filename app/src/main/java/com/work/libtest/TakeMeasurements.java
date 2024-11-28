package com.work.libtest;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;


public class TakeMeasurements extends AppCompatActivity {
    public static String TAG = "Take Measurements";
    public Menu menu;

    public static int shotWriteType = 00;

    public static final String EXTRA_DEVICE_NAME = "Device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "Device_address";
    public static final String EXTRA_DEVICE_CONNECTION_STATUS = "Connection_status";
    public static final String EXTRA_MEASUREMENT_TYPE = "Resume_or_new";
    public static final String EXTRA_PREV_DEPTH = "prev depth";
    public static final String EXTRA_NEXT_DEPTH = "next depth";

    int resumePosition = 128; //error code - NEEDED BY OTHER ACTIVITIES

    //probe info
    private TextView connectionStatus;
    private ImageView connectionStatusImg;

    private ImageView collectionNumImg;

    private ConstraintLayout loadingSection;

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
    private volatile static ArrayList<ProbeData> probeDataa = new ArrayList<ProbeData>();

    public static ArrayList<ProbeData> getProbeDataFromTakeMeasurement() {
        return probeDataa;
    }

    private static ArrayList<ProbeData> subProbeData = new ArrayList<>();

    private static final ArrayList<Double> accXData = new ArrayList<>();
    private static final ArrayList<Double> accYData = new ArrayList<>();
    private static final ArrayList<Double> accZData = new ArrayList<>();

    private static final ArrayList<Double> magXData = new ArrayList<>();
    private static final ArrayList<Double> magYData = new ArrayList<>();
    private static final ArrayList<Double> magZData = new ArrayList<>();

    public static ArrayList<String> rawData = new ArrayList<>();

//    private static final ArrayList<Integer> shotsToCollect = new ArrayList<>();

    public static ArrayList<String> dateData = new ArrayList<String>();
    public static ArrayList<String> timeData = new ArrayList<String>();
    public static ArrayList<String> depthData = new ArrayList<String>();
    public static ArrayList<String> tempData = new ArrayList<>();

    public static ArrayList<ProbeData> exportProbeData = new ArrayList<>();

    private BluetoothGattCharacteristic CORE_SHOT_CHARACTERISTIC;
    private BluetoothGattCharacteristic BORE_SHOT_CHARACTERISTIC;

    String shot_format = "", record_number = "", probe_temp = "", core_ax = "", core_ay = "", core_az = "", acc_temp = "";
    String record_number_binary = "", core_ax_binary = "", core_ay_binary, core_az_binary, mag_x_binary = "", mag_y_binary = "", mag_z_binary = "";
    String mag_x = "", mag_y = "", mag_z = "";
    String shot_interval = "";

    String highByte, lowByte;
    boolean hold = false;
    int numSaved = 0;
    private ShowAlertDialogs showAlert;

    private Button directionButton;
    //    private Button takeMeasurement;
    private TextView prevDepth;
    private TextView nextDepth;
    //    private CheckBox exportAllData;
    private Button withdrawButton;
    private TextView connectionStatusText;
    private ImageView connectionStatusImage;

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

    long startTime = 0;

    double elapsedTime; //a long may work better for these data points
    double elapsedSeconds;
    double secondsDisplay;
    double elapsedMinutes;

    //    private long startTime = 0; //for timer
    public int seconds;
    private int starttime = 0;

    private static final int REQ_CODE_ENABLE_BT = 1;                                            //Codes to identify activities that return results such as enabling Bluetooth
    private static final int REQ_CODE_SCAN_ACTIVITY = 2;                                            //or scanning for bluetooth devices
    private static final int REQ_CODE_ACCESS_LOC1 = 3;                                            //or requesting location access.
    private static final int REQ_CODE_ACCESS_LOC2 = 4;                                            //or requesting location access a second time.
    private static final long CONNECT_TIMEOUT = 10000;

    private enum StateConnection {DISCONNECTED, CONNECTING, DISCOVERING, CONNECTED, DISCONNECTING}

    private StateConnection stateConnection;

    private enum StateApp {STARTING_SERVICE, REQUEST_PERMISSION, ENABLING_BLUETOOTH, RUNNING}

    private StateApp stateApp;

    private BleService bleService;

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
    private Handler connectTimeoutHandler;
    private String bleDeviceName, bleDeviceAddress;

    private ProgressBar progressBar;                                                                //Progress bar (indeterminate circular) to show that activity is busy connecting to BLE device
    //Service that handles all interaction with the Bluetooth radio and remote device
    private ByteArrayOutputStream transparentUartData = new ByteArrayOutputStream();                //Stores all the incoming byte arrays received from BLE device in bleService

    private boolean haveSuitableProbeConnected = false;

    private TextView textDeviceNameAndAddress;                                                      //To show device and status information on the screen
    private TextView textDeviceStatus;                                                      //To show device and status information on the screen
    private TextView labelFirmwareVersion;
    private TextView textFirmwareVersion;                                                      //To show device and status information on the screen
    private TextView labelCalibratedDate;                                                      //To show device and status information on the screen
    private TextView textCalibratedDate;                                                      //To show device and status information on the screen
    private Button buttonReconnect;

    private boolean forceUncalibratedMode = false;  // user can tap CalibrationDate label and disable calibration


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

    // ideal variables for current acceptance test
    // HACK - to save time, these values are currently hard coded in the app!
    // (but can be modified for the current session by the align stuff)
    // BUT  WHEN DOING AN ALIGN, NEED TO MANUALLY RECORD THE RESULTS TO ADD TO THE SOURCE CODE
    private String acceptLocation = "None";
    private boolean acceptIdealModified = false;  // to indicate if align has updated these values (they are no longer the normal values
    private double acceptIdeal50Az = 0;
    private double acceptIdeal50Dip = 0;
    private double acceptIdeal60Az = 0;
    private double acceptIdeal60Dip = 0;
    private double acceptIdeal30Az = 0;
    private double acceptIdeal30Dip = 0;

    // Alignment variables
    private static int alignSamplesPerReading = 5; //10; //5;  // now explicitly set to 10 in AlignStartButton OnClick handler
    private int alignCount = 0;
    private double alignDipTotal = 0;
    private double alignAzTotal = 0;

    private double newAlignReadingDipSum = 0;  // actually sum of all the dip readings
    private double newAlignReadingAzSum = 0;  // actually sum of all the dip readings
    private int newAlignCountRemaining = 0;  // when non zero triggers actions in processNewReading

    // acceptance variables
    private int acceptState = 0;  // 0=idle, 1-12=sample points

    private boolean acceptShowLiveError = false;
    private double acceptCurrentLiveDipError = 0;
    private double acceptCurrentLiveAzError = 0;
    private double acceptCurrentLiveRoll360Error = 0;
    private static final int acceptAcceptableLiveError = 4;

    private static int acceptSamplesPerReading = 5; //10; //5;   // currenlt overriden by average edit box
    private double newAcceptReadingDipSum = 0;  // actually sum of all the dip readings
    private double newAcceptReadingAzSum = 0;  // actually sum of all the az readings
    private int newAcceptCountRemaining = 0;  // when non zero triggers actions in processNewReading

    private double acceptDip[] = new double[12];
    private double acceptAz[] = new double[12];
    private double acceptRoll[] = new double[12];
    private double acceptRmsDip = 0;
    private double acceptRmsAz = 0;

    private Button takeMeasurement;


    int acceptTestPointDip[] = {-50, -50, -50, -50, -60, -60, -60, -60, -30, -30, -30, -30};
    int acceptTestPointRoll[] = {355, 80, 170, 260, 355, 80, 170, 260, 355, 80, 170, 260};

    int recordCount = 0;  // number of shots recorded so far
    String recordFilename = "SensorData-Auto-yyyyMMdd-HHmmss.csv";

    /******************************************************************************
     * A semi-terrible timer:
     */

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            seconds = (int) (millis / 1000);

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
                    try {
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
                    } catch (Exception e) {
                        Log.e(TAG, "exception thrown when changing direction of measurement: " + e);
                    }
                case 15:
                    collectionNumImg.setImageResource(R.drawable.s15);
                    break;
            }

            timerHandler.postDelayed(this, 1000);
        }
    };

    /******************************************************************************************************************
     * Methods for handling life cycle events of the activity.
     */

    private String mPrevDepth;
    private String mNextDepth;

    // ----------------------------------------------------------------------------------------------------------------
    // Activity launched
    // Invoked by launcher Intent
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);  //Call superclass (AppCompatActivity) onCreate method

        try {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  // PJH - try preventing phone from sleeping
            setContentView(R.layout.activity_take_measurements);                                                   //Show the main screen - may be shown briefly if we immediately start the scan activity
            Toolbar myToolbar = findViewById(R.id.toolbar);                                             //Get a reference to the Toolbar at the top of the screen
            setSupportActionBar(myToolbar);                                                             //Treat the toolbar as an Action bar (used for app name, menu, navigation, etc.)
            showAlert = new ShowAlertDialogs(this);                                             //Create the object that will show alert dialogs
            Log.i(TAG, "========== TakeMeasurement - onCreate ============");    // just as a separator in hte logcat window (to distinguish this run from the previous run)
            stateConnection = StateConnection.DISCONNECTED;                                             //Initial stateConnection when app starts
            stateApp = StateApp.STARTING_SERVICE;                                                       //Are going to start the BleService service
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { //Check whether we have location permission, required to scan
                stateApp = StateApp.REQUEST_PERMISSION;                                                 //Are requesting Location permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_CODE_ACCESS_LOC1); //Request fine location permission
            }
            if (stateApp == StateApp.STARTING_SERVICE) {                                                //Only start BleService if we already have location permission
                Intent bleServiceIntent = new Intent(this, BleService.class);             //Create Intent to start the BleService
                this.bindService(bleServiceIntent, bleServiceConnection, BIND_AUTO_CREATE);             //Create and bind the new service with bleServiceConnection object that handles service connect and disconnect
            }
            connectTimeoutHandler = new Handler(Looper.getMainLooper());                                //Create a handler for a delayed runnable that will stop the connection attempt after a timeout

            prevDepth = findViewById(R.id.previous_depth_txt);
            nextDepth = findViewById(R.id.next_depth_txt);
            directionButton = findViewById(R.id.direction_button);

            final Intent intent = getIntent();
            bleDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME); //mDeviceName
            bleDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS); //mDeviceAddress


            //Works
            try {
                initialDepth = MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().getInitialDepth();
                depthInterval = MainActivity.surveys.get(MainActivity.surveySize-1).getSurveyOptions().getDepthInterval();
                Log.e(TAG, "initial depth: " + initialDepth);
                Log.e(TAG, "depth interval: " + depthInterval);
            } catch (Exception e) {
                Log.e(TAG, "Exception thrown in getting initial depth and the depth interval");
            }


            try {
                nextDepth.setText("NEXT DEPTH: " + (initialDepth + depthInterval * measurementNum));
            } catch (Exception e) {
                Log.e(TAG, "Exception in setting the next depth: " + e);
            }


            connectionStatusText = findViewById(R.id.connection_status_text);
            connectionStatusImage = findViewById(R.id.connection_status_img);


            connectionStatusText.setText("Connected");
            connectionStatusImage.setImageResource(R.drawable.ready);
            collectionNumImg = findViewById(R.id.collection_num);

            takeMeasurement = findViewById(R.id.take_measurement_button);

            buttonLive = (Button) findViewById(R.id.probeOn);
            buttonLive.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Log.i(TAG, "PJH - processing Live Data button press");
                    if (buttonLive.getText().equals("PAUSE")) { //TODO - @ANNA remove ability to pause the session once started, makes life a pain
                        bleService.setProbeIdle();
                    } else {
                        bleService.setProbeMode(1); //TODO - @ANNA needs to also start the timer here
                        connectionStatusText.setText("Collecting Data");
                        connectionStatusImage.setImageResource(R.drawable.calibrating);

                        startTime = System.currentTimeMillis();
                        //@ANNA - ISSUE - user can press this button multiple times and reset the probe internal shot count.....
                    }
                }
            });
            initializeDisplay();
        } catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Activity started
    // Nothing needed here, all done in onCreate() and onResume()
    @Override
    public void onStart() {
        super.onStart();                                                                            //Call superclass (AppCompatActivity) onStart method
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Activity resumed
    // Register the receiver for Intents from the BleService
    @Override
    protected void onResume() {
        super.onResume();                                                                           //Call superclass (AppCompatActivity) onResume method
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

    // ----------------------------------------------------------------------------------------------------------------
    // Activity paused
    // Unregister the receiver for Intents from the BleService
    @Override
    protected void onPause() {
        super.onPause();                                                                            //Call superclass (AppCompatActivity) onPause method
        timerHandler.removeCallbacks(timerRunnable);
        try {
            unregisterReceiver(bleServiceReceiver);                                                     //Unregister receiver that was registered in onResume()
        } catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }

// PJH - somewhere here, need to ensure probe is idle (not in rolling shot mode)

    // ----------------------------------------------------------------------------------------------------------------
    // Activity stopped
    // Nothing needed here, all done in onPause() and onDestroy()
    @Override
    public void onStop() {
        super.onStop();                                                                             //Call superclass (AppCompatActivity) onStop method
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Activity is ending
    // Unbind from BleService and save the details of the BLE device for next time
    @Override
    protected void onDestroy() {
        super.onDestroy();                                                                          //Call superclass (AppCompatActivity) onDestroy method
        bleService.setProbeIdle();

        if (stateApp != StateApp.REQUEST_PERMISSION) {                                              //See if we got past the permission request
            unbindService(bleServiceConnection);                                                    //Unbind from the service handling Bluetooth
        }
    }

    /******************************************************************************************************************
     * Methods for handling menu creation and operation.
     */

    // ----------------------------------------------------------------------------------------------------------------
    // Options menu is different depending on whether connected or not and if we have permission to scan
    // Show Disconnect option if we are connected or show Connect option if not connected and have a device address
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        getMenuInflater().inflate(R.menu.menu_take_measurements, menu);                                      //Show the menu
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
        Intent intent = new Intent(this, MainActivity.class);
        Log.d(TAG, "Device name: " + bleDeviceName + ", Device Address: " + bleDeviceAddress);
        intent.putExtra(MainActivity.EXTRA_DEVICE_NAME, bleDeviceName);
        intent.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, bleDeviceAddress);
        intent.putExtra(MainActivity.EXTRA_PARENT_ACTIVITY, "TakeMeasurements");
        startActivity(intent);
    }

    /******************************************************************************************************************
     * Callback methods for handling Service connection events and Activity result events.
     */

    // ----------------------------------------------------------------------------------------------------------------
    // Callbacks for BleService service connection and disconnection
    private final ServiceConnection bleServiceConnection = new ServiceConnection() {                //Create new ServiceConnection interface to handle connection and disconnection

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {              //Service connects
            try {
                Log.i(TAG, "BleService connected");
                BleService.LocalBinder binder = (BleService.LocalBinder) service;                   //Get the Binder for the Service
                bleService = binder.getService();                                                   //Get a link to the Service from the Binder
                if (bleService.isBluetoothRadioEnabled()) {                                         //See if the Bluetooth radio is on
                    stateApp = StateApp.RUNNING;                                                    //Service is running and Bluetooth is enabled, app is now fully operational
                    //startBleScanActivity();                                                         //Launch the BleScanActivity to scan for BLE devices
                } else {                                                                              //Radio needs to be enabled
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

    // ----------------------------------------------------------------------------------------------------------------
    // Callback for Activities that return a result
    // We call BluetoothAdapter to turn on the Bluetooth radio and BleScanActivity to scan
    // and return the name and address of a Bluetooth LE device that the user chooses
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);                                    //Pass the activity result up to the parent method
        switch (requestCode) {                                                                      //See which Activity returned the result
            case REQ_CODE_ENABLE_BT: {
                if (resultCode == Activity.RESULT_OK) {                                             //User chose to enable Bluetooth
                    stateApp = StateApp.RUNNING;                                                    //Service is running and Bluetooth is enabled, app is now fully operational
                    // PJH we want to stay on the main screen, until user taps 'Scan'
                    //startBleScanActivity();                                                         //Start the BleScanActivity to do a scan for devices
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

    // ----------------------------------------------------------------------------------------------------------------
    // Callback for permission requests (new feature of Android Marshmallow requires runtime permission requests)
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.i(TAG, "PJH - onRequestPermissionsResult");

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {                                 //See if location permission was granted
            Log.i(TAG, "PJH - Location permission granted");
            stateApp = StateApp.STARTING_SERVICE;                                                   //Are going to start the BleService service
            Intent bleServiceIntent = new Intent(this, BleService.class);             //Create Intent to start the BleService
            this.bindService(bleServiceIntent, bleServiceConnection, BIND_AUTO_CREATE);             //Create and bind the new service to bleServiceConnection object that handles service connect and disconnect
        } else if (requestCode == REQ_CODE_ACCESS_LOC1) {                                             //Not granted so see if first refusal and need to ask again
            showAlert.showLocationPermissionDialog(new Runnable() {                                 //Show the AlertDialog that scan cannot be performed without permission
                @TargetApi(Build.VERSION_CODES.M)
                @Override
                public void run() {                                                                 //Runnable to execute when Continue button pressed
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_CODE_ACCESS_LOC2); //Ask for location permission again
                }
            });
        } else {
            //Permission refused twice so send user to settings
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

    // ----------------------------------------------------------------------------------------------------------------
    // Method to create and return an IntentFilter with Intent Actions that will be broadcast by the BleService to the bleServiceReceiver BroadcastReceiver
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
                    stateConnection = StateConnection.DISCOVERING;                                  //BleService automatically starts service discovery after connecting
                    updateConnectionState();                                                        //Update the screen and menus
                    break;
                }
                case BleService.ACTION_BLE_DISCONNECTED: {                                          //Have disconnected from BLE device
                    Log.d(TAG, "Received Intent ACTION_BLE_DISCONNECTED");
                    initializeDisplay();                                                            //Clear the temperature and accelerometer text and graphs
                    transparentUartData.reset();                                                    //Also clear any buffered incoming data
                    //if (stateConnection == StateConnection.CONNECTED) {                             //See if we were connected before
                    //    showAlert.showLostConnectionDialog(new Runnable() {                         //Show the AlertDialog for a lost connection
                    //        @Override
                    //        public void run() {                                                     //Runnable to execute if OK button pressed
                    //            startBleScanActivity();                                             //Launch the BleScanActivity to scan for BLE devices
                    //        }
                    //    });
                    //}
                    stateConnection = StateConnection.DISCONNECTED;                                 //Are disconnected

                    // but we want to reconnect automatically - TODO add retry counter then give up
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
                    //shouldnt happen in this activity but just in case:
                    bleService.parseBinaryCalibration();   // process thr calibration data just retrieved
                    bleService.setNotifications(true);   // PJH - HACK - find place where this write doesn't kill something else
                    haveSuitableProbeConnected = true;   // this enables the test stuff
                    break;
                }
                case BleService.ACTION_BLE_FETCH_CAL: {                                        //Have completed service discovery
                    Log.d(TAG, "PJH - Received Intent  ACTION_BLE_FETCH_CAL");
                    // PJH - HACK - should be in updateConnectionState
//                    textDeviceStatus.setText("Fetching calibration");                            //Show "Discovering"
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                    break;
                }
                case BleService.ACTION_BLE_NEW_DATA_RECEIVED: {                                     //Have received data (characteristic notification) from BLE device
                    Log.i(TAG, "PJH - Received Intent ACTION_BLE_NEW_DATA_RECEIVED");
//                        final byte[] newBytes = bleService.readFromTransparentUART();
//                        processIncomingData(newBytes);
                    processNewReading();
                    break;
                }
                //case BleService.ACTION_BLE_CHARACTERISTIC_READ: {                                     //Have received data (characteristic notification) from BLE device
                //    Log.d(TAG, "Received Intent ACTION_BLE_CHARACTERISTIC_READ");
                //    final byte[] newBytes = bleService.getCharacteristicReadResults();
                //    processCharacteristicRead(newBytes);
                //    break;
                //}
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
//            textDeviceStatus.setText("Not Connected");  // PJH - hack - shouldn't be here!
//            labelFirmwareVersion.setVisibility(View.INVISIBLE);
//            textFirmwareVersion.setText("");
//            textFirmwareVersion.setVisibility(View.INVISIBLE);

//            labelCalibratedDate.setVisibility(View.INVISIBLE);
//            textCalibratedDate.setText("");
//            textCalibratedDate.setVisibility(View.INVISIBLE);

            // PJH - not sure if setting to a null string will fully overwrite any existing numbers
//            textAccX.setText("");
//            textAccY.setText("");
//            textAccZ.setText("");
//            textAccMag.setText("");

//            textMagX.setText("");
//            textMagY.setText("");
//            textMagZ.setText("");
//            textMagMag.setText("");

//            textRoll.setText("");
//            textRoll360.setText("");
//            textDip.setText("");
//            textAz.setText("");
//            textAzErr.setText("");


//            textAlignCount.setText("0");
//            textAlignAvgDip.setText("");
//            textAlignAvgAz.setText("");

//            textAcceptComment.setText("Select Location and press Start\n(in -50 tray, at 0 roll, az should be 283.26)");
//            textAcceptDip.setText("");
//            textAcceptAz.setText("");

//            buttonAlignStart.setText("START");  // was having issues with first click not working
//            buttonAcceptStart.setText("START");

//            textAcceptResultAz.setText("");
//            textAcceptResultDip.setText("");

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

    int shotsCollected = 0;
    static LinkedList<Measurement> recordedShots = new LinkedList<>();


    // a new shot has been received, so retrieve and display it
    private void processNewReading() {
        Measurement newMeasurementBeingCollected = null;
        try {

            int count = 1;
            Log.e(TAG, "PROCESSING A NEW READING");

            double newVal[] = bleService.getLatestBoreshot(count);

            recordCount = bleService.getSensorDataCount();

//            Log.e(TAG, "RECORD COUNT: " + recordCount);
            if (bleService.isRecordingSensorDataEnabled() && (recordCount > 0)) {
//                buttonRecord.setText(String.format("Save: %d", recordCount));
            }

            Log.e(TAG, "Measurement name: " + String.valueOf(newVal[0]));
            DecimalFormat numberFormat = new DecimalFormat("#.0000");

            Date c = Calendar.getInstance().getTime();
            System.out.println("Current time => " + c);

            SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
            String date = df.format(c);

            SimpleDateFormat dfer = new SimpleDateFormat("h:mm:ss a");
            String time = dfer.format(Calendar.getInstance().getTime());

            String depthRecorded = (String) nextDepth.getText();
            depthRecorded = depthRecorded.replace("NEXT DEPTH: ", "");

            if (depthRecorded != null) {
                if (newVal[0] >= 283) {
                    newMeasurementBeingCollected = new Measurement(String.valueOf(newVal[0] - 283), date, time, //HACK
                            String.valueOf(numberFormat.format(newVal[10])), depthRecorded, String.valueOf(numberFormat.format(newVal[8])),
                            String.valueOf(numberFormat.format(newVal[7])), String.valueOf(numberFormat.format(newVal[9]))); //names a bit of a mouthful...
                } else {
                    newMeasurementBeingCollected = new Measurement(String.valueOf(newVal[0]), date, time, //HACK
                            String.valueOf(numberFormat.format(newVal[10])), depthRecorded, String.valueOf(numberFormat.format(newVal[8])),
                            String.valueOf(numberFormat.format(newVal[7])), String.valueOf(numberFormat.format(newVal[9]))); //names a bit of a mouthful...
                }
            } else {
                if (newVal[0] >= 283) {
                    newMeasurementBeingCollected = new Measurement(String.valueOf(newVal[0] - 283), date, time, //HACK
                            String.valueOf(numberFormat.format(newVal[10])), null, String.valueOf(numberFormat.format(newVal[8])),
                            String.valueOf(numberFormat.format(newVal[7])), String.valueOf(numberFormat.format(newVal[9]))); //names a bit of a mouthful...
                } else {
                    newMeasurementBeingCollected = new Measurement(String.valueOf(newVal[0]), date, time, //HACK
                            String.valueOf(numberFormat.format(newVal[10])), null, String.valueOf(numberFormat.format(newVal[8])),
                            String.valueOf(numberFormat.format(newVal[7])), String.valueOf(numberFormat.format(newVal[9]))); //names a bit of a mouthful...
                }
            }

            boolean accValid = true;
            double accMag = Math.sqrt(newVal[1] * newVal[1] + newVal[2] * newVal[2] + newVal[3] * newVal[3]);
            if (Math.abs(accMag - 1.0) > 0.03) {
                accValid = false;
            }

            double magMag = Math.sqrt(newVal[4] * newVal[4] + newVal[5] * newVal[5] + newVal[6] * newVal[6]);

            //
//            textMagX.setText(String.format("%7.4f", newVal[4])); //TODO - @ANNA - add back in as a record or smth
//            textMagY.setText(String.format("%7.4f", newVal[5]));
//            textMagZ.setText(String.format("%7.4f", newVal[6]));

            Log.i(TAG, "Roll: " + String.format("%7.4f", newVal[7]));
            Log.i(TAG, "Dip: " + String.format("%7.4f", newVal[8]));
            Log.i(TAG, "Azimuth: " + String.format("%7.4f", newVal[9]));
            //
            // Check if taking reading for alignment
            //
            if (newAlignCountRemaining > 0) {
                //Log.i(TAG, String.format("PJH - align sample %d (remaining)", newAlignCountRemaining));
                newAlignReadingDipSum += newVal[8];
                newAlignReadingAzSum += newVal[9];
                newAlignCountRemaining -= 1;
//                textAlignCountdown.setText(String.format("(%d)",newAlignCountRemaining));

                if (newAlignCountRemaining == 0) {
                    // ok, we just took te last sample for this reading
                    if (switchRecord.isChecked()) {  // do we have a race condition here
                        // we have recorded the last of the shots
                        bleService.stopRecordingSensorData();
                    }

                    // now update the totals
                    alignDipTotal += newAlignReadingDipSum;
                    alignAzTotal += newAlignReadingAzSum;
                    alignCount += 1;
                    // and update the display
//                    textAlignCount.setText(String.format("%d", alignCount));
//                    textAlignAvgDip.setText(String.format("%7.4f", (alignDipTotal / alignCount / alignSamplesPerReading)));
//                    textAlignAvgAz.setText(String.format("%7.4f", (alignAzTotal / alignCount / alignSamplesPerReading)));

                    ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);   // beep
                    toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);

//                    textAlignCountdown.setText("");
                }
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


                // if current dip is more than 4 degrees from the desired position, show live dip in red
                if (acceptCurrentLiveDipError < acceptAcceptableLiveError) {
//                    textDip.setTextColor(Color.BLACK);
                } else {
//                    textDip.setTextColor(Color.RED);
                }
                // if current roll is more than 4 degrees from the desired position, show live roll360 in red
                if (acceptCurrentLiveRoll360Error < 4) {
//                    textRoll360.setTextColor(Color.BLACK);
                } else {
//                    textRoll360.setTextColor(Color.RED);
                }

//                textAzErr.setText(String.format("(err:%7.4f)", acceptCurrentLiveAzError));
            } else {
                // this should never be required, but just in case...
                acceptCurrentLiveDipError = 0;
                acceptCurrentLiveRoll360Error = 0;
                acceptCurrentLiveAzError = 0;
            }

            //
            // Check if taking reading for acceptance test
            //
            if (newAcceptCountRemaining > 0) {
                //Log.i(TAG, String.format("PJH - accept sample %d (remaining)", newAcceptCountRemaining));
                newAcceptReadingDipSum += newVal[8];
                newAcceptReadingAzSum += newVal[9];
                newAcceptCountRemaining -= 1;
//                textAlignCountdown.setText(String.format("(%d)",newAcceptCountRemaining));  // yes, I know it is in the wrong area

                if (acceptState > 0) {
//                    textAcceptResultAz.setText("");
//                    textAcceptResultDip.setText("");
                }

                if (newAcceptCountRemaining == 0) {
                    // ok, we just took te last sample for this reading

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
                    // now update the totals
                    //alignDipTotal += newAlignReadingDipSum;
                    //alignAzTotal  += newAlignReadingAzSum;
                    //alignCount += 1;
                    // and update the display
                    //textAlignCount.setText(String.format("%d", alignCount));
//                    textAcceptDip.setText(String.format("%7.4f", (newAcceptReadingDipSum / acceptSamplesPerReading)));
//                    textAcceptAz.setText(String.format("%7.4f", (newAcceptReadingAzSum  / acceptSamplesPerReading)));

                    if (acceptState >= 13) {
                        // Ok, have taken all 12 readings
//                        textAcceptComment.setText(String.format("Test complete"));
//                        buttonAcceptStart.setText("START");

                        acceptShowLiveError = false;  // turn off error display and make sure everything back to normal
//                        textDip.setTextColor(Color.BLACK);
//                        textRoll360.setTextColor(Color.BLACK);
                        textRoll.setVisibility(View.VISIBLE);

                        bleService.setProbeIdle();
                        // now generate results file
                        String nowDate = new SimpleDateFormat("yyyy-MM-dd_hh-mm", Locale.getDefault()).format(new Date());
                        String safeBleDeviceAddress = bleDeviceAddress.replace(':', '-');
                        String safeBleDeviceName = bleDeviceName.replace(':', '-');
                        String filename = String.format("AcceptTest_%s_%s_%s.csv", nowDate, safeBleDeviceAddress, safeBleDeviceName);
                        Log.i(TAG, String.format("PJH - Accept filename: %s", filename));
                        writeAcceptReadingsToFile(getExternalFilesDir("/").getAbsolutePath() + "/" + filename);
                        // and do calculations
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

                            //sqDipDeltaSum += ((acceptDip[i]-acceptIdeal50Dip) * (acceptDip[i]-acceptIdeal50Dip) );
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
                            //sqAzDeltaSum += ((acceptAz[i]-acceptIdeal60Az) * (acceptAz[i]-acceptIdeal60Az) );
                            err = acceptAz[i] - acceptIdeal60Az;
                            if (err > 180) {
                                err -= 360;
                            }
                            if (err < -180) {
                                err += 360;
                            }
                            sqAzDeltaSum += (err * err);

                            //sqDipDeltaSum += ((acceptDip[i]-acceptIdeal60Dip) * (acceptDip[i]-acceptIdeal60Dip) );
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
                            //sqAzDeltaSum += ((acceptAz[i]-acceptIdeal30Az) * (acceptAz[i]-acceptIdeal30Az) );
                            err = acceptAz[i] - acceptIdeal30Az;
                            if (err > 180) {
                                err -= 360;
                            }
                            if (err < -180) {
                                err += 360;
                            }
                            sqAzDeltaSum += (err * err);

                            //sqDipDeltaSum += ((acceptDip[i]-acceptIdeal30Dip) * (acceptDip[i]-acceptIdeal30Dip) );
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

//                        textAcceptResultAz.setText(String.format("%5.3f", acceptRmsAz));     // show the result (summary) of the test
//                        textAcceptResultDip.setText(String.format("%5.3f", acceptRmsDip));
//                        textAcceptComment.setText(String.format("Test complete\nPress Start to begin a new test."));
                        acceptState = 0;
                    } else {
                        // more readings to take...
//                        textAcceptComment.setText(String.format("%dof12 Place probe in '%d' tray, adjust roll to %d degrees, step back and press 'Take Reading'",
//                                acceptState, acceptTestPointDip[acceptState - 1], acceptTestPointRoll[acceptState - 1]));
                    }
                }
            }


        } catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }

        recordedShots.add(newMeasurementBeingCollected);

        try {
            shotsCollected++;
            if (shotsToCollect.size() > shotsCollected) {
                //collect shot
                try {
                    int shotToCollect = shotsToCollect.get(shotsCollected);
                    Log.e(TAG, "Getting shot for: " + shotToCollect);
                    bleService.setShotRequest(shotToCollect);
                } catch (Exception e) {
                    Log.e(TAG, "Exception thrown in first part: " + e);
                }
            } else {
                Log.i(TAG, "All shots collected");
                Log.e(TAG, "RecordedShots: " + recordedShots.size());
                try {
                    for (int i = 0; i < recordedShots.size(); i++) {
                        recordedShots.get(i).printMeasurement();
                    }
                    //NEED TO ACCESS RECORDEDSHOTS IN THE VIEW MEASUREMENT ACTIVITY
                    Intent intent = new Intent(this, ViewMeasurements.class);
                    intent.putExtra(ViewMeasurements.EXTRA_DEVICE_NAME, bleDeviceName);
                    intent.putExtra(ViewMeasurements.EXTRA_DEVICE_ADDRESS, bleDeviceAddress);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Exception thrown: " + e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown getting next shot: " + e);
        }
    }


    public void writeAcceptReadingsToFile(final String filename) {
        File file = new File(filename);
        try {

            // generate the original acceptance report
            String csvBody = "";
            csvBody += "Reading,Roll,Dip,Azimuth\n";
            for (int i = 0; i < 12; i++) {
                csvBody += String.format("%d,%f,%f,%f\n", i + 1, acceptRoll[i], acceptDip[i], acceptAz[i]);
            }
            csvBody += "\n\n\n";

            csvBody += String.format("Location, %s\n\n", acceptLocation);

            csvBody += String.format("Tray, IdealDip, IdealAzimuth\n");
            csvBody += String.format("-60, %f, %f\n", acceptIdeal60Dip, acceptIdeal60Az);
            csvBody += String.format("-50, %f, %f\n", acceptIdeal50Dip, acceptIdeal50Az);
            csvBody += String.format("-30, %f, %f\n", acceptIdeal30Dip, acceptIdeal30Az);
            csvBody += "\n\n\n";

            FileOutputStream stream = new FileOutputStream(file);
            stream.write(csvBody.getBytes());
            stream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void writeSensorDataToFile(final String filename) {
        File file = new File(filename);
        try {
            // generate the original SensorData report
            String csvBody = "";

            Log.i(TAG, "PJH - about to save");
            //buttonRecord.setText("Saving data");
            //bleService.stopRecordingSensorData();
            recordCount = bleService.getSensorDataCount();

            //Log.i(TAG, String.format("PJH - about to save %d sensorData records", recordCount);
            Log.i(TAG, "PJH - about to get header");
            String header = bleService.sensorDataReportGetReportHeader();
            csvBody += header + "\n";

            String record;
            for (int i = 0; i < recordCount; i++) {
                record = bleService.sensorDataReportGetReportLine(i);
                csvBody += record + "\n";
            }


            FileOutputStream stream = new FileOutputStream(file);
            stream.write(csvBody.getBytes());
            stream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


//    private void processIncomingData(byte[] newBytes) {
//        try {
//            /*
//            transparentUartData.write(newBytes);                                                    //Add new data to any previous bytes left over
//            boolean search = (transparentUartData.size() >= 8);                                     //Need at least 8 bytes for a complete message
//            while (search) {                                                                        //Keep searching until the code has worked through all the bytes
//                final byte[] allBytes = transparentUartData.toByteArray();                          //Put all the bytes into a byte array
//                search = false;                                                                     //Assume there is no termination byte, and update to repeat the search if we find one
//                for (int i = 0; i < allBytes.length; i++) {                                         //Loop through all the bytes
//                    if (allBytes[i] == (byte)']') {                                                 // to look for termination byte
//                        byte[] newLine = Arrays.copyOf(allBytes, i);                                //Get all the bytes up to the termination byte
//                        byte[] leftOver = Arrays.copyOfRange(allBytes, i + 1, allBytes.length); //Get all the remaining bytes after the termination byte
//                        transparentUartData.reset();                                                //Clear out the original data
//                        transparentUartData.write(leftOver);                                        // and save the remaining bytes for later
//                        final String newLineStr = new String(newLine, StandardCharsets.UTF_8);      //Create a string from the bytes up to the termination byte
//                        int ledIndex = newLineStr.indexOf("L02");                                   //Search for the text for the LED state
//                        int tempIndex = newLineStr.indexOf("T04");                                  //Search for the text for the temperature reading
//                        int accelIndex = newLineStr.indexOf("X0C");                                 //Search for the text for the accelerometer readings
//                        //Got LED packet
//                        if (ledIndex != -1) {                                                       //See if the LED text was found
//                            if (newLineStr.charAt(ledIndex + 3) == '0') {                           //See if the status is for the green LED (LED0)
//                                if (newLineStr.charAt(ledIndex + 4) == '0' && switchGreenLed.isChecked()) { //See if the LED is off and should be on
//                                    bleService.writeToTransparentUART("[0L0201]".getBytes());       //Write command to the Transparent UART to light the green LED (LED0)
//                                }
//                                else if (newLineStr.charAt(ledIndex + 4) == '1' && !switchGreenLed.isChecked()) { //See if the LED is on and should be off
//                                    bleService.writeToTransparentUART("[0L0200]".getBytes());       //Write command to the Transparent UART to turn off the green LED (LED0)
//                                }
//                            }
//                            else if (newLineStr.charAt(ledIndex + 3) == '1') {                      //See if the status is for the red LED (LED1)
//                                if (newLineStr.charAt(ledIndex + 4) == '0' && switchRedLed.isChecked()) { //See if the LED is off and should be on
//                                    bleService.writeToTransparentUART("[0L0211]".getBytes());       //Write command to the Transparent UART to light the red LED (LED1)
//                                }
//                                else if (newLineStr.charAt(ledIndex + 4) == '1' && !switchRedLed.isChecked()) { //See if the LED is on and should be off
//                                    bleService.writeToTransparentUART("[0L0210]".getBytes());       //Write command to the Transparent UART to turn off the red LED (LED1)
//                                }
//                            }
//                        }
//                        //Got temperature packet
//                        if (tempIndex != -1) {                                                      //See if the temperature text was found
//                            int rawTemp = (Character.digit(newLineStr.charAt(tempIndex + 5), 16) << 12) //Pull out the ascii characters for the temperature reading
//                                    + (Character.digit(newLineStr.charAt(tempIndex + 6), 16) << 8)
//                                    + (Character.digit(newLineStr.charAt(tempIndex + 3), 16) << 4)
//                                    + Character.digit(newLineStr.charAt(tempIndex + 4), 16);
//                            double temperature = (rawTemp > 32767) ? ((double)(rawTemp - 65536)) / 16 : ((double)(rawTemp)) / 16; //Convert unsigned left shifted to signed
//                            textTemperature.setText(String.format("%s%s", temperature, getString(R.string.degrees_celcius)));     //Display the temperature on the screen
//                        }
//                        //Got accelerometer packet
//                        if (accelIndex != -1) {                                                     //See if the accelerometer text was found
//                            int rawX = (Character.digit(newLineStr.charAt(accelIndex + 5), 16) << 12) //Pull out the ascii characters for the accelerometer X readings
//                                    + (Character.digit(newLineStr.charAt(accelIndex + 6), 16) << 8)
//                                    + (Character.digit(newLineStr.charAt(accelIndex + 3), 16) << 4)
//                                    + Character.digit(newLineStr.charAt(accelIndex + 4), 16);
//                            int accelX = (rawX > 2047) ? (rawX - 4096) : rawX;                      //Convert unsigned 12-bit to signed
//                            int rawY = (Character.digit(newLineStr.charAt(accelIndex + 9), 16) << 12) //Pull out the ascii characters for the accelerometer Y readings
//                                    + (Character.digit(newLineStr.charAt(accelIndex + 10), 16) << 8)
//                                    + (Character.digit(newLineStr.charAt(accelIndex + 7), 16) << 4)
//                                    + Character.digit(newLineStr.charAt(accelIndex + 8), 16);
//                            int accelY = (rawY > 2047) ? (rawY - 4096) : rawY;                      //Convert unsigned 12-bit to signed
//                            int rawZ = (Character.digit(newLineStr.charAt(accelIndex + 13), 16) << 12) //Pull out the ascii characters for the accelerometer Z readings
//                                    + (Character.digit(newLineStr.charAt(accelIndex + 14), 16) << 8)
//                                    + (Character.digit(newLineStr.charAt(accelIndex + 11), 16) << 4)
//                                    + Character.digit(newLineStr.charAt(accelIndex + 12), 16);
//                            int accelZ = (rawZ > 2047) ? (rawZ - 4096) : rawZ;                      //Convert unsigned 12-bit to signed
//                            textAccelerometerX.setText(String.format("%s%d", getString(R.string.accelerometer_x), accelX));        //Display the accelerometer X readings
//                            textAccelerometerY.setText(String.format("%s%d", getString(R.string.accelerometer_y), accelY));        //Display the accelerometer Y readings
//                            textAccelerometerZ.setText(String.format("%s%d", getString(R.string.accelerometer_z), accelZ));        //Display the accelerometer Z readings
//                            accelXSeries.appendData(new DataPoint(accelGraphHorizontalPoint, accelX), true, 100); //Update the graph with the new accelerometer readings
//                            accelYSeries.appendData(new DataPoint(accelGraphHorizontalPoint, accelY), true, 100);
//                            accelZSeries.appendData(new DataPoint(accelGraphHorizontalPoint, accelZ), true, 100);
//                            accelGraphHorizontalPoint += 1d;                                        //Increment the index for the next point on the horizontal axis of the graph
//                        }
//                        search = true;                                                              //Found a termination byte during this search so repeat the search to see if there are any more
//                        break;
//                    }
//                }
//
//
//            }
//        */
//        } catch (Exception e) {
//            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
//        }
//    }

    /******************************************************************************************************************
     * Methods for scanning, connecting, and showing event driven dialogs
     */

    // ----------------------------------------------------------------------------------------------------------------
    // Start the BleScanActivity that scans for available Bluetooth devices and lets the user select one
    private void startBleScanActivity() {
        try {
            bleService.invalidateCalibration();  // to force a full connect and reread of the calibration data
            if (stateApp == StateApp.RUNNING) {                                                     //Only do a scan if we got through startup (permission granted, service started, Bluetooth enabled)
                stateConnection = StateConnection.DISCONNECTING;                                    //Are disconnecting prior to doing a scan
                haveSuitableProbeConnected = false;
                bleService.disconnectBle();                                                         //Disconnect an existing Bluetooth connection or cancel a connection attempt
                final Intent bleScanActivityIntent = new Intent(TakeMeasurements.this, BleScanActivity.class); //Create Intent to start the BleScanActivity
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
                        //if (bleDeviceName != null) {                                                //See if there is a device name
                        //    textDeviceNameAndAddress.setText(bleDeviceName);                        //Display the name
                        //} else {
                        //    textDeviceNameAndAddress.setText(R.string.unknown_device);                     //or display "Unknown Device"
                        //}
                        //if (bleDeviceAddress != null) {                                             //See if there is an address
                        //    textDeviceNameAndAddress.append(" - " + bleDeviceAddress);              //Display the address
                        //}
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

    LinkedList<Double> measurementTime = new LinkedList<>();
    public static LinkedList<double[]> savedProbeData = new LinkedList<double[]>();
    double measurementStartTime = 0;
    boolean collectingDataMeasurement = false;


    //when "withdrawing" the probe, it must be connected so that data can be retreived
    LinkedList<Integer> shotsToCollect = new LinkedList<>(); //need to transfer time into shots

    public void withdrawClick(View view) {
        try {
            int shotInterval = bleService.getShotInterval();
            for (int i = 0; i < measurementTime.size(); i++) {
                shotsToCollect.add((int) (measurementTime.get(i) / shotInterval));
            }
            Log.i(TAG, "Shots to collect: " + shotsToCollect.toString());

            int shotToCollect = shotsToCollect.get(0);
            Log.e(TAG, "Getting shot for: " + shotToCollect);
            bleService.setShotRequest(shotToCollect);

            bleService.setProbeIdle(); //otherwise the timer gets mucky
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown in withdraw click: " + e);
        }
    }

    public void measurementClick(View view) throws InterruptedException {
        elapsedTime = System.currentTimeMillis() - startTime;
        elapsedSeconds = elapsedTime / 1000;
        secondsDisplay = elapsedSeconds % 60;
        elapsedMinutes = elapsedSeconds / 60;

        startTime = System.currentTimeMillis();
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.postDelayed(timerRunnable, 0);

//        if (!collectingDataMeasurement) {
        Log.i(TAG, "Time added to measurement collection list: " + elapsedSeconds);
        measurementTime.add((double) elapsedSeconds); //add this elapsed time to a linked list
        measurementStartTime = elapsedSeconds;
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
}