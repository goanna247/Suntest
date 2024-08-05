////////////////////////////////////////////////////////////////////////////////
/**
 * \file MainActivity.java
 * \brief Main activity of the app, manages what to do with the probe and collecting calibrating
 * \author Anna Pedersen
 * \date Created: 07/06/2024
 *
 * TODO - activity really needs to be cleaned up a bit
 */
package com.work.libtest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.work.libtest.databinding.ActivityMainBinding;

import java.lang.reflect.Array;
import java.util.ArrayList;

import static com.work.libtest.Operation.OPERATION_WRITE;
import static com.work.libtest.Operation.OPERATION_READ;
import static com.work.libtest.Operation.OPERATION_NOTIFY;

import androidx.appcompat.app.AppCompatActivity;

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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import com.work.libtest.About.AboutActivity;
import com.work.libtest.Preferences.PreferencesActivity;
//import com.work.suntech.SelectProbe.DeviceScanActivity;
import com.work.libtest.SurveyOptions.SurveyOptionsActivity;
import com.work.libtest.Preferences.Preferences;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;


public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("libtest");
    }

    private ActivityMainBinding binding;


    //information passed through from BluetoothTools.DeviceScanActivity after connecting to a device
    public static final String EXTRA_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRA_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRA_CONNECTION_STATUS = "CONNECTION_STATUS";
    private static final String NAME_STATE_KEY = "DEVICE NAME STATE KEY";
    private static final String ADDRESS_STATE_KEY = "ADDRESS STATE KEY";

    public static final int OPERATION_WRITE = 1;
    public static final int OPERATION_READ = 2;
    public static final int OPERATION_NOTIFY = 3;
    int _probeMode = 0;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private Menu menu;

    public int calibrationIndexNum = 00;

    private String mDeviceName;
    private String mDeviceAddress;
    private String mDeviceConnectionStatus;

    private String mMode;


    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private BluetoothLeService mBluetoothLeService;
    private BluetoothGatt mBluetoothGatt;

    private static final String TAG = "Main Activity";
    private Handler handler;

    public static ArrayList<Survey> surveys = new ArrayList<Survey>();
    public static int surveySize = 0;
    public static Preferences preferences = new Preferences();
    public static String connectedDeviceName = "No Probe Selected";

    public static ArrayList<String> caliData = new ArrayList<>();

    TextView blackProbeTxt;
    TextView whiteProbeTxt;

    TextView blackProbeStatusTxt;
    TextView whiteProbeStatusTxt;

    TextView singleProbeTxt;

    ImageView blackProbeStatusImg;

    LinearLayout WhiteProbeContainer; //the second container for a probe, used if the app is in dual mode

    private BluetoothGattCharacteristic CORE_SHOT_CHARACTERISTIC;
    private BluetoothGattCharacteristic BORE_SHOT_CHARACTERISTIC;

    private long startTime = 0; //for timer
    public int seconds;
    private int starttime = 0;

    public static int surveyNum = 0;

    Button operatorNameBtn;
    Button holeIDBtn;

    TextView HoleIDDisplayTxt;
    TextView OperatorNameDisplayTxt;

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            seconds = (int) (millis / 1000);

            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!mBluetoothAdapter.isEnabled()) {
                updateConnectionState("Disconnected");
            }

            if (mConnected == false) {
                updateConnectionState("Disconnected");
            }
            timerHandler.postDelayed(this, 5000);
        }
    };

    private final Queue<Operation> operations = new LinkedList<>();
    private Operation currentOp;
    private static boolean operationOngoing = false;

    /**
     *
     * @param operation being requested
     */
    public synchronized void request(Operation operation) {
//        Log.d(TAG, "requesting operation: " + operation.toString());
        try {
            operations.add(operation);
            if (currentOp == null) {
                currentOp = operations.poll();
                performOperation();
            } else {
//                Log.e(TAG, "current operation is not null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error thrown while requesting an operation");
        }
    }

    /**
     * Excecutes whenever an operation is finished
     */
    public synchronized void operationCompleted() {
//        Log.d(TAG, "Operation completed, moving onto the next");
        currentOp = null;
        if (operations.peek() != null) {
            currentOp = operations.poll();
            performOperation();
        } else {
//            Log.d(TAG, "Queue empty");
            if (!Globals.caliDataCollected) {

                try {
                    if (!caliData.get(caliData.size()-1).equals("-1:-1:-1:-1:-1:-1:-1:-1:-1:-1:-1:-1:-1:-1:-1:-1:")) {
                        calibrationIndexNum++;
                        indexNext();
                    } else {
                        //print original
                        Log.e(TAG, "All Calibration Data: ");
                        for (int i = 0; i < caliData.size(); i++) {
                            Log.e(TAG, caliData.get(i));
                        }

                        //delete duplicates
                        for (int i = 0; i < caliData.size(); i++) {
                            if (i % 2 == 0) {
                                caliData.remove(i);
                            }
                        }

                        Set<String> s = new LinkedHashSet<String>(caliData);
                        Log.e(TAG, s.toString());
                        List<String> list = new ArrayList<>(s);
                        list.remove(list.size()-1);

                        //print
                        Log.e(TAG, "All Calibration Data: ");
                        for (int i = 0; i < list.size(); i++) {
                            Log.e(TAG, list.get(i));
                        }
                        Globals.caliDataCollected = true;
                        updateConnectionState("Connected");
                        blackProbeStatusImg.setImageResource(R.drawable.ready);
                        blackProbeStatusTxt.setText("Connected");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception thrown: " + e);
                }
            }
        }
    }

    /**
     * Perform requested operation
     */
    public void performOperation() {
//        Log.d(TAG, "Performing operation: " + currentOp.getCharacteristic().getUuid().toString());
        if (currentOp != null) {
//            Log.e(TAG, "Current performing option on service: " + currentOp.getService().getUuid().toString() + " with characterisitc: " + currentOp.getCharacteristic().getUuid().toString());
            if (currentOp.getService() != null && currentOp.getCharacteristic() != null && currentOp.getAction() != 0) {
                switch (currentOp.getAction()) {
                    case OPERATION_WRITE:
//                        Log.e(TAG, "writing");
//                        Log.d(TAG, "attempting to write to a characterisitic: " + currentOp.getCharacteristic().getUuid().toString());
                        mBluetoothLeService.writeData(currentOp.getCharacteristic());
                        break;
                    case OPERATION_READ:
//                        Log.d(TAG, "Reading characterisitc with service: " + currentOp.getService().getUuid().toString() + " and characteristic: " + currentOp.getCharacteristic().getUuid().toString());
                        mBluetoothLeService.readCharacteristic(currentOp.getCharacteristic());
                    case OPERATION_NOTIFY:
                        try {
                            mBluetoothLeService.setCharacteristicNotification(currentOp.getCharacteristic(), true);
                        } catch (Exception e) {
//                            Log.e(TAG, "Cant set characterisitc to notiy");
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * reset all operations
     */
    public void resetQueue() {
        operations.clear();
        currentOp = null;
    }

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
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService(); //TODO had to chuck all the bluetooth files inside the main folder to get this working, can probs figure out a better solution
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Enable to initialize bluetooth");
                finish();
            }
            mBluetoothLeService.connect(mDeviceAddress);
        }

        /**
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
         * @param context The Context in which the receiver is running.
         * @param intent The Intent being received.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState("Connected");
                invalidateOptionsMenu();
//                getActionBar().setBackgroundDrawable(new ColorDrawable(Color.GREEN));
                mDeviceConnectionStatus = "Connected";

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState("Disconnected");
                invalidateOptionsMenu();
                clearUI();
                mDeviceConnectionStatus = "Disconnected";
//                getActionBar().setBackgroundDrawable(new ColorDrawable(Color.RED));
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                //we dont really want to show all the information here.
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//                Log.d(TAG, "BLE Data: " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));

                if (intent.getStringExtra(BluetoothLeService.EXTRA_DATA) != null) {
//                    Log.d(TAG, "OTHER DATA !? uwu: " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                } else if (intent.getStringExtra(BluetoothLeService.CALIBRATION_INDEX) != null) {
//                    Log.d(TAG, "Calibration index: " + intent.getStringExtra(BluetoothLeService.CALIBRATION_INDEX));
                } else if (intent.getStringExtra(BluetoothLeService.CALIBRATION_DATA) != null) {
//                    Log.d(TAG, "Calibration data: " + intent.getStringExtra(BluetoothLeService.CALIBRATION_DATA));
//                    if (!caliData.get(caliData.size()-1).equals(intent.getStringExtra(BluetoothLeService.CALIBRATION_DATA))) {
                    caliData.add(intent.getStringExtra(BluetoothLeService.CALIBRATION_DATA));
                } else if (intent.getStringExtra(BluetoothLeService.DEVICE_ADDRESS) != null) {
//                    Log.d(TAG, "Device Address: " + intent.getStringExtra(BluetoothLeService.DEVICE_ADDRESS));
                }
                operationCompleted();

            } else if (intent != null) {
                operationCompleted();
            }
        }
    };

    /**
     *
     * @param menu The options menu in which you place your items.
     *
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main_activity, menu);
        this.menu = menu;
        return true;
    }

    private void clearUI() {
//        mGattCharacteristics.setAdapter((SimpleExpandableListAdapter) null);
//        mDataField.setText("No Data");
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer;

        /**
         * @param socket - Bluetooth socket
         */
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occured when creating input stream", e);
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
                            MessageConstants.MESSAGE_READ, numBytes,-1, mmBuffer
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
                bundle.putString("toast", "couldnt send data to the other device");
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
     * initially show the probe as disconnected to ensure it connects properly
     */
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
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //TODO if in core mode / vs in bore mode
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        mDeviceConnectionStatus = intent.getStringExtra(EXTRA_CONNECTION_STATUS);

        Log.e(TAG, "passed in name: " + intent.getStringExtra(EXTRA_DEVICE_NAME) + "Passed in address: " + intent.getStringExtra(EXTRA_DEVICE_ADDRESS) + "Passed in connection: " + intent.getStringExtra(EXTRA_CONNECTION_STATUS));

        HoleIDDisplayTxt = (TextView)findViewById(R.id.HoleIDDisplayTxt);
        OperatorNameDisplayTxt = (TextView)findViewById(R.id.OperatorNameDisplayTxt);

        blackProbeTxt = findViewById(R.id.BlackProbeTxt);
        whiteProbeTxt = findViewById(R.id.WhiteProbeTxt);

        blackProbeStatusTxt = findViewById(R.id.BlackProbeStatusTxt);
        whiteProbeStatusTxt = findViewById(R.id.WhiteProbeStatusTxt);

        blackProbeStatusImg = findViewById(R.id.BlackProbeStatusImg);

        WhiteProbeContainer = (LinearLayout) findViewById(R.id.WhiteProbeContainer);


        blackProbeTxt.setText("Probe: " + mDeviceName);

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

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

//        getActionBar().setBackgroundDrawable(new ColorDrawable(Color.RED));

//        if (mDeviceConnectionStatus != null) {
//            updateConnectionState(mDeviceConnectionStatus);
////            mGattUpdateReceiver();
//            if (mDeviceConnectionStatus.equals("Connected")) {
//                mConnected = true;
//            }
//        } else {
//            Log.e(TAG, "ERROR DEVICE CONNECTION STATUS IS NULL");
//        }

        if (MainActivity.preferences.getMode().equals("Core Orientation (Dual)") || MainActivity.preferences.getMode().equals("Dual")) {
            WhiteProbeContainer.setVisibility(View.VISIBLE);
        } else if (MainActivity.preferences.getMode().equals("Bore Orientation (Single)") || MainActivity.preferences.getMode().equals("Single")) {
            WhiteProbeContainer.setVisibility(View.GONE);
        } else {
            Log.e(TAG, "Probe mode is invalid?!");
        }

        if (!Globals.caliDataCollected && Globals.enableCalibration) {
            updateConnectionState("Disconnected");
            Log.e(TAG, "Calibration data not aquired: hence disconnected");
        } else {
            if (mDeviceConnectionStatus != null) {
                updateConnectionState(mDeviceConnectionStatus);
                if (mDeviceConnectionStatus.equals("Connected")) {
                    mConnected = true;
                }
            } else {
                Log.e(TAG, "ERROR DEVICE CONNECTION STATUS IS NULL");
            }
//            updateConnectionState(mDeviceConnectionStatus);
            Log.e(TAG, "updating connection state to: " + mDeviceConnectionStatus);
        }


        byte calibrationData[] = {1, 16, 4, -81, 2, 111, 89, 32, -100, -84, 1, 63, -16, 53, 96, -100, 84, 90, -27, -65, 62, 5, -72, 27, 29, 27, -124, -65, -110, 20, 103, -84, 67, -29, -38, -65, -115, 90, -116, 62, -95, 103, 109, 63, 98, 11, -21, 119, 119, -19, -20, 63, 31, -18, 81, 16, -65, 81, -86, 63, -16, 59, -5, -91, -10, 23, -61, 2, 63, -120, 114, -125, 0, 24, 122, 62, 0, 0, 0, 0, 0, 0, 0, 0, 63, -118, 103, -57, -56, -9, -85, -61, 0, 0, 0, 0, 0, 0, 0, 0, -65, 105, -121, 26, -31, 74, -1, -12, 0, 0, 0, 0, 0, 0, 0,0, 0, 1, 64, 0, 59, 93, -12, -67, 70, 61, -65, 109, -65, 44, 117, 26, 26, 123, -65, 18, -99, -80, 100, -102, 26, -8, -65, 93, 54, -43, 78, -103, -61, 89, 48, -100, -84, 1, 63, -16, -126, -20, 71, -16, 68, 6, 63, 126, 97, -84, -10, -17, 98, -69, -65, -123, 112, 66, -59, -117, 40, -46, -65,96, 15, 10, 98, -2, -98, -54, -65, -107, 60, -98, -124, 20, -96, 98, 63, 81, -66, -66, -80, -77, 111, -22, 63, -15, 25, -25, -3, -55, -44, -84, 2, 64, 20, -115, -20, -73, -81, 12, -40, 0, 0, 0, 0, 0, 0, 0, 0, 64, 14, -92, -119, -95, -43, 100, 96, 0, 0, 0, 0, 0, 0, 0, 0, 63, -38, -46, -59, -39, 19, -67, 84, 0, 0, 0, 0, 0, 0, 0, 0, 1, 64, 0, 59, 93, -12, -67, 70, 61, 62, -105, 95, -84, 112, 51, -106, -59, 62, -104, -83, -71, -79, 77, 57, 1, 62, -120, 5, -45, 11, 86, 103};

        String calibrationDataInput = "";
        for (int i = 0; i < 290; i++) {
            calibrationDataInput = calibrationDataInput + "." + calibrationData[i];
        }
        Log.e(TAG, calibrateAllData("1,100000,3,4,5,6,7,384," + calibrationDataInput));
    }

    public native String calibrateAllData(String input_data);

    /**
     * Go through all gatt services avaliable
     * @param gattServices
     *
     * TODO - currently highly inefficient, can improve speed of app by fixing this
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
//            if (gattService.getUuid().toString().equals(SampleGattAttributes.PRIMARY_SERVICE_CHARACTERISTICS)) {
            HashMap<String, String> currentServiceData = new HashMap<>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<>();

//                Log.e(TAG, "Gatt service is: " + gattService.getUuid().toString());
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

//                        Log.d(TAG, "Gatt characteristic is: " + gattCharacteristic.getUuid().toString());
                if (gattCharacteristic.getUuid() != null) {
                    if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.CALIBRATION_INDEX)) {
//                                Log.e(TAG, "Reading calibration index");
                        Operation getCalibrationIndex = new Operation(gattService, gattCharacteristic, OPERATION_READ);
                        try {
                            request(getCalibrationIndex);
                        } catch (Exception e) {
                            Log.e(TAG, "Exception thrown for requesting operation");
                        }
                    } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.CALIBRATION_DATA)) {
//                                Log.e(TAG, "Reading calibration data");
                        Operation getCalibrationData = new Operation(gattService, gattCharacteristic, OPERATION_READ);
                        try {
                            request(getCalibrationData);
                        } catch (Exception e) {
                            Log.e(TAG, "Exception thrown for requesting operation");
                        }
                    } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.DEVICE_ADDRESS)) {
//                                Log.e(TAG, "Reading device address");
                        Operation getAddressData = new Operation(gattService, gattCharacteristic, OPERATION_READ);
                        try {
                            request(getAddressData);
                        } catch (Exception e) {
                            Log.e(TAG, "Exception thrown for requesting operation");
                        }
                    }
                } else {
//                            Log.e(TAG, "gatt characteristic uuid is null");
                }

            }
//            } else {
//                Log.e(TAG, "Something else: ");
//            }
        }
    }

    /**
     *
     * @param savedInstanceState the data most recently supplied in {@link #onSaveInstanceState}.
     *
     */
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        //save the address and name of the device last connected to so when we come back to it we can reconnect
        //reassign stuff?
        blackProbeTxt.setText("Probe: " + mDeviceName);
        updateConnectionState(mDeviceConnectionStatus);
        Log.e(TAG, "Saved instance:  " + savedInstanceState);
    }

    /**
     *
     * @param outState Bundle in which to place your saved state.
     *
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(NAME_STATE_KEY, mDeviceName);
        outState.putString(ADDRESS_STATE_KEY, mDeviceAddress);
        outState.putString(EXTRA_CONNECTION_STATUS, mDeviceConnectionStatus);

        Log.e(TAG, "Saved Device info: " + outState);

        super.onSaveInstanceState(outState);
    }

    /**
     * when the activity is resumed it needs to register the receiver and reset probe details
     */
    @Override
    protected void onResume() {
        super.onResume();
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connection request result=" + result);
        }

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        mDeviceConnectionStatus = intent.getStringExtra(EXTRA_CONNECTION_STATUS);
        blackProbeTxt.setText("Probe: " + mDeviceName);
//        if (mDeviceConnectionStatus != null) {
//            updateConnectionState(mDeviceConnectionStatus);
//        }
        if (!Globals.caliDataCollected && Globals.enableCalibration) { //if we interrupted the calibration process before it was finished
            resetQueue();
            calibrationIndexNum = 0;
            updateConnectionState("Disconnected");
            Log.e(TAG, "updating connection state to disconnected ");

        } else {
            if (mDeviceConnectionStatus != null) {
                updateConnectionState(mDeviceConnectionStatus);
                //            mGattUpdateReceiver();
                if (mDeviceConnectionStatus.equals("Connected")) {
                    mConnected = true;
                }
            } else {
                Log.e(TAG, "ERROR DEVICE CONNECTION STATUS IS NULL");
            }
//            updateConnectionState(mDeviceConnectionStatus);
        }
    }

    /**
     * When activity is paused the app needs to unregister the gattUpdateReceiver
     */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    /**
     * Update probe connection state based on resourceID
     * @param resourceId, should either be "Connected" or "Disconnected"
     *
     * TODO - should be made an enum so that spelling wont be an issue
     */
    private void updateConnectionState(final String resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                Log.e(TAG, "UPDATING CONNECTION STATE");
                blackProbeStatusTxt.setText(resourceId);
                if (resourceId.equals("Connected")) {
                    blackProbeStatusImg.setImageResource(R.drawable.ready);
                    mDeviceConnectionStatus = "Connected";
                    if (!Globals.caliDataCollected && Globals.enableCalibration) {
                        indexNext();
                    }
                } else if (resourceId.equals("Disconnected")) {
                    blackProbeStatusImg.setImageResource(R.drawable.unconnected);
                    registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
                    if (mBluetoothLeService != null) {
                        final boolean result = mBluetoothLeService.connect(mDeviceAddress);
                        Log.d(TAG, "Connection request result=" + result);
                    }
                    mDeviceConnectionStatus = "Disconnected";
                } else {
                    blackProbeStatusImg.setImageResource(R.drawable.disconnecting);
                    mDeviceConnectionStatus = "Disconnected";
                }
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            //display the data, probably not needed in this section
        }
    }

    /**
     * Go to survey options class on button click
     * @param v
     */
    public void operatorIDBtnClick(View v) {
        Intent intent = new Intent(this, SurveyOptionsActivity.class);
        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_NAME, mDeviceName);
        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_CONNECTION, mDeviceConnectionStatus);
        startActivity(intent);
    }

    /**
     * Go to survey options class on button click
     * @param v
     */
    public void holeIDBtnClick(View v) {
        Intent intent = new Intent(this, SurveyOptionsActivity.class);
        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_NAME, mDeviceName);
        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        startActivity(intent);
    }

    /**
     *
     * @param item The menu item that was selected.
     *
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        if (item.getItemId() == R.id.select_probe) {
            Intent intent = new Intent(this, DeviceScanActivity.class);
            if (mDeviceName != null && mDeviceAddress != null) {
                intent.putExtra(DeviceScanActivity.EXTRA_DEVICE_NAME, mDeviceName);
                intent.putExtra(DeviceScanActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
                intent.putExtra(DeviceScanActivity.EXTRA_DEVICE_NAME, mDeviceName);
            }
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.reset_survey) {
            //TODO reset survey functionality
            /**
             * pop-up page asking which survey to cancel
             * Both probes, black only, white only
             * or cancel out of the function
             *
             * has a confirmation after pressing a probe/s
             *
             * potench make this a fragment
             */
//                int surveyArraySize = MainActivity.surveys.size();
//                MainActivity.surveys.remove(surveyArraySize - 1);

            if (preferences.getMode() == "Core Orientation (Dual)") {
                mMode = "Dual";
            } else if (preferences.getMode() == "Bore Orientation (Single)") {
                mMode = "Single";
            }
            //pass in information
            Intent resetIntent = new Intent(this, ResetSurveyActivity.class);
            resetIntent.putExtra(ResetSurveyActivity.EXTRA_PREFERENCES_MODE, mMode);
            resetIntent.putExtra(ResetSurveyActivity.EXTRA_DEVICE_NAME, mDeviceName);
            resetIntent.putExtra(ResetSurveyActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
            startActivity(resetIntent);

            //TODO needs to refresh the holeID and operatorID display values here, they still show last known value
            return true;
        } else if (item.getItemId() == R.id.reset_probe) {
            //Todo reset probe functionaility
            /**
             * pop-up asking which probe to reset
             * either both, black or white or cancel out of the function
             *
             * has a confirmation after pressing a probe/s
             */
            return true;
        } else if (item.getItemId() == R.id.preferences) {
            Intent prefIntent = new Intent(this, PreferencesActivity.class);
            prefIntent.putExtra(PreferencesActivity.EXTRA_DEVICE_NAME, mDeviceName);
            prefIntent.putExtra(PreferencesActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
            prefIntent.putExtra(PreferencesActivity.EXTRA_DEVICE_CONNECTION_STATUS, mDeviceConnectionStatus);
            startActivity(prefIntent);
            return true;
        } else if (item.getItemId() == R.id.about) {
            Intent aboutIntent = new Intent(this, AboutActivity.class);
            aboutIntent.putExtra(AboutActivity.EXTRA_DEVICE_NAME, mDeviceName);
            aboutIntent.putExtra(AboutActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
            aboutIntent.putExtra(AboutActivity.EXTRA_DEVICE_CONNECTION_STATUS, mDeviceConnectionStatus);
            startActivity(aboutIntent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
//        switch (item.getItemId()) {
//            case R.id.select_probe:
//                Intent intent = new Intent(this, DeviceScanActivity.class);
//                if (mDeviceName != null && mDeviceAddress != null) {
//                    intent.putExtra(DeviceScanActivity.EXTRA_DEVICE_NAME, mDeviceName);
//                    intent.putExtra(DeviceScanActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
//                    intent.putExtra(DeviceScanActivity.EXTRA_DEVICE_NAME, mDeviceName);
//                }
//                startActivity(intent);
//                return true;
//            case R.id.reset_survey:
//                //TODO reset survey functionality
//                /**
//                 * pop-up page asking which survey to cancel
//                 * Both probes, black only, white only
//                 * or cancel out of the function
//                 *
//                 * has a confirmation after pressing a probe/s
//                 *
//                 * potench make this a fragment
//                 */
////                int surveyArraySize = MainActivity.surveys.size();
////                MainActivity.surveys.remove(surveyArraySize - 1);
//
//                if (preferences.getMode() == "Core Orientation (Dual)") {
//                    mMode = "Dual";
//                } else if (preferences.getMode() == "Bore Orientation (Single)") {
//                    mMode = "Single";
//                }
//                //pass in information
//                Intent resetIntent = new Intent(this, ResetSurveyActivity.class);
//                resetIntent.putExtra(ResetSurveyActivity.EXTRA_PREFERENCES_MODE, mMode);
//                resetIntent.putExtra(ResetSurveyActivity.EXTRA_DEVICE_NAME, mDeviceName);
//                resetIntent.putExtra(ResetSurveyActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
//                startActivity(resetIntent);
//
//                //TODO needs to refresh the holeID and operatorID display values here, they still show last known value
//                return true;
//            case R.id.reset_probe:
//                //Todo reset probe functionaility
//                /**
//                 * pop-up asking which probe to reset
//                 * either both, black or white or cancel out of the function
//                 *
//                 * has a confirmation after pressing a probe/s
//                 */
//                return true;
//            case R.id.preferences:
//                Intent prefIntent = new Intent(this, PreferencesActivity.class);
//                prefIntent.putExtra(PreferencesActivity.EXTRA_DEVICE_NAME, mDeviceName);
//                prefIntent.putExtra(PreferencesActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
//                prefIntent.putExtra(PreferencesActivity.EXTRA_DEVICE_CONNECTION_STATUS, mDeviceConnectionStatus);
//                startActivity(prefIntent);
//                return true;
//            case R.id.about:
//                Intent aboutIntent = new Intent(this, AboutActivity.class);
//                aboutIntent.putExtra(AboutActivity.EXTRA_DEVICE_NAME, mDeviceName);
//                aboutIntent.putExtra(AboutActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
//                aboutIntent.putExtra(AboutActivity.EXTRA_DEVICE_CONNECTION_STATUS, mDeviceConnectionStatus);
//                startActivity(aboutIntent);
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
    }

    /**
     * If probe connected and clicked on go to survey popup activity (InialisePopupActivity)
     * @param v
     */
    public void BlackProbeBtnClick(View v) {
        if (mConnected) {
            Intent intent = new Intent(this, InitalisePopupActivity.class);
            Log.d(TAG, "Device Name: " + mDeviceName + ", Device Address: " + mDeviceAddress);
            intent.putExtra(InitalisePopupActivity.EXTRA_DEVICE_NAME, mDeviceName);
            intent.putExtra(InitalisePopupActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
            startActivity(intent);
        } else {
            Log.e(TAG, "Probe is disconnected");
        }
    }

    //TODO fix this horror of a naming scheme
    public void WhiteProbeSelect(View v) {
        //Check if device is connected before allowing user to see data from the probe
        if (whiteProbeStatusTxt.equals("Disconnected")) {
            //TODO make a popup that says device not connected cannot get data
        } else {
            Intent intent = new Intent(this, ProbeDetails.class);
            startActivity(intent);
        }
    }

    /**
     * Is probe connected and clicked on go to probe details
     * @param v
     */
    public void blackProbeSelect(View v) {
        //Check if device is connected before allowing user to see data from the probe
        if (blackProbeStatusTxt.equals("Disconnected") || mDeviceConnectionStatus == null) {
            //TODO make a popup that says device not connected cannot get data
        } else {
            Intent intent = new Intent(this, ProbeDetails.class);
            intent.putExtra(ProbeDetails.EXTRA_DEVICE_NAME, mDeviceName);
            intent.putExtra(ProbeDetails.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
            intent.putExtra(ProbeDetails.EXTRA_DEVICE_CONNECTION_STATUS, mDeviceConnectionStatus);
            startActivity(intent);
        }
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
     * Calibrate probe
     * @param view
     */
    public void calibrate(View view) {
        //get calibration data + write to calibration index then read again
        displayGattServices(mBluetoothLeService.getSupportedGattServices()); //read calibation index then calibration data
    }


    /**
     * Get next calibration index value
     * @param view
     */
    public void index(View view) {
        if (Globals.enableCalibration) {
            boolean status = false;
            do {
                //(byte) calibrationIndexNum
                status = mBluetoothLeService.writeToCalibrationIndex((byte) 00); //TODO make this a variable input
//            Log.e(TAG, "Status of write: " + status);
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    Log.e(TAG, "Could not sleep" + e);
                }
                if (status) {
//                dataToBeRead = 0;
                    displayGattServices(mBluetoothLeService.getSupportedGattServices());

                } else {
                    try {
                        if (mDeviceConnectionStatus.equals("Connected")) {
                            updateConnectionState("Connected");
                        } else {
                            Log.e(TAG, "Device disconnected");
                            updateConnectionState("Disconnected");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error setting connection state: " + e);
                    }
                }
                new CountDownTimer(700, 1) { //definetly inefficicent, but if it works dont touch it lol

                    public void onTick(long millisUntilFinished) {
                        //                mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
                    }

                    public void onFinish() {
                        //                    dataToBeRead = 0;
                        //                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
                    }
                }.start();

                new CountDownTimer(3000, 1) { //definetly inefficicent, but if it works dont touch it lol

                    public void onTick(long millisUntilFinished) {
                        //                mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
                    }

                    public void onFinish() {
                        //                    dataToBeRead = 0;
                        //                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
                    }
                }.start();

            } while (!status);
//        calibrationIndexNum++;
        }
    }

    public void indexNext() {
        if (Globals.enableCalibration) {
            if (Globals.enableCalibration) {
                blackProbeStatusImg.setImageResource(R.drawable.calibrating);
                blackProbeStatusTxt.setText("Calibrating");
                boolean status = false;
                do {
                    status = mBluetoothLeService.writeToCalibrationIndex((byte) calibrationIndexNum); //TODO make this a variable input
//            Log.e(TAG, "Status of write: " + status);
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        Log.e(TAG, "Could not sleep" + e);
                    }
                    if (status) {
//                dataToBeRead = 0;
                        displayGattServices(mBluetoothLeService.getSupportedGattServices());

                        //                if (currentOp == null) {
                        //                    Log.e(TAG, "2nd");
                        //                    dataToBeRead = 0;
                        //                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
                        //                }
                    } else {
                        try {
                            if (mDeviceConnectionStatus.equals("Connected")) {
                                updateConnectionState("Connected");
                            } else {
                                Log.e(TAG, "Device disconnected");
                                updateConnectionState("Disconnected");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error setting connection state: " + e);
                        }
                    }
                    new CountDownTimer(700, 1) { //definetly inefficicent, but if it works dont touch it lol

                        public void onTick(long millisUntilFinished) {
                            //                mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
                        }

                        public void onFinish() {
                            //                    dataToBeRead = 0;
                            //                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
                        }
                    }.start();

                    new CountDownTimer(3000, 1) { //definetly inefficicent, but if it works dont touch it lol

                        public void onTick(long millisUntilFinished) {
                            //                mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
                        }

                        public void onFinish() {
                            //                    dataToBeRead = 0;
                            //                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
                        }
                    }.start();

                } while (!status);
            }
        }

    }
}
