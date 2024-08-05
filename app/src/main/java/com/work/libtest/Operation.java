////////////////////////////////////////////////////////////////////////////////
/**
 * \file Operation.java
 * \brief Class to store an operation to be performed on the probe, either write, read or notify
 * \author Anna Pedersen
 * \date Created: 07/06/2024
 *
 */
package com.work.libtest;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

public class Operation {
    private String TAG = "Operation";

    public static final int OPERATION_WRITE = 1;
    public static final int OPERATION_READ = 2;
    public static final int OPERATION_NOTIFY = 3;

    private BluetoothGattService _service;
    private BluetoothGattCharacteristic _characteristic;
    private int _action;
    private byte[] _data;

    /**
     * @param service - Bluetooth service
     * @param characteristic - Bluetooth characterisitic
     * @param action - Write(1), Read(2) or Notify(3)
     */
    Operation(BluetoothGattService service, BluetoothGattCharacteristic characteristic, int action) {
        _service = service;
        _characteristic = characteristic;
        _action = action;
    }

    /**
     * Set operation service UUID
     * @param service
     */
    public void setServiceUUID(BluetoothGattService service) {
        _service = service;
    }

    /**
     * Set operation characterisitic UUID
     * @param characteristic
     */
    public void setCharacterUUID(BluetoothGattCharacteristic characteristic) {
        _characteristic = characteristic;
    }

    /**
     * Set operation action
     * Write(1), Read(2), Notify(3)
     * @param action
     */
    public void setAction(int action) {
        _action = action;
    }

    /**
     * return service UUID
     * @return
     */
    public BluetoothGattService getService() {
        return _service;
    }

    /**
     * return characterisitic UUID
     * @return
     */
    public BluetoothGattCharacteristic getCharacteristic() {
        return _characteristic;
    }

    /**
     * return action being executed
     * @return
     */
    public int getAction() {
        return _action;
    }

    /**
     * print operation as a string
     * @return
     */
    public String toString() {
        String output = "Service: " + _service.getUuid().toString() + ", Characteristic: " + _characteristic.getUuid().toString() + ", Action: " + _action;
        return output;
    }
}
