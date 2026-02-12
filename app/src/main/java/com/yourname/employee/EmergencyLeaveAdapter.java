package com.yourname.employee;

import android.content.Context;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EmergencyLeaveAdapter extends RecyclerView.Adapter<EmergencyLeaveAdapter.ViewHolder> {

    private List<Map<String, Object>> emergencyLeaveList;
    private Context context;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public EmergencyLeaveAdapter(List<Map<String, Object>> emergencyLeaveList, Context context) {
        this.emergencyLeaveList = emergencyLeaveList;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_emergency_leave, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> leave = emergencyLeaveList.get(position);

        // Set employee details
        holder.tvEmployeeName.setText((String) leave.get("employeeName"));
        holder.tvDepartment.setText("Dept: " + leave.get("department"));
        holder.tvDesignation.setText("Designation: " + leave.get("designation"));

        // Set leave details
        holder.tvLeaveDates.setText("Dates: " + leave.get("fromDate") + " to " + leave.get("toDate"));
        holder.tvDays.setText("Days: " + leave.get("numberOfDays"));
        holder.tvReason.setText("Reason: " + leave.get("reason"));

        // Set applied date
        if (leave.get("appliedDate") != null) {
            Date appliedDate = (Date) leave.get("appliedDate");
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault());
            holder.tvAppliedDate.setText("Applied: " + sdf.format(appliedDate));
        }

        // Set status
        String status = (String) leave.get("status");
        String hrStatus = (String) leave.get("hrStatus");
        holder.tvStatus.setText(status != null ? status.toUpperCase() : "PENDING");

        // Show/Hide action buttons based on status
        // FIX: Check both status and hrStatus
        boolean isPending = "pending".equals(status) || "pending".equals(hrStatus);
        boolean isApproved = "approved".equals(status) || "approved".equals(hrStatus);
        boolean isRejected = "rejected".equals(status) || "rejected".equals(hrStatus);

        if (isPending) {
            holder.layoutActions.setVisibility(View.VISIBLE);

            holder.btnApprove.setOnClickListener(v -> {
                approveEmergencyLeave(leave, position);
            });

            holder.btnReject.setOnClickListener(v -> {
                rejectEmergencyLeave(leave, position);
            });
        } else {
            holder.layoutActions.setVisibility(View.GONE);

            if (isApproved) {
                holder.tvStatus.setText("APPROVED");
                holder.tvStatus.setBackgroundResource(R.drawable.status_background_approved);
            } else if (isRejected) {
                holder.tvStatus.setText("REJECTED");
                holder.tvStatus.setBackgroundResource(R.drawable.status_background_rejected);
            }
        }
    }

    private void approveEmergencyLeave(Map<String, Object> leave, int position) {
        String leaveId = (String) leave.get("id");
        String employeeId = (String) leave.get("userId");
        int numberOfDays;

        // Handle different data types for numberOfDays
        Object daysObj = leave.get("numberOfDays");
        if (daysObj instanceof Long) {
            numberOfDays = ((Long) daysObj).intValue();
        } else if (daysObj instanceof Integer) {
            numberOfDays = (Integer) daysObj;
        } else {
            numberOfDays = 0;
        }

        String hrId = mAuth.getCurrentUser().getUid();

        // Get HR name
        db.collection("users").document(hrId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String hrName = documentSnapshot.getString("name");

                    // Update leave status
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "approved");
                    updates.put("hrStatus", "approved");
                    updates.put("hrId", hrId);
                    updates.put("hrName", hrName);
                    updates.put("hrApprovedDate", new Date());

                    db.collection("leaveRequests").document(leaveId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                // Update employee's LOP days
                                updateEmployeeLOP(employeeId, numberOfDays);

                                // Update local list
                                emergencyLeaveList.get(position).put("status", "approved");
                                emergencyLeaveList.get(position).put("hrStatus", "approved");
                                notifyItemChanged(position);

                                // Send notification to employee
                                sendNotification(employeeId, "Emergency Leave Approved",
                                        "Your emergency leave of " + numberOfDays + " days has been approved as LOP.");

                                Toast.makeText(context, "Emergency leave approved", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to approve: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void rejectEmergencyLeave(Map<String, Object> leave, int position) {
        String leaveId = (String) leave.get("id");
        String employeeId = (String) leave.get("userId");
        int numberOfDays;

        Object daysObj = leave.get("numberOfDays");
        if (daysObj instanceof Long) {
            numberOfDays = ((Long) daysObj).intValue();
        } else if (daysObj instanceof Integer) {
            numberOfDays = (Integer) daysObj;
        } else {
            numberOfDays = 0;
        }

        String hrId = mAuth.getCurrentUser().getUid();

        // Get HR name
        db.collection("users").document(hrId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String hrName = documentSnapshot.getString("name");

                    // Update leave status
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "rejected");
                    updates.put("hrStatus", "rejected");
                    updates.put("hrId", hrId);
                    updates.put("hrName", hrName);
                    updates.put("hrRejectedDate", new Date());

                    db.collection("leaveRequests").document(leaveId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                // Update local list
                                emergencyLeaveList.get(position).put("status", "rejected");
                                emergencyLeaveList.get(position).put("hrStatus", "rejected");
                                notifyItemChanged(position);

                                // Send notification to employee
                                sendNotification(employeeId, "Emergency Leave Rejected",
                                        "Your emergency leave of " + numberOfDays + " days has been rejected.");

                                Toast.makeText(context, "Emergency leave rejected", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to reject: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void updateEmployeeLOP(String employeeId, int lopDays) {
        // Update LOP in leave balance
        db.collection("users").document(employeeId)
                .update(
                        "leaveBalance.lop", FieldValue.increment(lopDays),
                        "lopDaysThisMonth", FieldValue.increment(lopDays)
                );
    }

    private void sendNotification(String userId, String title, String message) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("type", "emergency_leave_status");
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);

        db.collection("notifications").add(notification);
    }

    @Override
    public int getItemCount() {
        return emergencyLeaveList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmployeeName, tvDepartment, tvDesignation, tvLeaveDates;
        TextView tvDays, tvReason, tvAppliedDate, tvStatus;
        View layoutActions;
        Button btnApprove, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmployeeName = itemView.findViewById(R.id.tvEmployeeName);
            tvDepartment = itemView.findViewById(R.id.tvDepartment);
            tvDesignation = itemView.findViewById(R.id.tvDesignation);
            tvLeaveDates = itemView.findViewById(R.id.tvLeaveDates);
            tvDays = itemView.findViewById(R.id.tvDays);
            tvReason = itemView.findViewById(R.id.tvReason);
            tvAppliedDate = itemView.findViewById(R.id.tvAppliedDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}