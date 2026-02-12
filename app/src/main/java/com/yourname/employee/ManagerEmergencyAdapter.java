package com.yourname.employee;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import models.EmergencyRequest;

public class ManagerEmergencyAdapter extends RecyclerView.Adapter<ManagerEmergencyAdapter.ViewHolder> {

    private List<EmergencyRequest> requestList;
    private Context context;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public ManagerEmergencyAdapter(List<EmergencyRequest> requestList, Context context, FirebaseFirestore db) {
        this.requestList = requestList;
        this.context = context;
        this.db = db;
        this.mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manager_emergency, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmergencyRequest request = requestList.get(position);

        // Set basic info
        holder.tvEmployeeName.setText(request.getEmployeeName());
        holder.tvDepartment.setText("Dept: " + request.getDepartment());
        holder.tvDesignation.setText("Desig: " + request.getDesignation());
        holder.tvDates.setText("Dates: " + request.getFromDate() + " to " + request.getToDate());
        holder.tvDays.setText("Days: " + request.getNumberOfDays());
        holder.tvLeaveType.setText("Type: " + request.getLeaveType());
        holder.tvReason.setText("Reason: " + request.getReason());

        if (request.getEmergencyDetails() != null && !request.getEmergencyDetails().isEmpty()) {
            holder.tvEmergencyDetails.setText("Emergency: " + request.getEmergencyDetails());
            holder.tvEmergencyDetails.setVisibility(View.VISIBLE);
        } else {
            holder.tvEmergencyDetails.setVisibility(View.GONE);
        }

        // Format request date
        if (request.getRequestDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            holder.tvRequestDate.setText("Requested: " + sdf.format(request.getRequestDate()));
        }

        // Set button listeners
        holder.btnApprove.setOnClickListener(v -> {
            showApprovalOptions(request, position);
        });

        holder.btnReject.setOnClickListener(v -> {
            rejectRequest(request, position);
        });
    }

    private void showApprovalOptions(EmergencyRequest request, int position) {
        String[] options = {"Approve", "Reject", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Emergency Leave Action");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    approveRequest(request, position);
                    break;
                case 1:
                    rejectRequest(request, position);
                    break;
            }
        });
        builder.show();
    }

    private void approveRequest(EmergencyRequest request, int position) {
        String managerId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(managerId)
                .get()
                .addOnSuccessListener(managerDoc -> {
                    String managerName = managerDoc.getString("name");

                    // Update emergency request
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "pending"); // Now goes to HR
                    updates.put("approvedBy", managerId);
                    updates.put("approvedByName", managerName);
                    updates.put("approvedDate", new Date());
                    updates.put("managerActionDate", new Date());

                    db.collection("emergencyRequests").document(request.getId())
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                // Create leave record
                                createLeaveRecord(request, managerName);

                                // Remove from list
                                requestList.remove(position);
                                notifyItemRemoved(position);

                                // Notify employee
                                notifyEmployee(request, "approved", managerName);

                                // Notify HR
                                notifyHR(request, managerName);

                                Toast.makeText(context, "Emergency leave approved! Sent to HR for final approval.", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to approve: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void rejectRequest(EmergencyRequest request, int position) {
        String managerId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(managerId)
                .get()
                .addOnSuccessListener(managerDoc -> {
                    String managerName = managerDoc.getString("name");

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "rejected");
                    updates.put("rejectedBy", managerId);
                    updates.put("rejectedByName", managerName);
                    updates.put("rejectedDate", new Date());

                    db.collection("emergencyRequests").document(request.getId())
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                // Remove from list
                                requestList.remove(position);
                                notifyItemRemoved(position);

                                // Notify employee
                                notifyEmployee(request, "rejected", managerName);

                                Toast.makeText(context, "Emergency leave rejected", Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void createLeaveRecord(EmergencyRequest request, String managerName) {
        Map<String, Object> leaveRecord = new HashMap<>();
        leaveRecord.put("userId", request.getEmployeeId());
        leaveRecord.put("employeeName", request.getEmployeeName());
        leaveRecord.put("fromDate", request.getFromDate());
        leaveRecord.put("toDate", request.getToDate());
        leaveRecord.put("reason", request.getReason() + " [Emergency - Manager Approved by: " + managerName + "]");
        leaveRecord.put("leaveType", request.getLeaveType());
        leaveRecord.put("numberOfDays", request.getNumberOfDays());
        leaveRecord.put("status", "pending"); // Pending HR approval
        leaveRecord.put("managerStatus", "approved");
        leaveRecord.put("hrStatus", "pending");
        leaveRecord.put("isLOP", false);
        leaveRecord.put("isEmergency", true);
        leaveRecord.put("emergencyDetails", request.getEmergencyDetails());
        leaveRecord.put("appliedDate", new Date());
        leaveRecord.put("managerApprovedDate", new Date());
        leaveRecord.put("approvedBy", mAuth.getCurrentUser().getUid());

        db.collection("leaveRequests").add(leaveRecord);
    }

    private void notifyEmployee(EmergencyRequest request, String action, String managerName) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", request.getEmployeeId());

        String message;
        if (action.equals("approved")) {
            message = "Your emergency leave request has been approved by manager " + managerName + ". Now pending HR approval.";
        } else {
            message = "Your emergency leave request has been rejected by manager " + managerName;
        }

        notification.put("title", "Emergency Leave " + action.toUpperCase());
        notification.put("message", message);
        notification.put("type", "emergency_" + action);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);

        db.collection("notifications").add(notification);
    }

    private void notifyHR(EmergencyRequest request, String managerName) {
        // Find HR users
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String role = document.getString("role");
                        String hrUserId = document.getId();

                        if (role != null && (role.toLowerCase().contains("hr") ||
                                role.toLowerCase().contains("admin"))) {

                            Map<String, Object> notification = new HashMap<>();
                            notification.put("userId", hrUserId);
                            notification.put("title", "Emergency Leave Manager Approved");
                            notification.put("message", managerName + " approved emergency leave for " +
                                    request.getEmployeeName() + " (" + request.getNumberOfDays() +
                                    " days " + request.getLeaveType() + ") - Needs HR final approval");
                            notification.put("type", "emergency_manager_approved");
                            notification.put("emergencyRequestId", request.getId());
                            notification.put("timestamp", System.currentTimeMillis());
                            notification.put("read", false);

                            db.collection("notifications").add(notification);
                        }
                    }
                });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmployeeName, tvDepartment, tvDesignation, tvDates, tvDays, tvLeaveType,
                tvReason, tvEmergencyDetails, tvRequestDate;
        Button btnApprove, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmployeeName = itemView.findViewById(R.id.tvEmployeeName);
            tvDepartment = itemView.findViewById(R.id.tvDepartment);
            tvDesignation = itemView.findViewById(R.id.tvDesignation);
            tvDates = itemView.findViewById(R.id.tvDates);
            tvDays = itemView.findViewById(R.id.tvDays);
            tvLeaveType = itemView.findViewById(R.id.tvLeaveType);
            tvReason = itemView.findViewById(R.id.tvReason);
            tvEmergencyDetails = itemView.findViewById(R.id.tvEmergencyDetails);
            tvRequestDate = itemView.findViewById(R.id.tvRequestDate);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}