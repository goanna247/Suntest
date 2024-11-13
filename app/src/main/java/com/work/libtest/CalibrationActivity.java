//////////////////////////////////////////////////////////////////////////////////
///**
// * \file CalibrationActivity
// * \brief Calibrates the probe and saved data to calibration helper before returning probe to main activity
// * \author Anna Pedersen
// * \date Created: 10/10/2024
// *
// */
//package com.work.libtest;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import android.os.Bundle;
//import android.util.Log;
//import android.widget.TextView;
//
//import com.work.libtest.databinding.ActivityMainBinding;
//
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
//import java.util.Queue;
//import java.util.Set;
//import java.util.TimeZone;
//
//public class CalibrationActivity extends AppCompatActivity {
//
//    //information passed through from BluetoothTools.DeviceScanActivity after connecting to a device
//    public static final String EXTRA_DEVICE_NAME = "DEVICE_NAME";
//    public static final String EXTRA_DEVICE_ADDRESS = "DEVICE_ADDRESS";
//    public static final String EXTRA_CONNECTION_STATUS = "CONNECTION_STATUS";
//    private static final String NAME_STATE_KEY = "DEVICE NAME STATE KEY";
//    private static final String ADDRESS_STATE_KEY = "ADDRESS STATE KEY";
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
//    private String mMode;
//    private String  modifiedDateString   = "No calibration data";
//    private String  calibratedDateString = "No calibration data";
//
//    private boolean isCalibrated = false;
//    final int NUM_CAL_PARAMS_EXPECTED_DURING_PARSING = 38;
//    private int binCalData_size = 0;
//    private double acc_A[]   = new double[3];    // offsets
//    private double acc_B[][] = new double[3][3]; // first order terms
//    private double acc_C[]   = new double[3];    // cubic terms
//
//    private double mag_A[]   = new double[3];
//    private double mag_B[][] = new double[3][3];
//    private double mag_C[]   = new double[3];
//
//    private double temp_param[]   = new double[2];   // offset, scale
//
//    private double accManualZeroOffset = 0;  // used in zero roll offset feature of corecams to align drill head
//    private int offset_of_accManualZeroOffset = 0;  // where accManualZeroOffset is located in the the current cal binary
//
//    private double magManualZeroOffset = 0;  // don't think this is used
//
//    boolean started = false;
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
//    private static final String TAG = "Calibration Activity";
//    private Handler handler;
//
//    public static ArrayList<Survey> surveys = new ArrayList<Survey>();
//    public static int surveySize = 0;
//    public static Preferences preferences = new Preferences();
//    public static String connectedDeviceName = "No Probe Selected";
//
//    public static ArrayList<String> caliData = new ArrayList<>();
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
//    Handler timerHandler = new Handler();
//    Runnable timerRunnable = new Runnable() {
//        @Override
//        public void run() {
//            long millis = System.currentTimeMillis() - startTime;
//            seconds = (int) (millis / 1000);
//
//            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//            if (!mBluetoothAdapter.isEnabled()) {
//                updateConnectionState("Disconnected");
//            }
//
//            if (mConnected == false) {
//                updateConnectionState("Disconnected");
//            }
////            timerHandler.postDelayed(this, 5000); //potential time delay
//
//            try {
//                if (caliDataCollected && !CalibrationMatrixCreated) {
//                    ArrayList<String> newCaliData = new ArrayList<>();
//                    String buffer = "";
//
//                    newCaliData.add(caliData.get(0));
//                    for (int i = 1; i < caliData.size(); i++) {
//                        if (i % 2 != 0) {
//                            buffer = caliData.get(i);
//                        } else {
//                            newCaliData.add(caliData.get(i));
//                            newCaliData.add(buffer);
//                            buffer = "";
//                        }
//                    }
//
//                    StringBuffer sb = new StringBuffer();
//                    for (int i = 0; i < newCaliData.size(); i++) {
//                        sb.append(newCaliData.get(i));
//                    }
//                    String str = sb.toString();
//
//                    Log.e(TAG, "RAW STRING: " + str);
//
//                    String[] stringNumbers = str.split(":");
//                    byte[] byteArray = new byte[stringNumbers.length]; // Create a byte array of the same length as the string array
//
//                    // Convert each string number to a byte and store in the byte array
//                    for (int i = 0; i < stringNumbers.length; i++) {
//                        byteArray[i] = Byte.parseByte(stringNumbers[i]);
//                    }
//
////                  Print the byte array to verify
//                    for (byte b : byteArray) {
//                        Log.e(TAG, String.valueOf(b));
//                    }
//
//                    binCalData = byteArray; //needs to be re-implemented
//                    binCalData = new byte[]{1, 0, 8, 1, 105, 4, -81, 2, 111, 89, 20, 16, 1, 91, 1, 16, 4, -81, 2, 111, 89, 32, -100, -84, 1, 63, -16, 53, 96, -100, 84, 90, -27, -65, 62, 5, -72, 27, 29, 27, -124, -65, -110, 20, 103, -84, 67, -29, 26, -65, 45, 18, -8, 105, -10, -69, 114, 63, -17, -123, -26, 55, 4, -27, -38, -65, -115, 90, -116, 62, -95, 103, 109, 63, 98, 11, -21, 119, 49, -19, -20, 63, 31, -18, 81, 16, -65, 81, -86, 63, -16, 59, -5, -91, -10, 23, -61, 2, 63, -120, 114, -125, 0, 24, 122, 62, 0, 0, 0, 0, 0, 0, 0, 0, 63, -118, 103, -57, -56, -9, -85, -61, 0, 0, 0, 0, 0, 0, 0, 0, -65, 105, -121, 26, -31, 74, -1, -12, 0, 0, 0, 0, 0, 0, 0, 0, 1, 64, 0, 59, 93, -12, -67, 70, 61, -65, 109, -65, 44, 117, 26, 26, 123, -65, 18, -99, -80, 100, -102, 26, -8, -65, 93, 54, -43, 78, -103, -61, 89, 48, -100, -84, 1, 63, -16, -126, -20, 71, -16, 68, 6, 63, 126, 97, -84, -10, -17, 98, -69, -65, -123, 112, 66, -59, -117, 40, -46, -65, 126, -48, -52, -66, 110, 24, -11, 63, -16, 96, 69, -18, -104, -8, 36, 63, 96, 15, 10, 98, -2, -98, -54, -65, -107, 60, -98, -124, 20, -96, 98, 63, 81, -66, -66, -80, -77, 111, -22, 63, -15, 25, -25, -3, -55, -44, -84, 2, 64, 20, -115, -20, -73, -81, 12, -40, 0, 0, 0, 0, 0, 0, 0, 0, 64, 14, -92, -119, -95, -43, 100, 96, 0, 0, 0, 0, 0, 0, 0, 0, 63, -38, -46, -59, -39, 19, -67, 84, 0, 0, 0, 0, 0, 0, 0, 0, 1, 64, 0, 59, 93, -12, -67, 70, 61, 62, -105, 95, -84, 112, 51, -106, -59, 62, -104, -83, -71, -79, 77, 57, 1, 62, -120, 5, -45, 11, 86, 103, -5, 64, 18, 1, 2, 64, 64, 0, 0, 0, 0, 0, 0, 63, -18, 102, 102, 102, 102, 102, 98, 0, 2, -58, -12, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
//
//                    calibration_status = parseBinaryCalibration();
//
//                    if (calibration_status == true) {
//                        //TODO - Add calibration details to calibration Helper
//                        CalibrationHelper.addKnowCalData(8034, byteArray); //change 8034 to be the current probe number
//                    } else {
//                        //will need to do something here to display that the probe is not connected
//                    }
//
//                    Log.e(TAG, "BINARY CAL CHECK NUMBER: " + String.valueOf(mag_A[0]));
//
//                    CalibrationHelper.mag_A = mag_A;
//                    CalibrationHelper.mag_B = mag_B;
//                    CalibrationHelper.mag_C = mag_C;
//
//                    CalibrationHelper.acc_A = acc_A;
//                    CalibrationHelper.acc_B = acc_B;
//                    CalibrationHelper.acc_C = acc_C;
//
//                    CalibrationHelper.temp_param = temp_param;
//                    CalibrationMatrixCreated = true;
//                    Log.e(TAG, "Updating connection state to connected");
//                    updateConnectionState("Connected");
//                }
//            } catch (Exception e) {
//                Log.e(TAG, "Exception thrown running main activity: " + e);
//            }
//        }
//    };
//
//    private final Queue<Operation> operations = new LinkedList<>();
//    private Operation currentOp;
//    private static boolean operationOngoing = false;
//
//    /**
//     *
//     * @param operation being requested
//     */
//    public synchronized void request(Operation operation) {
//        try {
//            operations.add(operation);
//            if (currentOp == null) {
//                currentOp = operations.poll();
//                performOperation();
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Error thrown while requesting an operation");
//        }
//    }
//
//    /**
//     * Excecutes whenever an operation is finished
//     */
//    public synchronized void operationCompleted() {
//        currentOp = null;
//        if (operations.peek() != null) {
//            currentOp = operations.poll();
//            performOperation();
//        } else {
//            if (!caliDataCollected && !CalibrationMatrixCreated) {
//
//                try {
//                    if (!caliData.get(caliData.size()-1).equals("-1:-1:-1:-1:-1:-1:-1:-1:-1:-1:-1:-1:-1:-1:-1:-1:")) {
//                        calibrationIndexNum++;
//                        indexNext();
//                    } else {
////                        print original
//                        Log.e(TAG, "All Calibration Data: ");
//                        for (int i = 0; i < caliData.size(); i++) {
//                            Log.e(TAG, caliData.get(i));
//                        }
//
//                        Set<String> s = new LinkedHashSet<String>(caliData);
//                        Log.e(TAG, s.toString());
//                        List<String> list = new ArrayList<>(s);
//                        list.remove(list.size()-1);
//
//                        //print
//                        Log.e(TAG, "All Calibration Data: ");
//                        for (int i = 0; i < list.size(); i++) {
//                            Log.e(TAG, list.get(i));
////                            CalibrationHelper.
////                            list.get(i);
//                        }
//                        caliData = (ArrayList<String>) list;
//                        caliDataCollected = true;
//                        updateConnectionState("Connected");
////                        blackProbeStatusImg.setImageResource(R.drawable.ready);
////                        blackProbeStatusTxt.setText("Connected");
//                    }
//                } catch (Exception e) {
//                    Log.e(TAG, "Exception thrown: " + e);
//                }
//            }
//        }
//    }
//
//    /**
//     * Perform requested operation
//     */
//    public void performOperation() {
//        if (currentOp != null) {
//            if (currentOp.getService() != null && currentOp.getCharacteristic() != null && currentOp.getAction() != 0) {
//                switch (currentOp.getAction()) {
//                    case OPERATION_WRITE:
//                        mBluetoothLeService.writeData(currentOp.getCharacteristic());
//                        break;
//                    case OPERATION_READ:
//                        mBluetoothLeService.readCharacteristic(currentOp.getCharacteristic());
//                    case OPERATION_NOTIFY:
//
//                        break;
//                    default:
//                        break;
//                }
//            }
//        }
//    }
//
//    /**
//     * reset all operations
//     */
//    public void resetQueue() {
//        operations.clear();
//        currentOp = null;
//    }
//
//    private interface MessageConstants {
//        public static final int MESSAGE_READ = 0;
//        public static final int MESSAGE_WRITE = 1;
//        public static final int MESSAGE_TOAST = 2;
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
//            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService(); //TODO had to chuck all the bluetooth files inside the main folder to get this working, can probs figure out a better solution
//            if (!mBluetoothLeService.initialize()) {
//                Log.e(TAG, "Enable to initialize bluetooth");
//                finish();
//            }
//            mBluetoothLeService.connect(mDeviceAddress);
//        }
//
//        /**
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
//         * @param context The Context in which the receiver is running.
//         * @param intent The Intent being received.
//         */
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            final String action = intent.getAction();
//            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
//                mConnected = true;
//                updateConnectionState("Connected");
//                invalidateOptionsMenu();
//                mDeviceConnectionStatus = "Connected";
//
//            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
//                mConnected = false;
//                updateConnectionState("Disconnected");
//                invalidateOptionsMenu();
//                clearUI();
//                mDeviceConnectionStatus = "Disconnected";
//            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
//            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
//
//                if (intent.getStringExtra(BluetoothLeService.EXTRA_DATA) != null) {
//                } else if (intent.getStringExtra(BluetoothLeService.CALIBRATION_INDEX) != null) {
//                } else if (intent.getStringExtra(BluetoothLeService.CALIBRATION_DATA) != null) {
//                    caliData.add(intent.getStringExtra(BluetoothLeService.CALIBRATION_DATA));
//                    Log.e(TAG, intent.getStringExtra(BluetoothLeService.CALIBRATION_DATA));
//                } else if (intent.getStringExtra(BluetoothLeService.DEVICE_ADDRESS) != null) {
//                }
//                operationCompleted();
//
//            } else if (intent != null) {
//                operationCompleted();
//            }
//        }
//    };
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
//        inflater.inflate(R.menu.menu_main_activity, menu);
//        this.menu = menu;
//        return true;
//    }
//
//    private void clearUI() {
//    }
//
//    private class ConnectedThread extends Thread {
//        private final BluetoothSocket mmSocket;
//        private final InputStream mmInStream;
//        private final OutputStream mmOutStream;
//        private byte[] mmBuffer;
//
//        /**
//         * @param socket - Bluetooth socket
//         */
//        public ConnectedThread(BluetoothSocket socket) {
//            mmSocket = socket;
//            InputStream tmpIn = null;
//            OutputStream tmpOut = null;
//
//            try {
//                tmpIn = socket.getInputStream();
//            } catch (IOException e) {
//                Log.e(TAG, "Error occured when creating input stream", e);
//            }
//            try {
//                tmpOut = socket.getOutputStream();
//            } catch (IOException e) {
//                Log.e(TAG, "Error occured when creating output stream", e);
//            }
//
//            mmInStream = tmpIn;
//            mmOutStream = tmpOut;
//        }
//
//        public void run() {
//            mmBuffer = new byte[1024];
//            int numBytes;
//
//            while (true) {
//                try {
//                    numBytes = mmInStream.read(mmBuffer);
//                    Message readMsg = handler.obtainMessage(
//                            MessageConstants.MESSAGE_READ, numBytes,-1, mmBuffer
//                    );
//                    readMsg.sendToTarget();
//                } catch (IOException e) {
//                    Log.d(TAG, "Input stream was disconnected", e);
//                    break;
//                }
//            }
//        }
//    }
//
//    /**
//     *
//     * @param savedInstanceState If the activity is being re-initialized after
//     *     previously being shut down then this Bundle contains the data it most
//     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
//     *
//     */
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        //TODO if in core mode / vs in bore mode
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_calibration);
//
//        Log.e(TAG, "CALIBRATION STATUS: " + caliDataCollected);
//
//        startTime = System.currentTimeMillis();
//        timerHandler.postDelayed(timerRunnable, 0);
//
//        final Intent intent = getIntent();
//        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
//        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
//        mDeviceConnectionStatus = intent.getStringExtra(EXTRA_CONNECTION_STATUS);
//
//        Log.e(TAG, "passed in name: " + intent.getStringExtra(EXTRA_DEVICE_NAME) + "Passed in address: " + intent.getStringExtra(EXTRA_DEVICE_ADDRESS) + "Passed in connection: " + intent.getStringExtra(EXTRA_CONNECTION_STATUS));
//
//        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
//        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
//
//        if (!caliDataCollected && Globals.enableCalibration) {
//            updateConnectionState("Disconnected");
//            Log.e(TAG, "Calibration data not aquired: hence disconnected");
//        } else {
//            if (mDeviceConnectionStatus != null) {
//                updateConnectionState(mDeviceConnectionStatus);
//                if (mDeviceConnectionStatus.equals("Connected")) {
//                    mConnected = true;
//                }
//            } else {
//                Log.e(TAG, "ERROR DEVICE CONNECTION STATUS IS NULL");
//            }
//            Log.e(TAG, "updating connection state to: " + mDeviceConnectionStatus);
//        }
//    }
//
//    //CALLBACK
//    private double getDouble(int offset) {
//        // concatenate the bytes to an 8 byte long (assume the endianness is correct)
//        double dValue = 0;
//        try {
//            long lValue = 0;
//            for (int i = 0; i < 8; i++) {
//                //for (int i = 7; i >= 0; i--) {
//                lValue = (lValue << 8) + (((long) binCalData[offset + i]) & 0xFF);
//            }
//            dValue = Double.longBitsToDouble(lValue);
//            //Log.i(TAG, String.format("PJH - getDouble: lValue=0x%08X dValue=%f", lValue, dValue));
//
//        } catch (Exception e) {
//            Log.e(TAG, "Exception caught in " + e);
//        }
//        return(dValue);
//    }
//
//    public boolean parseBinaryCalibration() {
//        boolean result = false;
//        int coeffs_found = 0; // should be NUM_CAL_PARAMS_EXPECTED_DURING_PARSING (38) when finished
//
//        Log.w(TAG, "Beginning parse calibration protocol");
//        try {
//            isCalibrated = false;
//            int p = 0; //base 0, pointer into cal binary
//
//            // dump the header for debugging
//            Log.i(TAG, String.format("- calPacket: %02X %02X %02X %02X %02X %02X %02X %02X", binCalData[0], binCalData[1], binCalData[2], binCalData[3], binCalData[4], binCalData[5], binCalData[6], binCalData[7]));
//            Log.i(TAG, String.format("-            %02X %02X %02X %02X %02X %02X %02X %02X", binCalData[8], binCalData[9], binCalData[10], binCalData[11], binCalData[12], binCalData[13], binCalData[14], binCalData[15]));
//
//            //CALLBACK
//            // first check if the header (of the header) is valid (first byte is 0x01 and high byte of length is 0x00)
//            if (((((int) binCalData[0]) & 0xFF) == 0x01) && ((((int) binCalData[1]) & 0xFF) == 0x00)) {
//                // use low byte of header length to determine the offset of the 8bit CRC
//                // (header length includes CRC, but not blk id or length)
//                // plus 3 to give total length of header, less 1 to convert nth byte to base 0 offset
//                int crc_offset = (((int) binCalData[2]) & 0xFF) + 3 - 1;   // this is also the number of bytes preceeding the crc byte
//
//                // the 8bit header CRC spans all bytes in this block, excluding the CRC itself
//                int crc8 = cal_crc8_gen(binCalData, 0, crc_offset-1);     // (data[], start, end)
//
//                if (crc8 == (((int) binCalData[crc_offset]) & 0xFF)) {   // beware sign extension
//                    // ok, the crc of the header is correct, so we can proceed
//
//                    // get the 'modified' date out of the header block (it is in a fixed location)
//                    int m_date_len = ((int)binCalData[5]) & 0xFF;   // should be 8 for current calibrations, or maybe 4 for archaic probes
//                    if ((m_date_len == 4) || (m_date_len==8)) {
//                        long unix_tick = 0;
//                        for (int i = (6 + m_date_len - 1); i >= 6; i--) {  // little endian
//                            unix_tick = (unix_tick << 8) + (((int)binCalData[i]) & 0xFF);
//                        }
//
//                        Date date = new Date(unix_tick * 1000L);
//                        SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
//                        jdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
//                        modifiedDateString = jdf.format(date);
//
//                        coeffs_found += 1;
//                        Log.i(TAG, String.format("PJH - parse - modified date = %s", modifiedDateString));
//                    }
//                    else
//                    {
//                        modifiedDateString = "not initialised";
//                    }
//
//                    // the calibrated date string will be retrieved later, from the main data block
//
//
//                    // now we want to check the crc of the actual calibration data
//                    //int start = (((int) binCalData[2]) & 0xFF) + 3 + 3;   // first byte after the block size
//                    //int total_len = ((((int) binCalData[3]) & 0xFF) << 8) + (((int) binCalData[4]) & 0xFF);
//                    //int end_plus_one = total_len - 2;
//                    //Log.i(TAG, String.format("PJH - start: %06X end+1: %06X", start, end_plus_one));
//                    //System.arraycopy(binCalData, start, buffer, 0, end_plus_one - start);    // (src,dst,len)
//                    //
//                    //int crc16 = cal_crc16_gen(buffer, end_plus_one - start);
//
//                    // the 16 bit CRC of the main data block starts AFTER the blk_id and blk_len
//                    // and includes the last byte before the actual CRC16
//                    // (so the main blk_id and blk_len are NOT covered by the CRC16 - this is a BUG,
//                    // but can't change it as we need to maintain compatibility with the ipod,
//                    // so will need to manually check those separately)
//                    int start = (((int) binCalData[2]) & 0xFF) + 3 + 3;   // offset of header byte of main data block
//                    int total_len = ((((int) binCalData[3]) & 0xFF) << 8) + (((int) binCalData[4]) & 0xFF);  // big endian
//                    binCalData_size = total_len;   // save the number of valid byte entries in binCalData
//                    coeffs_found += 1;
//                    int end = total_len - 2 - 1;   //  total len - 2 byte crc - 1 to convert len to offset
//                    Log.i(TAG, String.format("PJH - start: %06X end: %06X", start, end));
//                    int crc16 = cal_crc16_gen(binCalData, start, end);   // (data[], start, end)
//
//                    // get the crc16 from the binary cal data (from the last two bytes)
//                    int binCrc16 = ((((int) binCalData[total_len - 2]) & 0xFF) << 8) + (((int) binCalData[total_len - 1]) & 0xFF);  // big endian
//
//                    if (crc16 == binCrc16) {
//                        // both CRCs are good, so extract calibration coefficients
//                        Log.i(TAG, "PJH - parse - both calibration CRCs are good");
//                        p = crc_offset + 1;   // first byte of the main body
//
//                        // the following log entry shows the allocated size of binCalData which is not useful
//                        //Log.i(TAG, String.format("PJH - parse - binCalData length: 0x%04X", binCalData.length));
//
//                        if (((((int) binCalData[p]) & 0xFF) == 0x10)) {   // this blk_id is not covered by CRC so check it here
//                            // the subsequent 2 byte blk_len is not used in this parsing, so ignore it for now
//                            // TODO - we really should check it though, as don't know how ipod would react if it was corrupted
//                            p += 3;  // point to cal record type
//                            int cal_record_type = (((int) binCalData[p]) & 0xFF);
//                            // cal_record_type is always 0x01 for the calibration parameters we are using
//                            // TODO - should check it is 0x01, just to be sure
//                            Log.i(TAG, String.format("PJH - parse @ 0x%04X - cal record type: 0x%02X", p, cal_record_type));
//                            p += 1;  // and advance to the first block of calibration parameters
//
//                            // Now we can finally start reading in the calibration parameters that we need
//                            // Each set of related parameters and in a separate block
//                            int hdr_id, blk_len, pTmp, flags;
//                            while (p < total_len) {
//                                // first two bytes of the block contain the hdr_id in the highest nibble, and blk_len in remaining three nibbles
//                                hdr_id = (((int) binCalData[p]) & 0xF0) >> 4;
//                                blk_len = (((((int) binCalData[p]) & 0x0F) << 8) + (((int) binCalData[p + 1]) & 0xFF)) & 0x0FFF;
//                                p += 2;
//                                Log.i(TAG, String.format("PJH - parse @ 0x%04X - hdr_id: 0x%02X, length: 0x%04X", p, hdr_id, blk_len));
//
//                                // p is pointing to the first byte of content in this calibration parameter block
//                                if (hdr_id == 0x00) {    // CRC block - ignore =============================================
//                                    // the CRC16 block is ALWAYS the last block in the calibration file,
//                                    // so that the CRC16 is the last two bytes of the calibration file
//
//                                    // we have already checked this CRC, so ignore it
//                                    p += blk_len;
//                                }
//                                else if (hdr_id == 0x01) {    // timestamp block =============================================
//                                    // extract the Calibrated Date
//
//                                    int c_date_len = blk_len;  // should be 8 for current calibrations, or maybe 4 for archaic probes
//                                    if ((c_date_len == 4) || (c_date_len == 8)) {
//                                        long unix_tick = 0;
//                                        for (int i = (p + c_date_len - 1); i >= (p); i--) {  // timestamp is little endian
//                                            unix_tick = (unix_tick << 8) + (((int)binCalData[i]) & 0xFF);
//                                        }
//
//                                        Date date = new Date(unix_tick * 1000L);
//                                        SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
//                                        jdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
//                                        calibratedDateString = jdf.format(date);
//
//                                        coeffs_found += 1;
//                                        Log.i(TAG, String.format("PJH - parse - calibrated date = %s", calibratedDateString));
//
//                                    }
//                                    else
//                                    {
//                                        calibratedDateString = "not initialised";
//                                    }
//
//                                    p += blk_len;
//                                }
//                                else if (hdr_id == 0x02) {    // accel block =============================================
//                                    pTmp = p;  // we don't want to mess up 'p'
//                                    // flags tells up what sub field exist in this block
//                                    flags = (((((int) binCalData[pTmp + 1]) & 0xFF) << 8) + (((int) binCalData[pTmp]) & 0xFF));  // little endian
//                                    pTmp += 2;  // skip over flags
//
//                                    if ((flags & 0x0001) != 0) {
//                                        // temperature data - skip it   TODO
//                                        // PJH - this relates to probes that are calibrated a two different temperatures
//                                        // none of the probes have calibration data that is temperature dependent
//                                        pTmp += 8;
//                                    }
//                                    if ((flags & 0x0004) != 0) {
//                                        // bit 2 is set, so first order terms are present - 3x3
//                                        // each of the 9 parameters are double precision floats
//                                        for (int r = 0; r < 3; r++) {
//                                            for (int c = 0; c < 3; c++) {
//                                                acc_B[r][c] = getDouble(pTmp);
//                                                Log.i(TAG, String.format("PJH - parse - acc_B[%d][%d] = %15.12f", r, c, acc_B[r][c]));
//                                                coeffs_found += 1;
//                                                pTmp += 8;
//                                            }
//                                        }
//                                    }
//                                    if ((flags & 0x0008) != 0) {
//                                        // bit 3 is set, so zero order terms are present
//
//                                        // HACK - the next byte tells us the depth of this 3 wide matrix
//                                        // Don't understand why this is here, but it is
//                                        // ASSUME it will be 2 and skip over it - YUK
//                                        pTmp+=1; // just assume this count is for the 3x2 matrix  TODO
//                                        // zero order terms - 3x2 - don't know why there are extra co-efficients
//                                        for (int r = 0; r < 3; r++) {
//                                            for (int c = 0; c < 2; c++) {
//                                                if (c==0) {  // the first column is what we want (other column is zeros)
//                                                    acc_A[r] = getDouble(pTmp);
//                                                    Log.i(TAG, String.format("PJH - parse - acc_A[%d] = %15.12f", r, acc_A[r]));
//                                                    coeffs_found += 1;
//                                                }
//                                                pTmp += 8;
//                                            }
//                                        }
//                                    }
//                                    //
//                                    // manual zero offset
//                                    //
//                                    // next byte tells us how many zero offset terms there are
//                                    pTmp+=1; // just assume the count is 1 and skip over it - YUK   TODO
//                                    // PJH - don't know exactly how this field is used in the ipod, so need to reverse engineer that
//                                    // to ensure the android behaves the same (is this value +ve or -ve? is it added or subtracted?)
//                                    offset_of_accManualZeroOffset = pTmp;   // remember the location of this parameter, so we can change it later
//                                    accManualZeroOffset = getDouble(pTmp);
//                                    coeffs_found += 2;
//                                    Log.i(TAG, String.format("PJH - parse - acc Manual Zero offset = %15.12f", accManualZeroOffset));
//                                    Log.i(TAG, String.format("PJH - parse - acc Manual Zero offset is located at offset = %6d", offset_of_accManualZeroOffset));
//                                    pTmp += 8;
//
//                                    if ((flags & 0x0100) != 0) {
//                                        // bit 8 is set, so cubic order terms are present - 1x3
//                                        for (int c = 0; c < 3; c++) {
//                                            acc_C[c] = getDouble(pTmp);
//                                            Log.i(TAG, String.format("PJH - parse - acc_C[%d] = %15.12f", c, acc_C[c]));
//                                            coeffs_found += 1;
//                                            pTmp += 8;
//                                        }
//                                    }
//                                    p += blk_len;   // advance pointer to next block
//                                }
//                                else if (hdr_id == 0x03) {    // mag block =============================================
//                                    if (blk_len <= 10) {
//                                        // ignore this malformed magnetometer block - effects Ezycore (maybe Corecam)
//
//                                        // assume we are talking to a corecam and clear all mag variables
//                                        // and update parameter count, for compatibility
//                                        mag_A[0] = 0;
//                                        mag_A[1] = 0;
//                                        mag_A[2] = 0;
//                                        mag_B[0][0] = 0;
//                                        mag_B[0][1] = 0;
//                                        mag_B[0][2] = 0;
//                                        mag_B[1][0] = 0;
//                                        mag_B[1][1] = 0;
//                                        mag_B[1][2] = 0;
//                                        mag_B[2][0] = 0;
//                                        mag_B[2][1] = 0;
//                                        mag_B[2][2] = 0;
//                                        mag_C[0] = 0;
//                                        mag_C[1] = 0;
//                                        mag_C[2] = 0;
//                                        magManualZeroOffset = 0;
//                                        coeffs_found += 16;
//                                    }
//                                    else {
//                                        pTmp = p;  // we don't want to mess up 'p'
//                                        // flags tells up what sub field exist in this block
//                                        flags = (((((int) binCalData[pTmp + 1]) & 0xFF) << 8) + (((int) binCalData[pTmp]) & 0xFF));
//                                        pTmp += 2;  // skip over flags
//
//                                        if ((flags & 0x0001) != 0) {
//                                            // temperature data - skip it
//                                            pTmp += 8;
//                                        }
//                                        if ((flags & 0x0004) != 0) {
//                                            // first order terms - 3x3
//                                            for (int r = 0; r < 3; r++) {
//                                                for (int c = 0; c < 3; c++) {
//                                                    mag_B[r][c] = getDouble(pTmp);
//                                                    Log.i(TAG, String.format("PJH - parse - mag_B[%d][%d] = %15.12f", r, c, mag_B[r][c]));
//                                                    coeffs_found += 1;
//                                                    pTmp += 8;
//                                                }
//                                            }
//                                        }
//                                        if ((flags & 0x0008) != 0) {
//                                            pTmp+=1; // just assume this count is for the 3.2 matrix - TODO
//                                            // zero order terms - 3x2 - don't know why there are extra co-efficients
//                                            for (int r = 0; r < 3; r++) {
//                                                for (int c = 0; c < 2; c++) {
//                                                    if (c==0) {  // the first column is what we want (other column is zeros)
//                                                        mag_A[r] = getDouble(pTmp);
//                                                        Log.i(TAG, String.format("PJH - parse - mag_A[%d] = %15.12f", r, mag_A[r]));
//                                                        coeffs_found += 1;
//                                                    }
//                                                    pTmp += 8;
//                                                }
//                                            }
//                                        }
//                                        // manual zero offset - I don't believe this is used anywhere
//                                        pTmp+=1; // just assume the count is 1  TODO
//                                        magManualZeroOffset = getDouble(pTmp);
//                                        coeffs_found += 1;
//                                        Log.i(TAG, String.format("PJH - parse - mag Manual Zero offset = %15.12f", magManualZeroOffset));
//                                        pTmp += 8;
//
//                                        if ((flags & 0x0100) != 0) {
//                                            // cubic order terms - 1x3
//                                            for (int c = 0; c < 3; c++) {
//                                                mag_C[c] = getDouble(pTmp);
//                                                Log.i(TAG, String.format("PJH - parse - mag_C[%d] = %15.12f", c, mag_C[c]));
//                                                coeffs_found += 1;
//                                                pTmp += 8;
//                                            }
//                                        }
//
//                                    }
//
//                                    p += blk_len;   // advance pointer to next block
//                                }
//                                else if (hdr_id == 0x04) {    // temperature block =============================================
//                                    // these parameters are used to calibrate the main temperature sensor
//                                    pTmp = p;  // we don't want to mess up 'p'
//                                    // flags tells up what sub field exist in this block
//                                    flags = ((int) binCalData[pTmp]) & 0xFF;
//                                    int count = ((int) binCalData[pTmp + 1]) & 0xFF;   // YUK - just assuming this is 2
//                                    pTmp += 2;  // skip over flags and parameter count
//
//                                    if ((flags & 0x01) != 0) {
//                                        // temperature data - just assume 2 parameters??? - YUK - TODO
//                                        temp_param[0] = getDouble(pTmp);  //offset
//                                        pTmp += 8;
//                                        temp_param[1] = getDouble(pTmp);  // scale
//                                        pTmp += 8;
//                                        coeffs_found += 2;
//                                        Log.i(TAG, String.format("PJH - parse - temp[0] = %15.12f (offset)", temp_param[0]));
//                                        Log.i(TAG, String.format("PJH - parse - temp[1] = %15.12f (scale)",  temp_param[1]));
//                                    }
//                                    p += blk_len;   // advance pointer to next block
//                                }
//                                else {             // =============================================
//                                    p += blk_len;  // just skip any unknown blocks (shouldn't be any)
//                                }
//                            }
//
//                            // do a final sanity check to ensure we got all the parameters we expected
//                            if (coeffs_found == NUM_CAL_PARAMS_EXPECTED_DURING_PARSING) {
//                                isCalibrated = true;
//                                Log.i(TAG, String.format("PJH - parse - found %d calibration parameters", coeffs_found));
//                                Log.w(TAG, "PJH - successfully parsed binary calibration data  =======================");
//                                CalibrationMatrixCreated = true;
//                                caliDataCollected = true;
//                            }
//                        }
//
//
//                    } else {
//                        // oops, calibration data is not valid
//                        Log.w(TAG, "PJH - CRC of the body of the binary cal data is incorrect - aborting");
//                    }
//                } else {
//                    // oops, calibration data is not valid
//                    Log.w(TAG, "PJH - CRC of the header of the binary cal data is incorrect - aborting");
//                }
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "PJH - parse - Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
//        }
//
//        if (isCalibrated != true) {
//            Log.w(TAG, "PJH - parsing binary calibration data FAILED =======================");
//            Log.i(TAG, String.format("PJH - parse - only found %d calibration parameters (expected %d)", coeffs_found, NUM_CAL_PARAMS_EXPECTED_DURING_PARSING));
//        }
//
//        return isCalibrated;
//    }
//
//    public boolean isCalibrated() {
//        return isCalibrated;
//    }
//
//    public String getCalibratedDateString() {
//        return calibratedDateString;
//    }
//
//    private int cal_crc16_gen(byte[] buffer, int start, int end) {
//        // CRC parameters for calibration body
//        int CRC16_POLY_CCITT = 0x1021;
//        int CRC16_INIT_VALUE = 0x0000;
//        int CRC16_FINAL_XOR  = 0x0000;
//
//        int crc = CRC16_INIT_VALUE;
//
//        for (int i=start; i<=end; i++) {
//            int buff = buffer[i];   // get the next byte, nad let it sign extend
//            buff &= 0xFF;    // remove any sign extension
//            crc = crc ^ (buff << 8);
//            for (int bit=0; bit<8; bit++) {
//                if ((crc & 0x8000) != 0) {
//                    crc = (crc << 1) ^ CRC16_POLY_CCITT;
//                }
//                else {
//                    crc = (crc << 1);
//                }
//                crc &= 0xFFFF;    // ensure CRC is limited to 16 bit
//            }
//        }
//        crc = crc ^ CRC16_FINAL_XOR;
//        crc &= 0xFFFF;   // just to be sure (shouldn't be necessary)
//        //print("Resulting CRC = 0x{:04X}".format(crc))
//        Log.i(TAG, String.format("PJH - cal_crc16_gen: %04X", crc));
//
//        return (crc);
//    }
//
//    private int cal_crc8_gen(byte[] buffer, int start, int end) {
//        // CRC parameters for main calibration data header
//        int CRC8_POLY_CCITT = 0x8D;
//        int CRC8_INIT_VALUE = 0x00;
//        int CRC8_FINAL_XOR  = 0x00;
//        //print("Generating 8bit CRC of", len(buffer), "bytes of data")
//        int crc = CRC8_INIT_VALUE;
//
//        for (int i=start; i<=end; i++) {
//            int buff = buffer[i];   // get the next byte, nad let it sign extend
//            buff &= 0xFF;    // remove any sign extension
//            crc = crc ^ buff;
//            for (int bit=0; bit<8; bit++) {
//                if ((crc & 0x80) != 0) {
//                    crc = (crc << 1) ^ CRC8_POLY_CCITT;
//                }
//                else {
//                    crc = (crc << 1);
//                }
//                crc &= 0xFF;   // ensure CRC is limited to 8 bit
//            }
//        }
//        crc = crc ^ CRC8_FINAL_XOR;
//        crc &= 0xFF;   // just to be sure (shouldn't be necessary)
//        //print("Resulting CRC = 0x{:02X}".format(crc))
//        Log.i(TAG, String.format("PJH - cal_crc8_gen: %02X", crc));
//
//        return (crc);
//    }
//
////    public native String calibrateAllData(String input_data);
//
//    /**
//     * Go through all gatt services avaliable
//     * @param gattServices
//     *
//     * TODO - currently highly inefficient, can improve speed of app by fixing this
//     * TODO - Fix Calibration speed here
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
////            if (gattService.getUuid().toString().equals(SampleGattAttributes.PRIMARY_SERVICE_CHARACTERISTICS)) {
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
////                Log.e(TAG, "Gatt service is: " + gattService.getUuid().toString());
//            //look for the device ID
//            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
//                charas.add(gattCharacteristic);
//                HashMap<String, String> currentCharaData = new HashMap<>();
//                uuid = gattCharacteristic.getUuid().toString();
//                currentCharaData.put(
//                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString)
//                );
//                currentCharaData.put(LIST_UUID, uuid);
//                gattCharacteristicGroupData.add(currentCharaData);
//
////                        Log.d(TAG, "Gatt characteristic is: " + gattCharacteristic.getUuid().toString());
//                if (gattCharacteristic.getUuid() != null) {
//                    if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.CALIBRATION_INDEX)) {
////                                Log.e(TAG, "Reading calibration index");
//                        Operation getCalibrationIndex = new Operation(gattService, gattCharacteristic, OPERATION_READ);
//                        try {
//                            request(getCalibrationIndex);
//                        } catch (Exception e) {
//                            Log.e(TAG, "Exception thrown for requesting operation");
//                        }
//                    } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.CALIBRATION_DATA)) {
////                                Log.e(TAG, "Reading calibration data");
//                        Operation getCalibrationData = new Operation(gattService, gattCharacteristic, OPERATION_READ);
//                        try {
//                            request(getCalibrationData);
//                        } catch (Exception e) {
//                            Log.e(TAG, "Exception thrown for requesting operation");
//                        }
//                    } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.DEVICE_ADDRESS)) {
////                                Log.e(TAG, "Reading device address");
////                        if (caliDataCollected) {
////
//                        Operation getAddressData = new Operation(gattService, gattCharacteristic, OPERATION_READ);
//                        try {
//                            request(getAddressData);
//                        } catch (Exception e) {
//                            Log.e(TAG, "Exception thrown for requesting operation");
//                        }
//                    }
////                    }
//                } else {
////                            Log.e(TAG, "gatt characteristic uuid is null");
//                }
//
//            }
////            } else {
////                Log.e(TAG, "Something else: ");
////            }
//        }
//    }
//
//    /**
//     *
//     * @param savedInstanceState the data most recently supplied in {@link #onSaveInstanceState}.
//     *
//     */
//    @Override
//    public void onRestoreInstanceState(Bundle savedInstanceState) {
//        //save the address and name of the device last connected to so when we come back to it we can reconnect
//        //reassign stuff?
////        blackProbeTxt.setText("Probe: " + mDeviceName);
//        updateConnectionState(mDeviceConnectionStatus);
//        Log.e(TAG, "Saved instance:  " + savedInstanceState);
//    }
//
//    /**
//     *
//     * @param outState Bundle in which to place your saved state.
//     *
//     */
//    @Override
//    public void onSaveInstanceState(Bundle outState) {
//        outState.putString(NAME_STATE_KEY, mDeviceName);
//        outState.putString(ADDRESS_STATE_KEY, mDeviceAddress);
//        outState.putString(EXTRA_CONNECTION_STATUS, mDeviceConnectionStatus);
//
//        Log.e(TAG, "Saved Device info: " + outState);
//
//        super.onSaveInstanceState(outState);
//    }
//
//    /**
//     * when the activity is resumed it needs to register the receiver and reset probe details
//     */
//    @Override
//    protected void onResume() {
//        super.onResume();
//        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
//        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
//
//        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
//        if (mBluetoothLeService != null) {
//            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
//            Log.d(TAG, "Connection request result=" + result);
//        }
//
//        final Intent intent = getIntent();
//        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
//        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
//        mDeviceConnectionStatus = intent.getStringExtra(EXTRA_CONNECTION_STATUS);
//
//        if (!caliDataCollected && Globals.enableCalibration) { //if we interrupted the calibration process before it was finished
//            resetQueue();
//            calibrationIndexNum = 0;
//            updateConnectionState("Disconnected");
//        } else {
//            if (mDeviceConnectionStatus != null) {
//                updateConnectionState(mDeviceConnectionStatus);
//                if (mDeviceConnectionStatus.equals("Connected")) {
//                    mConnected = true;
//                }
//            } else {
//                Log.e(TAG, "ERROR DEVICE CONNECTION STATUS IS NULL");
//            }
//        }
//    }
//
//    /**
//     * When activity is paused the app needs to unregister the gattUpdateReceiver
//     */
//    @Override
//    protected void onPause() {
//        super.onPause();
//        unregisterReceiver(mGattUpdateReceiver);
//    }
//
//    /**
//     * Update probe connection state based on resourceID
//     * @param resourceId, should either be "Connected" or "Disconnected"
//     *
//     * TODO - should be made an enum so that spelling wont be an issue
//     */
//    private void updateConnectionState(final String resourceId) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
////                Log.e(TAG, "UPDATING CONNECTION STATE");
//                if (resourceId.equals("Connected")) {
//                    mDeviceConnectionStatus = "Connected";
//                    //if we want to collect calibration data, and haven't done so yet, or started
//                    if (Globals.enableCalibration && !caliDataCollected && !started) {
//                        boolean newProbe = true;
//
//                        try {
//                            if (!(mDeviceName == null && mDeviceName.isEmpty())) {
//                                Log.d(TAG, "------------ANNA---------------");
//                                Log.d(TAG, "Device (from mDeviceName): " + mDeviceName);
//
//                                for (int i = 0; i < CalibrationHelper.getKnownCal().length; i++) {
//                                    if (Integer.valueOf(mDeviceName) == CalibrationHelper.getKnownCal()[i]) {
//                                        //Probe calibration data is already saved
//                                        newProbe = false;
//                                    }
//                                }
//                            } else if (!(EXTRA_DEVICE_NAME == null && EXTRA_DEVICE_NAME.isEmpty())) {
//                                Log.d(TAG, "------------ANNA---------------");
//                                Log.d(TAG, "Device (from EXTRA_DEVICE_NAME): " + mDeviceName);
//
//                                for (int i = 0; i < CalibrationHelper.getKnownCal().length; i++) {
//                                    if (Integer.valueOf(mDeviceName) == CalibrationHelper.getKnownCal()[i]) {
//                                        //Probe calibration data is already saved
//                                        newProbe = false;
//                                    }
//                                }
//                            } else {
//
//                            }
//                        } catch (Exception e) {
//                            Log.e(TAG, "Exception thrown checking probe calibration storage status: " + e);
//                        }
//                        //check whether we have the probe or not
//                        if (newProbe) {
//                            started = true;
//                            indexNext();
//                        } else {
//                            //TODO use calibration data based on probes name
//                        }
//                    }
//                } else if (resourceId.equals("Disconnected")) {
//                    registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
//                    if (mBluetoothLeService != null) {
//                        final boolean result = mBluetoothLeService.connect(mDeviceAddress);
//                        Log.d(TAG, "Connection request result=" + result);
//                    }
//                    mDeviceConnectionStatus = "Disconnected";
//                } else {
//                    mDeviceConnectionStatus = "Disconnected";
//                }
//            }
//        });
//    }
//
//    private void displayData(String data) {
//        if (data != null) {
//            //display the data, probably not needed in this section
//        }
//    }
//
//    /**
//     * Go to survey options class on button click
//     * @param v
//     */
//    public void operatorIDBtnClick(View v) {
//        Intent intent = new Intent(this, SurveyOptionsActivity.class);
//        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_NAME, mDeviceName);
//        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
//        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_CONNECTION, mDeviceConnectionStatus);
//        startActivity(intent);
//    }
//
//    /**
//     * Go to survey options class on button click
//     * @param v
//     */
//    public void holeIDBtnClick(View v) {
//        Intent intent = new Intent(this, SurveyOptionsActivity.class);
//        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_NAME, mDeviceName);
//        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
//        startActivity(intent);
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
//        // Handle item selection
//        if (item.getItemId() == R.id.select_probe) {
//            Intent intent = new Intent(this, DeviceScanActivity.class);
//            if (mDeviceName != null && mDeviceAddress != null) {
//                intent.putExtra(DeviceScanActivity.EXTRA_DEVICE_NAME, mDeviceName);
//                intent.putExtra(DeviceScanActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
//                intent.putExtra(DeviceScanActivity.EXTRA_DEVICE_NAME, mDeviceName);
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
//    /**
//     * If probe connected and clicked on go to survey popup activity (InialisePopupActivity)
//     * @param v
//     */
//    public void BlackProbeBtnClick(View v) {
//        if (mConnected) {
//            Intent intent = new Intent(this, InitalisePopupActivity.class);
//            Log.d(TAG, "Device Name: " + mDeviceName + ", Device Address: " + mDeviceAddress);
//            intent.putExtra(InitalisePopupActivity.EXTRA_DEVICE_NAME, mDeviceName);
//            intent.putExtra(InitalisePopupActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
//            startActivity(intent);
//        } else {
//            Log.e(TAG, "Probe is disconnected");
//        }
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
//     * Calibrate probe
//     * @param view
//     */
//    public void calibrate(View view) {
//        //get calibration data + write to calibration index then read again
//        displayGattServices(mBluetoothLeService.getSupportedGattServices()); //read calibation index then calibration data
//    }
//
//
//    /**
//     * Get next calibration index value
//     * @param view
//     */
//    public void index(View view) {
//        if (Globals.enableCalibration && !CalibrationMatrixCreated) {
//            boolean status = false;
//            do {
//                //(byte) calibrationIndexNum
//                status = mBluetoothLeService.writeToCalibrationIndex((byte) 00); //TODO make this a variable input
////            Log.e(TAG, "Status of write: " + status);
//                try {
//                    Thread.sleep(1000);
//                } catch (Exception e) {
//                    Log.e(TAG, "Could not sleep" + e);
//                }
//                if (status) {
////                dataToBeRead = 0;
//                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
//
//                } else {
//                    try {
//                        if (mDeviceConnectionStatus.equals("Connected")) {
//                            updateConnectionState("Connected");
//                        } else {
//                            Log.e(TAG, "Device disconnected");
//                            updateConnectionState("Disconnected");
//                        }
//                    } catch (Exception e) {
//                        Log.e(TAG, "Error setting connection state: " + e);
//                    }
//                }
//                new CountDownTimer(700, 1) { //definetly inefficicent, but if it works dont touch it lol
//
//                    public void onTick(long millisUntilFinished) {
//                        //                mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
//                    }
//
//                    public void onFinish() {
//                        //                    dataToBeRead = 0;
//                        //                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
//                    }
//                }.start();
//
//                new CountDownTimer(3000, 1) { //definetly inefficicent, but if it works dont touch it lol
//
//                    public void onTick(long millisUntilFinished) {
//                        //                mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
//                    }
//
//                    public void onFinish() {
//                        //                    dataToBeRead = 0;
//                        //                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
//                    }
//                }.start();
//
//            } while (!status);
//        }
//    }
//
//    public void indexNext() {
//        if (Globals.enableCalibration) {
//            //Set the status indicator to calibrating
////            blackProbeStatusImg.setImageResource(R.drawable.calibrating);
////            blackProbeStatusTxt.setText("Calibrating");
//
//            boolean status = false; //default the status to false
//            do {
//                status = mBluetoothLeService.writeToCalibrationIndex((byte) calibrationIndexNum); //TODO make this a variable input
//                Log.e(TAG, "Status of writing: " + calibrationIndexNum + ": " + status);
//
//                if (writeCollectValue == 1) {
//                    calibrationFirstValue = calibrationIndexNum;
//                } else if (writeCollectValue == 2) {
//                    calibrationSecondValue = calibrationIndexNum;
//                } else if (writeCollectValue == 3) {
//                    calibrationThirdValue = calibrationIndexNum;
//                }
//                writeCollectValue++;
//                if (writeCollectValue >= 3) {
//                    writeCollectValue = 0;
//                }
//
//                if (((calibrationFirstValue + calibrationSecondValue + calibrationThirdValue) / 3) - calibrationIndexNum > 2) {
//                    Log.e(TAG, "------------------ANNA-----------------");
//                    Log.e(TAG, "Restarting calibration as data was invalid");
//                    caliData.clear();
//                    calibrationIndexNum = 0;
//                    calibrationFirstValue = 0;
//                    calibrationSecondValue = 0;
//                    calibrationThirdValue = 3;
//                }
//
//                try {
//                    Thread.sleep(500);
//                } catch (Exception e) {
//                    Log.e(TAG, "Could not sleep" + e);
//                }
//                if (status) {
//                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
//                } else {
//                    try {
//                        if (mDeviceConnectionStatus.equals("Connected")) {
//                            updateConnectionState("Connected");
//                        } else {
//                            Log.e(TAG, "Device disconnected");
//                            updateConnectionState("Disconnected");
//                        }
//                    } catch (Exception e) {
//                        Log.e(TAG, "Error setting connection state: " + e);
//                    }
//                }
//                new CountDownTimer(700, 1) { //definetly inefficicent, but if it works dont touch it lol
//
//                    public void onTick(long millisUntilFinished) {
//                        //                mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
//                    }
//
//                    public void onFinish() {
//                        //                    dataToBeRead = 0;
//                        //                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
//                    }
//                }.start();
//
//                new CountDownTimer(3000, 1) { //definetly inefficicent, but if it works dont touch it lol
//
//                    public void onTick(long millisUntilFinished) {
//                        //                mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
//                    }
//
//                    public void onFinish() {
//                        //                    dataToBeRead = 0;
//                        //                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
//                    }
//                }.start();
//            } while (!status);
//        }
//    }
//}

////////////////////////////////////////////////////////////////////////////////
/**
 * \file CalibrationActivity
 * \brief Calibrates the probe and saved data to calibration helper before returning probe to main activity
 * \author Anna Pedersen
 * \date Created: 10/10/2024
 *
 */
package com.work.libtest;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

//import com.work.libtest.databinding.ActivityMainBinding;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import static com.work.libtest.CalibrationHelper.binCalData;
import static com.work.libtest.Globals.caliDataCollected;
import static com.work.libtest.Operation.OPERATION_WRITE;
import static com.work.libtest.Operation.OPERATION_READ;
import static com.work.libtest.Operation.OPERATION_NOTIFY;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TimeZone;

public class CalibrationActivity extends AppCompatActivity {
    private static final String TAG = CalibrationActivity.class.getSimpleName(); //activity name for logging

    //TODO - these are probably codes which can be added to a globals/constants file
    private static final int REQ_CODE_ENABLE_BT = 1;
    private static final int REQ_CODE_SCAN_ACTIVITY = 2;
    private static final int REQ_CODE_ACCESS_LOC1 = 3;
    private static final int REQ_CODE_ACCESS_LOC2 = 4;
    private static final long CONNECT_TIMEOUT = 10000;

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
    private boolean haveSuitableProbeConnected = false;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private Menu menu;
    private BleService bleService;

    public int calibrationIndexNum = 00;
    boolean calibrated = false;
    boolean toBeCalibrated = true; //do we even want to gather the calibration matrix
    private String bleDeviceName, bleDeviceAddress;

    private String mDeviceName;
    private String mDeviceAddress;
    private String mDeviceConnectionStatus;
    private String mMode;
    private String modifiedDateString = "No calibration data";
    private String calibratedDateString = "No calibration data";

    private boolean isCalibrated = false;
    final int NUM_CAL_PARAMS_EXPECTED_DURING_PARSING = 38;
    private int binCalData_size = 0;
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

    boolean started = false;

    private enum StateConnection {DISCONNECTED, CONNECTING, DISCOVERING, CONNECTED, DISCONNECTING}

    private StateConnection stateConnection;

    private enum StateApp {STARTING_SERVICE, REQUEST_PERMISSION, ENABLING_BLUETOOTH, RUNNING}

    private StateApp stateApp;

    int writeCollectValue = 1;
    int calibrationFirstValue = 0;
    int calibrationSecondValue = 0;
    int calibrationThirdValue = 0;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

//    private BluetoothLeService mBluetoothLeService;
    private BluetoothGatt mBluetoothGatt;

    private Handler handler;
    private Handler connectTimeoutHandler;

    public static ArrayList<Survey> surveys = new ArrayList<Survey>();
    public static int surveySize = 0;
    public static Preferences preferences = new Preferences();
    public static String connectedDeviceName = "No Probe Selected";

    public static ArrayList<String> caliData = new ArrayList<>();

    private BluetoothGattCharacteristic CORE_SHOT_CHARACTERISTIC;
    private BluetoothGattCharacteristic BORE_SHOT_CHARACTERISTIC;

    private long startTime = 0; //for timer
    public int seconds;
    private int starttime = 0;
    boolean calibration_status = false;

    public static int surveyNum = 0;

    private boolean CalibrationMatrixCreated = false;

    private final Queue<Operation> operations = new LinkedList<>();
    private Operation currentOp;
    private static boolean operationOngoing = false;

    /******************************************************************************
     * Methods for handling life cycle events of the activity
     */

    /**
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     *    TODO - ShowAlertDialog -> could be used to show dialog boxes
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get data about probe passed in from last activity
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        mDeviceConnectionStatus = intent.getStringExtra(EXTRA_CONNECTION_STATUS);
        Log.e(TAG, "passed in name: " + intent.getStringExtra(EXTRA_DEVICE_NAME) + "Passed in address: " + intent.getStringExtra(EXTRA_DEVICE_ADDRESS) + "Passed in connection: " + intent.getStringExtra(EXTRA_CONNECTION_STATUS)); //check that passed in data is correct by outputing to the log monitor

        try {
            setContentView(R.layout.activity_calibration);

            stateConnection = StateConnection.DISCONNECTED;                                             //Initial stateConnection when app starts
            stateApp = StateApp.STARTING_SERVICE;                                                       //Are going to start the BleService service

            Log.i(TAG, "------ about to request FINE permission --------");
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { //Check whether we have location permission, required to scan
                stateApp = StateApp.REQUEST_PERMISSION;                                                 //Are requesting Location permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_CODE_ACCESS_LOC1); //Request fine location permission
            }
            if (stateApp == StateApp.STARTING_SERVICE) {                                                //Only start BleService if we already have location permission
                Intent bleServiceIntent = new Intent(this, BleService.class);             //Create Intent to start the BleService
                //^previously called gattServiceIntent, if an issue pops up may be this.
                this.bindService(bleServiceIntent, bleServiceConnection, BIND_AUTO_CREATE);             //Create and bind the new service with bleServiceConnection object that handles service connect and disconnect
            }
            connectTimeoutHandler = new Handler(Looper.getMainLooper());                                //Create a handler for a delayed runnable that will stop the connection attempt after a timeout
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown: " + e);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            registerReceiver(bleServiceReceiver, bleServiceIntentFilter());
            if (bleService != null && !bleService.isBluetoothRadioEnabled()) {
                if (stateApp == StateApp.RUNNING) {
                    stateApp = StateApp.ENABLING_BLUETOOTH;
                    stateConnection = StateConnection.DISCONNECTED;
//                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) { //not 100% sure this permission check is actually needed
//                        startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQ_CODE_ENABLE_BT);
//                    }
                    Log.i(TAG, "Requesting user to enable Bluetooth Radio");
                }
            }
            if ((bleDeviceName != null) && (bleDeviceAddress != null) && bleService.isCalibrated()) {
                //attempt a reconnection
                stateConnection = StateConnection.CONNECTING;
                connectWithAddress(bleDeviceAddress);
            }
            updateConnectionState();
        } catch (Exception e) {
            Log.e(TAG, "Exception caught: " + e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(bleServiceReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Exception caught: " + e);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        bleService.setProbeIdle();

        if (stateApp != StateApp.REQUEST_PERMISSION) {
            unbindService(bleServiceConnection);
        }
    }

    /******************************************************************************

     /******************************************************************************
     * Methods for handling menu creation and operation
     */

    //TODO - make an actual menu for this activity
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main_activity, menu);
        this.menu = menu;
        return true;
    }

    //TODO
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    /******************************************************************************

     /************************************************************************************
     * Callback methods for handling Service connection events and activity result events
     */

    private final ServiceConnection bleServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                Log.i(TAG, "BLEService connected");
                BleService.LocalBinder binder = (BleService.LocalBinder) service;
                bleService = binder.getService();
                if (bleService.isBluetoothRadioEnabled()) {
                    stateApp = stateApp.RUNNING;
                } else {
                    stateApp = StateApp.ENABLING_BLUETOOTH;
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    Log.i(TAG, "Requesting user enable bluetooth");
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception thrown: " + e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "Bluetooth service disconnected");
            bleService = null;
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
            case REQ_CODE_ENABLE_BT:
            {
                if (resultCode == Activity.RESULT_OK) {
                    stateApp = StateApp.RUNNING;
                } else {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQ_CODE_ENABLE_BT);
                    Log.i(TAG, "requesting user to turn on bluetooth again!");
                }
                break;
            }
            case REQ_CODE_SCAN_ACTIVITY:
            {
                //showAlert.dismiss();
                if (resultCode == Activity.RESULT_OK) {
                    stateApp = StateApp.RUNNING;
                    bleDeviceAddress = intent.getStringExtra(DeviceScanActivity.EXTRA_DEVICE_ADDRESS);
                    bleDeviceName = intent.getStringExtra(DeviceScanActivity.EXTRA_DEVICE_NAME);

                    if (bleDeviceName != null) {
                        //set text to be the probe name
                    } else {
                        //set text to be "unknown address"
                    }
                    if (bleDeviceAddress != null) {
                        stateConnection = StateConnection.DISCONNECTED;
                    } else {
                        stateConnection = StateConnection.CONNECTING;
                        connectWithAddress(bleDeviceAddress);
                    }
                } else {
                    stateConnection = StateConnection.DISCONNECTED;
                }
                updateConnectionState();
                break;
            }
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Callback for permission requests (new feature of Android Marshmallow requires runtime permission requests)
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i(TAG, "PJH - onRequestPermissionsResult");

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {                                 //See if location permission was granted
            Log.i(TAG, "PJH - Location permission granted");
            stateApp = StateApp.STARTING_SERVICE;                                                   //Are going to start the BleService service
            Intent bleServiceIntent = new Intent(this, BleService.class);             //Create Intent to start the BleService
            this.bindService(bleServiceIntent, bleServiceConnection, BIND_AUTO_CREATE);             //Create and bind the new service to bleServiceConnection object that handles service connect and disconnect
        } else if (requestCode == REQ_CODE_ACCESS_LOC1) {                                             //Not granted so see if first refusal and need to ask again
//            showAlert.showLocationPermissionDialog(new Runnable() {                                 //Show the AlertDialog that scan cannot be performed without permission
//                @TargetApi(Build.VERSION_CODES.M)
//                @Override
//                public void run() {                                                                 //Runnable to execute when Continue button pressed
//                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_CODE_ACCESS_LOC2); //Ask for location permission again
//                }
//            });
        } else {
            //Permission refused twice so send user to settings
            Log.i(TAG, "Location permission NOT granted");
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);               //Create Intent to open the app settings page
            Uri uri = Uri.fromParts("package", getPackageName(), null);            //Identify the package for the settings
            intent.setData(uri);                                                                    //Add the package to the Intent
            startActivity(intent);                                                                  //Start the settings activity
        }
    }
    /******************************************************************************

     /******************************************************************************
     * Methods for handling intents
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

    private final BroadcastReceiver bleServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case BleService.ACTION_BLE_CONNECTED:
                {
                    Log.d(TAG, "Received intent ACTION_BLE_CONNECTED");
                    stateConnection = StateConnection.CONNECTING;
                    updateConnectionState();
                    break;
                }
                case BleService.ACTION_BLE_DISCONNECTED:
                {
                    Log.e(TAG, "Recieved Intent ACTION_BLE_DISCONNECTED");
                    stateConnection = StateConnection.DISCONNECTED;
                    //attempt to reconnect automatically:
                    if ((bleDeviceName != null) && (bleDeviceAddress != null) && bleService.isCalibrated()) {
                        stateConnection = StateConnection.CONNECTING;
                        connectWithAddress(bleDeviceAddress);
                    }
                    updateConnectionState();
                    break;
                }
                case BleService.ACTION_BLE_DISCOVERY_DONE:
                {
                    Log.d(TAG, "Received intent for ACTION_BLE_DISCOVERY_DONE");
                    connectTimeoutHandler.removeCallbacks(abandonConnectionAttempt); //dont know what this does lol
                    stateConnection = StateConnection.CONNECTED;
                    updateConnectionState();

                    //Dont know what this is:
                    Log.i(TAG, "Requesting Ezy config");
                    bleService.requestEzyConfig();
                    break;
                }
                case BleService.ACTION_BLE_DISCOVERY_FAILED:
                {
                    Log.d(TAG, "Received intent ACTION_BLE_DISCOVERY_FAILED");
                    stateConnection = StateConnection.DISCONNECTING;
                    connectTimeoutHandler.removeCallbacks(abandonConnectionAttempt);
                    bleService.disconnectBle();
                    updateConnectionState();
                    break;
                }
                case BleService.ACTION_BLE_CONFIG_READY:
                {
                    Log.d(TAG, "Received intent ACTION_BLE_CONFIG_READY");
                    bleService.parseBinaryCalibration();
                    bleService.setNotifications(true);
                    haveSuitableProbeConnected = true; //dont think i actually need this
                    break;
                }
                case BleService.ACTION_BLE_FETCH_CAL:
                {
                    Log.e(TAG, "Received intent ACTION_BLE_FETCH_CAL");
                    //Show "fetching calibration: on calibration screen
                    break;
                }
                case BleService.ACTION_BLE_NEW_DATA_RECEIVED:
                {
                    Log.i(TAG, "Received intent ACTION_BLE_NEW_DATA_RECEIVED");
//                    processNewReading(); //may not need;
                    break;
                }
                default:
                {
                    Log.w(TAG, "Received intent with invalid action: " + action);
                }
            }
        }
    };
    /*************************************************************************************

     /******************************************************************************
     * Methods for scanning, connecting and showing event driven dialogs
     */

    // ----------------------------------------------------------------------------------------------------------------
    // Start the com.work.libtest.BleScanActivity that scans for available Bluetooth devices and lets the user select one
//    private void startBleScanActivity() {
//        try {
//            bleService.invalidateCalibration();  // to force a full connect and reread of the calibration data
//            if (stateApp == StateApp.RUNNING) {                                                     //Only do a scan if we got through startup (permission granted, service started, Bluetooth enabled)
//                stateConnection = StateConnection.DISCONNECTING;                                    //Are disconnecting prior to doing a scan
//                haveSuitableProbeConnected = false;
//                bleService.disconnectBle();                                                         //Disconnect an existing Bluetooth connection or cancel a connection attempt
//                final Intent bleScanActivityIntent = new Intent(CalibrationActivity.this, com.work.libtest.BleScanActivity.class); //Create Intent to start the com.work.libtest.BleScanActivity
//                startActivityForResult(bleScanActivityIntent, REQ_CODE_SCAN_ACTIVITY);              //Start the com.work.libtest.BleScanActivity
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
                if (stateConnection == StateConnection.CONNECTING) {                                //See if still trying to connect
                    stateConnection = StateConnection.DISCONNECTING;                                //Are now disconnecting
                    bleService.disconnectBle();                                                     //Stop the Bluetooth connection attempt in progress
                    updateConnectionState();                                                        //Update the screen and menus
                }
            } catch (Exception e) {
                Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
            }
        }
    };
    /*******************************************************************************************

     /******************************************************************************
     * Methods for updating connecting state on the screen
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

    /*******************************************************************************************

    /******************************************************************************
     * Custom queuing system
     */

//    public synchronized void request(Operation operation) {
//        try {
//            operations.add(operation);
//            if (currentOp == null) {
//                currentOp = operations.poll();
//                performOperation();
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Error thrown while requesting an operation");
//        }
//    }

    /**
     * Excecutes whenever an operation is finished
     */
//    public synchronized void operationCompleted() {
//        currentOp = null;
//        if (operations.peek() != null) {
//            currentOp = operations.poll();
//            performOperation();
//        } else {
//            if (!caliDataCollected && !CalibrationMatrixCreated) {
//
//                try {
//                    if (!caliData.get(caliData.size()-1).equals("-1:-1:-1:-1:-1:-1:-1:-1:-1:-1:-1:-1:-1:-1:-1:-1:")) {
//                        calibrationIndexNum++;
//                        indexNext();
//                    } else {
////                        print original
//                        Log.e(TAG, "All Calibration Data: ");
//                        for (int i = 0; i < caliData.size(); i++) {
//                            Log.e(TAG, caliData.get(i));
//                        }
//
//                        Set<String> s = new LinkedHashSet<String>(caliData);
//                        Log.e(TAG, s.toString());
//                        List<String> list = new ArrayList<>(s);
//                        list.remove(list.size()-1);
//
//                        //print
//                        Log.e(TAG, "All Calibration Data: ");
//                        for (int i = 0; i < list.size(); i++) {
//                            Log.e(TAG, list.get(i));
////                            CalibrationHelper.
////                            list.get(i);
//                        }
//                        caliData = (ArrayList<String>) list;
//                        caliDataCollected = true;
//                        updateConnectionState("Connected");
////                        blackProbeStatusImg.setImageResource(R.drawable.ready);
////                        blackProbeStatusTxt.setText("Connected");
//                    }
//                } catch (Exception e) {
//                    Log.e(TAG, "Exception thrown: " + e);
//                }
//            }
//        }
//    }

    /**
     * Perform requested operation
     */
//    public void performOperation() {
//        if (currentOp != null) {
//            if (currentOp.getService() != null && currentOp.getCharacteristic() != null && currentOp.getAction() != 0) {
//                switch (currentOp.getAction()) {
//                    case OPERATION_WRITE:
//                        mBluetoothLeService.writeData(currentOp.getCharacteristic());
//                        break;
//                    case OPERATION_READ:
//                        mBluetoothLeService.readCharacteristic(currentOp.getCharacteristic());
//                    case OPERATION_NOTIFY:
//
//                        break;
//                    default:
//                        break;
//                }
//            }
//        }
//    }

    /**
     * reset all operations
     */
//    public void resetQueue() {
//        operations.clear();
//        currentOp = null;
//    }
//
//    private interface MessageConstants {
//        public static final int MESSAGE_READ = 0;
//        public static final int MESSAGE_WRITE = 1;
//        public static final int MESSAGE_TOAST = 2;
//    }
//
//    private void clearUI() {
//    }
//
//    private class ConnectedThread extends Thread {
//        private final BluetoothSocket mmSocket;
//        private final InputStream mmInStream;
//        private final OutputStream mmOutStream;
//        private byte[] mmBuffer;
//
//        /**
//         * @param socket - Bluetooth socket
//         */
//        public ConnectedThread(BluetoothSocket socket) {
//            mmSocket = socket;
//            InputStream tmpIn = null;
//            OutputStream tmpOut = null;
//
//            try {
//                tmpIn = socket.getInputStream();
//            } catch (IOException e) {
//                Log.e(TAG, "Error occured when creating input stream", e);
//            }
//            try {
//                tmpOut = socket.getOutputStream();
//            } catch (IOException e) {
//                Log.e(TAG, "Error occured when creating output stream", e);
//            }
//
//            mmInStream = tmpIn;
//            mmOutStream = tmpOut;
//        }
//
//        public void run() {
//            mmBuffer = new byte[1024];
//            int numBytes;
//
//            while (true) {
//                try {
//                    numBytes = mmInStream.read(mmBuffer);
//                    Message readMsg = handler.obtainMessage(
//                            MessageConstants.MESSAGE_READ, numBytes,-1, mmBuffer
//                    );
//                    readMsg.sendToTarget();
//                } catch (IOException e) {
//                    Log.d(TAG, "Input stream was disconnected", e);
//                    break;
//                }
//            }
//        }
//    }

    //CALLBACK
//    private double getDouble(int offset) {
//        // concatenate the bytes to an 8 byte long (assume the endianness is correct)
//        double dValue = 0;
//        try {
//            long lValue = 0;
//            for (int i = 0; i < 8; i++) {
//                //for (int i = 7; i >= 0; i--) {
//                lValue = (lValue << 8) + (((long) binCalData[offset + i]) & 0xFF);
//            }
//            dValue = Double.longBitsToDouble(lValue);
//            //Log.i(TAG, String.format("PJH - getDouble: lValue=0x%08X dValue=%f", lValue, dValue));
//
//        } catch (Exception e) {
//            Log.e(TAG, "Exception caught in " + e);
//        }
//        return(dValue);
//    }
//
//    public boolean parseBinaryCalibration() {
//        boolean result = false;
//        int coeffs_found = 0; // should be NUM_CAL_PARAMS_EXPECTED_DURING_PARSING (38) when finished
//
//        Log.w(TAG, "Beginning parse calibration protocol");
//        try {
//            isCalibrated = false;
//            int p = 0; //base 0, pointer into cal binary
//
//            // dump the header for debugging
//            Log.i(TAG, String.format("- calPacket: %02X %02X %02X %02X %02X %02X %02X %02X", binCalData[0], binCalData[1], binCalData[2], binCalData[3], binCalData[4], binCalData[5], binCalData[6], binCalData[7]));
//            Log.i(TAG, String.format("-            %02X %02X %02X %02X %02X %02X %02X %02X", binCalData[8], binCalData[9], binCalData[10], binCalData[11], binCalData[12], binCalData[13], binCalData[14], binCalData[15]));
//
//            //CALLBACK
//            // first check if the header (of the header) is valid (first byte is 0x01 and high byte of length is 0x00)
//            if (((((int) binCalData[0]) & 0xFF) == 0x01) && ((((int) binCalData[1]) & 0xFF) == 0x00)) {
//                // use low byte of header length to determine the offset of the 8bit CRC
//                // (header length includes CRC, but not blk id or length)
//                // plus 3 to give total length of header, less 1 to convert nth byte to base 0 offset
//                int crc_offset = (((int) binCalData[2]) & 0xFF) + 3 - 1;   // this is also the number of bytes preceeding the crc byte
//
//                // the 8bit header CRC spans all bytes in this block, excluding the CRC itself
//                int crc8 = cal_crc8_gen(binCalData, 0, crc_offset-1);     // (data[], start, end)
//
//                if (crc8 == (((int) binCalData[crc_offset]) & 0xFF)) {   // beware sign extension
//                    // ok, the crc of the header is correct, so we can proceed
//
//                    // get the 'modified' date out of the header block (it is in a fixed location)
//                    int m_date_len = ((int)binCalData[5]) & 0xFF;   // should be 8 for current calibrations, or maybe 4 for archaic probes
//                    if ((m_date_len == 4) || (m_date_len==8)) {
//                        long unix_tick = 0;
//                        for (int i = (6 + m_date_len - 1); i >= 6; i--) {  // little endian
//                            unix_tick = (unix_tick << 8) + (((int)binCalData[i]) & 0xFF);
//                        }
//
//                        Date date = new Date(unix_tick * 1000L);
//                        SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
//                        jdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
//                        modifiedDateString = jdf.format(date);
//
//                        coeffs_found += 1;
//                        Log.i(TAG, String.format("PJH - parse - modified date = %s", modifiedDateString));
//                    }
//                    else
//                    {
//                        modifiedDateString = "not initialised";
//                    }
//
//                    // the calibrated date string will be retrieved later, from the main data block
//
//
//                    // now we want to check the crc of the actual calibration data
//                    //int start = (((int) binCalData[2]) & 0xFF) + 3 + 3;   // first byte after the block size
//                    //int total_len = ((((int) binCalData[3]) & 0xFF) << 8) + (((int) binCalData[4]) & 0xFF);
//                    //int end_plus_one = total_len - 2;
//                    //Log.i(TAG, String.format("PJH - start: %06X end+1: %06X", start, end_plus_one));
//                    //System.arraycopy(binCalData, start, buffer, 0, end_plus_one - start);    // (src,dst,len)
//                    //
//                    //int crc16 = cal_crc16_gen(buffer, end_plus_one - start);
//
//                    // the 16 bit CRC of the main data block starts AFTER the blk_id and blk_len
//                    // and includes the last byte before the actual CRC16
//                    // (so the main blk_id and blk_len are NOT covered by the CRC16 - this is a BUG,
//                    // but can't change it as we need to maintain compatibility with the ipod,
//                    // so will need to manually check those separately)
//                    int start = (((int) binCalData[2]) & 0xFF) + 3 + 3;   // offset of header byte of main data block
//                    int total_len = ((((int) binCalData[3]) & 0xFF) << 8) + (((int) binCalData[4]) & 0xFF);  // big endian
//                    binCalData_size = total_len;   // save the number of valid byte entries in binCalData
//                    coeffs_found += 1;
//                    int end = total_len - 2 - 1;   //  total len - 2 byte crc - 1 to convert len to offset
//                    Log.i(TAG, String.format("PJH - start: %06X end: %06X", start, end));
//                    int crc16 = cal_crc16_gen(binCalData, start, end);   // (data[], start, end)
//
//                    // get the crc16 from the binary cal data (from the last two bytes)
//                    int binCrc16 = ((((int) binCalData[total_len - 2]) & 0xFF) << 8) + (((int) binCalData[total_len - 1]) & 0xFF);  // big endian
//
//                    if (crc16 == binCrc16) {
//                        // both CRCs are good, so extract calibration coefficients
//                        Log.i(TAG, "PJH - parse - both calibration CRCs are good");
//                        p = crc_offset + 1;   // first byte of the main body
//
//                        // the following log entry shows the allocated size of binCalData which is not useful
//                        //Log.i(TAG, String.format("PJH - parse - binCalData length: 0x%04X", binCalData.length));
//
//                        if (((((int) binCalData[p]) & 0xFF) == 0x10)) {   // this blk_id is not covered by CRC so check it here
//                            // the subsequent 2 byte blk_len is not used in this parsing, so ignore it for now
//                            // TODO - we really should check it though, as don't know how ipod would react if it was corrupted
//                            p += 3;  // point to cal record type
//                            int cal_record_type = (((int) binCalData[p]) & 0xFF);
//                            // cal_record_type is always 0x01 for the calibration parameters we are using
//                            // TODO - should check it is 0x01, just to be sure
//                            Log.i(TAG, String.format("PJH - parse @ 0x%04X - cal record type: 0x%02X", p, cal_record_type));
//                            p += 1;  // and advance to the first block of calibration parameters
//
//                            // Now we can finally start reading in the calibration parameters that we need
//                            // Each set of related parameters and in a separate block
//                            int hdr_id, blk_len, pTmp, flags;
//                            while (p < total_len) {
//                                // first two bytes of the block contain the hdr_id in the highest nibble, and blk_len in remaining three nibbles
//                                hdr_id = (((int) binCalData[p]) & 0xF0) >> 4;
//                                blk_len = (((((int) binCalData[p]) & 0x0F) << 8) + (((int) binCalData[p + 1]) & 0xFF)) & 0x0FFF;
//                                p += 2;
//                                Log.i(TAG, String.format("PJH - parse @ 0x%04X - hdr_id: 0x%02X, length: 0x%04X", p, hdr_id, blk_len));
//
//                                // p is pointing to the first byte of content in this calibration parameter block
//                                if (hdr_id == 0x00) {    // CRC block - ignore =============================================
//                                    // the CRC16 block is ALWAYS the last block in the calibration file,
//                                    // so that the CRC16 is the last two bytes of the calibration file
//
//                                    // we have already checked this CRC, so ignore it
//                                    p += blk_len;
//                                }
//                                else if (hdr_id == 0x01) {    // timestamp block =============================================
//                                    // extract the Calibrated Date
//
//                                    int c_date_len = blk_len;  // should be 8 for current calibrations, or maybe 4 for archaic probes
//                                    if ((c_date_len == 4) || (c_date_len == 8)) {
//                                        long unix_tick = 0;
//                                        for (int i = (p + c_date_len - 1); i >= (p); i--) {  // timestamp is little endian
//                                            unix_tick = (unix_tick << 8) + (((int)binCalData[i]) & 0xFF);
//                                        }
//
//                                        Date date = new Date(unix_tick * 1000L);
//                                        SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
//                                        jdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
//                                        calibratedDateString = jdf.format(date);
//
//                                        coeffs_found += 1;
//                                        Log.i(TAG, String.format("PJH - parse - calibrated date = %s", calibratedDateString));
//
//                                    }
//                                    else
//                                    {
//                                        calibratedDateString = "not initialised";
//                                    }
//
//                                    p += blk_len;
//                                }
//                                else if (hdr_id == 0x02) {    // accel block =============================================
//                                    pTmp = p;  // we don't want to mess up 'p'
//                                    // flags tells up what sub field exist in this block
//                                    flags = (((((int) binCalData[pTmp + 1]) & 0xFF) << 8) + (((int) binCalData[pTmp]) & 0xFF));  // little endian
//                                    pTmp += 2;  // skip over flags
//
//                                    if ((flags & 0x0001) != 0) {
//                                        // temperature data - skip it   TODO
//                                        // PJH - this relates to probes that are calibrated a two different temperatures
//                                        // none of the probes have calibration data that is temperature dependent
//                                        pTmp += 8;
//                                    }
//                                    if ((flags & 0x0004) != 0) {
//                                        // bit 2 is set, so first order terms are present - 3x3
//                                        // each of the 9 parameters are double precision floats
//                                        for (int r = 0; r < 3; r++) {
//                                            for (int c = 0; c < 3; c++) {
//                                                acc_B[r][c] = getDouble(pTmp);
//                                                Log.i(TAG, String.format("PJH - parse - acc_B[%d][%d] = %15.12f", r, c, acc_B[r][c]));
//                                                coeffs_found += 1;
//                                                pTmp += 8;
//                                            }
//                                        }
//                                    }
//                                    if ((flags & 0x0008) != 0) {
//                                        // bit 3 is set, so zero order terms are present
//
//                                        // HACK - the next byte tells us the depth of this 3 wide matrix
//                                        // Don't understand why this is here, but it is
//                                        // ASSUME it will be 2 and skip over it - YUK
//                                        pTmp+=1; // just assume this count is for the 3x2 matrix  TODO
//                                        // zero order terms - 3x2 - don't know why there are extra co-efficients
//                                        for (int r = 0; r < 3; r++) {
//                                            for (int c = 0; c < 2; c++) {
//                                                if (c==0) {  // the first column is what we want (other column is zeros)
//                                                    acc_A[r] = getDouble(pTmp);
//                                                    Log.i(TAG, String.format("PJH - parse - acc_A[%d] = %15.12f", r, acc_A[r]));
//                                                    coeffs_found += 1;
//                                                }
//                                                pTmp += 8;
//                                            }
//                                        }
//                                    }
//                                    //
//                                    // manual zero offset
//                                    //
//                                    // next byte tells us how many zero offset terms there are
//                                    pTmp+=1; // just assume the count is 1 and skip over it - YUK   TODO
//                                    // PJH - don't know exactly how this field is used in the ipod, so need to reverse engineer that
//                                    // to ensure the android behaves the same (is this value +ve or -ve? is it added or subtracted?)
//                                    offset_of_accManualZeroOffset = pTmp;   // remember the location of this parameter, so we can change it later
//                                    accManualZeroOffset = getDouble(pTmp);
//                                    coeffs_found += 2;
//                                    Log.i(TAG, String.format("PJH - parse - acc Manual Zero offset = %15.12f", accManualZeroOffset));
//                                    Log.i(TAG, String.format("PJH - parse - acc Manual Zero offset is located at offset = %6d", offset_of_accManualZeroOffset));
//                                    pTmp += 8;
//
//                                    if ((flags & 0x0100) != 0) {
//                                        // bit 8 is set, so cubic order terms are present - 1x3
//                                        for (int c = 0; c < 3; c++) {
//                                            acc_C[c] = getDouble(pTmp);
//                                            Log.i(TAG, String.format("PJH - parse - acc_C[%d] = %15.12f", c, acc_C[c]));
//                                            coeffs_found += 1;
//                                            pTmp += 8;
//                                        }
//                                    }
//                                    p += blk_len;   // advance pointer to next block
//                                }
//                                else if (hdr_id == 0x03) {    // mag block =============================================
//                                    if (blk_len <= 10) {
//                                        // ignore this malformed magnetometer block - effects Ezycore (maybe Corecam)
//
//                                        // assume we are talking to a corecam and clear all mag variables
//                                        // and update parameter count, for compatibility
//                                        mag_A[0] = 0;
//                                        mag_A[1] = 0;
//                                        mag_A[2] = 0;
//                                        mag_B[0][0] = 0;
//                                        mag_B[0][1] = 0;
//                                        mag_B[0][2] = 0;
//                                        mag_B[1][0] = 0;
//                                        mag_B[1][1] = 0;
//                                        mag_B[1][2] = 0;
//                                        mag_B[2][0] = 0;
//                                        mag_B[2][1] = 0;
//                                        mag_B[2][2] = 0;
//                                        mag_C[0] = 0;
//                                        mag_C[1] = 0;
//                                        mag_C[2] = 0;
//                                        magManualZeroOffset = 0;
//                                        coeffs_found += 16;
//                                    }
//                                    else {
//                                        pTmp = p;  // we don't want to mess up 'p'
//                                        // flags tells up what sub field exist in this block
//                                        flags = (((((int) binCalData[pTmp + 1]) & 0xFF) << 8) + (((int) binCalData[pTmp]) & 0xFF));
//                                        pTmp += 2;  // skip over flags
//
//                                        if ((flags & 0x0001) != 0) {
//                                            // temperature data - skip it
//                                            pTmp += 8;
//                                        }
//                                        if ((flags & 0x0004) != 0) {
//                                            // first order terms - 3x3
//                                            for (int r = 0; r < 3; r++) {
//                                                for (int c = 0; c < 3; c++) {
//                                                    mag_B[r][c] = getDouble(pTmp);
//                                                    Log.i(TAG, String.format("PJH - parse - mag_B[%d][%d] = %15.12f", r, c, mag_B[r][c]));
//                                                    coeffs_found += 1;
//                                                    pTmp += 8;
//                                                }
//                                            }
//                                        }
//                                        if ((flags & 0x0008) != 0) {
//                                            pTmp+=1; // just assume this count is for the 3.2 matrix - TODO
//                                            // zero order terms - 3x2 - don't know why there are extra co-efficients
//                                            for (int r = 0; r < 3; r++) {
//                                                for (int c = 0; c < 2; c++) {
//                                                    if (c==0) {  // the first column is what we want (other column is zeros)
//                                                        mag_A[r] = getDouble(pTmp);
//                                                        Log.i(TAG, String.format("PJH - parse - mag_A[%d] = %15.12f", r, mag_A[r]));
//                                                        coeffs_found += 1;
//                                                    }
//                                                    pTmp += 8;
//                                                }
//                                            }
//                                        }
//                                        // manual zero offset - I don't believe this is used anywhere
//                                        pTmp+=1; // just assume the count is 1  TODO
//                                        magManualZeroOffset = getDouble(pTmp);
//                                        coeffs_found += 1;
//                                        Log.i(TAG, String.format("PJH - parse - mag Manual Zero offset = %15.12f", magManualZeroOffset));
//                                        pTmp += 8;
//
//                                        if ((flags & 0x0100) != 0) {
//                                            // cubic order terms - 1x3
//                                            for (int c = 0; c < 3; c++) {
//                                                mag_C[c] = getDouble(pTmp);
//                                                Log.i(TAG, String.format("PJH - parse - mag_C[%d] = %15.12f", c, mag_C[c]));
//                                                coeffs_found += 1;
//                                                pTmp += 8;
//                                            }
//                                        }
//
//                                    }
//
//                                    p += blk_len;   // advance pointer to next block
//                                }
//                                else if (hdr_id == 0x04) {    // temperature block =============================================
//                                    // these parameters are used to calibrate the main temperature sensor
//                                    pTmp = p;  // we don't want to mess up 'p'
//                                    // flags tells up what sub field exist in this block
//                                    flags = ((int) binCalData[pTmp]) & 0xFF;
//                                    int count = ((int) binCalData[pTmp + 1]) & 0xFF;   // YUK - just assuming this is 2
//                                    pTmp += 2;  // skip over flags and parameter count
//
//                                    if ((flags & 0x01) != 0) {
//                                        // temperature data - just assume 2 parameters??? - YUK - TODO
//                                        temp_param[0] = getDouble(pTmp);  //offset
//                                        pTmp += 8;
//                                        temp_param[1] = getDouble(pTmp);  // scale
//                                        pTmp += 8;
//                                        coeffs_found += 2;
//                                        Log.i(TAG, String.format("PJH - parse - temp[0] = %15.12f (offset)", temp_param[0]));
//                                        Log.i(TAG, String.format("PJH - parse - temp[1] = %15.12f (scale)",  temp_param[1]));
//                                    }
//                                    p += blk_len;   // advance pointer to next block
//                                }
//                                else {             // =============================================
//                                    p += blk_len;  // just skip any unknown blocks (shouldn't be any)
//                                }
//                            }
//
//                            // do a final sanity check to ensure we got all the parameters we expected
//                            if (coeffs_found == NUM_CAL_PARAMS_EXPECTED_DURING_PARSING) {
//                                isCalibrated = true;
//                                Log.i(TAG, String.format("PJH - parse - found %d calibration parameters", coeffs_found));
//                                Log.w(TAG, "PJH - successfully parsed binary calibration data  =======================");
//                                CalibrationMatrixCreated = true;
//                                caliDataCollected = true;
//                            }
//                        }
//
//
//                    } else {
//                        // oops, calibration data is not valid
//                        Log.w(TAG, "PJH - CRC of the body of the binary cal data is incorrect - aborting");
//                    }
//                } else {
//                    // oops, calibration data is not valid
//                    Log.w(TAG, "PJH - CRC of the header of the binary cal data is incorrect - aborting");
//                }
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "PJH - parse - Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
//        }
//
//        if (isCalibrated != true) {
//            Log.w(TAG, "PJH - parsing binary calibration data FAILED =======================");
//            Log.i(TAG, String.format("PJH - parse - only found %d calibration parameters (expected %d)", coeffs_found, NUM_CAL_PARAMS_EXPECTED_DURING_PARSING));
//        }
//
//        return isCalibrated;
//    }
//
//    public boolean isCalibrated() {
//        return isCalibrated;
//    }
//
//    public String getCalibratedDateString() {
//        return calibratedDateString;
//    }
//
//    private int cal_crc16_gen(byte[] buffer, int start, int end) {
//        // CRC parameters for calibration body
//        int CRC16_POLY_CCITT = 0x1021;
//        int CRC16_INIT_VALUE = 0x0000;
//        int CRC16_FINAL_XOR  = 0x0000;
//
//        int crc = CRC16_INIT_VALUE;
//
//        for (int i=start; i<=end; i++) {
//            int buff = buffer[i];   // get the next byte, nad let it sign extend
//            buff &= 0xFF;    // remove any sign extension
//            crc = crc ^ (buff << 8);
//            for (int bit=0; bit<8; bit++) {
//                if ((crc & 0x8000) != 0) {
//                    crc = (crc << 1) ^ CRC16_POLY_CCITT;
//                }
//                else {
//                    crc = (crc << 1);
//                }
//                crc &= 0xFFFF;    // ensure CRC is limited to 16 bit
//            }
//        }
//        crc = crc ^ CRC16_FINAL_XOR;
//        crc &= 0xFFFF;   // just to be sure (shouldn't be necessary)
//        //print("Resulting CRC = 0x{:04X}".format(crc))
//        Log.i(TAG, String.format("PJH - cal_crc16_gen: %04X", crc));
//
//        return (crc);
//    }
//
//    private int cal_crc8_gen(byte[] buffer, int start, int end) {
//        // CRC parameters for main calibration data header
//        int CRC8_POLY_CCITT = 0x8D;
//        int CRC8_INIT_VALUE = 0x00;
//        int CRC8_FINAL_XOR  = 0x00;
//        //print("Generating 8bit CRC of", len(buffer), "bytes of data")
//        int crc = CRC8_INIT_VALUE;
//
//        for (int i=start; i<=end; i++) {
//            int buff = buffer[i];   // get the next byte, nad let it sign extend
//            buff &= 0xFF;    // remove any sign extension
//            crc = crc ^ buff;
//            for (int bit=0; bit<8; bit++) {
//                if ((crc & 0x80) != 0) {
//                    crc = (crc << 1) ^ CRC8_POLY_CCITT;
//                }
//                else {
//                    crc = (crc << 1);
//                }
//                crc &= 0xFF;   // ensure CRC is limited to 8 bit
//            }
//        }
//        crc = crc ^ CRC8_FINAL_XOR;
//        crc &= 0xFF;   // just to be sure (shouldn't be necessary)
//        //print("Resulting CRC = 0x{:02X}".format(crc))
//        Log.i(TAG, String.format("PJH - cal_crc8_gen: %02X", crc));
//
//        return (crc);
//    }
//
////    public native String calibrateAllData(String input_data);
//
//    /**
//     * Go through all gatt services avaliable
//     * @param gattServices
//     *
//     * TODO - currently highly inefficient, can improve speed of app by fixing this
//     * TODO - Fix Calibration speed here
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
////            if (gattService.getUuid().toString().equals(SampleGattAttributes.PRIMARY_SERVICE_CHARACTERISTICS)) {
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
////                Log.e(TAG, "Gatt service is: " + gattService.getUuid().toString());
//            //look for the device ID
//            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
//                charas.add(gattCharacteristic);
//                HashMap<String, String> currentCharaData = new HashMap<>();
//                uuid = gattCharacteristic.getUuid().toString();
//                currentCharaData.put(
//                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString)
//                );
//                currentCharaData.put(LIST_UUID, uuid);
//                gattCharacteristicGroupData.add(currentCharaData);
//
////                        Log.d(TAG, "Gatt characteristic is: " + gattCharacteristic.getUuid().toString());
//                if (gattCharacteristic.getUuid() != null) {
//                    if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.CALIBRATION_INDEX)) {
////                                Log.e(TAG, "Reading calibration index");
//                        Operation getCalibrationIndex = new Operation(gattService, gattCharacteristic, OPERATION_READ);
//                        try {
//                            request(getCalibrationIndex);
//                        } catch (Exception e) {
//                            Log.e(TAG, "Exception thrown for requesting operation");
//                        }
//                    } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.CALIBRATION_DATA)) {
////                                Log.e(TAG, "Reading calibration data");
//                        Operation getCalibrationData = new Operation(gattService, gattCharacteristic, OPERATION_READ);
//                        try {
//                            request(getCalibrationData);
//                        } catch (Exception e) {
//                            Log.e(TAG, "Exception thrown for requesting operation");
//                        }
//                    } else if (gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.DEVICE_ADDRESS)) {
////                                Log.e(TAG, "Reading device address");
////                        if (caliDataCollected) {
////
//                        Operation getAddressData = new Operation(gattService, gattCharacteristic, OPERATION_READ);
//                        try {
//                            request(getAddressData);
//                        } catch (Exception e) {
//                            Log.e(TAG, "Exception thrown for requesting operation");
//                        }
//                    }
////                    }
//                } else {
////                            Log.e(TAG, "gatt characteristic uuid is null");
//                }
//
//            }
////            } else {
////                Log.e(TAG, "Something else: ");
////            }
//        }
//    }
//
//    /**
//     *
//     * @param savedInstanceState the data most recently supplied in {@link #onSaveInstanceState}.
//     *
//     */
//    @Override
//    public void onRestoreInstanceState(Bundle savedInstanceState) {
//        //save the address and name of the device last connected to so when we come back to it we can reconnect
//        //reassign stuff?
////        blackProbeTxt.setText("Probe: " + mDeviceName);
//        updateConnectionState(mDeviceConnectionStatus);
//        Log.e(TAG, "Saved instance:  " + savedInstanceState);
//    }
//
//    /**
//     *
//     * @param outState Bundle in which to place your saved state.
//     *
//     */
//    @Override
//    public void onSaveInstanceState(Bundle outState) {
//        outState.putString(NAME_STATE_KEY, mDeviceName);
//        outState.putString(ADDRESS_STATE_KEY, mDeviceAddress);
//        outState.putString(EXTRA_CONNECTION_STATUS, mDeviceConnectionStatus);
//
//        Log.e(TAG, "Saved Device info: " + outState);
//
//        super.onSaveInstanceState(outState);
//    }
//
//
//    /**
//     * Update probe connection state based on resourceID
//     * @param resourceId, should either be "Connected" or "Disconnected"
//     *
//     * TODO - should be made an enum so that spelling wont be an issue
//     */
//    private void updateConnectionState(final String resourceId) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
////                Log.e(TAG, "UPDATING CONNECTION STATE");
//                if (resourceId.equals("Connected")) {
//                    mDeviceConnectionStatus = "Connected";
//                    //if we want to collect calibration data, and haven't done so yet, or started
//                    if (Globals.enableCalibration && !caliDataCollected && !started) {
//                        boolean newProbe = true;
//
//                        try {
//                            if (!(mDeviceName == null && mDeviceName.isEmpty())) {
//                                Log.d(TAG, "------------ANNA---------------");
//                                Log.d(TAG, "Device (from mDeviceName): " + mDeviceName);
//
//                                for (int i = 0; i < CalibrationHelper.getKnownCal().length; i++) {
//                                    if (Integer.valueOf(mDeviceName) == CalibrationHelper.getKnownCal()[i]) {
//                                        //Probe calibration data is already saved
//                                        newProbe = false;
//                                    }
//                                }
//                            } else if (!(EXTRA_DEVICE_NAME == null && EXTRA_DEVICE_NAME.isEmpty())) {
//                                Log.d(TAG, "------------ANNA---------------");
//                                Log.d(TAG, "Device (from EXTRA_DEVICE_NAME): " + mDeviceName);
//
//                                for (int i = 0; i < CalibrationHelper.getKnownCal().length; i++) {
//                                    if (Integer.valueOf(mDeviceName) == CalibrationHelper.getKnownCal()[i]) {
//                                        //Probe calibration data is already saved
//                                        newProbe = false;
//                                    }
//                                }
//                            } else {
//
//                            }
//                        } catch (Exception e) {
//                            Log.e(TAG, "Exception thrown checking probe calibration storage status: " + e);
//                        }
//                        //check whether we have the probe or not
//                        if (newProbe) {
//                            started = true;
//                            indexNext();
//                        } else {
//                            //TODO use calibration data based on probes name
//                        }
//                    }
//                } else if (resourceId.equals("Disconnected")) {
//                    registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
//                    if (mBluetoothLeService != null) {
//                        final boolean result = mBluetoothLeService.connect(mDeviceAddress);
//                        Log.d(TAG, "Connection request result=" + result);
//                    }
//                    mDeviceConnectionStatus = "Disconnected";
//                } else {
//                    mDeviceConnectionStatus = "Disconnected";
//                }
//            }
//        });
//    }
//
//    private void displayData(String data) {
//        if (data != null) {
//            //display the data, probably not needed in this section
//        }
//    }
//
//    /**
//     * Go to survey options class on button click
//     * @param v
//     */
//    public void operatorIDBtnClick(View v) {
//        Intent intent = new Intent(this, SurveyOptionsActivity.class);
//        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_NAME, mDeviceName);
//        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
//        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_CONNECTION, mDeviceConnectionStatus);
//        startActivity(intent);
//    }
//
//    /**
//     * Go to survey options class on button click
//     * @param v
//     */
//    public void holeIDBtnClick(View v) {
//        Intent intent = new Intent(this, SurveyOptionsActivity.class);
//        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_NAME, mDeviceName);
//        intent.putExtra(SurveyOptionsActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
//        startActivity(intent);
//    }
//
//    /**
//     * If probe connected and clicked on go to survey popup activity (InialisePopupActivity)
//     * @param v
//     */
//    public void BlackProbeBtnClick(View v) {
//        if (mConnected) {
//            Intent intent = new Intent(this, InitalisePopupActivity.class);
//            Log.d(TAG, "Device Name: " + mDeviceName + ", Device Address: " + mDeviceAddress);
//            intent.putExtra(InitalisePopupActivity.EXTRA_DEVICE_NAME, mDeviceName);
//            intent.putExtra(InitalisePopupActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
//            startActivity(intent);
//        } else {
//            Log.e(TAG, "Probe is disconnected");
//        }
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
//     * Calibrate probe
//     * @param view
//     */
//    public void calibrate(View view) {
//        //get calibration data + write to calibration index then read again
//        displayGattServices(mBluetoothLeService.getSupportedGattServices()); //read calibation index then calibration data
//    }
//
//
//    /**
//     * Get next calibration index value
//     * @param view
//     */
//    public void index(View view) {
//        if (Globals.enableCalibration && !CalibrationMatrixCreated) {
//            boolean status = false;
//            do {
//                //(byte) calibrationIndexNum
//                status = mBluetoothLeService.writeToCalibrationIndex((byte) 00); //TODO make this a variable input
////            Log.e(TAG, "Status of write: " + status);
//                try {
//                    Thread.sleep(1000);
//                } catch (Exception e) {
//                    Log.e(TAG, "Could not sleep" + e);
//                }
//                if (status) {
////                dataToBeRead = 0;
//                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
//
//                } else {
//                    try {
//                        if (mDeviceConnectionStatus.equals("Connected")) {
//                            updateConnectionState("Connected");
//                        } else {
//                            Log.e(TAG, "Device disconnected");
//                            updateConnectionState("Disconnected");
//                        }
//                    } catch (Exception e) {
//                        Log.e(TAG, "Error setting connection state: " + e);
//                    }
//                }
//                new CountDownTimer(700, 1) { //definetly inefficicent, but if it works dont touch it lol
//
//                    public void onTick(long millisUntilFinished) {
//                        //                mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
//                    }
//
//                    public void onFinish() {
//                        //                    dataToBeRead = 0;
//                        //                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
//                    }
//                }.start();
//
//                new CountDownTimer(3000, 1) { //definetly inefficicent, but if it works dont touch it lol
//
//                    public void onTick(long millisUntilFinished) {
//                        //                mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
//                    }
//
//                    public void onFinish() {
//                        //                    dataToBeRead = 0;
//                        //                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
//                    }
//                }.start();
//
//            } while (!status);
//        }
//    }
//
//    public void indexNext() {
//        if (Globals.enableCalibration) {
//            //Set the status indicator to calibrating
////            blackProbeStatusImg.setImageResource(R.drawable.calibrating);
////            blackProbeStatusTxt.setText("Calibrating");
//
//            boolean status = false; //default the status to false
//            do {
//                status = mBluetoothLeService.writeToCalibrationIndex((byte) calibrationIndexNum); //TODO make this a variable input
//                Log.e(TAG, "Status of writing: " + calibrationIndexNum + ": " + status);
//
//                if (writeCollectValue == 1) {
//                    calibrationFirstValue = calibrationIndexNum;
//                } else if (writeCollectValue == 2) {
//                    calibrationSecondValue = calibrationIndexNum;
//                } else if (writeCollectValue == 3) {
//                    calibrationThirdValue = calibrationIndexNum;
//                }
//                writeCollectValue++;
//                if (writeCollectValue >= 3) {
//                    writeCollectValue = 0;
//                }
//
//                if (((calibrationFirstValue + calibrationSecondValue + calibrationThirdValue) / 3) - calibrationIndexNum > 2) {
//                    Log.e(TAG, "------------------ANNA-----------------");
//                    Log.e(TAG, "Restarting calibration as data was invalid");
//                    caliData.clear();
//                    calibrationIndexNum = 0;
//                    calibrationFirstValue = 0;
//                    calibrationSecondValue = 0;
//                    calibrationThirdValue = 3;
//                }
//
//                try {
//                    Thread.sleep(500);
//                } catch (Exception e) {
//                    Log.e(TAG, "Could not sleep" + e);
//                }
//                if (status) {
//                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
//                } else {
//                    try {
//                        if (mDeviceConnectionStatus.equals("Connected")) {
//                            updateConnectionState("Connected");
//                        } else {
//                            Log.e(TAG, "Device disconnected");
//                            updateConnectionState("Disconnected");
//                        }
//                    } catch (Exception e) {
//                        Log.e(TAG, "Error setting connection state: " + e);
//                    }
//                }
//                new CountDownTimer(700, 1) { //definetly inefficicent, but if it works dont touch it lol
//
//                    public void onTick(long millisUntilFinished) {
//                        //                mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
//                    }
//
//                    public void onFinish() {
//                        //                    dataToBeRead = 0;
//                        //                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
//                    }
//                }.start();
//
//                new CountDownTimer(3000, 1) { //definetly inefficicent, but if it works dont touch it lol
//
//                    public void onTick(long millisUntilFinished) {
//                        //                mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
//                    }
//
//                    public void onFinish() {
//                        //                    dataToBeRead = 0;
//                        //                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
//                    }
//                }.start();
//            } while (!status);
//        }
//    }
}