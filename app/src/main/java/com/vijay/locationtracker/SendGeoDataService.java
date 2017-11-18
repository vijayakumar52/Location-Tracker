/* $Id$ */
package com.vijay.locationtracker;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.vijay.locationtracker.firebase.Constants;


public class SendGeoDataService extends Service implements LocationListener {
    private final String COUNTER = "counter";

    LocationManager locationManager;
    private static final String TAG = SendGeoDataService.class.getSimpleName();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.d(TAG, "Location service started");
        sendCoordinates();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private void sendCoordinates() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        sendData(location);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ALARM SERVICE STOPPED");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "OnLocationChanged called");
        sendData(location);
        locationManager.removeUpdates(this);
    }

    private void sendData(Location location) {
        if (location != null) {
            double curLatitude = location.getLatitude();
            double curLongitude = location.getLongitude();
            long time = location.getTime();

            DatabaseReference recentCoordinate = FirebaseDatabase.getInstance().getReference(Constants.LAST_LOCATION);
            DatabaseReference lat = recentCoordinate.child(Constants.LATITUDE);
            DatabaseReference lon = recentCoordinate.child(Constants.LONGITUDE);
            DatabaseReference tim = recentCoordinate.child(Constants.TIME);

            lat.setValue(curLatitude);
            lon.setValue(curLongitude);
            tim.setValue(time);

            int value = PrefUtils.getPrefValueInt(this, COUNTER);
            if (value == -1) {
                value = 0;
            }

            DatabaseReference coordinates = FirebaseDatabase.getInstance().getReference(Constants.HISTORY);
            coordinates = coordinates.child(Constants.COORDINATE + value);
            DatabaseReference latReference = coordinates.child(Constants.LATITUDE);
            DatabaseReference lonReference = coordinates.child(Constants.LONGITUDE);
            DatabaseReference timeReference = coordinates.child(Constants.TIME);
            latReference.setValue(curLatitude);
            lonReference.setValue(curLongitude);
            timeReference.setValue(time);

            if (value >= 99) {
                value = -1;
            }

            PrefUtils.setPrefValueInt(this, COUNTER, value + 1);

            Logger.d(TAG, "Location updated.");
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
