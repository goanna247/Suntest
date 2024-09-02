////////////////////////////////////////////////////////////////////////////////
/**
 * \file ProbeDate.java
 * \brief Class to store probe data in various formats
 * \author Anna Pedersen
 * \date Created: 07/06/2024
 *
 * The various way probe data can be stored may be getting confusing, may need to migrate over to a
 * number of different classes or structs later
 */
package com.work.libtest;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class ProbeData implements Parcelable {
    private String TAG = "Probe Data";

    private int surveyNum; //counts the survey within the device

    //extra
    private int shotFormat;
    private int recordNumber;

    private String date;
    private String time;

    //orientation
    private double roll;
    private double dip;
    private double azimuth;
    private double orientation_temp;

    //accelerometer
    private double acc_x;
    private double acc_y;
    private double acc_z;
    private double acc_temp;
    private double acc_mag_error;

    //magnetometer
    private double mag_x;
    private double mag_y;
    private double mag_z;
    private double mag_temp;

    //max deviations / mean values
    private boolean show_mean_values;
    private double accelerometer;
    private double magnetometer;
    private double max_mean_accX;
    private double max_mean_accY;
    private double max_mean_accZ;
    private double max_mean_magX;
    private double max_mean_magY;
    private double max_mean_magZ;

    //options
    private boolean fast_mag_only;

    //Survey
    private String companyName;
    private String operatorName;
    private String probeID;
    private int holeID;
    private int measurementNumber;
    private String depth;

    //shot format 1
    public ProbeData(int survey_num, String _date, int shot_format, int record_number, double _roll, double _dip, double _orientation_temp, double _acc_x, double _acc_y,
                     double _acc_z, double _acc_temp, double _acc_mag_error, boolean _show_mean_values,
                     double _max_mean_accX, double _max_mean_accY, double _max_mean_accZ, boolean _fast_mag_only) {
        surveyNum = survey_num;
        date = _date;
        shotFormat = shot_format;
        recordNumber = record_number;
        roll = _roll;
        dip = _dip;
        orientation_temp = _orientation_temp;
        acc_x = _acc_x;
        acc_y = _acc_y;
        acc_z = _acc_z;
        acc_temp = _acc_temp;
        acc_mag_error = _acc_mag_error;
        show_mean_values = _show_mean_values;
        max_mean_accX = _max_mean_accX;
        max_mean_accY = _max_mean_accY;
        max_mean_accZ = _max_mean_accZ;
        fast_mag_only = _fast_mag_only;
    }

    //shot format 2 or 3
    public ProbeData(int survey_num, String _date, int _shotFormat, int _recordNumber, double _roll, double _dip, double _azimuth, double _orientation_temp, double _acc_x, double _acc_y,
                     double _acc_z, double _acc_mag_error, double _mag_x, double _mag_y, double _mag_z, double _mag_temp, double _acc_temp, boolean _show_mean_values,
                     double _accelerometer, double _max_mean_accX, double _max_mean_accY, double _max_mean_accZ, double _max_mean_magX, double _max_mean_magY, double _max_mean_magZ, boolean _fast_mag_only) {
        surveyNum = survey_num;
        date = _date;
        shotFormat = _shotFormat;
        recordNumber = _recordNumber;
        roll = _roll;
        dip = _dip;
        azimuth = _azimuth;
        orientation_temp = _orientation_temp;
        acc_x = _acc_x;
        acc_y = _acc_y;
        acc_z = _acc_z;
        acc_mag_error = _acc_mag_error;
        mag_x = _mag_x;
        mag_y = _mag_y;
        mag_z = _mag_z;
        mag_temp = _mag_temp;
        acc_temp = _acc_temp;
        show_mean_values = _show_mean_values;
        accelerometer = _accelerometer;
        max_mean_accX = _max_mean_accX;
        max_mean_accY = _max_mean_accY;
        max_mean_accZ = _max_mean_accZ;
        max_mean_magX = _max_mean_magX;
        max_mean_magY = _max_mean_magY;
        max_mean_magZ = _max_mean_magZ;
        fast_mag_only = _fast_mag_only;
    }

    //shot format 4
    public ProbeData(int survey_num, String company_name, String operator_name, String probe_ID, int hole_ID, int measurement_number, String _depth, String _date, String _time, int shot_format, int record_number, double _roll, double _dip, double _orientation_temp, double _acc_x, double _acc_y,
                     double _acc_z, double _acc_temp, double _acc_mag_error, boolean _show_mean_values,
                     double _max_mean_accX, double _max_mean_accY, double _max_mean_accZ, boolean _fast_mag_only) {
        surveyNum = survey_num;
        companyName = company_name;
        operatorName = operator_name;
        probeID = probe_ID;
        holeID = hole_ID;
        measurementNumber = measurement_number;
        depth = _depth;
        date = _date;
        time = _time;
        shotFormat = shot_format;
        recordNumber = record_number;
        roll = _roll;
        dip = _dip;
        orientation_temp = _orientation_temp;
        acc_x = _acc_x;
        acc_y = _acc_y;
        acc_z = _acc_z;
        acc_temp = _acc_temp;
        acc_mag_error = _acc_mag_error;
        show_mean_values = _show_mean_values;
        max_mean_accX = _max_mean_accX;
        max_mean_accY = _max_mean_accY;
        max_mean_accZ = _max_mean_accZ;
        fast_mag_only = _fast_mag_only;
    }



    //shot format 6 - shot format of 3 but in the survey format

    /**
     * TODO - Survey num needs to increment as surveys are taken in the app
     *
     */
    public ProbeData(int survey_num, String company_name, String operator_name, String probe_ID, int hole_ID, int measurement_number, String _depth, String _date, int _shotFormat, int _recordNumber, double _roll, double _dip, double _azimuth,
                     double _orientation_temp, double _acc_x, double _acc_y, double _acc_z, double _mag_x, double _mag_y, double _mag_z, boolean _show_mean_values,
                     double _max_mean_accX, double _max_mean_accY, double _max_mean_accZ, double _max_mean_magX, double _max_mean_magY, double _max_mean_magZ) {
        surveyNum = survey_num;
        companyName = company_name;
        operatorName = operator_name;
        probeID = probe_ID;
        holeID = hole_ID;
        measurementNumber = measurement_number;
        depth = _depth;
        date = _date;
        shotFormat = _shotFormat;
        recordNumber = _recordNumber;
        roll = _roll;
        dip = _dip;
        azimuth = _azimuth;
        orientation_temp = _orientation_temp;
        acc_x = _acc_x;
        acc_y = _acc_y;
        acc_z = _acc_z;
        mag_x = _mag_x;
        mag_y = _mag_y;
        mag_z = _mag_z;
        show_mean_values = _show_mean_values;
        max_mean_accX = _max_mean_accX;
        max_mean_accY = _max_mean_accY;
        max_mean_accZ = _max_mean_accZ;
        max_mean_magX = _max_mean_magX;
        max_mean_magY = _max_mean_magY;
        max_mean_magZ = _max_mean_magZ;
    }

    public String returnData() {
        String returnString;
        Log.e(TAG, "SHOT FORMAT: " + Integer.toString(shotFormat));
        if (shotFormat == 1) {
            returnString = surveyNum + "," + date + "," + Integer.toString(shotFormat) + "," +recordNumber + "," +roll + "," +dip + "," +orientation_temp +
                    "," +acc_x + "," +acc_y + "," +acc_z + "," +acc_temp + "," +acc_mag_error +
                    "," +show_mean_values + "," +max_mean_accX + "," +max_mean_accY + "," +max_mean_accZ + "," +fast_mag_only;
        } else if (shotFormat == 2) {
            returnString = "Type 2"; //TODO
        } else if (shotFormat == 3) {
            returnString = "7" + "," + date + "," + Integer.toString(shotFormat) + "," +recordNumber + "," +roll + "," +dip + "," + azimuth + "," + orientation_temp +
                    "," + acc_x + "," +acc_y + "," +acc_z + "," +acc_temp + "," +acc_mag_error + "," + mag_x + "," + mag_y + "," + mag_z + "," + mag_temp + "," +
                    "," + show_mean_values + "," +max_mean_accX + "," +max_mean_accY + "," +max_mean_accZ + "," + max_mean_magX + "," + max_mean_magY + "," + max_mean_magZ + "," + fast_mag_only;
            returnString.replace("null,", "");
        } else if (shotFormat == 4) {
            returnString = surveyNum + "," + companyName + "," +operatorName + "," +probeID + "," + holeID + "," +measurementNumber + "," + depth + "," + date + "," + Integer.toString(shotFormat) + "," +recordNumber + "," +roll + "," +dip + "," +orientation_temp +
                    "," +acc_x + "," +acc_y + "," +acc_z + "," +acc_temp + "," +acc_mag_error +
                    "," +show_mean_values + "," +max_mean_accX + "," +max_mean_accY + "," +max_mean_accZ + "," +fast_mag_only;
        } else if (shotFormat == 6) {
            returnString = surveyNum + "," + companyName + "," + operatorName + "," + probeID + "," + holeID + "," + measurementNumber + "," + depth + "," + date + "," + shotFormat + "," + recordNumber + "," + roll + "," + dip + "," + azimuth +
                    "," + orientation_temp + "," + acc_x + "," + acc_y + "," + acc_z + "," + mag_x + "," + mag_y + "," + mag_z + "," + show_mean_values + "," + max_mean_accX + "," + max_mean_accY + "," + max_mean_accZ + "," + max_mean_magX + "," + max_mean_magY + "," + max_mean_magZ;
        } else {
            returnString = "Error shot format is invalid";

        }
        Log.e(TAG, "RETURN STRING: " + returnString);
        return returnString;
    }

    public int getSurveyNum() {
        return surveyNum;
    }
    public int getShotType() {
        return shotFormat;
    }
    public int getRecordNumber() {
        return recordNumber;
    }
    public double getRoll() {
        return roll;
    }
    public double getDip() {
        return dip;
    }
    public double getOrientation_temp() {
        return orientation_temp;
    }
    public double getAcc_x() {
        return acc_x;
    }
    public double getAcc_y() {
        return acc_y;
    }
    public double getAcc_z() {
        return acc_z;
    }
    public double getAcc_temp() {
        return acc_temp;
    }
    public double getAcc_mag_error() {
        return acc_mag_error;
    }
    public boolean getShow_mean_values() {
        return show_mean_values;
    }
    public double getMax_mean_accX() {
        return max_mean_accX;
    }
    public double getMax_mean_accY() {
        return max_mean_accY;
    }
    public double getMax_mean_accZ() {
        return max_mean_accZ;
    }
    public boolean getFast_mag_only() {
        return fast_mag_only;
    }
    public String getDate() {
        return date;
    }
    public String getTime() { return time; }

    public String returnTitles() {
        String returnString;
        if (shotFormat == 1) {
            returnString = "Survey Num,Date,Date,Shot Format,Record Number,Roll,Dip,Orientation Temperature,Accelerometer X,Accelerometer Y,Accelerometer Z,Accelerometer Temperature,Acceleration Magnitude Error,Show Mean Values Boolean,Max Mean Acceleration X,Max Mean Acceleration Y, Max Mean Acceleration Z,Fast Mag Only Boolean";
        } else if (shotFormat == 2) {
            returnString = "todo"; //TODO
        } else if (shotFormat == 3) {
            returnString = "Survey Num,Date,Time,Shot Format,Record Number,Roll,Dip,Azimuth,Orientation Temp,Acc X,Acc Y,Acc Z,Acc Temp,Acc Mag Error,Mag X,Mag Y,Mag Z,Mag Temp,Show Mean Values,Max Mean AccX,Max Mean AccY,Max Mean AccZ,Max Mean MagX,Max Mean MagY,Max Mean MagZ,Fast Mag Only";
        } else if (shotFormat == 4) {
            returnString = "Survey Num,Company Name,OperatorName,Device ID,Hole ID,Measurement Num,Depth,Date,Shot Format,Record Number,Roll,Dip,Orientation Temperature,Accelerometer X,Accelerometer Y,Accelerometer Z,Accelerometer Temperature,Acceleration Magnitude Error,Show Mean Values Boolean,Max Mean Acceleration X,Max Mean Acceleration Y, Max Mean Acceleration Z,Fast Mag Only Boolean";
        } else if (shotFormat == 6) {
            returnString = "Survey Num,Company Name,OperatorName,Device ID,Hole ID,Measurement Num,Depth,Time,Date,Shot Format,Record Number,Roll,Dip,Azimuth,Orientation Temperature,Accelerometer X,Accelerometer Y,Accelerometer Z,Mag X,Mag Y,Mag Z,Show Mean Values,Max Mean Acceleration X,Max Mean Acceleration Y,Max Mean Acceleration Z,Max Mean Mag X,Max Mean Mag Y,Max Mean Mag Z";
        } else {
            returnString = "Error shot format is invalid";
        }

        return returnString;
    }

    protected ProbeData(Parcel in) {
        surveyNum = in.readInt();
        companyName = in.readString();
        operatorName = in.readString();
        probeID = in.readString();
        holeID = in.readInt();
        measurementNumber = in.readInt();
        depth = in.readString();
        date = in.readString();
        time = in.readString();
        shotFormat = in.readInt();
        recordNumber = in.readInt();
        roll = in.readDouble();
        dip = in.readDouble();
        azimuth = in.readDouble();
        orientation_temp = in.readDouble();
        acc_x = in.readDouble();
        acc_y = in.readDouble();
        acc_z = in.readDouble();
        acc_temp = in.readDouble();
        acc_mag_error = in.readDouble();
        mag_x = in.readDouble();
        mag_y = in.readDouble();
        mag_z = in.readDouble();
        mag_temp = in.readDouble();
        show_mean_values = in.readByte() != 0;
        accelerometer = in.readDouble();
        magnetometer = in.readDouble();
        max_mean_accX = in.readDouble();
        max_mean_accY = in.readDouble();
        max_mean_accZ = in.readDouble();
        max_mean_magX = in.readDouble();
        max_mean_magY = in.readDouble();
        max_mean_magZ = in.readDouble();
        fast_mag_only = in.readByte() != 0;
    }

    public static final Creator<ProbeData> CREATOR = new Creator<ProbeData>() {
        @Override
        public ProbeData createFromParcel(Parcel in) {
            return new ProbeData(in);
        }

        @Override
        public ProbeData[] newArray(int size) {
            return new ProbeData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(surveyNum);
        dest.writeString(companyName);
        dest.writeString(operatorName);
        dest.writeString(probeID);
        dest.writeInt(holeID);
        dest.writeInt(measurementNumber);
        dest.writeString(depth);
        dest.writeString(date);
        dest.writeString(time);
        dest.writeInt(shotFormat);
        dest.writeInt(recordNumber);
        dest.writeDouble(roll);
        dest.writeDouble(dip);
        dest.writeDouble(azimuth);
        dest.writeDouble(orientation_temp);
        dest.writeDouble(acc_x);
        dest.writeDouble(acc_y);
        dest.writeDouble(acc_z);
        dest.writeDouble(acc_temp);
        dest.writeDouble(acc_mag_error);
        dest.writeDouble(mag_x);
        dest.writeDouble(mag_y);
        dest.writeDouble(mag_z);
        dest.writeDouble(mag_temp);
        dest.writeByte((byte) (show_mean_values ? 1 : 0));
        dest.writeDouble(accelerometer);
        dest.writeDouble(magnetometer);
        dest.writeDouble(max_mean_accX);
        dest.writeDouble(max_mean_accY);
        dest.writeDouble(max_mean_accZ);
        dest.writeDouble(max_mean_magX);
        dest.writeDouble(max_mean_magY);
        dest.writeDouble(max_mean_magZ);
        dest.writeByte((byte) (fast_mag_only ? 1 : 0));
    }
}
