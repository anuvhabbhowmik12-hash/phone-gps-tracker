package com.example.phonegpstracker;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class LocationService extends Service implements LocationListener {

    private static final String CHANNEL_ID = "gps_tracking";
    private static final int NOTIFICATION_ID = 1;

    private LocationManager locationManager;
    private long lastUploadTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        Notification notification =
            new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Phone GPS Tracker")
                .setContentText("Location tracking is active")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            );
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        locationManager =
            (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        startLocationUpdates();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "GPS Tracking",
                NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager =
                getSystemService(NotificationManager.class);

            manager.createNotificationChannel(channel);
        }
    }

    private void startLocationUpdates() {
        try {
            if (checkSelfPermission(
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED
                &&
                checkSelfPermission(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                stopSelf();
                return;
            }

            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000,
                0,
                this,
                Looper.getMainLooper()
            );

        } catch (Exception e) {
            stopSelf();
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        long now = System.currentTimeMillis();

        if (now - lastUploadTime < 20000) {
            return;
        }

        lastUploadTime = now;

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        uploadLocation(latitude, longitude);
    }

    private void uploadLocation(double latitude, double longitude) {

        String writeKey = getSharedPreferences(
            "TrackerSettings",
            MODE_PRIVATE
        ).getString("write_key", "");

        if (writeKey == null || writeKey.trim().isEmpty()) {
            return;
        }

        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                String urlText =
                    "https://api.thingspeak.com/update?api_key="
                    + URLEncoder.encode(writeKey, "UTF-8")
                    + "&field1=" + latitude
                    + "&field2=" + longitude;

                URL url = new URL(urlText);

                connection =
                    (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                connection.getResponseCode();

            } catch (Exception e) {
                android.util.Log.e(
                    "GPS_TRACKER",
                    "Location upload failed",
                    e
                );
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onStatusChanged(
        String provider,
        int status,
        android.os.Bundle extras
    ) {}
}
