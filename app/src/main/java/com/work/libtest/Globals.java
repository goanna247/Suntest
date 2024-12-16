package com.work.libtest;

import com.work.libtest.Preferences.SimplePreferences;

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

    public static String probeConnectedBlackName = "";
    public static String probeConnectedBlackAddress = "";

    public static String probeConnectedWhiteName = "";
    public static String probeConnectedWhiteAddress = "";


    //Storage of previously done surveys to resume later, perhaps this should be stored locally to a file then read back?
    //If the app crashes this might delete this style of data storage
    public static LinkedList<LinkedList<DetailedMeasurement>> storedMeasurements = new LinkedList<>();

    public static boolean setNotification = false; //keep track of whether we have already set the probes notification values

    public static SimplePreferences simplePreferences = new SimplePreferences(true, true); //start with bore mode and 0-360 results

    public static boolean boreCamCalibrated = false;
}
