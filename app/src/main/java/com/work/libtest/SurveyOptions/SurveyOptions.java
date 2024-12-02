package com.work.libtest.SurveyOptions;

public class SurveyOptions {
    private String holeID;
    private String operatorName;
    private String companyName;
    private double initialDepth = 0;
    private double depthInterval = 5;

    public SurveyOptions(String pHoleID, String pOperatorName, String pCompanyName) {
        holeID = pHoleID;
        operatorName = pOperatorName;
        companyName = pCompanyName;
    }

    public SurveyOptions(String pHoleID, String pOperatorName, String pCompanyName, double pInitialDepth, double pDepthInterval) {
        holeID = pHoleID;
        operatorName = pOperatorName;
        companyName = pCompanyName;
        initialDepth = pInitialDepth;
        depthInterval = pDepthInterval;
    }

    //accessors
    public String getHoleID() {
        return holeID;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public double getInitialDepth() {
        return initialDepth;
    }

    public double getDepthInterval() {
        return depthInterval;
    }

    //mutators
    public void setHoleID(String pHoleID) {
        holeID = pHoleID;
    }

    public void setOperatorName(String pOperatorName) {
        operatorName = pOperatorName;
    }

    public void setCompanyName(String pCompanyName) {
        companyName = pCompanyName;
    }

    public void setInitialDepth(double pInitialDepth) {
        initialDepth = pInitialDepth;
    }

    public void setDepthInterval(double pDepthInterval) {
        depthInterval = pDepthInterval;
    }

    public String toString() {
        return "Hole ID: " + holeID + " Operator Name: " + operatorName + " Company Name: " + companyName + " Initial Depth: " + initialDepth + " Next Depth: " + depthInterval;
    }
}
