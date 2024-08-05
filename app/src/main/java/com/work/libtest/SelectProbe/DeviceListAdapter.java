package com.work.libtest.SelectProbe;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.work.libtest.R;

import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class DeviceListAdapter extends ArrayAdapter<BluetoothDevice> {
    private LayoutInflater mLayoutInflater;
    private ArrayList<BluetoothDevice> mDevices;
    private int mViewResourceId;

    public static ArrayList<Integer> greenPositions;
    public static ArrayList<Integer> orangePositions;
    public static ArrayList<Integer> redPositions;

    public DeviceListAdapter(Context context, int tvResourceId, ArrayList<BluetoothDevice> devices) {
        super(context, tvResourceId, devices);
        this.mDevices = devices;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mViewResourceId = tvResourceId;
    }


    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = mLayoutInflater.inflate(mViewResourceId, null);

        BluetoothDevice device = mDevices.get(position);
        if (device != null) {
            TextView deviceName = (TextView) convertView.findViewById(R.id.tvDeviceName);
            TextView deviceAddress = (TextView) convertView.findViewById(R.id.tvDeviceAddress);

            Log.d("Device list array", Integer.toString(position));

            if (deviceName != null) {
                deviceName.setText(device.getName());
//                for (int i = 0; i <= greenPositions.size(); i++) {
//                    if (greenPositions.get(i) == position) {
//                        deviceName.setTextColor(Color.parseColor("#2b7d1d"));
//                    }
//                }
            }
            if (deviceAddress != null) {
                deviceAddress.setText(device.getAddress());
            }
        }

        return convertView;
    }

    /**
     *
//     * @param arrayPos is the position of the array the device being connected to is, starts from 0
     */
    public static void setGreen() {
//        TextView deviceName = findViewById(R.id.tvDeviceName);
//        deviceName.setTextColor(Color.parseColor("#2b7d1d"));
    }

    public void setOrange(int position, View convertView) {
        convertView = mLayoutInflater.inflate(mViewResourceId, null);

        BluetoothDevice device = mDevices.get(position);
        if (device != null) {
            TextView deviceName = (TextView) convertView.findViewById(R.id.tvDeviceName);

            if (deviceName != null) {
                deviceName.setTextColor(Color.parseColor("#2b7d1d"));
            }
        }
    }

    public void setRed() {

    }

}
