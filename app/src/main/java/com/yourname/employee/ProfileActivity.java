package com.yourname.employee;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageView ivMenu;
    private NavigationView navigationView;

    private TextView tvName, tvEmail, tvEmployeeId, tvDepartment, tvDesignation, tvRole;
    private TextView tvJoiningDate, tvPhone, tvAddress, tvBasicSalary;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupNavigationMenu();
        loadProfileData();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        ivMenu = findViewById(R.id.ivMenu);
        navigationView = findViewById(R.id.navigationView);

        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvEmployeeId = findViewById(R.id.tvEmployeeId);
        tvDepartment = findViewById(R.id.tvDepartment);
        tvDesignation = findViewById(R.id.tvDesignation);
        tvRole = findViewById(R.id.tvRole);
        tvJoiningDate = findViewById(R.id.tvJoiningDate);
        tvPhone = findViewById(R.id.tvPhone);
        tvAddress = findViewById(R.id.tvAddress);
        tvBasicSalary = findViewById(R.id.tvBasicSalary);
        progressBar = findViewById(R.id.progressBar);

        ivMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
    }

    private void setupNavigationMenu() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, EmployeeDashboardActivity.class));
                finish();
            } else if (id == R.id.nav_apply_leave) {
                startActivity(new Intent(this, ApplyLeaveActivity.class));
            } else if (id == R.id.nav_leave_history) {
                startActivity(new Intent(this, LeaveHistoryActivity.class));
            } else if (id == R.id.nav_salary) {
                startActivity(new Intent(this, SalaryActivity.class));
            } else if (id == R.id.nav_holidays) {
                startActivity(new Intent(this, HolidayCalendarActivity.class));
            } else if (id == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationsActivity.class));
            } else if (id == R.id.nav_logout) {
                logoutUser();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void loadProfileData() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        progressBar.setVisibility(View.VISIBLE);

        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot document = task.getResult();

                        // Set basic information
                        tvName.setText(document.getString("name"));
                        tvEmail.setText(document.getString("email"));
                        tvDepartment.setText(document.getString("department"));
                        tvDesignation.setText(document.getString("designation"));
                        tvRole.setText(document.getString("role"));

                        // Set employee ID (using document ID or a specific field)
                        tvEmployeeId.setText(userId.substring(0, 8).toUpperCase());

                        // Set phone if available
                        if (document.contains("phone")) {
                            tvPhone.setText(document.getString("phone"));
                        } else {
                            tvPhone.setText("Not provided");
                        }

                        // Set address if available
                        if (document.contains("address")) {
                            tvAddress.setText(document.getString("address"));
                        } else {
                            tvAddress.setText("Not provided");
                        }

                        // Set joining date if available
                        if (document.contains("joiningDate")) {
                            tvJoiningDate.setText(document.getString("joiningDate"));
                        } else {
                            tvJoiningDate.setText("Not set");
                        }

                        // Set basic salary - FIXED: Use Double instead of double
                        if (document.contains("basicSalary")) {
                            Double salary = document.getDouble("basicSalary");
                            if (salary != null) {
                                tvBasicSalary.setText(String.format("₹%.2f", salary));
                            } else {
                                tvBasicSalary.setText("Not set");
                            }
                        } else {
                            tvBasicSalary.setText("Not set");
                        }

                        // Display leave balance
                        if (document.contains("leaveBalance")) {
                            Map<String, Object> leaves = (Map<String, Object>) document.get("leaveBalance");
                            int casual = ((Long) leaves.getOrDefault("casual", 12L)).intValue();
                            int sick = ((Long) leaves.getOrDefault("sick", 10L)).intValue();
                            int lop = ((Long) leaves.getOrDefault("lop", 0L)).intValue();

                            // Create a text view for leave balance or update existing one
                            TextView tvLeaveBalance = findViewById(R.id.tvLeaveBalance);
                            if (tvLeaveBalance != null) {
                                String leaveBalanceText = String.format(
                                        "Leave Balance:\n" +
                                                "• Casual: %d days\n" +
                                                "• Sick: %d days\n" +
                                                "• LOP Balance: %d days",
                                        casual, sick, lop
                                );
                                tvLeaveBalance.setText(leaveBalanceText);
                            }
                        }

                    } else {
                        Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    // Button click method to edit profile
    public void onEditProfileClick(View view) {
        Toast.makeText(this, "Edit profile feature coming soon", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileData();
    }
}