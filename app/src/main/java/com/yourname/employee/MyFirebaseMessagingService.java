package com.yourname.employee;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM_SERVICE";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM Token: " + token);

        // Save token to Firestore
        saveTokenToFirestore(token);

        // Show toast
        Toast.makeText(this, "Device registered for push notifications", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        // Check data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }

        // Check notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification: " + remoteMessage.getNotification().getBody());
            handleNotificationMessage(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody(),
                    remoteMessage.getData()
            );
        }
    }

    private void handleDataMessage(Map<String, String> data) {
        try {
            String title = data.get("title");
            String message = data.get("message");
            String type = data.get("type");
            String emergencyRequestId = data.get("emergencyRequestId");
            String leaveId = data.get("leaveId");
            String employeeName = data.get("employeeName");
            String department = data.get("department");
            String leaveType = data.get("leaveType");
            String days = data.get("numberOfDays");

            Log.d(TAG, "Processing data message - Type: " + type);

            NotificationHelper notificationHelper = new NotificationHelper(this);

            if (NotificationHelper.TYPE_EMERGENCY.equals(type) && employeeName != null) {
                // Emergency notification
                notificationHelper.showEmergencyNotification(
                        employeeName,
                        department != null ? department : "Unknown",
                        leaveType != null ? leaveType : "Emergency Leave",
                        days != null ? Integer.parseInt(days) : 1,
                        emergencyRequestId != null ? emergencyRequestId : "unknown"
                );
            } else {
                // Regular notification
                notificationHelper.showNotification(
                        title != null ? title : "Notification",
                        message != null ? message : "New message",
                        type != null ? type : NotificationHelper.TYPE_GENERAL
                );
            }

        } catch (Exception e) {
            Log.e(TAG, "Error handling data message: " + e.getMessage());
        }
    }

    private void handleNotificationMessage(String title, String body, Map<String, String> data) {
        NotificationHelper notificationHelper = new NotificationHelper(this);

        String type = data != null ? data.get("type") : NotificationHelper.TYPE_GENERAL;
        String emergencyRequestId = data != null ? data.get("emergencyRequestId") : null;
        String employeeName = data != null ? data.get("employeeName") : null;

        if (NotificationHelper.TYPE_EMERGENCY.equals(type) && employeeName != null) {
            String department = data.get("department");
            String leaveType = data.get("leaveType");
            String daysStr = data.get("numberOfDays");

            notificationHelper.showEmergencyNotification(
                    employeeName,
                    department != null ? department : "Unknown",
                    leaveType != null ? leaveType : "Emergency Leave",
                    daysStr != null ? Integer.parseInt(daysStr) : 1,
                    emergencyRequestId != null ? emergencyRequestId : "unknown"
            );
        } else {
            notificationHelper.showNotification(
                    title != null ? title : "Notification",
                    body != null ? body : "New message",
                    type
            );
        }
    }

    private void saveTokenToFirestore(String token) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Token saved to Firestore"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save token: " + e.getMessage()));
        } else {
            Log.d(TAG, "User not logged in, token not saved");
        }
    }
}