/*$Id$ */
package com.vijay.locationtracker;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.view.View;
import android.view.WindowManager;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

public class DialogUtils {
    static DialogUtils dialogUtils = new DialogUtils();

    public static DialogUtils getInstance() {
        return dialogUtils;
    }

    MaterialDialog materialDialog;


    public static boolean isAttachedToWindow(View hostView) {
        if (Build.VERSION.SDK_INT >= 19) {
            return hostView.isAttachedToWindow();
        } else {
            return (hostView.getHandler() != null);
        }
    }

    public void dismissDialog() {
        if (materialDialog != null && materialDialog.isShowing()) {
            if (materialDialog.getWindow() != null) {
                View view = materialDialog.getWindow().getDecorView();
                if (isAttachedToWindow(view)) {
                    materialDialog.dismiss();
                } else {
                    materialDialog = null;
                }
            } else {
                materialDialog = null;
            }
        } else {
            materialDialog = null;
        }
    }

    public MaterialDialog getDialog() {
        return materialDialog;
    }

    public void singleButtonDialog(Activity activity, CharSequence title, CharSequence content, String positiveButtonMessage,
                                   boolean isCancellable, final MaterialDialog.SingleButtonCallback buttonCallback,
                                   MaterialDialog.OnDismissListener dismissListener) {
        twoButtonDialog(activity, title, content, positiveButtonMessage, null, isCancellable, buttonCallback, dismissListener);
    }

    public void twoButtonDialog(Activity activity, CharSequence title, CharSequence content, String positiveButtonMessage, String negativeButtonMessage, boolean isCancellable,
                                final MaterialDialog.SingleButtonCallback buttonCallback, android.content.DialogInterface.OnDismissListener dismissListener) {
        showDialog(activity, title, content, null, false, null, positiveButtonMessage, negativeButtonMessage, isCancellable, buttonCallback, null, dismissListener);
    }

    public void editTextDialog(Activity activity, String title, String hint, String prefillText, String positiveButtonMsg, String negativeButtonMsg, boolean autoDismiss, final MaterialDialog.SingleButtonCallback dialogCallback) {

        final MaterialDialog builder = new MaterialDialog.Builder(activity)
                .customView(R.layout.edittext, true)
                .positiveText(positiveButtonMsg).onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialogCallback.onClick(dialog, which);
                    }
                })
                .negativeText(negativeButtonMsg).onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialogCallback.onClick(dialog, which);
                    }
                })
                .title(title)
                .canceledOnTouchOutside(autoDismiss)
                .build();
        ((TextInputLayout) builder.getCustomView()).getEditText().setText(prefillText);
        ((TextInputLayout) builder.getCustomView()).getEditText().setHint(hint);


        builder.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        builder.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        builder.show();
    }

    public void customViewDialog(Activity activity, CharSequence title, View customView, boolean wrapInScrollView, String positiveButtonMessage, boolean isCancellable,
                                 MaterialDialog.SingleButtonCallback buttonCallback, android.content.DialogInterface.OnDismissListener dismissListener) {
        showDialog(activity, title, null, customView, wrapInScrollView, null, positiveButtonMessage, null, isCancellable, buttonCallback, null, dismissListener);
    }

    public void listDialog(Activity activity, String title, CharSequence[] array, boolean isCancellable, MaterialDialog.ListCallback listCallback,
                           DialogInterface.OnDismissListener dismissListener) {
        showDialog(activity, title, null, null, false, array, null, null, isCancellable, null, listCallback, dismissListener);
    }

    public void showDialog(Activity activity, CharSequence title, CharSequence content, View customView, boolean wrapInScrollView, CharSequence[] listArray, String positiveButtonMessage, String negativeButtonMsg, boolean isCancellable,
                           MaterialDialog.SingleButtonCallback buttonCallback, MaterialDialog.ListCallback listCallback, android.content.DialogInterface.OnDismissListener dismissListener) {
        dismissDialog();

        MaterialDialog.Builder builder = new MaterialDialog.Builder(activity);
        builder.canceledOnTouchOutside(isCancellable);
        if (title != null) {
            builder.title(title);
        }
        if (content != null) {
            builder.content(content);
        }
        if (customView != null) {
            builder.customView(customView, wrapInScrollView);
        }
        if (listArray != null) {
            builder.items(listArray);
        }

        if (positiveButtonMessage != null) {
            builder.positiveText(positiveButtonMessage);
        }
        if (negativeButtonMsg != null) {
            builder.negativeText(negativeButtonMsg);
        }

        if (buttonCallback != null) {
            builder.onPositive(buttonCallback);
            builder.onNegative(buttonCallback);
        }
        if (listCallback != null) {
            builder.itemsCallback(listCallback);
        }
        if (dismissListener != null) {
            builder.dismissListener(dismissListener);
        }
        materialDialog = builder.build();
        try {
            materialDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            materialDialog = null;
        }
    }
}
