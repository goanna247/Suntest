package com.work.libtest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import androidx.appcompat.widget.Toolbar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.LinkedList;

public class ResumePrevious extends AppCompatActivity {

    public String TAG = "Resume Previous";
    public Menu menu;

    public String mDeviceName;
    public String mDeviceAddress;
    public String mConnectionStatus;

    public static final String EXTRA_DEVICE_NAME = "Device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "Device_address";
    public static final String EXTRA_DEVICE_CONNECTION_STATUS = "Connection_status";
    public static final String EXTRA_PROBE_MEASUREMENTS = "Measurements";
    
    private SurveyArrayAdapter surveyArrayAdapter;
    private ListView listView;

    LinkedList<DetailedMeasurement> previousSavedData = new LinkedList<>();

    public static File dir = new File(new File(Environment.getExternalStorageDirectory(), "Boreline"), "Boreline");

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1000:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission granted!");
                } else {
                    Log.d(TAG, "Permission not granted!");
                }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resume_previous);
        Toolbar toolbar = findViewById(R.id.toolbar);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        Log.i(TAG, "Name: " + mDeviceName + ", Address: " + mDeviceAddress);

        showAlert = new ShowAlertDialogs(this);

        Log.e(TAG, "Global saved surveys: " + Globals.storedMeasurements);

        for (int i = 0; i < Globals.storedMeasurements.size(); i++) {
            previousSavedData.add(Globals.storedMeasurements.get(i).get(0));
        }

        listView = (ListView) findViewById(R.id.listView);
        surveyArrayAdapter = new SurveyArrayAdapter(getApplicationContext(), R.layout.listitem_resume_survey);
        listView.setAdapter(surveyArrayAdapter);

        for (int i = 0; i < previousSavedData.size(); i++) {
            String date = previousSavedData.get(i).getBasicMeasurement().getDate();
            String time = previousSavedData.get(i).getBasicMeasurement().getTime();
            String holeID = previousSavedData.get(i).getHoleID();
            Measurement measurement = new Measurement(null, date, time, null, null, null, null, null);
            DetailedMeasurement newMeasurement = new DetailedMeasurement(measurement, null, null, holeID, null, null, null, null);
            surveyArrayAdapter.add(newMeasurement);
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                try {
                    Log.e(TAG, "Going to resume previous survey from position: " + position);
                    //go to View Measurements activity -> back should however go back to take measurements not to resume or main
                    goToView(position);


                } catch (Exception e) {
                    Log.e(TAG, "Exception thrown moving activities from old survey press: " + e);
                }
            }
        });
        setSupportActionBar(toolbar);
    }

    private void goToView(int positionNum) {
        Intent intent = new Intent(this, ViewMeasurements.class);
        intent.putExtra(ViewMeasurements.EXTRA_DEVICE_NAME, mDeviceName);
        intent.putExtra(ViewMeasurements.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(ViewMeasurements.EXTRA_SURVEY_TICKET, positionNum);
        intent.putExtra(ViewMeasurements.EXTRA_PARENT_ACTIVITY, "Resume");
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_view, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.back_button) {
            Log.d(TAG, "back button pressed");
            back();
            return true;
        } else if (item.getItemId() == R.id.remove_button) {
            //add remove all existing surveys WITH A POPUP
            removeAll();
        } else if (item.getItemId() == R.id.fetch_button) {
            //add fetch all surveys
            fetchAll();
        }
        return true;
    }

    private ShowAlertDialogs showAlert;

    //dangerous
    private void removeAll() {
        showAlert.removeAllSurveys(new Runnable() {
            @Override
            public void run() {
                //Remove all surveys DANGEROUS
                LinkedList<LinkedList<DetailedMeasurement>> emptyList = new LinkedList<>();
                Globals.storedMeasurements = emptyList;
                surveyArrayAdapter = new SurveyArrayAdapter(getApplicationContext(), R.layout.listitem_resume_survey);
                listView.setAdapter(surveyArrayAdapter);
            }
        });
    }

    //fetch all surveys
    private void fetchAll() {
        //Add everything to content then export
        String content = "";

        for (int i = 0; i < Globals.storedMeasurements.size(); i++) {
            //go through each survey
            content = content + "Survey Num: " + String.valueOf(i) + "\n";
            try {
                content = content + "Borecam - Precision Instrumentation\n";
                content = content + "Company Name " + Globals.storedMeasurements.get(i).get(0).getCompanyName() + "\n";
                content = content + "Operator Name " + Globals.storedMeasurements.get(i).get(0).getOperatorID() + "\n\n";


                content = content + "Probe ID,Hole,Measurement,Depth,Date,Time,DIntegrity,Azimuth,Dip,Roll,TotMag,Temperature\n"; //headings from measurement.java

                for (int j = 0; j < Globals.storedMeasurements.get(i).size(); j++) {
                    content = content + Globals.storedMeasurements.get(i).get(j).getProbeID();
                    content = content + "," + Globals.storedMeasurements.get(i).get(j).getHoleID();
                    content = content + "," + Globals.storedMeasurements.get(i).get(j).getDetailedName();
                    content = content + "," +  Globals.storedMeasurements.get(i).get(j).getBasicMeasurement().getDepth();
                    content = content + "," +  Globals.storedMeasurements.get(i).get(j).getBasicMeasurement().getDate();
                    content = content + "," +  Globals.storedMeasurements.get(i).get(j).getBasicMeasurement().getTime();
                    content = content + "," +  Globals.storedMeasurements.get(i).get(j).getDIntegrity();
                    content = content + "," +  Globals.storedMeasurements.get(i).get(j).getBasicMeasurement().getAzimuth();
                    content = content + "," +  Globals.storedMeasurements.get(i).get(j).getBasicMeasurement().getDip();
                    content = content + "," + Globals.storedMeasurements.get(i).get(j).getBasicMeasurement().getRoll();
                    content = content + "," + Globals.storedMeasurements.get(i).get(j).getTotMag();
                    content = content + "," +  Globals.storedMeasurements.get(i).get(j).getBasicMeasurement().getTemp();
                    content = content + "\n\n";
                }

                content = content + "DIntegrity Legend:\n";
                content = content + "D*                   Dip outside expected range\n";
                content = content + "M*                   Total magnetic field outside excpected range\n";
                content = content + "A*                   Azimuth outside expected range\n";
                content = content + "#                    Probe outside operating temperature\n";
                content = content + "!                    Probe moving during survey shot\n\n\n\n";
            } catch (Exception e) {
                Log.e(TAG, "Exception thrown in adding data to file to be saved: " + e);
            }
        }

        try {
            saveTextAsFile(content); //save all content collected
        } catch (Exception e) {
            Log.e(TAG, "failed to save: " + e);
        }
    }

    public void saveTextAsFile(String content) {
        String filename;
        Log.e(TAG, "SAVING TEXT AS FILE");
        Log.e(TAG, "CONTENT: " + content);

        askForPermissions();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                createDir();
            }
        }

        try {
            filename = String.valueOf(Globals.storedMeasurements.get(0).get(0).getBasicMeasurement().getDate());
            filename = filename + String.valueOf(Globals.storedMeasurements.get(0).get(0).getProbeID()); //just to make sure that it isn't inadvertantely adding the probe id to the date instead of as strings

            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + filename + ".csv");

            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
            Log.d(TAG, "Written to file");

            back();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown: " + e);
            e.printStackTrace();
        }
    }

    public void askForPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
                return;
            }
            createDir();
        }
    }

    public void createDir(){
        if (!dir.exists()){
            dir.mkdirs();
        }
    }

    private void back() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_DEVICE_NAME, mDeviceName);
        intent.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(MainActivity.EXTRA_PARENT_ACTIVITY, "SurveyOptions"); //change to correct name at somepoint but this should also work
        startActivity(intent);
    }
}