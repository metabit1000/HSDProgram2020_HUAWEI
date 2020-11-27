package com.hsd.contest.spain.feelsafe;

import android.app.Application;

public class GlobalVariables extends Application {
    private static String telf = "112";

    public static String getTelf() {
        return telf;
    }

    public static void setTelf(String telf) {
        GlobalVariables.telf = telf;
    }
}
