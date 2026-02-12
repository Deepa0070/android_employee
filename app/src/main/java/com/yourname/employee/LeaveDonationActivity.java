package com.yourname.employee;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeaveDonationActivity extends AppCompatActivity {

    private RadioGroup rgDonationType;
    private RadioButton rbSpecificEmployee, rbCommonPool;
    private LinearLayout layoutRecipient;
    private Spinner spinnerEmployees, spLeaveType;
    private EditText etNumberOfDays, etReason;
    private TextView tvAvailableBalance;
    private Button btnSubmitDonation;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private List<Employee> employeeList;
    private ArrayAdapter<Employee> employeeAdapter;
    private String selectedRecipientId = "";
    private String selectedRecipientName = "";
    private int availableCasual = 0;
    private int availableSick = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate_leave);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        employeeList = new ArrayList<>();

        initializeViews();
        setupLeaveTypeSpinner();
        setupDonationTypeListener();
        loadAvailableBalance();
        loadEmployees();
    }

    private void initializeViews() {
        rgDonationType = findViewById(R.id.rgDonationType);
        rbSpecificEmployee = findViewById(R.id.rbSpecificEmployee);
        rbCommonPool = findViewById(R.id.rbCommonPool);
        layoutRecipient = findViewById(R.id.layoutRecipient);
        spinnerEmployees = findViewById(R.id.spinnerEmployees);
        spLeaveType = findViewById(R.id.spLeaveType);
        etNumberOfDays = findViewById(R.id.etNumberOfDays);
        etReason = findViewById(R.id.etReason);
        tvAvailableBalance = findViewById(R.id.tvAvailableBalance);
        btnSubmitDonation = findViewById(R.id.btnSubmitDonation);
        progressBar = findViewById(R.id.progressBar);

        btnSubmitDonation.setOnClickListener(v -> submitDonation());
    }

    private void setupLeaveTypeSpinner() {
        String[] leaveTypes = {"Casual Leave", "Sick Leave"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, leaveTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLeaveType.setAdapter(adapter);

        spLeaveType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateAvailableBalanceDisplay();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupDonationTypeListener() {
        rgDonationType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbCommonPool) {
                layoutRecipient.setVisibility(View.GONE);
            } else {
                layoutRecipient.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadAvailableBalance() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> leaveBalance = (Map<String, Object>)
                                documentSnapshot.get("leaveBalance");
                        if (leaveBalance != null) {
                            availableCasual = ((Long) leaveBalance.getOrDefault("casual", 0L)).intValue();
                            availableSick = ((Long) leaveBalance.getOrDefault("sick", 0L)).intValue();
                            updateAvailableBalanceDisplay();
                        }
                    }
                });
    }

    private void updateAvailableBalanceDisplay() {
        String selectedLeaveType = spLeaveType.getSelectedItem().toString();
        int available = selectedLeaveType.equals("Casual Leave") ? availableCasual : availableSick;

        tvAvailableBalance.setText(String.format(
                "Available %s balance: %d days",
                selectedLeaveType, available));
    }

    private void loadEmployees() {
        // Exclude current user from recipient list
        String currentUserId = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .whereNotEqualTo("id", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    employeeList.clear();

                    // Add a placeholder for "Select Employee"
                    employeeList.add(new Employee("", "Select Employee", "", "", "", "", 0, 0, 0));

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String id = document.getId();
                        String name = document.getString("name");
                        String email = document.getString("email");
                        String department = document.getString("department");
                        String designation = document.getString("designation");
                        String role = document.getString("role");

                        // Get leave balance
                        Map<String, Object> leaveBalance = (Map<String, Object>) document.get("leaveBalance");
                        int casual = 0, sick = 0, lop = 0;
                        if (leaveBalance != null) {
                            casual = ((Long) leaveBalance.getOrDefault("casual", 0L)).intValue();
                            sick = ((Long) leaveBalance.getOrDefault("sick", 0L)).intValue();
                            lop = ((Long) leaveBalance.getOrDefault("lop", 0L)).intValue();
                        }

                        employeeList.add(new Employee(id, name, email, department,
                                designation, role, casual, sick, lop));
                    }

                    employeeAdapter = new ArrayAdapter<Employee>(this,
                            android.R.layout.simple_spinner_item, employeeList) {
                        @NonNull
                        public String toString(Employee employee) {
                            return employee.getName() + " (" + employee.getDepartment() + ")";
                        }
                    };
                    employeeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerEmployees.setAdapter(employeeAdapter);

                    spinnerEmployees.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            Employee selected = employeeList.get(position);
                            selectedRecipientId = selected.getId();
                            selectedRecipientName = selected.getName();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });
                });
    }

    private void submitDonation() {
        // Get form values
        boolean toCommonPool = rbCommonPool.isChecked();
        String leaveType = spLeaveType.getSelectedItem().toString();
        String daysStr = etNumberOfDays.getText().toString().trim();
        String reason = etReason.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(daysStr)) {
            etNumberOfDays.setError("Enter number of days");
            return;
        }

        int numberOfDays = Integer.parseInt(daysStr);
        if (numberOfDays <= 0) {
            etNumberOfDays.setError("Days must be greater than 0");
            return;
        }

        if (TextUtils.isEmpty(reason)) {
            etReason.setError("Enter reason for donation");
            return;
        }

        // Check if donating to specific employee but no recipient selected
        if (!toCommonPool && (TextUtils.isEmpty(selectedRecipientId) ||
                selectedRecipientId.equals(""))) {
            Toast.makeText(this, "Please select a recipient", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check available balance
        int availableBalance = leaveType.equals("Casual Leave") ? availableCasual : availableSick;
        if (numberOfDays > availableBalance) {
            Toast.makeText(this,
                    String.format("Not enough balance. Available: %d days", availableBalance),
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Get donor info
        String donorId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(donorId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String donorName = documentSnapshot.getString("name");

                    // Create donation request
                    Map<String, Object> donationRequest = new HashMap<>();
                    donationRequest.put("donorId", donorId);
                    donationRequest.put("donorName", donorName);
                    donationRequest.put("leaveType", leaveType);
                    donationRequest.put("numberOfDays", numberOfDays);
                    donationRequest.put("reason", reason);
                    donationRequest.put("status", "pending");
                    donationRequest.put("appliedDate", new Date());

                    if (toCommonPool) {
                        donationRequest.put("recipientId", null);
                        donationRequest.put("recipientName", "Common Pool");
                    } else {
                        donationRequest.put("recipientId", selectedRecipientId);
                        donationRequest.put("recipientName", selectedRecipientName);
                    }

                    // Show progress
                    progressBar.setVisibility(View.VISIBLE);
                    btnSubmitDonation.setEnabled(false);

                    // Save to Firestore
                    db.collection("donationRequests")
                            .add(donationRequest)
                            .addOnSuccessListener(documentReference -> {
                                progressBar.setVisibility(View.GONE);
                                btnSubmitDonation.setEnabled(true);

                                Toast.makeText(this,
                                        "Donation request submitted successfully! Awaiting HR approval.",
                                        Toast.LENGTH_LONG).show();

                                // Clear form
                                etNumberOfDays.setText("");
                                etReason.setText("");
                                spLeaveType.setSelection(0);
                                if (!toCommonPool) {
                                    spinnerEmployees.setSelection(0);
                                }

                                // Notify HR about new donation request
                                notifyHRAboutDonation(donorName, leaveType, numberOfDays,
                                        toCommonPool ? "Common Pool" : selectedRecipientName);
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                btnSubmitDonation.setEnabled(true);
                                Toast.makeText(this,
                                        "Failed to submit donation: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void notifyHRAboutDonation(String donorName, String leaveType,
                                       int numberOfDays, String recipient) {
        // Find only ONE HR user (limit to 1)
        db.collection("users")
                .whereEqualTo("role", "hr admin")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        String hrUserId = document.getId();

                        // Create notification for HR
                        Map<String, Object> notification = new HashMap<>();
                        notification.put("userId", hrUserId);
                        notification.put("title", "New Leave Donation Request");
                        notification.put("message", donorName + " wants to donate " +
                                numberOfDays + " days of " + leaveType + " to " + recipient);
                        notification.put("type", "new_donation_request");
                        notification.put("timestamp", System.currentTimeMillis());
                        notification.put("read", false);

                        db.collection("notifications").add(notification);
                    }
                });
    }
}