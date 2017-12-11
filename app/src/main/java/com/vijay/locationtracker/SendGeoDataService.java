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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.google.firebase.database.ValueEventListener;
import com.vijay.androidutils.Logger;
import com.vijay.androidutils.PrefUtils;
import com.vijay.locationtracker.firebase.Constants;

import java.util.Locale;


public class SendGeoDataService extends Service implements ValueEventListener {
    private final String COUNTER = "counter";

    LocationManager locationManager;
    DatabaseReference trackingStatus;
    private static final String TAG = SendGeoDataService.class.getSimpleName();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Logger.d(TAG, "onBind called");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.d(TAG, "onStartCommand called");
        checkTrackingStatus();
        return super.onStartCommand(intent, flags, startId);
    }

    private void checkTrackingStatus() {
        trackingStatus = FirebaseDatabase.getInstance().getReference(Constants.TRACKING_STATUS);
        trackingStatus.addValueEventListener(this);
        Logger.d(TAG, "trackingStatus listener registered");
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        Boolean status = dataSnapshot.getValue(Boolean.class);
        Logger.d(TAG, "onDataChange called : tracking = " + status);
        if (status != null && status) {
            sendCoordinates();
        } else {
            TrackingService.disableTracking(SendGeoDataService.this);
            stopSelf();
        }
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.d(TAG, "onCreate called");
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

        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                sendData(location);
            }
        });

        mFusedLocationClient.getLastLocation()
                .addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Location mLastLocation = task.getResult();
                            sendData(mLastLocation);
                        }
                    }
                });


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service stopped");
    }

    private void sendData(Location location) {
        Logger.d(TAG, "Sending location");
        if (location != null) {
            double curLatitude = location.getLatitude();
            double curLongitude = location.getLongitude();
            long time = location.getTime();

            int value = PrefUtils.getPrefValueInt(this, COUNTER);
            if (value == -1) {
                value = 0;
            }

            DatabaseReference coordinates = FirebaseDatabase.getInstance().getReference(Constants.HISTORY);
            coordinates = coordinates.child(value + Constants.COORDINATE);

            LocationData data = new LocationData(curLatitude, curLongitude, time);
            coordinates.setValue(data, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    Logger.d(TAG, "Location updated.");
                }
            });
            if (value >= Constants.MAX_COUNT) {
                value = -1;
            }

            PrefUtils.setPrefValueInt(this, COUNTER, value + 1);

        } else {
            Logger.d(TAG, "Location null");
        }
    }
}
