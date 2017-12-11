package com.vijay.locationtracker;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.greysonparrelli.permiso.Permiso;

import com.vijay.androidutils.Logger;
import com.vijay.androidutils.ToastUtils;
import com.vijay.locationtracker.firebase.Constants;

public class MainActivity extends AppCompatActivity implements ValueEventListener{
    private static final String TAG = MainActivity.class.getSimpleName();
    public static final String REQUEST_PERMISSION = "requestPermission";
    DatabaseReference trackingStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(TAG, "onCreate called");
        Permiso.getInstance().setActivity(this);

        boolean requestPermission = getIntent().getBooleanExtra(REQUEST_PERMISSION, false);
        if (requestPermission) {
            requestPermission(TrackingService.permissions);
        }
        setContentView(R.layout.activity_main);

        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        trackingStatus = firebaseDatabase.getReference(Constants.TRACKING_STATUS);
        trackingStatus.addValueEventListener(this);
        Logger.d(TAG, "tracking status listener registered");
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        Boolean value = dataSnapshot.getValue(Boolean.class);
        Logger.d(TAG, "onDataChange called : tracking = "+ value);
        ImageView status = findViewById(R.id.service_status);
        if (value != null && value) {
            status.setImageResource(R.drawable.active);
        } else {
            status.setImageResource(R.drawable.inactive);
        }
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }

    private void requestPermission(String... permissions) {
        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (resultSet.areAllPermissionsGranted()) {
                    ToastUtils.makeToastLong(MainActivity.this, getResources().getString(R.string.toast_permission_granted));

                    Intent intent = new Intent(MainActivity.this, TrackingService.class);
                    intent.putExtra(TrackingService.EXTRA_TRACKING_STATUS, true);
                    startService(intent);

                } else {
                    ToastUtils.makeToastLong(MainActivity.this, getResources().getString(R.string.toast_permission_denied));
                }
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                Permiso.getInstance().showRationaleInDialog("Title", "Message", null, callback);
            }
        }, permissions);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.d(TAG, "onResume called");
        Permiso.getInstance().setActivity(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Permiso.getInstance().onRequestPermissionResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        trackingStatus.removeEventListener(this);
        Logger.d(TAG, "tracking status listener removed");
        Logger.d(TAG, "onDestroy called");
    }
}
