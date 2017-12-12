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
    private static final String TAG = WakefulIntentService.class.getSimpleName();
    public static final String
            LOCK_NAME_STATIC = "com.vijay.locaiontracker.Static";
    ;
    public static final String
            LOCK_NAME_LOCAL = "com.vijay.locaiontracker.Local";
    private static PowerManager.WakeLock lockStatic = null;
    private PowerManager.WakeLock lockLocal = null;

    public WakefulIntentService(String name) {
        super(name);
    }


    /**
     * Acquire a partial static WakeLock, you need too call this within the class
     * that calls startService()
     *
     * @param context
     */
    public static void acquireStaticLock(Context context) {
        getLock(context).acquire();
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
        getLock(this).release();
    }

    public void releaseWakeLock() {
        lockLocal.release();
        Logger.d(TAG, "Wakelock released");
    }

    public void acquireWakeLock() {
        lockLocal.acquire();
        Logger.d(TAG, "Wakelock acquired");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        releaseWakeLock();
    }

}