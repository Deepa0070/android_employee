package com.yourname.employee;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import models.CommonLeavePool;
import models.DonationRequest;

public class CommonPoolActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageView ivMenu;
    private NavigationView navigationView;

    private TextView tvCasualTotal, tvCasualUsed, tvCasualAvailable;
    private TextView tvSickTotal, tvSickUsed, tvSickAvailable;
    private Button btnManageCasual, btnManageSick;
    private RecyclerView recyclerView;
    private TextView tvEmpty;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private List<DonationRequest> donationList;
    private CommonPoolAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common_pool);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        donationList = new ArrayList<>();

        initializeViews();
        setupNavigationMenu();
        loadCommonPoolStats();
        loadRecentDonations();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        ivMenu = findViewById(R.id.ivMenu);
        navigationView = findViewById(R.id.navigationView);

        tvCasualTotal = findViewById(R.id.tvCasualTotal);
        tvCasualUsed = findViewById(R.id.tvCasualUsed);
        tvCasualAvailable = findViewById(R.id.tvCasualAvailable);
        tvSickTotal = findViewById(R.id.tvSickTotal);
        tvSickUsed = findViewById(R.id.tvSickUsed);
        tvSickAvailable = findViewById(R.id.tvSickAvailable);

        btnManageCasual = findViewById(R.id.btnManageCasual);
        btnManageSick = findViewById(R.id.btnManageSick);

        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);

        ivMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CommonPoolAdapter(donationList, this);
        recyclerView.setAdapter(adapter);

        // Button listeners
        btnManageCasual.setOnClickListener(v -> {
            Toast.makeText(this, "Manage Casual Pool feature coming soon", Toast.LENGTH_SHORT).show();
        });

        btnManageSick.setOnClickListener(v -> {
            Toast.makeText(this, "Manage Sick Pool feature coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupNavigationMenu() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, HrDashboardActivity.class));
                finish();
            } else if (id == R.id.nav_pending_leaves) {
                startActivity(new Intent(this, HrDashboardActivity.class));
                finish();
            } else if (id == R.id.nav_donation_requests) {
                startActivity(new Intent(this, DonationHistoryActivity.class));
                finish();
            } else if (id == R.id.nav_all_employees) {
                startActivity(new Intent(this, AllEmployeesActivity.class));
            } else if (id == R.id.nav_add_employee) {
                startActivity(new Intent(this, AddEmployeeActivity.class));
            } else if (id == R.id.nav_logout) {
                logoutUser();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void loadCommonPoolStats() {
        // Load Casual Leave Pool
        db.collection("commonLeavePool")
                .whereEqualTo("leaveType", "casual")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        int total = document.getLong("totalDays").intValue();
                        int used = document.getLong("usedDays").intValue();
                        int available = total - used;

                        tvCasualTotal.setText(total + " days");
                        tvCasualUsed.setText(used + " days");
                        tvCasualAvailable.setText(available + " days");
                    } else {
                        // Initialize if not exists
                        tvCasualTotal.setText("0 days");
                        tvCasualUsed.setText("0 days");
                        tvCasualAvailable.setText("0 days");
                    }
                });

        // Load Sick Leave Pool
        db.collection("commonLeavePool")
                .whereEqualTo("leaveType", "sick")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        int total = document.getLong("totalDays").intValue();
                        int used = document.getLong("usedDays").intValue();
                        int available = total - used;

                        tvSickTotal.setText(total + " days");
                        tvSickUsed.setText(used + " days");
                        tvSickAvailable.setText(available + " days");
                    } else {
                        // Initialize if not exists
                        tvSickTotal.setText("0 days");
                        tvSickUsed.setText("0 days");
                        tvSickAvailable.setText("0 days");
                    }
                });
    }

    private void loadRecentDonations() {
        // Load recent donations to common pool (approved ones)
        db.collection("donationRequests")
                .whereEqualTo("recipientId", null) // Donations to common pool
                .whereEqualTo("status", "approved")
                .limit(10) // Limit to recent 10
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    donationList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        DonationRequest donation = document.toObject(DonationRequest.class);
                        donation.setId(document.getId());
                        donationList.add(donation);
                    }

                    adapter.notifyDataSetChanged();

                    if (donationList.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load donations: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
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
        loadCommonPoolStats();
        loadRecentDonations();
    }
}