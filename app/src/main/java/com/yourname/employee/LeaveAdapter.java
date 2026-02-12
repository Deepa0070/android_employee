package com.yourname.employee;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.LeaveRequest;

public class LeaveAdapter extends RecyclerView.Adapter<LeaveAdapter.LeaveViewHolder> {

    private List<LeaveRequest> leaveList;
    private FirebaseFirestore db;
    private Context context;
    private FirebaseAuth mAuth;

    public LeaveAdapter(List<LeaveRequest> leaveList, Context context) {
        this.leaveList = leaveList;
        this.db = FirebaseFirestore.getInstance();
        this.context = context;
        this.mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public LeaveViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leave_hr, parent, false);
        return new LeaveViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaveViewHolder holder, int position) {
        LeaveRequest leave = leaveList.get(position);

        // Set data to views
        holder.tvEmployeeName.setText(leave.getEmployeeName());
        holder.tvLeaveType.setText("Type: " + leave.getLeaveType());
        holder.tvDate.setText("Dates: " + leave.getFromDate() + " to " + leave.getToDate());
        holder.tvReason.setText("Reason: " + leave.getReason());

        // Show department and designation if available
        if (leave.getDepartment() != null && !leave.getDepartment().isEmpty()) {
            holder.tvDepartment.setText("Dept: " + leave.getDepartment());
            holder.tvDepartment.setVisibility(View.VISIBLE);
        } else {
            holder.tvDepartment.setVisibility(View.GONE);
        }

        if (leave.getDesignation() != null && !leave.getDesignation().isEmpty()) {
            holder.tvDesignation.setText("Designation: " + leave.getDesignation());
            holder.tvDesignation.setVisibility(View.VISIBLE);
        } else {
            holder.tvDesignation.setVisibility(View.GONE);
        }

        // Calculate and show number of days
        long days = leave.getDays();
        if (days <= 0) {
            days = leave.calculateDaysFromDates();
        }
        holder.tvDays.setText("Days: " + days);
        holder.tvDays.setVisibility(View.VISIBLE);

        // Set status - show manager's decision
        String managerStatus = leave.getManagerStatus();
        if (managerStatus == null) {
            managerStatus = "pending";
        }

        // Show overall status and manager status
        String statusText = "Manager: " + managerStatus.toUpperCase();
        if (managerStatus.equals("approved")) {
            statusText += " | HR: PENDING";
        }
        holder.tvStatus.setText(statusText);

        // Set status background color
        if (managerStatus.equalsIgnoreCase("approved")) {
            holder.tvStatus.setBackgroundResource(R.drawable.status_background_approved);
        } else if (managerStatus.equalsIgnoreCase("rejected")) {
            holder.tvStatus.setBackgroundResource(R.drawable.status_background_rejected);
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.status_background_pending);
        }

        // Show approve/reject buttons for leaves approved by manager but pending HR
        if (managerStatus.equals("approved") && leave.getHrStatus() != null && leave.getHrStatus().equals("pending")) {
            holder.btnApprove.setVisibility(View.VISIBLE);
            holder.btnReject.setVisibility(View.VISIBLE);

            holder.btnApprove.setOnClickListener(v -> {
                updateLeaveStatus(leave, "approved", position);
            });

            holder.btnReject.setOnClickListener(v -> {
                updateLeaveStatus(leave, "rejected", position);
            });
        } else {
            holder.btnApprove.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
        }
    }

    private void updateLeaveStatus(LeaveRequest leave, String status, int position) {
        // Update leave status in Firestore - HR is making final decision
        Map<String, Object> updates = new HashMap<>();
        updates.put("hrStatus", status);
        updates.put("status", status); // Final status

        // If approved by HR, update leave balance
        if (status.equals("approved")) {
            // Update user's leave balance
            updateUserLeaveBalance(leave);

            // Notify team leaders and managers
            notifyTeamLeader(leave);
            notifyManager(leave);
        }

        db.collection("leaveRequests").document(leave.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update local list
                    leaveList.get(position).setHrStatus(status);
                    leaveList.get(position).setStatus(status);
                    notifyItemChanged(position);

                    Toast.makeText(context, "Leave " + status + " by HR", Toast.LENGTH_SHORT).show();

                    // Create final notification for employee
                    createNotificationForEmployee(leave.getUserId(), leave.getEmployeeName(),
                            leave.getLeaveType(), status, (int) leave.getDays(), leave.getId());

                    // Remove from list
                    leaveList.remove(position);
                    notifyItemRemoved(position);

                    // Update UI if list is empty
                    if (leaveList.isEmpty()) {
                        // You can update the UI to show empty state
                        if (context instanceof HrDashboardActivity) {
                            ((HrDashboardActivity) context).loadPendingLeaves();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Method to notify team leader of the same department
    private void notifyTeamLeader(LeaveRequest leave) {
        String employeeDepartment = leave.getDepartment();
        String employeeName = leave.getEmployeeName();
        String leaveType = leave.getLeaveType();
        int numberOfDays = (int) leave.getDays();
        String fromDate = leave.getFromDate();
        String toDate = leave.getToDate();

        if (employeeDepartment == null || employeeDepartment.isEmpty()) {
            Log.d("TEAM_LEADER_NOTIFY", "No department found for employee: " + employeeName);
            return;
        }

        Log.d("TEAM_LEADER_NOTIFY", "Looking for team leaders in department: " + employeeDepartment);

        // Find team leaders in the same department
        db.collection("users")
                .whereEqualTo("department", employeeDepartment)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("TEAM_LEADER_NOTIFY", "Found " + queryDocumentSnapshots.size() + " users in " + employeeDepartment);

                    boolean teamLeaderFound = false;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String role = document.getString("role");
                        String userId = document.getId();
                        String userName = document.getString("name");

                        Log.d("TEAM_LEADER_NOTIFY", "Checking user: " + userName + " with role: " + role);

                        // Check if user is team leader (based on role)
                        if (role != null && (role.equals("team_leader") ||
                                role.equals("team leader") ||
                                role.equals("teamleader") ||
                                role.equals("Team Lead") ||
                                role.equals("Team Leader"))) {

                            teamLeaderFound = true;
                            Log.d("TEAM_LEADER_NOTIFY", "Found team leader: " + userName);

                            // Don't notify the employee themselves
                            if (userId.equals(leave.getUserId())) {
                                Log.d("TEAM_LEADER_NOTIFY", "Skipping notification to employee themselves");
                                continue;
                            }

                            // Create notification for team leader
                            Map<String, Object> notification = new HashMap<>();
                            notification.put("userId", userId);
                            notification.put("title", "Leave Approved in Your Department");
                            notification.put("message", employeeName + "'s " + numberOfDays +
                                    " days " + leaveType + " has been approved. " +
                                    "Dates: " + fromDate + " to " + toDate);
                            notification.put("type", "team_leave_approved");
                            notification.put("leaveId", leave.getId());
                            notification.put("fromUserId", mAuth.getCurrentUser().getUid());
                            notification.put("fromUserName", "HR Department");
                            notification.put("timestamp", System.currentTimeMillis());
                            notification.put("read", false);

                            db.collection("notifications")
                                    .add(notification)
                                    .addOnSuccessListener(documentReference -> {
                                        Log.d("TEAM_LEADER_NOTIFY", "Notification ID: " + documentReference.getId());
                                        Toast.makeText(context, "Notification sent to team leader", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("TEAM_LEADER_NOTIFY", "Error: " + e.getMessage());
                                    });
                        }
                    }

                    if (!teamLeaderFound) {
                        Log.d("TEAM_LEADER_NOTIFY", "No team leader found in " + employeeDepartment + " department");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TEAM_LEADER_NOTIFY", "Failed to find team leader", e);
                });
    }

    // Method to notify Manager
    private void notifyManager(LeaveRequest leave) {
        String employeeName = leave.getEmployeeName();
        String department = leave.getDepartment();
        String leaveType = leave.getLeaveType();
        int numberOfDays = (int) leave.getDays();
        String fromDate = leave.getFromDate();
        String toDate = leave.getToDate();

        Log.d("MANAGER_NOTIFY", "Looking for managers...");

        // Find ONE Manager user (limit to 1)
        db.collection("users")
                .whereEqualTo("role", "manager")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        String managerUserId = document.getId();
                        String managerName = document.getString("name");

                        // Don't notify the employee themselves
                        if (managerUserId.equals(leave.getUserId())) {
                            Log.d("MANAGER_NOTIFY", "Skipping notification to employee themselves");
                            return;
                        }

                        // Create notification for Manager
                        Map<String, Object> notification = new HashMap<>();
                        notification.put("userId", managerUserId);
                        notification.put("title", "Leave Approved - " + department + " Department");
                        notification.put("message", employeeName + " (" + department + ") " +
                                "has been approved for " + numberOfDays +
                                " days " + leaveType + ". " +
                                "Dates: " + fromDate + " to " + toDate);
                        notification.put("type", "manager_leave_approved");
                        notification.put("leaveId", leave.getId());
                        notification.put("fromUserId", mAuth.getCurrentUser().getUid());
                        notification.put("fromUserName", "HR Department");
                        notification.put("timestamp", System.currentTimeMillis());
                        notification.put("read", false);

                        db.collection("notifications")
                                .add(notification)
                                .addOnSuccessListener(documentReference -> {
                                    Log.d("MANAGER_NOTIFY", "Manager notified: " + managerName);
                                    Toast.makeText(context, "Manager " + managerName + " notified", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("MANAGER_NOTIFY", "Failed to notify Manager: " + managerName, e);
                                });
                    } else {
                        Log.d("MANAGER_NOTIFY", "No Manager found in the system");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MANAGER_NOTIFY", "Failed to find Manager", e);
                });
    }

    // Method to create notification for employee (when approved/rejected)
    private void createNotificationForEmployee(String employeeUserId, String employeeName,
                                               String leaveType, String status,
                                               int numberOfDays, String leaveId) {

        Log.d("EMPLOYEE_NOTIFY", "Creating notification for employee: " + employeeName);

        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", employeeUserId);
        notification.put("title", "Leave " + status.toUpperCase());
        notification.put("message", "Your " + leaveType + " for " + numberOfDays +
                " days has been " + status + " by HR");
        notification.put("type", "leave_" + status.toLowerCase());
        notification.put("leaveId", leaveId);
        notification.put("fromUserId", mAuth.getCurrentUser().getUid());
        notification.put("fromUserName", "HR Department");
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);

        // Save to Firestore
        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d("EMPLOYEE_NOTIFY", "Employee notification created for: " + employeeName);
                    Toast.makeText(context, "Employee " + employeeName + " notified", Toast.LENGTH_SHORT).show();

                    // ALSO SHOW SYSTEM NOTIFICATION
                    NotificationHelper notificationHelper = new NotificationHelper(context);
                    notificationHelper.showNotification(
                            "Leave " + status.toUpperCase(),
                            "Your " + leaveType + " for " + numberOfDays + " days has been " + status + " by HR",
                            "leave_" + status.toLowerCase()
                    );
                })
                .addOnFailureListener(e -> {
                    Log.e("EMPLOYEE_NOTIFY", "Failed to create employee notification", e);
                });
    }

    private void updateUserLeaveBalance(LeaveRequest leave) {
        String userId = leave.getUserId();
        String leaveType = leave.getLeaveType();

        // Calculate number of days and store in final variable
        final long numberOfDays;
        if (leave.getDays() <= 0) {
            numberOfDays = leave.calculateDaysFromDates();
        } else {
            numberOfDays = leave.getDays();
        }

        // Get current user data
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Handle different leave types
                        if (leaveType.equals("Casual Leave")) {
                            // Deduct from casual leave balance
                            long deductionValue = -numberOfDays;
                            db.collection("users").document(userId)
                                    .update("leaveBalance.casual", FieldValue.increment(deductionValue))
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("LEAVE_BALANCE", "Casual leave balance updated (-" + numberOfDays + " days)");
                                    });

                        } else if (leaveType.equals("Sick Leave")) {
                            // Deduct from sick leave balance
                            long deductionValue = -numberOfDays;
                            db.collection("users").document(userId)
                                    .update("leaveBalance.sick", FieldValue.increment(deductionValue))
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("LEAVE_BALANCE", "Sick leave balance updated (-" + numberOfDays + " days)");
                                    });

                        } else if (leaveType.equals("Loss of Pay (LOP)")) {
                            // Update LOP balance and current month LOP days
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("leaveBalance.lop", FieldValue.increment(numberOfDays));
                            updates.put("lopDaysThisMonth", FieldValue.increment(numberOfDays));

                            db.collection("users").document(userId)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("LEAVE_BALANCE", "LOP Updated: Added " + numberOfDays + " day(s) to LOP balance");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("LEAVE_BALANCE", "Failed to update LOP: " + e.getMessage());
                                    });
                        }
                    } else {
                        Log.e("LEAVE_BALANCE", "User not found in database");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("LEAVE_BALANCE", "Failed to update leave balance: " + e.getMessage());
                });
    }

    @Override
    public int getItemCount() {
        return leaveList != null ? leaveList.size() : 0;
    }

    // LeaveViewHolder inner class
    static class LeaveViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmployeeName, tvLeaveType, tvDate, tvReason, tvStatus, tvDays;
        TextView tvDepartment, tvDesignation;
        Button btnApprove, btnReject;

        public LeaveViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmployeeName = itemView.findViewById(R.id.tvEmployeeName);
            tvLeaveType = itemView.findViewById(R.id.tvLeaveType);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvReason = itemView.findViewById(R.id.tvReason);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDays = itemView.findViewById(R.id.tvDays);
            tvDepartment = itemView.findViewById(R.id.tvDepartment);
            tvDesignation = itemView.findViewById(R.id.tvDesignation);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}