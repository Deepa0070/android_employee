package com.yourname.employee;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseInstanceIdService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseIIDService";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);

        // Send the token to your server if needed
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        // TODO: Implement this method to send token to your app server
        Log.d(TAG, "Token to send to server: " + token);

        // Example: Save token to SharedPreferences
        getSharedPreferences("FCM_PREF", MODE_PRIVATE)
                .edit()
                .putString("fcm_token", token)
                .apply();
    }
}