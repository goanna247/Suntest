package com.work.libtest.SurveyOptions;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

//import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.work.libtest.CoreMain;
import com.work.libtest.Globals;
import com.work.libtest.MainActivity;
import com.work.libtest.R;
import com.work.libtest.Survey;

//TODO if survey options already exist, it should autofill them
//TODO edit text should have a suggestion that goes away when clicked on, not just text
//TODO layout looks kinda messy :|

public class SurveyOptionsActivity extends AppCompatActivity {
    private static final String TAG = "SURVEY OPTIONS";

    EditText HoleIDEditTxt;
    EditText OperatorNameEditTxt;
    EditText CompanyNameEditTxt;

    public static final String EXTRA_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRA_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRA_DEVICE_CONNECTION = "DEVICE_CONNECTION_STATUS";

    public static final String EXTRA_SCAN_ADDRESS = "BLE_SCAN_DEVICE_ADDRESS";                      //Identifier for Bluetooth device address attached to Intent that returns a result
    public static final String EXTRA_SCAN_NAME = "BLE_SCAN_DEVICE_NAME";

    public static final String EXTRA_WHITE_NAME = "white_name";
    public static final String EXTRA_WHITE_ADDRESS = "white_address";
    public static final String EXTRA_BLACK_NAME = "black_name";
    public static final String EXTRA_BLACK_ADDRESS = "black_address";
    public static final String EXTRA_PARENT = "parent_activity";

    private String mDeviceName;
    private String mDeviceAddress;
    private String mDeviceConnectionStatus;

    private String mBlackDeviceName;
    private String mBlackDeviceAddress;
    private String mWhiteDeviceName;
    private String mWhiteDeviceAddress;
    private String mParentActivity;

//    private OnBackPressedCallback onBackPressedCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_options);
        Toolbar toolbar = findViewById(R.id.toolbar);

        HoleIDEditTxt = findViewById(R.id.holeIDInput);
        OperatorNameEditTxt = findViewById(R.id.operatorNameInput);
        CompanyNameEditTxt = findViewById(R.id.companyNameInput);

        final Intent intent = getIntent();
        mParentActivity = intent.getStringExtra(EXTRA_PARENT);
        if (mParentActivity.equals("Core")) {
            mBlackDeviceName = intent.getStringExtra(EXTRA_BLACK_NAME);
            mBlackDeviceAddress = intent.getStringExtra(EXTRA_BLACK_ADDRESS);
            mWhiteDeviceName = intent.getStringExtra(EXTRA_WHITE_NAME);
            mWhiteDeviceAddress = intent.getStringExtra(EXTRA_WHITE_ADDRESS);
        } else {
            mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
            mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
            mDeviceConnectionStatus = intent.getStringExtra(EXTRA_DEVICE_CONNECTION);
        }
        Log.e(TAG, "Passed in device name: " + mDeviceName);

        setSupportActionBar(toolbar);
    }

    public void surveyOptionsSubmit(View v) {
        try {
            if (HoleIDEditTxt != null || HoleIDEditTxt.equals(" ") || Integer.valueOf(HoleIDEditTxt.getText().toString()) != 0
                || OperatorNameEditTxt != null || OperatorNameEditTxt.equals(" ") || Integer.valueOf(OperatorNameEditTxt.getText().toString()) != 0
                || CompanyNameEditTxt != null || CompanyNameEditTxt.equals(" ") || Integer.valueOf(CompanyNameEditTxt.getText().toString()) != 0)
            {
                SurveyOptions newSurveyOptions = new SurveyOptions(HoleIDEditTxt.getText().toString(), OperatorNameEditTxt.getText().toString(), CompanyNameEditTxt.getText().toString());
                if (MainActivity.surveySize == MainActivity.surveyNum) {
                    //no survey options saved, create a new entire survey
                    Survey newSurvey = new Survey(newSurveyOptions);
                    MainActivity.surveys.add(newSurvey);
                    MainActivity.surveySize++;
                } else {
                    //survey already created, overwrite options
                    MainActivity.surveys.get(MainActivity.surveyNum).setSurveyOptions(newSurveyOptions);
                }
                if (mParentActivity.equals("Core")) {
                    Intent intent = new Intent(this, CoreMain.class);
                    intent.putExtra(CoreMain.EXTRA_BLACK_DEVICE_NAME, mBlackDeviceName);
                    intent.putExtra(CoreMain.EXTRA_BLACK_DEVICE_ADDRESS, mBlackDeviceAddress);
                    intent.putExtra(CoreMain.EXTRA_WHITE_DEVICE_NAME, mWhiteDeviceName);
                    intent.putExtra(CoreMain.EXTRA_WHITE_DEVICE_ADDRESS, mWhiteDeviceAddress);
                    intent.putExtra(CoreMain.EXTRA_PARENT_ACTIVITY, "SurveyOptions");
                    startActivity(intent);
                } else {
                    Intent returnToMain = new Intent(this, MainActivity.class);
                    returnToMain.putExtra(MainActivity.EXTRA_DEVICE_NAME, mDeviceName);
                    returnToMain.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
                    returnToMain.putExtra(MainActivity.EXTRA_PARENT_ACTIVITY, "SurveyOptions");
                    Log.e(TAG, "Name being passed back through are: " + returnToMain.toString());
                    startActivity(returnToMain);
                }
            } else {
                Log.d(TAG, "Please ensure all values are valid");
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception thrown: " + e);
        }
    }

    public void backButtonClick(View view) {
        if (mParentActivity.equals("Core")) {
            Intent intent = new Intent(this, CoreMain.class);
            intent.putExtra(CoreMain.EXTRA_BLACK_DEVICE_NAME, mBlackDeviceName);
            intent.putExtra(CoreMain.EXTRA_BLACK_DEVICE_ADDRESS, mBlackDeviceAddress);
            intent.putExtra(CoreMain.EXTRA_WHITE_DEVICE_NAME, mWhiteDeviceName);
            intent.putExtra(CoreMain.EXTRA_WHITE_DEVICE_ADDRESS, mWhiteDeviceAddress);
            intent.putExtra(CoreMain.EXTRA_PARENT_ACTIVITY, "SurveyOptions");
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_DEVICE_NAME, mDeviceName);
            intent.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
            intent.putExtra(MainActivity.EXTRA_PARENT_ACTIVITY, "SurveyOptions");
            startActivity(intent);
        }
    }
}
