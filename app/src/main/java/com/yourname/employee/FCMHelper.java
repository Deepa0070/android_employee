package com.yourname.employee;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

public class FCMHelper {

    private static final String TAG = "FCM_HELPER";

    public static void initializeFCM(Context context) {
        // Get FCM token
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token);

                    // Save token to Firestore
                    saveTokenToFirestore(token);

                    Toast.makeText(context, "Push notifications enabled!", Toast.LENGTH_SHORT).show();
                });

        // Subscribe to topics (optional)
        subscribeToTopics();
    }

    private static void saveTokenToFirestore(String token) {
        // Implementation similar to MyFirebaseMessagingService
        // Save token to current user's document in Firestore
    }

    private static void subscribeToTopics() {
        // Subscribe to general topics
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                .addOnCompleteListener(task -> {
                    String msg = "Subscribed to all_users topic";
                    if (!task.isSuccessful()) {
                        msg = "Subscribe failed";
                    }
                    Log.d(TAG, msg);
                });

        // You can add more topic subscriptions based on user role
        // Example: FirebaseMessaging.getInstance().subscribeToTopic("hr_department");
    }

    public static void subscribeToUserTopics(String userId) {
        // Subscribe to user-specific topic
        FirebaseMessaging.getInstance().subscribeToTopic("user_" + userId)
                .addOnCompleteListener(task -> {
                    Log.d(TAG, "Subscribed to user topic: user_" + userId);
                });
    }

    public static void unsubscribeFromTopics(String userId) {
        // Unsubscribe when needed
        FirebaseMessaging.getInstance().unsubscribeFromTopic("user_" + userId);
    }
}