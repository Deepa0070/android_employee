package com.yourname.employee;

import android.util.Log;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class NotificationSender {

    public static void sendEmergencyPushNotification(String employeeName, String department,
                                                     String leaveType, int numberOfDays,
                                                     String emergencyRequestId) {

        // Get all HR users' FCM tokens
        FirebaseFirestore.getInstance().collection("users")
                .whereIn("role", Arrays.asList("hr", "hr admin", "admin"))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String token = document.getString("fcmToken");
                        if (token != null && !token.isEmpty()) {
                            // Send notification to each HR
                            sendToIndividual(token, createEmergencyMessage(
                                    employeeName, department, leaveType, numberOfDays, emergencyRequestId
                            ));
                        }
                    }
                });
    }

    private static Map<String, String> createEmergencyMessage(String employeeName, String department,
                                                              String leaveType, int numberOfDays,
                                                              String requestId) {
        Map<String, String> message = new HashMap<>();
        message.put("title", "🚨 Emergency Leave Request");
        message.put("message", employeeName + " needs " + numberOfDays + " days " + leaveType);
        message.put("type", "emergency_request");
        message.put("emergencyRequestId", requestId);
        message.put("employeeName", employeeName);
        message.put("department", department);
        message.put("leaveType", leaveType);
        message.put("numberOfDays", String.valueOf(numberOfDays));
        return message;
    }

    private static void sendToIndividual(String token, Map<String, String> data) {
        // You would need a server endpoint to send FCM messages
        // This is just the structure
        Log.d("FCM_SENDER", "Would send to token: " + token);
    }
}