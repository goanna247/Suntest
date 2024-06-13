package com.work.libtest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.work.libtest.databinding.ActivityMainBinding;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private String TAG = "Main Activity";

    // Used to load the 'libtest' library on application startup.
    static {
        System.loadLibrary("libtest");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText(stringFromJNI());

        lMatrixCreate(5, 5); //create a 5 * 5 matrix
//        int[] entry = {1, 2, 3};
        String entry = "1,6,4,3,5:9:3:2:8";
//        try {
            lcrc8Begin(entry);
//        } catch (Exception e) {
//            Log.e(TAG, "Exception thrown attempting to call lcrc8Begin: " + e);
//        }
    }

    /**
     * A native method that is implemented by the 'libtest' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    public native void lMatrixCreate(int rows, int cols);

    //CRC
    public native int lcrc8Init(Object[] crc8, int poly, int initial_value, int final_xor);
    public native int lcrc8Begin(String crc8);
    public native int lcrc8Calc(Object[] crc8, Object[] buf, int size);
    public native int lcrc8End(Object[] crc8);
    public native void lcrc8Bitwise(Object[] crc8, Object[] buf, int size);

    public native int lcrc16Init();
    public native int lcrc16Begin();
    public native int lcrc16Calc();
    public native int lcrc16End();
    public native void lcrc16Bitwise();

    public native int lcrc32Init();
    public native int lcrc32Begin();
    public native int lcrc32Calc();
    public native int lcrc32End();
    public native void lcrc32Bitwise();

}