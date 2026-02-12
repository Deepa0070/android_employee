package com.yourname.employee;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EmergencyLeaveActivity extends AppCompatActivity {

    private EditText etFromDate, etToDate, etReason, etEmergencyDetails;
    private Spinner spLeaveType;
    private Button btnSubmit;
    private ProgressBar progressBar;
    private TextView tvPoolAvailability, tvEligibilityStatus;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_leave);

        try {
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            Log.d("EMERGENCY_LEAVE", "User logged in: " + currentUser.getUid());

            initializeViews();
            setupLeaveTypeSpinner();
            checkEligibility();

        } catch (Exception e) {
            Log.e("EMERGENCY_LEAVE", "Error in onCreate: ", e);
            Toast.makeText(this, "Error initializing emergency leave: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeViews() {
        try {
            etFromDate = findViewById(R.id.etFromDate);
            etToDate = findViewById(R.id.etToDate);
            etReason = findViewById(R.id.etReason);
            etEmergencyDetails = findViewById(R.id.etEmergencyDetails);
            spLeaveType = findViewById(R.id.spLeaveType);
            btnSubmit = findViewById(R.id.btnSubmit);
            progressBar = findViewById(R.id.progressBar);
            tvPoolAvailability = findViewById(R.id.tvPoolAvailability);
            tvEligibilityStatus = findViewById(R.id.tvEligibilityStatus);

            etFromDate.setOnClickListener(v -> showDatePicker(etFromDate));
            etToDate.setOnClickListener(v -> showDatePicker(etToDate));

            btnSubmit.setOnClickListener(v -> submitEmergencyRequest());
            btnSubmit.setEnabled(false);

            Log.d("EMERGENCY_LEAVE", "All views initialized successfully");

        } catch (Exception e) {
            Log.e("EMERGENCY_LEAVE", "Error initializing views: ", e);
            Toast.makeText(this, "Error loading UI: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupLeaveTypeSpinner() {
        try {
            String[] leaveTypes = {"Casual Leave", "Sick Leave"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, leaveTypes);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spLeaveType.setAdapter(adapter);

            spLeaveType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    if (btnSubmit.isEnabled()) {
                        loadPoolAvailability();
                    }
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {
                }
            });
        } catch (Exception e) {
            Log.e("EMERGENCY_LEAVE", "Error setting up spinner: ", e);
        }
    }

    @SuppressLint("SetTextI18n")
    private void checkEligibility() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            if (tvEligibilityStatus != null) {
                tvEligibilityStatus.setText("User not logged in");
            }
            return;
        }

        String userId = currentUser.getUid();
        if (tvEligibilityStatus != null) {
            tvEligibilityStatus.setText("Checking eligibility...");
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        if (name != null && !name.isEmpty()) {
                            checkLeavesTakenThisMonth(userId, name);
                        } else {
                            runOnUiThread(() -> {
                                if (tvEligibilityStatus != null) {
                                    tvEligibilityStatus.setText("Error: User name not found");
                                }
                                if (progressBar != null) {
                                    progressBar.setVisibility(View.GONE);
                                }
                                if (btnSubmit != null) {
                                    btnSubmit.setEnabled(false);
                                }
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            if (tvEligibilityStatus != null) {
                                tvEligibilityStatus.setText("Error: User data not found");
                            }
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                            if (btnSubmit != null) {
                                btnSubmit.setEnabled(false);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        Log.e("EMERGENCY_LEAVE", "Error checking eligibility: ", e);
                        if (tvEligibilityStatus != null) {
                            tvEligibilityStatus.setText("Error: " + e.getMessage());
                        }
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        if (btnSubmit != null) {
                            btnSubmit.setEnabled(false);
                        }
                    });
                });
    }

    @SuppressLint("SetTextI18n")
    private void checkLeavesTakenThisMonth(String userId, String employeeName) {
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH) + 1;
        int currentYear = calendar.get(Calendar.YEAR);

        db.collection("leaveRequests")
                .whereEqualTo("userId", userId)
                .whereIn("leaveType", Arrays.asList("Casual Leave", "Sick Leave"))
                .whereIn("status", Arrays.asList("approved", "pending"))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int casualLeavesThisMonth = 0;
                    int sickLeavesThisMonth = 0;

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
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
                                    if ("Casual Leave".equals(leaveType)) {
                                        casualLeavesThisMonth++;
                                    } else if ("Sick Leave".equals(leaveType)) {
                                        sickLeavesThisMonth++;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e("EMERGENCY_LEAVE", "Error parsing date: " + e.getMessage());
                        }
                    }

                    boolean isEligible = (casualLeavesThisMonth >= 1 && sickLeavesThisMonth >= 1);

                    int finalCasualLeavesThisMonth = casualLeavesThisMonth;
                    int finalSickLeavesThisMonth = sickLeavesThisMonth;
                    runOnUiThread(() -> {
                        if (isEligible) {
                            if (tvEligibilityStatus != null) {
                                tvEligibilityStatus.setText("✅ Eligible for Emergency Leave\nYou have used both casual and sick leaves this month");
                                tvEligibilityStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                            }
                            if (btnSubmit != null) {
                                btnSubmit.setEnabled(true);
                            }
                            loadPoolAvailability();
                        } else {
                            String message = "❌ Not Eligible for Emergency Leave\n";
                            message += "Casual leaves taken: " + finalCasualLeavesThisMonth + "/1\n";
                            message += "Sick leaves taken: " + finalSickLeavesThisMonth + "/1\n";
                            message += "You must use both casual and sick leaves first.";

                            if (tvEligibilityStatus != null) {
                                tvEligibilityStatus.setText(message);
                                tvEligibilityStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                            }
                            if (btnSubmit != null) {
                                btnSubmit.setEnabled(false);
                            }
                            if (tvPoolAvailability != null) {
                                tvPoolAvailability.setText("Not eligible - Check eligibility status above");
                            }
                        }
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        Log.e("EMERGENCY_LEAVE", "Error checking leaves: ", e);
                        Toast.makeText(this, "Error checking leave history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        if (tvEligibilityStatus != null) {
                            tvEligibilityStatus.setText("Error checking eligibility");
                        }
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        if (btnSubmit != null) {
                            btnSubmit.setEnabled(false);
                        }
                    });
                });
    }

    @SuppressLint("SetTextI18n")
    private void loadPoolAvailability() {
        try {
            if (spLeaveType == null || spLeaveType.getSelectedItem() == null) {
                return;
            }

            String selectedType = spLeaveType.getSelectedItem().toString();
            String poolType = selectedType.equals("Casual Leave") ? "casual" : "sick";

            if (tvPoolAvailability != null) {
                tvPoolAvailability.setText("Loading pool availability...");
            }

            db.collection("commonLeavePool")
                    .whereEqualTo("leaveType", poolType)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        runOnUiThread(() -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                DocumentSnapshot poolDoc = queryDocumentSnapshots.getDocuments().get(0);
                                Long total = poolDoc.getLong("totalDays");
                                Long used = poolDoc.getLong("usedDays");

                                if (total != null && used != null && tvPoolAvailability != null) {
                                    int available = total.intValue() - used.intValue();
                                    tvPoolAvailability.setText("Available in Common Pool: " + available + " days");

                                    if (available <= 0) {
                                        tvPoolAvailability.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                                        tvPoolAvailability.append("\n⚠️ No leaves available in common pool!");
                                        if (btnSubmit != null) {
                                            btnSubmit.setEnabled(false);
                                        }
                                    } else {
                                        tvPoolAvailability.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                                        if (btnSubmit != null) {
                                            btnSubmit.setEnabled(true);
                                        }
                                    }
                                }
                            } else if (tvPoolAvailability != null) {
                                tvPoolAvailability.setText("Common Pool not initialized. Contact HR.");
                                tvPoolAvailability.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                                if (btnSubmit != null) {
                                    btnSubmit.setEnabled(false);
                                }
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        runOnUiThread(() -> {
                            if (tvPoolAvailability != null) {
                                tvPoolAvailability.setText("Error loading pool availability");
                            }
                            Log.e("EMERGENCY_LEAVE", "Error loading pool: ", e);
                        });
                    });
        } catch (Exception e) {
            Log.e("EMERGENCY_LEAVE", "Error in loadPoolAvailability: ", e);
        }
    }

    private void showDatePicker(EditText editText) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        String selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%d",
                                dayOfMonth, month + 1, year);
                        if (editText != null) {
                            editText.setText(selectedDate);
                        }
                    }
                }, year, month, day);
        datePickerDialog.show();
    }

    private int calculateDays(String fromDate, String toDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date from = sdf.parse(fromDate);
            Date to = sdf.parse(toDate);

            long diff = to.getTime() - from.getTime();
            int days = (int) (diff / (1000 * 60 * 60 * 24)) + 1;
            return Math.max(days, 1);
        } catch (Exception e) {
            Log.e("EMERGENCY_LEAVE", "Error calculating days: ", e);
            return 1;
        }
    }

    private void submitEmergencyRequest() {
        if (etFromDate == null || etToDate == null || etReason == null || etEmergencyDetails == null ||
                spLeaveType == null || spLeaveType.getSelectedItem() == null) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String fromDate = etFromDate.getText().toString().trim();
        String toDate = etToDate.getText().toString().trim();
        String reason = etReason.getText().toString().trim();
        String emergencyDetails = etEmergencyDetails.getText().toString().trim();
        String leaveType = spLeaveType.getSelectedItem().toString();

        if (TextUtils.isEmpty(fromDate) || TextUtils.isEmpty(toDate) ||
                TextUtils.isEmpty(reason) || TextUtils.isEmpty(emergencyDetails)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int numberOfDays = calculateDays(fromDate, toDate);
        if (numberOfDays <= 0) {
            Toast.makeText(this, "Invalid date range", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (btnSubmit != null) {
            btnSubmit.setEnabled(false);
        }

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String employeeName = documentSnapshot.getString("name");
                        String department = documentSnapshot.getString("department");
                        String designation = documentSnapshot.getString("designation");

                        if (employeeName == null || department == null || designation == null) {
                            Toast.makeText(this, "Error: Incomplete user profile", Toast.LENGTH_SHORT).show();
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                            if (btnSubmit != null) {
                                btnSubmit.setEnabled(true);
                            }
                            return;
                        }

                        findManagerAndCreateRequest(userId, employeeName, department, designation,
                                fromDate, toDate, reason, emergencyDetails, leaveType, numberOfDays);
                    } else {
                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        if (btnSubmit != null) {
                            btnSubmit.setEnabled(true);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    if (btnSubmit != null) {
                        btnSubmit.setEnabled(true);
                    }
                });
    }

    private void findManagerAndCreateRequest(String userId, String employeeName, String department,
                                             String designation, String fromDate, String toDate,
                                             String reason, String emergencyDetails, String leaveType,
                                             int numberOfDays) {

        Log.d("EMERGENCY_LEAVE", "Looking for manager in department: " + department);

        // Find manager in the same department - LIMIT TO 1
        db.collection("users")
                .whereEqualTo("department", department)
                .whereEqualTo("role", "manager")
                .limit(1) // CRITICAL: Only get 1 manager
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    String managerId;
                    String managerName;

                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        managerId = document.getId();
                        managerName = document.getString("name");
                        Log.d("EMERGENCY_LEAVE", "Found manager: " + managerName + " (ID: " + managerId + ")");
                    } else {
                        managerId = null;
                        managerName = null;
                        Log.d("EMERGENCY_LEAVE", "No manager found in department: " + department);
                    }

                    // Create emergency request
                    Map<String, Object> emergencyRequest = new HashMap<>();
                    emergencyRequest.put("employeeId", userId);
                    emergencyRequest.put("employeeName", employeeName);
                    emergencyRequest.put("department", department);
                    emergencyRequest.put("designation", designation);
                    emergencyRequest.put("fromDate", fromDate);
                    emergencyRequest.put("toDate", toDate);
                    emergencyRequest.put("reason", reason);
                    emergencyRequest.put("emergencyDetails", emergencyDetails);
                    emergencyRequest.put("leaveType", leaveType);
                    emergencyRequest.put("numberOfDays", numberOfDays);
                    emergencyRequest.put("requestDate", new Date());
                    emergencyRequest.put("poolType", leaveType.equals("Casual Leave") ? "casual" : "sick");

                    // Set manager info
                    if (managerId != null) {
                        emergencyRequest.put("status", "pending_manager"); // Goes to manager first
                        emergencyRequest.put("managerId", managerId);
                        emergencyRequest.put("managerName", managerName);
                        Log.d("EMERGENCY_LEAVE", "Setting status to: pending_manager for manager: " + managerName);
                    } else {
                        // If no manager, go directly to HR
                        emergencyRequest.put("status", "pending");
                        emergencyRequest.put("managerId", null);
                        emergencyRequest.put("managerName", null);
                        Log.d("EMERGENCY_LEAVE", "No manager found, setting status to: pending");
                    }

                    // HR fields (will be filled later)
                    emergencyRequest.put("hrId", null);
                    emergencyRequest.put("hrName", null);
                    emergencyRequest.put("actionDate", null);

                    // Save to Firestore
                    db.collection("emergencyRequests")
                            .add(emergencyRequest)
                            .addOnSuccessListener(documentReference -> {
                                String requestId = documentReference.getId();
                                Log.d("EMERGENCY_LEAVE", "Emergency request created with ID: " + requestId);

                                // NOTIFY ONLY ONE PERSON - Manager OR HR, NOT BOTH
                                if (managerId != null) {
                                    // Notify ONLY Manager (not HR)
                                    notifyManager(managerId, employeeName, department, leaveType,
                                            numberOfDays, requestId);
                                    Log.d("EMERGENCY_LEAVE", "Notification sent to MANAGER only");
                                } else {
                                    // Notify ONLY HR (no manager found)
                                    notifyHR(employeeName, department, leaveType, numberOfDays, requestId);
                                    Log.d("EMERGENCY_LEAVE", "Notification sent to HR only (no manager)");
                                }

                                // Show success message
                                String message = "✅ Emergency Leave Request Submitted!\n\n";
                                if (managerId != null) {
                                    message += "Approval Flow: Employee → Manager → HR\n";
                                    message += "Sent to: " + managerName + " (Manager)";
                                } else {
                                    message += "Approval Flow: Employee → HR (No manager in department)\n";
                                    message += "Sent directly to HR";
                                }

                                Toast.makeText(EmergencyLeaveActivity.this, message, Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to submit: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e("EMERGENCY_LEAVE", "Error creating emergency request: ", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to find manager: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("EMERGENCY_LEAVE", "Error finding manager: ", e);
                });
    }

    private void notifyManager(String managerId, String employeeName, String department,
                               String leaveType, int numberOfDays, String requestId) {
        // Firestore notification - ONLY ONE to manager
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", managerId);
        notification.put("title", "Emergency Leave Request");
        notification.put("message", employeeName + " (" + department + ") " +
                "has requested " + numberOfDays + " days emergency " + leaveType);
        notification.put("type", "emergency_request");
        notification.put("emergencyRequestId", requestId);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);

        db.collection("notifications").add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d("EMERGENCY_LEAVE", "ONE Manager notification sent: " + managerId);
                })
                .addOnFailureListener(e -> {
                    Log.e("EMERGENCY_LEAVE", "Failed to send manager notification: ", e);
                });

        // Add ONE system notification
        NotificationHelper notificationHelper = new NotificationHelper(this);
        notificationHelper.showEmergencyNotification(
                employeeName,
                department,
                leaveType,
                numberOfDays,
                requestId
        );
    }

    private void notifyHR(String employeeName, String department,
                          String leaveType, int numberOfDays, String requestId) {
        // Find only ONE HR to notify
        db.collection("users")
                .whereEqualTo("role", "hr admin")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        String hrUserId = document.getId();
                        String hrName = document.getString("name");

                        // Create ONE notification
                        Map<String, Object> notification = new HashMap<>();
                        notification.put("userId", hrUserId);
                        notification.put("title", "Emergency Leave Request");
                        notification.put("message", employeeName + " (" + department + ") " +
                                "has requested " + numberOfDays + " days emergency " + leaveType);
                        notification.put("type", "emergency_request_hr");
                        notification.put("emergencyRequestId", requestId);
                        notification.put("timestamp", System.currentTimeMillis());
                        notification.put("read", false);

                        db.collection("notifications").add(notification)
                                .addOnSuccessListener(documentReference -> {
                                    Log.d("EMERGENCY_LEAVE", "ONE HR notification sent to: " + hrName);

                                    // Create ONE system notification
                                    NotificationHelper notificationHelper = new NotificationHelper(EmergencyLeaveActivity.this);
                                    notificationHelper.showEmergencyNotification(
                                            employeeName,
                                            department,
                                            leaveType,
                                            numberOfDays,
                                            requestId
                                    );
                                });
                    } else {
                        // If no HR admin found, try to find any HR
                        db.collection("users")
                                .whereEqualTo("role", "hr")
                                .limit(1)
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots2 -> {
                                    if (!queryDocumentSnapshots2.isEmpty()) {
                                        DocumentSnapshot document = queryDocumentSnapshots2.getDocuments().get(0);
                                        String hrUserId = document.getId();
                                        String hrName = document.getString("name");

                                        Map<String, Object> notification = new HashMap<>();
                                        notification.put("userId", hrUserId);
                                        notification.put("title", "Emergency Leave Request");
                                        notification.put("message", employeeName + " (" + department + ") " +
                                                "has requested " + numberOfDays + " days emergency " + leaveType);
                                        notification.put("type", "emergency_request_hr");
                                        notification.put("emergencyRequestId", requestId);
                                        notification.put("timestamp", System.currentTimeMillis());
                                        notification.put("read", false);

                                        db.collection("notifications").add(notification)
                                                .addOnSuccessListener(documentReference -> {
                                                    Log.d("EMERGENCY_LEAVE", "ONE HR notification sent to: " + hrName);

                                                    NotificationHelper notificationHelper = new NotificationHelper(EmergencyLeaveActivity.this);
                                                    notificationHelper.showEmergencyNotification(
                                                            employeeName,
                                                            department,
                                                            leaveType,
                                                            numberOfDays,
                                                            requestId
                                                    );
                                                });
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("EMERGENCY_LEAVE", "Failed to send HR notifications: ", e);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}