package com.yourname.employee;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.LeaveRequest;

public class HrDashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageView ivMenu;
    private TextView tvWelcome, tvPendingCount, tvTotalEmployees, tvEmpty, tvNotificationCount;
    private NavigationView navigationView;
    private RecyclerView recyclerView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private List<LeaveRequest> pendingLeavesList;
    private LeaveAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hr_dashboard);

        Log.d("HR_DASHBOARD", "onCreate: Starting HR Dashboard");

        try {
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();

            pendingLeavesList = new ArrayList<>();

            // Initialize views
            initializeViews();

            // Setup RecyclerView
            setupRecyclerView();

            // Check if user is logged in and get their role
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                // User not logged in, redirect to login
                Log.d("HR_DASHBOARD", "User not logged in, redirecting to login");
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }

            Log.d("HR_DASHBOARD", "User logged in: " + currentUser.getEmail());
            // User is logged in, check role
            checkUserRole();

        } catch (Exception e) {
            Log.e("HR_DASHBOARD", "Error in onCreate: ", e);
            Toast.makeText(this, "Error initializing dashboard: " + e.getMessage(), Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void initializeViews() {
        try {
            Log.d("HR_DASHBOARD", "Initializing views...");

            drawerLayout = findViewById(R.id.drawerLayout);
            ivMenu = findViewById(R.id.ivMenu);
            tvWelcome = findViewById(R.id.tvWelcome);
            tvPendingCount = findViewById(R.id.tvPendingCount);
            tvTotalEmployees = findViewById(R.id.tvTotalEmployees);
            tvNotificationCount = findViewById(R.id.tvNotificationCount);
            navigationView = findViewById(R.id.navigationView);
            recyclerView = findViewById(R.id.recyclerView);
            tvEmpty = findViewById(R.id.tvEmpty);

            // Debug check for notification count view
            if (tvNotificationCount == null) {
                Log.e("HR_DASHBOARD", "ERROR: tvNotificationCount is NULL!");
                Toast.makeText(this, "Notification count view not found in layout!", Toast.LENGTH_SHORT).show();
            } else {
                Log.d("HR_DASHBOARD", "tvNotificationCount initialized successfully");
                // Set initial state
                tvNotificationCount.setVisibility(View.GONE);
            }

            if (navigationView == null) {
                Log.e("HR_DASHBOARD", "navigationView is NULL!");
            } else {
                Log.d("HR_DASHBOARD", "navigationView found");
            }

            if (recyclerView == null) {
                Log.e("HR_DASHBOARD", "recyclerView is NULL!");
            } else {
                Log.d("HR_DASHBOARD", "recyclerView found");
            }

            if (drawerLayout == null) {
                Log.e("HR_DASHBOARD", "drawerLayout is NULL!");
            } else {
                Log.d("HR_DASHBOARD", "drawerLayout found");
            }

            Log.d("HR_DASHBOARD", "All views initialized successfully");

        } catch (Exception e) {
            Log.e("HR_DASHBOARD", "Error in initializeViews: ", e);
            Toast.makeText(this, "Error loading UI components: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupRecyclerView() {
        try {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new LeaveAdapter(pendingLeavesList, this);
            recyclerView.setAdapter(adapter);
            Log.d("HR_DASHBOARD", "RecyclerView setup complete");
        } catch (Exception e) {
            Log.e("HR_DASHBOARD", "Error setting up RecyclerView: ", e);
        }
    }

    private void checkUserRole() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String userId = currentUser.getUid();
        Log.d("HR_DASHBOARD", "Checking role for user: " + userId);

        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot document = task.getResult();
                        String role = document.getString("role");
                        String name = document.getString("name");

                        Log.d("HR_DASHBOARD", "Role found: " + role + ", Name: " + name);

                        // More comprehensive HR role check
                        boolean isHR = false;
                        if (role != null) {
                            String lowerRole = role.toLowerCase().trim();
                            isHR = lowerRole.contains("hr") ||
                                    lowerRole.contains("admin") ||
                                    lowerRole.equals("human resources") ||
                                    lowerRole.equals("hr admin");
                            Log.d("HR_DASHBOARD", "Lowercase role: " + lowerRole + ", isHR: " + isHR);
                        } else {
                            Log.d("HR_DASHBOARD", "Role is null!");
                        }

                        if (isHR) {
                            // User is HR, setup dashboard
                            Log.d("HR_DASHBOARD", "User is HR, setting up dashboard");
                            setupMenuClick();
                            setupNavigationMenu();
                            loadUserData();
                            loadPendingLeaves();
                            loadTotalEmployees();
                            loadNotificationCount();

                            if (name != null && tvWelcome != null) {
                                tvWelcome.setText("Welcome HR, " + name);
                            }
                        } else {
                            // User is not HR, redirect with clear message
                            Toast.makeText(HrDashboardActivity.this,
                                    "Access Denied: HR Admin Access Only. Your role: " + role,
                                    Toast.LENGTH_LONG).show();
                            Log.d("HR_DASHBOARD", "Redirecting to Employee Dashboard. Role: " + role);
                            startActivity(new Intent(HrDashboardActivity.this, EmployeeDashboardActivity.class));
                            finish();
                        }
                    } else {
                        // Document doesn't exist or error
                        Toast.makeText(HrDashboardActivity.this,
                                "Error: User data not found. Please contact admin.",
                                Toast.LENGTH_SHORT).show();
                        if (task.getException() != null) {
                            Log.e("HR_DASHBOARD", "Error getting user document: " + task.getException().getMessage());
                        }
                        startActivity(new Intent(HrDashboardActivity.this, LoginActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("HR_DASHBOARD", "Firestore error: " + e.getMessage());
                });
    }

    private void setupMenuClick() {
        if (ivMenu != null) {
            ivMenu.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }
    }

    private void setupNavigationMenu() {
        if (navigationView == null) {
            Log.e("HR_DASHBOARD", "NavigationView is null in setupNavigationMenu!");
            return;
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_pending_leaves) {
                // Refresh leaves when menu item clicked
                loadPendingLeaves();
                Toast.makeText(this, "Refreshing leaves...", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_donation_requests) {
                startActivity(new Intent(this, DonationHistoryActivity.class));
                finish();
            } else if (id == R.id.nav_common_pool) {
                startActivity(new Intent(this, CommonPoolActivity.class));
                finish();
            } else if (id == R.id.nav_emergency_requests) {
                startActivity(new Intent(this, EmergencyRequestsActivity.class));
                finish();
            } else if (id == R.id.nav_all_employees) {
                startActivity(new Intent(this, AllEmployeesActivity.class));
            } else if (id == R.id.nav_add_employee) {
                startActivity(new Intent(this, AddEmployeeActivity.class));
            } else if (id == R.id.nav_reports) {
                startActivity(new Intent(this, ReportsActivity.class));
            } else if (id == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationsActivity.class));
            } else if (id == R.id.nav_logout) {
                logoutUser();
            }

            if (drawerLayout != null) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            return true;
        });
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        String name = task.getResult().getString("name");
                        if (name != null && tvWelcome != null) {
                            tvWelcome.setText("Welcome HR, " + name);
                        }
                    }
                });
    }

    @SuppressLint("NotifyDataSetChanged")
    void loadPendingLeaves() {
        Log.d("HR_DASHBOARD", "Loading leaves awaiting HR approval...");

        if (tvEmpty == null || recyclerView == null) {
            Log.e("HR_DASHBOARD", "UI components not initialized");
            return;
        }

        // Show loading
        tvEmpty.setText("Loading leaves awaiting HR approval...");
        tvEmpty.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        // Load leaves that have been approved by manager but pending HR approval
        db.collection("leaveRequests")
                .whereEqualTo("managerStatus", "approved") // Manager has approved
                .whereEqualTo("hrStatus", "pending") // Waiting for HR approval
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        pendingLeavesList.clear();
                        int count = 0;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                // Get data from document
                                String employeeName = document.getString("employeeName");
                                String leaveType = document.getString("leaveType");
                                String fromDate = document.getString("fromDate");
                                String toDate = document.getString("toDate");
                                String reason = document.getString("reason");
                                String status = document.getString("status");
                                String userId = document.getString("userId");
                                Date appliedDate = document.getDate("appliedDate");
                                Long days = document.getLong("days");
                                Boolean isLOP = document.getBoolean("isLOP");
                                String department = document.getString("department");
                                String designation = document.getString("designation");

                                // Add these lines to get manager info
                                String managerStatus = document.getString("managerStatus");
                                String hrStatus = document.getString("hrStatus");
                                String managerName = document.getString("managerName");

                                // Create LeaveRequest object
                                LeaveRequest leave = new LeaveRequest();
                                leave.setId(document.getId());
                                leave.setUserId(userId);
                                leave.setEmployeeName(employeeName != null ? employeeName : "Unknown");
                                leave.setLeaveType(leaveType != null ? leaveType : "Unknown");
                                leave.setFromDate(fromDate != null ? fromDate : "");
                                leave.setToDate(toDate != null ? toDate : "");
                                leave.setReason(reason != null ? reason : "");
                                leave.setStatus(status != null ? status : "pending");
                                leave.setAppliedDate(appliedDate);
                                leave.setLOP(isLOP != null ? isLOP : leaveType != null && leaveType.equals("Loss of Pay (LOP)"));
                                leave.setDepartment(department != null ? department : "");
                                leave.setDesignation(designation != null ? designation : "");

                                // Set manager and HR status
                                leave.setManagerStatus(managerStatus != null ? managerStatus : "pending");
                                leave.setHrStatus(hrStatus != null ? hrStatus : "pending");
                                leave.setManagerName(managerName);

                                // Set days
                                if (days != null) {
                                    leave.setDays(days);
                                } else {
                                    // Calculate from dates
                                    leave.setDays(leave.calculateDaysFromDates());
                                }

                                pendingLeavesList.add(leave);
                                count++;

                            } catch (Exception e) {
                                Log.e("HR_DASHBOARD", "Error parsing document: " + e.getMessage());
                            }
                        }

                        if (tvPendingCount != null) {
                            tvPendingCount.setText("Pending Leaves: " + count);
                        }

                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }

                        if (count == 0) {
                            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
                            if (tvEmpty != null) {
                                tvEmpty.setVisibility(View.VISIBLE);
                                tvEmpty.setText("No pending leaves");
                            }
                        } else {
                            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
                            if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
                        }

                        Log.d("HR_DASHBOARD", "Loaded " + count + " pending leaves");

                    } else {
                        Log.e("HR_DASHBOARD", "Failed to load leaves: " + task.getException());
                        if (tvEmpty != null) {
                            tvEmpty.setText("Error loading leaves: " + task.getException().getMessage());
                        }
                        Toast.makeText(this, "Failed to load leaves", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadTotalEmployees() {
        db.collection("users")
                .whereIn("role", java.util.Arrays.asList("employee", "team_leader", "manager"))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && tvTotalEmployees != null) {
                        int count = task.getResult().size();
                        tvTotalEmployees.setText("Total Employees: " + count);
                    } else if (tvTotalEmployees != null) {
                        tvTotalEmployees.setText("Total Employees: 0");
                    }
                });
    }

    private void loadNotificationCount() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.d("HR_DASHBOARD", "Current user is null, cannot load notifications");
            return;
        }

        String userId = currentUser.getUid();
        Log.d("HR_DASHBOARD", "Loading notification count for user: " + userId);

        // Make sure tvNotificationCount is initialized
        if (tvNotificationCount == null) {
            Log.e("HR_DASHBOARD", "tvNotificationCount is null in loadNotificationCount()!");
            // Try to re-initialize
            tvNotificationCount = findViewById(R.id.tvNotificationCount);
            if (tvNotificationCount == null) {
                Log.e("HR_DASHBOARD", "Still null after re-finding!");
                return;
            }
        }

        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int count = task.getResult().size();
                        Log.d("HR_DASHBOARD", "Found " + count + " unread notifications");

                        runOnUiThread(() -> {
                            if (tvNotificationCount != null) {
                                if (count > 0) {
                                    tvNotificationCount.setText(String.valueOf(count));
                                    tvNotificationCount.setVisibility(View.VISIBLE);
                                    Log.d("HR_DASHBOARD", "Notification badge set to: " + count);
                                } else {
                                    tvNotificationCount.setVisibility(View.GONE);
                                    Log.d("HR_DASHBOARD", "No unread notifications, hiding badge");
                                }
                            } else {
                                Log.e("HR_DASHBOARD", "tvNotificationCount is null in UI thread!");
                            }
                        });
                    } else {
                        Log.e("HR_DASHBOARD", "Failed to load notifications: " + task.getException());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("HR_DASHBOARD", "Firestore error loading notifications: " + e.getMessage());
                });
    }

    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    // Quick Actions Click Methods
    public void onViewEmployeesClick(View view) {
        startActivity(new Intent(this, AllEmployeesActivity.class));
    }

    public void onAddEmployeeClick(View view) {
        startActivity(new Intent(this, AddEmployeeActivity.class));
    }

    public void onViewReportsClick(View view) {
        startActivity(new Intent(this, ReportsActivity.class));
    }

    public void onViewLeavesClick(View view) {
        loadPendingLeaves();
        Toast.makeText(this, "Refreshing leaves...", Toast.LENGTH_SHORT).show();
    }

    // NEW: Emergency Requests Quick Action
    public void onViewEmergencyRequestsClick(View view) {
        startActivity(new Intent(this, EmergencyRequestsActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("HR_DASHBOARD", "onResume called");
        if (mAuth.getCurrentUser() != null) {
            loadPendingLeaves();
            loadTotalEmployees();
            loadNotificationCount(); // This will refresh the notification count
        }
    }
}