package com.work.libtest.Preferences;

public class SimplePreferences {
    private static final String TAG = "Simple Preferences";

    private boolean boreMode; //mode can either be core or bore (default)
    private boolean rollMode; //either (-180 to 180) or (0 to 360) (default)
    private int nominalValueMagneticField;
    private int maximumDeviationMagneticField;

    public SimplePreferences(boolean boreMode, boolean rollMode, int nomValue, int maxDev) {
        super();
        this.boreMode = boreMode;
        this.rollMode = rollMode;
        this.nominalValueMagneticField = nomValue;
        this.maximumDeviationMagneticField = maxDev;
    }

    public boolean getBoreMode() {
        return boreMode;
    }

    public boolean getRollMode() {
        return rollMode;
    }

    public int getNominalValueMagneticField() {
        return nominalValueMagneticField;
    }

    public int getMaximumDeviationMagneticField() {
        return maximumDeviationMagneticField;
    }

    public void setBoreMode(boolean boreMode) {
        this.boreMode = boreMode;
    }

    public void setRollMode(boolean rollMode) {
        this.rollMode = rollMode;
    }

    public void setNominalValueMagneticField(int nomValue) {
        this.nominalValueMagneticField = nomValue;
    }

    public void setMaximumDeviationMagneticField(int maxDev) {
        this.maximumDeviationMagneticField = maxDev;
    }
}
