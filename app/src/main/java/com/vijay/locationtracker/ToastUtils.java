package com.vijay.locationtracker;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by vijay-3593 on 19/11/17.
 */

public class ToastUtils {

    static void showToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }
}
