package com.example.phonegpstracker;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity {

    private static final int PERMISSION_REQUEST = 100;
    private EditText keyInput;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 40, 30, 30);

        TextView title = new TextView(this);
        title.setText("PHONE GPS TRACKER");
        title.setTextSize(25);

        TextView info = new TextView(this);
        info.setText("\nThingSpeak Write API Key:");

        keyInput = new EditText(this);
        keyInput.setHint("Enter your Write API Key");

        String savedKey = getSharedPreferences(
            "TrackerSettings", MODE_PRIVATE
        ).getString("write_key", "");

        keyInput.setText(savedKey);

        Button save = new Button(this);
        save.setText("SAVE API KEY");

        Button start = new Button(this);
        start.setText("START TRACKING");

        Button stop = new Button(this);
        stop.setText("STOP TRACKING");

        status = new TextView(this);
        status.setText("\nTracking is stopped.");
        status.setTextSize(17);

        layout.addView(title);
        layout.addView(info);
        layout.addView(keyInput);
        layout.addView(save);
        layout.addView(start);
        layout.addView(stop);
        layout.addView(status);

        setContentView(layout);

        save.setOnClickListener(v -> {
            String key = keyInput.getText().toString().trim();

            if (key.isEmpty()) {
                status.setText("Please enter your Write API Key.");
                return;
            }

            getSharedPreferences(
                "TrackerSettings", MODE_PRIVATE
            ).edit().putString("write_key", key).apply();

            status.setText("API Key saved on this phone.");
        });

        start.setOnClickListener(v -> checkPermissions());

        stop.setOnClickListener(v -> {
            stopService(new Intent(this, LocationService.class));
            status.setText("Tracking stopped.");
        });
    }

    private void checkPermissions() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                },
                PERMISSION_REQUEST
            );
            return;
        }

        startTracking();
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode,
        String[] permissions,
        int[] grantResults
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        );

        if (requestCode == PERMISSION_REQUEST) {
            if (checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
                startTracking();
            } else {
                status.setText("Location permission is required.");
            }
        }
    }

    private void startTracking() {
        String key = getSharedPreferences(
            "TrackerSettings", MODE_PRIVATE
        ).getString("write_key", "");

        if (key.isEmpty()) {
            status.setText("Save your Write API Key first.");
            return;
        }

        Intent intent = new Intent(this, LocationService.class);

        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        status.setText("GPS tracking service started.");
    }
}
