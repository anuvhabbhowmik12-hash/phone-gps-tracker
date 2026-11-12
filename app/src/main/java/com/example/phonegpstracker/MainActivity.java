package com.example.phonegpstracker;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final int PERMISSION_REQUEST = 100;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 50, 30, 30);

        TextView title = new TextView(this);
        title.setText("PHONE GPS TRACKER");
        title.setTextSize(24);

        status = new TextView(this);
        status.setText("\nTracking is stopped.");
        status.setTextSize(17);

        Button start = new Button(this);
        start.setText("START TRACKING");

        Button stop = new Button(this);
        stop.setText("STOP TRACKING");

        layout.addView(title);
        layout.addView(status);
        layout.addView(start);
        layout.addView(stop);

        setContentView(layout);

        start.setOnClickListener(v -> checkPermissions());

        stop.setOnClickListener(v -> {
            stopService(new Intent(this, LocationService.class));
            status.setText("\nTracking stopped.");
        });
    }

    private void checkPermissions() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (Build.VERSION.SDK_INT >= 23) {
                requestPermissions(
                    new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    PERMISSION_REQUEST
                );
            }
            return;
        }

        startTracking();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST) {
            if (checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                startTracking();
            } else {
                status.setText("\nLocation permission is required.");
            }
        }
    }

    private void startTracking() {
        Intent intent = new Intent(this, LocationService.class);

        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        status.setText("\nStarting GPS service...");
    }
}
