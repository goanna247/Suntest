////////////////////////////////////////////////////////////////////////////////
/*
 * \file Preferences.java
 * \brief Object which stores all possible preferences
 * \author Anna Pedersen
 * \date Created: 07/06/2024
 *
 * includes necessary getters and setters
 */
package com.work.libtest.Preferences;

public class Preferences {

    //Operations
    private String mode;
    private boolean advancedMode;
    private boolean multiShotMode;
    private boolean depthTracking;
    private double initialDepth;
    private double depthInterval;

    //Alignment thresholds
    private double roll;
    private double dip;

    //movement alarm
    private boolean movementAlarmEnable;
    private double movementMaximumDeviation;

    //magnetic field alarm
    private boolean magneticFieldAlarmEnable;
    private double nominalMagnitude;
    private double magneticMaximumDeviation;

    //presentation
    private boolean showDip; //show dip during orientation
    private boolean showRoll; //show roll as -180 to 180
    private boolean audioAlerts;

    //about
    private double currentVersion;


    /**
     * Initalise a blank preferences object
     */
    public Preferences() {
        mode = "Bore Orientation (Single)";
        advancedMode = true;
        multiShotMode = true;
        depthTracking = true;
        initialDepth = 0;
        depthInterval = 10;

        roll = 0.5;
        dip = 0.5;

        movementAlarmEnable = true;
        movementMaximumDeviation = 0.010;

        magneticFieldAlarmEnable = false;
        nominalMagnitude = 58000;
        magneticMaximumDeviation = 5000;

        showDip = false;
        showRoll = false;
        audioAlerts = true;

        currentVersion = 1.0;
    }


    //accessors

    /**
     * @return probe mode
     */
    public String getMode() {
        return mode;
    }

    /**
     * @return whether the probe is in advanced mode
     */
    public boolean getAdvancedMode() {
        return advancedMode;
    }

    /**
     * @return get whether the probe is in multi shot mode
     */
    public boolean getmultiShotMode() {
        return multiShotMode;
    }

    /**
     * @return the depth tracking of the probe
     */
    public boolean getDepthTracking() {
        return  depthTracking;
    }

    /**
     * @return the initial depth of the probe
     */
    public double getInitialDepth() {
        return  initialDepth;
    }

    /**
     * @return the depth interval of the probe by default
     */
    public double getDepthInterval() {
        return  depthInterval;
    }

    /**
     * Get the roll value.
     * @return The roll value.
     */
    public double getRoll() {
        return  roll;
    }

    /**
     * Get the dip value.
     * @return The dip value.
     */
    public double getDip() {
        return  dip;
    }

    /**
     * Get the movement alarm enable status.
     * @return The movement alarm enable status.
     */
    public boolean getMovementEnable() {
        return  movementAlarmEnable;
    }

    /**
     * Get the movement maximum deviation.
     * @return The movement maximum deviation.
     */
    public double getMovementMaximumDeviation() {
        return movementMaximumDeviation;
    }

    /**
     * Get the magnetic field alarm enable status.
     * @return The magnetic field alarm enable status.
     */
    public boolean getMagneticEnable() {
        return magneticFieldAlarmEnable;
    }

    /**
     * Get the nominal magnitude.
     * @return The nominal magnitude.
     */
    public double getNominalMagnitude() {
        return  nominalMagnitude;
    }

    /**
     * Get the magnetic maximum deviation.
     * @return The magnetic maximum deviation.
     */
    public double getMagneticMaximumDeviation() {
        return  magneticMaximumDeviation;
    }

    /**
     * Get the show dip status.
     * @return The show dip status.
     */
    public boolean getShowDip() {
        return showDip;
    }

    /**
     * Get the show roll status.
     * @return The show roll status.
     */
    public boolean getShowRoll() {
        return  showRoll;
    }

    /**
     * Get the audio alerts status.
     * @return The audio alerts status.
     */
    public boolean getAudioAlerts() {
        return audioAlerts;
    }

    /**
     * Get the current version.
     * @return The current version.
     */
    public double getCurrentVersion() {
        return  currentVersion;
    }

    // Setters

    /**
     * Set the mode.
     * @param pMode The mode to set.
     */
    public void setMode(String pMode) {
        mode = pMode;
    }

    /**
     * Set the advanced mode.
     * @param pAdvancedMode The advanced mode to set.
     */
    public void setAdvancedMode(boolean pAdvancedMode) {
        advancedMode = pAdvancedMode;
    }

    /**
     * Set the multi-shot mode.
     * @param pMultiShotMode The multi-shot mode to set.
     */
    public void setMultiShotMode(boolean pMultiShotMode) {
        multiShotMode = pMultiShotMode;
    }

    /**
     * Set the depth tracking.
     * @param pDepthTracking The depth tracking to set.
     */
    public void setDepthTracking(boolean pDepthTracking) {
        depthTracking = pDepthTracking;
    }

    /**
     * Set the initial depth.
     * @param pInitialDepth The initial depth to set.
     */
    public void setInitialDepth(double pInitialDepth) {
        initialDepth = pInitialDepth;
    }

    /**
     * Set the depth interval.
     * @param pDepthInterval The depth interval to set.
     */
    public void setDepthInterval(double pDepthInterval) {
        depthInterval = pDepthInterval;
    }

    /**
     * Set the roll value.
     * @param pRoll The roll value to set.
     */
    public void setRoll(double pRoll) {
        roll = pRoll;
    }

    /**
     * Set the dip value.
     * @param pDip The dip value to set.
     */
    public void setDip(double pDip) {
        dip = pDip;
    }

    /**
     * Set the movement alarm enable status.
     * @param pMovementAlarmEnable The movement alarm enable status to set.
     */
    public void setMovementAlarmEnable(boolean pMovementAlarmEnable) {
        movementAlarmEnable = pMovementAlarmEnable;
    }

    /**
     * Set the movement maximum deviation.
     * @param pMovementMaximumDeviation The movement maximum deviation to set.
     */
    public void setMovementMaximumDeviation(double pMovementMaximumDeviation) {
        movementMaximumDeviation = pMovementMaximumDeviation;
    }

    /**
     * Set the magnetic field alarm enable status.
     * @param pMagneticFieldAlarmEnable The magnetic field alarm enable status to set.
     */
    public void setMagneticFieldAlarmEnable(boolean pMagneticFieldAlarmEnable) {
        magneticFieldAlarmEnable = pMagneticFieldAlarmEnable;
    }

    /**
     * Set the nominal magnitude.
     * @param pNominalMagnitude The nominal magnitude to set.
     */
    public void setNominalMagnitude(double pNominalMagnitude) {
        nominalMagnitude = pNominalMagnitude;
    }

    /**
     * Set the magnetic maximum deviation.
     * @param pMagneticMaxiumumDeviation The magnetic maximum deviation to set.
     */
    public void setMagneticMaximumDeviation(double pMagneticMaxiumumDeviation) {
        magneticMaximumDeviation = pMagneticMaxiumumDeviation;
    }

    /**
     * Set the show dip status.
     * @param pShowDip The show dip status to set.
     */
    public void setShowDip(boolean pShowDip) {
        showDip = pShowDip;
    }

    /**
     * Set the show roll status.
     * @param pShowRoll The show roll status to set.
     */
    public void setShowRoll(boolean pShowRoll) {
        showRoll = pShowRoll;
    }

    /**
     * Set the audio alerts status.
     * @param pAudioAlerts The audio alerts status to set.
     */
    public void setAudioAlerts(boolean pAudioAlerts) {
        audioAlerts = pAudioAlerts;
    }

    /**
     * Set the current version.
     * @param pCurrentVersion The current version to set.
     */
    public void setCurrentVersion(double pCurrentVersion) {
        currentVersion = pCurrentVersion;
    }
}