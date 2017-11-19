package com.vijay.locationtracker.firebase;

import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * Created by vijay-3593 on 18/11/17.
 */

public class InstanceIdService extends FirebaseInstanceIdService {
    private static final String TAG = InstanceIdService.class.getSimpleName();

    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(refreshedToken);
    }

    private void sendRegistrationToServer(String refreshedToken) {
        DatabaseReference coordinatesInstance = FirebaseDatabase.getInstance().getReference(Constants.DEVICE_TOKEN);
        coordinatesInstance.setValue(refreshedToken);
        Log.d(TAG, "Refreshed token updated in server database");

    }
}
