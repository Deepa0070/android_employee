package com.yourname.employee;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.util.List;

import models.EmergencyRequest;

public class ManagerEmergencyActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private List<EmergencyRequest> emergencyList;
    private ManagerEmergencyAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_emergency);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        emergencyList = new ArrayList<>();

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ManagerEmergencyAdapter(emergencyList, this, db);
        recyclerView.setAdapter(adapter);

        loadEmergencyRequests();
    }

    private void loadEmergencyRequests() {
        String managerId = mAuth.getCurrentUser().getUid();

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        Log.d("MANAGER_EMERGENCY", "Loading emergency requests for manager: " + managerId);

        // Load emergency requests where managerId matches AND status is pending_manager
        db.collection("emergencyRequests")
                .whereEqualTo("managerId", managerId)
                .whereEqualTo("status", "pending_manager")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    emergencyList.clear();
                    Log.d("MANAGER_EMERGENCY", "Found " + queryDocumentSnapshots.size() + " requests for this manager");

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            EmergencyRequest request = document.toObject(EmergencyRequest.class);
                            request.setId(document.getId());
                            emergencyList.add(request);

                            Log.d("MANAGER_EMERGENCY", "Added request: " + request.getEmployeeName() +
                                    " - Status: " + request.getStatus() +
                                    " - Manager ID: " + request.getManagerId());
                        } catch (Exception e) {
                            Log.e("MANAGER_EMERGENCY", "Error parsing document: " + e.getMessage());
                        }
                    }

                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);

                    if (emergencyList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        tvEmpty.setText("No pending emergency requests assigned to you");
                        Log.d("MANAGER_EMERGENCY", "No emergency requests found");
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        Log.d("MANAGER_EMERGENCY", "Loaded " + emergencyList.size() + " requests");
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e("MANAGER_EMERGENCY", "Error loading requests: " + e.getMessage());
                    tvEmpty.setText("Error loading requests: " + e.getMessage());
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEmergencyRequests();
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