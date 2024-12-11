package com.work.libtest;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
//import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
//import android.widget.ProgressBar;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
//import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

//import com.jjoe64.graphview.GraphView;
//import com.jjoe64.graphview.series.DataPoint;
//import com.jjoe64.graphview.series.LineGraphSeries;

import com.work.libtest.Preferences.Preferences;
import com.work.libtest.Preferences.PreferencesActivity;
import com.work.libtest.SurveyOptions.SurveyOptions;
import com.work.libtest.SurveyOptions.SurveyOptionsActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
//import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
//import java.util.ArrayList;
import java.util.ArrayList;
import java.util.Date;
//import java.util.List;
import java.util.Locale;

public class CoreMain extends AppCompatActivity {
    private final static String TAG = CoreMain.class.getSimpleName();                        //Activity name for logging messages on the ADB

    public static final String EXTRA_WHITE_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRA_WHITE_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRA_BLACK_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRA_BLACK_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRA_CONNECTION_STATUS = "CONNECTION_STATUS";
    public static final String EXTRA_PARENT_ACTIVITY = "Parent_Activity";

    private static final int REQ_CODE_ENABLE_BT = 1;                                            //Codes to identify activities that return results such as enabling Bluetooth
    private static final int REQ_CODE_SCAN_ACTIVITY = 2;                                            //or scanning for bluetooth devices
    private static final int REQ_CODE_ACCESS_LOC1 = 3;                                            //or requesting location access.
    private static final int REQ_CODE_ACCESS_LOC2 = 4;                                            //or requesting location access a second time.
    private static final long CONNECT_TIMEOUT = 10000;                                        //Length of time in milliseconds to try to connect to a device

    private BleService bleService;                                                                  //Service that handles all interaction with the Bluetooth radio and remote device
    private ByteArrayOutputStream transparentUartData = new ByteArrayOutputStream();                //Stores all the incoming byte arrays received from BLE device in bleService
    private ShowAlertDialogs showAlert;                                                             //Object that creates and shows all the alert pop ups used in the app
    private Handler connectTimeoutHandler;                                                          //Handler to provide a time out if connection attempt takes too long
    public static String bleDeviceNameWhite;
    public static String bleDeviceAddressWhite;
    public static String bleDeviceNameBlack;
    public static String bleDeviceAddressBlack;

    private boolean haveSuitableProbeConnected = false;

    private TextView textDeviceNameAndAddress;                                                      //To show device and status information on the screen
    private TextView textDeviceStatus;

    private ImageView blackProbeStatusImg;
//    private LinearLayout WhiteProbeContainer; //the second container for a probe, used if the app is in dual mode

    private boolean forceUncalibratedMode = false;  // user can tap CalibrationDate label and disable calibration

    int recordCount = 0;  // number of shots recorded so far
    String recordFilename = "SensorData-Auto-yyyyMMdd-HHmmss.csv";

    //Switches to control and show LED state
    private enum StateConnection {DISCONNECTED, CONNECTING, DISCOVERING, CONNECTED, DISCONNECTING}  //States of the Bluetooth connection

    private StateConnection stateConnectionBlack;
    private StateConnection stateConnectionWhite;   //State of Bluetooth connection

    private enum StateApp {STARTING_SERVICE, REQUEST_PERMISSION, ENABLING_BLUETOOTH, RUNNING}       //States of the app

    private StateApp stateApp;

    public static Preferences preferences = new Preferences();

    //Survey information stuff:
    public static int surveySize = 0;
    public static ArrayList<Survey> surveys = new ArrayList<Survey>();
    public static int surveyNum = 0;

    //survey information details
    private TextView HoleIDDisplayTxt;
    private TextView OperatorNameDisplayTxt;

    public int seconds;
    private long startTime = 0;

    private TextView blackTextDeviceNameAndAddress;
    private TextView whiteTextDeviceNameAndAddress;

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            seconds = (int) (millis / 1000);

            //Check black device connection
            if (bleDeviceNameBlack != null && bleDeviceAddressBlack != null) {
                if (stateConnectionBlack == CoreMain.StateConnection.DISCONNECTED || stateConnectionBlack == CoreMain.StateConnection.DISCONNECTING) {
                    if ((bleDeviceNameBlack != null) && (bleDeviceAddressBlack != null) /**&& bleService.isCalibrated()**/) {                                                //See if there is a device name
                        // attempt a reconnection
                        stateConnectionBlack = CoreMain.StateConnection.CONNECTING;                               //Got an address so we are going to start connecting
                        connectWithAddress(bleDeviceAddressBlack);                                       //Initiate a connection
                    }

                    updateConnectionState();
                }
            }
            timerHandler.postDelayed(this, 1000);

            //Check White device connection
            if (bleDeviceNameWhite != null && bleDeviceAddressWhite != null) {
                if (stateConnectionWhite == CoreMain.StateConnection.DISCONNECTED || stateConnectionWhite == CoreMain.StateConnection.DISCONNECTING) {
                    if ((bleDeviceNameWhite != null) && (bleDeviceAddressWhite != null) /**&& bleService.isCalibrated()**/) {                                                //See if there is a device name
                        // attempt a reconnection
                        stateConnectionWhite = CoreMain.StateConnection.CONNECTING;                               //Got an address so we are going to start connecting
                        connectWithAddress(bleDeviceAddressWhite);                                       //Initiate a connection
                    }
                    updateConnectionState();
                }
            }
            timerHandler.postDelayed(this, 1000);
        }
    };

    /******************************************************************************************************************
     * Methods for handling life cycle events of the activity.
     */

    // ----------------------------------------------------------------------------------------------------------------
    // Activity launched
    // Invoked by launcher Intent
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);  //Call superclass (AppCompatActivity) onCreate method

        startTime = System.currentTimeMillis();
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.postDelayed(timerRunnable, 0);
        try {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  // PJH - try preventing phone from sleeping
            setContentView(R.layout.activity_core_main);                                                   //Show the main screen - may be shown briefly if we immediately start the scan activity             //Hide the circular progress bar
            showAlert = new ShowAlertDialogs(this);                                             //Create the object that will show alert dialogs
            Log.i(TAG, "PJH - ========== onCreate ============");    // just as a separator in hte logcat window (to distinguish this run from the previous run)
            stateConnectionBlack = StateConnection.DISCONNECTED;                                             //Initial stateConnection when app starts
            stateConnectionWhite = StateConnection.DISCONNECTED;                                             //Initial stateConnection when app starts
            stateApp = StateApp.STARTING_SERVICE;                                                       //Are going to start the BleService service
            Log.i(TAG, "PJH - about to request FINE permission");
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { //Check whether we have location permission, required to scan
                stateApp = StateApp.REQUEST_PERMISSION;                                                 //Are requesting Location permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_CODE_ACCESS_LOC1); //Request fine location permission
            }
            if (stateApp == StateApp.STARTING_SERVICE) {                                                //Only start BleService if we already have location permission
                Intent bleServiceIntent = new Intent(this, BleService.class);             //Create Intent to start the BleService
                this.bindService(bleServiceIntent, bleServiceConnection, BIND_AUTO_CREATE);             //Create and bind the new service with bleServiceConnection object that handles service connect and disconnect
            }
            Log.i(TAG, "PJH - ========== onCreate2 ============");    // just as a separator in hte logcat window (to distinguish this run from the previous run)
            connectTimeoutHandler = new Handler(Looper.getMainLooper());                                //Create a handler for a delayed runnable that will stop the connection attempt after a timeout

            HoleIDDisplayTxt = findViewById(R.id.HoleIDDisplayTxt);
            OperatorNameDisplayTxt = findViewById(R.id.OperatorNameDisplayTxt);

            blackTextDeviceNameAndAddress = findViewById(R.id.BlackProbeTxt);                     //Get a reference to the TextView that will display the device name and address
            whiteTextDeviceNameAndAddress = findViewById(R.id.WhiteProbeTxt);
            textDeviceStatus = findViewById(R.id.BlackProbeStatusTxt);                     //Get a reference to the TextView that will display the device name and address
            blackProbeStatusImg = findViewById(R.id.BlackProbeStatusImg);

            try {
                if (surveys.size() > 0) { //array has begun to be populated
                    if (surveys.get(0).getSurveyOptions().getHoleID() != null && surveys.get(0).getSurveyOptions() != null) {
                        HoleIDDisplayTxt.setText(surveys.get(0).getSurveyOptions().getHoleID());
                        OperatorNameDisplayTxt.setText(surveys.get(0).getSurveyOptions().getOperatorName());

                    } else {
                        HoleIDDisplayTxt.setText("Not set");
                        OperatorNameDisplayTxt.setText("Not set");
                    }
                } else {
                    HoleIDDisplayTxt.setText("Not set");
                    OperatorNameDisplayTxt.setText("Not set");
                }
            } catch (Exception e) {
                Log.d(TAG, "Exception thrown: " + e);
            }
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
        super.onResume();

        //Call superclass (AppCompatActivity) onResume method
        try {
            registerReceiver(bleServiceReceiver, bleServiceIntentFilter());                         //Register receiver to handles events fired by the BleService
            if (bleService != null && !bleService.isBluetoothRadioEnabled()) {                      //Check if Bluetooth radio was turned off while app was paused
                if (stateApp == StateApp.RUNNING) {                                                 //Check that app is running, to make sure service is connected
                    stateApp = StateApp.ENABLING_BLUETOOTH;                                         //Are going to request user to turn on Bluetooth
                    stateConnectionBlack = StateConnection.DISCONNECTED;                                 //Must be disconnected if Bluetooth is off
                    stateConnectionWhite = StateConnection.DISCONNECTED;                                 //Must be disconnected if Bluetooth is off
                    startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQ_CODE_ENABLE_BT); //Start the activity to ask the user to grant permission to enable Bluetooth
                    Log.i(TAG, "Requesting user to enable Bluetooth radio");
                }
            }
            final Intent intent = getIntent();
            try {
                String parentActivityValue = intent.getStringExtra(EXTRA_PARENT_ACTIVITY);
                Log.e(TAG, intent.getStringExtra(EXTRA_PARENT_ACTIVITY) + "," + intent.getStringExtra(EXTRA_BLACK_DEVICE_NAME) + "," + intent.getStringExtra(EXTRA_BLACK_DEVICE_ADDRESS));
                if (parentActivityValue != null) {
                    if (parentActivityValue.equals("SurveyOptions") || parentActivityValue.equals("ProbeDetails") || parentActivityValue.equals("TakeMeasurements") || parentActivityValue.equals("AllSurveyOptions") || parentActivityValue.equals("Preferences")) {
                        stateApp = StateApp.RUNNING;
                        bleDeviceAddressBlack = intent.getStringExtra(EXTRA_BLACK_DEVICE_ADDRESS);
                        bleDeviceNameBlack = intent.getStringExtra(EXTRA_BLACK_DEVICE_NAME);
                        bleDeviceAddressWhite = intent.getStringExtra(EXTRA_WHITE_DEVICE_ADDRESS);
                        bleDeviceNameWhite = intent.getStringExtra(EXTRA_WHITE_DEVICE_NAME);

                        //set black probe details
                        if (bleDeviceNameBlack != null) {
                            blackTextDeviceNameAndAddress.setText(bleDeviceNameBlack);
                        } else {
                            blackTextDeviceNameAndAddress.setText(R.string.unknown_device);
                        }
                        if (bleDeviceAddressBlack != null) {
                            blackTextDeviceNameAndAddress.append(" - " + bleDeviceAddressBlack);
                        }

                        if (bleDeviceAddressBlack == null) {
                            stateConnectionBlack = StateConnection.DISCONNECTED;
                        } else {
                            stateConnectionBlack = StateConnection.CONNECTED;
                        }

                        //set white probe details
                        if (bleDeviceNameWhite != null) {
                            whiteTextDeviceNameAndAddress.setText(bleDeviceNameWhite);
                        } else {
                            whiteTextDeviceNameAndAddress.setText(R.string.unknown_device);
                        }
                        if (bleDeviceAddressWhite != null) {
                            whiteTextDeviceNameAndAddress.append(" - " + bleDeviceAddressWhite);
                        }

                        if (bleDeviceAddressWhite == null) {
                            stateConnectionWhite = StateConnection.DISCONNECTED;
                        } else {
                            stateConnectionWhite = StateConnection.CONNECTED;
                        }
                        updateConnectionState();
                    } else {
                        Log.e(TAG, "Impossible"); //error as these are the only activities which lead back to the main activity
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception thrown in getting intent: " + e);
            }

            if ((bleDeviceNameBlack != null) && (bleDeviceAddressBlack != null)) {                                                //See if there is a device name
                // attempt a reconnection
                stateConnectionBlack = StateConnection.CONNECTING;                               //Got an address so we are going to start connecting
                connectWithAddress(bleDeviceAddressBlack);                                       //Initiate a connection
            }
            if ((bleDeviceNameWhite != null) && (bleDeviceAddressWhite != null)) {                                                //See if there is a device name
                // attempt a reconnection
                stateConnectionWhite = StateConnection.CONNECTING;                               //Got an address so we are going to start connecting
                connectWithAddress(bleDeviceAddressWhite);                                       //Initiate a connection
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
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable); //Call superclass (AppCompatActivity) onPause method
        try {
            unregisterReceiver(bleServiceReceiver);                                                     //Unregister receiver that was registered in onResume()
        } catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }

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

        // TODO - need to check...
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
        getMenuInflater().inflate(R.menu.core_menu, menu);                                      //Show the menu
        return true;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Menu item selected
    // Scan, connect or disconnect, etc.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            switch (item.getItemId()) {
                case R.id.menu_disconnect: {                                                        //Menu option Disconnect chosen
                    stateConnectionBlack = StateConnection.DISCONNECTING;                                //StateConnection is used to determine whether disconnect event should trigger a popup to reconnect
                    stateConnectionWhite = StateConnection.DISCONNECTING;
                    updateConnectionState();                                                        //Update the screen and menus
                    bleService.disconnectBle();                                                     //Ask the BleService to disconnect from the Bluetooth device
                    return true;
                }
                case R.id.menu_help: {                                                              //Menu option Help chosen
                    showAlert.showHelpMenuDialog(this.getApplicationContext());                     //Show the AlertDialog that has the Help text
                    return true;
                }
                case R.id.menu_about: {                                                             //Menu option About chosen
                    showAlert.showAboutMenuDialog(this);                                    //Show the AlertDialog that has the About text
                    return true;
                }
                case R.id.menu_exit: {                                                              //Menu option Exit chosen
                    showAlert.showExitMenuDialog(new Runnable() {                                   //Show the AlertDialog that has the Exit warning text
                        @Override
                        public void run() {                                                         //Runnable to execute if OK button pressed
                            if (bleService != null) {                                               //Check if the service is running
                                bleService.disconnectBle();                                         //Ask the BleService to disconnect in case there is a Bluetooth connection
                            }
                            onBackPressed();                                                        //Exit by going back - ultimately calls finish()
                        }
                    });
                    return true;
                }
                case R.id.menu_preferences: {
                    //start preferences activity
                    Intent intent = new Intent(this, PreferencesActivity.class);
                    intent.putExtra(PreferencesActivity.EXTRA_BLACK_DEVICE_NAME, bleDeviceNameBlack);
                    intent.putExtra(PreferencesActivity.EXTRA_BLACK_DEVICE_ADDRESS, bleDeviceAddressBlack);
                    intent.putExtra(PreferencesActivity.EXTRA_WHITE_DEVICE_NAME, bleDeviceNameBlack);
                    intent.putExtra(PreferencesActivity.EXTRA_WHITE_DEVICE_ADDRESS, bleDeviceAddressBlack);
                    startActivity(intent);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
        return super.onOptionsItemSelected(item);                                                   //No valid menu item selected so pass up to superclass method
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
                //pretty sure we dont need this here as the activity does not directly link to the bleScanCore activity
//                showAlert.dismiss();
//                if (resultCode == Activity.RESULT_OK) {                                             //User chose a Bluetooth device to connect
//                    stateApp = StateApp.RUNNING;                                                    //Service is running and Bluetooth is enabled, app is fully operational
//                    bleDeviceAddress = intent.getStringExtra(BleScanActivity.EXTRA_SCAN_ADDRESS);   //Get the address of the BLE device selected in the BleScanActivity
//                    bleDeviceName = intent.getStringExtra(BleScanActivity.EXTRA_SCAN_NAME);         //Get the name of the BLE device selected in the BleScanActivity
//
//                    if (!boreMode) { //if in core mode we want to print the colour of the probe that has just been selected
//                        String probeColor = intent.getStringExtra(BleScanActivity.EXTRA_PROBE_COLOR);
//                        Log.e(TAG, "Probe Colour: " + probeColor);
//                    }
//
//
//                    if (bleDeviceName != null) {                                                //See if there is a device name
//                        textDeviceNameAndAddress.setText(bleDeviceName);                        //Display the name
//                    } else {
//                        if (boreMode) {
//                            textDeviceNameAndAddress.setText("Probe");                     //or display "Unknown Device"
//                        } else {
//                            textDeviceNameAndAddress.setText("Black Probe");                     //or display "Unknown Device"
//                        }
//                    }
//                    if (bleDeviceAddress != null) {                                             //See if there is an address
//                        textDeviceNameAndAddress.append(" - " + bleDeviceAddress);              //Display the address
//                    }
//
//                    if (bleDeviceAddress == null) {                                                 //Check whether we were given a device address
//                        stateConnection = StateConnection.DISCONNECTED;                             //No device address so not connected and not going to connect
//                    } else {
//                        stateConnection = StateConnection.CONNECTING;                               //Got an address so we are going to start connecting
//                        connectWithAddress(bleDeviceAddress);                                       //Initiate a connection
//                    }
//                } else {                                                                            //Did not get a valid result from the BleScanActivity
//                    stateConnection = StateConnection.DISCONNECTED;                                 //No result so not connected and not going to connect
//                }
//                updateConnectionState();                                                            //Update the connection state on the screen and menus
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
                    stateConnectionBlack = StateConnection.DISCOVERING;                                  //BleService automatically starts service discovery after connecting
                    blackProbeStatusImg.setImageResource(R.drawable.ready); //HOW DO I ALSO DO THIS FOR WHITE????
                    updateConnectionState();                                                        //Update the screen and menus
                    break;
                }
                case BleService.ACTION_BLE_DISCONNECTED: {                                          //Have disconnected from BLE device
                    Log.d(TAG, "Received Intent ACTION_BLE_DISCONNECTED");
                    initializeDisplay();                                                            //Clear the temperature and accelerometer text and graphs
                    transparentUartData.reset();                                                    //Also clear any buffered incoming data
                    stateConnectionBlack = StateConnection.DISCONNECTED;
                    blackProbeStatusImg.setImageResource(R.drawable.unconnected);

                    // but we want to reconnect automatically - TODO add retry counter then give up
                    if ((bleDeviceNameBlack != null) && (bleDeviceAddressBlack != null) && bleService.isCalibrated()) {                                                //See if there is a device name
                        // attempt a reconnection
                        stateConnectionBlack = StateConnection.CONNECTING;                               //Got an address so we are going to start connecting
                        connectWithAddress(bleDeviceAddressBlack);                                       //Initiate a connection
                    }

                    updateConnectionState();                                                        //Update the screen and menus
                    break;
                }
                case BleService.ACTION_BLE_DISCOVERY_DONE: {                                        //Have completed service discovery
                    Log.d(TAG, "PJH - Received Intent  ACTION_BLE_DISCOVERY_DONE");
                    connectTimeoutHandler.removeCallbacks(abandonConnectionAttempt);                //Stop the connection timeout handler from calling the runnable to stop the connection attempt
                    blackProbeStatusImg.setImageResource(R.drawable.calibrating);
                    stateConnectionBlack = StateConnection.CONNECTED;                                    //Were already connected but showing discovering, not connected
                    updateConnectionState();                                                        //Update the screen and menus
                    Log.i(TAG, "PJH - about to request Ezy config");
                    bleService.requestEzyConfig();                                                         //Ask the BleService to connect to start interrogating the device for its configuration
                    break;
                }
                case BleService.ACTION_BLE_DISCOVERY_FAILED: {                                      //Service discovery failed to find the right service and characteristics
                    Log.d(TAG, "Received Intent  ACTION_BLE_DISCOVERY_FAILED");
                    blackProbeStatusImg.setImageResource(R.drawable.unconnected);
                    stateConnectionBlack = StateConnection.DISCONNECTING;                                //Were already connected but showing discovering, so are now disconnecting
                    connectTimeoutHandler.removeCallbacks(abandonConnectionAttempt);                //Stop the connection timeout handler from calling the runnable to stop the connection attempt
                    bleService.disconnectBle();                                                     //Ask the BleService to disconnect from the Bluetooth device
                    updateConnectionState();                                                        //Update the screen and menus
//                    showAlert.showFaultyDeviceDialog(new Runnable() {                               //Show the AlertDialog for a faulty device
//                        @Override
//                        public void run() {                                                         //Runnable to execute if OK button pressed
//                            startBleScanActivity();                                                 //Launch the BleScanActivity to scan for BLE devices
//                        }
//                    });
                    break;
                }
                case BleService.ACTION_BLE_CONFIG_READY: {                                             //Have read all the Ezy parameters from BLE device
                    Log.d(TAG, "Received Intent  ACTION_BLE_CONFIG_READY");
                    textDeviceStatus.setText(R.string.ready);
                    blackProbeStatusImg.setImageResource(R.drawable.ready);

                    try {
                        Globals.probeConnectedBlackName = bleDeviceNameBlack;
                        Globals.probeConnectedBlackAddress = bleDeviceAddressBlack;
                        Log.e(TAG, "Name: " + Globals.probeConnectedBlackName + ", Address: " + Globals.probeConnectedBlackAddress);
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

    /******************************************************************************************************************
     * Method for processing incoming data and updating the display
     */

    private void initializeDisplay() {
        try {
            textDeviceStatus.setText("Not Connected");  // PJH - hack - shouldn't be here!
        } catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }

    /******************************************************************************************************************
     * Methods for scanning, connecting, and showing event driven dialogs
     */

    // ----------------------------------------------------------------------------------------------------------------
    // Start the BleScanActivity that scans for available Bluetooth devices and lets the user select one
//    private void startBleScanActivity() {
//        try {
//            bleService.invalidateCalibration();  // to force a full connect and reread of the calibration data
//            if (stateApp == StateApp.RUNNING) {                                                     //Only do a scan if we got through startup (permission granted, service started, Bluetooth enabled)
//                stateConnection = StateConnection.DISCONNECTING;                                    //Are disconnecting prior to doing a scan
//                haveSuitableProbeConnected = false;
//                bleService.disconnectBle();                                                         //Disconnect an existing Bluetooth connection or cancel a connection attempt
//                final Intent bleScanActivityIntent = new Intent(CoreMain.this, BleScanActivity.class); //Create Intent to start the BleScanActivity
//                startActivityForResult(bleScanActivityIntent, REQ_CODE_SCAN_ACTIVITY);              //Start the BleScanActivity
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
//        }
//    }

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
                if (stateConnectionBlack == StateConnection.CONNECTING) {                                //See if still trying to connect
                    stateConnectionBlack = StateConnection.DISCONNECTING;                                //Are now disconnecting
                    bleService.disconnectBle();                                                     //Stop the Bluetooth connection attempt in progress
                    updateConnectionState();                                                        //Update the screen and menus
//                    showAlert.showFailedToConnectDialog(new Runnable() {                            //Show the AlertDialog for a connection attempt that failed
//                        @Override
//                        public void run() {                                                         //Runnable to execute if OK button pressed
//                            startBleScanActivity();                                                 //Launch the BleScanActivity to scan for BLE devices
//                        }
//                    });
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
                switch (stateConnectionBlack) {
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
                        stateConnectionBlack = StateConnection.DISCONNECTED;                             //Default, in case state is unknown
                        textDeviceStatus.setText(R.string.not_connected);                          //Show "Not Connected"
                        break;
                    }
                }
                invalidateOptionsMenu();                                                            //Update the menu to reflect the connection state stateConnection
            }
        });
    }

    /********************************************************************************
     * On Screen button methods
     */
    public void blackProbeSelect(View v) {
        //Check if device is connected before allowing user to see data from the probe
        if (bleDeviceNameBlack != null && haveSuitableProbeConnected) { //TODO @ANNA - add something here that says the probe has to be calibrated before we can move on
            Intent intent = new Intent(this, CoreProbeDetails.class);
            intent.putExtra(CoreProbeDetails.EXTRA_DEVICE_NAME, bleDeviceNameBlack);
            intent.putExtra(CoreProbeDetails.EXTRA_DEVICE_ADDRESS, bleDeviceAddressBlack);
            intent.putExtra(CoreProbeDetails.EXTRA_DEVICE_CONNECTION_STATUS, haveSuitableProbeConnected);
            startActivity(intent);
        } else {
            //TODO make a popup that says device not connected cannot get data
            Intent intent = new Intent(this, CoreProbeDetails.class);
            intent.putExtra(CoreProbeDetails.EXTRA_DEVICE_NAME, bleDeviceNameBlack);
            intent.putExtra(CoreProbeDetails.EXTRA_DEVICE_ADDRESS, bleDeviceAddressBlack);
            intent.putExtra(CoreProbeDetails.EXTRA_DEVICE_CONNECTION_STATUS, haveSuitableProbeConnected);
            startActivity(intent);
        }
    }

    public void WhiteProbeSelect(View v) {
        Intent intent = new Intent(this, CoreProbeDetails.class);
        intent.putExtra(CoreProbeDetails.EXTRA_DEVICE_NAME, bleDeviceNameWhite);
        intent.putExtra(CoreProbeDetails.EXTRA_DEVICE_ADDRESS, bleDeviceAddressWhite);
        intent.putExtra(CoreProbeDetails.EXTRA_DEVICE_CONNECTION_STATUS, haveSuitableProbeConnected);
        startActivity(intent);
    }

    public void BlackProbeBtnClick(View v) {
        if (haveSuitableProbeConnected) {
            Intent intent = new Intent(this, InitalisePopupActivity.class);
            Log.d(TAG, "Device Name: " + bleDeviceNameBlack + ", Device Address: " + bleDeviceAddressBlack);
            intent.putExtra(InitalisePopupActivity.EXTRA_DEVICE_NAME, bleDeviceNameBlack);
            intent.putExtra(InitalisePopupActivity.EXTRA_DEVICE_ADDRESS, bleDeviceAddressBlack);
            startActivity(intent);
        } else {
            Log.e(TAG, "Probe is disconnected");
            stateConnectionBlack = StateConnection.DISCONNECTED;
            blackProbeStatusImg.setImageResource(R.drawable.unconnected);
            if ((bleDeviceNameBlack != null) && (bleDeviceAddressBlack != null)) {                                                //See if there is a device name
                // attempt a reconnection
                stateConnectionBlack = StateConnection.CONNECTING;                               //Got an address so we are going to start connecting
                connectWithAddress(bleDeviceAddressBlack);                                       //Initiate a connection
            }
            updateConnectionState();
        }
    }

    public void holeIDBtnClick(View v) {
//        Intent intent = new Intent(this, SurveyOptionsActivity.class);
//        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_NAME, bleDeviceNameBlack);
//        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_ADDRESS, bleDeviceAddressBlack);
//        startActivity(intent);
    }

    public void operatorIDBtnClick(View v) {
//        Intent intent = new Intent(this, SurveyOptionsActivity.class);
//        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_NAME, bleDeviceNameBlack);
//        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_ADDRESS, bleDeviceAddressBlack);
//        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_CONNECTION, haveSuitableProbeConnected);
//        startActivity(intent);
    }
}