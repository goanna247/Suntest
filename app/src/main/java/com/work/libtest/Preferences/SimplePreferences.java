package com.work.libtest.Preferences;

public class SimplePreferences {
    private static final String TAG = "Simple Preferences";

    private boolean boreMode; //mode can either be core or bore
    private boolean rollMode; //either (-180 to 180) or (0 to 360) (default)

    public SimplePreferences(boolean boreMode, boolean rollMode) {
        super();
        this.boreMode = boreMode;
        this.rollMode = rollMode;
    }

    public boolean getBoreMode() {
        return boreMode;
    }

    public boolean getRollMode() {
        return rollMode;
    }

    public void setBoreMode(boolean boreMode) {
        this.boreMode = boreMode;
    }

    public void setRollMode(boolean rollMode) {
        this.rollMode = rollMode;
    }
}
