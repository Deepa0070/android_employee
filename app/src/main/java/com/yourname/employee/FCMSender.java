package com.yourname.employee;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Scanner;

public class FCMSender {

    private static final String FCM_API = "https://fcm.googleapis.com/fcm/send";
    private static final String SERVER_KEY = BuildConfig.FCM_SERVER_KEY; // store in BuildConfig

    public static void sendPushNotification(String token, String title, String body, Map<String, String> data) {
        new SendFcmTask().execute(token, title, body, data);
    }

    private static class SendFcmTask extends AsyncTask<Object, Void, Void> {
        @Override
        protected Void doInBackground(Object... params) {
            String token = (String) params[0];
            String title = (String) params[1];
            String body = (String) params[2];
            Map<String, String> data = (Map<String, String>) params[3];

            try {
                JSONObject root = new JSONObject();
                root.put("to", token);

                JSONObject notification = new JSONObject();
                notification.put("title", title);
                notification.put("body", body);
                notification.put("sound", "default");
                notification.put("click_action", "OPEN_ACTIVITY"); // optional
                root.put("notification", notification);

                JSONObject dataPayload = new JSONObject(data);
                root.put("data", dataPayload);

                URL url = new URL(FCM_API);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "key=" + SERVER_KEY);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(root.toString().getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Scanner scanner = new Scanner(conn.getInputStream());
                    String response = scanner.useDelimiter("\\A").next();
                    scanner.close();
                    Log.d("FCM", "Push sent successfully: " + response);
                } else {
                    Log.e("FCM", "Error sending push, code: " + responseCode);
                }

                conn.disconnect();
            } catch (IOException | JSONException e) {
                Log.e("FCM", "Failed to send push", e);
            }
            return null;
        }
    }
}