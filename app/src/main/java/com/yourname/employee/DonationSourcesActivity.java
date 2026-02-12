package com.yourname.employee;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import models.DonationRequest;

public class DonationSourcesActivity extends AppCompatActivity {

    private TextView tvEmergencyInfo, tvEmpty;
    private RecyclerView recyclerView;
    private ImageView ivBack;

    private FirebaseFirestore db;
    private List<DonationRequest> donationList;
    private DonationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation_sources);

        String emergencyRequestId = getIntent().getStringExtra("emergencyRequestId");

        if (emergencyRequestId == null || emergencyRequestId.isEmpty()) {
            Toast.makeText(this, "No emergency request ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        donationList = new ArrayList<>();

        // Initialize views
        ivBack = findViewById(R.id.ivBack);
        tvEmergencyInfo = findViewById(R.id.tvEmergencyInfo);
        tvEmpty = findViewById(R.id.tvEmpty);
        recyclerView = findViewById(R.id.recyclerView);

        // Setup back button
        ivBack.setOnClickListener(v -> onBackPressed());

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DonationAdapter(donationList, this, "hr");
        recyclerView.setAdapter(adapter);

        loadDonationSources(emergencyRequestId);
    }

    private void loadDonationSources(String emergencyRequestId) {
        // Show loading state
        tvEmpty.setText("Loading donation sources...");
        tvEmpty.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        // Load emergency request details first
        db.collection("emergencyRequests").document(emergencyRequestId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String employeeName = documentSnapshot.getString("employeeName");
                        int days = documentSnapshot.getLong("numberOfDays").intValue();
                        String leaveType = documentSnapshot.getString("leaveType");
                        String fromDate = documentSnapshot.getString("fromDate");
                        String toDate = documentSnapshot.getString("toDate");

                        String infoText = employeeName + "\n" +
                                days + " days " + leaveType + "\n" +
                                "Dates: " + fromDate + " to " + toDate;

                        tvEmergencyInfo.setText(infoText);

                        // Load source donations
                        List<String> sourceDonations = (List<String>) documentSnapshot.get("sourceDonations");
                        if (sourceDonations != null && !sourceDonations.isEmpty()) {
                            loadDonationDetails(sourceDonations);
                        } else {
                            tvEmpty.setText("No donations were used for this request");
                            tvEmpty.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        }
                    } else {
                        tvEmpty.setText("Emergency request not found");
                        tvEmpty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    tvEmpty.setText("Error loading data: " + e.getMessage());
                    tvEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadDonationDetails(List<String> donationIds) {
        for (String donationId : donationIds) {
            db.collection("donationRequests").document(donationId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            DonationRequest donation = documentSnapshot.toObject(DonationRequest.class);
                            if (donation != null) {
                                donation.setId(documentSnapshot.getId());
                                donationList.add(donation);
                                adapter.notifyDataSetChanged();

                                // Update UI when donations are loaded
                                if (donationList.isEmpty()) {
                                    tvEmpty.setText("No donation details found");
                                    tvEmpty.setVisibility(View.VISIBLE);
                                    recyclerView.setVisibility(View.GONE);
                                } else {
                                    tvEmpty.setVisibility(View.GONE);
                                    recyclerView.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error loading donation: " + donationId, Toast.LENGTH_SHORT).show();
                    });
        }
    }
}