package com.yourname.employee;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import models.EmergencyRequest;

public class EmergencyRequestsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private List<EmergencyRequest> requestList;
    private EmergencyRequestAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_requests);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        requestList = new ArrayList<>();

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EmergencyRequestAdapter(requestList, this, db);
        recyclerView.setAdapter(adapter);

        // Check for auto actions from notification
        checkNotificationIntent();

        loadEmergencyRequests();
    }

    private void checkNotificationIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            String emergencyRequestId = intent.getStringExtra("emergencyRequestId");
            boolean autoApprove = intent.getBooleanExtra("auto_approve", false);
            boolean autoReject = intent.getBooleanExtra("auto_reject", false);

            Log.d("EMERGENCY_REQUESTS", "Intent received - RequestId: " + emergencyRequestId +
                    ", AutoApprove: " + autoApprove + ", AutoReject: " + autoReject);

            if (emergencyRequestId != null) {
                if (autoApprove) {
                    // Find and approve the request
                    approveEmergencyRequestFromNotification(emergencyRequestId);
                } else if (autoReject) {
                    // Find and reject the request
                    rejectEmergencyRequestFromNotification(emergencyRequestId);
                }
            }
        }
    }

    private void approveEmergencyRequestFromNotification(String requestId) {
        Log.d("EMERGENCY_REQUESTS", "Auto-approving request from notification: " + requestId);

        // First load all requests to find the right one
        db.collection("emergencyRequests").document(requestId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        EmergencyRequest request = documentSnapshot.toObject(EmergencyRequest.class);
                        request.setId(documentSnapshot.getId());

                        // Show message
                        Toast.makeText(this, "Auto-approving emergency request for " +
                                request.getEmployeeName(), Toast.LENGTH_SHORT).show();

                        // Call the adapter's approval method
                        if (adapter != null) {
                            // You'll need to add a public method to your adapter to handle this
                            // For now, we'll reload the list which will show the request
                            loadEmergencyRequests();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("EMERGENCY_REQUESTS", "Error finding request: " + e.getMessage());
                });
    }

    private void rejectEmergencyRequestFromNotification(String requestId) {
        Log.d("EMERGENCY_REQUESTS", "Auto-rejecting request from notification: " + requestId);

        db.collection("emergencyRequests").document(requestId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        EmergencyRequest request = documentSnapshot.toObject(EmergencyRequest.class);
                        request.setId(documentSnapshot.getId());

                        Toast.makeText(this, "Auto-rejecting emergency request for " +
                                request.getEmployeeName(), Toast.LENGTH_SHORT).show();

                        // Reload list
                        loadEmergencyRequests();
                    }
                });
    }

    private void loadEmergencyRequests() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        Log.d("EMERGENCY_REQUESTS", "Loading emergency requests...");

        // Load pending emergency requests for HR approval
        db.collection("emergencyRequests")
                .whereIn("status", Arrays.asList("pending", "pending_manager"))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("EMERGENCY_REQUESTS", "Found " + queryDocumentSnapshots.size() + " requests");

                    requestList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            EmergencyRequest request = document.toObject(EmergencyRequest.class);
                            request.setId(document.getId());
                            requestList.add(request);

                            Log.d("EMERGENCY_REQUESTS", "Request: " + request.getEmployeeName() +
                                    " - " + request.getNumberOfDays() + " days " +
                                    request.getLeaveType() + " - Status: " + request.getStatus());
                        } catch (Exception e) {
                            Log.e("EMERGENCY_REQUESTS", "Error parsing document: " + e.getMessage());
                        }
                    }

                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);

                    if (requestList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("No pending emergency requests");
                        Log.d("EMERGENCY_REQUESTS", "No requests found");
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        Log.d("EMERGENCY_REQUESTS", "Loaded " + requestList.size() + " requests");
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e("EMERGENCY_REQUESTS", "Error: " + e.getMessage());
                    Toast.makeText(this, "Error loading requests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Error loading requests");
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check for new intents when resuming
        checkNotificationIntent();
        loadEmergencyRequests();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkNotificationIntent();
    }

    public void debugEmergencyRequests(View view) {
        db.collection("emergencyRequests")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("DEBUG", "=== ALL EMERGENCY REQUESTS IN FIRESTORE ===");
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Log.d("DEBUG", "ID: " + doc.getId());
                        Log.d("DEBUG", "Data: " + doc.getData());
                        Log.d("DEBUG", "--------------------------------");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("DEBUG", "Error: " + e.getMessage());
                });
    }
}