package com.vijay.locationtracker;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.vijay.locationtracker.firebase.Constants;

/**
 * Created by vijay-3593 on 18/11/17.
 */

public class TrackingService extends Service {

    private final static String TAG = TrackingService.class.getSimpleName();
    public static final int PENDING_INTENT_CODE = 5257;
    public static final String ALARM_KEY = "alarmSet";
    public static final String ALARM_INTERVAL_KEY = "alarmInterval";
    public static final long DEFAULT_INTERVAL = 60 * 1000;
    public final long DEFAULT_START_INTERVAL = 60 * 1000;
    public static final String EXTRA_TRACKING_STATUS = "trackingStatus";
    public static final String EXTRA_ALARM_INTERVAL = "alarmInterval";

    public static String[] permissions = {android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION};

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Logger.d(TAG, "onBind called");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.d(TAG, "onCreate called");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.d(TAG, "onStartCommand called");
        if (intent != null) {
            String trackingStatus = intent.getStringExtra(EXTRA_TRACKING_STATUS);
            String alarmInterval = intent.getStringExtra(EXTRA_ALARM_INTERVAL);

            if (alarmInterval != null) {
                Long timeInterval = null;
                try {
                    timeInterval = Long.parseLong(alarmInterval);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Logger.d(TAG, "Time interval is not valid");
                }

                if (timeInterval != null) {
                    Logger.d(TAG, "Updating interval time : " + timeInterval);
                    PrefUtils.setPrefValueLong(this, ALARM_INTERVAL_KEY, timeInterval);
                    enableTracking(true);
                }
            }
            if (trackingStatus != null) {
                boolean status = Boolean.parseBoolean(trackingStatus);
                if (status) {
                    enableTracking(false);
                } else {
                    disableTracking(this);
                }
            }
        }
        stopSelf();
        return super.onStartCommand(intent, flags, startId);
    }

    public void enableTracking(boolean forceEnable) {
        // if (forceEnable || !isTrackingEnabled(this)) {
        //Start Service
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!isGPSEnabled) {
            Logger.d(TAG, "GPS is not enabled. Opening settings screen now.");
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        } else {
            if (hasPermission()) {
                scheduleAlarm();
            } else {
                Logger.d(TAG, "Permission not acquired. throwing notification now.");
                throwNotification();
            }
        }
       /* } else {
            Logger.d(TAG, "Tracking service already enabled");
            ToastUtils.showToast(this, getResources().getString(R.string.toast_service_already_enabled));
        }*/
    }

    private void throwNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.REQUEST_PERMISSION, true);
        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);

        Notification n = new Notification.Builder(this)
                .setContentTitle("Permission Required!")
                .setContentText("Android needs permission for network access.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.granted, "Grant", pIntent)
                .addAction(R.drawable.denied, "Deny", pIntent).build();
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(0, n);
        Logger.d(TAG, "Notification shown regarding permission");
    }

    public static void disableTracking(Context context) {
        //if (isTrackingEnabled(context)) {

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        alarmManager.cancel(getAlarmPendingIntent(context));

        PrefUtils.setPrefValueBoolean(context, ALARM_KEY, false);

        //Set DB value in cloud
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference trackingStatus = firebaseDatabase.getReference(Constants.TRACKING_STATUS);
        trackingStatus.setValue(false);

        Log.d(TAG, "Tracking disabled");
        /*} else {
            ToastUtils.showToast(context, context.getResources().getString(R.string.toast_service_already_disabled));
        }*/
    }

    public boolean hasPermission() {
        if (permissions.length > 0) {
            for (int i = 0; i < permissions.length; i++) {
                if (ActivityCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void scheduleAlarm() {
        long interval = PrefUtils.getPrefValueLong(this, ALARM_INTERVAL_KEY);
        if (interval == -1) {
            interval = DEFAULT_INTERVAL;
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, DEFAULT_START_INTERVAL, interval, getAlarmPendingIntent(this));

        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference intervalTime = firebaseDatabase.getReference(Constants.TIME_INTERVAL);
        intervalTime.setValue(interval);

        PrefUtils.setPrefValueBoolean(this, ALARM_KEY, true);

        DatabaseReference trackingStatus = firebaseDatabase.getReference(Constants.TRACKING_STATUS);
        trackingStatus.setValue(true);

        ToastUtils.showToast(this, getResources().getString(R.string.toast_alarm_set_for_interval) + interval);
        Log.d(TAG, "Tracking Enabled");
    }

    private static Intent getAlarmIntent(Context context) {
        return new Intent(context, SendGeoDataService.class);
    }

    private static PendingIntent getAlarmPendingIntent(Context context) {
        Intent serviceIntent = getAlarmIntent(context);
        return PendingIntent.getService(context, PENDING_INTENT_CODE, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static boolean isTrackingEnabled(Context context) {
        return PrefUtils.getPrefValueBoolean(context, ALARM_KEY);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.d(TAG, "onBind called");
    }
}
