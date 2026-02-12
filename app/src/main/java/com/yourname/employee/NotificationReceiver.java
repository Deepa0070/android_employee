package com.yourname.employee;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationReceiver";

    // Action constants
    public static final String ACTION_EMERGENCY_APPROVE = "com.yourname.employee.ACTION_EMERGENCY_APPROVE";
    public static final String ACTION_EMERGENCY_REJECT = "com.yourname.employee.ACTION_EMERGENCY_REJECT";
    public static final String ACTION_LEAVE_APPROVED = "com.yourname.employee.ACTION_LEAVE_APPROVED";
    public static final String ACTION_LEAVE_REJECTED = "com.yourname.employee.ACTION_LEAVE_REJECTED";
    public static final String ACTION_NEW_LEAVE = "com.yourname.employee.ACTION_NEW_LEAVE";
    public static final String ACTION_VIEW_LEAVES = "com.yourname.employee.ACTION_VIEW_LEAVES";
    public static final String ACTION_VIEW_NOTIFICATIONS = "com.yourname.employee.ACTION_VIEW_NOTIFICATIONS";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Notification action received: " + action);

        if (action == null) {
            Log.e(TAG, "Action is null");
            return;
        }

        try {
            switch (action) {
                case ACTION_EMERGENCY_APPROVE:
                    handleEmergencyApprove(context, intent);
                    break;

                case ACTION_EMERGENCY_REJECT:
                    handleEmergencyReject(context, intent);
                    break;

                case ACTION_LEAVE_APPROVED:
                    handleLeaveApproved(context, intent);
                    break;

                case ACTION_LEAVE_REJECTED:
                    handleLeaveRejected(context, intent);
                    break;

                case ACTION_NEW_LEAVE:
                    handleNewLeave(context, intent);
                    break;

                case ACTION_VIEW_LEAVES:
                    handleViewLeaves(context, intent);
                    break;

                case ACTION_VIEW_NOTIFICATIONS:
                    handleViewNotifications(context, intent);
                    break;

                default:
                    Log.w(TAG, "Unknown action: " + action);
                    break;
            }

            // Cancel the notification after action is performed
            int notificationId = intent.getIntExtra("notification_id", -1);
            if (notificationId != -1) {
                cancelNotification(context, notificationId);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error handling notification action: " + e.getMessage(), e);
            Toast.makeText(context, "Error handling notification", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleEmergencyApprove(Context context, Intent intent) {
        String emergencyRequestId = intent.getStringExtra("emergency_request_id");
        String employeeName = intent.getStringExtra("employee_name");
        String leaveType = intent.getStringExtra("leave_type");
        int numberOfDays = intent.getIntExtra("number_of_days", 1);

        Log.d("EMERGENCY_NOTIFY", "Emergency leave approved from notification: " + employeeName);

        // Show toast
        Toast.makeText(context, "Approving emergency leave for " + employeeName, Toast.LENGTH_SHORT).show();

        // Open the app to emergency requests activity
        Intent appIntent = new Intent(context, EmergencyRequestsActivity.class);
        appIntent.putExtra("emergencyRequestId", emergencyRequestId);
        appIntent.putExtra("auto_approve", true);
        appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(appIntent);
    }

    private void handleEmergencyReject(Context context, Intent intent) {
        String emergencyRequestId = intent.getStringExtra("emergency_request_id");
        String employeeName = intent.getStringExtra("employee_name");

        Log.d("EMERGENCY_NOTIFY", "Emergency leave rejected from notification: " + employeeName);

        // Show toast
        Toast.makeText(context, "Rejecting emergency leave for " + employeeName, Toast.LENGTH_SHORT).show();

        // Open the app to emergency requests activity
        Intent appIntent = new Intent(context, EmergencyRequestsActivity.class);
        appIntent.putExtra("emergencyRequestId", emergencyRequestId);
        appIntent.putExtra("auto_reject", true);
        appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(appIntent);
    }

    private void handleLeaveApproved(Context context, Intent intent) {
        String leaveId = intent.getStringExtra("leave_id");
        String employeeName = intent.getStringExtra("employee_name");
        String leaveType = intent.getStringExtra("leave_type");
        int numberOfDays = intent.getIntExtra("number_of_days", 1);

        Log.d(TAG, "Leave approved for: " + employeeName +
                ", ID: " + leaveId +
                ", Type: " + leaveType +
                ", Days: " + numberOfDays);

        // Show confirmation toast
        String message = "Leave approved for " + employeeName;
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

        // Open LeaveHistoryActivity
        Intent newIntent = new Intent(context, LeaveHistoryActivity.class);
        newIntent.putExtra("show_snackbar", true);
        newIntent.putExtra("snackbar_message", "Leave approved successfully!");
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(newIntent);
    }

    private void handleLeaveRejected(Context context, Intent intent) {
        String leaveId = intent.getStringExtra("leave_id");
        String employeeName = intent.getStringExtra("employee_name");
        String leaveType = intent.getStringExtra("leave_type");
        String reason = intent.getStringExtra("rejection_reason");

        Log.d(TAG, "Leave rejected for: " + employeeName +
                ", ID: " + leaveId +
                ", Type: " + leaveType +
                ", Reason: " + reason);

        // Show confirmation toast
        String message = "Leave rejected for " + employeeName;
        if (reason != null && !reason.isEmpty()) {
            message += ": " + reason;
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

        // Open LeaveHistoryActivity
        Intent newIntent = new Intent(context, LeaveHistoryActivity.class);
        newIntent.putExtra("show_snackbar", true);
        newIntent.putExtra("snackbar_message", "Leave request rejected");
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(newIntent);
    }

    private void handleNewLeave(Context context, Intent intent) {
        String employeeName = intent.getStringExtra("employee_name");
        String leaveType = intent.getStringExtra("leave_type");
        int numberOfDays = intent.getIntExtra("number_of_days", 1);
        String leaveId = intent.getStringExtra("leave_id");

        Log.d(TAG, "New leave from: " + employeeName +
                ", Type: " + leaveType +
                ", Days: " + numberOfDays +
                ", ID: " + leaveId);

        // Show confirmation toast
        String message = "New leave application from " + employeeName;
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

        // Open appropriate dashboard based on user role
        Intent newIntent = new Intent(context, ManagerDashboardActivity.class);
        newIntent.putExtra("show_pending_leaves", true);
        newIntent.putExtra("leave_id", leaveId);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(newIntent);
    }

    private void handleViewLeaves(Context context, Intent intent) {
        Log.d(TAG, "View leaves action triggered");

        // Show confirmation toast
        Toast.makeText(context, "Opening leave history", Toast.LENGTH_SHORT).show();

        // Open LeaveHistoryActivity
        Intent newIntent = new Intent(context, LeaveHistoryActivity.class);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(newIntent);
    }

    private void handleViewNotifications(Context context, Intent intent) {
        Log.d(TAG, "View notifications action triggered");

        // Show confirmation toast
        Toast.makeText(context, "Opening notifications", Toast.LENGTH_SHORT).show();

        // Open NotificationsActivity
        Intent newIntent = new Intent(context, NotificationsActivity.class);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(newIntent);
    }

    private void cancelNotification(Context context, int notificationId) {
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            // Check permission before canceling
            if (hasNotificationPermission(context)) {
                notificationManager.cancel(notificationId);
                Log.d(TAG, "Notification cancelled: " + notificationId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling notification: " + e.getMessage());
        }
    }

    // Helper method to create notification with actions
    public static NotificationCompat.Builder createNotificationWithActions(Context context,
                                                                           String channelId, String title, String message, String type, int notificationId) {

        // Main intent when notification is clicked
        Intent mainIntent = new Intent(context, LoginActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mainIntent.putExtra("notification_type", type);

        // Create pending intent flags
        int flags = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M ?
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE :
                android.app.PendingIntent.FLAG_UPDATE_CURRENT;

        android.app.PendingIntent mainPendingIntent = android.app.PendingIntent.getActivity(
                context,
                notificationId,
                mainIntent,
                flags
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(mainPendingIntent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVibrate(new long[]{0, 300, 200, 300});

        // Add actions based on notification type
        if (type.contains("leave_approved") || type.contains("leave_rejected")) {
            // Add view leaves action
            Intent viewLeavesIntent = new Intent(context, NotificationReceiver.class);
            viewLeavesIntent.setAction(ACTION_VIEW_LEAVES);
            viewLeavesIntent.putExtra("notification_id", notificationId);

            android.app.PendingIntent viewLeavesPendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    notificationId + 1,
                    viewLeavesIntent,
                    flags
            );

            builder.addAction(
                    android.R.drawable.ic_menu_view,
                    "View Leaves",
                    viewLeavesPendingIntent
            );
        }

        if (type.contains("new_leave_application")) {
            // Add approve and reject actions for managers
            Intent approveIntent = new Intent(context, NotificationReceiver.class);
            approveIntent.setAction(ACTION_LEAVE_APPROVED);
            approveIntent.putExtra("notification_id", notificationId);

            Intent rejectIntent = new Intent(context, NotificationReceiver.class);
            rejectIntent.setAction(ACTION_LEAVE_REJECTED);
            rejectIntent.putExtra("notification_id", notificationId);

            android.app.PendingIntent approvePendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    notificationId + 2,
                    approveIntent,
                    flags
            );

            android.app.PendingIntent rejectPendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    notificationId + 3,
                    rejectIntent,
                    flags
            );

            builder.addAction(
                    android.R.drawable.ic_menu_upload,
                    "Approve",
                    approvePendingIntent
            ).addAction(
                    android.R.drawable.ic_menu_delete,
                    "Reject",
                    rejectPendingIntent
            );
        }

        // Add emergency actions
        if (type.contains("emergency")) {
            Intent emergencyApproveIntent = new Intent(context, NotificationReceiver.class);
            emergencyApproveIntent.setAction(ACTION_EMERGENCY_APPROVE);
            emergencyApproveIntent.putExtra("notification_id", notificationId);

            Intent emergencyRejectIntent = new Intent(context, NotificationReceiver.class);
            emergencyRejectIntent.setAction(ACTION_EMERGENCY_REJECT);
            emergencyRejectIntent.putExtra("notification_id", notificationId);

            android.app.PendingIntent emergencyApprovePendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    notificationId + 5,
                    emergencyApproveIntent,
                    flags
            );

            android.app.PendingIntent emergencyRejectPendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    notificationId + 6,
                    emergencyRejectIntent,
                    flags
            );

            builder.addAction(
                    android.R.drawable.ic_menu_upload,
                    "EMERGENCY APPROVE",
                    emergencyApprovePendingIntent
            ).addAction(
                    android.R.drawable.ic_menu_delete,
                    "EMERGENCY REJECT",
                    emergencyRejectPendingIntent
            );
        }

        // Always add view notifications action
        Intent viewNotificationsIntent = new Intent(context, NotificationReceiver.class);
        viewNotificationsIntent.setAction(ACTION_VIEW_NOTIFICATIONS);
        viewNotificationsIntent.putExtra("notification_id", notificationId);

        android.app.PendingIntent viewNotificationsPendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                notificationId + 4,
                viewNotificationsIntent,
                flags
        );

        builder.addAction(
                android.R.drawable.ic_menu_agenda,
                "Notifications",
                viewNotificationsPendingIntent
        );

        return builder;
    }

    // Helper method to send a simple notification
    public static void sendSimpleNotification(Context context, String title, String message,
                                              String type, String channelId) {
        try {
            int notificationId = (int) System.currentTimeMillis();

            NotificationCompat.Builder builder = createNotificationWithActions(
                    context, channelId, title, message, type, notificationId
            );

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            // Check if we have permission to post notifications
            if (hasNotificationPermission(context)) {
                notificationManager.notify(notificationId, builder.build());
                Log.d(TAG, "Notification sent: " + title);
            } else {
                Log.w(TAG, "Notifications are disabled by user");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending notification: " + e.getMessage(), e);
        }
    }

    // Method to check if we have notification permission
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 (API 33) and above, we need to check POST_NOTIFICATIONS permission
            return ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        } else {
            // For Android versions below 13, notification permission is granted by default
            return true;
        }
    }

    // Method to check permission with try-catch for SecurityException
    public static void sendNotificationWithPermissionCheck(Context context, String title, String message,
                                                           String type, String channelId) {
        try {
            if (hasNotificationPermission(context)) {
                sendSimpleNotification(context, title, message, type, channelId);
            } else {
                Log.w(TAG, "Cannot send notification: Permission not granted");
                // Optionally, you can request permission here or show a message to the user
                Toast.makeText(context, "Please enable notifications in app settings", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException when trying to show notification: " + e.getMessage());
            // Handle the SecurityException gracefully
            Toast.makeText(context, "Notification permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}