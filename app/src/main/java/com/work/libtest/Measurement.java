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

public class Measurement {
    private static final String TAG = "Measurement";

    private String measurementName;
    private String date;
    private String time;
    private String temp;
    private String nanotesla; //wtf is this
    private String depth;
    private String dip;
    private String roll;
    private String azimuth;

    public Measurement(String measurementName, String date, String time, String temp, String nanotesla,
                       String depth, String dip, String roll, String azimuth) {
        super();
        this.setMeasurementName(measurementName);
        this.setDate(date);
        this.setTime(time);
        this.setTemp(temp);
        this.setNanotesla(nanotesla);
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

    public String getNanotesla() {
        return nanotesla;
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

    public void setNanotesla(String nanotesla) {
        this.nanotesla = nanotesla;
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


}
