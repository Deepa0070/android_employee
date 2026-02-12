package com.yourname.employee;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AllEmployeesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmpty, tvCount;
    private ProgressBar progressBar;

    private List<Employee> employeeList;
    private EmployeeAdapter adapter;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_employees);

        db = FirebaseFirestore.getInstance();
        employeeList = new ArrayList<>();

        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvCount = findViewById(R.id.tvCount);
        progressBar = findViewById(R.id.progressBar);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EmployeeAdapter(employeeList, this);
        recyclerView.setAdapter(adapter);

        loadAllEmployees();
    }

    private void loadAllEmployees() {
        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        // Fetch all users
        db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        employeeList.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                // Get user data
                                String name = document.getString("name");
                                String email = document.getString("email");
                                String department = document.getString("department");
                                String designation = document.getString("designation");
                                String role = document.getString("role");
                                String userId = document.getId();

                                // Get leave balance if exists
                                int casualLeave = 12;
                                int sickLeave = 12;
                                int lopBalance = 0;

                                if (document.contains("leaveBalance")) {
                                    Object leaveBalanceObj = document.get("leaveBalance");
                                    if (leaveBalanceObj instanceof Map) {
                                        Map<String, Object> leaveBalance = (Map<String, Object>) leaveBalanceObj;

                                        Object casualObj = leaveBalance.get("casual");
                                        Object sickObj = leaveBalance.get("sick");
                                        Object lopObj = leaveBalance.get("lop");

                                        if (casualObj instanceof Long) {
                                            casualLeave = ((Long) casualObj).intValue();
                                        } else if (casualObj instanceof Integer) {
                                            casualLeave = (Integer) casualObj;
                                        }

                                        if (sickObj instanceof Long) {
                                            sickLeave = ((Long) sickObj).intValue();
                                        } else if (sickObj instanceof Integer) {
                                            sickLeave = (Integer) sickObj;
                                        }

                                        if (lopObj instanceof Long) {
                                            lopBalance = ((Long) lopObj).intValue();
                                        } else if (lopObj instanceof Integer) {
                                            lopBalance = (Integer) lopObj;
                                        }
                                    }
                                }

                                // Create Employee object
                                Employee employee = new Employee(
                                        userId,
                                        name != null ? name : "Unknown",
                                        email != null ? email : "No Email",
                                        department != null ? department : "Not Assigned",
                                        designation != null ? designation : "Not Assigned",
                                        role != null ? role : "employee",
                                        casualLeave,
                                        sickLeave,
                                        lopBalance
                                );

                                employeeList.add(employee);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        // Debug: Check if data is loaded
                        Toast.makeText(this, "Loaded " + employeeList.size() + " employees", Toast.LENGTH_SHORT).show();

                        // Notify adapter
                        adapter.notifyDataSetChanged();

                        // Update count in toolbar
                        if (tvCount != null) {
                            tvCount.setText(String.valueOf(employeeList.size()));
                        }

                        if (employeeList.isEmpty()) {
                            tvEmpty.setText("No employees found");
                            tvEmpty.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            recyclerView.setVisibility(View.VISIBLE);
                            tvEmpty.setVisibility(View.GONE);
                        }

                    } else {
                        Toast.makeText(this, "Failed to load employees: " +
                                        task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                        tvEmpty.setText("Error loading employees");
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllEmployees();
    }
}