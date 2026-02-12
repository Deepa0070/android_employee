package com.yourname.employee;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import models.Notification;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> notificationsList;
    private Context context;

    public NotificationAdapter(List<Notification> notificationsList, Context context) {
        this.notificationsList = notificationsList;
        this.context = context;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationsList.get(position);

        // Set notification data
        if (notification.getTitle() != null) {
            holder.tvTitle.setText(notification.getTitle());
        } else {
            holder.tvTitle.setText("Notification");
        }

        if (notification.getMessage() != null) {
            holder.tvMessage.setText(notification.getMessage());
        } else {
            holder.tvMessage.setText("No message");
        }

        // Show sender if available
        if (notification.getFromUserName() != null && !notification.getFromUserName().isEmpty()) {
            holder.tvFrom.setText("From: " + notification.getFromUserName());
            holder.tvFrom.setVisibility(View.VISIBLE);
        } else {
            holder.tvFrom.setVisibility(View.GONE);
        }

        // Format and display timestamp
        if (notification.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            String dateString = sdf.format(new Date(notification.getTimestamp()));
            holder.tvTime.setText(dateString);
        } else {
            holder.tvTime.setText("");
        }

        // Set background based on read status
        if (notification.isRead()) {
            holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.white));
        } else {
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.unread_notification));
        }
    }

    @Override
    public int getItemCount() {
        return notificationsList != null ? notificationsList.size() : 0;
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvFrom, tvTime;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvFrom = itemView.findViewById(R.id.tvFrom);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}