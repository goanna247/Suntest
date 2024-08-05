package com.work.libtest;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public static String PRIMARY_SERVICE_CHARACTERISTICS = "0000ff00-0000-1000-8000-00805f9b34fb"; //TODO make actual thingo
    public static String PARAMETER_STORAGE_SERVICES = "0000fff0-0000-1000-8000-00805f9b34fb"; //TODO make actual thingo
    public static String UNKNOWN_PARAMETER = "00001800-0000-1000-8000-00805f9b34fb";
    public static String UNKNOWN_PARAMETER2 = "00001801-0000-1000-8000-00805f9b34fb";

    public static String MANUFACTURER_NAME = "00002a29-0000-1000-8000-00805f9b34fb";
    public static String DEVICE_ID = "0000fff1-0000-1000-8000-00805f9b34fb";
    public static String PROBE_MODE = "0000fff2-0000-1000-8000-00805f9b34fb";
    public static String SHOT_INTERVAL = "0000fff3-0000-1000-8000-00805f9b34fb";
    public static String RECORD_COUNT = "0000fff4-0000-1000-8000-00805f9b34fb"; //not used by app TODO make it not display
    public static String SHOT_REQUEST = "0000fff5-0000-1000-8000-00805f9b34fb";
    public static String SURVEY_MAX_SHOTS = "0000fff6-0000-1000-8000-00805f9b34fb";
    public static String BORE_SHOT = "0000fff8-0000-1000-8000-00805f9b34fb";
    public static String CORE_SHOT = "0000fff9-0000-1000-8000-00805f9b34fb";
    public static String DEVICE_ADDRESS = "0000fffb-0000-1000-8000-00805f9b34fb";
    public static String VERSION_MAJOR = "0000fffc-0000-1000-8000-00805f9b34fb";
    public static String VERSION_MINOR = "0000fffd-0000-1000-8000-00805f9b34fb";
    public static String ROLLING_SHOT_INTERVAL = "0000fffe-0000-1000-8000-00805f9b34fb";
    public static String CALIBRATION_INDEX = "0000ff01-0000-1000-8000-00805f9b34fb"; //WRITE TO TO GET CALIBRATION DATA
    public static String CALIBRATION_DATA = "0000ff02-0000-1000-8000-00805f9b34fb"; //READ AFTER TO GET CALIBRATION DATA

    static {
        // Sample Services.
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");

        attributes.put(PRIMARY_SERVICE_CHARACTERISTICS, "Primary service characteristics");
        attributes.put(PARAMETER_STORAGE_SERVICES, "Storage services");
        attributes.put(UNKNOWN_PARAMETER, "Unknown Parameter");
        attributes.put(UNKNOWN_PARAMETER2, "Unknown Parameter 2");

        // Sample Characteristics.
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");

        attributes.put(MANUFACTURER_NAME, "Manufacturer Name String");

        /**
         * n Bytes forming an Device ID string. The n bytes may not
         * have a null terminator. The Device ID is normally set by the
         * customer and is typically the probe serial number. The
         * Device ID is used as the name of the probe if it is available.
         */
        attributes.put(DEVICE_ID, "Device ID");

        /**
         * 1 Byte operating mode of the probe.
         * 0 = Idle (Probe not busy, ready to begin survey)
         * 1 = Recording Survey (Probe busy logging survey shots)
         * 2 = Real Time Mode (Probe is sending shots in real time)
         * Can be read to get the current mode of the probe.
         * Can be written to change the mode of the operating probe
         * for example to start or end a survey or to put it into real
         * time measurement mode (a.k.a. rolling shot).
         * When in real time mode, the probe will continually send
         * "Core Shot" or "Bore Shot" notifications containing the real
         * time data at the "Rolling Shot Interval". To prevent
         * excessive battery drain, the probe will drop out of mode 2
         * back to mode 0 after sending 1200 real time shots. To keep
         * the probe in real time mode, the app can reset real time
         * mode by writing 2 to the Probe Mode again before this
         * happens.
         * Two additional testing modes are available but not used
         * with the Android application:
         * 3 = Real Time No Magnetometer (same as mode 2 but
         * magnetometer sensor is not reported)
         * 4 = Real Time No Accelerometer (same as mode 2 but
         * accelerometer sensor is not reported)
         */
        attributes.put(PROBE_MODE, "Probe Mode");

        /**
         *  1 Byte shot interval. The number of seconds between
         * measurement recording in survey mode. Although this is a
         * writeable characteristic, the application never allows it to
         * be modified. The shot interval it purely dictated by the
         * probe. The Shot Interval x Survey Max Shots determines the
         * maximum duration of a survey.
         */
        attributes.put(SHOT_INTERVAL, "Shot Interval");

        /**
         *  2 byte value (low byte first). The number of measurements
         * (a.k.a. shots) that the probe has recorded. Not used by the
         * app. The app infers the number of measurements from the
         * survey duration and the Shot Interval.
         */
        attributes.put(RECORD_COUNT, "Record Count");

        /**
         *  2 byte value (low byte first). Requests a survey
         * measurement (a.k.a. shot) from the probe. Write the
         * desired shot number (index starting from 1) to this
         * characteristic. The probe will fetch the record from its
         * memory then send it back to the app as a "Bore Shot" or
         * "Core Shot" notification.
         */
        attributes.put(SHOT_REQUEST, "Shot request");

        /**
         *2 byte value (low byte first). Maximum number of
         * measurements that the probe can record when in survey
         * mode. Multiplying this by the Shot Interval gives the
         * maximum duration of a survey
         */
        attributes.put(SURVEY_MAX_SHOTS, "Survey Max Shots");

        /**
         *A block of data representing a single shot (or measurement
         * record) from a BoreCam probe, containing accelerometer
         * axial components, magnetometer axial components and
         * associated temperatures. The shot format is discussed in
         * detail later
         */
        attributes.put(BORE_SHOT, "Bore Shot");

        /**
         *A block of data representing a single shot (or measurement
         * record) from a CoreCam probe, containing accelerometer
         * axial components, magnetometer axial components and
         * associated temperatures. The shot format is discussed in
         * detail later.
         */
        attributes.put(CORE_SHOT, "Core Shot");



        /**
         * 6 byte MAC address of the probe. Sent in reverse order. If
         * Device Address characteristic is [0xAA, 0xBB, 0xCC, 0xDD,
         * 0xEE, 0xFF] then the MAC address is FF:EE:DD:CC:BB:AA
         */
        attributes.put(DEVICE_ADDRESS, "Device Address");

        /**
         * 1 byte major portion of the probe's firmware version
         * number
         */
        attributes.put(VERSION_MAJOR, "Version Major");

        /**
         * 1 byte minor portion of the probe's firmware version
         * number.
         */
        attributes.put(VERSION_MINOR, "Version Minor");

        /**
         * 2 byte value (low byte first). Approximate number of
         * milliseconds between rolling shots (real time measurements
         * when Probe Mode is set to 2). This value is used to
         * determine a timeout for real time probe measurements by
         * adding 40% (i.e. timeout = Rolling Shot Interval * 1.4). If
         * more than this amount of time has elapsed since the last
         * real time measurement was received, the application
         * assumes that the probe has stopped sending them and
         * updates the status on the "Real Time Orientation" and "Real
         * Time Sensors" pages
         */
        attributes.put(ROLLING_SHOT_INTERVAL, "Rolling Shot Interval");

        //FF00

        /**
         * 1 Byte block index (0-63). Selects the storage block number
         * that will be used by the Calibration Data characteristic
         */
        attributes.put(CALIBRATION_INDEX, "Calibration Index");

        /**
         * 16 bytes of data corresponding to the storage block
         * selected by the Calibration Index characteristic. Writing 16
         * bytes stores the data into the selected block. Reading
         * returns the 16 bytes stored in the selected block of the
         * probe
         */
        attributes.put(CALIBRATION_DATA, "Calibration Data");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
