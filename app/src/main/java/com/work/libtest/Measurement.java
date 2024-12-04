////////////////////////////////////////////////////////////////////////////////
/**
 * \file Measurement.java
 * \brief Main activity of the app, manages what to do with the probe and collecting calibrating
 * \author Anna Pedersen
 * \date Created: 07/06/2024
 *
 * TODO - activity really needs to be cleaned up a bit
 */
package com.work.libtest;

import android.util.Log;

public class Measurement {
    private static final String TAG = "Measurement";

    private String measurementName; //basicaly serving as a record number right now
    private String date;
    private String time;
    private String temp;
    private String depth;
    private String dip;
    private String roll;
    private String azimuth;

    public Measurement(String measurementName, String date, String time, String temp,
                       String depth, String dip, String roll, String azimuth) {
        super();
        this.setMeasurementName(measurementName);
        this.setDate(date);
        this.setTime(time);
        this.setTemp(temp);
        this.setDepth(depth);
        this.setDip(dip);
        this.setRoll(roll);
        this.setAzimuth(azimuth);
    }

    public String getName() {
        return measurementName;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getTemp() {
        return temp;
    }

    public String getDepth() {
        return depth;
    }

    public String getRoll() {
        return roll;
    }

    public String getDip() {
        return dip;
    }

    public String getAzimuth() {
        return azimuth;
    }

    public void setMeasurementName(String measurementName) {
        this.measurementName = measurementName;
    }

    public void setAzimuth(String azimuth) {
        this.azimuth = azimuth;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setDepth(String depth) {
        this.depth = depth;
    }

    public void setDip(String dip) {
        this.dip = dip;
    }

    public void setRoll(String roll) {
        this.roll = roll;
    }

    public void setTemp(String temp) {
        this.temp = temp;
    }

    public void setTime(String time) {
        this.time = time;
    }


    public void printMeasurement() {
        Log.i(TAG, "Measurement name: " + this.measurementName + ", date: " + this.date + ", time: " + this.time +
                ", temp: " + this.temp + ", depth: " + this.depth + ", roll: " + this.roll + ", dip: " + this.dip + ",azimuth: "
                + this.azimuth);
    }
}
