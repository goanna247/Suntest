package com.work.libtest;


// sensorData is effectively just a C++ structure to group all data for a reading together
// This is the raw data from the shot, but unpacked into floating point
//
// also handles the report formatting, to avoid having to pass all the data around (for now)
public class sensorData {
    int rec_type;
    int rec_num;
    String rec_time;   // "yyyy-MM-dd HH:mm:ss"
    double roll, dip, azimuth;

    double ax_cal;
    double ay_cal;
    double az_cal;
    double at;
    double a_error;

    double mx_cal;
    double my_cal;
    double mz_cal;
    double mt;

    double probe_temp;

    double ax_uncal;
    double ay_uncal;
    double az_uncal;
    double mx_uncal;
    double my_uncal;
    double mz_uncal;

    public sensorData(int rec_type, int rec_num, String rec_time, double roll, double dip, double azimuth,
                      double ax_cal, double ay_cal, double az_cal, double at, double a_error,
                      double mx_cal, double my_cal, double mz_cal, double mt, double probe_temp,
                      double ax_uncal, double ay_uncal, double az_uncal, double mx_uncal, double my_uncal, double mz_uncal) {
        super();
        this.rec_type = rec_type;
        this.rec_num = rec_num;
        this.rec_time = rec_time;
        this.roll = roll;
        this.dip = dip;
        this.azimuth = azimuth;

        this.ax_cal = ax_cal;
        this.ay_cal = ay_cal;
        this.az_cal = az_cal;
        this.at = at;
        this.a_error = a_error;

        this.mx_cal = mx_cal;
        this.my_cal = my_cal;
        this.mz_cal = mz_cal;
        this.mt = mt;
        this.probe_temp = probe_temp;

        this.ax_uncal = ax_uncal;
        this.ay_uncal = ay_uncal;
        this.az_uncal = az_uncal;
        this.mx_uncal = mx_uncal;
        this.my_uncal = my_uncal;
        this.mz_uncal = mz_uncal;

    }

    public String getReportHeader() {
        //return("Num,Time,Roll,Pitch,Azimuth,Acc-X,Acc-Y,Acc-Z,Acc-T,Acc-Error,Mag-X,Mag-Y,Mag-Z,Mag-T,Probe-T,AccUnc-X,AccUnc-Y,AccUnc-Z,MagUnc-X,MagUnc-Y,MagUnc-Z");
        return(" Num,      Time,              Roll,      Pitch,     Azimuth,      Acc-X,           Acc-Y,           Acc-Z,      Acc-T,  Acc-Error,        Mag-X,          Mag-Y,           Mag-Z,      Mag-T, Probe-T,   AccUnc-X,        AccUnc-Y,        AccUnc-Z,        MagUnc-X,        MagUnc-Y,        MagUnc-Z");
    }
    public String getReportLine() {
        //String line = String.format("% 5d,% 20s,% 10.4f,% 10.4f,% 10.4f", rec_num, rec_time, roll, dip, azimuth); - BOOM
        //String line = String.format("% 5d,%20s,% 10.4f,% 10.4f,% 10.4f,% 16.12f,% 16.12f,% 16.12f,%6.1f,% 12.8f,% 16.12f,% 16.12f,% 16.12f,%6.1f,%6.1f", rec_num, rec_time, roll, dip, azimuth, ax_cal, ay_cal, az_cal, at, a_error, mx_cal, my_cal, mz_cal, mt, probe_temp);
        String line = String.format("% 5d,%20s,% 10.4f,% 10.4f,% 10.4f,% 16.12f,% 16.12f,% 16.12f,%6.1f,% 12.8f,% 16.12f,% 16.12f,% 16.12f,%6.1f,%6.1f,% 16.12f,% 16.12f,% 16.12f,% 16.12f,% 16.12f,% 16.12f", rec_num, rec_time, roll, dip, azimuth, ax_cal, ay_cal, az_cal, at, a_error, mx_cal, my_cal, mz_cal, mt, probe_temp, ax_uncal, ay_uncal, az_uncal, mx_uncal, my_uncal, mz_uncal);
        return(line); //String.format("% 5d,% 20s,% 10.4f,% 10.4f,% 10.4f,% 16.12f,% 16.12f,% 16.12f,%6.1f,% 12.8f,% 16.12f,% 16.12f,% 16.12f,%6.1f,%6.1f,% 16.12f,% 16.12f,% 16.12f,% 16.12f,% 16.12f,% 16.12f",
        //rec_num, time, roll, dip, azimuth, ax_cal, ay_cal, az_cal, at, a_error,
        // mx_cal, my_cal, mz_cal, mt, probe_temp, ax_uncal, ay_uncal, az_uncal, mx_uncal, my_uncal, mz_uncal));
    }
}
