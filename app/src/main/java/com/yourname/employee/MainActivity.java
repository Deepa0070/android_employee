package com.yourname.employee;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this);

            // Initialize FCM
            FCMHelper.initializeFCM(this);

            // Initialize Firebase Auth
            FirebaseAuth auth = FirebaseAuth.getInstance();

            if (auth != null) {
                Toast.makeText(this, "Firebase Connected! ✓", Toast.LENGTH_LONG).show();
            }

            // Create notification channels
            createEmergencyNotificationChannel();
            createRegularNotificationChannel();

            // Check and request notification permission
            checkNotificationPermission();

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("MainActivity", "Initialization error", e);
        }
    }

    private void createEmergencyNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel emergencyChannel = new NotificationChannel(
                    "emergency_channel_id",
                    "Emergency Leave Requests",
                    NotificationManager.IMPORTANCE_HIGH
            );
            emergencyChannel.setDescription("Emergency leave request notifications");
            emergencyChannel.enableLights(true);
            emergencyChannel.setLightColor(Color.RED);
            emergencyChannel.enableVibration(true);
            emergencyChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000, 500, 1000});
            emergencyChannel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            emergencyChannel.setImportance(NotificationManager.IMPORTANCE_HIGH);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(emergencyChannel);
                Log.d("MainActivity", "Emergency notification channel created");
            }
        }
    }

    private void createRegularNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NotificationHelper.getChannelId(),
                    NotificationHelper.getChannelName(),
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Regular leave notifications");
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            channel.setImportance(NotificationManager.IMPORTANCE_HIGH);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d("MainActivity", "Regular notification channel created");
            }
        }
    }

    private void checkNotificationPermission() {
        // For Android 13 (API 33) and above, we need to request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                // Show explanation if needed
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.POST_NOTIFICATIONS)) {
                    Toast.makeText(this,
                            "Notifications help you stay updated with leave approvals and applications",
                            Toast.LENGTH_LONG).show();
                }

                // Request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE);
            } else {
                // Permission already granted
                Log.d("MainActivity", "Notification permission already granted");
            }
        } else {
            // For older versions, permission is granted by default
            Log.d("MainActivity", "No need for notification permission (Android < 13)");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted ✓", Toast.LENGTH_SHORT).show();
                Log.d("MainActivity", "Notification permission granted by user");
            } else {
                Toast.makeText(this,
                        "Notification permission denied. Some features may not work properly.",
                        Toast.LENGTH_LONG).show();
                Log.d("MainActivity", "Notification permission denied by user");
            }
        }
    }
}