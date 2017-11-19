package com.vijay.locationtracker.firebase;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import com.vijay.locationtracker.TrackingService;

/**
 * Created by vijay-3593 on 18/11/17.
 */

public class MessagingService extends FirebaseMessagingService {
    private static final String TAG = InstanceIdService.class.getSimpleName();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Message received from "+ remoteMessage.getFrom());
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
        String enableTracking = message.get("tracking");
        String interval = message.get("interval");

        Log.d(TAG, "Message Details "+ "enableTracking :"+ enableTracking + " Interval : "+ interval);
        Intent intent = new Intent(this, TrackingService.class);

        if (enableTracking != null) {
            boolean enable = "true".equals(enableTracking);
            intent.putExtra(TrackingService.EXTRA_TRACKING_STATUS, enable);
        }

        if (interval != null) {
            intent.putExtra(TrackingService.EXTRA_ALARM_INTERVAL, interval);
        }
        startService(intent);
        Log.d(TAG, "Tracking service started with above details");
    }
}
