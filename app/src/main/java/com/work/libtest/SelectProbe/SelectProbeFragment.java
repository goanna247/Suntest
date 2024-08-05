package com.work.libtest.SelectProbe;

import androidx.fragment.app.Fragment;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.view.View;
import android.os.Bundle;

import com.work.libtest.R;

import java.util.Set;

/**
 * A placeholder fragment containing a simple view.
 */
public class SelectProbeFragment extends Fragment {


    public SelectProbeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
//        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
//        if (bluetoothAdapter == null) {
//            //Device doesnt support bluetooth
//        }


        return inflater.inflate(R.layout.fragment_select_probe, container, false);

    }
}
