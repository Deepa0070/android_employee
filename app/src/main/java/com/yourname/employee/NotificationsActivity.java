package com.yourname.employee;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import models.Notification;

public class NotificationsActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageView ivMenu;
    private NavigationView navigationView;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private List<Notification> notificationsList;
    private NotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        notificationsList = new ArrayList<>();

        initializeViews();
        setupRecyclerView();
        setupNavigationMenu();
        loadNotifications();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        ivMenu = findViewById(R.id.ivMenu);
        navigationView = findViewById(R.id.navigationView);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        ivMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notificationsList, this);
        recyclerView.setAdapter(adapter);
    }

    private void setupNavigationMenu() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, EmployeeDashboardActivity.class));
                finish();
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (id == R.id.nav_apply_leave) {
                startActivity(new Intent(this, ApplyLeaveActivity.class));
            } else if (id == R.id.nav_leave_history) {
                startActivity(new Intent(this, LeaveHistoryActivity.class));
            } else if (id == R.id.nav_salary) {
                startActivity(new Intent(this, SalaryActivity.class));
            } else if (id == R.id.nav_holidays) {
                startActivity(new Intent(this, HolidayCalendarActivity.class));
            } else if (id == R.id.nav_logout) {
                logoutUser();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void loadNotifications() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        Log.d("NOTIFICATIONS", "Loading notifications for user: " + userId);

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        notificationsList.clear();
                        Log.d("NOTIFICATIONS", "Found " + task.getResult().size() + " notifications");

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Log.d("NOTIFICATIONS", "Document data: " + document.getData());

                                // Create Notification object
                                Notification notification = new Notification();
                                notification.setId(document.getId());
                                notification.setUserId(document.getString("userId"));
                                notification.setTitle(document.getString("title"));
                                notification.setMessage(document.getString("message"));
                                notification.setType(document.getString("type"));
                                notification.setFromUserId(document.getString("fromUserId"));
                                notification.setFromUserName(document.getString("fromUserName"));
                                notification.setLeaveId(document.getString("leaveId"));

                                // Handle timestamp
                                Object timestampObj = document.get("timestamp");
                                if (timestampObj instanceof Long) {
                                    notification.setTimestamp((Long) timestampObj);
                                } else if (timestampObj instanceof Double) {
                                    notification.setTimestamp(((Double) timestampObj).longValue());
                                } else if (timestampObj instanceof Integer) {
                                    notification.setTimestamp(((Integer) timestampObj).longValue());
                                } else {
                                    Log.e("NOTIFICATIONS", "Timestamp is of unknown type: " + (timestampObj != null ? timestampObj.getClass().getName() : "null"));
                                    // Use current time as fallback
                                    notification.setTimestamp(System.currentTimeMillis());
                                }

                                // Handle read status
                                Object readObj = document.get("read");
                                if (readObj instanceof Boolean) {
                                    notification.setRead((Boolean) readObj);
                                } else {
                                    notification.setRead(false);
                                }

                                notificationsList.add(notification);
                                Log.d("NOTIFICATIONS", "Added notification: " + notification.getTitle());

                            } catch (Exception e) {
                                Log.e("NOTIFICATIONS", "Error parsing notification: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }

                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }

                        if (notificationsList.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            tvEmpty.setText("No notifications");
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                        }

                        // Mark all as read
                        markAllAsRead(userId);

                    } else {
                        Log.e("NOTIFICATIONS", "Failed to load notifications: " + task.getException());
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("Failed to load notifications");
                        Toast.makeText(this, "Failed to load notifications: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void markAllAsRead(String userId) {
        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        db.collection("notifications").document(document.getId())
                                .update("read", true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("NOTIFICATIONS", "Failed to mark notifications as read: " + e.getMessage());
                });
    }

    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
    }
}