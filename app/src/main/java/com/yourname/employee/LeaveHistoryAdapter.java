package com.yourname.employee;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import models.LeaveRequest;  // ✅ CORRECT IMPORT
import java.text.SimpleDateFormat;
import java.util.List;
import androidx.core.content.ContextCompat;
import java.util.Locale;

public class LeaveHistoryAdapter extends RecyclerView.Adapter<LeaveHistoryAdapter.LeaveViewHolder> {

    private List<LeaveRequest> leaveList;
    private Context context;

    public LeaveHistoryAdapter(List<LeaveRequest> leaveList, Context context) {
        this.leaveList = leaveList;
        this.context = context;
    }

    @NonNull
    @Override
    public LeaveViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leave_history, parent, false);
        return new LeaveViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaveViewHolder holder, int position) {
        LeaveRequest leave = leaveList.get(position);

        // Set leave type
        if (leave.getLeaveType() != null) {
            holder.tvLeaveType.setText(leave.getLeaveType().toUpperCase() + " LEAVE");
        } else {
            holder.tvLeaveType.setText("LEAVE");
        }

        // Set dates
        if (leave.getFromDate() != null && leave.getToDate() != null) {
            holder.tvDates.setText(leave.getFromDate() + " to " + leave.getToDate());
        }

        // Set reason
        if (leave.getReason() != null) {
            holder.tvReason.setText(leave.getReason());
        }

        // Set status with color
        if (leave.getStatus() != null) {
            holder.tvStatus.setText(leave.getStatus().toUpperCase());

            // Set status color and background
            if (leave.getStatus().equalsIgnoreCase("approved")) {
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
                holder.tvStatus.setBackgroundResource(R.drawable.status_background_approved);
            } else if (leave.getStatus().equalsIgnoreCase("rejected")) {
                holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
                holder.tvStatus.setBackgroundResource(R.drawable.status_background_rejected);
            } else {
                holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_orange_dark));
                holder.tvStatus.setBackgroundResource(R.drawable.status_background_pending);
            }
        }

        // Format and set applied date
        if (leave.getAppliedDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.tvAppliedDate.setText("Applied: " + sdf.format(leave.getAppliedDate()));
        } else {
            holder.tvAppliedDate.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return leaveList.size();
    }

    static class LeaveViewHolder extends RecyclerView.ViewHolder {
        TextView tvLeaveType, tvDates, tvReason, tvStatus, tvAppliedDate;

        public LeaveViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLeaveType = itemView.findViewById(R.id.tvLeaveType);
            tvDates = itemView.findViewById(R.id.tvDates);
            tvReason = itemView.findViewById(R.id.tvReason);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvAppliedDate = itemView.findViewById(R.id.tvAppliedDate);
        }
    }
}