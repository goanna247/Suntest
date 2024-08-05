package com.work.libtest;

import java.util.ArrayList;

public class ProbeDataStorage {
    public static String TAG = "Probe Data Storage";

    public static int arrayListNum = 0;
    public static ArrayList<ProbeData> probeDataTotal = new ArrayList<>();

//    public static void addArray(ArrayList<ProbeData> data) {
//        Log.e(TAG, "Adding: " + data.get(0).returnData());
//        probeDataTotal.add(data);
//        for (int i = 0; i < probeDataTotal.size(); i++) {
//            for (int j = 0; j < probeDataTotal.get(i).size(); j++) {
//                Log.e(TAG, "ARRAY: " + i + " DATA " + j + " IS: " + probeDataTotal.get(i).get(j).returnData());
//            }
//        }
//    }
//
//    public static int getArraySize() {
//        Log.e(TAG, "Size: " + probeDataTotal.size());
//        return probeDataTotal.size();
//    }
//
//    public static String getArrays(int i) {
//        String returnData = "";
//        returnData = Integer.toString(probeDataTotal.get(i).get(0).getSurveyNum()) + " : " + probeDataTotal.get(i).get(0).getDate();
//        Log.e(TAG, returnData);
//        return returnData;
//    }

}

