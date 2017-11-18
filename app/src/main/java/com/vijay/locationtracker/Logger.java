package com.vijay.locationtracker;

/**
 * Created by vijay-3593 on 19/11/17.
 */

import android.util.Log;

public class Logger {
    static boolean isLogEnabled = true;
    static StringBuffer logString = new StringBuffer();

    public static void d(String tag, String message) {
        if (isLogEnabled) {
            Log.d(tag, message);
        }
    }

    public static StringBuffer getLogString() {
        return logString;
    }
}
