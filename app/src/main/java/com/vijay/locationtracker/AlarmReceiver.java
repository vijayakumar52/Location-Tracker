package com.vijay.locationtracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import com.vijay.locationtracker.firebase.MessagingService;

/**
 * Created by vijay-3593 on 18/11/17.
 */

public class AlarmReceiver extends BroadcastReceiver {
    public static final String
            LOCK_NAME_STATIC = "com.vijay.locaiontracker.Static";
    private static PowerManager.WakeLock lockStatic = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        //Schedule alarm again
        MessagingService.scheduleAlarm(context);
        acquireStaticLock(context); //acquire a partial WakeLock
        context.startService(new Intent(context, SendGeoDataService.class)); //start SendLocationService
    }

    /**
     * Acquire a partial static WakeLock, you need too call this within the class
     * that calls startService()
     *
     * @param context
     */
    public static void acquireStaticLock(Context context) {
        getLock(context).acquire(10 * 60 * 1000L /*10 minutes*/);
    }

    public static void releaseStaticLock(Context context) {
        PowerManager.WakeLock wakeLock = getLock(context);
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    synchronized private static PowerManager.WakeLock getLock(Context context) {
        if (lockStatic == null) {
            PowerManager
                    mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    LOCK_NAME_STATIC);
            lockStatic.setReferenceCounted(true);
        }
        return (lockStatic);
    }


}