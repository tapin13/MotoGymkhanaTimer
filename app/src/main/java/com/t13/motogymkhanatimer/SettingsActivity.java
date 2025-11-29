package com.t13.motogymkhanatimer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final float GPS_MOVE_SPEED_KMH = 4.5f;
    private static final float GPS_STOP_SPEED_KMH = 2.5f;

    // Accelerometer thresholds (motorcycle has vibrations)
    private static final float ACCEL_MOVE_THRESHOLD = 2.0f;
    private static final float ACCEL_STOP_THRESHOLD = 1.5f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);

        float minAccelerate = 1.0f;
        float maxAccelerate = 10.0f;
        float stepAccelerate = 0.1f;
        int rangeAccelSeekBar = Math.round((maxAccelerate - minAccelerate) / stepAccelerate);

        SeekBar accelerateStartSeekBar = findViewById(R.id.accelerateStartSeekBar);
        accelerateStartSeekBar.setMax(rangeAccelSeekBar);

        TextView accelStartLabel = findViewById(R.id.accelerateStartTextView);

        float accelerationStart = prefs.getFloat("accelerationStart", ACCEL_MOVE_THRESHOLD);

        accelStartLabel.setText(String.format("Start: %.1f", accelerationStart));
        accelerateStartSeekBar.setProgress(Math.round((accelerationStart - minAccelerate) / stepAccelerate));

        accelerateStartSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float minAccelerate = 1.0f;
                float stepAccelerate = 0.1f;
                float accelValue = minAccelerate + (progress * stepAccelerate);
                accelStartLabel.setText(String.format("Start: %.1f", accelValue));

                SharedPreferences.Editor editor = prefs.edit();
                editor.putFloat("accelerationStart", accelValue);
                editor.apply();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // ~~~~~~~~~~ //

        SeekBar accelerateStopSeekBar = findViewById(R.id.accelerateStopSeekBar);
        accelerateStopSeekBar.setMax(rangeAccelSeekBar);

        TextView accelStopLabel = findViewById(R.id.accelerateStopTextView);

        float accelerationStop = prefs.getFloat("accelerationStop", ACCEL_MOVE_THRESHOLD);

        accelStopLabel.setText(String.format("Stop: %.1f", accelerationStop));
        accelerateStopSeekBar.setProgress(Math.round((accelerationStop - minAccelerate) / stepAccelerate));

        accelerateStopSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float minAccelerate = 1.0f;
                float stepAccelerate = 0.1f;
                float accelValue = minAccelerate + (progress * stepAccelerate);
                accelStopLabel.setText(String.format("Stop: %.1f", accelValue));

                SharedPreferences.Editor editor = prefs.edit();
                editor.putFloat("accelerationStop", accelValue);
                editor.apply();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        //////////////////////////////////////////////////////////////////////////

        float minSpeed = 1.0f;
        float maxSpeed = 10.0f;
        float stepSpeed = 0.1f;
        int rangeSpeedSeekBar = (int) Math.round((maxSpeed - minSpeed) / stepSpeed);

        float speedStart = prefs.getFloat("speedStart", GPS_MOVE_SPEED_KMH);

        SeekBar speedStartSeekBar = findViewById(R.id.speedStartSeekBar);
        speedStartSeekBar.setMax(rangeSpeedSeekBar);

        TextView speedStartLabel = findViewById(R.id.speedStartTextView);
        speedStartLabel.setText(String.format("Start: %.1f", speedStart));

        speedStartSeekBar.setProgress(Math.round((speedStart - minSpeed) / stepSpeed));

        speedStartSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float minSpeed = 1.0f;
                float stepSpeed = 0.1f;

                float speedValue = minSpeed + (progress * stepSpeed);
                speedStartLabel.setText(String.format("Start: %.1f", speedValue));

                SharedPreferences.Editor editor = prefs.edit();
                editor.putFloat("speedStart", speedValue);
                editor.apply();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // ~~~~~~~~ //

        float speedStop = prefs.getFloat("speedStop", GPS_MOVE_SPEED_KMH);

        SeekBar speedStopSeekBar = findViewById(R.id.speedStopSeekBar);
        speedStopSeekBar.setMax(rangeSpeedSeekBar);

        TextView speedStopLabel = findViewById(R.id.speedStopTextView);
        speedStopLabel.setText(String.format("Stop: %.1f", speedStop));

        speedStopSeekBar.setProgress(Math.round((speedStop - minSpeed) / stepSpeed));

        speedStopSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float minSpeed = 1.0f;
                float stepSpeed = 0.1f;

                float speedValue = minSpeed + (progress * stepSpeed);
                speedStopLabel.setText(String.format("Stop: %.1f", speedValue));

                SharedPreferences.Editor editor = prefs.edit();
                editor.putFloat("speedStop", speedValue);
                editor.apply();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

    }
}