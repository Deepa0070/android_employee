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
import java.util.List;

import models.LeaveRequest;

public class ManagerDashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageView ivMenu;
    private TextView tvWelcome, tvPendingCount, tvTotalEmployees, tvEmpty, tvNotificationCount;
    private NavigationView navigationView;
    private RecyclerView recyclerView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private List<LeaveRequest> pendingLeavesList;
    private ManagerLeaveAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_dashboard);

        Log.d("MANAGER_DASHBOARD", "onCreate: Starting Manager Dashboard");

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
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }

            Log.d("MANAGER_DASHBOARD", "User logged in: " + currentUser.getEmail());
            // Check role and setup dashboard
            checkUserRole();

        } catch (Exception e) {
            Log.e("MANAGER_DASHBOARD", "Error in onCreate: ", e);
            Toast.makeText(this, "Error initializing dashboard: " + e.getMessage(), Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void initializeViews() {
        try {
            Log.d("MANAGER_DASHBOARD", "Initializing views...");

            drawerLayout = findViewById(R.id.drawerLayout);
            ivMenu = findViewById(R.id.ivMenu);
            tvWelcome = findViewById(R.id.tvWelcome);
            tvPendingCount = findViewById(R.id.tvPendingCount);
            tvTotalEmployees = findViewById(R.id.tvTotalEmployees);
            tvNotificationCount = findViewById(R.id.tvNotificationCount);
            navigationView = findViewById(R.id.navigationView);
            recyclerView = findViewById(R.id.recyclerView);
            tvEmpty = findViewById(R.id.tvEmpty);

            Log.d("MANAGER_DASHBOARD", "All views initialized successfully");

        } catch (Exception e) {
            Log.e("MANAGER_DASHBOARD", "Error in initializeViews: ", e);
            Toast.makeText(this, "Error loading UI components", Toast.LENGTH_LONG).show();
        }
    }

    private void setupRecyclerView() {
        try {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new ManagerLeaveAdapter(pendingLeavesList, this);
            recyclerView.setAdapter(adapter);
            Log.d("MANAGER_DASHBOARD", "RecyclerView setup complete");
        } catch (Exception e) {
            Log.e("MANAGER_DASHBOARD", "Error setting up RecyclerView: ", e);
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
        Log.d("MANAGER_DASHBOARD", "Checking role for user: " + userId);

        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot document = task.getResult();
                        String role = document.getString("role");
                        String name = document.getString("name");
                        String department = document.getString("department");

                        Log.d("MANAGER_DASHBOARD", "Role found: " + role + ", Name: " + name + ", Dept: " + department);

                        // Check if user is Manager
                        boolean isManager = false;
                        if (role != null) {
                            String lowerRole = role.toLowerCase().trim();
                            isManager = lowerRole.contains("manager") ||
                                    lowerRole.equals("manager");
                            Log.d("MANAGER_DASHBOARD", "Is Manager: " + isManager);
                        } else {
                            Log.d("MANAGER_DASHBOARD", "Role is null!");
                        }

                        if (isManager) {
                            // User is Manager, setup dashboard
                            Log.d("MANAGER_DASHBOARD", "User is Manager, setting up dashboard");
                            setupMenuClick();
                            setupNavigationMenu();
                            loadUserData();
                            loadPendingLeaves();
                            loadTotalEmployeesInDepartment(department);
                            loadNotificationCount();

                            if (name != null) {
                                tvWelcome.setText("Welcome Manager, " + name);
                            }
                        } else {
                            // User is not Manager, redirect to appropriate dashboard
                            Toast.makeText(ManagerDashboardActivity.this,
                                    "Access Denied: Manager Access Only. Your role: " + role,
                                    Toast.LENGTH_LONG).show();
                            Log.d("MANAGER_DASHBOARD", "Redirecting to appropriate dashboard. Role: " + role);

                            Intent intent;
                            if (role != null && role.toLowerCase().contains("hr")) {
                                intent = new Intent(ManagerDashboardActivity.this, HrDashboardActivity.class);
                            } else {
                                intent = new Intent(ManagerDashboardActivity.this, EmployeeDashboardActivity.class);
                            }
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        // Document doesn't exist or error
                        Toast.makeText(ManagerDashboardActivity.this,
                                "Error: User data not found. Please contact admin.",
                                Toast.LENGTH_SHORT).show();
                        if (task.getException() != null) {
                            Log.e("MANAGER_DASHBOARD", "Error getting user document: " + task.getException().getMessage());
                        }
                        startActivity(new Intent(ManagerDashboardActivity.this, LoginActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("MANAGER_DASHBOARD", "Firestore error: " + e.getMessage());
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
            Log.e("MANAGER_DASHBOARD", "NavigationView is null in setupNavigationMenu!");
            return;
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_pending_leaves) {
                // Refresh leaves when menu item clicked
                loadPendingLeaves();
                Toast.makeText(this, "Refreshing leaves...", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_emergency_requests) {
                // ADDED: Emergency requests option
                startActivity(new Intent(this, ManagerEmergencyActivity.class));
                finish();
            } else if (id == R.id.nav_department_employees) {
                startActivity(new Intent(this, AllEmployeesActivity.class));
            } else if (id == R.id.nav_reports) {
                startActivity(new Intent(this, ReportsActivity.class));
            } else if (id == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationsActivity.class));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (id == R.id.nav_logout) {
                logoutUser();
            }

            if (drawerLayout != null) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            return true;
        });
    }

    @SuppressLint("SetTextI18n")
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
                            tvWelcome.setText("Welcome Manager," + name);
                        }
                    }
                });
    }

    private void loadPendingLeaves() {
        Log.d("MANAGER_DASHBOARD", "Loading pending leaves for manager approval...");

        if (tvEmpty == null || recyclerView == null) {
            Log.e("MANAGER_DASHBOARD", "UI components not initialized");
            return;
        }

        // Show loading
        tvEmpty.setText("Loading pending leaves...");
        tvEmpty.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        // Get current manager's department
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String managerId = currentUser.getUid();

        db.collection("users").document(managerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String managerDepartment = documentSnapshot.getString("department");
                        String managerName = documentSnapshot.getString("name");

                        Log.d("MANAGER_DASHBOARD", "Manager's department: " + managerDepartment);

                        if (managerDepartment == null || managerDepartment.isEmpty()) {
                            tvEmpty.setText("No department assigned to manager");
                            return;
                        }

                        // Load leaves from manager's department with status "pending"
                        // (awaiting manager approval)
                        db.collection("leaveRequests")
                                .whereEqualTo("department", managerDepartment)
                                .whereEqualTo("managerStatus", "pending") // Waiting for manager approval
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

                                                // Create LeaveRequest object
                                                LeaveRequest leave = new LeaveRequest();
                                                leave.setId(document.getId());
                                                leave.setUserId(userId);
                                                leave.setEmployeeName(employeeName != null ? employeeName : "Unknown");
                                                leave.setLeaveType(leaveType != null ? leaveType : "Unknown");
                                                leave.setFromDate(fromDate != null ? fromDate : "");
                                                leave.setToDate(toDate != null ? toDate : "");
                                                leave.setReason(reason != null ? reason : "");
                                                leave.setStatus("pending"); // Overall status
                                                leave.setManagerStatus("pending"); // Waiting for manager
                                                leave.setHrStatus("pending"); // Waiting for HR after manager
                                                leave.setAppliedDate(appliedDate);
                                                leave.setLOP(isLOP != null ? isLOP : leaveType != null && leaveType.equals("Loss of Pay (LOP)"));
                                                leave.setDepartment(department != null ? department : "");
                                                leave.setDesignation(designation != null ? designation : "");
                                                leave.setManagerId(managerId);
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
                                                Log.e("MANAGER_DASHBOARD", "Error parsing document: " + e.getMessage());
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
                                                tvEmpty.setText("No pending leaves for approval");
                                            }
                                        } else {
                                            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
                                            if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
                                        }

                                        Log.d("MANAGER_DASHBOARD", "Loaded " + count + " pending leaves for manager approval");

                                    } else {
                                        Log.e("MANAGER_DASHBOARD", "Failed to load leaves: " + task.getException());
                                        if (tvEmpty != null) {
                                            tvEmpty.setText("Error loading leaves: " + task.getException().getMessage());
                                        }
                                        Toast.makeText(this, "Failed to load leaves", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        tvEmpty.setText("Manager data not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MANAGER_DASHBOARD", "Failed to get manager data: " + e.getMessage());
                    tvEmpty.setText("Error loading manager data");
                });
    }

    private void loadTotalEmployeesInDepartment(String department) {
        if (department == null || department.isEmpty()) {
            if (tvTotalEmployees != null) {
                tvTotalEmployees.setText("Total Employees: 0");
            }
            return;
        }

        db.collection("users")
                .whereEqualTo("department", department)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && tvTotalEmployees != null) {
                        int count = task.getResult().size();
                        tvTotalEmployees.setText("Department Employees: " + count);
                    } else if (tvTotalEmployees != null) {
                        tvTotalEmployees.setText("Department Employees: 0");
                    }
                });
    }

    private void loadNotificationCount() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    if (count > 0 && tvNotificationCount != null) {
                        tvNotificationCount.setText(String.valueOf(count));
                        tvNotificationCount.setVisibility(View.VISIBLE);
                    } else if (tvNotificationCount != null) {
                        tvNotificationCount.setVisibility(View.GONE);
                    }
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

    public void onViewLeavesClick(View view) {
        loadPendingLeaves();
        Toast.makeText(this, "Refreshing leaves...", Toast.LENGTH_SHORT).show();
    }

    // ADDED: Emergency requests quick action
    public void onViewEmergencyRequestsClick(View view) {
        startActivity(new Intent(this,ManagerEmergencyActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAuth.getCurrentUser() != null) {
            loadPendingLeaves();
            loadNotificationCount();
        }
    }
}