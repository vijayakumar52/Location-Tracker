/* $Id$ */
package com.vijay.locationtracker;


import com.vijay.androidutils.AsyncTaskListener;

public interface ServerAPI {
    void sendCoordinates(String time, String latitude, String longitude, AsyncTaskListener asyncTaskListener);
    void clearHistory(AsyncTaskListener listener);
}
