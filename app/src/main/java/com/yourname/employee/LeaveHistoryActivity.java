package com.yourname.employee;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import models.LeaveRequest;

import java.util.ArrayList;
import java.util.List;

public class LeaveHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private List<LeaveRequest> leaveList;
    private LeaveHistoryAdapter adapter; // Changed to LeaveHistoryAdapter

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leave_history);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        leaveList = new ArrayList<>();

        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create adapter for leave history (not HR view)
        adapter = new LeaveHistoryAdapter(leaveList, this);
        recyclerView.setAdapter(adapter);

        loadLeaveHistory();
    }

    private void loadLeaveHistory() {
        String userId = mAuth.getCurrentUser().getUid();

        // Show loading
        tvEmpty.setText("Loading leave history...");
        tvEmpty.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        db.collection("leaveRequests")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    leaveList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        LeaveRequest leave = document.toObject(LeaveRequest.class);
                        leave.setId(document.getId());
                        leaveList.add(leave);
                    }

                    adapter.notifyDataSetChanged();

                    if (leaveList.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("No leave history found");
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load leave history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    tvEmpty.setText("Error loading leave history");
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLeaveHistory();
    }
}