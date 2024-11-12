package com.work.libtest;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.ByteArrayOutputStream;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.*;  // for cal date
import java.text.*;  // for cal date


/**
 * Service for handling Bluetooth communication with a Bluetooth Low Energy module.
 *
 */

public class BleService extends Service {
    private final static String TAG = BleService.class.getSimpleName();                             //Service name for logging messages on the ADB

    private final static String ACTION_ADAPTER_STATE_CHANGED = "android.bluetooth.adapter.action.STATE_CHANGED";           //Identifier for Intent that announces a change in the state of the Bluetooth radio
    private final static String EXTRA_ADAPTER_STATE = "android.bluetooth.adapter.extra.STATE";                    //Identifier for Bluetooth connection state attached to state changed Intent
    public final static String ACTION_BLE_CONNECTED = "com.example.javatest.ACTION_BLE_CONNECTED";         //Identifier for Intent to announce that a BLE device connected
    public final static String ACTION_BLE_DISCONNECTED = "com.example.javatest.ACTION_BLE_DISCONNECTED";      //Identifier for Intent to announce that a BLE device disconnected
    public final static String ACTION_BLE_DISCOVERY_DONE = "com.example.javatest.ACTION_BLE_DISCOVERY_DONE";    //Identifier for Intent to announce that service discovery is complete
    public final static String ACTION_BLE_DISCOVERY_FAILED = "com.example.javatest.ACTION_BLE_DISCOVERY_FAILED";  //Identifier for Intent to announce that service discovery failed to find the service and characteristics
    public final static String ACTION_BLE_CONFIG_READY = "com.example.javatest.ACTION_BLE_CONFIG_READY"; //Identifier for Intent to announce a new characteristic notification
    public final static String ACTION_BLE_NEW_DATA_RECEIVED = "com.example.javatest.ACTION_BLE_NEW_DATA_RECEIVED"; //Identifier for Intent to announce a new characteristic notification
    public final static String ACTION_BLE_FETCH_CAL = "com.example.javatest.ACTION_BLE_FETCH_CAL"; //Identifier for Intent to announce a new characteristic notification

    //public final static String ACTION_BLE_CHARACTERISTIC_READ =  "com.example.javatest.ACTION_BLE_CHARACTERISTIC_READ"; //Identifier for Intent to announce a new characteristic notification


    //private final static UUID UUID_TRANSPARENT_PRIVATE_SERVICE = UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455"); //Private service for Transparent UART
    //private final static UUID UUID_TRANSPARENT_SEND_CHAR =       UUID.fromString("49535343-8841-43f4-a8d4-ecbe34729bb3"); //Characteristic for Transparent UART to send to RN or BM module, properties - write, write no response
    //private final static UUID UUID_TRANSPARENT_RECEIVE_CHAR =    UUID.fromString("49535343-1e4d-4bd9-ba61-23c647249616"); //Characteristic for Transparent UART to receive from RN or BM module, properties - notify, write, write no response

    // PJH - don't know where this magic number comes from - initially assume it is a generic constant - CORRECT LightBlue lists this value for BoreShot
    private final static UUID UUID_CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"); //Descriptor to enable notification for a characteristic


    //
    // BLE API for Borecam, Corecam and Ezy products
    //
    private final static UUID UUID_CAMERA_SERVICE = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"); //Private service for Camera

    private final static UUID UUID_DEVICE_ID_CHAR = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb"); //Characteristic for Major Version Number, properties - read, write
    private final static UUID UUID_PROBE_MODE_CHAR = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb"); //Characteristic for Major Version Number, properties - read, write
    private final static UUID UUID_SHOT_INTERVAL_CHAR = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb"); //Characteristic for Major Version Number, properties - read, write
    private final static UUID UUID_RECORD_COUNT_CHAR = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb"); //Characteristic for Major Version Number, properties - read, write
    private final static UUID UUID_SHOT_REQUEST_CHAR = UUID.fromString("0000fff5-0000-1000-8000-00805f9b34fb"); //Characteristic for Major Version Number, properties - read, write
    private final static UUID UUID_SURVEY_MAX_SHOTS_CHAR = UUID.fromString("0000fff6-0000-1000-8000-00805f9b34fb"); //Characteristic for Major Version Number, properties - read, write
    // probe type (0xFFF7) is not used (it always returns 'Borecam')
    private final static UUID UUID_BORE_SHOT_CHAR = UUID.fromString("0000fff8-0000-1000-8000-00805f9b34fb"); //Characteristic for Major Version Number, properties - read, write
    private final static UUID UUID_CORE_SHOT_CHAR = UUID.fromString("0000fff9-0000-1000-8000-00805f9b34fb"); //Characteristic for Major Version Number, properties - read, write
    // there is no 0xFFFA
    private final static UUID UUID_DEVICE_ADDRESS_CHAR = UUID.fromString("0000fffb-0000-1000-8000-00805f9b34fb"); //Characteristic for Major Version Number, properties - read, write
    private final static UUID UUID_MAJOR_VERSION_NUMBER_CHAR = UUID.fromString("0000fffc-0000-1000-8000-00805f9b34fb"); //Characteristic for Major Version Number, properties - read
    private final static UUID UUID_MINOR_VERSION_NUMBER_CHAR = UUID.fromString("0000fffd-0000-1000-8000-00805f9b34fb"); //Characteristic for Minor Version Number, properties - read
    private final static UUID UUID_ROLLING_SHOT_INTERVAL_CHAR = UUID.fromString("0000fffe-0000-1000-8000-00805f9b34fb"); //Characteristic for Major Version Number, properties - read, write
    // 0xFFE3 and 0xFFE4 are not used
    private final static UUID UUID_DEBUG_CHAR = UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb"); //Characteristic for Major Version Number, properties - read, write
    private final static UUID UUID_PROBE_NAME_CHAR = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"); //Characteristic for Major Version Number, properties - read, write
    private final static UUID UUID_DEBUG2_CHAR = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"); //Characteristic for Major Version Number, properties - read, write


    private final static UUID UUID_CALIBRATION_SERVICE = UUID.fromString("0000ff00-0000-1000-8000-00805f9b34fb"); //Private service for Calibration

    private final static UUID UUID_CALIBRATION_ADDRESS_CHAR = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb"); //Characteristic for Calibration Address, properties - write
    private final static UUID UUID_CALIBRATION_DATA_CHAR = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb"); //Characteristic for Calibration Data, properties - read, write


    //private final Queue<byte[]> characteristicWriteQueue = new LinkedList<>();                      //Queue to buffer multiple writes since the radio does one at a time
    //private final Queue<BluetoothGattDescriptor> descriptorWriteQueue = new LinkedList<>();         //Queue to buffer multiple writes since the radio does one at a time
    private BluetoothAdapter btAdapter;                                                             //BluetoothAdapter is used to control the Bluetooth radio
    private BluetoothGatt btGatt;                                                                   //BluetoothGatt is used to control the Bluetooth connection

    // one byte characteristics
    private BluetoothGattCharacteristic probeModeCharacteristic = null;                              //Characteristic used to send data from the Android device to the BM7x or RN487x module
    private BluetoothGattCharacteristic shotIntervalCharacteristic = null;                              //Characteristic used to send data from the Android device to the BM7x or RN487x module
    private BluetoothGattCharacteristic majorVersionNumberCharacteristic = null;                              //Characteristic used to send data from the Android device to the BM7x or RN487x module
    private BluetoothGattCharacteristic minorVersionNumberCharacteristic = null;                              //Characteristic used to send data from the Android device to the BM7x or RN487x module

    // two byte characteristics
    private BluetoothGattCharacteristic surveyMaxShotsCharacteristic = null;                              //Characteristic used to send data from the Android device to the BM7x or RN487x module
    private BluetoothGattCharacteristic rollingShotIntervalCharacteristic = null;                              //Characteristic used to send data from the Android device to the BM7x or RN487x module
    private BluetoothGattCharacteristic debugCharacteristic = null;                              //Characteristic used to send data from the Android device to the BM7x or RN487x module
    private BluetoothGattCharacteristic debug2Characteristic = null;                              //Characteristic used to send data from the Android device to the BM7x or RN487x module

    private BluetoothGattCharacteristic boreShotCharacteristic = null;                              //Characteristic used to send data from the Android device to the BM7x or RN487x module
    private BluetoothGattCharacteristic coreShotCharacteristic = null;                              //Characteristic used to send data from the Android device to the BM7x or RN487x module

    private BluetoothGattCharacteristic calibrationAddressCharacteristic = null;                              //Characteristic used to send data from the Android device to the BM7x or RN487x module
    private BluetoothGattCharacteristic calibrationDataCharacteristic = null;                              //Characteristic used to send data from the Android device to the BM7x or RN487x module


    private ByteArrayOutputStream transparentReceiveOutput = new ByteArrayOutputStream();           //Object to hold incoming bytes from the Transparent UART Receive characteristic until the Main Activity requests them
    private int CharacteristicSize = 20;                                                            //To keep track of the maximum length of the characteristics (always 3 less than the real MTU size to fit in opcode and handle)
    private int connectionAttemptCountdown = 0;                                                     //To keep track of connection attempts for greater reliability

    //
    // Ezy stuff
    //

    // status flags
    private boolean isFirmwareVersionValid = false;   // unused
    private boolean isCalibrated = false;
    private boolean isConfigValid = false;

    private boolean ignoreCalibration = false;  // to allow uncalibrated values to be displayed (during accel calibration setup)

    // PJH - unsure how this app would respond to a new probe, without any calibration data

    private byte[] deviceAddress = "000000".getBytes();
    private byte[] deviceId = "00000000".getBytes();

    // java doesn't have types such as UB16, so store them in a larger signed int

    // UB8
    private int majorVersionNumber = -1;
    private int minorVersionNumber = -1;
    private String firmwareVersionString = "vX.XX";

    //private byte[]  probeName = [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0];
    private byte[] probeName = "uninitialised   ".getBytes();
    private String probeNameString = "uninitialised";

    // UB8
    private int shotInterval = -1;

    private int probeMode = -1;

    //
    // Probe modes
    //
    final int PROBE_MODE_IDLE = 0;
    final int PROBE_MODE_SURVEY = 1;
    final int PROBE_MODE_ROLLING_SHOTS = 2;
    final int PROBE_MODE_ROLLING_SHOTS_ACC_ONLY = 3;
    final int PROBE_MODE_ROLLING_SHOTS_MAG_ONLY = 4;

    // UB16
    private int rollingShotInterval = -1;
    private int surveyMaxShots = -1;
    private int debugValue = -1;
    private int debug2Value = -1;

    //////////////////////////////////////////////////////////////////////
    //
    // Calibration variables, extracted from binary calibration blob
    //
    // SET BEFORE PARSING:
    private byte binCalData[] = new byte[2050];   // need to allocate enough space for entire binary cal data (128*16 bytes)
    // nominally only need about 400 bytes
    // - maybe allocate dynamically after getting size from header (page 0)

    // SET DURING PARSING:
    final int NUM_CAL_PARAMS_EXPECTED_DURING_PARSING = 38;

    private int binCalData_size = 0;  // determine the length of binCalData during parsing

    private String calibratedDateString = "No calibration data";
    private String modifiedDateString = "No calibration data";

    // calibration coefficients
    private double acc_A[] = new double[3];    // offsets
    private double acc_B[][] = new double[3][3]; // first order terms
    private double acc_C[] = new double[3];    // cubic terms

    private double mag_A[] = new double[3];
    private double mag_B[][] = new double[3][3];
    private double mag_C[] = new double[3];

    private double temp_param[] = new double[2];   // offset, scale

    private double accManualZeroOffset = 0;  // used in zero roll offset feature of corecams to align drill head
    private int offset_of_accManualZeroOffset = 0;  // where accManualZeroOffset is located in the the current cal binary

    private double magManualZeroOffset = 0;  // don't think this is used

    //
    // end of calibration variables
    //
    //////////////////////////////////////////////////////////////////////


    //
    // A ring buffer is used to hold the history of readings,
    // this allows the averaging level to be adjust dynamically (without a lag)
    // The ring buffer normally contains calibrated values,
    // but user can set ignoreCalibration, to force uncalibrated values into ring buffer
    // (for accel calibration)
    private int averagingLevel = 1;
    private int headRB = 0;
    private int countRB = 0;   // only needed during the initial fill

    final private int ringBufferSize = 122;
    private int recNum_RingBuffer[] = new int[ringBufferSize];

    private double acc_X_RingBuffer[] = new double[ringBufferSize];
    private double acc_Y_RingBuffer[] = new double[ringBufferSize];
    private double acc_Z_RingBuffer[] = new double[ringBufferSize];

    private double mag_X_RingBuffer[] = new double[ringBufferSize];
    private double mag_Y_RingBuffer[] = new double[ringBufferSize];
    private double mag_Z_RingBuffer[] = new double[ringBufferSize];

    private double roll_RingBuffer[] = new double[ringBufferSize];
    private double dip_RingBuffer[] = new double[ringBufferSize];
    private double az_RingBuffer[] = new double[ringBufferSize];
    private double temp_RingBuffer[] = new double[ringBufferSize];

    private byte latestBoreshot[] = new byte[25];    // PJH - can these be commoned into latestShot?
    private byte latestCoreshot[] = new byte[25];


    // using a state machine to walk through the interrogating configuration process
    enum statesInterrogateConfig {
        IDLE,
        WAITING_PROBE_MODE,
        WAITING_SHOT_INTERVAL,
        WAITING_MAJOR_VERSION_NUMBER,
        WAITING_MINOR_VERSION_NUMBER,
        WAITING_SURVEY_MAX_SHOTS,
        WAITING_ROLLING_SHOT_INTERVAL,
        WAITING_DEBUG,
        WAITING_DEBUG2,
        SELECT_CAL_HDR,
        //WAITING_CAL_HDR,
        RETRIEVE_CAL,        // this also uses the calPageNumber variable
        CONFIG_READY
    }

    private statesInterrogateConfig stateInterrogateConfig = statesInterrogateConfig.IDLE;
    private byte calPageNumber[] = new byte[1];
    //byte param[] = new byte[1];   // used to pass calPageNumber to

    private boolean sensorDataRecordingEnable = false;  // controlled by main record switch
    private boolean sensorDataRecordingActive = false;  // flag to allow only specific shots to be recorded
    private int sensorDataCount = 0;
    private List<sensorData> sensorDataList = new ArrayList<>();   // just recorded raw sensor data


    //
    // PJH - these conversion routines are a hack, but couldn't find a nice off the shelf solution
    //
    private int convertUb8ToInt(byte[] b) {
        int value = 0;
        value = ((int) b[0]) & 0xFF;
        return (value);
    }

    // PJH - have confirmed this endianness is correct (RollingShotInterval is returned as '1000' - 1000ms = 1 sec)
    private int convertUb16ToInt(byte[] b) {  // little endian
        int value = 0;
        value = ((((int) b[1]) & 0xFF) << 8) + (((int) b[0]) & 0xFF);
        return (value);
    }


    // ----------------------------------------------------------------------------------------------------------------
    // Binder to return a reference to this BleService so clients of the service can access it's methods

    /******************************************************************************************************************
     * Methods for handling creation and binding of the BleService.
     */

    // ----------------------------------------------------------------------------------------------------------------
    // Client Activity has bound to our Service
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Binding to BleService");
        try {
            registerReceiver(broadcastReceiver, new IntentFilter(BleService.ACTION_ADAPTER_STATE_CHANGED)); //Register receiver to handle Intents from the BluetoothAdapter
            btAdapter = BluetoothAdapter.getDefaultAdapter();                                       //Get a reference to the BluetoothAdapter
            if (btAdapter == null) {                                                                //Unlikely that there is no Bluetooth radio but best to check anyway
                Log.e(TAG, "Could not get a BluetoothAdapter on this device");
            }
        } catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
        return new LocalBinder();                                                                   //Return Binder object that the binding Activity needs to use the service
    }

    public class LocalBinder extends Binder {
        BleService getService() {
            return BleService.this;
        }
    }


    // ----------------------------------------------------------------------------------------------------------------
    // All activities have stopped using the service and it will be destroyed

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Service ends when all Activities have unbound
    // Close any existing connection
    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(broadcastReceiver);                                                  //Unregister receiver to handle Intents from the BluetoothAdapter
            if (btGatt != null) {                                                                   //See if there is an existing Bluetooth connection
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                btGatt.close();                                                                     //Close the connection as the service is ending
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
        super.onDestroy();
    }

    /******************************************************************************************************************
     * Methods for handling Intents.
     */

    // ----------------------------------------------------------------------------------------------------------------
    // BroadcastReceiver handles the intents from the BluetoothAdapter for changes in state
    // For reference only, the code does not use this information
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                final String action = intent.getAction();
                if (BleService.ACTION_ADAPTER_STATE_CHANGED.equals(action)) {                       //See if BluetoothAdapter has broadcast that its state changed
                    Log.d(TAG, "BluetoothAdapter state changed");
                    final int stateAdapter = intent.getIntExtra(BleService.EXTRA_ADAPTER_STATE, 0); //Get the current state of the adapter
                    switch (stateAdapter) {
                        case BluetoothAdapter.STATE_OFF:
                        case BluetoothAdapter.STATE_TURNING_ON:
                        case BluetoothAdapter.STATE_ON:
                        case BluetoothAdapter.STATE_TURNING_OFF:
                        default:
                    }
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
            }
        }
    };

    /******************************************************************************************************************
     * Callback methods for handling events related to the BluetoothGatt connection
     */

    // ----------------------------------------------------------------------------------------------------------------
    // GATT callback methods for GATT events such as connecting, discovering services, write completion, etc.
    private final BluetoothGattCallback btGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {         //Connected or disconnected
            try {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    connectionAttemptCountdown = 0;                                                 //Stop counting connection attempts
                    switch (newState) {
                        case BluetoothProfile.STATE_CONNECTED: {                                    //Are now connected
                            Log.i(TAG, "Connected to BLE device");
                            transparentReceiveOutput.reset();      // PJH REMOVE                                 //Reset (empty) the ByteArrayOutputStream of any data left over from a previous connection
                            sendBroadcast(new Intent(ACTION_BLE_CONNECTED));                        //Let the BleMainActivity know that we are connected by broadcasting an Intent
                            //descriptorWriteQueue.clear();                                           //Clear write queues in case there was something left in the queue from the previous connection
                            //characteristicWriteQueue.clear();
                            btGatt.discoverServices();                                              //Discover services after successful connection
                            break;
                        }
                        case BluetoothProfile.STATE_DISCONNECTED: {                                 //Are now disconnected
                            Log.i(TAG, "Disconnected from BLE device");
                            sendBroadcast(new Intent(ACTION_BLE_DISCONNECTED));                     //Let the BleMainActivity know that we are disconnected by broadcasting an Intent
                        }
                    }
                }
                else {                                                                              //Something went wrong with the connection or disconnection request
                    if (connectionAttemptCountdown-- > 0) {                                         //See if we should try another attempt at connecting
                        gatt.connect();                                                             //Use the existing BluetoothGatt to try connect
                        Log.d(TAG, "Connection attempt failed, trying again");
                    }
                    else if (newState == BluetoothProfile.STATE_DISCONNECTED) {                     //Not trying another connection attempt and are not connected
                        sendBroadcast(new Intent(ACTION_BLE_DISCONNECTED));                         //Let the BleMainActivity know that we are disconnected by broadcasting an Intent
                        Log.i(TAG, "Unexpectedly disconnected from BLE device");
                    }
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {                          //Service discovery completed

            try {
                boolean discoveryFailed = false;                                                    //Record any failures as services, characteristics, and descriptors are requested
                //transparentSendCharacteristic = null;                                               //Have not found characteristic yet

                // UB8
                probeModeCharacteristic = null;
                shotIntervalCharacteristic = null;
                majorVersionNumberCharacteristic = null;
                minorVersionNumberCharacteristic = null;

                // UB16
                surveyMaxShotsCharacteristic = null;                              //Characteristic used to send data from the Android device to the BM7x or RN487x module
                rollingShotIntervalCharacteristic = null;                              //Characteristic used to send data from the Android device to the BM7x or RN487x module
                debugCharacteristic = null;                              //Characteristic used to send data from the Android device to the BM7x or RN487x module
                debug2Characteristic = null;                              //Characteristic used to send data from the Android device to the BM7x or RN487x module

                boreShotCharacteristic = null;    // 20 bytes
                coreShotCharacteristic = null;    // 12 bytes

                calibrationAddressCharacteristic = null;  //  1 byte
                calibrationDataCharacteristic    = null;  // 16 bytes





                if (status == BluetoothGatt.GATT_SUCCESS) {                                         //See if service discovery was successful
                    BluetoothGattService gattCameraService = gatt.getService(UUID_CAMERA_SERVICE); //Get the Transparent UART service
                    if (gattCameraService != null) {                                                      //Check that the service was discovered
                        Log.i(TAG, "PJH - Found Camera service");
                        //final BluetoothGattCharacteristic transparentReceiveCharacteristic = gattService.getCharacteristic(UUID_TRANSPARENT_RECEIVE_CHAR); //Get the characteristic for receiving from the Transparent UART
                        //if (transparentReceiveCharacteristic != null) {                             //See if the characteristic was found
                        //    Log.i(TAG, "Found Transparent Receive characteristic");
                        //    final int characteristicProperties = transparentReceiveCharacteristic.getProperties(); //Get the properties of the characteristic
                        //    if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_NOTIFY)) > 0) { //See if the characteristic has the Notify property
                        //        BluetoothGattDescriptor descriptor = transparentReceiveCharacteristic.getDescriptor(UUID_CCCD); //Get the descriptor that enables notification on the server
                        //        if (descriptor != null) {                                           //See if we got the descriptor
                        //            btGatt.setCharacteristicNotification(transparentReceiveCharacteristic, true); //If so then enable notification in the BluetoothGatt
                        //            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE); //Set the value of the descriptor to enable notification
                        //            descriptorWriteQueue.add(descriptor);                           //Put the descriptor into the write queue
                        //            if (descriptorWriteQueue.size() == 1) {                         //If there is only 1 item in the queue, then write it.  If more than 1, we handle asynchronously in the onDescriptorWrite callback below
                        //                btGatt.writeDescriptor(descriptor);                         //Write the descriptor
                        //            }
                        //        }
                        //        else {
                        //            discoveryFailed = true;
                        //            Log.w(TAG, "No CCCD descriptor for Transparent Receive characteristic");
                        //        }
                        //    }
                        //    else {
                        //        discoveryFailed = true;
                        //        Log.w(TAG, "Transparent Receive characteristic does not have notify property");
                        //    }
                        //}
                        //else {
                        //    discoveryFailed = true;
                        //    Log.w(TAG, "Did not find Transparent Receive characteristic");
                        //}

                        probeModeCharacteristic = gattCameraService.getCharacteristic(UUID_PROBE_MODE_CHAR);
                        if (initWritableCharacteristic(probeModeCharacteristic, "Probe Mode") == true) { discoveryFailed = true; }

                        shotIntervalCharacteristic = gattCameraService.getCharacteristic(UUID_SHOT_INTERVAL_CHAR);
                        if (initWritableCharacteristic(shotIntervalCharacteristic, "Shot Interval") == true) { discoveryFailed = true; }

                        majorVersionNumberCharacteristic = gattCameraService.getCharacteristic(UUID_MAJOR_VERSION_NUMBER_CHAR);
                        if (initNonWritableCharacteristic(shotIntervalCharacteristic, "Major Version Number") == true) { discoveryFailed = true; }

                        minorVersionNumberCharacteristic = gattCameraService.getCharacteristic(UUID_MINOR_VERSION_NUMBER_CHAR);
                        if (initNonWritableCharacteristic(shotIntervalCharacteristic, "MinorVersionNumber") == true) { discoveryFailed = true; }


                        surveyMaxShotsCharacteristic = gattCameraService.getCharacteristic(UUID_SURVEY_MAX_SHOTS_CHAR);
                        if (initNonWritableCharacteristic(surveyMaxShotsCharacteristic, "Survey Max Shots") == true) { discoveryFailed = true; }

                        rollingShotIntervalCharacteristic = gattCameraService.getCharacteristic(UUID_ROLLING_SHOT_INTERVAL_CHAR);
                        if (initWritableCharacteristic(rollingShotIntervalCharacteristic, "Rolling Shot Interval") == true) { discoveryFailed = true; }

                        debugCharacteristic = gattCameraService.getCharacteristic(UUID_DEBUG_CHAR);
                        if (initWritableCharacteristic(debugCharacteristic, "Debug") == true) { discoveryFailed = true; }

                        debug2Characteristic = gattCameraService.getCharacteristic(UUID_DEBUG2_CHAR);
                        if (initWritableCharacteristic(debug2Characteristic, "Debug2") == true) { discoveryFailed = true; }


                        //
                        // set up Boreshot characteristic TODO Coreshot similarly
                        //
                        //final BluetoothGattCharacteristic
                        /*
                        boreShotCharacteristic = gattCameraService.getCharacteristic(UUID_BORE_SHOT_CHAR); //Get the characteristic for receiving from the Transparent UART
                        if (boreShotCharacteristic != null) {                             //See if the characteristic was found
                            Log.i(TAG, "PJH - Found Bore shot characteristic");
                            final int characteristicProperties = boreShotCharacteristic.getProperties(); //Get the properties of the characteristic
                            if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_NOTIFY)) > 0) { //See if the characteristic has the Notify property
                                BluetoothGattDescriptor descriptor = boreShotCharacteristic.getDescriptor(UUID_CCCD); //Get the descriptor that enables notification on the server
                                if (descriptor != null) {                                           //See if we got the descriptor
                                    btGatt.setCharacteristicNotification(boreShotCharacteristic, true); //If so then enable notification in the BluetoothGatt
                                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE); //Set the value of the descriptor to enable notification

                                    // PJH - we have a potential problem here - is it currently safe to write to BLE??? (cross our fingers) - FAILING on the next Read 8-(
                                    btGatt.writeDescriptor(descriptor);                         //Write the descriptor
                                    Log.i(TAG, "PJH - Have (hopefully) enabled notifications for Bore shot characteristic");
                                    //descriptorWriteQueue.add(descriptor);                           //Put the descriptor into the write queue
                                    //if (descriptorWriteQueue.size() == 1) {                         //If there is only 1 item in the queue, then write it.  If more than 1, we handle asynchronously in the onDescriptorWrite callback below
                                    //    btGatt.writeDescriptor(descriptor);                         //Write the descriptor
                                    //}
                                }
                                else {
                                    discoveryFailed = true;
                                    Log.w(TAG, "PJH - No CCCD descriptor for Bore Shot characteristic");
                                }
                            }
                            else {
                                discoveryFailed = true;
                                Log.w(TAG, "PJH - Bore Shot characteristic does not have notify property");
                            }
                        }
                        else {
                            discoveryFailed = true;
                            Log.w(TAG, "PJH - Did not find Bore Shot characteristic");
                        }

                         */





                    /*
                        probeModeCharacteristic = gattCameraService.getCharacteristic(UUID_PROBE_MODE_CHAR); //Get the Major Version Number characteristic
                        if (probeModeCharacteristic != null) {                                //See if the characteristic was found
                            Log.i(TAG, "PJH - Found Probe Mode characteristic");
                            final int characteristicProperties = probeModeCharacteristic.getProperties(); //Get the properties of the characteristic
                            if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) { //See if the characteristic has the Write (unacknowledged) property
                                probeModeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE); //If so then set the write type (write with no acknowledge) in the BluetoothGatt
                            } else if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE)) > 0) { //Else see if the characteristic has the Write (acknowledged) property
                                probeModeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT); //If so then set the write type (write with no acknowledge) in the BluetoothGatt
                            } else {
                                discoveryFailed = true;
                                Log.w(TAG, "PJH - Probe Mode characteristic does not have write property");
                            }
                        }
                        else {
                            discoveryFailed = true;
                            Log.w(TAG, "PJH - Did not find Probe Mode characteristic");
                        }

                     */

                        /*
                        shotIntervalCharacteristic = gattCameraService.getCharacteristic(UUID_SHOT_INTERVAL_CHAR); //Get the Major Version Number characteristic
                        if (shotIntervalCharacteristic != null) {                                //See if the characteristic was found
                            Log.i(TAG, "PJH - Found Shot Interval characteristic");
                            final int characteristicProperties = shotIntervalCharacteristic.getProperties(); //Get the properties of the characteristic
                            if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) { //See if the characteristic has the Write (unacknowledged) property
                                shotIntervalCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE); //If so then set the write type (write with no acknowledge) in the BluetoothGatt
                            } else if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE)) > 0) { //Else see if the characteristic has the Write (acknowledged) property
                                shotIntervalCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT); //If so then set the write type (write with no acknowledge) in the BluetoothGatt
                            } else {
                                discoveryFailed = true;
                                Log.w(TAG, "PJH - Shot Interval characteristic does not have write property");
                            }
                        }
                        else {
                            discoveryFailed = true;
                            Log.w(TAG, "PJH - Did not find Shot Interval characteristic");
                        }

                         */

/*
                        majorVersionNumberCharacteristic = gattCameraService.getCharacteristic(UUID_MAJOR_VERSION_NUMBER_CHAR); //Get the Major Version Number characteristic
                        if (majorVersionNumberCharacteristic != null) {                                //See if the characteristic was found
                            Log.i(TAG, "PJH - Found Major Version Number characteristic");
                            final int characteristicProperties = majorVersionNumberCharacteristic.getProperties(); //Get the properties of the characteristic
                            //if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) { //See if the characteristic has the Write (unacknowledged) property
                            //    majorVersionNumberCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE); //If so then set the write type (write with no acknowledge) in the BluetoothGatt
                            //} else if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE)) > 0) { //Else see if the characteristic has the Write (acknowledged) property
                            //    majorVersionNumberCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT); //If so then set the write type (write with no acknowledge) in the BluetoothGatt
                            //} else {
                            //    discoveryFailed = true;
                            //    Log.w(TAG, "Major Version Number characteristic does not have write property");
                            //}
                        }
                        else {
                            discoveryFailed = true;
                            Log.w(TAG, "PJH - Did not find Major Version Number characteristic");
                        }

                        minorVersionNumberCharacteristic = gattCameraService.getCharacteristic(UUID_MINOR_VERSION_NUMBER_CHAR); //Get the Major Version Number characteristic
                        if (minorVersionNumberCharacteristic != null) {                                //See if the characteristic was found
                            Log.i(TAG, "PJH - Found Minor Version Number characteristic");
                            final int characteristicProperties = minorVersionNumberCharacteristic.getProperties(); //Get the properties of the characteristic
                            //if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) { //See if the characteristic has the Write (unacknowledged) property
                            //    majorVersionNumberCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE); //If so then set the write type (write with no acknowledge) in the BluetoothGatt
                            //} else if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE)) > 0) { //Else see if the characteristic has the Write (acknowledged) property
                            //    majorVersionNumberCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT); //If so then set the write type (write with no acknowledge) in the BluetoothGatt
                            //} else {
                            //    discoveryFailed = true;
                            //    Log.w(TAG, "Major Version Number characteristic does not have write property");
                            //}
                        }
                        else {
                            discoveryFailed = true;
                            Log.w(TAG, "PJH - Did not find Minor Version Number characteristic");
                        }



 */

                    }
                    else {
                        discoveryFailed = true;
                        Log.w(TAG, "PJH - Did not find Camera service");
                    }

                    BluetoothGattService gattCalibrationService = gatt.getService(UUID_CALIBRATION_SERVICE); //Get the Transparent UART service
                    if (gattCalibrationService != null) {                                                      //Check that the service was discovered
                        Log.i(TAG, "PJH - Found Calibration service");


                        calibrationAddressCharacteristic = gattCalibrationService.getCharacteristic(UUID_CALIBRATION_ADDRESS_CHAR); //Get the Calibration Address characteristic
                        if (initNonWritableCharacteristic(calibrationAddressCharacteristic, "Calibration Address") == true) { discoveryFailed = true; }

                        calibrationDataCharacteristic = gattCalibrationService.getCharacteristic(UUID_CALIBRATION_DATA_CHAR); //Get the Calibration Data characteristic
                        if (initWritableCharacteristic(calibrationAddressCharacteristic, "Calibration Data") == true) { discoveryFailed = true; }

/*
                        probeModeCharacteristic = gattCameraService.getCharacteristic(UUID_CALIBRATION_ADDRESS_CHAR); //Get the Calibration Address characteristic
                        if (probeModeCharacteristic != null) {                                //See if the characteristic was found
                            Log.i(TAG, "Found Calibration Address characteristic");
                            final int characteristicProperties = probeModeCharacteristic.getProperties(); //Get the properties of the characteristic
                            if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) { //See if the characteristic has the Write (unacknowledged) property
                                probeModeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE); //If so then set the write type (write with no acknowledge) in the BluetoothGatt
                            } else if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE)) > 0) { //Else see if the characteristic has the Write (acknowledged) property
                                probeModeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT); //If so then set the write type (write with no acknowledge) in the BluetoothGatt
                            } else {
                                discoveryFailed = true;
                                Log.w(TAG, "Calibration Address characteristic does not have write property");
                            }
                        }
                        else {
                            discoveryFailed = true;
                            Log.w(TAG, "Did not find Calibration Address characteristic");
                        }

                        probeModeCharacteristic = gattCameraService.getCharacteristic(UUID_CALIBRATION_DATA_CHAR); //Get the Calibration Data characteristic
                        if (probeModeCharacteristic != null) {                                //See if the characteristic was found
                            Log.i(TAG, "Found Calibration Data characteristic");
                            final int characteristicProperties = probeModeCharacteristic.getProperties(); //Get the properties of the characteristic
                            if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) { //See if the characteristic has the Write (unacknowledged) property
                                probeModeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE); //If so then set the write type (write with no acknowledge) in the BluetoothGatt
                            } else if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE)) > 0) { //Else see if the characteristic has the Write (acknowledged) property
                                probeModeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT); //If so then set the write type (write with no acknowledge) in the BluetoothGatt
                            } else {
                                discoveryFailed = true;
                                Log.w(TAG, "Calibration Data characteristic does not have write property");
                            }
                        }
                        else {
                            discoveryFailed = true;
                            Log.w(TAG, "Did not find Calibration Data characteristic");
                        }
 */
                    }
                    else {
                        discoveryFailed = true;
                        Log.w(TAG, "PJH - Did not find Calibration service");
                    }




                }
                else {
                    discoveryFailed = true;
                    Log.w(TAG, "PJH - Failed service discovery with status: " + status);
                }


                if (!discoveryFailed) {                                                             //Service discovery returned the correct service and characteristics
                    // btGatt.requestMtu(512);     think cc2541 is limited to 20/23                                                    //Request max data length and get the negotiated length in mtu argument of onMtuChanged()
                    sendBroadcast(new Intent(ACTION_BLE_DISCOVERY_DONE));                           //Broadcast Intent to announce the completion of service discovery
                    Log.i(TAG, "PJH - BLE discovery successful");
                }
                else {
                    sendBroadcast(new Intent(ACTION_BLE_DISCOVERY_FAILED));                         //Broadcast Intent to announce the failure of service discovery
                    Log.i(TAG, "PJH - BLE discovery FAILED");
                }
            }
            catch (Exception e) {
                Log.e(TAG, "PJH - Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
            }
            Log.i(TAG, "PJH - exiting onServicesDiscovered");
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {                         //A new maximum transmission unit (MTU) size was negotiated with the Bluetooth device
            super.onMtuChanged(gatt, mtu, status);
            CharacteristicSize = mtu - 3;                                                           //The mtu argument indicates the size of a characteristic using Data Length Extension. It includes space for 1 byte opcode and 2 byte handle in addition to data so subtract 3.
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) { //Received notification or indication with a new value for a characteristic
            Log.i(TAG, "PJH - entering onCharacteristicChanged");
            try {

                //
                // Just received a new Boreshot
                //
                if (UUID_BORE_SHOT_CHAR.equals(characteristic.getUuid())) {               //See if it is the Transparent Receive characteristic (the only notification expected)
                    Log.d(TAG, "New boreshot notification");
                    latestBoreshot = characteristic.getValue();                      //Get the bytes from the characteristic and put them in the ByteArrayOutputStream for later
                    processBoreshot();
                    sendBroadcast(new Intent(ACTION_BLE_NEW_DATA_RECEIVED));                        //Broadcast Intent to announce the new data. This does not send the data, it needs to be read by calling readFromTransparentUART() below
                }

                //
                // Just received a new Coreshot
                //
                if (UUID_CORE_SHOT_CHAR.equals(characteristic.getUuid())) {               //See if it is the Transparent Receive characteristic (the only notification expected)
                    Log.d(TAG, "New coreshot notification");
                    latestCoreshot = characteristic.getValue();                      //Get the bytes from the characteristic and put them in the ByteArrayOutputStream for later
                    processCoreshot();
                    sendBroadcast(new Intent(ACTION_BLE_NEW_DATA_RECEIVED));                        //Broadcast Intent to announce the new data. This does not send the data, it needs to be read by calling readFromTransparentUART() below
                }

                /*
                if (UUID_MAJOR_VERSION_NUMBER_CHAR.equals(characteristic.getUuid())) {               //See if it is the Transparent Receive characteristic (the only notification expected)
                    Log.d(TAG, "New notification or indication");
                    transparentReceiveOutput.write(characteristic.getValue());                      //Get the bytes from the characteristic and put them in the ByteArrayOutputStream for later
                    sendBroadcast(new Intent(ACTION_BLE_NEW_DATA_RECEIVED));                        //Broadcast Intent to announce the new data. This does not send the data, it needs to be read by calling readFromTransparentUART() below
                }

                 */
            }
            catch (Exception e) {
                Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) { //Write completed
            Log.i(TAG, "PJH - entering onCharacteristicWrite");
            try {
                if (status != BluetoothGatt.GATT_SUCCESS) {                                         //See if the write was successful
                    Log.w(TAG, "PJH - Error writing GATT characteristic with status: " + status);
                }

                if (UUID_CALIBRATION_ADDRESS_CHAR.equals(characteristic.getUuid())) {               //See if it is the Transparent Receive characteristic (the only notification expected)
                    Log.d(TAG, "PJH - Have written Cal Address char");
                    // WARNING GetValue is deprecated in API 33
                    //debug2Value = convertUb16ToInt(characteristic.getValue());                      //Get the bytes from the characteristic and put them in the ByteArrayOutputStream for later

                    // PJH - TODO can we verify the value written?

                    if (stateInterrogateConfig == statesInterrogateConfig.RETRIEVE_CAL) {
                        // request the next characteristic
                        // TEMPORARY HALT HERE TO TEST
                        //stateInterrogateConfig = statesInterrogateConfig.RETRIEVE_CAL;
                        ezyReadCharacteristic(calibrationDataCharacteristic);
                        Log.i(TAG, "PJH - selecting next cal data packet");
                        //sendBroadcast(new Intent(ACTION_BLE_CONFIG_READY));  // PJH - finished (for now) - in progress
                    }
                }





                //A queue is used because BluetoothGatt can only do one write at a time
                Log.d(TAG, "PJH - Characteristic write completed");
                //characteristicWriteQueue.remove();                                                  //Pop the item that we just finishing writing
                //if(characteristicWriteQueue.size() > 0) {                                           //See if there is more to write
                //    transparentSendCharacteristic.setValue(characteristicWriteQueue.element());     //Set the new value of the characteristic
                //    btGatt.writeCharacteristic(transparentSendCharacteristic);                      //Write characteristic
                //}
            }
            catch (Exception e) {
                Log.e(TAG, "PJH - Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) { //Write descriptor completed
            try {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.w(TAG, "Error writing GATT descriptor with status: " + status);
                }                                                                                   //A queue is used because BluetoothGatt can only do one write at a time
                Log.d(TAG, "Descriptor write completed");
                //descriptorWriteQueue.remove();                                                      //Pop the item that we just finishing writing
                //if(descriptorWriteQueue.size() > 0) {                                               //See if there are more descriptors to write
                //    btGatt.writeDescriptor(descriptorWriteQueue.element());                         //Write descriptor
                //}
            }
            catch (Exception e) {
                Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
            }
        }


        //@Override
        //public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG, "PJH - entering onCharacteristicRead");
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "PJH - characteristicRead FAILED");
            }
            try {
                //
                // Handle each type of characteristic, that can be read (and any state machines, that chain commands)
                //
                if (UUID_PROBE_MODE_CHAR.equals(characteristic.getUuid())) {               //See if it is the Transparent Receive characteristic (the only notification expected)
                    Log.d(TAG, "PJH - Have read probe mode char");
                    // WARNING GetValue is deprecated in API 33
                    probeMode = convertUb8ToInt(characteristic.getValue());                      //Get the bytes from the characteristic and put them in the ByteArrayOutputStream for later
                    if (stateInterrogateConfig == statesInterrogateConfig.WAITING_PROBE_MODE) {
                        // request the next characteristic
                        stateInterrogateConfig = statesInterrogateConfig.WAITING_SHOT_INTERVAL;
                        ezyReadCharacteristic(shotIntervalCharacteristic);
                        Log.i(TAG, "PJH - starting shot interval read");
                    }
                }
                else if (UUID_SHOT_INTERVAL_CHAR.equals(characteristic.getUuid())) {               //See if it is the Transparent Receive characteristic (the only notification expected)
                    Log.d(TAG, "PJH - Have read shot interval char");
                    // WARNING GetValue is deprecated in API 33
                    shotInterval = convertUb8ToInt(characteristic.getValue());                      //Get the bytes from the characteristic and put them in the ByteArrayOutputStream for later
                    if (stateInterrogateConfig == statesInterrogateConfig.WAITING_SHOT_INTERVAL) {
                        // request the next characteristic
                        stateInterrogateConfig = statesInterrogateConfig.WAITING_MAJOR_VERSION_NUMBER;
                        ezyReadCharacteristic(majorVersionNumberCharacteristic);
                        Log.i(TAG, "PJH - starting major version read");
                        //sendBroadcast(new Intent(ACTION_BLE_CONFIG_READY));
                    }

                }
                else if (UUID_MAJOR_VERSION_NUMBER_CHAR.equals(characteristic.getUuid())) {               //See if it is the Transparent Receive characteristic (the only notification expected)
                    Log.d(TAG, "PJH - Have read major version number char");
                    // WARNING GetValue is deprecated in API 33
                    majorVersionNumber = convertUb8ToInt(characteristic.getValue());                      //Get the bytes from the characteristic and put them in the ByteArrayOutputStream for later
                    if (stateInterrogateConfig == statesInterrogateConfig.WAITING_MAJOR_VERSION_NUMBER) {
                        // request the next characteristic
                        stateInterrogateConfig = statesInterrogateConfig.WAITING_MINOR_VERSION_NUMBER;
                        ezyReadCharacteristic(minorVersionNumberCharacteristic);
                        Log.i(TAG, "PJH - starting minor version read");
                        //sendBroadcast(new Intent(ACTION_BLE_CONFIG_READY));
                    }

                }
                else if (UUID_MINOR_VERSION_NUMBER_CHAR.equals(characteristic.getUuid())) {               //See if it is the Transparent Receive characteristic (the only notification expected)
                    Log.d(TAG, "PJH - Have read minor version number char");
                    // WARNING GetValue is deprecated in API 33
                    minorVersionNumber = convertUb8ToInt(characteristic.getValue());                      //Get the bytes from the characteristic and put them in the ByteArrayOutputStream for later
                    if (stateInterrogateConfig == statesInterrogateConfig.WAITING_MINOR_VERSION_NUMBER) {
                        // request the next characteristic
                        // TEMPORARY HALT HERE TO TEST
                        stateInterrogateConfig = statesInterrogateConfig.WAITING_SURVEY_MAX_SHOTS;
                        ezyReadCharacteristic(surveyMaxShotsCharacteristic);
                        Log.i(TAG, "PJH - starting Survey Max Shots read");
                        //sendBroadcast(new Intent(ACTION_BLE_CONFIG_READY));
                    }
                }
                else if (UUID_SURVEY_MAX_SHOTS_CHAR.equals(characteristic.getUuid())) {               //See if it is the Transparent Receive characteristic (the only notification expected)
                    Log.d(TAG, "PJH - Have read Survey Max Shots char");
                    // WARNING GetValue is deprecated in API 33
                    surveyMaxShots = convertUb16ToInt(characteristic.getValue());                      //Get the bytes from the characteristic and put them in the ByteArrayOutputStream for later
                    if (stateInterrogateConfig == statesInterrogateConfig.WAITING_SURVEY_MAX_SHOTS) {
                        // request the next characteristic
                        // TEMPORARY HALT HERE TO TEST
                        stateInterrogateConfig = statesInterrogateConfig.WAITING_ROLLING_SHOT_INTERVAL;
                        ezyReadCharacteristic(rollingShotIntervalCharacteristic);
                        Log.i(TAG, "PJH - starting Rolling Shot Interval read");
                        //sendBroadcast(new Intent(ACTION_BLE_CONFIG_READY));
                    }
                }
                else if (UUID_ROLLING_SHOT_INTERVAL_CHAR.equals(characteristic.getUuid())) {               //See if it is the Transparent Receive characteristic (the only notification expected)
                    Log.d(TAG, "PJH - Have read Rolling Shot Interval char");
                    // WARNING GetValue is deprecated in API 33
                    rollingShotInterval = convertUb16ToInt(characteristic.getValue());                      //Get the bytes from the characteristic and put them in the ByteArrayOutputStream for later
                    if (stateInterrogateConfig == statesInterrogateConfig.WAITING_ROLLING_SHOT_INTERVAL) {
                        // request the next characteristic
                        // TEMPORARY HALT HERE TO TEST
                        stateInterrogateConfig = statesInterrogateConfig.WAITING_DEBUG;
                        ezyReadCharacteristic(debugCharacteristic);
                        Log.i(TAG, "PJH - starting Debug read");
                        //sendBroadcast(new Intent(ACTION_BLE_CONFIG_READY));
                    }
                }
                else if (UUID_DEBUG_CHAR.equals(characteristic.getUuid())) {               //See if it is the Transparent Receive characteristic (the only notification expected)
                    Log.d(TAG, "PJH - Have read Debug char");
                    // WARNING GetValue is deprecated in API 33
                    debugValue = convertUb16ToInt(characteristic.getValue());                      //Get the bytes from the characteristic and put them in the ByteArrayOutputStream for later
                    if (stateInterrogateConfig == statesInterrogateConfig.WAITING_DEBUG) {
                        // request the next characteristic
                        // TEMPORARY HALT HERE TO TEST
                        stateInterrogateConfig = statesInterrogateConfig.WAITING_DEBUG2;
                        ezyReadCharacteristic(debug2Characteristic);
                        Log.i(TAG, "PJH - starting Debug2 read");
                        //sendBroadcast(new Intent(ACTION_BLE_CONFIG_READY));
                    }
                }
                else if (UUID_DEBUG2_CHAR.equals(characteristic.getUuid())) {               //See if it is the Transparent Receive characteristic (the only notification expected)
                    Log.d(TAG, "PJH - Have read Debug2 char");
                    // WARNING GetValue is deprecated in API 33
                    debug2Value = convertUb16ToInt(characteristic.getValue());                      //Get the bytes from the characteristic and put them in the ByteArrayOutputStream for later
                    if (stateInterrogateConfig == statesInterrogateConfig.WAITING_DEBUG2) {
                        // request the next characteristic
                        // TEMPORARY HALT HERE TO TEST
                        stateInterrogateConfig = statesInterrogateConfig.RETRIEVE_CAL;
                        calPageNumber[0] = 0;   // init to first page (cal header)
                        ezyWriteCharacteristic(calibrationAddressCharacteristic, 1, calPageNumber);
                        Log.i(TAG, "PJH - starting Cal Addr write");
                        sendBroadcast(new Intent(ACTION_BLE_FETCH_CAL));
                        //sendBroadcast(new Intent(ACTION_BLE_CONFIG_READY));  // PJH - finished (for now) - in progress
                    }
                }
                else if (UUID_CALIBRATION_DATA_CHAR.equals(characteristic.getUuid())) {               //See if it is the Transparent Receive characteristic (the only notification expected)
                    // PJH - remember this is on a different service
                    Log.d(TAG, "PJH - Have read Calibration data char");
                    // WARNING GetValue is deprecated in API 33
                    byte[] calPacket = characteristic.getValue();                      //Get the bytes from the characteristic and put them in the ByteArrayOutputStream for later
                    Log.i(TAG,String.format("PJH - calPacket: %02X %02X %02X %02X %02X %02X ", calPacket[0], calPacket[1], calPacket[2], calPacket[3], calPacket[4], calPacket[5]));

                    if (stateInterrogateConfig == statesInterrogateConfig.RETRIEVE_CAL) {
                        // this is 16 bytes of the calibration data, calPageNumber indicates which 16 bytes
                        // (halt here, to allow main activity to process it,
                        //  and work out how many more packets are required)

                        // copy 16 bytes from calPacket[0] to binCalData[calPageNumber*16]
                        System.arraycopy(calPacket, 0, binCalData, (calPageNumber[0]*16), calPacket.length);
                        Log.i(TAG,String.format("PJH - processing Cal packet %d", calPageNumber[0]));
                        int last_page = (( ((((int)binCalData[3])&0xFF) << 8) + (((int)binCalData[4])&0xFF) ) >> 4);  // would be better to not calculate this every time
                        if (calPageNumber[0] < last_page) {
                            // we need to get more packets
                            calPageNumber[0] += 1;   // next page
                            ezyWriteCharacteristic(calibrationAddressCharacteristic, 1, calPageNumber);
                        }
                        else {  // we have finished
                            Log.w(TAG, String.format("PJH - have successfully read %d calibration records", last_page+1));
                            stateInterrogateConfig = statesInterrogateConfig.CONFIG_READY;
                            // ANNAS CHECK ----------------------------------------------------------
                            for (int i = 0; i < binCalData.length; i++) {
                                Log.e(TAG, String.valueOf(binCalData[i]));
                            }
                            Log.i(TAG, "PJH - returning to main activity after doing interrogating config");
                            sendBroadcast(new Intent(ACTION_BLE_CONFIG_READY));  // PJH - finished (for now) - in progress
                            // HACK - force notifications on
                            //setNotifications(true);
                        }
                    }
                }
                else {  // handle generic characteristic write - NEVER USED (deliberately)
                    Log.d(TAG, "PJH - New char read (not handled)");
                    //    // WARNING GetValue is deprecated in API 33
                    //    transparentReceiveOutput.write(characteristic.getValue());                      //Get the bytes from the characteristic and put them in the ByteArrayOutputStream for later
                    //    sendBroadcast(new Intent(ACTION_BLE_CHARACTERISTIC_READ));                        //Broadcast Intent to announce the new data. This does not send the data, it needs to be read by calling readFromTransparentUART() below
                }
            }
            catch (Exception e) {
                Log.e(TAG, "PJH - Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
            }



        } //Read completed - not used because this application uses Notification or Indication to receive characteristic data

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG, "PJH - entering onDescriptorRead");
        } //Read descriptor completed - not used

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            Log.i(TAG, "PJH - entering onReliableWriteCompleted");
        }                     //Write with acknowledgement completed - not used

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.i(TAG, "PJH - entering onReadRemoteRssi");
        }                   //Read remote RSSI completed - not used
    };

    // factored out of onServiceDiscovery
    private boolean initWritableCharacteristic(BluetoothGattCharacteristic characteristic, String name) {
        boolean failed = false;

        //characteristic = gattCameraService.getCharacteristic(UUID_PROBE_MODE_CHAR); //Get the Major Version Number characteristic
        if (characteristic != null) {                                //See if the characteristic was found
            Log.i(TAG, String.format("PJH - Found %s characteristic", name));
            final int characteristicProperties = characteristic.getProperties(); //Get the properties of the characteristic
            if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) { //See if the characteristic has the Write (unacknowledged) property
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE); //If so then set the write type (write with no acknowledge) in the BluetoothGatt
            } else if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE)) > 0) { //Else see if the characteristic has the Write (acknowledged) property
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT); //If so then set the write type (write with no acknowledge) in the BluetoothGatt
            } else {
                failed = true;
                Log.w(TAG, String.format("PJH - %s characteristic does not have write property", name));
            }
        } else {
            failed = true;
            Log.w(TAG, String.format("PJH - Did not find %s characteristic", name));
        }
        return failed;
    }


    // factored out of onServiceDiscovery
    private boolean initNonWritableCharacteristic(BluetoothGattCharacteristic characteristic, String name) {
        boolean failed = false;

        //characteristic = gattCameraService.getCharacteristic(UUID_PROBE_MODE_CHAR); //Get the Major Version Number characteristic
        if (characteristic != null) {                                //See if the characteristic was found
            Log.i(TAG, String.format("PJH - Found %s characteristic", name));
            //final int characteristicProperties = characteristic.getProperties(); //Get the properties of the characteristic
            //if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) { //See if the characteristic has the Write (unacknowledged) property
            //    characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE); //If so then set the write type (write with no acknowledge) in the BluetoothGatt
            //} else if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE)) > 0) { //Else see if the characteristic has the Write (acknowledged) property
            //    characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT); //If so then set the write type (write with no acknowledge) in the BluetoothGatt
            //} else {
            //    failed = true;
            //    Log.w(TAG, String.format("PJH - %s characteristic does not have write property", name));
            //}
        } else {
            failed = true;
            Log.w(TAG, String.format("PJH - Did not find %s characteristic", name));
        }
        return failed;
    }



    /******************************************************************************************************************
     * Methods for bound activities to access Bluetooth LE functions
     */

    // ----------------------------------------------------------------------------------------------------------------
    // Check if Bluetooth radio is enabled
    public boolean isBluetoothRadioEnabled() {
        try {
            if (btAdapter != null) {                                                                //Check that we have a BluetoothAdapter
                return btAdapter.isEnabled();                                                       //Return enabled state of Bluetooth radio
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
        return false;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Connect to a Bluetooth LE device with a specific address (address is usually obtained from a scan)
    public void connectBle(final String address) {
        try {
            if (btAdapter == null || address == null) {                                             //See if there is a radio and an address
                Log.w(TAG, "BluetoothAdapter not initialized or unspecified address");
                return;
            }
            BluetoothDevice btDevice = btAdapter.getRemoteDevice(address);                          //Use the address to get the remote device
            if (btDevice == null) {                                                                 //See if the device was found
                Log.w(TAG, "Unable to connect because device was not found");
                return;
            }
            if (btGatt != null) {                                                                   //See if an existing connection needs to be closed
                btGatt.close();                                                                     //Faster to create new connection than reconnect with existing BluetoothGatt
            }
            connectionAttemptCountdown = 3;                                                         //Try to connect three times for reliability

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {                                   //Build.VERSION_CODES.M = 23 for Android 6
                btGatt = btDevice.connectGatt(this, false, btGattCallback, BluetoothDevice.TRANSPORT_LE); //Directly connect to the device now, so set autoConnect to false, connect using BLE if device is dual-mode
            }
            else {
                btGatt = btDevice.connectGatt(this, false, btGattCallback);      //Directly connect to the device now, so set autoConnect to false
            }
            Log.d(TAG, "Attempting to create a new Bluetooth connection");
        }
        catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Disconnect an existing connection or cancel a connection that has been requested
    public void disconnectBle() {
        try {
            if (btAdapter != null && btGatt != null) {                                              //See if we have a connection before attempting to disconnect
                connectionAttemptCountdown = 0;                                                     //Stop counting connection attempts
                btGatt.disconnect();                                                                //Disconnect
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Read from the Transparent UART - get all the bytes that have been received since the last read
    /*
    public byte[] readFromTransparentUART() {
        try {
            final byte[] out = transparentReceiveOutput.toByteArray();                              //Get bytes from the ByteArrayOutputStream where they were put when onCharacteristicChanged was executed
            transparentReceiveOutput.reset();                                                       //Reset (empty) the ByteArrayOutputStream since we have all the bytes
            return out;                                                                             //Return the array of bytes
        } catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
        return new byte[0];
    }

     */

    // ----------------------------------------------------------------------------------------------------------------
    // Write to the Transparent UART
    /*
    public void writeToTransparentUART(byte[] bytesToWrite) {
        try {

            if (btAdapter != null && btGatt != null && transparentSendCharacteristic != null) {     //See if there is a radio, a connection, and a valid characteristic
                while (bytesToWrite.length > 0) {                                                   //Keep doing writes (adding to the write queue) until all bytes have been written
                    int length = Math.min(bytesToWrite.length, CharacteristicSize);                 //Get the number of bytes to write, limited to the max size of a characteristic
                    byte[] limitedBytesToWrite = Arrays.copyOf(bytesToWrite, length);               //Get a subset of the bytes that will fit into a characteristic
                    bytesToWrite = Arrays.copyOfRange(bytesToWrite, length, bytesToWrite.length);   //Get the remaining bytes ready for the next write
                    characteristicWriteQueue.add(limitedBytesToWrite);                              //Put the characteristic value into the write queue
                    if (characteristicWriteQueue.size() == 1) {                                     //If there is only 1 item in the queue, then write it.  If more than 1, we do it in the onCharacteristicWrite() callback above
                        transparentSendCharacteristic.setValue(limitedBytesToWrite);                //Put the bytes into the characteristic value
                        Log.i(TAG, "Characteristic write started");
                        if (!btGatt.writeCharacteristic(transparentSendCharacteristic)) {           //Request the BluetoothGatt to do the Write
                            Log.w(TAG, "Failed to write characteristic");                      //Warning that write request was not accepted by the BluetoothGatt
                        }
                    }
                }
            }
            else {
                Log.w(TAG, "Write attempted with Bluetooth uninitialized or not connected");
            }


        } catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }

     */




    // ----------------------------------------------------------------------------------------------------------------
    //
    public byte[] processBoreshot() {
        try {
            // new data is in latestBoreshot[]
            if (latestBoreshot[0] == 3) {
                Log.i(TAG, "PJH - processing Bore shot");
                Log.i(TAG, String.format("PJHK - Boreshot: %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X", latestBoreshot[0], latestBoreshot[1], latestBoreshot[2], latestBoreshot[3], latestBoreshot[4], latestBoreshot[5], latestBoreshot[6], latestBoreshot[7], latestBoreshot[8], latestBoreshot[9]));
                Log.i(TAG, String.format("PJHK - Boreshot: %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X", latestBoreshot[10], latestBoreshot[11], latestBoreshot[12], latestBoreshot[13], latestBoreshot[14], latestBoreshot[15], latestBoreshot[16], latestBoreshot[17], latestBoreshot[18], latestBoreshot[19]));


                int shotRecNum    = ((((int)latestBoreshot[1]) & 0xFF) << 8) + (((int)latestBoreshot[2]) & 0xFF);   // UB16, big endian

                // temperature is SB16, big endian - it is fixed point, hi byte is magnitude, lo byte is fraction
                int shotProbeTempRaw = ((int)latestBoreshot[3] << 8) + (((int)latestBoreshot[4]) & 0x00FF); // SB16, big endian

                // the accel values are 16 bit signed, so allow to sign extent properly
                int shotAccX      = ((int)latestBoreshot[5] << 8) + (((int)latestBoreshot[6]) & 0x00FF);
                int shotAccY      = ((int)latestBoreshot[7] << 8) + (((int)latestBoreshot[8]) & 0x00FF);
                int shotAccZ      = ((int)latestBoreshot[9] << 8) + (((int)latestBoreshot[10]) & 0x00FF);
                Log.i(TAG, String.format("PJHK - shotAcc: %08X %08X %08X", shotAccX, shotAccY, shotAccZ));
                Log.i(TAG, String.format("PJHK - shotAcc: %d %d %d", shotAccX, shotAccY, shotAccZ));

                // the mag values are also 16 bit signed, so allow to sign extent properly
                int shotMagX      = ( ( ((int)latestBoreshot[11] << 8) + (((int)latestBoreshot[12]) & 0x00FF) ) << 8) + (((int)latestBoreshot[13]) & 0x00FF);
                int shotMagY      = ( ( ((int)latestBoreshot[14] << 8) + (((int)latestBoreshot[15]) & 0x00FF) ) << 8) + (((int)latestBoreshot[16]) & 0x00FF);
                int shotMagZ      = ( ( ((int)latestBoreshot[17] << 8) + (((int)latestBoreshot[18]) & 0x00FF) ) << 8) + (((int)latestBoreshot[19]) & 0x00FF);


                headRB += 1;    // advance head to new position
                if (headRB >= ringBufferSize) {
                    headRB = 0;
                }
                if (countRB < ringBufferSize) {
                    countRB += 1;
                }

                recNum_RingBuffer[headRB] = shotRecNum;

                // normalise the raw accelerometer reading
                double ux = ((double)shotAccX)/32.0d/512.0d;        //  divide by 0x4000 (16,384)
                double uy = ((double)shotAccY)/32.0d/512.0d;        //  1 bit sign, 11 bit value 4 bit fraction
                double uz = ((double)shotAccZ)/32.0d/512.0d;        //  ( 1 bit of value equates to 2mg)
                // PJH - TODO - check this for borecam
                //double ux = ((double)shotAccX) * 2.0 / 0x7FFF;        // divide by 0x3FFF (about 16,000) - this is the ipod approximations
                //double uy = ((double)shotAccY) * 2.0 / 0x7FFF;
                //double uz = ((double)shotAccZ) * 2.0 / 0x7FFF;         // this is how ipod does it
                Log.i(TAG, String.format("PJHK - u: %f %f %f", ux, uy, uz));

                double cx, cy, cz;
                if (!isCalibrated || ignoreCalibration) {
                    // we have manually chaosen to not calibrate the raw data
                    cx = ux;
                    cy = uy;
                    cz = uz;
                }
                else {
                    double test1 = (acc_B[0][0] * ux);
                    Log.i(TAG, String.format("PJHK - test1: ux=%15.12f acc_b00=%15.12f result=%15.12f", ux, acc_B[0][0], test1));

                    double test2 = acc_A[0] + (acc_B[0][0] * ux);
                    Log.i(TAG, String.format("PJHK - test2: ux=%15.12f acc_a00=%15.12f acc_b00=%15.12f result=%15.12f", ux, acc_A[0], acc_B[0][0], test1));

                    cx = acc_A[0] + (acc_B[0][0] * ux) + (acc_B[0][1] * uy) + (acc_B[0][2] * uz) + (acc_C[0] * ux * ux * ux);
                    cy = acc_A[1] + (acc_B[1][0] * ux) + (acc_B[1][1] * uy) + (acc_B[1][2] * uz) + (acc_C[1] * uy * uy * uy);
                    cz = acc_A[2] + (acc_B[2][0] * ux) + (acc_B[2][1] * uy) + (acc_B[2][2] * uz) + (acc_C[2] * uz * uz * uz);
                    Log.i(TAG, String.format("PJHK - c: %15.12f %15.12f %15.12f", cx, cy, cz));
                }
                acc_X_RingBuffer[headRB] = cx;
                acc_Y_RingBuffer[headRB] = cy;
                acc_Z_RingBuffer[headRB] = cz;

                double accMag = Math.sqrt(cx*cx + cy*cy + cz*cz);

                // normalise the raw magnetometer reading
                double m_ux = ((double)shotMagX) * 0.001;    // convert to uT (from nT)
                double m_uy = ((double)shotMagY) * 0.001;
                double m_uz = ((double)shotMagZ) * 0.001;

                double m_cx, m_cy, m_cz;
                if (!isCalibrated || ignoreCalibration) {
                    // we have manually chaosen to not calibrate the raw data
                    m_cx = m_ux;
                    m_cy = m_uy;
                    m_cz = m_uz;
                }
                else {
                    m_cx = mag_A[0] + (mag_B[0][0] * m_ux) + (mag_B[0][1] * m_uy) + (mag_B[0][2] * m_uz) + (mag_C[0] * m_ux * m_ux * m_ux);
                    m_cy = mag_A[1] + (mag_B[1][0] * m_ux) + (mag_B[1][1] * m_uy) + (mag_B[1][2] * m_uz) + (mag_C[1] * m_uy * m_uy * m_uy);
                    m_cz = mag_A[2] + (mag_B[2][0] * m_ux) + (mag_B[2][1] * m_uy) + (mag_B[2][2] * m_uz) + (mag_C[2] * m_uz * m_uz * m_uz);
                }

                mag_X_RingBuffer[headRB] = m_cx;
                mag_Y_RingBuffer[headRB] = m_cy;
                mag_Z_RingBuffer[headRB] = m_cz;

                // keep all calculations in radians until complete, then return results in degrees (-180 <= roll <= 180)
                double cal_roll_radian = Math.atan2(cy, cz);
                if (cal_roll_radian > Math.PI)  { cal_roll_radian -= (2*Math.PI); }
                if (cal_roll_radian < -Math.PI) { cal_roll_radian += (2*Math.PI); }
                double cal_dip_radian  = Math.atan2(-cx, Math.sqrt((cy*cy)+(cz*cz)));

                double den = (m_cx * Math.cos(cal_dip_radian)) + (m_cy * Math.sin(cal_dip_radian) * Math.sin(cal_roll_radian)) + (m_cz * Math.sin(cal_dip_radian) * Math.cos(cal_roll_radian));
                double num = (m_cy * Math.cos(cal_roll_radian)) - (m_cz * Math.sin(cal_roll_radian));
                double cal_az_radian = Math.atan2(-num, den);
                // believe this az is +/- 180
                if (cal_az_radian > Math.PI)  { cal_az_radian -= (2*Math.PI); }
                if (cal_az_radian < -Math.PI) { cal_az_radian += (2*Math.PI); }
                // now convert it to 0..360
                if (cal_az_radian < 0) { cal_az_radian += (2*Math.PI); }

                // check for wrap

                double cal_roll_degree = cal_roll_radian*180/Math.PI;
                double cal_dip_degree  = cal_dip_radian*180/Math.PI;
                double cal_az_degree   = cal_az_radian*180/Math.PI;

                roll_RingBuffer[headRB] = cal_roll_degree;
                dip_RingBuffer[headRB]  = cal_dip_degree;
                az_RingBuffer[headRB]   = cal_az_degree;

                double probe_temperature_uncal = (double)shotProbeTempRaw/256.0;  // convert fixed point to floating point
                double probe_temperature = temp_param[0] + (temp_param[1] * probe_temperature_uncal);  // apply calibration
                temp_RingBuffer[headRB] = probe_temperature;

                // check if we are recording raw data
                if (sensorDataRecordingActive) {
                    String nowDate = new SimpleDateFormat("yyyy-MM-dd hh-mm", Locale.getDefault()).format(new Date());
                    // int type, int recnum, String time, double ax, double ay, double az, double at, double mx, double my, double mz, double mt
                    sensorData newShot = new sensorData(3,shotRecNum, nowDate, cal_roll_degree, cal_dip_degree, cal_az_degree, cx,cy,cz,probe_temperature,(accMag-1.0), m_cx,m_cy,m_cz,probe_temperature,probe_temperature, ux,uy,uz,m_ux,m_uy,m_uz);
                    sensorDataList.add(newShot);
                    sensorDataCount += 1;
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
        return new byte[0];
    }


    // ----------------------------------------------------------------------------------------------------------------
    //  TODO - there are things in processCoreshot that are common to processBoreshot, and should be factored out
    public byte[] processCoreshot() {
        try {
            // new data is in latestCoreshot[]
            if (latestCoreshot[0] == 1) {
                Log.i(TAG, "PJH - processing Core shot");
                Log.i(TAG, String.format("PJHK - Coreshot: %02X  %02X %02X  %02X %02X  %02X %02X  %02X %02X  %02X %02X  %02X", latestCoreshot[0], latestCoreshot[1], latestCoreshot[2], latestCoreshot[3], latestCoreshot[4], latestCoreshot[5], latestCoreshot[6], latestCoreshot[7], latestCoreshot[8], latestCoreshot[9], latestCoreshot[10], latestCoreshot[11]));


                int shotRecNum    = ((((int)latestCoreshot[1]) & 0xFF) << 8) + (((int)latestCoreshot[2]) & 0xFF);   // UB16, big endian

                // temperature is SB16, big endian - it is fixed point, hi byte is magnitude, lo byte is fraction
                int shotProbeTempRaw = ((int)latestCoreshot[3] << 8) + (((int)latestCoreshot[4]) & 0x00FF); // SB16, big endian

                // the accel values are 16 bit signed, so allow to sign extent properly
                int shotAccX      = ((int)latestCoreshot[5] << 8) + (((int)latestCoreshot[6]) & 0x00FF);    // SB16, big endian
                int shotAccY      = ((int)latestCoreshot[7] << 8) + (((int)latestCoreshot[8]) & 0x00FF);
                int shotAccZ      = ((int)latestCoreshot[9] << 8) + (((int)latestCoreshot[10]) & 0x00FF);
                Log.i(TAG, String.format("PJHK - shotAcc: 0x%08X 0x%08X 0x%08X", shotAccX, shotAccY, shotAccZ));
                Log.i(TAG, String.format("PJHK - shotAcc: %d %d %d", shotAccX, shotAccY, shotAccZ));

                int shotMagX      = 0;
                int shotMagY      = 0;
                int shotMagZ      = 0;

                headRB += 1;    // advance head to new position
                if (headRB >= ringBufferSize) {
                    headRB = 0;
                }
                if (countRB < ringBufferSize) {
                    countRB += 1;
                }

                recNum_RingBuffer[headRB] = shotRecNum;

                // normalise the raw accelerometer reading
                double ux = ((double)shotAccX)/32.0d/512.0d;        // divide by 0x4000 (16,384)
                double uy = ((double)shotAccY)/32.0d/512.0d;
                double uz = ((double)shotAccZ)/32.0d/512.0d;
                //double ux = ((double)shotAccX) * 2.0 / 0x7FFF;        // divide by 0x3FFF (about 16,000)
                //double uy = ((double)shotAccY) * 2.0 / 0x7FFF;        // - this is the ipod approximations
                //double uz = ((double)shotAccZ) * 2.0 / 0x7FFF;        //   why is it not *2/0x8000 - maybe they had sign issues?
                // but near enough, diff is maybe 0.00005
                Log.i(TAG, String.format("PJHK - u: %f %f %f", ux, uy, uz));

                double cx, cy, cz;
                if (!isCalibrated || ignoreCalibration) {
                    // we have manually chaosen to not calibrate the raw data
                    cx = ux;
                    cy = uy;
                    cz = uz;
                }
                else {
                    double test1 = (acc_B[0][0] * ux);
                    Log.i(TAG, String.format("PJHK - test1: ux=%15.12f acc_b00=%15.12f result=%15.12f", ux, acc_B[0][0], test1));

                    double test2 = acc_A[0] + (acc_B[0][0] * ux);
                    Log.i(TAG, String.format("PJHK - test2: ux=%15.12f acc_a00=%15.12f acc_b00=%15.12f result=%15.12f", ux, acc_A[0], acc_B[0][0], test1));

                    cx = acc_A[0] + (acc_B[0][0] * ux) + (acc_B[0][1] * uy) + (acc_B[0][2] * uz) + (acc_C[0] * ux * ux * ux);
                    cy = acc_A[1] + (acc_B[1][0] * ux) + (acc_B[1][1] * uy) + (acc_B[1][2] * uz) + (acc_C[1] * uy * uy * uy);
                    cz = acc_A[2] + (acc_B[2][0] * ux) + (acc_B[2][1] * uy) + (acc_B[2][2] * uz) + (acc_C[2] * uz * uz * uz);
                    Log.i(TAG, String.format("PJHK - c: %15.12f %15.12f %15.12f", cx, cy, cz));
                }
                acc_X_RingBuffer[headRB] = cx;
                acc_Y_RingBuffer[headRB] = cy;
                acc_Z_RingBuffer[headRB] = cz;

                double accMag = Math.sqrt(cx*cx + cy*cy + cz*cz);

                // normalise the raw magnetometer reading
                double m_ux = ((double)shotMagX) * 0.001;    // convert to uT (from nT)
                double m_uy = ((double)shotMagY) * 0.001;
                double m_uz = ((double)shotMagZ) * 0.001;

                double m_cx, m_cy, m_cz;
                if (!isCalibrated || ignoreCalibration) {
                    // we have manually chaosen to not calibrate the raw data
                    m_cx = m_ux;
                    m_cy = m_uy;
                    m_cz = m_uz;
                }
                else {
                    m_cx = mag_A[0] + (mag_B[0][0] * m_ux) + (mag_B[0][1] * m_uy) + (mag_B[0][2] * m_uz) + (mag_C[0] * m_ux * m_ux * m_ux);
                    m_cy = mag_A[1] + (mag_B[1][0] * m_ux) + (mag_B[1][1] * m_uy) + (mag_B[1][2] * m_uz) + (mag_C[1] * m_uy * m_uy * m_uy);
                    m_cz = mag_A[2] + (mag_B[2][0] * m_ux) + (mag_B[2][1] * m_uy) + (mag_B[2][2] * m_uz) + (mag_C[2] * m_uz * m_uz * m_uz);
                }

                mag_X_RingBuffer[headRB] = m_cx;
                mag_Y_RingBuffer[headRB] = m_cy;
                mag_Z_RingBuffer[headRB] = m_cz;

                // keep all calculations in radians until complete, then return results in degrees (-180 <= roll <= 180)
                double cal_roll_radian = Math.atan2(cy, cz);
                if (cal_roll_radian > Math.PI)  { cal_roll_radian -= (2*Math.PI); }
                if (cal_roll_radian < -Math.PI) { cal_roll_radian += (2*Math.PI); }
                double cal_dip_radian  = Math.atan2(-cx, Math.sqrt((cy*cy)+(cz*cz)));

                double den = (m_cx * Math.cos(cal_dip_radian)) + (m_cy * Math.sin(cal_dip_radian) * Math.sin(cal_roll_radian)) + (m_cz * Math.sin(cal_dip_radian) * Math.cos(cal_roll_radian));
                double num = (m_cy * Math.cos(cal_roll_radian)) - (m_cz * Math.sin(cal_roll_radian));
                double cal_az_radian = Math.atan2(-num, den);
                // believe this az is +/- 180
                if (cal_az_radian > Math.PI)  { cal_az_radian -= (2*Math.PI); }
                if (cal_az_radian < -Math.PI) { cal_az_radian += (2*Math.PI); }
                // now convert it to 0..360
                if (cal_az_radian < 0) { cal_az_radian += (2*Math.PI); }

                // check for wrap

                double cal_roll_degree = cal_roll_radian*180/Math.PI;
                double cal_dip_degree  = cal_dip_radian*180/Math.PI;
                double cal_az_degree   = cal_az_radian*180/Math.PI;

                roll_RingBuffer[headRB] = cal_roll_degree;
                dip_RingBuffer[headRB]  = cal_dip_degree;
                az_RingBuffer[headRB]   = cal_az_degree;

                double probe_tremperature_uncal = (double)shotProbeTempRaw/256.0d;  // convert fixed point to floating point
                double probe_tremperature = temp_param[0] + (temp_param[1] * probe_tremperature_uncal);  // apply calibration
                temp_RingBuffer[headRB] = probe_tremperature;

                // check if we are recording raw data
                if (sensorDataRecordingActive) {
                    String nowDate = new SimpleDateFormat("yyyy-MM-dd hh-mm", Locale.getDefault()).format(new Date());
                    // int type, int recnum, String time, double ax, double ay, double az, double at, double mx, double my, double mz, double mt
                    sensorData newShot = new sensorData(1,shotRecNum, nowDate, cal_roll_degree, cal_dip_degree, cal_az_degree, cx,cy,cz,probe_tremperature,(accMag-1.0), m_cx,m_cy,m_cz,probe_tremperature,probe_tremperature, ux,uy,uz,m_ux,m_uy,m_uz);
                    sensorDataList.add(newShot);
                    sensorDataCount += 1;
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
        return new byte[0];
    }


    private boolean ezyReadCharacteristic(BluetoothGattCharacteristic characteristic) {

        //Log.i(TAG, "PJH - entering my readCharacteristic");
        if (btAdapter == null || btGatt == null) {
            Log.w(TAG, "PJH - BluetoothAdapter not initialized (in ezyReadCharacteristic)");
            return(false);
        }
        /*check if the service is available on the device*/
        BluetoothGattService mCustomService = btGatt.getService(UUID_CAMERA_SERVICE);
        if(mCustomService == null){
            Log.w(TAG, "PJH - Custom BLE Service not found (in ezyReadCharacteristic)");
            return(false);
        }

        // PJH - this test works, so why isnt onCharacteristicRead firing???
        //byte[] tt = "A".getBytes();
        //tt[0] = 0;
        //MYprobeModeCharacteristic.setValue(tt);                //Put the bytes into the characteristic value
        //Log.i(TAG, "PJH - Characteristic write TEST started");
        //if (!btGatt.writeCharacteristic(MYprobeModeCharacteristic)) {           //Request the BluetoothGatt to do the Write
        //    Log.w(TAG, "PJH - Failed to write characteristic");                      //Warning that write request was not accepted by the BluetoothGatt
        //}


        /*get the read characteristic from the service*/
        //BluetoothGattCharacteristic mReadCharacteristic = mCustomService.getCharacteristic(UUID_PROBE_MODE_CHAR);
        ////if(btGatt.readCharacteristic(mReadCharacteristic) == false){
        if(btGatt.readCharacteristic(characteristic) == false){
            Log.w(TAG, "PJH - Failed to read characteristic (in ezyReadCharacteristic)");
        }
        //Log.i(TAG, "PJH - seemed to call readCharacteristic correctly");


        //if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) == 0) {
        //    return false;
        //}

        //if (VDBG) Log.d(TAG, "readCharacteristic() - uuid: " + characteristic.getUuid());
        //if (mService == null || mClientIf == 0) return false;

        //BluetoothGattService service = characteristic.getService();
        //if (service == null) return false;

        //BluetoothDevice device = service.getDevice();
        //if (device == null) return false;
        //BluetoothGattService gattCameraService = gatt.getService(UUID_CAMERA_SERVICE); //Get the Transparent UART service
        //if (gattCameraService != null) {                                                      //Check that the service was discovered

        //       btGatt.readCharacteristic(characteristic);

        //synchronized (mDeviceBusy) {
        //    if (mDeviceBusy) return false;
        //    mDeviceBusy = true;
        //}

        //try {
        //    service.readCharacteristic(characteristic);
        //} catch (RemoteException e) {
        //    Log.e(TAG, "", e);
        //    mDeviceBusy = false;
        //    return false;
        //}

        return true;
    }

    private boolean ezyWriteCharacteristic(BluetoothGattCharacteristic characteristic, int length, byte[] newValue) {

        //Log.i(TAG, "PJH - entering my writeCharacteristic");
        if (btAdapter == null || btGatt == null) {
            Log.w(TAG, "PJH - BluetoothAdapter not initialized (in ezyWriteCharacteristic)");
            return(false);
        }
        // check if the service is available on the device - JUST ASSUME IT IS (presumably the characteristic variable contains service info?
        //BluetoothGattService mCustomService = btGatt.getService(UUID_CAMERA_SERVICE);  // TODO - is there a matching close?
        //if(mCustomService == null){
        //    Log.w(TAG, "PJH - Custom BLE Service not found (in ezyWriteCharacteristic)");
        //    return(false);
        //}

        // PJH - this test works, so why isnt onCharacteristicRead firing???
        //byte[] tt = "A".getBytes();
        //tt[0] = 0;
        characteristic.setValue(newValue);                //Put the bytes into the characteristic value
        Log.i(TAG, "PJH - Characteristic write initiated");
        if (!btGatt.writeCharacteristic(characteristic)) {           //Request the BluetoothGatt to do the Write
            Log.w(TAG, "PJH - Failed to write characteristic");                      //Warning that write request was not accepted by the BluetoothGatt
        }

        return true;
    }


    // ##############################################################################################
    //
    //  Ezy methods
    //
    //


    // calculate the 8 bit CRC of the bytes starting at offset 'start'
    // and ending at offset 'end'
    // (used for the calibration header block)
    //
    // remember: the byte array actually contains 8bit SIGNED values,
    //           and java does not have an unsigned type, so beware of sign extending
    //
    private int cal_crc8_gen(byte[] buffer, int start, int end) {
        // CRC parameters for main calibration data header
        int CRC8_POLY_CCITT = 0x8D;
        int CRC8_INIT_VALUE = 0x00;
        int CRC8_FINAL_XOR  = 0x00;
        //print("Generating 8bit CRC of", len(buffer), "bytes of data")
        int crc = CRC8_INIT_VALUE;

        for (int i=start; i<=end; i++) {
            int buff = buffer[i];   // get the next byte, nad let it sign extend
            buff &= 0xFF;    // remove any sign extension
            crc = crc ^ buff;
            for (int bit=0; bit<8; bit++) {
                if ((crc & 0x80) != 0) {
                    crc = (crc << 1) ^ CRC8_POLY_CCITT;
                }
                else {
                    crc = (crc << 1);
                }
                crc &= 0xFF;   // ensure CRC is limited to 8 bit
            }
        }
        crc = crc ^ CRC8_FINAL_XOR;
        crc &= 0xFF;   // just to be sure (shouldn't be necessary)
        //print("Resulting CRC = 0x{:02X}".format(crc))
        Log.i(TAG, String.format("PJH - cal_crc8_gen: %02X", crc));

        return (crc);
    }



    // calculate the 16 bit CRC of the bytes starting at offset 'start'
    // and ending at offset 'end'
    // (used for the main calibration block)
    //
    // remember: the byte array actually contains 8bit SIGNED values,
    //           and java does not have an unsigned type, so beware of sign extending
    //
    private int cal_crc16_gen(byte[] buffer, int start, int end) {
        // CRC parameters for calibration body
        int CRC16_POLY_CCITT = 0x1021;
        int CRC16_INIT_VALUE = 0x0000;
        int CRC16_FINAL_XOR  = 0x0000;

        int crc = CRC16_INIT_VALUE;

        for (int i=start; i<=end; i++) {
            int buff = buffer[i];   // get the next byte, nad let it sign extend
            buff &= 0xFF;    // remove any sign extension
            crc = crc ^ (buff << 8);
            for (int bit=0; bit<8; bit++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ CRC16_POLY_CCITT;
                }
                else {
                    crc = (crc << 1);
                }
                crc &= 0xFFFF;    // ensure CRC is limited to 16 bit
            }
        }
        crc = crc ^ CRC16_FINAL_XOR;
        crc &= 0xFFFF;   // just to be sure (shouldn't be necessary)
        //print("Resulting CRC = 0x{:04X}".format(crc))
        Log.i(TAG, String.format("PJH - cal_crc16_gen: %04X", crc));

        return (crc);
    }



    // retrieves the double from the binCalData byte array, at position 'offset'
    // refer: https://www.baeldung.com/java-byte-array-to-number
    private double getDouble(int offset) {
        // concatenate the bytes to an 8 byte long (assume the endianness is correct)
        double dValue = 0;
        try {
            long lValue = 0;
            for (int i = 0; i < 8; i++) {
                //for (int i = 7; i >= 0; i--) {
                lValue = (lValue << 8) + (((long) binCalData[offset + i]) & 0xFF);
            }
            dValue = Double.longBitsToDouble(lValue);
            //Log.i(TAG, String.format("PJH - getDouble: lValue=0x%08X dValue=%f", lValue, dValue));

        } catch (Exception e) {
            Log.e(TAG, "PJH - getDouble - Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }

        return(dValue);
    }


    // ----------------------------------------------------------------------------------------------------------------
    // parseBinaryCalibration walks the structure of the binary calibration data and extracts the
    // required parameters
    // The raw binary calibration data is in binCalData[]
    // modifies the global calibration variables and sets ??? if successful
    //
    // Remember, the code looks like sh*t because Java doesn't have an unsigned data type.
    // Bytes are actually 8 bit signed, so to compare with hex values, you need to cast to a
    // 32 bit signed integer and then mask off the unwanted high bytes - YUK YUK YUK
    //
    // Retrieving the full calibration data and parsing it is quite lengthy (maybe 5 seconds ???)
    // for a real app, need to cache the binary data, and the parsed coefficients in a file
    //
    public boolean parseBinaryCalibration() {
        boolean result = false;       // assume the worst
        int coeffs_found = 0;   // should be NUM_CAL_PARAMS_EXPECTED_DURING_PARSING when finished (38)

        Log.w(TAG, "PJH - parseBinaryCalibration");
        try {
            isCalibrated = false;     // assume the worst
            int p = 0;        // base 0, pointer into cal binary

            // dump the header for debugging
            //Log.i(TAG, String.format("PJH - calPacket: %02X %02X %02X %02X %02X %02X %02X %02X", binCalData[0], binCalData[1], binCalData[2], binCalData[3], binCalData[4], binCalData[5], binCalData[6], binCalData[7]));
            //Log.i(TAG, String.format("PJH -            %02X %02X %02X %02X %02X %02X %02X %02X", binCalData[8], binCalData[9], binCalData[10], binCalData[11], binCalData[12], binCalData[13], binCalData[14], binCalData[15]));

            //CALLBACK
            // first check if the header (of the header) is valid (first byte is 0x01 and high byte of length is 0x00)
            if (((((int) binCalData[0]) & 0xFF) == 0x01) && ((((int) binCalData[1]) & 0xFF) == 0x00)) {
                // use low byte of header length to determine the offset of the 8bit CRC
                // (header length includes CRC, but not blk id or length)
                // plus 3 to give total length of header, less 1 to convert nth byte to base 0 offset
                int crc_offset = (((int) binCalData[2]) & 0xFF) + 3 - 1;   // this is also the number of bytes preceeding the crc byte

                // the 8bit header CRC spans all bytes in this block, excluding the CRC itself
                int crc8 = cal_crc8_gen(binCalData, 0, crc_offset-1);     // (data[], start, end)

                if (crc8 == (((int) binCalData[crc_offset]) & 0xFF)) {   // beware sign extension
                    // ok, the crc of the header is correct, so we can proceed

                    // get the 'modified' date out of the header block (it is in a fixed location)
                    int m_date_len = ((int)binCalData[5]) & 0xFF;   // should be 8 for current calibrations, or maybe 4 for archaic probes
                    if ((m_date_len == 4) || (m_date_len==8)) {
                        long unix_tick = 0;
                        for (int i = (6 + m_date_len - 1); i >= 6; i--) {  // little endian
                            unix_tick = (unix_tick << 8) + (((int)binCalData[i]) & 0xFF);
                        }

                        Date date = new Date(unix_tick * 1000L);
                        SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
                        jdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
                        modifiedDateString = jdf.format(date);

                        coeffs_found += 1;
                        Log.i(TAG, String.format("PJH - parse - modified date = %s", modifiedDateString));
                    }
                    else
                    {
                        modifiedDateString = "not initialised";
                    }

                    // the calibrated date string will be retrieved later, from the main data block


                    // now we want to check the crc of the actual calibration data
                    //int start = (((int) binCalData[2]) & 0xFF) + 3 + 3;   // first byte after the block size
                    //int total_len = ((((int) binCalData[3]) & 0xFF) << 8) + (((int) binCalData[4]) & 0xFF);
                    //int end_plus_one = total_len - 2;
                    //Log.i(TAG, String.format("PJH - start: %06X end+1: %06X", start, end_plus_one));
                    //System.arraycopy(binCalData, start, buffer, 0, end_plus_one - start);    // (src,dst,len)
                    //
                    //int crc16 = cal_crc16_gen(buffer, end_plus_one - start);

                    // the 16 bit CRC of the main data block starts AFTER the blk_id and blk_len
                    // and includes the last byte before the actual CRC16
                    // (so the main blk_id and blk_len are NOT covered by the CRC16 - this is a BUG,
                    // but can't change it as we need to maintain compatibility with the ipod,
                    // so will need to manually check those separately)
                    int start = (((int) binCalData[2]) & 0xFF) + 3 + 3;   // offset of header byte of main data block
                    int total_len = ((((int) binCalData[3]) & 0xFF) << 8) + (((int) binCalData[4]) & 0xFF);  // big endian
                    binCalData_size = total_len;   // save the number of valid byte entries in binCalData
                    coeffs_found += 1;
                    int end = total_len - 2 - 1;   //  total len - 2 byte crc - 1 to convert len to offset
                    Log.i(TAG, String.format("PJH - start: %06X end: %06X", start, end));
                    int crc16 = cal_crc16_gen(binCalData, start, end);   // (data[], start, end)

                    // get the crc16 from the binary cal data (from the last two bytes)
                    int binCrc16 = ((((int) binCalData[total_len - 2]) & 0xFF) << 8) + (((int) binCalData[total_len - 1]) & 0xFF);  // big endian

                    if (crc16 == binCrc16) {
                        // both CRCs are good, so extract calibration coefficients
                        Log.i(TAG, "PJH - parse - both calibration CRCs are good");
                        p = crc_offset + 1;   // first byte of the main body

                        // the following log entry shows the allocated size of binCalData which is not useful
                        //Log.i(TAG, String.format("PJH - parse - binCalData length: 0x%04X", binCalData.length));

                        if (((((int) binCalData[p]) & 0xFF) == 0x10)) {   // this blk_id is not covered by CRC so check it here
                            // the subsequent 2 byte blk_len is not used in this parsing, so ignore it for now
                            // TODO - we really should check it though, as don't know how ipod would react if it was corrupted
                            p += 3;  // point to cal record type
                            int cal_record_type = (((int) binCalData[p]) & 0xFF);
                            // cal_record_type is always 0x01 for the calibration parameters we are using
                            // TODO - should check it is 0x01, just to be sure
                            Log.i(TAG, String.format("PJH - parse @ 0x%04X - cal record type: 0x%02X", p, cal_record_type));
                            p += 1;  // and advance to the first block of calibration parameters

                            // Now we can finally start reading in the calibration parameters that we need
                            // Each set of related parameters and in a separate block
                            int hdr_id, blk_len, pTmp, flags;
                            while (p < total_len) {
                                // first two bytes of the block contain the hdr_id in the highest nibble, and blk_len in remaining three nibbles
                                hdr_id = (((int) binCalData[p]) & 0xF0) >> 4;
                                blk_len = (((((int) binCalData[p]) & 0x0F) << 8) + (((int) binCalData[p + 1]) & 0xFF)) & 0x0FFF;
                                p += 2;
                                Log.i(TAG, String.format("PJH - parse @ 0x%04X - hdr_id: 0x%02X, length: 0x%04X", p, hdr_id, blk_len));

                                // p is pointing to the first byte of content in this calibration parameter block
                                if (hdr_id == 0x00) {    // CRC block - ignore =============================================
                                    // the CRC16 block is ALWAYS the last block in the calibration file,
                                    // so that the CRC16 is the last two bytes of the calibration file

                                    // we have already checked this CRC, so ignore it
                                    p += blk_len;
                                }
                                else if (hdr_id == 0x01) {    // timestamp block =============================================
                                    // extract the Calibrated Date

                                    int c_date_len = blk_len;  // should be 8 for current calibrations, or maybe 4 for archaic probes
                                    if ((c_date_len == 4) || (c_date_len == 8)) {
                                        long unix_tick = 0;
                                        for (int i = (p + c_date_len - 1); i >= (p); i--) {  // timestamp is little endian
                                            unix_tick = (unix_tick << 8) + (((int)binCalData[i]) & 0xFF);
                                        }

                                        Date date = new Date(unix_tick * 1000L);
                                        SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
                                        jdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
                                        calibratedDateString = jdf.format(date);

                                        coeffs_found += 1;
                                        Log.i(TAG, String.format("PJH - parse - calibrated date = %s", calibratedDateString));

                                    }
                                    else
                                    {
                                        calibratedDateString = "not initialised";
                                    }

                                    p += blk_len;
                                }
                                else if (hdr_id == 0x02) {    // accel block =============================================
                                    pTmp = p;  // we don't want to mess up 'p'
                                    // flags tells up what sub field exist in this block
                                    flags = (((((int) binCalData[pTmp + 1]) & 0xFF) << 8) + (((int) binCalData[pTmp]) & 0xFF));  // little endian
                                    pTmp += 2;  // skip over flags

                                    if ((flags & 0x0001) != 0) {
                                        // temperature data - skip it   TODO
                                        // PJH - this relates to probes that are calibrated a two different temperatures
                                        // none of the probes have calibration data that is temperature dependent
                                        pTmp += 8;
                                    }
                                    if ((flags & 0x0004) != 0) {
                                        // bit 2 is set, so first order terms are present - 3x3
                                        // each of the 9 parameters are double precision floats
                                        for (int r = 0; r < 3; r++) {
                                            for (int c = 0; c < 3; c++) {
                                                acc_B[r][c] = getDouble(pTmp);
                                                Log.i(TAG, String.format("PJH - parse - acc_B[%d][%d] = %15.12f", r, c, acc_B[r][c]));
                                                coeffs_found += 1;
                                                pTmp += 8;
                                            }
                                        }
                                    }
                                    if ((flags & 0x0008) != 0) {
                                        // bit 3 is set, so zero order terms are present

                                        // HACK - the next byte tells us the depth of this 3 wide matrix
                                        // Don't understand why this is here, but it is
                                        // ASSUME it will be 2 and skip over it - YUK
                                        pTmp+=1; // just assume this count is for the 3x2 matrix  TODO
                                        // zero order terms - 3x2 - don't know why there are extra co-efficients
                                        for (int r = 0; r < 3; r++) {
                                            for (int c = 0; c < 2; c++) {
                                                if (c==0) {  // the first column is what we want (other column is zeros)
                                                    acc_A[r] = getDouble(pTmp);
                                                    Log.i(TAG, String.format("PJH - parse - acc_A[%d] = %15.12f", r, acc_A[r]));
                                                    coeffs_found += 1;
                                                }
                                                pTmp += 8;
                                            }
                                        }
                                    }
                                    //
                                    // manual zero offset
                                    //
                                    // next byte tells us how many zero offset terms there are
                                    pTmp+=1; // just assume the count is 1 and skip over it - YUK   TODO
                                    // PJH - don't know exactly how this field is used in the ipod, so need to reverse engineer that
                                    // to ensure the android behaves the same (is this value +ve or -ve? is it added or subtracted?)
                                    offset_of_accManualZeroOffset = pTmp;   // remember the location of this parameter, so we can change it later
                                    accManualZeroOffset = getDouble(pTmp);
                                    coeffs_found += 2;
                                    Log.i(TAG, String.format("PJH - parse - acc Manual Zero offset = %15.12f", accManualZeroOffset));
                                    Log.i(TAG, String.format("PJH - parse - acc Manual Zero offset is located at offset = %6d", offset_of_accManualZeroOffset));
                                    pTmp += 8;

                                    if ((flags & 0x0100) != 0) {
                                        // bit 8 is set, so cubic order terms are present - 1x3
                                        for (int c = 0; c < 3; c++) {
                                            acc_C[c] = getDouble(pTmp);
                                            Log.i(TAG, String.format("PJH - parse - acc_C[%d] = %15.12f", c, acc_C[c]));
                                            coeffs_found += 1;
                                            pTmp += 8;
                                        }
                                    }
                                    p += blk_len;   // advance pointer to next block
                                }
                                else if (hdr_id == 0x03) {    // mag block =============================================
                                    if (blk_len <= 10) {
                                        // ignore this malformed magnetometer block - effects Ezycore (maybe Corecam)

                                        // assume we are talking to a corecam and clear all mag variables
                                        // and update parameter count, for compatibility
                                        mag_A[0] = 0;
                                        mag_A[1] = 0;
                                        mag_A[2] = 0;
                                        mag_B[0][0] = 0;
                                        mag_B[0][1] = 0;
                                        mag_B[0][2] = 0;
                                        mag_B[1][0] = 0;
                                        mag_B[1][1] = 0;
                                        mag_B[1][2] = 0;
                                        mag_B[2][0] = 0;
                                        mag_B[2][1] = 0;
                                        mag_B[2][2] = 0;
                                        mag_C[0] = 0;
                                        mag_C[1] = 0;
                                        mag_C[2] = 0;
                                        magManualZeroOffset = 0;
                                        coeffs_found += 16;
                                    }
                                    else {
                                        pTmp = p;  // we don't want to mess up 'p'
                                        // flags tells up what sub field exist in this block
                                        flags = (((((int) binCalData[pTmp + 1]) & 0xFF) << 8) + (((int) binCalData[pTmp]) & 0xFF));
                                        pTmp += 2;  // skip over flags

                                        if ((flags & 0x0001) != 0) {
                                            // temperature data - skip it
                                            pTmp += 8;
                                        }
                                        if ((flags & 0x0004) != 0) {
                                            // first order terms - 3x3
                                            for (int r = 0; r < 3; r++) {
                                                for (int c = 0; c < 3; c++) {
                                                    mag_B[r][c] = getDouble(pTmp);
                                                    Log.i(TAG, String.format("PJH - parse - mag_B[%d][%d] = %15.12f", r, c, mag_B[r][c]));
                                                    coeffs_found += 1;
                                                    pTmp += 8;
                                                }
                                            }
                                        }
                                        if ((flags & 0x0008) != 0) {
                                            pTmp+=1; // just assume this count is for the 3.2 matrix - TODO
                                            // zero order terms - 3x2 - don't know why there are extra co-efficients
                                            for (int r = 0; r < 3; r++) {
                                                for (int c = 0; c < 2; c++) {
                                                    if (c==0) {  // the first column is what we want (other column is zeros)
                                                        mag_A[r] = getDouble(pTmp);
                                                        Log.i(TAG, String.format("PJH - parse - mag_A[%d] = %15.12f", r, mag_A[r]));
                                                        coeffs_found += 1;
                                                    }
                                                    pTmp += 8;
                                                }
                                            }
                                        }
                                        // manual zero offset - I don't believe this is used anywhere
                                        pTmp+=1; // just assume the count is 1  TODO
                                        magManualZeroOffset = getDouble(pTmp);
                                        coeffs_found += 1;
                                        Log.i(TAG, String.format("PJH - parse - mag Manual Zero offset = %15.12f", magManualZeroOffset));
                                        pTmp += 8;

                                        if ((flags & 0x0100) != 0) {
                                            // cubic order terms - 1x3
                                            for (int c = 0; c < 3; c++) {
                                                mag_C[c] = getDouble(pTmp);
                                                Log.i(TAG, String.format("PJH - parse - mag_C[%d] = %15.12f", c, mag_C[c]));
                                                coeffs_found += 1;
                                                pTmp += 8;
                                            }
                                        }

                                    }

                                    p += blk_len;   // advance pointer to next block
                                }
                                else if (hdr_id == 0x04) {    // temperature block =============================================
                                    // these parameters are used to calibrate the main temperature sensor
                                    pTmp = p;  // we don't want to mess up 'p'
                                    // flags tells up what sub field exist in this block
                                    flags = ((int) binCalData[pTmp]) & 0xFF;
                                    int count = ((int) binCalData[pTmp + 1]) & 0xFF;   // YUK - just assuming this is 2
                                    pTmp += 2;  // skip over flags and parameter count

                                    if ((flags & 0x01) != 0) {
                                        // temperature data - just assume 2 parameters??? - YUK - TODO
                                        temp_param[0] = getDouble(pTmp);  //offset
                                        pTmp += 8;
                                        temp_param[1] = getDouble(pTmp);  // scale
                                        pTmp += 8;
                                        coeffs_found += 2;
                                        Log.i(TAG, String.format("PJH - parse - temp[0] = %15.12f (offset)", temp_param[0]));
                                        Log.i(TAG, String.format("PJH - parse - temp[1] = %15.12f (scale)",  temp_param[1]));
                                    }
                                    p += blk_len;   // advance pointer to next block
                                }
                                else {             // =============================================
                                    p += blk_len;  // just skip any unknown blocks (shouldn't be any)
                                }
                            }

                            // do a final sanity check to ensure we got all the parameters we expected
                            if (coeffs_found == NUM_CAL_PARAMS_EXPECTED_DURING_PARSING) {
                                isCalibrated = true;
                                Log.i(TAG, String.format("PJH - parse - found %d calibration parameters", coeffs_found));
                                Log.w(TAG, "PJH - successfully parsed binary calibration data  =======================");
                            }
                        }


                    } else {
                        // oops, calibration data is not valid
                        Log.w(TAG, "PJH - CRC of the body of the binary cal data is incorrect - aborting");
                    }
                } else {
                    // oops, calibration data is not valid
                    Log.w(TAG, "PJH - CRC of the header of the binary cal data is incorrect - aborting");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "PJH - parse - Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }

        if (isCalibrated != true) {
            Log.w(TAG, "PJH - parsing binary calibration data FAILED =======================");
            Log.i(TAG, String.format("PJH - parse - only found %d calibration parameters (expected %d)", coeffs_found, NUM_CAL_PARAMS_EXPECTED_DURING_PARSING));
        }

        return result;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Start interrogateConfig - Initiate a read of ProbeMode, which cascades into the other reads:
    // - FirmwareVersionMajor, FirmwareVersionMinor, ShotInterval, SurveyMaxShots, RollingShotInterval, etc
    public void requestEzyConfig() {
        try {
            Log.i(TAG, "PJH - initiating request Ezy config");
            // PJH - TODO - blindly assume we are connected and initiate the get config state machine
            // TODO - needs some form of timeout

            if (isCalibrated()) {
                // don't go through the whole process, as we are just reconnecting

                // will need to do some things here

                // but for now, just tell main activity we are ready
                stateInterrogateConfig = statesInterrogateConfig.CONFIG_READY;
                sendBroadcast(new Intent(ACTION_BLE_CONFIG_READY));  // PJH - finished (for now) - in progress
            }
            else {
                // TODO - request read of probe mode characteristic
                stateInterrogateConfig = statesInterrogateConfig.WAITING_PROBE_MODE;  //should this be before or after?
                ezyReadCharacteristic(probeModeCharacteristic);
            }
            //readCharacteristic(probeModeCharacteristic);
            // PJH - we get here correctly
            //if (btGatt != null) {
            //    btGatt.readCharacteristic(probeModeCharacteristic);
            //    // PJH - got to here correctly, now check onread
            //    Log.i(TAG, "PJH - call readChar for probe mode");
            //}

            /*
            if (btAdapter == null || address == null) {                                             //See if there is a radio and an address
                Log.w(TAG, "BluetoothAdapter not initialized or unspecified address");
                return;
            }
            BluetoothDevice btDevice = btAdapter.getRemoteDevice(address);                          //Use the address to get the remote device
            if (btDevice == null) {                                                                 //See if the device was found
                Log.w(TAG, "Unable to connect because device was not found");
                return;
            }
            if (btGatt != null) {                                                                   //See if an existing connection needs to be closed
                btGatt.close();                                                                     //Faster to create new connection than reconnect with existing BluetoothGatt
            }
            connectionAttemptCountdown = 3;                                                         //Try to connect three times for reliability

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {                                   //Build.VERSION_CODES.M = 23 for Android 6
                btGatt = btDevice.connectGatt(this, false, btGattCallback, BluetoothDevice.TRANSPORT_LE); //Directly connect to the device now, so set autoConnect to false, connect using BLE if device is dual-mode
            }
            else {
                btGatt = btDevice.connectGatt(this, false, btGattCallback);      //Directly connect to the device now, so set autoConnect to false
            }
            Log.d(TAG, "Attempting to create a new Bluetooth connection");

             */
        }
        catch (Exception e) {
            Log.e(TAG, "PJH - Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }




    // ----------------------------------------------------------------------------------------------------------------
    // Start interogateConfig - Initiate a read of ProbeMode, which cascades into the other reads:
    // - FirmwareVersionMajor, FirmwareVersionMinor, ShotInterval, SurveyMaxShots, RollingShotInterval, etc
    public void setNotifications(boolean enableNotifications) {
        try {
            Log.i(TAG, "PJH - set notifications");
            // PJH - TODO - blindly assume we are connected and initiate the get config state machine
            // TODO - needs some form of timeout


            BluetoothGattService gattCameraService = btGatt.getService(UUID_CAMERA_SERVICE);
// PJH - TODO should be checking for null here

            boreShotCharacteristic = gattCameraService.getCharacteristic(UUID_BORE_SHOT_CHAR); //Get the characteristic for receiving from the Transparent UART
            if (boreShotCharacteristic != null) {                             //See if the characteristic was found
                Log.i(TAG, "PJH - Found Bore shot characteristic");
                int characteristicProperties = boreShotCharacteristic.getProperties(); //Get the properties of the characteristic
                if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_NOTIFY)) > 0) { //See if the characteristic has the Notify property
                    BluetoothGattDescriptor descriptor = boreShotCharacteristic.getDescriptor(UUID_CCCD); //Get the descriptor that enables notification on the server
                    if (descriptor != null) {                                           //See if we got the descriptor
                        btGatt.setCharacteristicNotification(boreShotCharacteristic, true); //If so then enable notification in the BluetoothGatt
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE); //Set the value of the descriptor to enable notification

                        // PJH - we have a potential problem here - is it currently safe to write to BLE??? (cross our fingers) - FAILING on the next Read 8-(
                        btGatt.writeDescriptor(descriptor);                         //Write the descriptor
                        Log.i(TAG, "PJH - Have (hopefully) enabled notifications for Bore shot characteristic");
                        //descriptorWriteQueue.add(descriptor);                           //Put the descriptor into the write queue
                        //if (descriptorWriteQueue.size() == 1) {                         //If there is only 1 item in the queue, then write it.  If more than 1, we handle asynchronously in the onDescriptorWrite callback below
                        //    btGatt.writeDescriptor(descriptor);                         //Write the descriptor
                        //}
                    }
                    else {
                        //discoveryFailed = true;
                        Log.w(TAG, "PJH - No CCCD descriptor for Bore Shot characteristic");
                    }
                }
                else {
                    //discoveryFailed = true;
                    Log.w(TAG, "PJH - Bore Shot characteristic does not have notify property");
                }
            }
            else {
                //discoveryFailed = true;
                Log.w(TAG, "PJH - Did not find Bore Shot characteristic");
            }

// BEWARE - the following code to enable coreshot notifications FAILS as we have no BLE queuing yet
// TODO - need to wait for last BLE thing to finish... (don't know how to do this, at this time of night!!!)


            coreShotCharacteristic = gattCameraService.getCharacteristic(UUID_CORE_SHOT_CHAR); //Get the characteristic for receiving from the Transparent UART
            if (coreShotCharacteristic != null) {                             //See if the characteristic was found
                Log.i(TAG, "PJH - Found Core shot characteristic");
                int characteristicProperties = coreShotCharacteristic.getProperties(); //Get the properties of the characteristic
                if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_NOTIFY)) > 0) { //See if the characteristic has the Notify property
                    BluetoothGattDescriptor descriptor = coreShotCharacteristic.getDescriptor(UUID_CCCD); //Get the descriptor that enables notification on the server
                    if (descriptor != null) {                                           //See if we got the descriptor
                        btGatt.setCharacteristicNotification(coreShotCharacteristic, true); //If so then enable notification in the BluetoothGatt
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE); //Set the value of the descriptor to enable notification

                        // PJH - we have a potential problem here - is it currently safe to write to BLE??? (cross our fingers) - FAILING on the next Read 8-(
                        btGatt.writeDescriptor(descriptor);                         //Write the descriptor
                        Log.i(TAG, "PJH - Have (hopefully) enabled notifications for Core shot characteristic");
                        //descriptorWriteQueue.add(descriptor);                           //Put the descriptor into the write queue
                        //if (descriptorWriteQueue.size() == 1) {                         //If there is only 1 item in the queue, then write it.  If more than 1, we handle asynchronously in the onDescriptorWrite callback below
                        //    btGatt.writeDescriptor(descriptor);                         //Write the descriptor
                        //}
                    }
                    else {
                        //discoveryFailed = true;
                        Log.w(TAG, "PJH - No CCCD descriptor for Core Shot characteristic");
                    }
                }
                else {
                    //discoveryFailed = true;
                    Log.w(TAG, "PJH - Core Shot characteristic does not have notify property");
                }
            }
            else {
                //discoveryFailed = true;
                Log.w(TAG, "PJH - Did not find Core Shot characteristic");
            }








            //final BluetoothGattCharacteristic transparentReceiveCharacteristic = gattService.getCharacteristic(UUID_TRANSPARENT_RECEIVE_CHAR); //Get the characteristic for receiving from the Transparent UART
            //if (transparentReceiveCharacteristic != null) {                             //See if the characteristic was found
            //    Log.i(TAG, "Found Transparent Receive characteristic");
            //    final int characteristicProperties = transparentReceiveCharacteristic.getProperties(); //Get the properties of the characteristic
            //    if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_NOTIFY)) > 0) { //See if the characteristic has the Notify property
/*
            BluetoothGattDescriptor descriptor = boreShotCharacteristic.getDescriptor(UUID_CCCD); //Get the descriptor that enables notification on the server
            if (descriptor != null) {                                           //See if we got the descriptor
                btGatt.setCharacteristicNotification(boreShotCharacteristic, enableNotifications); //If so then enable notification in the BluetoothGatt
                // PJH TODO will need to change next line to disable
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE); //Set the value of the descriptor to enable notification
                btGatt.writeDescriptor(descriptor);                         //Write the descriptor

            }
            else {
                //discoveryFailed = true;
                Log.w(TAG, "PJH - No CCCD descriptor for Boreshot characteristic");
            }

 */
            //    }
            //    else {
            //        discoveryFailed = true;
            //        Log.w(TAG, "Transparent Receive characteristic does not have notify property");
            //    }


        }
        catch (Exception e) {
            Log.e(TAG, "PJH - Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }






    // ----------------------------------------------------------------------------------------------------------------
    // Start interrogateConfig - Initiate a read of ProbeMode, which cascades into the other reads:
    // - FirmwareVersionMajor, FirmwareVersionMinor, ShotInterval, SurveyMaxShots, RollingShotInterval, etc
    public void setProbeMode(int mode) {
        try {
            Log.i(TAG, String.format("PJH - setting new probe mode (%d)", mode));
            // PJH - TODO - blindly assume we are connected and initiate the get config state machine
            // TODO - needs some form of timeout

            // reset the ring buffer
            headRB = 0;
            countRB = 0;   // only needed during the initial fill

            // turn on notification for shots - no
            // (don't need to do this explicitly, setting Rolling Shot mode is sufficient)
            byte[] tt = "A".getBytes();   // ???
            tt[0] = (byte)(mode & 0xFF);
            probeModeCharacteristic.setValue(tt);                //Put the bytes into the characteristic value
            //Log.i(TAG, "PJH - Characteristic write TEST started");
            if (!btGatt.writeCharacteristic(probeModeCharacteristic)) {           //Request the BluetoothGatt to do the Write
                Log.w(TAG, "PJH - Failed to set new Probe mode");                      //Warning that write request was not accepted by the BluetoothGatt
            }
            probeMode = mode;   // assuming success
            Log.i(TAG, String.format("PJH - Set Probe Mode to %d", probeMode));
        }
        catch (Exception e) {
            Log.e(TAG, "PJH - Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }


    // ----------------------------------------------------------------------------------------------------------------
    // Start interrogateConfig - Initiate a read of ProbeMode, which cascades into the other reads:
    // - FirmwareVersionMajor, FirmwareVersionMinor, ShotInterval, SurveyMaxShots, RollingShotInterval, etc
    public void setProbeIdle() {
        try {
            Log.i(TAG, "PJH - setting probe back to Idle mode");
            // PJH - TODO - blindly assume we are connected and initiate the get config state machine
            // TODO - needs some form of timeout
            if (probeMode != PROBE_MODE_IDLE) {
                // reset the ring buffer
                headRB = 0;
                countRB = 0;   // only needed during the initial fill

                // turn on notification for shots
                // (don't need to do this explicitly, setting Rolling Shot mode is sufficient)
                byte[] tt = "A".getBytes();
                tt[0] = 0;
                probeModeCharacteristic.setValue(tt);                //Put the bytes into the characteristic value
                //Log.i(TAG, "PJH - Characteristic write TEST started");
                if (!btGatt.writeCharacteristic(probeModeCharacteristic)) {           //Request the BluetoothGatt to do the Write
                    Log.w(TAG, "PJH - Failed to set probe to Idle");                      //Warning that write request was not accepted by the BluetoothGatt
                }
                probeMode = PROBE_MODE_IDLE;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "PJH - Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }



    // ----------------------------------------------------------------------------------------------------------------
    // Read from the Transparent UART - get all the bytes that have been received since the last read
    public boolean isCalibrated() {

        return isCalibrated;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // The isCalibrated flag is used during a reconnect, to prevent calibration data being reread.
    // The main activity calls this routing when it is initiating a scan
    public void invalidateCalibration() {

        isCalibrated = false;
    }


    // ----------------------------------------------------------------------------------------------------------------
    // Read from the Transparent UART - get all the bytes that have been received since the last read
    public String getFirmwareVersionString() {
        // how do we know if firmware numbers are valid?
        if ((majorVersionNumber != -1) && (minorVersionNumber != -1)) {
            firmwareVersionString = String.format("v%1d.%1d", majorVersionNumber, minorVersionNumber);
            // presume a major number of "10' will ignore the single digit width request
            // Reduce minor pattern to %1d, to try and remove the space after the period
        }
        else {
            firmwareVersionString = "vX.XX";
        }
        return firmwareVersionString;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Read from the Transparent UART - get all the bytes that have been received since the last read
    public String getCalibratedDateString() {

        return calibratedDateString;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Read from the Transparent UART - get all the bytes that have been received since the last read
    public int getRollingShotInterval() {

        return rollingShotInterval;
    }


    // ----------------------------------------------------------------------------------------------------------------
    // Read from the Transparent UART - get all the bytes that have been received since the last read
    public int getShotInterval() {

        return shotInterval;
    }


    // ----------------------------------------------------------------------------------------------------------------
    // Read from the Transparent UART - get all the bytes that have been received since the last read
    public int getDebug1() {

        return debugValue;
    }


    // ----------------------------------------------------------------------------------------------------------------
    // Read from the Transparent UART - get all the bytes that have been received since the last read
    public int getDebug2() {

        return debug2Value;
    }


    // ----------------------------------------------------------------------------------------------------------------
    // Read from the Transparent UART - get all the bytes that have been received since the last read
    public boolean getIgnoreCalibrationState() {

        return ignoreCalibration;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Read from the Transparent UART - get all the bytes that have been received since the last read
    public void disableCalibration() {

        ignoreCalibration = true;
        headRB = 0;    // and reset the ring buffer
        countRB = 0;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Read from the Transparent UART - get all the bytes that have been received since the last read
    public void enableCalibration() {

        ignoreCalibration = false;
        headRB = 0;    // and reset the ring buffer
        countRB = 0;
    }



    // ----------------------------------------------------------------------------------------------------------------
    // Read from the Transparent UART - get all the bytes that have been received since the last read
    //public byte[] getBinaryCalData() {
//
    //    return binCalData;
    //}



    // ----------------------------------------------------------------------------------------------------------------
    //

    // could potentially get called, with the ring buffer empty (very unlikely)
    public double[] getLatestBoreshot(int count) {
        double result[] = new double[11];
        int i, c;
        double tally, tmp;

        if (count < 1) { count = 1; }
        if (count > 120) { count = 120; }
        if (count > countRB) { count = countRB; }   // in case the ring buffer has not yet filled

        if (count == 1) {
            result[0] = recNum_RingBuffer[headRB];

            result[1] = acc_X_RingBuffer[headRB];
            result[2] = acc_Y_RingBuffer[headRB];
            result[3] = acc_Z_RingBuffer[headRB];

            result[4] = mag_X_RingBuffer[headRB];
            result[5] = mag_Y_RingBuffer[headRB];
            result[6] = mag_Z_RingBuffer[headRB];

            result[7] = roll_RingBuffer[headRB];
            result[8] = dip_RingBuffer[headRB];
            result[9] = az_RingBuffer[headRB];
            result[10] = temp_RingBuffer[headRB];
        }
        else {
            result[0] = count;  // HACK, as this field no longer makes sense


            i = headRB;
            c = count;
            tally = 0;
            while (c > 0) {
                tally += acc_X_RingBuffer[i];
                c -= 1;
                i -= 1;
                if (i < 0) { i = ringBufferSize - 1; }
            }
            result[1] = tally / count;  // Acc X


            i = headRB;
            c = count;
            tally = 0;
            while (c > 0) {
                tally += acc_Y_RingBuffer[i];
                c -= 1;
                i -= 1;
                if (i < 0) { i = ringBufferSize - 1; }
            }
            result[2] = tally / count;  // Acc Y


            i = headRB;
            c = count;
            tally = 0;
            while (c > 0) {
                tally += acc_Z_RingBuffer[i];
                c -= 1;
                i -= 1;
                if (i < 0) { i = ringBufferSize - 1; }
            }
            result[3] = tally / count;  // Acc Z



            i = headRB;
            c = count;
            tally = 0;
            while (c > 0) {
                tally += mag_X_RingBuffer[i];
                c -= 1;
                i -= 1;
                if (i < 0) { i = ringBufferSize - 1; }
            }
            result[4] = tally / count;  // Acc X


            i = headRB;
            c = count;
            tally = 0;
            while (c > 0) {
                tally += mag_Y_RingBuffer[i];
                c -= 1;
                i -= 1;
                if (i < 0) { i = ringBufferSize - 1; }
            }
            result[5] = tally / count;  // Acc Y


            i = headRB;
            c = count;
            tally = 0;
            while (c > 0) {
                tally += mag_Z_RingBuffer[i];
                c -= 1;
                i -= 1;
                if (i < 0) { i = ringBufferSize - 1; }
            }
            result[6] = tally / count;  // Acc Z



            i = headRB;
            c = count;
            tally = 0;
            while (c > 0) {
                tally += roll_RingBuffer[i];
                c -= 1;
                i -= 1;
                if (i < 0) { i = ringBufferSize - 1; }
            }
            tmp = tally / count;
            if (tmp < -180) { tmp += 360; }
            if (tmp > 180)  { tmp -= 360; }
            result[7] = tmp;  // Roll


            i = headRB;
            c = count;
            tally = 0;
            while (c > 0) {
                tally += dip_RingBuffer[i];
                c -= 1;
                i -= 1;
                if (i < 0) { i = ringBufferSize - 1; }
            }
            result[8] = tally / count;  // Dip


            i = headRB;
            c = count;
            tally = 0;
            while (c > 0) {
                tally += az_RingBuffer[i];
                c -= 1;
                i -= 1;
                if (i < 0) { i = ringBufferSize - 1; }
            }
            tmp = tally / count;
            if (tmp < 0) { tmp += 360; }
            if (tmp > 360)  { tmp -= 360; }
            result[9] = tmp;  // Az


            i = headRB;
            c = count;
            tally = 0;
            while (c > 0) {
                tally += temp_RingBuffer[i];
                c -= 1;
                i -= 1;
                if (i < 0) { i = ringBufferSize - 1; }
            }
            tmp = tally / count;
            if (tmp < 0) { tmp += 360; }
            if (tmp > 360)  { tmp -= 360; }
            result[10] = tmp;  // Az


        }


        return result;
    }



    /******************************************************************************************************************
     * Methods for recording raw sensor data
     */


    public String sensorDataReportGetReportHeader() {
        //return("Num,Time,Roll,Pitch,Azimuth,Acc-X,Acc-Y,Acc-Z,Acc-T,Acc-Error,Mag-X,Mag-Y,Mag-Z,Mag-T,Probe-T,AccUnc-X,AccUnc-Y,AccUnc-Z,MagUnc-X,MagUnc-Y,MagUnc-Z");
        return(sensorDataList.get(0).getReportHeader());  // all elements return the same header
    }

    public String sensorDataReportGetReportLine(int index) {
        return(sensorDataList.get(index).getReportLine());
    }

    public int getSensorDataCount() {
        return(sensorDataCount);
    }

    public boolean isRecordingSensorDataEnabled() {
        return(sensorDataRecordingEnable);
    }

    public void enableRecordingSensorData () {
        sensorDataRecordingEnable = true;
    }

    public void disableRecordingSensorData () {
        sensorDataRecordingEnable = false;
    }

    public void initRecordingSensorData () {
        sensorDataCount = 0;
        sensorDataRecordingActive = false;
        sensorDataList.clear();
    }

    public void startRecordingSensorData () {
        sensorDataRecordingActive = true;
    }

    public void stopRecordingSensorData () {  // or should this be called pause?
        sensorDataRecordingActive = false;
    }

}