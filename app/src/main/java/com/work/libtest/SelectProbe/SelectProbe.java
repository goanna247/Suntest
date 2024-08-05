package com.work.libtest.SelectProbe;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

//import com.work.libtest.HoleID.HoleIDActivity;
import com.work.libtest.MainActivity;
import com.work.libtest.R;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SelectProbe extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_probe);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}

//TODO rename this to SelectProbeActivity
//public class SelectProbe extends AppCompatActivity implements AdapterView.OnItemClickListener{
//    private  static final String TAG = "SelectProbe";
//
//    BluetoothAdapter mBluetoothAdapter;
//    Button btnEnableDisable_Discoverable;
//    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
//    public DeviceListAdapter mDeviceListAdapter;
//    ListView lvNewDevices;
//
//    //Create a BroadcastReceiver for ACTION_FOUND
//    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            //When discovery finds a device
//            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
//                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);
//
//                switch(state) {
//                    case BluetoothAdapter.STATE_OFF:
//                        Log.d(TAG, "onReceive: STATE OFF");
//                        break;
//                    case BluetoothAdapter.STATE_TURNING_OFF:
//                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
//                        break;
//                    case BluetoothAdapter.STATE_ON:
//                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
//                        break;
//                    case BluetoothAdapter.STATE_TURNING_ON:
//                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
//                        break;
//
//                }
//            }
//        }
//    };
//
//    /**
//     * Broadcast Receiver for changes made to bluetooth states such as:
//     * 1) Discoverability mode on/off or expire
//     */
//
//    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            final String action = intent.getAction();
//            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
//                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
//
//                switch (mode) {
//                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
//                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
//                        break;
//                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
//                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
//                        break;
//                    case BluetoothAdapter.SCAN_MODE_NONE:
//                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections");
//                        break;
//                    case BluetoothAdapter.STATE_CONNECTING:
//                        Log.d(TAG, "mBroadcastReceiver2: Connecting.....");
//                        break;
//                    case BluetoothAdapter.STATE_CONNECTED:
//                        Log.d(TAG, "mBroadcastReceiver2: Connected");
//                        break;
//                }
//            }
//        }
//    };
//
//    /**
//     * Broadcast receiver for listing devices that are not yet paired
//     * - executed by btnDiscover Method
//     */
//
//    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            final String action = intent.getAction();
//            Log.d(TAG, "onReceive: ACTION FOUND");
//            boolean newDevice = false;
//            boolean isNewDevice = true;
//            String compare ="";
//            String newAddress ="";
//
//            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
//                BluetoothDevice device = intent.getParcelableExtra( BluetoothDevice.EXTRA_DEVICE);
//
//                Log.d(TAG, "compared to: " + device.getAddress());
//
//                //TODO remove duplicates being added to the array
//
//                if (mBTDevices.size() == 0) { //if array is empty we need to first add a value
//                    if (device.getName() != null) {
//                        mBTDevices.add(device);
//                        Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
//                        mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
//                    }
//                } else { //if the array is not already empty we should compare the new value to the existing values to ensure no duplicates
//                    for (int i=0; i < mBTDevices.size(); i++) {
//                        //check if device is already in the array
//                        if (isNewDevice) {
//                            if (mBTDevices.get(i).getName() == device.getName()) {
//                                newDevice = false;
//                                Log.d(TAG, "Found duplicate");
//                                isNewDevice = false;
//                                //we need to go through the entire for loop dumbass
//                            } else { //does equal a current value in the array
//                                //found a duplicate device, we don't want to store or display this
//                                newDevice = true;
//                            }
//                        } else {
//                            break;
//                        }
//
//                    }
//
//                    if (newDevice) {
//                        //found new device
//                        mBTDevices.add(device);
//                        Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
//                        mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
//                    }
//                }
//
//                lvNewDevices.setAdapter(mDeviceListAdapter);
//            }
//        }
//    };
//
//    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
//        String BLE_PIN = "1234";
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            final String action = intent.getAction();
//            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
//                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                mDevice.setPin("1234".getBytes());
//                //3 cases
//                //case 1: bonded already
//                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
//                    Log.d(TAG, "BroadcastReciever: BOND_BOUNDED");
//                    //Connected! -> go from here to displaying data
//                    //Open a new fragment which asks the user which bore colour or to cancel the connection
////                    Intent activityIntent = new Intent(context, MainActivity.class);
////                    startActivity(activityIntent);
//                    //make colour green
//                    MainActivity.connectedDeviceName = mDevice.getName();
//
//                }
//                //case2: creating a bone
//                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
//                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING");
//                    //make colour orange
////                    DeviceListAdapter.setGreen(R.layout.device_adapter_view);
////                    DeviceListAdapter.greenPositions.add(5);
//
////                    mDevice.setPin(BLE_PIN.getBytes());
////                    Log.e(TAG, "Auto-entering pin: " + BLE_PIN);
////                    mDevice.createBond();
////                    Log.e(TAG, "pin entered and request sent...");
////                    BluetoothDevice device = intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
//
////                    mDevice.setPairingConfirmation(true);
//                    MainActivity.connectedDeviceName = "No Probe Selected"; //Default name TODO probs make this a variable
//
//                }
//                //case 3: breaking a bond
//                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
//                    Log.d(TAG, "BroadcastReceiver: BOND_NONE");
//                    //make colour red
//                    MainActivity.connectedDeviceName = "No Probe Selected"; //Default name TODO probs make this a variable
//
//                }
//            }
//
//        }
//    };
//
//    private final BroadcastReceiver mBroadcastReceiver5 = new BroadcastReceiver() {
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if (action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {
//                try {
//                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                    int pin=intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 0000);
//                    //the pin in case you need to accept for an specific pin
//                    Log.d(TAG, "Start Auto Pairing. PIN = " + intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY",0000));
//                    byte[] pinBytes;
//                    pinBytes = (""+pin).getBytes("UTF-8");
//                    device.setPin(pinBytes);
//                    //setPairing confirmation if neeeded
//                    device.setPairingConfirmation(true);
//                } catch (Exception e) {
//                    Log.e(TAG, "Error occurs when trying to auto pair");
//                    e.printStackTrace();
//                }
//            }
//        }
//    };
//
//    @Override
//    protected void onDestroy() {
//        Log.d(TAG, "onDestroy: called");
//        super.onDestroy();
////        unregisterReceiver(mBroadcastReceiver1);
////        unregisterReceiver(mBroadcastReceiver2);
////        unregisterReceiver(mBroadcastReceiver3);
////        unregisterReceiver(mBroadcastReceiver4);
//        //mBluetoothAdapter.cancelDiscovery();
//    }
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_select_probe);
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//
//        Button btnONOFF = (Button) findViewById(R.id.btnONOFF);
//        btnEnableDisable_Discoverable = (Button) findViewById(R.id.btnDiscoverable_on_off);
//        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
//        mBTDevices = new ArrayList<>();
//
//
//        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
//        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
//        registerReceiver(mBroadcastReceiver4, filter);
//        registerReceiver(mBroadcastReceiver5, filter2);
//
//        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//
//        lvNewDevices.setOnItemClickListener(SelectProbe.this);
//
//        btnONOFF.setOnClickListener(new View.OnClickListener() {
//           @Override
//           public void onClick(View view) {
//               Log.d(TAG, "onClick: enabling/disabling bluetooth");
//               enableDisableBT();
//           }
//        });
//    }
//
//    public void enableDisableBT() {
//        if (mBluetoothAdapter == null) {
//            Log.d(TAG, "enableDisableBT: Does not have BT capablities.");
//        }
//        if (!mBluetoothAdapter.isEnabled()) {
//            Log.d(TAG, "enableDisableDT: enabling BT");
//            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivity(enableBTIntent);
//
//            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
//            registerReceiver(mBroadcastReceiver1, BTIntent);
//        }
//        if (mBluetoothAdapter.isEnabled()) {
//            Log.d(TAG, "enableDisableBTL disabling BT");
//            mBluetoothAdapter.disable();
//
//            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
//            registerReceiver(mBroadcastReceiver1, BTIntent);
//        }
//    }
//
//    public void btnEnableDisable_Discoverable(View view) {
//        Log.d(TAG, "btnEnableDisable_Discoverable: Making device discoverable for 300 seconds.");
//        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//        startActivity(discoverableIntent);
//
//        IntentFilter intentFilter = new IntentFilter(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
//        registerReceiver(mBroadcastReceiver2, intentFilter);
//    }
//
//    public void btnDiscover(View view) {
//        // TODO check if bluetooth is enabled
//        if (!mBluetoothAdapter.isEnabled()) {
//            enableDisableBT();
//        }
//        Log.d(TAG, "btnDiscover: looking for unpaired devices.");
//
//        if (mBluetoothAdapter.isDiscovering()) {
//            mBluetoothAdapter.cancelDiscovery();
//            Log.d(TAG, "btnDiscover: Canceling discovery");
//
//            checkBTPermissions();
//
//            mBluetoothAdapter.startDiscovery();
//            IntentFilter discoverDevicesIntent = new IntentFilter((BluetoothDevice.ACTION_FOUND));
//            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
//        }
//        if (!mBluetoothAdapter.isDiscovering()) {
//            checkBTPermissions();
//
//            mBluetoothAdapter.startDiscovery();
//            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
//        }
//    }
//
//    private void checkBTPermissions() {
//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
//            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
//            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
//            if (permissionCheck != 0) {
//                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
//            }
//        } else {
//            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP");
//        }
//    }
//
//    @Override
//    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//        mBluetoothAdapter.cancelDiscovery(); //discovery is very memory intensive
//
//        Log.d(TAG, "onItemClick: You clicked on a device.");
//        String deviceName = mBTDevices.get(i).getName();
//        String deviceAddress = mBTDevices.get(i).getAddress();
//
//        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
//        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);
//
//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
//            Log.d(TAG, "Trying to pair with " + deviceName);
//            mBTDevices.get(i).createBond();
//        }
//    }
//}

