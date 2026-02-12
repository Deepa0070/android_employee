package com.yourname.employee;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
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

public class EmergencyRequestAdapter extends RecyclerView.Adapter<EmergencyRequestAdapter.ViewHolder> {

    private List<EmergencyRequest> requestList;
    private Context context;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public EmergencyRequestAdapter(List<EmergencyRequest> requestList, Context context, FirebaseFirestore db) {
        this.requestList = requestList;
        this.context = context;
        this.db = db;
        this.mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_emergency_request, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmergencyRequest request = requestList.get(position);

        holder.tvEmployeeName.setText(request.getEmployeeName());
        holder.tvDepartment.setText("Dept: " + request.getDepartment());
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
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
            holder.tvRequestDate.setText("Requested: " + sdf.format(request.getRequestDate()));
        }

        // Load donation pool availability and display
        loadPoolAvailability(request, holder);

        // Show approve/reject buttons for pending requests
        String status = request.getStatus();
        if (status != null && (status.equals("pending") || status.equals("pending_manager"))) {
            holder.btnApprove.setVisibility(View.VISIBLE);
            holder.btnReject.setVisibility(View.VISIBLE);

            holder.btnApprove.setOnClickListener(v -> {
                // Show confirmation dialog before approving
                showApprovalConfirmationDialog(request, position);
            });

            holder.btnReject.setOnClickListener(v -> {
                showRejectionConfirmationDialog(request, position);
            });
        } else {
            holder.btnApprove.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);

            // If approved, show approval info
            if (status != null && status.equals("approved")) {
                if (holder.tvApprovalInfo != null) {
                    holder.tvApprovalInfo.setVisibility(View.VISIBLE);
                    if (request.getHrName() != null) {
                        holder.tvApprovalInfo.setText("Approved by: " + request.getHrName());
                    }
                }
            }
        }
    }

    private void loadPoolAvailability(EmergencyRequest request, ViewHolder holder) {
        String poolType = request.getLeaveType().equals("Casual Leave") ? "casual" : "sick";

        // Check Common Pool availability
        db.collection("commonLeavePool")
                .whereEqualTo("leaveType", poolType)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot poolDoc = queryDocumentSnapshots.getDocuments().get(0);
                        Long total = poolDoc.getLong("totalDays");
                        Long used = poolDoc.getLong("usedDays");

                        if (total != null && used != null) {
                            int available = total.intValue() - used.intValue();
                            int required = request.getNumberOfDays();

                            // Update the donation info TextView
                            if (holder.tvDonationInfo != null) {
                                String info = String.format(
                                        "Available in Common Pool: %d days\nRequired: %d days\nStatus: %s",
                                        available, required,
                                        available >= required ? "✅ Sufficient" : "❌ Insufficient"
                                );
                                holder.tvDonationInfo.setText(info);
                                holder.tvDonationInfo.setVisibility(View.VISIBLE);

                                if (available >= required) {
                                    holder.tvDonationInfo.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
                                } else {
                                    holder.tvDonationInfo.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
                                }
                            }
                        }
                    } else {
                        // No pool exists
                        if (holder.tvDonationInfo != null) {
                            holder.tvDonationInfo.setText("⚠️ Common Pool not initialized");
                            holder.tvDonationInfo.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
                        }
                    }
                });
    }

    private void showApprovalConfirmationDialog(EmergencyRequest request, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Approve Emergency Leave");

        String message = "Employee: " + request.getEmployeeName() + "\n" +
                "Days: " + request.getNumberOfDays() + " days " + request.getLeaveType() + "\n" +
                "Dates: " + request.getFromDate() + " to " + request.getToDate() + "\n\n" +
                "Are you sure you want to approve this emergency leave?";

        builder.setMessage(message)
                .setPositiveButton("APPROVE", (dialog, which) -> {
                    checkPoolAvailabilityAndApprove(request, position);
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void showRejectionConfirmationDialog(EmergencyRequest request, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Reject Emergency Leave");

        String message = "Are you sure you want to reject " + request.getEmployeeName() +
                "'s emergency leave request for " + request.getNumberOfDays() + " days?";

        builder.setMessage(message)
                .setPositiveButton("REJECT", (dialog, which) -> {
                    rejectRequest(request, position);
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void checkPoolAvailabilityAndApprove(EmergencyRequest request, int position) {
        String poolType = request.getLeaveType().equals("Casual Leave") ? "casual" : "sick";
        int requiredDays = request.getNumberOfDays();

        db.collection("commonLeavePool")
                .whereEqualTo("leaveType", poolType)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot poolDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String poolId = poolDoc.getId();
                        int total = poolDoc.getLong("totalDays").intValue();
                        int used = poolDoc.getLong("usedDays").intValue();
                        int available = total - used;

                        if (available >= requiredDays) {
                            // Enough in pool, approve using pool
                            approveUsingCommonPool(request, position, poolId, available, requiredDays);
                        } else {
                            // Not enough in pool, show options
                            showInsufficientPoolOptions(request, position, available, requiredDays);
                        }
                    } else {
                        // No pool exists
                        showInsufficientPoolOptions(request, position, 0, requiredDays);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to check pool: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void approveUsingCommonPool(EmergencyRequest request, int position, String poolId, int available, int requiredDays) {
        String hrId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(hrId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String hrName = documentSnapshot.getString("name");
                    if (hrName == null) hrName = "HR";

                    // Update pool
                    String finalHrName = hrName;
                    db.collection("commonLeavePool").document(poolId)
                            .update("usedDays", FieldValue.increment(requiredDays))
                            .addOnSuccessListener(aVoid -> {
                                // Complete approval
                                completeApproval(request, position, hrId, finalHrName, "common_pool", requiredDays);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to update pool: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to get HR info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showInsufficientPoolOptions(EmergencyRequest request, int position, int available, int requiredDays) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Insufficient Common Pool");

        String message = String.format(
                "Common Pool has only %d days available.\n" +
                        "Required: %d days\n\n" +
                        "What would you like to do?",
                available, requiredDays
        );

        builder.setMessage(message)
                .setPositiveButton("Approve as LOP", (dialog, which) -> {
                    approveAsLOP(request, position);
                })
                .setNegativeButton("Reject", (dialog, which) -> {
                    rejectRequest(request, position);
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void completeApproval(EmergencyRequest request, int position, String hrId, String hrName, String sourceType, int allocatedDays) {
        // Update emergency request
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "approved");
        updates.put("hrId", hrId);
        updates.put("hrName", hrName);
        updates.put("actionDate", new Date());
        updates.put("approvalSource", sourceType);
        updates.put("allocatedDays", allocatedDays);
        updates.put("allocatedFromCommonPool", sourceType.equals("common_pool"));

        db.collection("emergencyRequests").document(request.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Create regular leave record
                    createLeaveRecord(request, sourceType, hrId, hrName);

                    // Remove from list and notify
                    requestList.remove(position);
                    notifyItemRemoved(position);

                    // Notify employee (Firestore notification)
                    notifyEmployee(request, "approved", hrName, sourceType);

                    // ========== PUSH NOTIFICATION TO EMPLOYEE ==========
                    // SHOW SYSTEM NOTIFICATION TO EMPLOYEE
                    try {
                        NotificationHelper notificationHelper = new NotificationHelper(context);
                        notificationHelper.showNotification(
                                "Emergency Leave Approved",
                                "Your emergency leave for " + request.getNumberOfDays() + " days " + request.getLeaveType() +
                                        " has been approved by HR. Approved from " + sourceType + ".",
                                NotificationHelper.TYPE_LEAVE_APPROVED
                        );
                        Log.d("NOTIFICATION", "Push notification sent to employee for approval");
                    } catch (Exception e) {
                        Log.e("NOTIFICATION", "Failed to send push notification: " + e.getMessage());
                    }
                    // ==================================================

                    Toast.makeText(context,
                            "Emergency leave approved! Allocated " + allocatedDays + " days from " + sourceType,
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to approve: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createLeaveRecord(EmergencyRequest request, String sourceType, String hrId, String hrName) {
        Map<String, Object> leaveRecord = new HashMap<>();
        leaveRecord.put("userId", request.getEmployeeId());
        leaveRecord.put("employeeName", request.getEmployeeName());
        leaveRecord.put("fromDate", request.getFromDate());
        leaveRecord.put("toDate", request.getToDate());
        leaveRecord.put("reason", request.getReason() + " [Emergency - Source: " + sourceType + "]");
        leaveRecord.put("leaveType", request.getLeaveType());
        leaveRecord.put("numberOfDays", request.getNumberOfDays());
        leaveRecord.put("status", "approved");
        leaveRecord.put("hrStatus", "approved");
        leaveRecord.put("managerStatus", "approved");
        leaveRecord.put("hrId", hrId);
        leaveRecord.put("hrName", hrName);
        leaveRecord.put("isLOP", sourceType.equals("lop"));
        leaveRecord.put("isEmergency", true);
        leaveRecord.put("emergencyDetails", request.getEmergencyDetails());
        leaveRecord.put("appliedDate", new Date());
        leaveRecord.put("approvedDate", new Date());

        db.collection("leaveRequests").add(leaveRecord)
                .addOnSuccessListener(documentReference -> {
                    // Successfully created leave record
                    Log.d("LEAVE_RECORD", "Leave record created successfully");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to create leave record: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void approveAsLOP(EmergencyRequest request, int position) {
        String hrId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(hrId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String hrName = documentSnapshot.getString("name");
                    if (hrName == null) hrName = "HR";

                    // Update emergency request as LOP
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "approved");
                    updates.put("hrId", hrId);
                    updates.put("hrName", hrName);
                    updates.put("actionDate", new Date());
                    updates.put("approvalSource", "lop");
                    updates.put("allocatedDays", request.getNumberOfDays());
                    updates.put("allocatedFromCommonPool", false);

                    String finalHrName = hrName;
                    db.collection("emergencyRequests").document(request.getId())
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                // Create LOP leave record
                                createLOPLeaveRecord(request, hrId, finalHrName);

                                // Remove from list
                                requestList.remove(position);
                                notifyItemRemoved(position);

                                // Notify employee (Firestore notification)
                                notifyEmployee(request, "approved_as_lop", finalHrName, "lop");

                                // ========== PUSH NOTIFICATION TO EMPLOYEE ==========
                                // SHOW SYSTEM NOTIFICATION TO EMPLOYEE
                                try {
                                    NotificationHelper notificationHelper = new NotificationHelper(context);
                                    notificationHelper.showNotification(
                                            "Emergency Leave Approved as LOP",
                                            "Your emergency leave for " + request.getNumberOfDays() + " days has been approved as Loss of Pay (LOP). This will be deducted from your salary.",
                                            NotificationHelper.TYPE_LEAVE_APPROVED
                                    );
                                    Log.d("NOTIFICATION", "Push notification sent to employee for LOP approval");
                                } catch (Exception e) {
                                    Log.e("NOTIFICATION", "Failed to send push notification: " + e.getMessage());
                                }
                                // ==================================================

                                Toast.makeText(context, "Emergency leave approved as LOP", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to approve as LOP: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to get HR info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createLOPLeaveRecord(EmergencyRequest request, String hrId, String hrName) {
        Map<String, Object> leaveRecord = new HashMap<>();
        leaveRecord.put("userId", request.getEmployeeId());
        leaveRecord.put("employeeName", request.getEmployeeName());
        leaveRecord.put("fromDate", request.getFromDate());
        leaveRecord.put("toDate", request.getToDate());
        leaveRecord.put("reason", request.getReason() + " [Emergency - LOP]");
        leaveRecord.put("leaveType", "Loss of Pay (LOP)");
        leaveRecord.put("numberOfDays", request.getNumberOfDays());
        leaveRecord.put("status", "approved");
        leaveRecord.put("hrStatus", "approved");
        leaveRecord.put("managerStatus", "approved");
        leaveRecord.put("hrId", hrId);
        leaveRecord.put("hrName", hrName);
        leaveRecord.put("isLOP", true);
        leaveRecord.put("isEmergency", true);
        leaveRecord.put("emergencyDetails", request.getEmergencyDetails());
        leaveRecord.put("appliedDate", new Date());
        leaveRecord.put("approvedDate", new Date());

        db.collection("leaveRequests").add(leaveRecord)
                .addOnSuccessListener(documentReference -> {
                    // Update employee's LOP balance
                    db.collection("users").document(request.getEmployeeId())
                            .update("leaveBalance.lop", FieldValue.increment(request.getNumberOfDays()),
                                    "lopDaysThisMonth", FieldValue.increment(request.getNumberOfDays()))
                            .addOnSuccessListener(aVoid -> {
                                // Successfully updated LOP balance
                                Log.d("LOP_BALANCE", "LOP balance updated successfully");
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to update LOP balance: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to create LOP record: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void rejectRequest(EmergencyRequest request, int position) {
        String hrId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(hrId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String hrName = documentSnapshot.getString("name");
                    if (hrName == null) hrName = "HR";

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "rejected");
                    updates.put("hrId", hrId);
                    updates.put("hrName", hrName);
                    updates.put("actionDate", new Date());

                    String finalHrName = hrName;
                    db.collection("emergencyRequests").document(request.getId())
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                // Remove from list
                                requestList.remove(position);
                                notifyItemRemoved(position);

                                // Notify employee (Firestore notification)
                                notifyEmployee(request, "rejected", finalHrName, null);

                                // ========== PUSH NOTIFICATION TO EMPLOYEE ==========
                                // SHOW SYSTEM NOTIFICATION TO EMPLOYEE
                                try {
                                    NotificationHelper notificationHelper = new NotificationHelper(context);
                                    notificationHelper.showNotification(
                                            "Emergency Leave Rejected",
                                            "Your emergency leave for " + request.getNumberOfDays() + " days has been rejected by HR.",
                                            NotificationHelper.TYPE_LEAVE_REJECTED
                                    );
                                    Log.d("NOTIFICATION", "Push notification sent to employee for rejection");
                                } catch (Exception e) {
                                    Log.e("NOTIFICATION", "Failed to send push notification: " + e.getMessage());
                                }
                                // ==================================================

                                Toast.makeText(context, "Emergency leave rejected", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to reject: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to get HR info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void notifyEmployee(EmergencyRequest request, String action, String approverName, String sourceType) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", request.getEmployeeId());

        String title = "Emergency Leave " + action.toUpperCase();
        String message;

        if (action.equals("approved")) {
            message = "Your emergency leave request for " + request.getNumberOfDays() +
                    " days " + request.getLeaveType() + " has been approved by " + approverName +
                    ". Approved from " + sourceType + ".";
        } else if (action.equals("approved_as_lop")) {
            message = "Your emergency leave request has been approved as LOP (Loss of Pay) for " +
                    request.getNumberOfDays() + " days by " + approverName +
                    ". This will be deducted from your salary.";
        } else {
            message = "Your emergency leave request has been rejected by " + approverName;
        }

        notification.put("title", title);
        notification.put("message", message);
        notification.put("type", "emergency_" + action);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);

        db.collection("notifications").add(notification)
                .addOnSuccessListener(documentReference -> {
                    // Notification sent successfully to Firestore
                    Log.d("NOTIFICATION", "Firestore notification created for employee: " + request.getEmployeeName());
                })
                .addOnFailureListener(e -> {
                    // Failed to send notification
                    Log.e("NOTIFICATION", "Failed to create Firestore notification: " + e.getMessage());
                });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmployeeName, tvDepartment, tvDates, tvDays, tvLeaveType,
                tvReason, tvEmergencyDetails, tvRequestDate, tvDonationInfo, tvApprovalInfo;
        Button btnApprove, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmployeeName = itemView.findViewById(R.id.tvEmployeeName);
            tvDepartment = itemView.findViewById(R.id.tvDepartment);
            tvDates = itemView.findViewById(R.id.tvDates);
            tvDays = itemView.findViewById(R.id.tvDays);
            tvLeaveType = itemView.findViewById(R.id.tvLeaveType);
            tvReason = itemView.findViewById(R.id.tvReason);
            tvEmergencyDetails = itemView.findViewById(R.id.tvEmergencyDetails);
            tvRequestDate = itemView.findViewById(R.id.tvRequestDate);
            tvDonationInfo = itemView.findViewById(R.id.tvDonationInfo);
            tvApprovalInfo = itemView.findViewById(R.id.tvApprovalInfo);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}