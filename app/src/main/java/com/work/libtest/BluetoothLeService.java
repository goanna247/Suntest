////////////////////////////////////////////////////////////////////////////////
/**
 * \file BluetoothLeService.java
 * \brief Helper class for all Bluetooth Low energy connections
 * \author Anna Pedersen
 * \date Created: 07/06/2024
 *
 */
package com.work.libtest;

import static com.work.libtest.TakeMeasurements.shotWriteType;
import static java.lang.Math.pow;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    //Probe status indicators
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    //Bluetooth actions
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    public final static String DISABLED =
            "com.example.bluetooth.le.DISABLED";

    //Data codes
    public final static String SERIAL_NUMBER =
            "com.example.bluetooth.le.SERIAL_NUMBER";
    public final static String DEVICE_ADDRESS =
            "com.example.bluetooth.le.DEVICE_ADDRESS";
    public final static String MAJOR_FIRMWARE_VERSION =
            "com.example.bluetooth.le.MAJOR_FIRMWARE_VERSION";
    public final static String MINOR_FIRMWARE_VERSION =
            "com.example.bluetooth.le.MINOR_FIRMWARE_VERSION";
    public final static String CALIBRATION_DATE =
            "com.example.bluetooth.le.CALIBRATION_DATE";
    public final static String BORE_SHOT =
            "com.example.bluetooth.le.BORE_SHOT";
    public final static String CORE_SHOT =
            "com.example.bluetooth.le.CORE_SHOT";
    public final static String PROBE_MODE =
            "com.example.bluetooth.le.PROBE_MODE";
    public final static String SHOT_INTERVAL =
            "com.example.bluetooth.le.SHOT_INTERVAL";
    public final static String RECORD_COUNT =
            "com.example.bluetooth.le.RECORD_COUNT";
    public final static String SHOT_REQUEST =
            "com.example.bluetooth.le.SHOT_REQUEST"; //About to be very important
    public final static String SURVEY_MAX_SHOTS =
            "com.example.bluetooth.le.SURVEY_MAX_SHOTS";
    public final static String ROLLING_SHOT_INTERVAL =
            "com.example.bluetooth.le.ROLLING_SHOT_INTERVAL";
    public final static String CALIBRATION_INDEX =
            "com.example.bluetooth.le.CALIBRATION_INDEX";
    public final static String CALIBRATION_DATA =
            "com.example.bluetooth.le.CALIBRATION_DATA";

    public final static String CORE_SHOT_SHOT_FORMAT =
            "com.example.bluetooth.le.CORE_SHOT_SHOT_FORMAT";
    public final static String CORE_SHOT_RECORD_NUMBER =
            "com.example.bluetooth.le.CORE_SHOT_RECORD_NUMBER";
    public final static String CORE_SHOT_PROBE_TEMP =
            "com.example.bluetooth.le.CORE_SHOT_PROBE_TEMP";
    public final static String CORE_SHOT_AX =
            "com.example.bluetooth.le.CORE_SHOT_AX";
    public final static String CORE_SHOT_AY =
            "com.example.bluetooth.le.CORE_SHOT_AY";
    public final static String CORE_SHOT_AZ =
            "com.example.bluetooth.le.CORE_SHOT_AZ";
    public final static String CORE_SHOT_ACC_TEMP =
            "com.example.bluetooth.le.CORE_SHOT_ACC_TEMP";

    //Data UUID codes
    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);
    public final static UUID UUID_DEVICE_ID =
            UUID.fromString(SampleGattAttributes.DEVICE_ID);
    public final static UUID UUID_PROBE_MODE =
            UUID.fromString(SampleGattAttributes.PROBE_MODE);
    public final static UUID UUID_SHOT_INTERVAL =
            UUID.fromString(SampleGattAttributes.SHOT_INTERVAL);
    public final static UUID UUID_RECORD_COUNT =
            UUID.fromString(SampleGattAttributes.RECORD_COUNT);
    public final static UUID UUID_SHOT_REQUEST =
            UUID.fromString(SampleGattAttributes.SHOT_REQUEST);
    public final static UUID UUID_SURVEY_MAX_SHOTS =
            UUID.fromString(SampleGattAttributes.SURVEY_MAX_SHOTS);
    public final static UUID UUID_BORE_SHOT =
            UUID.fromString(SampleGattAttributes.BORE_SHOT);
    public final static UUID UUID_CORE_SHOT =
            UUID.fromString(SampleGattAttributes.CORE_SHOT);
    public final static UUID UUID_DEVICE_ADDRESS =
            UUID.fromString(SampleGattAttributes.DEVICE_ADDRESS);
    public final static UUID UUID_VERSION_MAJOR =
            UUID.fromString(SampleGattAttributes.VERSION_MAJOR);
    public final static UUID UUID_VERSION_MINOR =
            UUID.fromString(SampleGattAttributes.VERSION_MINOR);
    public final static UUID UUID_ROLLING_SHOT_INTERVAL =
            UUID.fromString(SampleGattAttributes.ROLLING_SHOT_INTERVAL);
    public final static UUID UUID_CALIBRATION_INDEX =
            UUID.fromString(SampleGattAttributes.CALIBRATION_INDEX);
    public final static UUID UUID_CALIBRATION_DATA =
            UUID.fromString(SampleGattAttributes.CALIBRATION_DATA);


    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
//                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        /**
         *
         * @param gatt GATT client invoked {@link BluetoothGatt#discoverServices}
         * @param status {@link BluetoothGatt#GATT_SUCCESS} if the remote device has been explored
         * successfully.
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        /**
         *
         * @param gatt GATT client invoked {@link BluetoothGatt#readCharacteristic}
         * @param characteristic Characteristic that was read from the associated remote device.
         * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation was completed
         * successfully.
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
//            Log.d(TAG, "characteristic read status: " + status);
            try {
//                Log.d(TAG, "Broadcasting");
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            } catch (Exception e) {
                Log.e(TAG, "Exception thrown");
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            } else {
//                Log.e(TAG, "Failed to read?");
            }
        }

        /**
         *
         * @param gatt GATT client invoked {@link BluetoothGatt#readDescriptor}
         * @param descriptor Descriptor that was read from the associated remote device.
         * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation was completed
         * successfully
         */
        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
//            Log.e(TAG, "ON descriptor read");
            super.onDescriptorRead(gatt, descriptor, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
//                Log.d(TAG, "DESCRIPTOR: " + descriptor.getUuid().toString());
            }
            broadcastUpdate(ACTION_DATA_AVAILABLE);
        }

        /**
         *
         * @param gatt GATT client invoked {@link BluetoothGatt#writeDescriptor}
         * @param descriptor Descriptor that was writte to the associated remote device.
         * @param status The result of the write operation {@link BluetoothGatt#GATT_SUCCESS} if the
         * operation succeeds.
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
//            Log.e(TAG, "ON descriptor write");
            super.onDescriptorWrite(gatt, descriptor, status);
//            Log.d(TAG, "Descriptor write: " + descriptor.getUuid().toString() + ", Descriptor characteristic: "+descriptor.getCharacteristic().getUuid().toString() + "Status: " + status);
        }

        /**
         *
         * @param gatt GATT client the characteristic is associated with
         * @param characteristic Characteristic that has been updated as a result of a remote
         * notification event.
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
//            Log.d(TAG, "CHARACTERISTIC: " + characteristic.getUuid().toString() + " Changed!");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    /**
     *
     * @param action to be broadcast
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
//        Log.d(TAG, "BROADCASTING UPDATE: ");
        intent.putExtra(CORE_SHOT, "1");
        sendBroadcast(intent);
    }

    /**
     *
     * @param action to be broadcast
     * @param characteristic to be used in the broadcast
     */
    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
//                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
//                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
//            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else if (UUID_SHOT_REQUEST.equals(characteristic.getUuid())) {
            //need to write to this value instead of reading from it
            writeData(characteristic);
        } else if (UUID_DEVICE_ID.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data) {
                    if (String.format("%02X ", byteChar) != "?") {
                        stringBuilder.append(String.format("%02X ", byteChar));
                    }
                }
                intent.putExtra(SERIAL_NUMBER, new String(data)).toString();
            }
        }
        else if (UUID_RECORD_COUNT.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
//            Log.d(TAG, "Characteristic: " + characteristic.getUuid().toString() + "has data: " + data[0] + ", " + data[1]);

            int recordCount = 0;

            for (int i = data.length; i > 0; i--) {
                if (i == data.length) {
                    recordCount = recordCount + data[i-1] * 256;
                } else if (i == 1) {
                    if (data[i-1] < 0) {
                        recordCount = recordCount + (256 + data[0]);
                    } else {
                        recordCount = recordCount + (data[0]);
                    }
                }
            }

            intent.putExtra(RECORD_COUNT, Integer.toString(recordCount));
        }
        else if (UUID_SHOT_INTERVAL.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
//            Log.d(TAG, "Characteristic: " + characteristic.getUuid().toString() + "has data: " + data);

            if (data != null && data.length > 0) {
                if (data.length == 1) {
//                    Log.d(TAG, "Shot interval has length of 1 and value of: " + String.valueOf(data[0]));
                    intent.putExtra(SHOT_INTERVAL, String.valueOf(data[0]));
                } else {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
//                    Log.d(TAG, "Number of bytes in shot data is: " + data.length);
                    for(byte byteChar : data) {
                        if (String.format("%02X ", byteChar) != "?") {
                            stringBuilder.append(String.format("%02X ", byteChar));
                        }
                    }
                    intent.putExtra(SHOT_INTERVAL, new String(data)).toString(); // + "\n" + stringBuilder.toString()
                }

            }
        }
        else if (UUID_VERSION_MAJOR.equals(characteristic.getUuid())) {
//            Log.d(TAG, "Reading Firmware version major");
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                if (data.length == 1) {
                    intent.putExtra(MAJOR_FIRMWARE_VERSION, String.valueOf(data[0]));
                } else {
//                    Log.e(TAG, "ERROR, versions should only have a length of 1 byte");
                }
            }
        } else if (UUID_VERSION_MINOR.equals(characteristic.getUuid())) {
//            Log.d(TAG, "Reading Firmware version minor");
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                if (data.length == 1) {
                    intent.putExtra(MINOR_FIRMWARE_VERSION, String.valueOf(data[0]));
                } else {
//                    Log.e(TAG, "ERROR, versions should only have a length of 1 byte");
                }
            }
        } else if (UUID_PROBE_MODE.equals(characteristic.getUuid())) {
//            Log.d(TAG, "Reading Probe Mode");
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                if (data.length == 1) {
                    intent.putExtra(PROBE_MODE, String.valueOf(data[0]));
//                    Log.d(TAG, "PROBE MODE IS: " + String.valueOf(data[0]));
                } else {
//                    Log.e(TAG, "Error, probe mode should only have a length of 1 byte");
                }
            }
        } else if (UUID_SHOT_REQUEST.equals(characteristic.getUuid())) {
//            Log.d(TAG, "Reading device address");
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (int i=data.length-1; i > 0; i--) {
                    stringBuilder.append(String.format("%02X", data[i]));
                    if (i != 1) { stringBuilder.append(":"); }
                }
//                Log.d(TAG, "Data from LE Service: " + stringBuilder.toString() );
                intent.putExtra(SHOT_REQUEST, stringBuilder.toString());
            }
        } else if (UUID_DEVICE_ADDRESS.equals(characteristic.getUuid())) {
//            Log.d(TAG, "Reading device address");
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (int i=data.length-1; i > 0; i--) {
                    stringBuilder.append(String.format("%02X", data[i]));
                    if (i != 1) { stringBuilder.append(":"); }
                }
//                Log.d(TAG, "Data from LE Service: " + stringBuilder.toString() );
                intent.putExtra(DEVICE_ADDRESS, stringBuilder.toString());
            }
        } else if (UUID_ROLLING_SHOT_INTERVAL.equals(characteristic.getUuid())) {
            double rolling_shot_interval = 0;
            //FIXME Getting results of 10 seconds, seems too long
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                //reverse the order of the bytes
                for (int i = data.length-1; i > 0; i--) {
                    stringBuilder.append(String.format("%02X", data[i]));
                }
                //convert a hex value into a decimal
                for (int i = 0; i < stringBuilder.length(); i++) {
                    rolling_shot_interval += stringBuilder.toString().charAt(i) * pow(16, stringBuilder.length() - (i+1));
                }
                rolling_shot_interval = rolling_shot_interval / 1000; //convert from millseconds to seconds
//                Log.d(TAG, "Rolling shot interval: " + rolling_shot_interval);
                intent.putExtra(ROLLING_SHOT_INTERVAL, String.valueOf(rolling_shot_interval));
            }
        } else if (UUID_BORE_SHOT.equals(characteristic.getUuid())) {
            try {
                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (int i = 0; i < data.length; i++) {
                        stringBuilder.append( data[i]);
                        stringBuilder.append(":");
                    }
//                    Log.d(TAG, "Data from LE Service BORE: " + stringBuilder.toString());
                    intent.putExtra(BORE_SHOT, stringBuilder.toString());
                }
            } catch (Exception e) {
//                Log.e(TAG, "getting bore shot data throwing errors: " + e);
            }
        } else if (UUID_CORE_SHOT.equals(characteristic.getUuid())) {
            try {
                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (int i = 0; i < data.length; i++) {
                        stringBuilder.append( data[i]);
                        stringBuilder.append(":");
                    }
//                    Log.d(TAG, "Data from LE Service CORE: " + stringBuilder.toString());
                    intent.putExtra(CORE_SHOT, stringBuilder.toString());
                }
            } catch (Exception e) {
//                Log.e(TAG, "getting core shot data throwing errors: " + e);
            }
        } else if (UUID_CALIBRATION_INDEX.equals(characteristic.getUuid())) {
//            Log.d(TAG, "Reading Calibration Index");
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                if (data.length == 1) {
                    intent.putExtra(CALIBRATION_INDEX, String.valueOf(data[0]));
//                    Log.d(TAG, "Calibration Index IS: " + String.valueOf(data[0]));
                } else {
//                    Log.e(TAG, "Error with calibration index");
                }
            }
        } else if (UUID_CALIBRATION_DATA.equals(characteristic.getUuid())) {
            try {
                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (int i = 0; i < data.length; i++) {
                        stringBuilder.append( data[i]);
                        stringBuilder.append(":");
                    }
//                    Log.d(TAG, "Data from calibration data: " + stringBuilder.toString());
                    intent.putExtra(CALIBRATION_DATA, stringBuilder.toString());
                }
            } catch (Exception e) {
//                Log.e(TAG, "getting calibration data throwing errors: " + e);
            }
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
//            Log.d(TAG, "Characteristic: " + characteristic.getUuid().toString() + "has data: " + data);
            if (characteristic.getUuid().toString().equals("0000ff02-0000-1000-8000-00805f9b34fb")) {
                if (data != null && data.length > 0) {
                    if (data.length == 1) {
//                        Log.d(TAG, "EPIC Calibration DATA has length of 1 and value of: " + String.valueOf(data[0]));
                        intent.putExtra(CALIBRATION_DATA, String.valueOf(data[0]));
                    } else {
                        final StringBuilder stringBuilder = new StringBuilder(data.length);
//                        Log.d(TAG, "Number of bytes in piece of data is: " + data.length);
                        for(byte byteChar : data) {
                            if (String.format("%02X ", byteChar) != "?") {
                                stringBuilder.append(String.format("%02X ", byteChar));
                            }
                        }
//                        intent.putExtra(CALIBRATION_DATA, new String(data)).toString();
                    }

                }
            } else {
                if (data != null && data.length > 0) {
                    if (data.length == 1) {
//                        Log.d(TAG, "DATA has length of 1 and value of: " + String.valueOf(data[0]));
                        intent.putExtra(EXTRA_DATA, String.valueOf(data[0]));
                    } else {
                        final StringBuilder stringBuilder = new StringBuilder(data.length);
//                        Log.d(TAG, "Number of bytes in piece of data is: " + data.length);
                        for(byte byteChar : data) {
                            if (String.format("%02X ", byteChar) != "?") {
                                stringBuilder.append(String.format("%02X ", byteChar));
                            }
                        }
                        intent.putExtra(EXTRA_DATA, new String(data)).toString(); // + "\n" + stringBuilder.toString()
//                    Log.d(TAG, "Data from LE Service: " + new String(data) + "\n" + stringBuilder.toString() );
                    }

                }
            }

        }
        sendBroadcast(intent);
    }


    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    /**
     *
     * @param intent The Intent that was used to bind to this service,
     * as given to {@link Context#bindService
     * Context.bindService}.  Note that any extras that were included with
     * the Intent at that point will <em>not</em> be seen here.
     *
     * @return
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     *
     * @param intent The Intent that was used to bind to this service,
     * as given to {@link Context#bindService
     * Context.bindService}.  Note that any extras that were included with
     * the Intent at that point will <em>not</em> be seen here.
     *
     * @return
     */
    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        //add a close
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
//        Log.e(TAG, "Reading Characteristic: " + characteristic.getUuid().toString());
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            final Intent intent = new Intent();
            intent.putExtra(DISABLED, "Disabled");
            sendBroadcast(intent);
            return;
        } else {
//            Log.d(TAG, "Reading characteristic");
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
//        Log.e(TAG, "Setting characteristic notification");
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
//        Log.d(TAG, "Enabling notification on the following characteristic: " + characteristic.getUuid().toString());

//        if (UUID_CALIBRATION_INDEX.equals(characteristic.getUuid())) {
//            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
//                    UUID.fromString(SampleGattAttributes.CALIBRATION_INDEX));
//            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//            mBluetoothGatt.writeDescriptor(descriptor);
//        }

        // This is specific to Heart Rate Measurement.
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }


        if (UUID_RECORD_COUNT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.RECORD_COUNT)
            );
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }

        if (UUID_DEVICE_ID.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.DEVICE_ID));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }

        if (UUID_DEVICE_ADDRESS.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.DEVICE_ADDRESS));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }

        if (UUID_VERSION_MINOR.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.VERSION_MINOR));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
            Log.d(TAG, "VERSION MINOR IS: " + descriptor.toString());
        }

        if (UUID_VERSION_MAJOR.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.VERSION_MAJOR));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
            Log.d(TAG, "VERSION MAJOR IS: " + descriptor.toString());
        }

        if (UUID_CALIBRATION_INDEX.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CALIBRATION_INDEX));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
            Log.e(TAG, "CALIBRATION INDEX IS: " + descriptor.toString());
        }

        if (UUID_SHOT_INTERVAL.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.SHOT_INTERVAL));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
            Log.e(TAG, "SHOT INTERVAL IS: " + descriptor.toString());
        }

        if (UUID_CALIBRATION_DATA.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CALIBRATION_DATA));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
            Log.e(TAG, "CALIBRATION DATA IS: " + descriptor.toString());
        }

//        if (UUID_BORE_SHOT.equals(characteristic.getUuid())) {
//            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
//                    UUID.fromString(SampleGattAttributes.BORE_SHOT));
//            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//            mBluetoothGatt.writeDescriptor(descriptor);
//            Log.d(TAG, "Bore shot is: " + descriptor.toString());
//        }

        if (UUID_BORE_SHOT.equals(characteristic.getUuid())) {
            Log.d(TAG, "BORE SHOT NOTIFICATION SETTING ON");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Log.e(TAG, "ERROR SLEEPING");
                e.printStackTrace();
            }
            mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Log.e(TAG, "ERROR SLEEPING");
                e.printStackTrace();
            }

            for (BluetoothGattDescriptor descriptor:characteristic.getDescriptors()){
                Log.e(TAG, "BluetoothGattDescriptor: "+descriptor.getUuid().toString());
                Log.d(TAG, "setting enable notifiction value: " + descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE));
                if (descriptor.getUuid().toString().equals("00002902-0000-1000-8000-00805f9b34fb")) {
                    BluetoothGattDescriptor descriptorToBeRead = characteristic.getDescriptor(descriptor.getUuid());
                    mBluetoothGatt.readDescriptor(descriptorToBeRead);

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "ERROR SLEEPING");
                        e.printStackTrace();
                    }
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "ERROR SLEEPING");
                        e.printStackTrace();
                    }
                    mBluetoothGatt.writeDescriptor(descriptor);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "ERROR SLEEPING");
                        e.printStackTrace();
                    }

                } else if (descriptor.getUuid().toString().equals("00002901-0000-1000-8000-00805f9b34fb")) {
                    BluetoothGattDescriptor descriptorToBeRead = characteristic.getDescriptor(descriptor.getUuid());
                    mBluetoothGatt.readDescriptor(descriptorToBeRead);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "ERROR SLEEPING");
                        e.printStackTrace();
                    }
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "ERROR SLEEPING");
                        e.printStackTrace();
                    }
                    mBluetoothGatt.writeDescriptor(descriptor);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "ERROR SLEEPING");
                        e.printStackTrace();
                    }
                }
            }
        }

        if (UUID_CORE_SHOT.equals(characteristic.getUuid())) {
            Log.d(TAG, "CORE SHOT NOTIFICATION SETTING ON");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Log.e(TAG, "ERROR SLEEPING");
                    e.printStackTrace();
                }
                mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Log.e(TAG, "ERROR SLEEPING");
                    e.printStackTrace();
                }

            for (BluetoothGattDescriptor descriptor:characteristic.getDescriptors()){
                        Log.e(TAG, "BluetoothGattDescriptor: "+descriptor.getUuid().toString());
                        Log.d(TAG, "setting enable notifiction value: " + descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE));
                        if (descriptor.getUuid().toString().equals("00002902-0000-1000-8000-00805f9b34fb")) {
                            BluetoothGattDescriptor descriptorToBeRead = characteristic.getDescriptor(descriptor.getUuid());
                            mBluetoothGatt.readDescriptor(descriptorToBeRead);

                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                Log.e(TAG, "ERROR SLEEPING");
                                e.printStackTrace();
                            }
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                Log.e(TAG, "ERROR SLEEPING");
                                e.printStackTrace();
                            }
                            mBluetoothGatt.writeDescriptor(descriptor);
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                Log.e(TAG, "ERROR SLEEPING");
                                e.printStackTrace();
                            }

                        } else if (descriptor.getUuid().toString().equals("00002901-0000-1000-8000-00805f9b34fb")) {
                            BluetoothGattDescriptor descriptorToBeRead = characteristic.getDescriptor(descriptor.getUuid());
                            mBluetoothGatt.readDescriptor(descriptorToBeRead);
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                Log.e(TAG, "ERROR SLEEPING");
                                e.printStackTrace();
                            }
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                Log.e(TAG, "ERROR SLEEPING");
                                e.printStackTrace();
                            }
                            mBluetoothGatt.writeDescriptor(descriptor);
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                Log.e(TAG, "ERROR SLEEPING");
                                e.printStackTrace();
                            }
                        }
          }
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    /**
     * sends data to a characteristic
     *
     * @param characteristic Characteristic to write to
     */
    public void writeData(BluetoothGattCharacteristic characteristic) {
        boolean status = false;

        Log.d(TAG, "characteristic Uuid is: " + characteristic.getUuid().toString());
        try {
            BluetoothGattCharacteristic charac = characteristic;
            if (charac != null) {
                byte[] value = new byte[2];
                value[0] = (byte)(shotWriteType & 0xFF);
//                value[1] = (byte)(3 & 0xFF);

                charac.setValue(value);

                status = mBluetoothGatt.writeCharacteristic(charac);
                Log.d(TAG, "Write status is: " + status);
            } else {
                Log.e(TAG, "characteristic is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "exception thrown in writing to characteristic: " + e);
        }
    }

    private byte[] intToByteArray ( final int i ) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeInt(i);
            dos.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    public boolean writeToShotRequest(int byte1, int byte2) {
        UUID shotRequestUUID = UUID.fromString("0000fff5-0000-1000-8000-00805f9b34fb");
        UUID parameterStorageService = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
        BluetoothGattService service = new BluetoothGattService(shotRequestUUID, 0); //primary service type
        BluetoothGattCharacteristic probeMode = new BluetoothGattCharacteristic(shotRequestUUID, 8, 16);

        Log.e(TAG, "Trying to write to shot request");


        //check mBluetoothGatt is available
        if (mBluetoothGatt == null) {
            Log.e(TAG, "lost connection");
            return false;
        }
        BluetoothGattService Service = mBluetoothGatt.getService(parameterStorageService);
        if (Service == null) {
            Log.e(TAG, "service not found!");
            return false;
        }
        BluetoothGattCharacteristic charac = Service
                .getCharacteristic(shotRequestUUID);
        if (charac == null) {
            Log.e(TAG, "char not found!");
            return false;
        }

        byte[] value = new byte[2];
        value[0] = (byte) (byte1 & 0xFF); //TODO make this a variable we can change to select different shots
        value[1] = (byte) (byte2 & 0xFF);
//        value[0] = (byte) (shotWriteType);

        charac.setValue(value);
        Log.e(TAG, "Written: " + (char)value[0] + (char)value[1] + " to shot request");
        Log.e(TAG, "Written: " + Arrays.toString(value) + " to shot request");
        boolean status = mBluetoothGatt.writeCharacteristic(charac);
        return status;
    }

    public boolean writeToProbeMode(int mode) {
        UUID probeModeUUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
        UUID parameterStorageService = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
        BluetoothGattService service = new BluetoothGattService(probeModeUUID, 0); //primary service type
        BluetoothGattCharacteristic probeMode = new BluetoothGattCharacteristic(probeModeUUID, 8, 10);

        //check mBluetoothGatt is available
        if (mBluetoothGatt == null) {
            Log.e(TAG, "lost connection");
            return false;
        }
        BluetoothGattService Service = mBluetoothGatt.getService(parameterStorageService);
        if (Service == null) {
            Log.e(TAG, "service not found!");
            return false;
        }
        BluetoothGattCharacteristic charac = Service
                .getCharacteristic(probeModeUUID);
        if (charac == null) {
            Log.e(TAG, "char not found!");
            return false;
        }

        byte[] value = new byte[1];
        value[0] = (byte) (mode);
//        value[0] = (byte) (shotWriteType);
        charac.setValue(value);
        boolean status = mBluetoothGatt.writeCharacteristic(charac);
        return status;
    }

    public boolean writeToCalibrationIndex(byte data) {
        UUID calibrationIndexUUID = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb"); //Calibration index character
        UUID primaryServiceCharacteristicsService = UUID.fromString("0000ff00-0000-1000-8000-00805f9b34fb"); //calibration index service
        BluetoothGattService service = new BluetoothGattService(calibrationIndexUUID, 1); //secondary service type?
        BluetoothGattCharacteristic calibrationIndex = new BluetoothGattCharacteristic(calibrationIndexUUID, 8, 16);

        //check mBluetoothGatt is available
        if (mBluetoothGatt == null) {
            Log.e(TAG, "lost connection");
            return false;
        }
        BluetoothGattService Service = mBluetoothGatt.getService(primaryServiceCharacteristicsService);
        if (Service == null) {
            Log.e(TAG, "service not found!");
            return false;
        }
        BluetoothGattCharacteristic charac = Service
                .getCharacteristic(calibrationIndexUUID);
        if (charac == null) {
            Log.e(TAG, "char not found!");
            return false;
        }

        byte[] value = new byte[1];
//        value[0] = (byte) (calibrationIndexValue);
        value[0] = data;
        charac.setValue(value);
        boolean status = mBluetoothGatt.writeCharacteristic(charac);
//        Log.e(TAG, "Written: " + value + " to the calibration index");
        return status;
    }
}
