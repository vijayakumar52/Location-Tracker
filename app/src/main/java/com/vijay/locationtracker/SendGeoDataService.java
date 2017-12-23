/* $Id$ */
package com.vijay.locationtracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
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

    Intent currentIntent;
    DatabaseReference trackingStatus;
    FusedLocationProviderClient mFusedLocationProvider;
    LocationCallback mLocationCallback;

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private static final String TAG = SendGeoDataService.class.getSimpleName();

    public SendGeoDataService() {
        super("SendLocation service");
        setIntentRedelivery(true);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Logger.d(TAG, "onHandleIntent called");
        currentIntent = intent;
        if (NetworkUtils.isNetworkAvailable(this)) {
            trackingStatus = FirebaseDatabase.getInstance().getReference(Constants.TRACKING_STATUS);
            trackingStatus.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Boolean status = dataSnapshot.getValue(Boolean.class);
                    Logger.d(TAG, "onDataChange called : tracking = " + status);
                    if (status != null && status) {
                        startLocationUpdates();
                    } else {
                        MessagingService.disableTracking(SendGeoDataService.this);
                        SendGeoDataService.super.onHandleIntent(currentIntent);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else {
            SendGeoDataService.super.onHandleIntent(currentIntent);
        }
        MessagingService.scheduleAlarm(this);
    }

    private void startLocationUpdates() {
        //This case should not come here. You have to handle permissions before stating this service.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            SendGeoDataService.super.onHandleIntent(currentIntent);
            return;
        }

        //Initializing LocationRequest
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        //Initializing LocationCallback
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();
                sendData(location);
            }
        };

        mFusedLocationProvider = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationProvider.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                stopLocationUpdates();
            }
        },60000);
    }

    private void sendData(Location location) {
        Logger.d(TAG, "Sending location");
        if (location != null) {
            double curLatitude = location.getLatitude();
            double curLongitude = location.getLongitude();
            long time = location.getTime();

            String COUNTER = "counter";
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

    private void stopLocationUpdates() {
        if (mFusedLocationProvider != null) {
            // It is a good practice to remove location requests when the activity is in a paused or
            // stopped state. Doing so helps battery performance and is especially
            // recommended in applications that request frequent location updates.
            mFusedLocationProvider.removeLocationUpdates(mLocationCallback);
        }
        mFusedLocationProvider = null;
        SendGeoDataService.super.onHandleIntent(currentIntent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.d(TAG, "onCreate called");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.d(TAG, "onDestroy called");
    }
}
