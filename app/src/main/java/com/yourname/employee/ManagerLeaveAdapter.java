package com.yourname.employee;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.LeaveRequest;

public class ManagerLeaveAdapter extends RecyclerView.Adapter<ManagerLeaveAdapter.ManagerLeaveViewHolder> {

    private List<LeaveRequest> leaveList;
    private FirebaseFirestore db;
    private Context context;
    private FirebaseAuth mAuth;

    public ManagerLeaveAdapter(List<LeaveRequest> leaveList, Context context) {
        this.leaveList = leaveList;
        this.db = FirebaseFirestore.getInstance();
        this.context = context;
        this.mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public ManagerLeaveViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leave_manager, parent, false);
        return new ManagerLeaveViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ManagerLeaveViewHolder holder, int position) {
        LeaveRequest leave = leaveList.get(position);

        // Set data to views
        holder.tvEmployeeName.setText(leave.getEmployeeName());
        holder.tvLeaveType.setText("Type: " + leave.getLeaveType());
        holder.tvDate.setText("Dates: " + leave.getFromDate() + " to " + leave.getToDate());
        holder.tvReason.setText("Reason: " + leave.getReason());

        // Show department and designation
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

        // Set status (showing manager's decision status)
        String managerStatus = leave.getManagerStatus();
        if (managerStatus == null) {
            managerStatus = "pending";
        }
        holder.tvStatus.setText("Manager: " + managerStatus.toUpperCase());

        // Set status background color
        if (managerStatus.equalsIgnoreCase("approved")) {
            holder.tvStatus.setBackgroundResource(R.drawable.status_background_approved);
        } else if (managerStatus.equalsIgnoreCase("rejected")) {
            holder.tvStatus.setBackgroundResource(R.drawable.status_background_rejected);
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.status_background_pending);
        }

        // Show approve/reject buttons for pending leaves
        if (managerStatus.equals("pending")) {
            holder.btnApprove.setVisibility(View.VISIBLE);
            holder.btnReject.setVisibility(View.VISIBLE);

            holder.btnApprove.setOnClickListener(v -> {
                updateManagerLeaveStatus(leave, "approved", position);
            });

            holder.btnReject.setOnClickListener(v -> {
                updateManagerLeaveStatus(leave, "rejected", position);
            });
        } else {
            holder.btnApprove.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
        }
    }

    private void updateManagerLeaveStatus(LeaveRequest leave, String status, int position) {
        // Get current manager info
        String managerId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(managerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String managerName = documentSnapshot.getString("name");

                        // Update leave status in Firestore
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("managerStatus", status);
                        updates.put("managerId", managerId);
                        updates.put("managerName", managerName);
                        updates.put("status", "manager_" + status); // Overall status

                        db.collection("leaveRequests").document(leave.getId())
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    // Update local list
                                    leaveList.get(position).setManagerStatus(status);
                                    leaveList.get(position).setStatus("manager_" + status);
                                    notifyItemChanged(position);

                                    Toast.makeText(context, "Leave " + status + " by manager", Toast.LENGTH_SHORT).show();

                                    // 1. Create notification for employee
                                    createNotificationForEmployee(leave.getUserId(), leave.getEmployeeName(),
                                            leave.getLeaveType(), "manager_" + status, (int) leave.getDays(), leave.getId());

                                    // 2. Create notification for HR (only if approved)
                                    if (status.equals("approved")) {
                                        notifyHRDepartment(leave, managerName);
                                    }

                                    // 3. Create notification for Team Leaders in same department
                                    if (status.equals("approved")) {
                                        notifyTeamLeaders(leave, managerName);
                                    }

                                    // Remove from list since it's no longer pending
                                    leaveList.remove(position);
                                    notifyItemRemoved(position);

                                    // Update count
                                    if (leaveList.isEmpty()) {
                                        // You might want to update UI to show empty state
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(context, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                });
    }

    private void createNotificationForEmployee(String employeeUserId, String employeeName,
                                               String leaveType, String status,
                                               int numberOfDays, String leaveId) {

        Log.d("MANAGER_NOTIFY", "Creating notification for employee: " + employeeName);

        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", employeeUserId);
        notification.put("title", "Leave " + status.toUpperCase());
        notification.put("message", "Your " + leaveType + " for " + numberOfDays +
                " days has been " + status + " by your Manager");
        notification.put("type", "leave_" + status.toLowerCase());
        notification.put("leaveId", leaveId);
        notification.put("fromUserId", mAuth.getCurrentUser().getUid());
        notification.put("fromUserName", "Your Manager");
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);

        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d("MANAGER_NOTIFY", "Employee notification created for: " + employeeName);

                    // Add system notification
                    NotificationHelper notificationHelper = new NotificationHelper(context);
                    notificationHelper.showNotification(
                            "Leave " + status.toUpperCase(),
                            "Your " + leaveType + " for " + numberOfDays + " days has been " + status + " by your Manager",
                            "leave_" + status.toLowerCase()
                    );
                })
                .addOnFailureListener(e -> {
                    Log.e("MANAGER_NOTIFY", "Failed to create employee notification", e);
                });
    }

    private void notifyHRDepartment(LeaveRequest leave, String managerName) {
        String employeeName = leave.getEmployeeName();
        String department = leave.getDepartment();
        String leaveType = leave.getLeaveType();
        int numberOfDays = (int) leave.getDays();
        String fromDate = leave.getFromDate();
        String toDate = leave.getToDate();

        Log.d("MANAGER_NOTIFY", "Notifying HR department about approved leave");

        // Find only ONE HR user (limit to 1)
        db.collection("users")
                .whereEqualTo("role", "hr admin")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        String hrUserId = document.getId();
                        String hrUserName = document.getString("name");

                        // Create notification for HR
                        Map<String, Object> notification = new HashMap<>();
                        notification.put("userId", hrUserId);
                        notification.put("title", "Leave Awaiting HR Approval");
                        notification.put("message", employeeName + " (" + department + ") " +
                                "has been approved by Manager " + managerName + " for " + numberOfDays +
                                " days " + leaveType + ". " +
                                "Dates: " + fromDate + " to " + toDate);
                        notification.put("type", "manager_approved_leave");
                        notification.put("leaveId", leave.getId());
                        notification.put("fromUserId", mAuth.getCurrentUser().getUid());
                        notification.put("fromUserName", "Manager " + managerName);
                        notification.put("timestamp", System.currentTimeMillis());
                        notification.put("read", false);

                        db.collection("notifications")
                                .add(notification)
                                .addOnSuccessListener(documentReference -> {
                                    Log.d("MANAGER_NOTIFY", "HR notified: " + hrUserName);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("MANAGER_NOTIFY", "Failed to notify HR: " + hrUserName, e);
                                });
                    }
                });
    }

    private void notifyTeamLeaders(LeaveRequest leave, String managerName) {
        String employeeDepartment = leave.getDepartment();
        String employeeName = leave.getEmployeeName();
        String leaveType = leave.getLeaveType();
        int numberOfDays = (int) leave.getDays();
        String fromDate = leave.getFromDate();
        String toDate = leave.getToDate();

        if (employeeDepartment == null || employeeDepartment.isEmpty()) {
            return;
        }

        // Find team leaders in the same department
        db.collection("users")
                .whereEqualTo("department", employeeDepartment)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String role = document.getString("role");
                        String userId = document.getId();
                        String userName = document.getString("name");

                        // Check if user is team leader
                        if (role != null && (role.equals("team_leader") ||
                                role.equals("team leader") ||
                                role.equals("teamleader") ||
                                role.equals("Team Lead") ||
                                role.equals("Team Leader"))) {

                            // Don't notify the employee themselves
                            if (userId.equals(leave.getUserId())) {
                                continue;
                            }

                            // Create notification for team leader
                            Map<String, Object> notification = new HashMap<>();
                            notification.put("userId", userId);
                            notification.put("title", "Leave Approved in Your Department");
                            notification.put("message", employeeName + "'s " + numberOfDays +
                                    " days " + leaveType + " has been approved by Manager " + managerName + ". " +
                                    "Dates: " + fromDate + " to " + toDate);
                            notification.put("type", "team_leave_approved");
                            notification.put("leaveId", leave.getId());
                            notification.put("fromUserId", mAuth.getCurrentUser().getUid());
                            notification.put("fromUserName", "Manager " + managerName);
                            notification.put("timestamp", System.currentTimeMillis());
                            notification.put("read", false);

                            db.collection("notifications")
                                    .add(notification)
                                    .addOnSuccessListener(documentReference -> {
                                        Log.d("MANAGER_NOTIFY", "Team leader notified: " + userName);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("MANAGER_NOTIFY", "Failed to notify team leader: " + userName, e);
                                    });
                        }
                    }
                });
    }

    @Override
    public int getItemCount() {
        return leaveList != null ? leaveList.size() : 0;
    }

    static class ManagerLeaveViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmployeeName, tvLeaveType, tvDate, tvReason, tvStatus, tvDays;
        TextView tvDepartment, tvDesignation;
        Button btnApprove, btnReject;

        public ManagerLeaveViewHolder(@NonNull View itemView) {
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