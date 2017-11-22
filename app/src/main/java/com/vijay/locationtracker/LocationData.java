package com.vijay.locationtracker;

/**
 * Created by vijay-3593 on 22/11/17.
 */

public class LocationData {
    private double latitude;
    private double longitude;
    private long time;
    public LocationData(){

    }

    public LocationData(double latitude, double longitude, long time) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public long getTime() {
        return time;
    }
}
