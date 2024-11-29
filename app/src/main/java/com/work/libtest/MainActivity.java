//////////////////////////////////////////////////////////////////////////////////
///**
// * \file MainActivity.java
// * \brief Main activity of the app, manages what to do with the probe and collecting calibrating
// * \author Anna Pedersen
// * \date Created: 07/06/2024
// *
// * TODO - activity really needs to be cleaned up a bit - currently moving the calibration process into the Calibration
// *  activity which will run after the probe is selected.
// */
//package com.work.libtest;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import android.Manifest;
//import android.annotation.TargetApi;
//import android.app.Activity;
//import android.content.pm.PackageManager;
//import android.media.AudioManager;
//import android.media.ToneGenerator;
//import android.net.Uri;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Looper;
//import android.provider.Settings;
//import android.util.Log;
//import android.view.KeyEvent;
//import android.view.WindowManager;
//import android.view.inputmethod.EditorInfo;
//import android.widget.ProgressBar;
//import android.widget.Switch;
//import android.widget.TextView;
//
////import com.work.libtest.databinding.ActivityMainBinding;
//
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.FileOutputStream;
//import java.lang.reflect.Array;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//
//import static com.work.libtest.CalibrationHelper.binCalData;
//import static com.work.libtest.Globals.caliDataCollected;
//import static com.work.libtest.Operation.OPERATION_WRITE;
//import static com.work.libtest.Operation.OPERATION_READ;
//import static com.work.libtest.Operation.OPERATION_NOTIFY;
//
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.appcompat.widget.Toolbar;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothGatt;
//import android.bluetooth.BluetoothGattCharacteristic;
//import android.bluetooth.BluetoothGattService;
//import android.bluetooth.BluetoothSocket;
//import android.content.BroadcastReceiver;
//import android.content.ComponentName;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.content.ServiceConnection;
//import android.graphics.Color;
//import android.graphics.drawable.ColorDrawable;
//import android.os.Bundle;
//import android.os.CountDownTimer;
//import android.os.Handler;
//import android.os.IBinder;
//import android.os.Message;
//import android.util.Log;
//import android.view.Menu;
//import android.view.MenuInflater;
//import android.view.MenuItem;
//import android.view.View;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.SimpleExpandableListAdapter;
//import android.widget.TextView;
//
//import com.work.libtest.About.AboutActivity;
//import com.work.libtest.Preferences.PreferencesActivity;
////import com.work.suntech.SelectProbe.DeviceScanActivity;
//import com.work.libtest.SurveyOptions.SurveyOptionsActivity;
//import com.work.libtest.Preferences.Preferences;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.LinkedHashSet;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Locale;
//import java.util.Queue;
//import java.util.Set;
//import java.util.TimeZone;
//
//public class MainActivity extends AppCompatActivity {
//
////    private ActivityMainBinding binding;
//
//    //information passed through from BluetoothTools.DeviceScanActivity after connecting to a device
//    public static final String EXTRA_DEVICE_NAME = "DEVICE_NAME";
//    public static final String EXTRA_DEVICE_ADDRESS = "DEVICE_ADDRESS";
//    public static final String EXTRA_CONNECTION_STATUS = "CONNECTION_STATUS";
//    private static final String NAME_STATE_KEY = "DEVICE NAME STATE KEY";
//    private static final String ADDRESS_STATE_KEY = "ADDRESS STATE KEY";
//
//    private static final int REQ_CODE_ENABLE_BT = 1;                                            //Codes to identify activities that return results such as enabling Bluetooth
//    private static final int REQ_CODE_SCAN_ACTIVITY = 2;                                            //or scanning for bluetooth devices
//    private static final int REQ_CODE_ACCESS_LOC1 = 3;                                            //or requesting location access.
//    private static final int REQ_CODE_ACCESS_LOC2 = 4;                                            //or requesting location access a second time.
//    private static final long CONNECT_TIMEOUT = 10000;
//
//    private ProgressBar progressBar;                                                                //Progress bar (indeterminate circular) to show that activity is busy connecting to BLE device
//    private BleService bleService;                                                                  //Service that handles all interaction with the Bluetooth radio and remote device
//    private ByteArrayOutputStream transparentUartData = new ByteArrayOutputStream();                //Stores all the incoming byte arrays received from BLE device in bleService
//    private Handler connectTimeoutHandler;                                                          //Handler to provide a time out if connection attempt takes too long
//    private String bleDeviceName, bleDeviceAddress;                                                 //Name and address of remote Bluetooth device
//
//    private boolean haveSuitableProbeConnected = false;
//
//    private enum StateConnection {DISCONNECTED, CONNECTING, DISCOVERING, CONNECTED, DISCONNECTING}  //States of the Bluetooth connection
//
//    private StateConnection stateConnection;                                                        //State of Bluetooth connection
//
//    private enum StateApp {STARTING_SERVICE, REQUEST_PERMISSION, ENABLING_BLUETOOTH, RUNNING}       //States of the app
//
//    private StateApp stateApp;
//
//    public static final int OPERATION_WRITE = 1;
//    public static final int OPERATION_READ = 2;
//    public static final int OPERATION_NOTIFY = 3;
//    int _probeMode = 0;
//
//    private final String LIST_NAME = "NAME";
//    private final String LIST_UUID = "UUID";
//
//    private Menu menu;
//
//    public int calibrationIndexNum = 00;
//    boolean calibrated = false;
//    boolean toBeCalibrated = true; //do we even want to gather the calibration matrix
//
//    private String mDeviceName;
//    private String mDeviceAddress;
//    private String mDeviceConnectionStatus;
//
//    private String mMode;
//
//    private boolean isCalibrated = false;
//    final int NUM_CAL_PARAMS_EXPECTED_DURING_PARSING = 38;
//    private int binCalData_size = 0;
//    private String modifiedDateString = "No calibration data";
//    private String calibratedDateString = "No calibration data";
//    private double acc_A[] = new double[3];    // offsets
//    private double acc_B[][] = new double[3][3]; // first order terms
//    private double acc_C[] = new double[3];    // cubic terms
//
//    private double mag_A[] = new double[3];
//    private double mag_B[][] = new double[3][3];
//    private double mag_C[] = new double[3];
//
//    private double temp_param[] = new double[2];   // offset, scale
//
//    private double accManualZeroOffset = 0;  // used in zero roll offset feature of corecams to align drill head
//    private int offset_of_accManualZeroOffset = 0;  // where accManualZeroOffset is located in the the current cal binary
//
//    private double magManualZeroOffset = 0;  // don't think this is used
//
//    int writeCollectValue = 1;
//    int calibrationFirstValue = 0;
//    int calibrationSecondValue = 0;
//    int calibrationThirdValue = 0;
//
//    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<>();
//    private boolean mConnected = false;
//    private BluetoothGattCharacteristic mNotifyCharacteristic;
//
//    private BluetoothLeService mBluetoothLeService;
//    private BluetoothGatt mBluetoothGatt;
//
//    private static final String TAG = "Main Activity";
//    private Handler handler;
//
//    public static ArrayList<Survey> surveys = new ArrayList<Survey>();
//    public static int surveySize = 0;
//    public static Preferences preferences = new Preferences();
//    public static String connectedDeviceName = "No Probe Selected";
//
//    public static ArrayList<String> caliData = new ArrayList<>();
//
//    TextView blackProbeTxt;
//    TextView whiteProbeTxt;
//
//    TextView blackProbeStatusTxt;
//    TextView whiteProbeStatusTxt;
//
//    TextView singleProbeTxt;
//
//    ImageView blackProbeStatusImg;
//
//    LinearLayout WhiteProbeContainer; //the second container for a probe, used if the app is in dual mode
//
//    private BluetoothGattCharacteristic CORE_SHOT_CHARACTERISTIC;
//    private BluetoothGattCharacteristic BORE_SHOT_CHARACTERISTIC;
//
//    private long startTime = 0; //for timer
//    public int seconds;
//    private int starttime = 0;
//    boolean calibration_status = false;
//
//    public static int surveyNum = 0;
//
//    private boolean CalibrationMatrixCreated = false;
//
//    Button operatorNameBtn;
//    Button holeIDBtn;
//
//    TextView HoleIDDisplayTxt;
//    TextView OperatorNameDisplayTxt;
//
//    /******************************************************************************************************************
//     * Methods for handling life cycle events of the activity.
//     */
//
//    // ----------------------------------------------------------------------------------------------------------------
//    // Activity launched
//    // Invoked by launcher Intent
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);  //Call superclass (AppCompatActivity) onCreate method
//
//        try {
//            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  //try preventing phone from sleeping
//            setContentView(R.layout.activity_main);                                                   //Show the main screen - may be shown briefly if we immediately start the scan activity
//            Log.i(TAG, "========== onCreate ============");    // just as a separator in hte logcat window (to distinguish this run from the previous run)
//            stateConnection = StateConnection.DISCONNECTED;                                             //Initial stateConnection when app starts
//            stateApp = StateApp.STARTING_SERVICE;                                                       //Are going to start the BleService service
//            Log.i(TAG, "about to request FINE permission");
//            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { //Check whether we have location permission, required to scan
//                stateApp = StateApp.REQUEST_PERMISSION;                                                 //Are requesting Location permission
//                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQ_CODE_ACCESS_LOC1); //Request fine location permission
//            }
//            if (stateApp == StateApp.STARTING_SERVICE) {                                                //Only start BleService if we already have location permission
//                Log.e(TAG, "Starting bluetooth service");
//                Intent bleServiceIntent = new Intent(this, BleService.class);             //Create Intent to start the BleService
//                this.bindService(bleServiceIntent, bleServiceConnection, BIND_AUTO_CREATE);             //Create and bind the new service with bleServiceConnection object that handles service connect and disconnect
//            }
//            Log.i(TAG, "PJH - ========== onCreate2 ============");    // just as a separator in hte logcat window (to distinguish this run from the previous run)
//            connectTimeoutHandler = new Handler(Looper.getMainLooper());                                //Create a handler for a delayed runnable that will stop the connection attempt after a timeout
//
//            HoleIDDisplayTxt = (TextView)findViewById(R.id.HoleIDDisplayTxt);
//            OperatorNameDisplayTxt = (TextView)findViewById(R.id.OperatorNameDisplayTxt);
//
//            blackProbeTxt = findViewById(R.id.BlackProbeTxt);
//            whiteProbeTxt = findViewById(R.id.WhiteProbeTxt);
//
//            blackProbeStatusTxt = findViewById(R.id.BlackProbeStatusTxt);
//            whiteProbeStatusTxt = findViewById(R.id.WhiteProbeStatusTxt);
//
//            blackProbeStatusImg = findViewById(R.id.BlackProbeStatusImg);
//
//            WhiteProbeContainer = (LinearLayout) findViewById(R.id.WhiteProbeContainer);
//
//            try {
//                if (surveys.size() > 0) { //array has begun to be populated
//                    if (surveys.get(0).getSurveyOptions().getHoleID() != 0 && surveys.get(0).getSurveyOptions() != null) {
//                        HoleIDDisplayTxt.setText(Integer.toString(surveys.get(0).getSurveyOptions().getHoleID()));
//                        OperatorNameDisplayTxt.setText(surveys.get(0).getSurveyOptions().getOperatorName());
//
//                    } else {
//                        HoleIDDisplayTxt.setText("Not set");
//                        OperatorNameDisplayTxt.setText("Not set");
//                    }
//                } else {
//                    HoleIDDisplayTxt.setText("Not set");
//                    OperatorNameDisplayTxt.setText("Not set");
//                }
//            } catch (Exception e) {
//                Log.d(TAG, "Exception thrown: " + e);
//            }
//
////            if (MainActivity.preferences.getMode().equals("Core Orientation (Dual)") || MainActivity.preferences.getMode().equals("Dual")) {
////                WhiteProbeContainer.setVisibility(View.VISIBLE);
////            } else if (MainActivity.preferences.getMode().equals("Bore Orientation (Single)") || MainActivity.preferences.getMode().equals("Single")) {
////                WhiteProbeContainer.setVisibility(View.GONE);
////            } else {
////                Log.e(TAG, "Probe mode is invalid?!");
////            }
//            initializeDisplay();  // PJH - MIGHT GET SOME FLASH OF INITIAL DATA - TODO remove from layout file
//
//        } catch (Exception e) {
//            Log.e(TAG, "Oops, exception caught in onCreate: " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
//        }
//
//    }
//
//    // ----------------------------------------------------------------------------------------------------------------
//    // Activity started
//    // Nothing needed here, all done in onCreate() and onResume()
//    @Override
//    public void onStart() {
//        super.onStart();                                                                            //Call superclass (AppCompatActivity) onStart method
//    }
//
//    // ----------------------------------------------------------------------------------------------------------------
//    // Activity resumed
//    // Register the receiver for Intents from the BleService
//    @Override
//    protected void onResume() {
//        super.onResume();                                                                           //Call superclass (AppCompatActivity) onResume method
//        try {
//            registerReceiver(bleServiceReceiver, bleServiceIntentFilter());                         //Register receiver to handles events fired by the BleService
//            if (bleService != null && !bleService.isBluetoothRadioEnabled()) {                      //Check if Bluetooth radio was turned off while app was paused
//                if (stateApp == StateApp.RUNNING) {                                                 //Check that app is running, to make sure service is connected
//                    stateApp = StateApp.ENABLING_BLUETOOTH;                                         //Are going to request user to turn on Bluetooth
//                    stateConnection = StateConnection.DISCONNECTED;                                 //Must be disconnected if Bluetooth is off
//                    startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQ_CODE_ENABLE_BT); //Start the activity to ask the user to grant permission to enable Bluetooth
//                    Log.i(TAG, "Requesting user to enable Bluetooth radio");
//                }
//            }
//            if ((bleDeviceName != null) && (bleDeviceAddress != null) && bleService.isCalibrated()) {                                                //See if there is a device name
//                // attempt a reconnection
//                stateConnection = StateConnection.CONNECTING;                               //Got an address so we are going to start connecting
//                connectWithAddress(bleDeviceAddress);                                       //Initiate a connection
//            }
//
//            updateConnectionState();                                                                //Update the screen and menus
//        } catch (Exception e) {
//            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
//        }
//    }
//
//    // ----------------------------------------------------------------------------------------------------------------
//    // Activity paused
//    // Unregister the receiver for Intents from the BleService
//    @Override
//    protected void onPause() {
//        super.onPause();                                                                            //Call superclass (AppCompatActivity) onPause method
//        try {
//            unregisterReceiver(bleServiceReceiver);                                                     //Unregister receiver that was registered in onResume()
//        } catch (Exception e) {
//            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
//        }
//    }
//
//    // ----------------------------------------------------------------------------------------------------------------
//    // Activity stopped
//    // Nothing needed here, all done in onPause() and onDestroy()
//    @Override
//    public void onStop() {
//        super.onStop();                                                                             //Call superclass (AppCompatActivity) onStop method
//    }
//
//    // ----------------------------------------------------------------------------------------------------------------
//    // Activity is ending
//    // Unbind from BleService and save the details of the BLE device for next time
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();                                                                          //Call superclass (AppCompatActivity) onDestroy method
//
//        // TODO - need to check...
//        bleService.setProbeIdle();
//
//        if (stateApp != StateApp.REQUEST_PERMISSION) {                                              //See if we got past the permission request
//            unbindService(bleServiceConnection);                                                    //Unbind from the service handling Bluetooth
//        }
//    }
//
//    /******************************************************************************************************************
//     * Methods for handling menu creation and operation.
//     */
//
//    // ----------------------------------------------------------------------------------------------------------------
//    // Options menu is different depending on whether connected or not and if we have permission to scan
//    // Show Disconnect option if we are connected or show Connect option if not connected and have a device address
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.menu_main_activity, menu);
//        this.menu = menu;
//        return true;
//    }
//
//    // ----------------------------------------------------------------------------------------------------------------
//    // Menu item selected
//    // Scan, connect or disconnect, etc.
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle item selection
//        if (item.getItemId() == R.id.select_probe) {
//            Intent intent = new Intent(this, BleScanActivity.class);
//            if (mDeviceName != null && mDeviceAddress != null) {
//                intent.putExtra(BleScanActivity.EXTRA_SCAN_NAME, mDeviceName);
//                intent.putExtra(BleScanActivity.EXTRA_SCAN_ADDRESS, mDeviceAddress);
////                intent.putExtra(DeviceScanActivity.EXTRA_DEVICE_NAME, mDeviceName);
//            }
//            startActivity(intent);
//            return true;
//        } else if (item.getItemId() == R.id.reset_survey) {
//            //TODO reset survey functionality
//            /**
//             * pop-up page asking which survey to cancel
//             * Both probes, black only, white only
//             * or cancel out of the function
//             *
//             * has a confirmation after pressing a probe/s
//             *
//             * potench make this a fragment
//             */
////                int surveyArraySize = MainActivity.surveys.size();
////                MainActivity.surveys.remove(surveyArraySize - 1);
//
//            if (preferences.getMode() == "Core Orientation (Dual)") {
//                mMode = "Dual";
//            } else if (preferences.getMode() == "Bore Orientation (Single)") {
//                mMode = "Single";
//            }
//            //pass in information
//            Intent resetIntent = new Intent(this, ResetSurveyActivity.class);
//            resetIntent.putExtra(ResetSurveyActivity.EXTRA_PREFERENCES_MODE, mMode);
//            resetIntent.putExtra(ResetSurveyActivity.EXTRA_DEVICE_NAME, mDeviceName);
//            resetIntent.putExtra(ResetSurveyActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
//            startActivity(resetIntent);
//
//            //TODO needs to refresh the holeID and operatorID display values here, they still show last known value
//            return true;
//        } else if (item.getItemId() == R.id.reset_probe) {
//            //Todo reset probe functionaility
//            /**
//             * pop-up asking which probe to reset
//             * either both, black or white or cancel out of the function
//             *
//             * has a confirmation after pressing a probe/s
//             */
//            return true;
//        } else if (item.getItemId() == R.id.preferences) {
//            Intent prefIntent = new Intent(this, PreferencesActivity.class);
//            prefIntent.putExtra(PreferencesActivity.EXTRA_DEVICE_NAME, mDeviceName);
//            prefIntent.putExtra(PreferencesActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
//            prefIntent.putExtra(PreferencesActivity.EXTRA_DEVICE_CONNECTION_STATUS, mDeviceConnectionStatus);
//            startActivity(prefIntent);
//            return true;
//        } else if (item.getItemId() == R.id.about) {
//            Intent aboutIntent = new Intent(this, AboutActivity.class);
//            aboutIntent.putExtra(AboutActivity.EXTRA_DEVICE_NAME, mDeviceName);
//            aboutIntent.putExtra(AboutActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
//            aboutIntent.putExtra(AboutActivity.EXTRA_DEVICE_CONNECTION_STATUS, mDeviceConnectionStatus);
//            startActivity(aboutIntent);
//            return true;
//        } else {
//            return super.onOptionsItemSelected(item);
//        }
//    }
//
//    /******************************************************************************************************************
//     * Callback methods for handling Service connection events and Activity result events.
//     */
//
//    // ----------------------------------------------------------------------------------------------------------------
//    // Callbacks for BleService service connection and disconnection
//    private final ServiceConnection bleServiceConnection = new ServiceConnection() {                //Create new ServiceConnection interface to handle connection and disconnection
//
//        @Override
//        public void onServiceConnected(ComponentName componentName, IBinder service) {              //Service connects
//            try {
//                Log.i(TAG, "BleService connected");
//                BleService.LocalBinder binder = (BleService.LocalBinder) service;                   //Get the Binder for the Service
//                bleService = binder.getService();                                                   //Get a link to the Service from the Binder
//                if (bleService.isBluetoothRadioEnabled()) {                                         //See if the Bluetooth radio is on
//                    stateApp = StateApp.RUNNING;                                                    //Service is running and Bluetooth is enabled, app is now fully operational
//                    //startBleScanActivity();                                                         //Launch the com.work.libtest.BleScanActivity to scan for BLE devices
//                } else {                                                                              //Radio needs to be enabled
//                    stateApp = StateApp.ENABLING_BLUETOOTH;                                         //Are requesting Bluetooth to be turned on
//                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);     //Create an Intent asking the user to grant permission to enable Bluetooth
//                    startActivityForResult(enableBtIntent, REQ_CODE_ENABLE_BT);                     //Send the Intent to start the Activity that will return a result based on user input
//                    Log.i(TAG, "Requesting user to turn on Bluetooth");
//                }
//            } catch (Exception e) {
//                Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
//            }
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName componentName) {                            //BleService disconnects - should never happen
//            Log.i(TAG, "BleService disconnected");
//            bleService = null;                                                                      //Not bound to BleService
//        }
//    };
//
//    // ----------------------------------------------------------------------------------------------------------------
//    // Callback for Activities that return a result
//    // We call BluetoothAdapter to turn on the Bluetooth radio and com.work.libtest.BleScanActivity to scan
//    // and return the name and address of a Bluetooth LE device that the user chooses
//    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
//        super.onActivityResult(requestCode, resultCode, intent);                                    //Pass the activity result up to the parent method
//        switch (requestCode) {                                                                      //See which Activity returned the result
//            case REQ_CODE_ENABLE_BT: {
//                if (resultCode == Activity.RESULT_OK) {                                             //User chose to enable Bluetooth
//                    stateApp = StateApp.RUNNING;                                                    //Service is running and Bluetooth is enabled, app is now fully operational
//                    startBleScanActivity();                                                         //Start the com.work.libtest.BleScanActivity to do a scan for devices
//                } else {
//                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);     //User chose not to enable Bluetooth so create an Intent to ask again
//                    startActivityForResult(enableBtIntent, REQ_CODE_ENABLE_BT);                     //Send the Intent to start the activity that will return a result based on user input
//                    Log.i(TAG, "Requesting user to turn on Bluetooth again");
//                }
//                break;
//            }
//            case REQ_CODE_SCAN_ACTIVITY: {
////                showAlert.dismiss();
//                if (resultCode == Activity.RESULT_OK) {                                             //User chose a Bluetooth device to connect
//                    stateApp = StateApp.RUNNING;                                                    //Service is running and Bluetooth is enabled, app is fully operational
//                    bleDeviceAddress = intent.getStringExtra(BleScanActivity.EXTRA_SCAN_ADDRESS);   //Get the address of the BLE device selected in the com.work.libtest.BleScanActivity
//                    bleDeviceName = intent.getStringExtra(BleScanActivity.EXTRA_SCAN_NAME);         //Get the name of the BLE device selected in the com.work.libtest.BleScanActivity
//                    Log.e(TAG, "Device name: " + bleDeviceName + ", Device Address: " + bleDeviceAddress);
//                    //TODO - Add display address and name
//                    if (bleDeviceName != null) {                                                //See if there is a device name
//                        blackProbeTxt.setText(bleDeviceName);
//                    } else {
//                        blackProbeTxt.setText(R.string.unknown_device);
//                    }
//                    if (bleDeviceAddress != null) {                                             //See if there is an address
//                        blackProbeTxt.append(" - " + bleDeviceAddress);
//                    }
//
//                    if (bleDeviceAddress == null) {                                                 //Check whether we were given a device address
//                        stateConnection = StateConnection.DISCONNECTED;                             //No device address so not connected and not going to connect
//                    } else {
//                        stateConnection = StateConnection.CONNECTING;                               //Got an address so we are going to start connecting
//                        connectWithAddress(bleDeviceAddress);                                       //Initiate a connection
//                    }
//                } else {                                                                            //Did not get a valid result from the com.work.libtest.BleScanActivity
//                    stateConnection = StateConnection.DISCONNECTED;                                 //No result so not connected and not going to connect
//                }
//                updateConnectionState();                                                            //Update the connection state on the screen and menus
//                break;
//            }
//        }
//    }
//
//    // ----------------------------------------------------------------------------------------------------------------
//    // Callback for permission requests (new feature of Android Marshmallow requires runtime permission requests)
//    @TargetApi(Build.VERSION_CODES.M)
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        Log.i(TAG, "PJH - onRequestPermissionsResult");
//
//        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {                                 //See if location permission was granted
//            Log.i(TAG, "PJH - Location permission granted");
//            stateApp = StateApp.STARTING_SERVICE;                                                   //Are going to start the BleService service
//            Intent bleServiceIntent = new Intent(this, BleService.class);             //Create Intent to start the BleService
//            this.bindService(bleServiceIntent, bleServiceConnection, BIND_AUTO_CREATE);             //Create and bind the new service to bleServiceConnection object that handles service connect and disconnect
//        } else if (requestCode == REQ_CODE_ACCESS_LOC1) {                                             //Not granted so see if first refusal and need to ask again
////            showAlert.showLocationPermissionDialog(new Runnable() {                                 //Show the AlertDialog that scan cannot be performed without permission
////                @TargetApi(Build.VERSION_CODES.M)
////                @Override
////                public void run() {                                                                 //Runnable to execute when Continue button pressed
////                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_CODE_ACCESS_LOC2); //Ask for location permission again
////                }
////            });
//        } else {
//            //Permission refused twice so send user to settings
//            Log.i(TAG, "PJH - Location permission NOT granted");
//            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);               //Create Intent to open the app settings page
//            Uri uri = Uri.fromParts("package", getPackageName(), null);            //Identify the package for the settings
//            intent.setData(uri);                                                                    //Add the package to the Intent
//            startActivity(intent);                                                                  //Start the settings activity
//        }
//    }
//
//    /******************************************************************************************************************
//     * Methods for handling Intents.
//     */
//
//    // ----------------------------------------------------------------------------------------------------------------
//    // Method to create and return an IntentFilter with Intent Actions that will be broadcast by the BleService to the bleServiceReceiver BroadcastReceiver
//    private static IntentFilter bleServiceIntentFilter() {                                          //Method to create and return an IntentFilter
//        final IntentFilter intentFilter = new IntentFilter();                                       //Create a new IntentFilter
//        intentFilter.addAction(BleService.ACTION_BLE_CONNECTED);                                    //Add filter for receiving an Intent from BleService announcing a new connection
//        intentFilter.addAction(BleService.ACTION_BLE_DISCONNECTED);                                 //Add filter for receiving an Intent from BleService announcing a disconnection
//        intentFilter.addAction(BleService.ACTION_BLE_DISCOVERY_DONE);                               //Add filter for receiving an Intent from BleService announcing a service discovery
//        intentFilter.addAction(BleService.ACTION_BLE_DISCOVERY_FAILED);                             //Add filter for receiving an Intent from BleService announcing failure of service discovery
//        intentFilter.addAction(BleService.ACTION_BLE_NEW_DATA_RECEIVED);                            //Add filter for receiving an Intent from BleService announcing new data received
//        intentFilter.addAction(BleService.ACTION_BLE_CONFIG_READY);                            //Add filter for receiving an Intent from BleService announcing new data received
//        intentFilter.addAction(BleService.ACTION_BLE_FETCH_CAL);  // PJH - just to update display
//        return intentFilter;                                                                        //Return the new IntentFilter
//    }
//
//    // ----------------------------------------------------------------------------------------------------------------
//    // BroadcastReceiver handles various Intents sent by the BleService service.
//    private final BroadcastReceiver bleServiceReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {                                     //Intent received
//            final String action = intent.getAction();                                               //Get the action String from the Intent
//            switch (action) {                                                                       //See which action was in the Intent
//                case BleService.ACTION_BLE_CONNECTED: {                                             //Have connected to BLE device
//                    Log.d(TAG, "Received Intent  ACTION_BLE_CONNECTED");
//                    initializeDisplay();                                                            //Clear the temperature and accelerometer text and graphs
//                    transparentUartData.reset();   // PJH remove                                                 //Also clear any buffered incoming data
//                    stateConnection = StateConnection.DISCOVERING;                                  //BleService automatically starts service discovery after connecting
//                    updateConnectionState();                                                        //Update the screen and menus
//                    break;
//                }
//                case BleService.ACTION_BLE_DISCONNECTED: {                                          //Have disconnected from BLE device
//                    Log.d(TAG, "Received Intent ACTION_BLE_DISCONNECTED");
//                    initializeDisplay();                                                            //Clear the temperature and accelerometer text and graphs
//                    transparentUartData.reset();                                                    //Also clear any buffered incoming data
//                    //if (stateConnection == StateConnection.CONNECTED) {                             //See if we were connected before
//                    //    showAlert.showLostConnectionDialog(new Runnable() {                         //Show the AlertDialog for a lost connection
//                    //        @Override
//                    //        public void run() {                                                     //Runnable to execute if OK button pressed
//                    //            startBleScanActivity();                                             //Launch the com.work.libtest.BleScanActivity to scan for BLE devices
//                    //        }
//                    //    });
//                    //}
//                    stateConnection = StateConnection.DISCONNECTED;                                 //Are disconnected
//
//                    // but we want to reconnect automatically - TODO add retry counter then give up
//                    if ((bleDeviceName != null) && (bleDeviceAddress != null) && bleService.isCalibrated()) {                                                //See if there is a device name
//                        // attempt a reconnection
//                        stateConnection = StateConnection.CONNECTING;                               //Got an address so we are going to start connecting
//                        connectWithAddress(bleDeviceAddress);                                       //Initiate a connection
//                    }
//
//                    updateConnectionState();                                                        //Update the screen and menus
//                    break;
//                }
//                case BleService.ACTION_BLE_DISCOVERY_DONE: {                                        //Have completed service discovery
//                    Log.d(TAG, "PJH - Received Intent  ACTION_BLE_DISCOVERY_DONE");
//                    connectTimeoutHandler.removeCallbacks(abandonConnectionAttempt);                //Stop the connection timeout handler from calling the runnable to stop the connection attempt
//                    stateConnection = StateConnection.CONNECTED;                                    //Were already connected but showing discovering, not connected
//                    updateConnectionState();                                                        //Update the screen and menus
//                    Log.i(TAG, "PJH - about to request Ezy config");
//                    bleService.requestEzyConfig();                                                         //Ask the BleService to connect to start interrogating the device for its configuration
//                    break;
//                }
//                case BleService.ACTION_BLE_DISCOVERY_FAILED: {                                      //Service discovery failed to find the right service and characteristics
//                    Log.d(TAG, "Received Intent  ACTION_BLE_DISCOVERY_FAILED");
//                    stateConnection = StateConnection.DISCONNECTING;                                //Were already connected but showing discovering, so are now disconnecting
//                    connectTimeoutHandler.removeCallbacks(abandonConnectionAttempt);                //Stop the connection timeout handler from calling the runnable to stop the connection attempt
//                    bleService.disconnectBle();                                                     //Ask the BleService to disconnect from the Bluetooth device
//                    updateConnectionState();                                                        //Update the screen and menus
////                    showAlert.showFaultyDeviceDialog(new Runnable() {                               //Show the AlertDialog for a faulty device
////                        @Override
////                        public void run() {                                                         //Runnable to execute if OK button pressed
////                            startBleScanActivity();                                                 //Launch the com.work.libtest.BleScanActivity to scan for BLE devices
////                        }
////                    });
//                    break;
//                }
//                case BleService.ACTION_BLE_CONFIG_READY: {                                             //Have read all the Ezy parameters from BLE device
//                    Log.d(TAG, "Received Intent  ACTION_BLE_CONFIG_READY");
////                    progressBar.setVisibility(ProgressBar.INVISIBLE);
//                                                     //Update the screen and menus
//
//                    String verString = bleService.getFirmwareVersionString();
////                    textDeviceStatus.setText(R.string.ready);  // PJH - hack - shouldn't be here!
////                    labelFirmwareVersion.setVisibility(View.VISIBLE);
////                    textFirmwareVersion.setText(verString);
////                    textFirmwareVersion.setVisibility(View.VISIBLE);
//
//                    bleService.parseBinaryCalibration();   // process thr calibration data just retrieved
//
//                    bleService.setNotifications(true);
//                    String java_date = bleService.getCalibratedDateString();
////                    labelCalibratedDate.setVisibility(View.VISIBLE);
////                    textCalibratedDate.setText(java_date);
////                    textCalibratedDate.setVisibility(View.VISIBLE);
//                    //}
//
//                    int probeShotInterval = bleService.getShotInterval();
////                    textInterval.setText(Integer.toString(probeShotInterval));
//                    int probeDebug1 = bleService.getDebug1();
////                    editBoxDebug1.setText(Integer.toString(probeDebug1));
//                    int probeDebug2 = bleService.getDebug2();
////                    editBoxDebug1.setText(Integer.toString(probeDebug2));
//
//                    haveSuitableProbeConnected = true;   // this enables the test stuff
//                    break;
//                }
//                case BleService.ACTION_BLE_FETCH_CAL: {                                        //Have completed service discovery
//                    Log.d(TAG, "PJH - Received Intent  ACTION_BLE_FETCH_CAL");
////                    textDeviceStatus.setText("Fetching calibration");   TODO - fix                        //Show "Discovering"
////                    progressBar.setVisibility(ProgressBar.VISIBLE);
//                    break;
//                }
//                case BleService.ACTION_BLE_NEW_DATA_RECEIVED: {                                     //Have received data (characteristic notification) from BLE device
//                    Log.i(TAG, "PJH - Received Intent ACTION_BLE_NEW_DATA_RECEIVED");
//                    processNewReading();
//                    break;
//                }
//                default: {
//                    Log.w(TAG, "Received Intent with invalid action: " + action);
//                }
//            }
//        }
//    };
//
//    /******************************************************************************************************************
//     * Method for processing incoming data and updating the display
//     */
//
//    private void initializeDisplay() {
//        try {
////            textDeviceStatus.setText("Not Connected");  // PJH - hack - shouldn't be here!
////            labelFirmwareVersion.setVisibility(View.INVISIBLE);
////            textFirmwareVersion.setText("");
////            textFirmwareVersion.setVisibility(View.INVISIBLE);
////
////            labelCalibratedDate.setVisibility(View.INVISIBLE);
////            textCalibratedDate.setText("");
////            textCalibratedDate.setVisibility(View.INVISIBLE);
////
////            // PJH - not sure if setting to a null string will fully overwrite any existing numbers
////            textAccX.setText("");
////            textAccY.setText("");
////            textAccZ.setText("");
////            textAccMag.setText("");
////
////            textMagX.setText("");
////            textMagY.setText("");
////            textMagZ.setText("");
////            textMagMag.setText("");
////
////            textRoll.setText("");
////            textRoll360.setText("");
////            textDip.setText("");
////            textAz.setText("");
////            textAzErr.setText("");
////
////
////            textAlignCount.setText("0");
////            textAlignAvgDip.setText("");
////            textAlignAvgAz.setText("");
////
////            textAcceptComment.setText("Select Location and press Start\n(in -50 tray, at 0 roll, az should be 283.26)");
////            textAcceptDip.setText("");
////            textAcceptAz.setText("");
////
////            buttonAlignStart.setText("START");  // was having issues with first click not working
////            buttonAcceptStart.setText("START");
////
////            textAcceptResultAz.setText("");
////            textAcceptResultDip.setText("");
//
//        } catch (Exception e) {
//            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
//        }
//    }
//
//    public static Integer toInteger(Object value) {
//        if (value instanceof Integer) {
//            return (Integer) value;
//        } else if (value instanceof Number) {
//            return ((Number) value).intValue();
//        } else if (value instanceof String) {
//            try {
//                return (int) Double.parseDouble((String) value);
//            } catch (NumberFormatException ignored) {
//            }
//        }
//        return null;
//    }
//
//
//    // a new shot has been received, so retrieve and display it
//    private void processNewReading() {
//        try {
////            int count = toInteger(editBoxAverage.getText().toString());
////            if (count < 1) {
////                count = 1;
////                editBoxAverage.setText(Integer.toString(count));  // only update if it is invalid
////            }
////            if (count > 120) {   // this value is set by ringBufferSize in bleService
////                count = 120;
////                editBoxAverage.setText(Integer.toString(count));  // only update if it is invalid
////            }
//
//            //textFirmwareVersion.setText(Integer.toString(count));  // hack
////            double newVal[] = bleService.getLatestBoreshot(count);
////
////            recordCount = bleService.getSensorDataCount();
////            if (bleService.isRecordingSensorDataEnabled() && (recordCount > 0)) {
////                buttonRecord.setText(String.format("Save: %d", recordCount));
////            }
////
////
////            textAccX.setText(String.format("%7.4f", newVal[1]));
////            textAccY.setText(String.format("%7.4f", newVal[2]));
////            textAccZ.setText(String.format("%7.4f", newVal[3]));
////            boolean accValid = true;
////            double accMag = Math.sqrt(newVal[1] * newVal[1] + newVal[2] * newVal[2] + newVal[3] * newVal[3]);
////            if (Math.abs(accMag - 1.0) > 0.03) {
////                accValid = false;
////            }
////            textAccMag.setText(String.format("(%7.4f)", accMag));
////            if (accValid) {
////                textAccMag.setTextColor(Color.BLACK);
////            } else {
////                textAccMag.setTextColor(Color.RED);
////            }
////
////            double magMag = Math.sqrt(newVal[4] * newVal[4] + newVal[5] * newVal[5] + newVal[6] * newVal[6]);
////
////            //
////            textMagX.setText(String.format("%7.4f", newVal[4]));
////            textMagY.setText(String.format("%7.4f", newVal[5]));
////            textMagZ.setText(String.format("%7.4f", newVal[6]));
////
////            textMagMag.setText(String.format("(%7.4f)", magMag));
////
////            textRoll.setText(String.format("%7.4f", newVal[7]));
////            textRoll360.setText(String.format("(%7.4f)", newVal[7] + 180));
////            textDip.setText(String.format("%7.4f", newVal[8]));
////            textAz.setText(String.format("%7.4f", newVal[9]));
////            textAzErr.setText("");  // just in case we are out of accept mode - will this flicker?
////
////            textTempUc.setText(String.format("%7.4f", newVal[10]));
////
////            //
////            // Check if taking reading for alignment
////            //
////            if (newAlignCountRemaining > 0) {
////                //Log.i(TAG, String.format("PJH - align sample %d (remaining)", newAlignCountRemaining));
////                newAlignReadingDipSum += newVal[8];
////                newAlignReadingAzSum += newVal[9];
////                newAlignCountRemaining -= 1;
////                textAlignCountdown.setText(String.format("(%d)", newAlignCountRemaining));
////
////                if (newAlignCountRemaining == 0) {
////                    // ok, we just took te last sample for this reading
////                    if (switchRecord.isChecked()) {  // do we have a race condition here
////                        // we have recorded the last of the shots
////                        bleService.stopRecordingSensorData();
////                    }
////
////                    // now update the totals
////                    alignDipTotal += newAlignReadingDipSum;
////                    alignAzTotal += newAlignReadingAzSum;
////                    alignCount += 1;
////                    // and update the display
////                    textAlignCount.setText(String.format("%d", alignCount));
////                    textAlignAvgDip.setText(String.format("%7.4f", (alignDipTotal / alignCount / alignSamplesPerReading)));
////                    textAlignAvgAz.setText(String.format("%7.4f", (alignAzTotal / alignCount / alignSamplesPerReading)));
////
////                    ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);   // beep
////                    toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
////
////                    textAlignCountdown.setText("");
////                }
////            }
////
////            if (acceptShowLiveError) {
////                acceptCurrentLiveDipError = Math.abs(newVal[8] - acceptTestPointDip[acceptState - 1]);
////                acceptCurrentLiveRoll360Error = Math.abs(newVal[7] + 180 - acceptTestPointRoll[acceptState - 1]);
////                //textFirmwareVersion.setText(String.format("Acc DipErr=%3.2f RollErr=%3.2f", acceptCurrentLiveDipError, acceptCurrentLiveRoll360Error));// PJH TODO DEBUG HACK
////                double azIdeal = 0;
////                if ((acceptState >= 1) && (acceptState <= 4)) {
////                    azIdeal = acceptIdeal50Az;
////                }
////                if ((acceptState >= 5) && (acceptState <= 8)) {
////                    azIdeal = acceptIdeal60Az;
////                }
////                if ((acceptState >= 9) && (acceptState <= 12)) {
////                    azIdeal = acceptIdeal30Az;
////                }
////                acceptCurrentLiveAzError = Math.abs(newVal[9] - azIdeal);
////
////
////                // if current dip is more than 4 degrees from the desired position, show live dip in red
////                if (acceptCurrentLiveDipError < acceptAcceptableLiveError) {
////                    textDip.setTextColor(Color.BLACK);
////                } else {
////                    textDip.setTextColor(Color.RED);
////                }
////                // if current roll is more than 4 degrees from the desired position, show live roll360 in red
////                if (acceptCurrentLiveRoll360Error < 4) {
////                    textRoll360.setTextColor(Color.BLACK);
////                } else {
////                    textRoll360.setTextColor(Color.RED);
////                }
////
////                textAzErr.setText(String.format("(err:%7.4f)", acceptCurrentLiveAzError));
////            } else {
////                // this should never be required, but just in case...
////                acceptCurrentLiveDipError = 0;
////                acceptCurrentLiveRoll360Error = 0;
////                acceptCurrentLiveAzError = 0;
////            }
////
////            //
////            // Check if taking reading for acceptance test
////            //
////            if (newAcceptCountRemaining > 0) {
////                //Log.i(TAG, String.format("PJH - accept sample %d (remaining)", newAcceptCountRemaining));
////                newAcceptReadingDipSum += newVal[8];
////                newAcceptReadingAzSum += newVal[9];
////                newAcceptCountRemaining -= 1;
////                textAlignCountdown.setText(String.format("(%d)", newAcceptCountRemaining));  // yes, I know it is in the wrong area
////
////                if (acceptState > 0) {
////                    textAcceptResultAz.setText("");
////                    textAcceptResultDip.setText("");
////                }
////
////                if (newAcceptCountRemaining == 0) {
////                    // ok, we just took te last sample for this reading
////
////                    if (switchRecord.isChecked()) {  // do we have a race condition here
////                        // we have recorded the last of the shots
////                        bleService.stopRecordingSensorData();
////                    }
////
////                    if ((acceptState > 0) && (acceptState <= 12)) {
////                        acceptDip[acceptState - 1] = newAcceptReadingDipSum / acceptSamplesPerReading;
////                        acceptAz[acceptState - 1] = newAcceptReadingAzSum / acceptSamplesPerReading;
////                        acceptRoll[acceptState - 1] = newVal[7];
////                        acceptState += 1;
////                    }
////                    // now update the totals
////                    //alignDipTotal += newAlignReadingDipSum;
////                    //alignAzTotal  += newAlignReadingAzSum;
////                    //alignCount += 1;
////                    // and update the display
////                    //textAlignCount.setText(String.format("%d", alignCount));
////                    textAcceptDip.setText(String.format("%7.4f", (newAcceptReadingDipSum / acceptSamplesPerReading)));
////                    textAcceptAz.setText(String.format("%7.4f", (newAcceptReadingAzSum / acceptSamplesPerReading)));
////
////                    if (acceptState >= 13) {
////                        // Ok, have taken all 12 readings
////                        textAcceptComment.setText(String.format("Test complete"));
////                        buttonAcceptStart.setText("START");
////
////                        acceptShowLiveError = false;  // turn off error display and make sure everything back to normal
////                        textDip.setTextColor(Color.BLACK);
////                        textRoll360.setTextColor(Color.BLACK);
////                        textRoll.setVisibility(View.VISIBLE);
////
////                        bleService.setProbeIdle();
////                        // now generate results file
////                        String nowDate = new SimpleDateFormat("yyyy-MM-dd_hh-mm", Locale.getDefault()).format(new Date());
////                        String safeBleDeviceAddress = bleDeviceAddress.replace(':', '-');
////                        String safeBleDeviceName = bleDeviceName.replace(':', '-');
////                        String filename = String.format("AcceptTest_%s_%s_%s.csv", nowDate, safeBleDeviceAddress, safeBleDeviceName);
////                        Log.i(TAG, String.format("PJH - Accept filename: %s", filename));
////                        writeAcceptReadingsToFile(getExternalFilesDir("/").getAbsolutePath() + "/" + filename);
////                        // and do calculations
////                        double sqAzDeltaSum = 0;
////                        double sqDipDeltaSum = 0;
////                        double err = 0;
////                        for (int i = 0; i < 4; i++) {
////                            err = acceptAz[i] - acceptIdeal50Az;
////                            if (err > 180) {
////                                err -= 360;
////                            }
////                            if (err < -180) {
////                                err += 360;
////                            }
////                            sqAzDeltaSum += (err * err);
////
////                            //sqDipDeltaSum += ((acceptDip[i]-acceptIdeal50Dip) * (acceptDip[i]-acceptIdeal50Dip) );
////                            err = acceptDip[i] - acceptIdeal50Dip;
////                            if (err > 180) {
////                                err -= 360;
////                            }
////                            if (err < -180) {
////                                err += 360;
////                            }
////                            sqDipDeltaSum += (err * err);
////
////                        }
////                        for (int i = 4; i < 8; i++) {
////                            //sqAzDeltaSum += ((acceptAz[i]-acceptIdeal60Az) * (acceptAz[i]-acceptIdeal60Az) );
////                            err = acceptAz[i] - acceptIdeal60Az;
////                            if (err > 180) {
////                                err -= 360;
////                            }
////                            if (err < -180) {
////                                err += 360;
////                            }
////                            sqAzDeltaSum += (err * err);
////
////                            //sqDipDeltaSum += ((acceptDip[i]-acceptIdeal60Dip) * (acceptDip[i]-acceptIdeal60Dip) );
////                            err = acceptDip[i] - acceptIdeal60Dip;
////                            if (err > 180) {
////                                err -= 360;
////                            }
////                            if (err < -180) {
////                                err += 360;
////                            }
////                            sqDipDeltaSum += (err * err);
////
////                        }
////                        for (int i = 8; i < 12; i++) {
////                            //sqAzDeltaSum += ((acceptAz[i]-acceptIdeal30Az) * (acceptAz[i]-acceptIdeal30Az) );
////                            err = acceptAz[i] - acceptIdeal30Az;
////                            if (err > 180) {
////                                err -= 360;
////                            }
////                            if (err < -180) {
////                                err += 360;
////                            }
////                            sqAzDeltaSum += (err * err);
////
////                            //sqDipDeltaSum += ((acceptDip[i]-acceptIdeal30Dip) * (acceptDip[i]-acceptIdeal30Dip) );
////                            err = acceptDip[i] - acceptIdeal30Dip;
////                            if (err > 180) {
////                                err -= 360;
////                            }
////                            if (err < -180) {
////                                err += 360;
////                            }
////                            sqDipDeltaSum += (err * err);
////
////                        }
////                        acceptRmsAz = Math.sqrt(sqAzDeltaSum / 12);
////                        acceptRmsDip = Math.sqrt(sqDipDeltaSum / 12);
////
////                        textAcceptResultAz.setText(String.format("%5.3f", acceptRmsAz));     // show the result (summary) of the test
////                        textAcceptResultDip.setText(String.format("%5.3f", acceptRmsDip));
////                        textAcceptComment.setText(String.format("Test complete\nPress Start to begin a new test."));
////                        acceptState = 0;
////                    } else {
////                        // more readings to take...
////                        textAcceptComment.setText(String.format("%dof12 Place probe in '%d' tray, adjust roll to %d degrees, step back and press 'Take Reading'",
////                                acceptState, acceptTestPointDip[acceptState - 1], acceptTestPointRoll[acceptState - 1]));
////                    }
////
////                    ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
////                    toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
////
////                    textAlignCountdown.setText("");   // hide the countdown
////
////                }
////            }
//
//        } catch (Exception e) {
//            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
//        }
//    }
//
//
//    public void writeAcceptReadingsToFile(final String filename) {
////        File file = new File(filename);
////        try {
////
////            // generate the original acceptance report
////            String csvBody = "";
////            csvBody += "Reading,Roll,Dip,Azimuth\n";
////            for (int i = 0; i < 12; i++) {
////                csvBody += String.format("%d,%f,%f,%f\n", i + 1, acceptRoll[i], acceptDip[i], acceptAz[i]);
////            }
////            csvBody += "\n\n\n";
////
////            csvBody += String.format("Location, %s\n\n", acceptLocation);
////
////            csvBody += String.format("Tray, IdealDip, IdealAzimuth\n");
////            csvBody += String.format("-60, %f, %f\n", acceptIdeal60Dip, acceptIdeal60Az);
////            csvBody += String.format("-50, %f, %f\n", acceptIdeal50Dip, acceptIdeal50Az);
////            csvBody += String.format("-30, %f, %f\n", acceptIdeal30Dip, acceptIdeal30Az);
////            csvBody += "\n\n\n";
////
////            FileOutputStream stream = new FileOutputStream(file);
////            stream.write(csvBody.getBytes());
////            stream.close();
////
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
//    }
//
//
//    public void writeSensorDataToFile(final String filename) {
////        File file = new File(filename);
////        try {
////            // generate the original SensorData report
////            String csvBody = "";
////
////            Log.i(TAG, "PJH - about to save");
////            //buttonRecord.setText("Saving data");
////            //bleService.stopRecordingSensorData();
////            recordCount = bleService.getSensorDataCount();
////
////            //Log.i(TAG, String.format("PJH - about to save %d sensorData records", recordCount);
////            Log.i(TAG, "PJH - about to get header");
////            String header = bleService.sensorDataReportGetReportHeader();
////            csvBody += header + "\n";
////
////            String record;
////            for (int i = 0; i < recordCount; i++) {
////                record = bleService.sensorDataReportGetReportLine(i);
////                csvBody += record + "\n";
////            }
////
////
////            FileOutputStream stream = new FileOutputStream(file);
////            stream.write(csvBody.getBytes());
////            stream.close();
////
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
//    }
//
//
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
//
///*
//    private void processCharacteristicRead(byte[] newBytes) {
//        try {
//
//        } catch (Exception e) {
//            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
//        }
//    }
// */
//
//
//    /******************************************************************************************************************
//     * Methods for scanning, connecting, and showing event driven dialogs
//     */
//
//    // ----------------------------------------------------------------------------------------------------------------
//    // Start the com.work.libtest.BleScanActivity that scans for available Bluetooth devices and lets the user select one
//    private void startBleScanActivity() {
//        try {
//            bleService.invalidateCalibration();  // to force a full connect and reread of the calibration data
//            if (stateApp == StateApp.RUNNING) {                                                     //Only do a scan if we got through startup (permission granted, service started, Bluetooth enabled)
//                stateConnection = StateConnection.DISCONNECTING;                                    //Are disconnecting prior to doing a scan
//                haveSuitableProbeConnected = false;
//                bleService.disconnectBle();                                                         //Disconnect an existing Bluetooth connection or cancel a connection attempt
//                final Intent bleScanActivityIntent = new Intent(MainActivity.this, BleScanActivity.class); //Create Intent to start the com.work.libtest.BleScanActivity
//                startActivityForResult(bleScanActivityIntent, REQ_CODE_SCAN_ACTIVITY);              //Start the com.work.libtest.BleScanActivity
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
//        }
//    }
//
//    // ----------------------------------------------------------------------------------------------------------------
//    // Attempt to connect to a Bluetooth device given its address and time out after CONNECT_TIMEOUT milliseconds
//    private void connectWithAddress(String address) {
//        try {
//            updateConnectionState();                                                                //Update the screen and menus (stateConnection is either CONNECTING or AUTO_CONNECT
//            connectTimeoutHandler.postDelayed(abandonConnectionAttempt, CONNECT_TIMEOUT);           //Start a delayed runnable to time out if connection does not occur
//            bleService.connectBle(address);                                                         //Ask the BleService to connect to the device
//        } catch (Exception e) {
//            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
//        }
//    }
//
//    // ----------------------------------------------------------------------------------------------------------------
//    // Runnable used by the connectTimeoutHandler to stop the connection attempt
//    private Runnable abandonConnectionAttempt = new Runnable() {
//        @Override
//        public void run() {
//            try {
//                if (stateConnection == StateConnection.CONNECTING) {                                //See if still trying to connect
//                    stateConnection = StateConnection.DISCONNECTING;                                //Are now disconnecting
//                    bleService.disconnectBle();                                                     //Stop the Bluetooth connection attempt in progress
//                    updateConnectionState();                                                        //Update the screen and menus
////                    showAlert.showFailedToConnectDialog(new Runnable() {                            //Show the AlertDialog for a connection attempt that failed
////                        @Override
////                        public void run() {                                                         //Runnable to execute if OK button pressed
////                            startBleScanActivity();                                                 //Launch the com.work.libtest.BleScanActivity to scan for BLE devices
////                        }
////                    });
//                }
//            } catch (Exception e) {
//                Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
//            }
//        }
//    };
//
//    /******************************************************************************************************************
//     * Methods for updating connection state on the screen
//     */
//
//    // ----------------------------------------------------------------------------------------------------------------
//    // Update the text showing what Bluetooth device is connected, connecting, discovering, disconnecting, or not connected
//    private void updateConnectionState() {
//        runOnUiThread(new Runnable() {                                                              //Always do display updates on UI thread
//            @Override
//            public void run() {
//                switch (stateConnection) {
//                    case CONNECTING: {
////                        textDeviceStatus.setText(R.string.waiting_to_connect);                             //Show "Connecting"
////                        progressBar.setVisibility(ProgressBar.VISIBLE);                             //Show the circular progress bar
//                        break;
//                    }
//                    case CONNECTED: {
////                        textDeviceStatus.setText(R.string.interrogating_configuration);
////                        progressBar.setVisibility(ProgressBar.INVISIBLE);                           //Hide the circular progress bar
//                        break;
//                    }
//                    case DISCOVERING: {
////                        textDeviceStatus.setText(R.string.interrogating_features);                            //Show "Discovering"
////                        progressBar.setVisibility(ProgressBar.VISIBLE);                             //Show the circular progress bar
//                        break;
//                    }
//                    case DISCONNECTING: {
////                        textDeviceStatus.setText(R.string.disconnecting);                          //Show "Disconnectiong"
////                        progressBar.setVisibility(ProgressBar.INVISIBLE);                           //Hide the circular progress bar
//                        break;
//                    }
//                    case DISCONNECTED:
//                    default: {
//                        stateConnection = StateConnection.DISCONNECTED;                             //Default, in case state is unknown
////                        textDeviceStatus.setText(R.string.not_connected);                          //Show "Not Connected"
////                        progressBar.setVisibility(ProgressBar.INVISIBLE);                           //Hide the circular progress bar
//                        break;
//                    }
//                }
//                invalidateOptionsMenu();                                                            //Update the menu to reflect the connection state stateConnection
//            }
//        });
//    }
//
//
//    // alignInit is called when Align Start button is pressed,
//    // so it also has to handle the alternate abort functionality
//    private void initAlign(View v, int count) {
////        if ((haveSuitableProbeConnected) && (acceptState == 0)) {
////            Log.i(TAG, "PJH - processing Align Start Data button press");
////            if (buttonAlignStart.getText() == "START") {
////                // PJH TODO - need to stop LIVE DATA or accept
////                alignCount = 0;
////                alignDipTotal = 0;
////                alignAzTotal = 0;
////                textAlignCount.setText("0");
////                textAlignAvgDip.setText("0");
////                textAlignAvgAz.setText("0");
////
////                alignSamplesPerReading = count;
////
////                buttonAlignStart.setText("ABORT");
////                bleService.setProbeMode(2); //PROBE_MODE_ROLLING_SHOTS);  // TODO - this is wrong for corecam
////
////                if (switchRecord.isChecked()) {
////                    // sensor data recording is enabled
////                    bleService.initRecordingSensorData();
////                }
////            } else {    // must be abort
////                buttonAlignStart.setText("START");
////                bleService.setProbeIdle();
////                textAlignCountdown.setText("");
////            }
////            //ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
////            //toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,150);
////        }
//
//    }
//}

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

/**
 * Activity shows temperature and accelerometer data from the AVR BLE development board (running custom firmware)
 */
public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();                        //Activity name for logging messages on the ADB

    public static final String EXTRA_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRA_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRA_CONNECTION_STATUS = "CONNECTION_STATUS";
    public static final String EXTRA_PARENT_ACTIVITY = "Parent_Activity";


    private static final int REQ_CODE_ENABLE_BT =     1;                                            //Codes to identify activities that return results such as enabling Bluetooth
    private static final int REQ_CODE_SCAN_ACTIVITY = 2;                                            //or scanning for bluetooth devices
    private static final int REQ_CODE_ACCESS_LOC1 =   3;                                            //or requesting location access.
    private static final int REQ_CODE_ACCESS_LOC2 =   4;                                            //or requesting location access a second time.
    private static final long CONNECT_TIMEOUT =       10000;                                        //Length of time in milliseconds to try to connect to a device

    private BleService bleService;                                                                  //Service that handles all interaction with the Bluetooth radio and remote device
    private ByteArrayOutputStream transparentUartData = new ByteArrayOutputStream();                //Stores all the incoming byte arrays received from BLE device in bleService
    private ShowAlertDialogs showAlert;                                                             //Object that creates and shows all the alert pop ups used in the app
    private Handler connectTimeoutHandler;                                                          //Handler to provide a time out if connection attempt takes too long
    public static String bleDeviceName;
    public static String bleDeviceAddress;                                                 //Name and address of remote Bluetooth device

    private boolean haveSuitableProbeConnected = false;

    private TextView textDeviceNameAndAddress;                                                      //To show device and status information on the screen
    private TextView textDeviceStatus;

    private ImageView blackProbeStatusImg;
    private LinearLayout WhiteProbeContainer; //the second container for a probe, used if the app is in dual mode

    private boolean forceUncalibratedMode = false;  // user can tap CalibrationDate label and disable calibration

    int recordCount = 0;  // number of shots recorded so far
    String recordFilename = "SensorData-Auto-yyyyMMdd-HHmmss.csv";
                                             //Switches to control and show LED state
    private enum StateConnection {DISCONNECTED, CONNECTING, DISCOVERING, CONNECTED, DISCONNECTING}  //States of the Bluetooth connection
    private StateConnection stateConnection;                                                        //State of Bluetooth connection
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

    /******************************************************************************************************************
     * Methods for handling life cycle events of the activity.
     */

    // ----------------------------------------------------------------------------------------------------------------
    // Activity launched
    // Invoked by launcher Intent
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);  //Call superclass (AppCompatActivity) onCreate method

        try {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  // PJH - try preventing phone from sleeping
            setContentView(R.layout.activity_main);                                                   //Show the main screen - may be shown briefly if we immediately start the scan activity             //Hide the circular progress bar
            showAlert = new ShowAlertDialogs(this);                                             //Create the object that will show alert dialogs
            Log.i(TAG, "PJH - ========== onCreate ============");    // just as a separator in hte logcat window (to distinguish this run from the previous run)
            stateConnection = StateConnection.DISCONNECTED;                                             //Initial stateConnection when app starts
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

            textDeviceNameAndAddress = findViewById(R.id.BlackProbeTxt);                     //Get a reference to the TextView that will display the device name and address
            textDeviceStatus = findViewById(R.id.BlackProbeStatusTxt);                     //Get a reference to the TextView that will display the device name and address
            blackProbeStatusImg = findViewById(R.id.BlackProbeStatusImg);
            WhiteProbeContainer = (LinearLayout) findViewById(R.id.WhiteProbeContainer);

//            blackProbeStatusImg.setImageResource(R.drawable.unconnected);

            if (MainActivity.preferences.getMode().equals("Core Orientation (Dual)") || MainActivity.preferences.getMode().equals("Dual")) {
                WhiteProbeContainer.setVisibility(View.VISIBLE);
            } else if (MainActivity.preferences.getMode().equals("Bore Orientation (Single)") || MainActivity.preferences.getMode().equals("Single")) {
                WhiteProbeContainer.setVisibility(View.GONE);
            } else {
                Log.e(TAG, "Probe mode is invalid?!");
            }

            try {
                if (surveys.size() > 0) { //array has begun to be populated
                    if (surveys.get(0).getSurveyOptions().getHoleID() != 0 && surveys.get(0).getSurveyOptions() != null) {
                        HoleIDDisplayTxt.setText(Integer.toString(surveys.get(0).getSurveyOptions().getHoleID()));
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
                    stateConnection = StateConnection.DISCONNECTED;                                 //Must be disconnected if Bluetooth is off
                    startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQ_CODE_ENABLE_BT); //Start the activity to ask the user to grant permission to enable Bluetooth
                    Log.i(TAG, "Requesting user to enable Bluetooth radio");
                }
            }
            final Intent intent = getIntent();
            try {
                String parentActivityValue = intent.getStringExtra(EXTRA_PARENT_ACTIVITY);
                Log.e(TAG, intent.getStringExtra(EXTRA_PARENT_ACTIVITY) + "," + intent.getStringExtra(EXTRA_DEVICE_NAME) + "," + intent.getStringExtra(EXTRA_DEVICE_ADDRESS));
                if (parentActivityValue != null) {
                    if (parentActivityValue.equals("SurveyOptions") || parentActivityValue.equals("ProbeDetails") || parentActivityValue.equals("TakeMeasurements") || parentActivityValue.equals("AllSurveyOptions")) {
                        stateApp = StateApp.RUNNING;
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
                            stateConnection = StateConnection.CONNECTED;
//                            connectWithAddress(bleDeviceAddress);
                        }
                        //the method below works for reconnecting on a button, havent gotten it to work in a life cycle method
//                        if ((bleDeviceName != null) && (bleDeviceAddress != null) && bleService.isCalibrated()) {                                                //See if there is a device name
//                            // attempt a reconnection
//                            stateConnection = StateConnection.CONNECTING;                               //Got an address so we are going to start connecting
//                            connectWithAddress(bleDeviceAddress);                                       //Initiate a connection
//                        }
//                        updateConnectionState();
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
        getMenuInflater().inflate(R.menu.ble_main_menu, menu);                                      //Show the menu
        if (stateApp == StateApp.RUNNING) {                                                         //See if we have permission, service started and Bluetooth enabled
            menu.findItem(R.id.menu_scan).setVisible(true);                                         //Scan menu item
            if (stateConnection == StateConnection.CONNECTED) {                                     //See if we are connected
                menu.findItem(R.id.menu_disconnect).setVisible(true);                               //Are connected so show Disconnect menu
                menu.findItem(R.id.menu_connect).setVisible(false);                                 //and hide Connect menu
            }
            else {                                                                                  //Else are not connected so
                menu.findItem(R.id.menu_disconnect).setVisible(false);                              // hide the disconnect menu
                if (bleDeviceAddress != null) {                                                     //See if we have a device address
                    menu.findItem(R.id.menu_connect).setVisible(true);                              // then show the connect menu
                }
                else {                                                                              //Else no device address so
                    menu.findItem(R.id.menu_connect).setVisible(false);                             // hide the connect menu
                }
            }
        }
        else {
            menu.findItem(R.id.menu_scan).setVisible(false);                                        //No permission so hide scan menu item
            menu.findItem(R.id.menu_connect).setVisible(false);                                     //and hide Connect menu
            menu.findItem(R.id.menu_disconnect).setVisible(false);                                  //Are not connected so hide the disconnect menu
        }
        return true;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Menu item selected
    // Scan, connect or disconnect, etc.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            switch (item.getItemId()) {
                case R.id.menu_scan: {                                                            //Menu option Scan chosen
                    initializeDisplay();   // remove any old readings, before selecting new probe
                    startBleScanActivity();                                                         //Launch the BleScanActivity to scan for BLE devices
                    return true;
                }
                case R.id.menu_connect: {                                                           //Menu option Connect chosen
                    if (bleDeviceAddress != null) {                                                 //Check that there is a valid Bluetooth LE address
                        stateConnection = StateConnection.CONNECTING;                               //Have an address so we are going to start connecting
                        connectWithAddress(bleDeviceAddress);                                       //Call method to ask the BleService to connect
                    }
                    return true;
                }
                case R.id.menu_disconnect: {                                                        //Menu option Disconnect chosen
                    stateConnection = StateConnection.DISCONNECTING;                                //StateConnection is used to determine whether disconnect event should trigger a popup to reconnect
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
                    blackProbeStatusImg.setImageResource(R.drawable.ready);
                    updateConnectionState();                                                        //Update the screen and menus
                    break;
                }
                case BleService.ACTION_BLE_DISCONNECTED: {                                          //Have disconnected from BLE device
                    Log.d(TAG, "Received Intent ACTION_BLE_DISCONNECTED");
                    initializeDisplay();                                                            //Clear the temperature and accelerometer text and graphs
                    transparentUartData.reset();                                                    //Also clear any buffered incoming data
                    stateConnection = StateConnection.DISCONNECTED;
                    blackProbeStatusImg.setImageResource(R.drawable.unconnected);

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
                    blackProbeStatusImg.setImageResource(R.drawable.calibrating);
                    stateConnection = StateConnection.CONNECTED;                                    //Were already connected but showing discovering, not connected
                    updateConnectionState();                                                        //Update the screen and menus
                    Log.i(TAG, "PJH - about to request Ezy config");
                    bleService.requestEzyConfig();                                                         //Ask the BleService to connect to start interrogating the device for its configuration
                    break;
                }
                case BleService.ACTION_BLE_DISCOVERY_FAILED: {                                      //Service discovery failed to find the right service and characteristics
                    Log.d(TAG, "Received Intent  ACTION_BLE_DISCOVERY_FAILED");
                    blackProbeStatusImg.setImageResource(R.drawable.unconnected);
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
                    textDeviceStatus.setText(R.string.ready);
                    blackProbeStatusImg.setImageResource(R.drawable.ready);

                    Globals.probeConnectedName = bleDeviceName;
                    Globals.probeConnectedAddress = bleDeviceAddress;
                    Log.e(TAG, "Name: " + Globals.probeConnectedName + ", Address: " + Globals.probeConnectedAddress);

                    bleService.parseBinaryCalibration();   // process thr calibration data just retrieved
                    bleService.setNotifications(true);   // PJH - HACK - find place where this write doesn't kill something else
                    haveSuitableProbeConnected = true;   // this enables the test stuff
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
                final Intent bleScanActivityIntent = new Intent(MainActivity.this, BleScanActivity.class); //Create Intent to start the BleScanActivity
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
                        stateConnection = StateConnection.DISCONNECTED;                             //Default, in case state is unknown
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
        if (bleDeviceName != null && haveSuitableProbeConnected) { //TODO @ANNA - add something here that says the probe has to be calibrated before we can move on
            Intent intent = new Intent(this, ProbeDetails.class);
            intent.putExtra(ProbeDetails.EXTRA_DEVICE_NAME, bleDeviceName);
            intent.putExtra(ProbeDetails.EXTRA_DEVICE_ADDRESS, bleDeviceAddress);
            intent.putExtra(ProbeDetails.EXTRA_DEVICE_CONNECTION_STATUS, haveSuitableProbeConnected);
            startActivity(intent);
        } else {
            //TODO make a popup that says device not connected cannot get data
            Intent intent = new Intent(this, ProbeDetails.class);
            intent.putExtra(ProbeDetails.EXTRA_DEVICE_NAME, bleDeviceName);
            intent.putExtra(ProbeDetails.EXTRA_DEVICE_ADDRESS, bleDeviceAddress);
            intent.putExtra(ProbeDetails.EXTRA_DEVICE_CONNECTION_STATUS, haveSuitableProbeConnected);
            startActivity(intent);
        }
    }

    public void BlackProbeBtnClick(View v) {
        if (haveSuitableProbeConnected) {
            Intent intent = new Intent(this, InitalisePopupActivity.class);
            Log.d(TAG, "Device Name: " + bleDeviceName + ", Device Address: " + bleDeviceAddress);
            intent.putExtra(InitalisePopupActivity.EXTRA_DEVICE_NAME, bleDeviceName);
            intent.putExtra(InitalisePopupActivity.EXTRA_DEVICE_ADDRESS, bleDeviceAddress);
            startActivity(intent);
        } else {
            Log.e(TAG, "Probe is disconnected");
            stateConnection = StateConnection.DISCONNECTED;
            blackProbeStatusImg.setImageResource(R.drawable.unconnected);
            if ((bleDeviceName != null) && (bleDeviceAddress != null) && bleService.isCalibrated()) {                                                //See if there is a device name
                // attempt a reconnection
                stateConnection = StateConnection.CONNECTING;                               //Got an address so we are going to start connecting
                connectWithAddress(bleDeviceAddress);                                       //Initiate a connection
            }
            updateConnectionState();
        }
    }

    public void holeIDBtnClick(View v) {
        Intent intent = new Intent(this, SurveyOptionsActivity.class);
        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_NAME, bleDeviceName);
        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_ADDRESS, bleDeviceAddress);
        startActivity(intent);
    }

    public void operatorIDBtnClick(View v) {
        Intent intent = new Intent(this, SurveyOptionsActivity.class);
        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_NAME, bleDeviceName);
        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_ADDRESS, bleDeviceAddress);
        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_CONNECTION, haveSuitableProbeConnected);
        startActivity(intent);
    }

    public void reconnect(View view) {
        if ((bleDeviceName != null) && (bleDeviceAddress != null) && bleService.isCalibrated()) {                                                //See if there is a device name
            // attempt a reconnection
            stateConnection = StateConnection.CONNECTING;                               //Got an address so we are going to start connecting
            connectWithAddress(bleDeviceAddress);                                       //Initiate a connection
        }

        updateConnectionState();
    }
}