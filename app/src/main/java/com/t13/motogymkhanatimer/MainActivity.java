package com.t13.motogymkhanatimer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.Manifest;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener {
    String appUId = "NoUId";

    private SensorManager sensorManager;
    private LocationManager locationManager;
    private Sensor accelerometer;
    private Sensor gyroscope;

    // -------------------------------
    // FILTER VARIABLES (TUNABLE)
    // -------------------------------

    private static final float MOVE_AFTER_START = 5.0f;     // above -> moving

    // GPS speed thresholds
    private float gpsMoveSpeedKmh = 3.0f;   // above -> moving
    private float gpsStopSpeedKmh = 2.0f;   // below -> could be stop

    // Accelerometer thresholds (motorcycle has vibrations)
    private float accelMoveThreshold = 1.0f;   // g-force difference
    private float accelStopThreshold = 0.5f;

    // Gyroscope thresholds (motorcycle has micro-rotations)
    private static final double GYRO_MOVE_THRESHOLD = 0.05;   // rad/s
    private static final double GYRO_STOP_THRESHOLD = 0.02;

    private boolean readyToStart = false;

    private final Handler handler = new Handler();

    // -------------------------------
    // VARIABLES
    // -------------------------------
    private double gyroTotal = 0f;
    private float gpsSpeedKmh = 0f;

    // for debug

    private double accelMax = 0f;
    private double gyroMax = 0f;
    private float gpsMax = 0f;

    private boolean isMoving = false;

    private TextView timerText;
    private Button startButton;

    private TextView debugEcho;
    private TextView debugStopEcho;

    private float accel = 0f;

    private long rideStartTime = 0;

    // for log
    List<String> accelBuffer = new ArrayList<>();
    List<String> gpsBuffer = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        appUId = getOrCreateAppId();
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        gpsMoveSpeedKmh = prefs.getFloat("speedStart", 4.0f);
        accelMoveThreshold = prefs.getFloat("accelerationStart", 1.0f);

        timerText = findViewById(R.id.timerText);
        startButton = findViewById(R.id.startButton);
        ImageButton settingsButton = findViewById(R.id.settings_button);

        debugEcho = findViewById(R.id.debugEcho);
        debugStopEcho = findViewById(R.id.debugStopEcho);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        startButton.setOnClickListener(v -> {
            isMoving = false;
            readyToStart = true;

            accelMax = 0f;
            gyroMax = 0f;
            gpsMax = 0f;

            timerText.setText("00:00.0");
            startButton.setText("GO");
            startButton.setEnabled(false);
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);

        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        gpsMoveSpeedKmh = prefs.getFloat("speedStart", 4.0f);
        accelMoveThreshold = prefs.getFloat("accelerationStart", 1.0f);

        requestGPS();
    }

    @SuppressLint("MissingPermission")
    private void requestGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 11);
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        gpsSpeedKmh = location.getSpeed() * 3.6f;

        String row = System.currentTimeMillis() + "," + gpsSpeedKmh + "," + location.getTime() +
                "," + location.getAccuracy() + ","
                + location.getLatitude() + "," + location.getLongitude();
        gpsBuffer.add(row);

        detectMovement();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            double totalSeconds = (System.currentTimeMillis() - rideStartTime) / 1000.0;
            int minutes = (int)(totalSeconds / 60);
            int secs = (int)(totalSeconds % 60);
            int tenths = (int)((totalSeconds * 10) % 10);

            String newText = String.format("%d:%02d.%d", minutes, secs, tenths);
            if (!newText.equals(timerText.getText().toString())) {
                timerText.setText(newText);
            }

            handler.postDelayed(this, 250);
        }
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(!readyToStart) {
            return;
        }

        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            float ax = event.values[0];
            float ay = event.values[1];
            float az = event.values[2];

            accel = (float) Math.sqrt(ax*ax + ay*ay + az*az);

            String row = System.currentTimeMillis() + "," + ax + "," + ay + "," + az + "," + accel;
            accelBuffer.add(row);
        }

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float rx = event.values[0];
            float ry = event.values[1];
            float rz = event.values[2];

            gyroTotal = Math.sqrt(rx * rx + ry * ry + rz * rz);
        }

        detectMovement();
    }

    private void startTimer() {
        rideStartTime = System.currentTimeMillis();

        handler.postDelayed(timerRunnable, 100);
    }
    private void stopTimer() {
        double totalSeconds = (System.currentTimeMillis() - rideStartTime) / 1000.0;
        int minutes = (int)(totalSeconds / 60);
        int secs = (int)(totalSeconds % 60);
        int tenths = (int)((totalSeconds * 10) % 10);
        timerText.setText(String.format("%d:%02d.%d", minutes, secs, tenths));

        handler.removeCallbacks(timerRunnable);

        startButton.setText(R.string.start);
        startButton.setEnabled(true);
        readyToStart = false;

        long logTime = System.currentTimeMillis();
        writeCsvLines("ride_" + appUId + "_" + logTime + "_accel.csv", accelBuffer);
        accelBuffer.clear();
        writeCsvLines("ride_" + appUId + "_" + logTime + "_gps.csv", gpsBuffer);
        gpsBuffer.clear();
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] p, @NonNull int[] r) {
        super.onRequestPermissionsResult(req, p, r);

        if (req == 11 && r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) {
            requestGPS();
        }
    }

    private void detectMovement() {
        if(!readyToStart) {
            return;
        }

        boolean gpsMoving = gpsSpeedKmh > gpsMoveSpeedKmh;
        boolean gpsStopping = gpsSpeedKmh < gpsStopSpeedKmh;

        boolean accelMoving = accel > accelMoveThreshold;
        boolean accelStopping = accel < accelStopThreshold;

        boolean gyroMoving = gyroTotal > GYRO_MOVE_THRESHOLD;
        boolean gyroStopping = gyroTotal < GYRO_STOP_THRESHOLD;

        gpsMax = Math.max(gpsMax, gpsSpeedKmh);
        accelMax = Math.max(accelMax, accel);
        gyroMax = Math.max(gyroMax, gyroTotal);

        // --------------------------
        // MOVING LOGIC
        // --------------------------

        if (!isMoving) {
//            if (gpsMoving || accelMoving || gyroMoving) {
            if (gpsMoving || accelMoving) {
                    isMoving = true;

                debugStopEcho.setText(
                        String.format(
                                "start speed: %s - %.1f\n accel: %s - %.3f - %.3f\n",
                                gpsMoving, gpsSpeedKmh,
                                accelMoving, accel, accelMax
                        )
                );

                    startTimer();
            }
        }

        // --------------------------
        // STOPPED LOGIC
        // --------------------------
        else {
            debugEcho.setText(
                    String.format(
                        "detect speed: %s - %.1f\n accel: %s - %.3f - %.3f\n",
                        gpsStopping, gpsSpeedKmh,
                        accelStopping, accel, accelMax
                    )
            );

//            if (gpsStopping && accelStopping && gyroStopping) {
            if ((System.currentTimeMillis() - rideStartTime)/1000 > MOVE_AFTER_START && gpsStopping && accelStopping) {
                    isMoving = false;

                    debugStopEcho.setText(
                        String.format(
                            "stop speed: %s - %.1f\n accel: %s - %.3f - %.3f\n Top Speed: %.1f\n",
                            gpsStopping, gpsSpeedKmh,
                            accelStopping, accel, accelMax,
                            gpsMax
                        )
                    );

                    stopTimer();
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "GPS is disabled. App is closing.", Toast.LENGTH_LONG).show();

        finish();
    }

    public void writeCsvLines(String fileName, List<String> rows) {
        try {
            File dir = new File(getExternalFilesDir(null), "logs");
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, fileName);
            FileWriter fw = new FileWriter(file, true); // append

            for (String row : rows) {
                fw.write(row);
                fw.write("\n");
            }

            fw.close();

            File fileup = new File(dir, fileName);
            uploadFile(fileup, BuildConfig.LOGS_UPLOAD_URL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void uploadFile(File file, String uploadUrl) {
        Thread thread = new Thread(() -> {
            String boundary = "===" + System.currentTimeMillis() + "===";

            try {
                URL url = new URL(uploadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream request = new DataOutputStream(conn.getOutputStream());

                request.writeBytes("--" + boundary + "\r\n");
                request.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\""
                        + file.getName() + "\"\r\n");
                request.writeBytes("Content-Type: text/plain\r\n\r\n");

                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    request.write(buffer, 0, bytesRead);
                }
                fis.close();

                request.writeBytes("\r\n--" + boundary + "--\r\n");
                request.flush();
                request.close();

                int responseCode = conn.getResponseCode();
                Log.d("UPLOAD", "Server response: " + responseCode);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        thread.start();
    }

    private String getOrCreateAppId() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        String id = prefs.getString("uId", null);

        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
            prefs.edit().putString("uId", id).apply();
        }

        return id;
    }
}