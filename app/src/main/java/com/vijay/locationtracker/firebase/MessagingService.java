package com.vijay.locationtracker.firebase;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.vijay.androidutils.Logger;
import com.vijay.androidutils.PrefUtils;
import com.vijay.locationtracker.AlarmReceiver;
import com.vijay.locationtracker.MainActivity;
import com.vijay.locationtracker.R;

import java.util.Map;

/**
 * Created by vijay-3593 on 18/11/17.
 */

public class MessagingService extends FirebaseMessagingService {
    private final static String TAG = MessagingService.class.getSimpleName();
    public static final int PENDING_INTENT_CODE = 5257;
    public static final String ALARM_KEY = "alarmSet";
    public static final String ALARM_INTERVAL_KEY = "alarmInterval";
    public static final long DEFAULT_INTERVAL = 60 * 1000;

    public static String[] permissions = {android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION};


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Message received from " + remoteMessage.getFrom());
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleNow(remoteMessage.getData());
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }

    private void handleNow(Map<String, String> message) {
        String trackingStatus = message.get(Constants.NOTIFICATION_SET_TRACKING);
        String alarmInterval = message.get(Constants.NOTIFICATION_SET_INTERVAL);

        Log.d(TAG, "Message Details " + "enableTracking :" + trackingStatus + " Interval : " + alarmInterval);

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
                scheduleAlarm(this);
            }
        }
        if (trackingStatus != null) {
            boolean status = Boolean.parseBoolean(trackingStatus);
            if (status) {
                enableTracking(this);
            } else {
                disableTracking(this);
            }
        }

    }

    public static void enableTracking(Context context) {
        // if (forceEnable || !isTrackingEnabled(this)) {
        //Start Service
        LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!isGPSEnabled) {
            Logger.d(TAG, "GPS is not enabled. Opening settings screen now.");
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            //Caused by: android.util.AndroidRuntimeException: Calling startActivity()
            // from outside of an Activity  context requires the FLAG_ACTIVITY_NEW_TASK flag. Is this really what you want?

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            if (hasPermission(context)) {
                scheduleAlarm(context);
            } else {
                Logger.d(TAG, "Permission not acquired. throwing notification now.");
                throwNotification(context);
            }
        }
       /* } else {
            Logger.d(TAG, "Tracking service already enabled");
            ToastUtils.showToast(this, getResources().getString(R.string.toast_service_already_enabled));
        }*/
    }


    private static void throwNotification(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.REQUEST_PERMISSION, true);
        PendingIntent pIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);

        Notification n = new Notification.Builder(context)
                .setContentTitle("Permission Required!")
                .setContentText("Android needs permission for network access.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.granted, "Grant", pIntent)
                .addAction(R.drawable.denied, "Deny", pIntent).build();
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

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

    public static boolean hasPermission(Context context) {
        if (permissions.length > 0) {
            for (int i = 0; i < permissions.length; i++) {
                if (ActivityCompat.checkSelfPermission(context, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void scheduleAlarm(Context context) {
        long interval = PrefUtils.getPrefValueLong(context, ALARM_INTERVAL_KEY);
        if (interval == -1) {
            interval = DEFAULT_INTERVAL;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + interval, getAlarmPendingIntent(context));
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + interval, getAlarmPendingIntent(context));
        }

        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference intervalTime = firebaseDatabase.getReference(Constants.TIME_INTERVAL);
        intervalTime.setValue(interval);

        PrefUtils.setPrefValueBoolean(context, ALARM_KEY, true);

        DatabaseReference trackingStatus = firebaseDatabase.getReference(Constants.TRACKING_STATUS);
        trackingStatus.setValue(true);

        Log.d(TAG, "Tracking Enabled");
    }

    private static Intent getAlarmIntent(Context context) {
        return new Intent(context, AlarmReceiver.class);
    }

    private static PendingIntent getAlarmPendingIntent(Context context) {
        Intent serviceIntent = getAlarmIntent(context);
        return PendingIntent.getBroadcast(context, PENDING_INTENT_CODE, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static boolean isTrackingEnabled(Context context) {
        return PrefUtils.getPrefValueBoolean(context, ALARM_KEY);
    }

}
