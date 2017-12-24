package com.vijay.locationtracker;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import com.vijay.androidutils.Logger;

/**
 * Created by vijay-3593 on 18/11/17.
 */
public class WakefulIntentService extends IntentService {
    public static final String
            LOCK_NAME_LOCAL = "com.vijay.locaiontracker.Local";
    private PowerManager.WakeLock lockLocal = null;


    private static final String TAG = WakefulIntentService.class.getSimpleName();

    public WakefulIntentService(String name) {
        super(name);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        lockLocal = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                LOCK_NAME_LOCAL);
        lockLocal.setReferenceCounted(true);
    }

    @Override
    public void onStart(Intent intent, final int startId) {
        acquireWakeLock();
        super.onStart(intent, startId);
        AlarmReceiver.releaseStaticLock(this);
    }

    public void acquireWakeLock() {
        lockLocal.acquire(10 * 60 * 1000L /*10 minutes*/);
        Logger.d(TAG, "Wakelock acquired");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (lockLocal.isHeld()) {
            lockLocal.release();
            Logger.d(TAG, "Wakelock released");
        }
    }
}