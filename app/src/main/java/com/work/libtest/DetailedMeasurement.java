package com.work.libtest;

import android.util.Log;

public class DetailedMeasurement {
    private static final String TAG = "Detailed Measurement";

    private Measurement basicMeasurement;
    private String detailedName;
    private String probeID;
    private String holeID;
    private String companyName;
    private String operatorID;
    private String DIntegrity;
    private String totMag; //tots cool bro

    public DetailedMeasurement(Measurement basicMeasurement, String detailedName, String probeID, String holeID,
                               String companyName, String operatorID, String DIntegrity, String totMag) {
        super();
        this.setBasicMeasurement(basicMeasurement);
        this.setDetailedName(detailedName);
        this.setProbeID(probeID);
        this.setHoleID(holeID);
        this.setCompanyName(companyName);
        this.setOperatorID(operatorID);
        this.setDIntegrity(DIntegrity);
        this.setTotMag(totMag);
    }

    public Measurement getBasicMeasurement() {
        return basicMeasurement;
    }

    public String getDetailedName() {
        return detailedName;
    }

    public String getProbeID() {
        return probeID;
    }

    public String getHoleID() {
        return holeID;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getOperatorID() {
        return operatorID;
    }

    public String getDIntegrity() {
        return DIntegrity;
    }

    public String getTotMag() {
        return totMag;
    }

    public void setBasicMeasurement(Measurement basicMeasurement) {
        this.basicMeasurement = basicMeasurement;
    }

    public void setDetailedName(String detailedName) {
        this.detailedName = detailedName;
    }

    public void setProbeID(String probeID) {
        this.probeID = probeID;
    }

    public void setHoleID(String holeID) {
        this.holeID = holeID;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public void setOperatorID(String operatorID) {
        this.operatorID = operatorID;
    }

    public void setDIntegrity(String DIntegrity) {
        this.DIntegrity = DIntegrity;
    }

    public void setTotMag(String totMag) {
        this.totMag = totMag;
    }

    public void printDetailedMeasurement() {
//        basicMeasurement.printMeasurement();
//        Log.i(TAG, "Detailed name: " + this.detailedName + ", Probe ID: " + this.probeID + ", Hole ID: " + this.holeID +
//                ", Company Name: " + this.companyName + ",  Operator Name: " + this.operatorID + ", DIntegrity: " +
//                this.DIntegrity + ", Tot mag: " + this.totMag);
        Log.e(TAG, "ha lol you thought");
    }
}
