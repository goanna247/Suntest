package com.work.libtest;

public class Globals {
    /**
     * whether the app will collect and use calibration data from the probe,
     * should be true for release
     */
    public static boolean enableCalibration = true;
    /**
     * Whether the app has already collected the calibration matrix
     */
    public static boolean caliDataCollected = false;


    public static enum ActivityName {
        AllSurveyOptions,
        SurveyOptions,
        Scan,
        Main,
        Orientation,
        Sensor,
        ProbeDetails,
        TakeMeasurement,
        ViewMeasurement
    }
}
