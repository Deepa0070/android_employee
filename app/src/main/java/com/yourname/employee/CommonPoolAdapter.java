package com.yourname.employee;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import models.DonationRequest;

public class CommonPoolAdapter extends RecyclerView.Adapter<CommonPoolAdapter.CommonPoolViewHolder> {

    private List<DonationRequest> donationList;
    private Context context;

    public CommonPoolAdapter(List<DonationRequest> donationList, Context context) {
        this.donationList = donationList;
        this.context = context;
    }

    @NonNull
    @Override
    public CommonPoolViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_common_pool_donation, parent, false);
        return new CommonPoolViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommonPoolViewHolder holder, int position) {
        DonationRequest donation = donationList.get(position);

        // Set donor info
        holder.tvDonorName.setText(donation.getDonorName());

        // Set leave type and days
        holder.tvLeaveDetails.setText(donation.getNumberOfDays() + " days " + donation.getLeaveType());

        // Format and set date
        if (donation.getAppliedDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            holder.tvDate.setText(sdf.format(donation.getAppliedDate()));
        }

        // Set reason
        if (donation.getReason() != null && !donation.getReason().isEmpty()) {
            holder.tvReason.setText("Reason: " + donation.getReason());
        } else {
            holder.tvReason.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return donationList.size();
    }

    static class CommonPoolViewHolder extends RecyclerView.ViewHolder {
        TextView tvDonorName, tvLeaveDetails, tvDate, tvReason;

        public CommonPoolViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDonorName = itemView.findViewById(R.id.tvDonorName);
            tvLeaveDetails = itemView.findViewById(R.id.tvLeaveDetails);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvReason = itemView.findViewById(R.id.tvReason);
        }
    }
}