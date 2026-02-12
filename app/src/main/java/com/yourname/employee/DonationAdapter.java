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
import models.DonationRequest;

public class DonationAdapter extends RecyclerView.Adapter<DonationAdapter.DonationViewHolder> {

    private List<DonationRequest> donationList;
    private Context context;
    private String userRole;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public DonationAdapter(List<DonationRequest> donationList, Context context, String userRole) {
        this.donationList = donationList;
        this.context = context;
        this.userRole = userRole;
        this.mAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public DonationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_donation_request, parent, false);
        return new DonationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DonationViewHolder holder, int position) {
        DonationRequest donation = donationList.get(position);

        // Set donation info
        String donationInfo;
        if (donation.getRecipientId() == null || donation.getRecipientId().isEmpty()) {
            donationInfo = donation.getDonorName() + " → Common Pool";
        } else {
            donationInfo = donation.getDonorName() + " → " + donation.getRecipientName();
        }
        holder.tvDonationInfo.setText(donationInfo);

        // Set leave type and days
        holder.tvLeaveType.setText("Type: " + donation.getLeaveType());
        holder.tvDays.setText(donation.getNumberOfDays() + " days");

        // Set reason
        holder.tvReason.setText("Reason: " + donation.getReason());

        // Format and set date
        if (donation.getAppliedDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            holder.tvDates.setText("Applied: " + sdf.format(donation.getAppliedDate()));
        }

        // Set status with color
        String status = donation.getStatus();
        holder.tvStatus.setText(status.toUpperCase());

        // Set status background
        if (status.equalsIgnoreCase("approved")) {
            holder.tvStatus.setBackgroundResource(R.drawable.status_background_approved);
        } else if (status.equalsIgnoreCase("rejected")) {
            holder.tvStatus.setBackgroundResource(R.drawable.status_background_rejected);
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.status_background_pending);
        }

        // Show action buttons for HR on pending requests
        boolean isHR = isUserHR();
        if (isHR && status.equals("pending")) {
            holder.layoutActions.setVisibility(View.VISIBLE);

            holder.btnApprove.setOnClickListener(v -> {
                approveDonation(donation, position);
            });

            holder.btnReject.setOnClickListener(v -> {
                rejectDonation(donation, position);
            });
        } else {
            holder.layoutActions.setVisibility(View.GONE);
        }
    }

    private boolean isUserHR() {
        return userRole != null &&
                (userRole.toLowerCase().contains("hr") ||
                        userRole.toLowerCase().contains("admin"));
    }

    private void approveDonation(DonationRequest donation, int position) {
        String hrId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(hrId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String hrName = documentSnapshot.getString("name");

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "approved");
                    updates.put("approvedDate", new Date());
                    updates.put("hrId", hrId);
                    updates.put("hrName", hrName);

                    db.collection("donationRequests").document(donation.getId())
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                // Update donor's leave balance
                                updateDonorBalance(donation.getDonorId(),
                                        donation.getLeaveType(), donation.getNumberOfDays());

                                // If donated to common pool, update common pool
                                if (donation.getRecipientId() == null || donation.getRecipientId().isEmpty()) {
                                    updateCommonPool(donation.getLeaveType(), donation.getNumberOfDays());
                                } else {
                                    // If donated to specific employee, update recipient's balance
                                    updateRecipientBalance(donation.getRecipientId(),
                                            donation.getLeaveType(), donation.getNumberOfDays());
                                }

                                // Update local list
                                donationList.get(position).setStatus("approved");
                                notifyItemChanged(position);

                                // Send notifications
                                sendDonationNotification(donation, "approved");

                                Toast.makeText(context, "Donation approved", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to approve: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void rejectDonation(DonationRequest donation, int position) {
        String hrId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(hrId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String hrName = documentSnapshot.getString("name");

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "rejected");
                    updates.put("approvedDate", new Date());
                    updates.put("hrId", hrId);
                    updates.put("hrName", hrName);

                    db.collection("donationRequests").document(donation.getId())
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                // Update local list
                                donationList.get(position).setStatus("rejected");
                                notifyItemChanged(position);

                                // Send notification
                                sendDonationNotification(donation, "rejected");

                                Toast.makeText(context, "Donation rejected", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to reject: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void updateDonorBalance(String donorId, String leaveType, int numberOfDays) {
        String field = leaveType.equals("Casual Leave") ? "casual" : "sick";
        long deduction = -numberOfDays;

        db.collection("users").document(donorId)
                .update("leaveBalance." + field, FieldValue.increment(deduction));
    }

    private void updateRecipientBalance(String recipientId, String leaveType, int numberOfDays) {
        String field = leaveType.equals("Casual Leave") ? "casual" : "sick";

        db.collection("users").document(recipientId)
                .update("leaveBalance." + field, FieldValue.increment(numberOfDays));
    }

    private void updateCommonPool(String leaveType, int numberOfDays) {
        String poolType = leaveType.equals("Casual Leave") ? "casual" : "sick";

        db.collection("commonLeavePool")
                .whereEqualTo("leaveType", poolType)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Update existing pool
                        String poolId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        db.collection("commonLeavePool").document(poolId)
                                .update("totalDays", FieldValue.increment(numberOfDays),
                                        "donatedDays", FieldValue.increment(numberOfDays));
                    } else {
                        // Create new pool
                        Map<String, Object> pool = new HashMap<>();
                        pool.put("leaveType", poolType);
                        pool.put("totalDays", numberOfDays);
                        pool.put("donatedDays", numberOfDays);
                        pool.put("usedDays", 0);

                        db.collection("commonLeavePool").add(pool);
                    }
                });
    }

    private void sendDonationNotification(DonationRequest donation, String action) {
        // Notify donor
        Map<String, Object> donorNotification = new HashMap<>();
        donorNotification.put("userId", donation.getDonorId());
        donorNotification.put("title", "Donation " + action.toUpperCase());
        donorNotification.put("message", "Your donation of " + donation.getNumberOfDays() +
                " days " + donation.getLeaveType() + " has been " + action + " by HR");
        donorNotification.put("type", "donation_" + action);
        donorNotification.put("timestamp", System.currentTimeMillis());
        donorNotification.put("read", false);

        db.collection("notifications").add(donorNotification);

        // Notify recipient if not common pool
        if (donation.getRecipientId() != null && !donation.getRecipientId().isEmpty() && action.equals("approved")) {
            Map<String, Object> recipientNotification = new HashMap<>();
            recipientNotification.put("userId", donation.getRecipientId());
            recipientNotification.put("title", "Received Leave Donation");
            recipientNotification.put("message", "You have received " + donation.getNumberOfDays() +
                    " days " + donation.getLeaveType() + " from " + donation.getDonorName());
            recipientNotification.put("type", "received_donation");
            recipientNotification.put("timestamp", System.currentTimeMillis());
            recipientNotification.put("read", false);

            db.collection("notifications").add(recipientNotification);
        }
    }

    @Override
    public int getItemCount() {
        return donationList.size();
    }

    static class DonationViewHolder extends RecyclerView.ViewHolder {
        TextView tvDonationInfo, tvLeaveType, tvDays, tvReason, tvDates, tvStatus;
        View layoutActions;
        Button btnApprove, btnReject;

        public DonationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDonationInfo = itemView.findViewById(R.id.tvDonationInfo);
            tvLeaveType = itemView.findViewById(R.id.tvLeaveType);
            tvDays = itemView.findViewById(R.id.tvDays);
            tvReason = itemView.findViewById(R.id.tvReason);
            tvDates = itemView.findViewById(R.id.tvDates);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}