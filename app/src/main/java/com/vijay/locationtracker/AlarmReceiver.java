package com.vijay.locationtracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by vijay-3593 on 18/11/17.
 */

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        WakefulIntentService.acquireStaticLock(context); //acquire a partial WakeLock
        context.startService(new Intent(context, SendGeoDataService.class)); //start SendLocationService
    }
}