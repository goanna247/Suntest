package com.work.libtest;

public class CalibrationHelper {
    //code for types of calibration
    final static int accelerationCalibration = 0;
    final static int magnetometerCalibration = 1;

    //data for probe 8034
    static double[] acc_A = {0.011937163773, 0.012893257924, -0.003116180897};
    static double[][] acc_B = {{1.013031588232, -0.000458104561, -0.017655963792},
                        {-0.000221817802, 0.985095126590, -0.014332862531},
                        {0.002202949423, 0.000121806810, 1.014644287382}};
    static double[] acc_C = {-0.003631197768, -0.000071014301, -0.001783092792};

    static double[] mag_A = {5.138598318156, 3.830340637517, 0.419114553441};
    static double[][] mag_B = {{1.031963616381, 0.007417369502, -0.010468026784},
                        {-0.007523345738, 1.023504192373, 0.001960296904},
                        {-0.020739056408, 0.001083074780, 1.068824759828}};
    static double[] mag_C = {0.000000348296 ,0.000000367740, 0.000000178983};

    public static double[] CalibrationHelp(int calibrationType, double ux, double uy, double uz) {
        double[] returnValue = {0, 0, 0};
        switch (calibrationType) {
            case accelerationCalibration:
                returnValue[0] = acc_A[0] + (acc_B[0][0] * ux) + (acc_B[0][1] * uy) + (acc_B[0][2] * uz) + (acc_C[0] * ux * ux * ux); //dont really get why there are uy and uz in this but?
                returnValue[1] = acc_A[1] + (acc_B[1][0] * ux) + (acc_B[1][1] * uy) + (acc_B[1][2] * uz) + (acc_C[1] * uy * uy * uy);
                returnValue[2] = acc_A[2] + (acc_B[2][0] * ux) + (acc_B[2][1] * uy) + (acc_B[2][2] * uz) + (acc_C[2] * uz * uz * uz);
                break;
            case magnetometerCalibration:
                returnValue[0] = mag_A[0] + (mag_B[0][0] * ux) + (mag_B[0][1] * uy) + (mag_B[0][2] * uz) + (mag_C[0] * ux * ux * ux); //dont really get why there are uy and uz in this but?
                returnValue[1] = mag_A[1] + (mag_B[1][0] * ux) + (mag_B[1][1] * uy) + (mag_B[1][2] * uz) + (mag_C[1] * uy * uy * uy);
                returnValue[2] = mag_A[2] + (mag_B[2][0] * ux) + (mag_B[2][1] * uy) + (mag_B[2][2] * uz) + (mag_C[2] * uz * uz * uz);
                break;
        }
        return returnValue;
    }
}
