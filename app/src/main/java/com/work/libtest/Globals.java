package com.work.libtest;

import java.util.LinkedList;

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

    public static String probeConnectedName = "";
    public static String probeConnectedAddress = "";


    //Storage of previously done surveys to resume later, perhaps this should be stored locally to a file then read back?
    //If the app crashes this might delete this style of data storage
    public static LinkedList<LinkedList<DetailedMeasurement>> storedMeasurements = new LinkedList<>();
}
