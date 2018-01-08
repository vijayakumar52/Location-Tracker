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
    public static final String PREF_ALARM_STATUS = "alarmStatus";
    public static final String PREF_ALARM_INTERVAL = "alarmInterval";
    public static final String PREF_DURATION = "duration";
    public static final long DEFAULT_ALARM_INTERVAL = 60 * 1000;

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
        String trackingStatus = message.get(Constants.NOTIFICATION_TRACKING_STATUS);
        String alarmInterval = message.get(Constants.NOTIFICATION_ALARM_INTERVAL);
        String duration = message.get(Constants.NOTIFICATION_DURATION);

        Log.d(TAG, "Message Details " + "enableTracking :" + trackingStatus + " Interval : " + alarmInterval + " Duration : " + duration);

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
                PrefUtils.setPrefValueLong(this, PREF_ALARM_INTERVAL, timeInterval);
                FirebaseDatabase.getInstance().getReference(Constants.TRACKING_INFO).child(Constants.ALARM_INTERVAL).setValue(timeInterval);
            }
        }

        if (duration != null) {
            Long updatesDuration = null;
            try {
                updatesDuration = Long.parseLong(duration);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                Logger.d(TAG, "Duration not valid");
            }

            if (updatesDuration != null) {
                Logger.d(TAG, "Update duration : " + updatesDuration);
                PrefUtils.setPrefValueLong(this, PREF_DURATION, updatesDuration);
                FirebaseDatabase.getInstance().getReference(Constants.TRACKING_INFO).child(Constants.DURATION).setValue(updatesDuration);
                scheduleAlarm(this);
            }
        }
        if (trackingStatus != null) {
            boolean status = Boolean.parseBoolean(trackingStatus);
            if (status) {
                enableTracking(this);
                FirebaseDatabase.getInstance().getReference(Constants.TRACKING_STATUS).setValue(true);
            } else {
                disableTracking(this);
                FirebaseDatabase.getInstance().getReference(Constants.TRACKING_STATUS).setValue(false);
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

    public static void scheduleAlarm(Context context) {
        long interval = PrefUtils.getPrefValueLong(context, PREF_ALARM_INTERVAL);
        if (interval == -1) {
            interval = DEFAULT_ALARM_INTERVAL;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        PendingIntent pendingIntent = getAlarmPendingIntent(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
           /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + interval, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + interval, pendingIntent);
            }*/
            AlarmManager.AlarmClockInfo ac = new AlarmManager.AlarmClockInfo(System.currentTimeMillis() + interval, null);
            alarmManager.setAlarmClock(ac, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + interval, pendingIntent);
        }

        PrefUtils.setPrefValueBoolean(context, PREF_ALARM_STATUS, true);

        Log.d(TAG, "Tracking Enabled");
    }

    public static void disableTracking(Context context) {
        //if (isTrackingEnabled(context)) {

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        alarmManager.cancel(getAlarmPendingIntent(context));

        PrefUtils.setPrefValueBoolean(context, PREF_ALARM_STATUS, false);

        Log.d(TAG, "Tracking disabled");
    }


    private static PendingIntent getAlarmPendingIntent(Context context) {
        Intent serviceIntent = new Intent(context, AlarmReceiver.class);
        if (android.os.Build.VERSION.SDK_INT >= 16) {
            serviceIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }
        return PendingIntent.getBroadcast(context, PENDING_INTENT_CODE, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
