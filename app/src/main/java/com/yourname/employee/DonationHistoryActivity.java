package com.yourname.employee;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class DonationHistoryActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageView ivMenu;
    private NavigationView navigationView;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userRole = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation_history);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        checkUserRoleAndSetup();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        ivMenu = findViewById(R.id.ivMenu);
        navigationView = findViewById(R.id.navigationView);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        ivMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
    }

    private void checkUserRoleAndSetup() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String userId = currentUser.getUid();

        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot document = task.getResult();
                        userRole = document.getString("role");

                        if (userRole == null) {
                            userRole = "employee";
                        }

                        setupNavigationMenu();
                        setupViewPager();

                    } else {
                        Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupNavigationMenu() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                checkUserRoleAndRedirect();
            } else if (id == R.id.nav_donate_leave) {
                startActivity(new Intent(this, LeaveDonationActivity.class));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (id == R.id.nav_apply_leave) {
                startActivity(new Intent(this, ApplyLeaveActivity.class));
            } else if (id == R.id.nav_leave_history) {
                startActivity(new Intent(this, LeaveHistoryActivity.class));
            } else if (id == R.id.nav_salary) {
                startActivity(new Intent(this, SalaryActivity.class));
            } else if (id == R.id.nav_logout) {
                logoutUser();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void setupViewPager() {
        DonationPagerAdapter adapter = new DonationPagerAdapter(getSupportFragmentManager(), userRole);
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);

        // Show different tabs based on user role
        if (userRole != null && (userRole.toLowerCase().contains("hr") || userRole.toLowerCase().contains("admin"))) {
            // HR sees all 3 tabs
            tabLayout.setVisibility(View.VISIBLE);
        } else {
            // Employees see only 2 tabs (hide "All Donations" tab)
            tabLayout.setVisibility(View.VISIBLE);
            // We'll handle this in the adapter
        }
    }

    private void checkUserRoleAndRedirect() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String userId = currentUser.getUid();

        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot document = task.getResult();
                        String role = document.getString("role");

                        Intent intent;
                        if (role != null && role.toLowerCase().contains("hr")) {
                            intent = new Intent(DonationHistoryActivity.this, HrDashboardActivity.class);
                        } else if (role != null && role.toLowerCase().contains("manager")) {
                            intent = new Intent(DonationHistoryActivity.this, ManagerDashboardActivity.class);
                        } else {
                            intent = new Intent(DonationHistoryActivity.this, EmployeeDashboardActivity.class);
                        }

                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();

                    } else {
                        Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    // Pager Adapter for fragments
    class DonationPagerAdapter extends FragmentPagerAdapter {

        private final String[] tabTitles;
        private String userRole;

        public DonationPagerAdapter(@NonNull FragmentManager fm, String userRole) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            this.userRole = userRole;

            // HR sees all tabs, employees see only 2
            if (userRole != null && (userRole.toLowerCase().contains("hr") || userRole.toLowerCase().contains("admin"))) {
                tabTitles = new String[]{"My Donations", "Received Donations", "All Donations"};
            } else {
                tabTitles = new String[]{"My Donations", "Received Donations"};
            }
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            if (userRole != null && (userRole.toLowerCase().contains("hr") || userRole.toLowerCase().contains("admin"))) {
                // HR view
                switch (position) {
                    case 0:
                        return DonationFragment.newInstance("donor", userRole);
                    case 1:
                        return DonationFragment.newInstance("recipient", userRole);
                    case 2:
                        return DonationFragment.newInstance("all", userRole);
                    default:
                        return DonationFragment.newInstance("donor", userRole);
                }
            } else {
                // Employee view
                switch (position) {
                    case 0:
                        return DonationFragment.newInstance("donor", userRole);
                    case 1:
                        return DonationFragment.newInstance("recipient", userRole);
                    default:
                        return DonationFragment.newInstance("donor", userRole);
                }
            }
        }

        @Override
        public int getCount() {
            return tabTitles.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabTitles[position];
        }
    }
}