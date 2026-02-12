package com.yourname.employee;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class EmployeeDashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageView ivMenu;
    private TextView tvWelcome, tvName, tvDepartment, tvLeaveBalance, tvDesignation, tvNotificationCount;
    private NavigationView navigationView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Setup menu click
        ivMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // Setup navigation menu
        setupNavigationMenu();

        // Load user data
        loadUserData();
        loadNotificationCount();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        ivMenu = findViewById(R.id.ivMenu);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvName = findViewById(R.id.tvName);
        tvDepartment = findViewById(R.id.tvDepartment);
        tvLeaveBalance = findViewById(R.id.tvLeaveBalance);
        tvDesignation = findViewById(R.id.tvDesignation);
        tvNotificationCount = findViewById(R.id.tvNotificationCount);
        navigationView = findViewById(R.id.navigationView);
    }

    private void setupNavigationMenu() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (id == R.id.nav_apply_leave) {
                startActivity(new Intent(this, ApplyLeaveActivity.class));
            } else if (id == R.id.nav_donate_leave) {
                startActivity(new Intent(this, LeaveDonationActivity.class));
            } else if (id == R.id.nav_donation_history) {
                startActivity(new Intent(this, DonationHistoryActivity.class));
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

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot document = task.getResult();

                        String name = document.getString("name");
                        String department = document.getString("department");
                        String designation = document.getString("designation");
                        String role = document.getString("role");

                        // Update UI
                        if (name != null) {
                            tvWelcome.setText("Welcome, " + name);
                            tvName.setText(name);
                        }

                        if (designation != null) {
                            tvDesignation.setText(designation);
                        }

                        if (department != null) {
                            tvDepartment.setText(department);
                        }

                        // Get leave balance
                        if (document.contains("leaveBalance")) {
                            Map<String, Object> leaves = (Map<String, Object>) document.get("leaveBalance");
                            int casual = ((Long) leaves.getOrDefault("casual", 12L)).intValue();
                            int sick = ((Long) leaves.getOrDefault("sick", 10L)).intValue();
                            int lop = ((Long) leaves.getOrDefault("lop", 0L)).intValue();

                            // Display leave balance
                            String leaveBalanceText = String.format(
                                    "Leave Balance:\n" +
                                            "• Casual: %d days\n" +
                                            "• Sick: %d days\n" +
                                            "• LOP Balance: %d days",
                                    casual, sick, lop
                            );
                            tvLeaveBalance.setText(leaveBalanceText);

                            // Also show LOP days for current month
                            int lopDaysThisMonth = 0;
                            if (document.contains("lopDaysThisMonth")) {
                                Object lopObj = document.get("lopDaysThisMonth");
                                if (lopObj instanceof Long) {
                                    lopDaysThisMonth = ((Long) lopObj).intValue();
                                } else if (lopObj instanceof Integer) {
                                    lopDaysThisMonth = (Integer) lopObj;
                                }
                            }

                            if (lopDaysThisMonth > 0) {
                                tvLeaveBalance.append(String.format("\n\n⚠️ Current Month LOP: %d day(s)",
                                        lopDaysThisMonth));
                            }
                        }
                    } else {
                        Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadNotificationCount() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

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
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    // Quick Actions Click Methods
    public void onApplyLeaveClick(View view) {
        startActivity(new Intent(this, ApplyLeaveActivity.class));
    }

    public void onViewSalaryClick(View view) {
        startActivity(new Intent(this, SalaryActivity.class));
    }

    public void onViewLeavesClick(View view) {
        startActivity(new Intent(this, LeaveHistoryActivity.class));
    }

    public void onViewProfileClick(View view) {
        startActivity(new Intent(this, ProfileActivity.class));
    }

    public void onDonateLeaveClick(View view) {
        startActivity(new Intent(this, LeaveDonationActivity.class));
    }

    // Emergency Leave Click Method - Check Eligibility First
    public void onEmergencyLeaveClick(View view) {
        checkEmergencyEligibility();
    }

    private void checkEmergencyEligibility() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        checkLeavesTakenThisMonth(userId, name);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking eligibility: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkLeavesTakenThisMonth(String userId, String employeeName) {
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH) + 1;
        int currentYear = calendar.get(Calendar.YEAR);

        String currentMonthName = new SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.getTime());

        db.collection("leaveRequests")
                .whereEqualTo("userId", userId)
                .whereIn("leaveType", Arrays.asList(new String[]{"Casual Leave", "Sick Leave"}))
                .whereIn("status", Arrays.asList(new String[]{"approved", "pending"}))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int casualLeavesThisMonth = 0;
                    int sickLeavesThisMonth = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            String leaveType = document.getString("leaveType");
                            String fromDate = document.getString("fromDate");

                            if (fromDate != null) {
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                Date date = sdf.parse(fromDate);
                                Calendar leaveCal = Calendar.getInstance();
                                leaveCal.setTime(date);

                                int leaveMonth = leaveCal.get(Calendar.MONTH) + 1;
                                int leaveYear = leaveCal.get(Calendar.YEAR);

                                if (leaveMonth == currentMonth && leaveYear == currentYear) {
                                    if (leaveType.equals("Casual Leave")) {
                                        casualLeavesThisMonth++;
                                    } else if (leaveType.equals("Sick Leave")) {
                                        sickLeavesThisMonth++;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // Show eligibility dialog
                    showEligibilityDialog(casualLeavesThisMonth, sickLeavesThisMonth, currentMonthName);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking leave history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showEligibilityDialog(int casualCount, int sickCount, String monthName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Emergency Leave Eligibility");

        String message = "Monthly Leave Usage for " + monthName + ":\n\n";
        message += "• Casual Leaves: " + casualCount + "/1\n";
        message += "• Sick Leaves: " + sickCount + "/1\n\n";

        // Check if eligible (must have used BOTH casual and sick leaves)
        boolean isEligible = (casualCount >= 1 && sickCount >= 1);

        if (isEligible) {
            message += "✅ ELIGIBLE for Emergency Leave!\n\n";
            message += "You have used both your casual and sick leaves for this month.\n";
            message += "You can now apply for emergency leave from the common pool.";

            builder.setMessage(message)
                    .setPositiveButton("Apply Emergency Leave", (dialog, which) -> {
                        startActivity(new Intent(this, EmergencyLeaveActivity.class));
                    })
                    .setNegativeButton("Cancel", null);
        } else {
            message += "❌ NOT ELIGIBLE for Emergency Leave\n\n";

            if (casualCount < 1 && sickCount < 1) {
                message += "You haven't used any casual or sick leaves this month.\n";
                message += "Please use your regular leaves first.";
            } else if (casualCount < 1) {
                message += "You still have 1 casual leave available.\n";
                message += "Please use it before applying for emergency leave.";
            } else if (sickCount < 1) {
                message += "You still have 1 sick leave available.\n";
                message += "Please use it before applying for emergency leave.";
            }

            builder.setMessage(message)
                    .setPositiveButton("Apply Regular Leave", (dialog, which) -> {
                        startActivity(new Intent(this, ApplyLeaveActivity.class));
                    })
                    .setNegativeButton("Cancel", null);
        }

        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
        loadNotificationCount();
    }
}