package com.yourname.employee;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Random;

public class NotificationHelper {

    // Channel constants
    private static final String CHANNEL_ID = "employee_leave_channel";
    private static final String CHANNEL_NAME = "Employee Leave Notifications";
    private static final String CHANNEL_DESC = "Notifications for leave approvals, applications, and updates";

    // Action constants (should match NotificationReceiver)
    public static final String ACTION_EMERGENCY_APPROVE = "com.yourname.employee.ACTION_EMERGENCY_APPROVE";
    public static final String ACTION_EMERGENCY_REJECT = "com.yourname.employee.ACTION_EMERGENCY_REJECT";
    public static final String ACTION_LEAVE_APPROVED = "com.yourname.employee.ACTION_LEAVE_APPROVED";
    public static final String ACTION_LEAVE_REJECTED = "com.yourname.employee.ACTION_LEAVE_REJECTED";
    public static final String ACTION_NEW_LEAVE = "com.yourname.employee.ACTION_NEW_LEAVE";
    public static final String ACTION_VIEW_LEAVES = "com.yourname.employee.ACTION_VIEW_LEAVES";
    public static final String ACTION_VIEW_NOTIFICATIONS = "com.yourname.employee.ACTION_VIEW_NOTIFICATIONS";

    // Notification type constants
    public static final String TYPE_EMERGENCY = "emergency";
    public static final String TYPE_LEAVE_APPROVED = "leave_approved";
    public static final String TYPE_LEAVE_REJECTED = "leave_rejected";
    public static final String TYPE_LEAVE_PENDING = "leave_pending";
    public static final String TYPE_DONATION = "donation";
    public static final String TYPE_SALARY = "salary";
    public static final String TYPE_GENERAL = "general";

    private Context context;
    private NotificationManager notificationManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    // Static method to show toast for token updates
    public static void showTokenUpdateToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 300, 200, 100});
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            channel.setShowBadge(true);
            channel.setImportance(NotificationManager.IMPORTANCE_HIGH);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d("NOTIFICATION_HELPER", "✅ Notification channel created: " + CHANNEL_ID);
            }
        }
    }

    // Main method to show notification
    public void showNotification(String title, String message, String type) {
        try {
            // Generate unique notification ID
            int notificationId = generateNotificationId();

            // Get intent based on type
            Intent intent = getTargetIntent(type);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("notification_type", type);
            intent.putExtra("notification_id", notificationId);

            // Create pending intent
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId,
                    intent,
                    flags
            );

            // Create notification builder
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(getNotificationIcon(type))
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(false)
                    .setCategory(getNotificationCategory(type))
                    .setColor(getNotificationColor(type))
                    .setVibrate(getVibrationPattern(type))
                    .setDefaults(NotificationCompat.DEFAULT_ALL);

            // Add actions based on type
            addNotificationActions(builder, type, notificationId);

            // Add large icon
            Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
            if (largeIcon != null) {
                builder.setLargeIcon(largeIcon);
            }

            // Set sound
            Uri soundUri = getNotificationSound(type);
            if (soundUri != null) {
                builder.setSound(soundUri);
            }

            // Show notification
            if (areNotificationsEnabled()) {
                notificationManager.notify(notificationId, builder.build());
                Log.d("NOTIFICATION_HELPER", "✅ Notification shown: " + title);
                Log.d("NOTIFICATION_HELPER", "📱 Type: " + type);
                Log.d("NOTIFICATION_HELPER", "🆔 ID: " + notificationId);
            } else {
                Log.w("NOTIFICATION_HELPER", "⚠️ Notifications are disabled");
            }

        } catch (Exception e) {
            Log.e("NOTIFICATION_HELPER", "❌ Error showing notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Method to show notification with custom data
    public void showNotificationWithData(String title, String message, String type,
                                         String employeeName, String leaveType,
                                         int numberOfDays, String leaveId) {
        try {
            int notificationId = generateNotificationId();

            // Get intent
            Intent intent = getTargetIntent(type);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("notification_type", type);
            intent.putExtra("employee_name", employeeName);
            intent.putExtra("leave_type", leaveType);
            intent.putExtra("number_of_days", numberOfDays);
            intent.putExtra("leave_id", leaveId);
            intent.putExtra("notification_id", notificationId);

            // Create pending intent
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId,
                    intent,
                    flags
            );

            // Build notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(getNotificationIcon(type))
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(false)
                    .setCategory(getNotificationCategory(type))
                    .setColor(getNotificationColor(type))
                    .setVibrate(getVibrationPattern(type))
                    .setDefaults(NotificationCompat.DEFAULT_ALL);

            // Add large icon
            Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
            if (largeIcon != null) {
                builder.setLargeIcon(largeIcon);
            }

            // Add actions if needed
            addNotificationActions(builder, type, notificationId);

            // Show notification
            if (areNotificationsEnabled()) {
                notificationManager.notify(notificationId, builder.build());
                Log.d("NOTIFICATION_HELPER", "✅ Notification with data shown: " + title);
                Log.d("NOTIFICATION_HELPER", "👤 Employee: " + employeeName);
                Log.d("NOTIFICATION_HELPER", "🆔 Leave ID: " + leaveId);
            }

        } catch (Exception e) {
            Log.e("NOTIFICATION_HELPER", "❌ Error showing notification with data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Emergency notification with approve/reject actions
    public void showEmergencyNotification(String employeeName, String department,
                                          String leaveType, int numberOfDays,
                                          String emergencyRequestId) {

        String title = "🚨 Emergency Leave Request";
        String message = employeeName + " (" + department + ") " +
                "needs " + numberOfDays + " day(s) " + leaveType;

        try {
            int notificationId = generateNotificationId();

            // Main intent to open emergency requests
            Intent mainIntent = new Intent(context, EmergencyRequestsActivity.class);
            mainIntent.putExtra("emergencyRequestId", emergencyRequestId);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            mainIntent.putExtra("notification_type", TYPE_EMERGENCY);
            mainIntent.putExtra("notification_id", notificationId);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent mainPendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId,
                    mainIntent,
                    flags
            );

            // Approve action
            Intent approveIntent = new Intent(context, NotificationReceiver.class);
            approveIntent.setAction(ACTION_EMERGENCY_APPROVE);
            approveIntent.putExtra("notification_id", notificationId);
            approveIntent.putExtra("emergency_request_id", emergencyRequestId);
            approveIntent.putExtra("employee_name", employeeName);
            approveIntent.putExtra("number_of_days", numberOfDays);
            approveIntent.putExtra("leave_type", leaveType);
            approveIntent.putExtra("department", department);

            PendingIntent approvePendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId + 1,
                    approveIntent,
                    flags
            );

            // Reject action
            Intent rejectIntent = new Intent(context, NotificationReceiver.class);
            rejectIntent.setAction(ACTION_EMERGENCY_REJECT);
            rejectIntent.putExtra("notification_id", notificationId);
            rejectIntent.putExtra("emergency_request_id", emergencyRequestId);
            rejectIntent.putExtra("employee_name", employeeName);

            PendingIntent rejectPendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId + 2,
                    rejectIntent,
                    flags
            );

            // Build notification
            Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setLargeIcon(largeIcon)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setContentIntent(mainPendingIntent)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(false)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setColor(Color.RED)
                    .setVibrate(new long[]{0, 1000, 500, 1000, 500, 1000}) // Urgent pattern
                    .setSound(alarmSound)
                    .setLights(Color.RED, 1000, 1000)
                    .addAction(R.drawable.ic_approve, "APPROVE", approvePendingIntent)
                    .addAction(R.drawable.ic_reject, "REJECT", rejectPendingIntent)
                    .setDefaults(NotificationCompat.DEFAULT_ALL);

            // Show notification
            if (areNotificationsEnabled()) {
                notificationManager.notify(notificationId, builder.build());
                Log.d("NOTIFICATION_HELPER", "🚨 Emergency notification sent: " + employeeName);
                Log.d("NOTIFICATION_HELPER", "🆔 Request ID: " + emergencyRequestId);
            }

        } catch (Exception e) {
            Log.e("NOTIFICATION_HELPER", "❌ Error sending emergency notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Simple notification without actions (backward compatibility)
    public void showSimpleNotification(String title, String message, String type) {
        try {
            Intent intent = getTargetIntent(type);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("notification_type", type);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    generateNotificationId(),
                    intent,
                    flags
            );

            Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
            Uri soundUri = getNotificationSound(type);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(getNotificationIcon(type))
                    .setLargeIcon(largeIcon)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(false)
                    .setCategory(getNotificationCategory(type))
                    .setColor(getNotificationColor(type))
                    .setVibrate(getVibrationPattern(type))
                    .setDefaults(NotificationCompat.DEFAULT_ALL);

            if (soundUri != null) {
                builder.setSound(soundUri);
            }

            int notificationId = generateNotificationId();
            if (areNotificationsEnabled()) {
                notificationManager.notify(notificationId, builder.build());
                Log.d("NOTIFICATION_HELPER", "✅ Simple notification shown: " + title);
            }

        } catch (Exception e) {
            Log.e("NOTIFICATION_HELPER", "❌ Error showing simple notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper method to add actions to notification
    private void addNotificationActions(NotificationCompat.Builder builder, String type, int notificationId) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        if (TYPE_EMERGENCY.equals(type)) {
            // Emergency actions are added in showEmergencyNotification method
            return;
        } else if (TYPE_LEAVE_APPROVED.equals(type) || TYPE_LEAVE_REJECTED.equals(type)) {
            // Add view leave history action
            Intent viewIntent = new Intent(context, NotificationReceiver.class);
            viewIntent.setAction(ACTION_VIEW_LEAVES);
            viewIntent.putExtra("notification_id", notificationId);
            viewIntent.putExtra("notification_type", type);

            PendingIntent viewPendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId + 10,
                    viewIntent,
                    flags
            );

            builder.addAction(R.drawable.ic_view, "VIEW LEAVES", viewPendingIntent);

        } else if (TYPE_LEAVE_PENDING.equals(type)) {
            // Add approve/reject actions for pending leaves
            Intent approveIntent = new Intent(context, NotificationReceiver.class);
            approveIntent.setAction(ACTION_LEAVE_APPROVED);
            approveIntent.putExtra("notification_id", notificationId);

            Intent rejectIntent = new Intent(context, NotificationReceiver.class);
            rejectIntent.setAction(ACTION_LEAVE_REJECTED);
            rejectIntent.putExtra("notification_id", notificationId);

            PendingIntent approvePendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId + 20,
                    approveIntent,
                    flags
            );

            PendingIntent rejectPendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId + 30,
                    rejectIntent,
                    flags
            );

            builder.addAction(R.drawable.ic_approve, "APPROVE", approvePendingIntent)
                    .addAction(R.drawable.ic_reject, "REJECT", rejectPendingIntent);
        }
    }

    // Helper method to get target intent based on notification type
    private Intent getTargetIntent(String type) {
        if (type == null || TYPE_GENERAL.equals(type)) {
            return new Intent(context, NotificationsActivity.class);
        }

        switch (type) {
            case TYPE_EMERGENCY:
                return new Intent(context, EmergencyRequestsActivity.class);

            case TYPE_LEAVE_APPROVED:
            case TYPE_LEAVE_REJECTED:
            case TYPE_LEAVE_PENDING:
                return new Intent(context, LeaveHistoryActivity.class);

            case TYPE_DONATION:
                return new Intent(context, DonationHistoryActivity.class);

            case TYPE_SALARY:
                return new Intent(context, SalaryActivity.class);

            default:
                return new Intent(context, NotificationsActivity.class);
        }
    }

    // Helper method to get notification icon
    private int getNotificationIcon(String type) {
        if (type == null) {
            return R.drawable.ic_notification;
        }

        switch (type) {
            case TYPE_EMERGENCY:
                return android.R.drawable.ic_dialog_alert;
            case TYPE_LEAVE_APPROVED:
                return R.drawable.ic_approve;
            case TYPE_LEAVE_REJECTED:
                return R.drawable.ic_reject;
            case TYPE_DONATION:
                return android.R.drawable.ic_menu_share;
            case TYPE_SALARY:
                return android.R.drawable.ic_menu_save;
            default:
                return R.drawable.ic_notification;
        }
    }

    // Helper method to get notification color
    private int getNotificationColor(String type) {
        if (type == null) {
            return context.getResources().getColor(R.color.purple_500);
        }

        switch (type) {
            case TYPE_EMERGENCY:
                return Color.RED;
            case TYPE_LEAVE_APPROVED:
                return Color.GREEN;
            case TYPE_LEAVE_REJECTED:
                return Color.YELLOW;
            case TYPE_DONATION:
                return Color.BLUE;
            case TYPE_SALARY:
                return Color.MAGENTA;
            default:
                return context.getResources().getColor(R.color.purple_500);
        }
    }

    // Helper method to get notification sound
    private Uri getNotificationSound(String type) {
        if (TYPE_EMERGENCY.equals(type)) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        } else {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
    }

    // Helper method to get vibration pattern
    private long[] getVibrationPattern(String type) {
        if (TYPE_EMERGENCY.equals(type)) {
            return new long[]{0, 500, 200, 500, 200, 500}; // Urgent pattern
        } else if (TYPE_LEAVE_APPROVED.equals(type)) {
            return new long[]{0, 200, 100, 200}; // Success pattern
        } else {
            return new long[]{0, 300, 200, 300}; // Default pattern
        }
    }

    // Helper method to get notification category
    private String getNotificationCategory(String type) {
        if (TYPE_EMERGENCY.equals(type)) {
            return NotificationCompat.CATEGORY_ALARM;
        } else if (TYPE_LEAVE_APPROVED.equals(type) || TYPE_LEAVE_REJECTED.equals(type)) {
            return NotificationCompat.CATEGORY_STATUS;
        } else {
            return NotificationCompat.CATEGORY_REMINDER;
        }
    }

    // Generate unique notification ID
    private int generateNotificationId() {
        return (int) System.currentTimeMillis() % Integer.MAX_VALUE;
    }

    // Get a random notification ID (alternative)
    private int getRandomNotificationId() {
        return new Random().nextInt(10000);
    }

    // Check if notifications are enabled
    public boolean areNotificationsEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return notificationManager != null && notificationManager.areNotificationsEnabled();
        }
        return true; // For older versions, assume enabled
    }

    // Cancel all notifications
    public void cancelAllNotifications() {
        if (notificationManager != null) {
            notificationManager.cancelAll();
            Log.d("NOTIFICATION_HELPER", "📭 All notifications cancelled");
        }
    }

    // Cancel specific notification
    public void cancelNotification(int notificationId) {
        if (notificationManager != null) {
            notificationManager.cancel(notificationId);
            Log.d("NOTIFICATION_HELPER", "📭 Notification cancelled: " + notificationId);
        }
    }

    // Get channel ID
    public static String getChannelId() {
        return CHANNEL_ID;
    }

    // Get channel name
    public static String getChannelName() {
        return CHANNEL_NAME;
    }

    // Test notification
    public void testNotification() {
        showSimpleNotification(
                "Test Notification",
                "This is a test notification to verify the notification system is working properly.",
                TYPE_GENERAL
        );
    }
}