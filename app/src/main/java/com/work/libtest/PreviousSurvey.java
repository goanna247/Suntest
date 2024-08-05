////////////////////////////////////////////////////////////////////////////////
/**
 * \file PreviousSurvey.java
 * \brief UNIMPLEMENTED
 * \author Anna Pedersen
 * \date Created: 07/06/2024
 *
 */
package com.work.libtest;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.work.libtest.SurveyOptions.AllSurveyOptionsActivity;

import java.util.ArrayList;

public class PreviousSurvey extends ListActivity {
    private String TAG = "Select Previous Survey";

    public static final String EXTRA_DEVICE_NAME = "Device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "Device_address";
    public static final String EXTRA_MEASUREMENT_TYPE = "New_old";


    private String mDeviceAddress;
    private String mDeviceName;

    private Menu menu;

    ArrayList<ArrayList<ProbeData>> probeData = new ArrayList<ArrayList<ProbeData>>();
    String numList [] = new String[(ProbeDataStorage.arrayListNum)+2];
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_previous_survey);

        Log.e(TAG, "Surveys size: " + ProbeDataStorage.probeDataTotal.size());
        //add the record num and the date to a string array to be displayed
        for (int i = 0; i < ProbeDataStorage.probeDataTotal.size(); i++) {
//            Log.e(TAG, "Probe data: " + ProbeDataStorage.probeDataTotal.get(i).get(0).returnData());
            if (i == 0) {
                numList[i] = Integer.valueOf(ProbeDataStorage.probeDataTotal.get(i).getSurveyNum()) + " : " + ProbeDataStorage.probeDataTotal.get(i).getDate();
            } else {
                if (ProbeDataStorage.probeDataTotal.get(i).getSurveyNum() != ProbeDataStorage.probeDataTotal.get(i-1).getSurveyNum()) {
                    numList[i] = Integer.valueOf(ProbeDataStorage.probeDataTotal.get(i).getSurveyNum()) + " : " + ProbeDataStorage.probeDataTotal.get(i).getDate();
                }
            }
        }

        listView = (ListView) findViewById(android.R.id.list);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.activity_list_view, R.id.survey_num, numList);
        listView.setAdapter(arrayAdapter);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Item clicked at " + position + " position");
                startNew(position);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_previous_survey, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.back_button) {
            Log.d(TAG, "Exit previous survey selection activity");
            back();
            return true;
        }
//        switch (item.getItemId()) {
//            case R.id.back_button:
//                Log.d(TAG, "Exit previous survey selection activity");
//                back();
//                return true;
//        }
        return true;
    }

    public void back() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_DEVICE_NAME, mDeviceName);
        intent.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        startActivity(intent);
    }

    public void startNew(int position) {
        Intent intent = new Intent(this, AllSurveyOptionsActivity.class);
        intent.putExtra(AllSurveyOptionsActivity.EXTRA_DEVICE_NAME, mDeviceName);
        intent.putExtra(AllSurveyOptionsActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(AllSurveyOptionsActivity.EXTRA_MEASUREMENT_TYPE, "0");
        startActivity(intent);
    }
}