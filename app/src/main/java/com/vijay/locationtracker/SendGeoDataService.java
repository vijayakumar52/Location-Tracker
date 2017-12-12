/* $Id$ */
package com.vijay.locationtracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

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
import com.vijay.androidutils.NetworkUtils;
import com.vijay.androidutils.PrefUtils;
import com.vijay.locationtracker.firebase.Constants;
import com.vijay.locationtracker.firebase.MessagingService;


public class SendGeoDataService extends WakefulIntentService {
    private final String COUNTER = "counter";

    LocationManager locationManager;
    DatabaseReference trackingStatus;
    private static final String TAG = SendGeoDataService.class.getSimpleName();

    public SendGeoDataService() {
        super("SendLocation service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (NetworkUtils.isNetworkAvailable(this)) {
            trackingStatus = FirebaseDatabase.getInstance().getReference(Constants.TRACKING_STATUS);
            trackingStatus.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Boolean status = dataSnapshot.getValue(Boolean.class);
                    Logger.d(TAG, "onDataChange called : tracking = " + status);
                    if (status != null && status) {
                        sendCoordinates();
                    } else {
                        MessagingService.disableTracking(SendGeoDataService.this);
                        releaseWakeLock();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else {
            releaseWakeLock();
        }
        MessagingService.scheduleAlarm(this);
    }

    private void sendCoordinates() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            releaseWakeLock();
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
                        } else {
                            releaseWakeLock();
                        }
                    }
                });


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
                    releaseWakeLock();
                }
            });
            if (value >= Constants.MAX_COUNT) {
                value = -1;
            }

            PrefUtils.setPrefValueInt(this, COUNTER, value + 1);

        } else {
            releaseWakeLock();
            Logger.d(TAG, "Location null");
        }
    }
}
