/* $Id$ */
package com.vijay.locationtracker;


import android.util.Log;

import com.vijay.androidutils.AsyncTaskListener;

public class ServerAPIImpl implements ServerAPI {
    private static final String TAG = ServerAPIImpl.class.getSimpleName();
    private static ServerAPIImpl serverAPI = new ServerAPIImpl();

    public static ServerAPIImpl getServerAPI() {
        return serverAPI;
    }

    @Override
    public void sendCoordinates(String time, String latitude, String longitude, AsyncTaskListener asyncTaskListener) {
        Log.d(TAG, "Sending coordinates");
        NetworkManager.getInstance().addRecord(time,latitude,longitude,asyncTaskListener);
    }

    @Override
    public void clearHistory(AsyncTaskListener listener) {
        Log.d(TAG,"Clearing Previous History");
        NetworkManager.getInstance().deleteRecord(listener);
    }


}
