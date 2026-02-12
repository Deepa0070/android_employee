package com.yourname.employee;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ApplyLeaveActivity extends AppCompatActivity {

    private EditText etFromDate, etToDate, etReason;
    private Spinner spLeaveType;
    private Button btnApply;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_leave);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        etFromDate = findViewById(R.id.etFromDate);
        etToDate = findViewById(R.id.etToDate);
        etReason = findViewById(R.id.etReason);
        spLeaveType = findViewById(R.id.spLeaveType);
        btnApply = findViewById(R.id.btnApply);

        // Setup leave type spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.leave_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLeaveType.setAdapter(adapter);

        // Set click listeners for date pickers
        etFromDate.setOnClickListener(v -> showDatePicker(etFromDate));
        etToDate.setOnClickListener(v -> showDatePicker(etToDate));

        // Apply button click
        btnApply.setOnClickListener(v -> applyLeave());
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
                        String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                        editText.setText(selectedDate);
                    }
                }, year, month, day);
        datePickerDialog.show();
    }

    // Method to calculate number of days between two dates
    private int calculateNumberOfDays(String fromDateStr, String toDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date fromDate = sdf.parse(fromDateStr);
            Date toDate = sdf.parse(toDateStr);

            long difference = toDate.getTime() - fromDate.getTime();
            int days = (int) (difference / (1000 * 60 * 60 * 24)) + 1;
            return days > 0 ? days : 1;
        } catch (ParseException e) {
            Log.e("ApplyLeave", "Error calculating days: " + e.getMessage());
            return 1;
        }
    }

    private void applyLeave() {
        String fromDate = etFromDate.getText().toString().trim();
        String toDate = etToDate.getText().toString().trim();
        String reason = etReason.getText().toString().trim();
        String leaveType = spLeaveType.getSelectedItem().toString();

        if (TextUtils.isEmpty(fromDate) || TextUtils.isEmpty(toDate) || TextUtils.isEmpty(reason)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate number of days
        int numberOfDays = calculateNumberOfDays(fromDate, toDate);

        // Validate dates
        if (numberOfDays <= 0) {
            Toast.makeText(this, "To date must be after from date", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current user ID
        String userId = mAuth.getCurrentUser().getUid();

        // First get employee details
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String employeeName = documentSnapshot.getString("name");
                        String department = documentSnapshot.getString("department");
                        String designation = documentSnapshot.getString("designation");

                        // Check if it's LOP leave
                        boolean isLOP = leaveType.equals("Loss of Pay (LOP)");

                        // For casual and sick leaves, check monthly limit
                        if (leaveType.equals("Casual Leave") || leaveType.equals("Sick Leave")) {
                            checkMonthlyLeaveLimit(userId, leaveType, fromDate, numberOfDays,
                                    new MonthlyLimitCheckListener() {
                                        @Override
                                        public void onLimitChecked(boolean limitExceeded, String monthName, int alreadyTaken) {
                                            if (limitExceeded) {
                                                String message = String.format(
                                                        "❌ Monthly Limit Exceeded\n" +
                                                                "You can only take 1 %s per month.\n" +
                                                                "Already taken: %d in %s",
                                                        leaveType, alreadyTaken, monthName
                                                );
                                                Toast.makeText(ApplyLeaveActivity.this, message, Toast.LENGTH_LONG).show();
                                                return;
                                            }

                                            // Check leave balance
                                            checkLeaveBalance(userId, leaveType, numberOfDays,
                                                    new BalanceCheckListener() {
                                                        @Override
                                                        public void onBalanceChecked(boolean sufficient, int availableBalance) {
                                                            if (!sufficient) {
                                                                String message = String.format(
                                                                        "Not enough %s balance\n" +
                                                                                "Required: %d days\n" +
                                                                                "Available: %d days",
                                                                        leaveType, numberOfDays, availableBalance
                                                                );
                                                                Toast.makeText(ApplyLeaveActivity.this, message, Toast.LENGTH_LONG).show();
                                                                return;
                                                            }

                                                            // All checks passed, apply leave
                                                            saveLeaveRequest(userId, employeeName, department, designation,
                                                                    fromDate, toDate, reason, leaveType, numberOfDays, isLOP);
                                                        }
                                                    });
                                        }
                                    });
                        } else {
                            // For LOP leaves, just apply
                            saveLeaveRequest(userId, employeeName, department, designation,
                                    fromDate, toDate, reason, leaveType, numberOfDays, isLOP);
                        }
                    } else {
                        Toast.makeText(ApplyLeaveActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ApplyLeaveActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                });
    }

    // Check monthly leave limit (1 casual leave and 1 sick leave per month)
    private void checkMonthlyLeaveLimit(String userId, String leaveType, String fromDate,
                                        int requestedDays, MonthlyLimitCheckListener listener) {
        try {
            // Parse the fromDate to get month and year
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = sdf.parse(fromDate);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            int targetMonth = cal.get(Calendar.MONTH) + 1; // Month is 0-based, so +1
            int targetYear = cal.get(Calendar.YEAR);
            String monthName = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date);

            // Query for existing leaves of same type in same month
            // We check both approved AND pending leaves to prevent multiple applications
            db.collection("leaveRequests")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("leaveType", leaveType)
                    .whereIn("status", Arrays.asList("approved", "pending"))
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                int leavesInSameMonth = 0;

                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    try {
                                        String existingFromDate = document.getString("fromDate");
                                        if (existingFromDate != null) {
                                            Date existingDate = sdf.parse(existingFromDate);
                                            Calendar existingCal = Calendar.getInstance();
                                            existingCal.setTime(existingDate);

                                            int existingMonth = existingCal.get(Calendar.MONTH) + 1;
                                            int existingYear = existingCal.get(Calendar.YEAR);

                                            // Check if leave is in same month and year
                                            if (existingMonth == targetMonth && existingYear == targetYear) {
                                                leavesInSameMonth++;
                                            }
                                        }
                                    } catch (ParseException e) {
                                        Log.e("ApplyLeave", "Error parsing existing date: " + e.getMessage());
                                    }
                                }

                                // Check if limit exceeded (max 1 per month)
                                boolean limitExceeded = (leavesInSameMonth >= 1);
                                listener.onLimitChecked(limitExceeded, monthName, leavesInSameMonth);

                            } else {
                                Log.e("ApplyLeave", "Error checking monthly limit: " + task.getException());
                                listener.onLimitChecked(false, monthName, 0); // Allow if error
                            }
                        }
                    });

        } catch (ParseException e) {
            Log.e("ApplyLeave", "Error parsing date: " + e.getMessage());
            listener.onLimitChecked(false, "Unknown Month", 0);
        }
    }

    // Check leave balance - UPDATED: Changed sick leave default from 10 to 12
    private void checkLeaveBalance(String userId, String leaveType, int requestedDays,
                                   BalanceCheckListener listener) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> leaveBalance = (Map<String, Object>) documentSnapshot.get("leaveBalance");
                        if (leaveBalance != null) {
                            long casualBalance = ((Long) leaveBalance.getOrDefault("casual", 12L));
                            long sickBalance = ((Long) leaveBalance.getOrDefault("sick", 12L)); // Changed from 10L to 12L

                            if (leaveType.equals("Casual Leave")) {
                                listener.onBalanceChecked(casualBalance >= requestedDays, (int) casualBalance);
                            } else if (leaveType.equals("Sick Leave")) {
                                listener.onBalanceChecked(sickBalance >= requestedDays, (int) sickBalance);
                            }
                        } else {
                            listener.onBalanceChecked(false, 0);
                        }
                    } else {
                        listener.onBalanceChecked(false, 0);
                    }
                })
                .addOnFailureListener(e -> {
                    listener.onBalanceChecked(false, 0);
                });
    }

    // Save leave request to Firestore
    private void saveLeaveRequest(String userId, String employeeName, String department,
                                  String designation, String fromDate, String toDate,
                                  String reason, String leaveType, int numberOfDays, boolean isLOP) {

        // Create a leave request object
        Map<String, Object> leaveRequest = new HashMap<>();
        leaveRequest.put("userId", userId);
        leaveRequest.put("employeeName", employeeName);
        leaveRequest.put("department", department);
        leaveRequest.put("designation", designation);
        leaveRequest.put("fromDate", fromDate);
        leaveRequest.put("toDate", toDate);
        leaveRequest.put("reason", reason);
        leaveRequest.put("leaveType", leaveType);
        leaveRequest.put("numberOfDays", numberOfDays);
        leaveRequest.put("days", numberOfDays);
        leaveRequest.put("status", "pending"); // Overall status
        leaveRequest.put("managerStatus", "pending"); // Waiting for manager
        leaveRequest.put("hrStatus", "pending"); // Waiting for HR
        leaveRequest.put("appliedDate", Calendar.getInstance().getTime());
        leaveRequest.put("isLOP", isLOP);

        // Add to Firestore
        db.collection("leaveRequests")
                .add(leaveRequest)
                .addOnSuccessListener(documentReference -> {
                    // Notify Manager about new leave application
                    notifyManagerAboutNewLeave(userId, employeeName, department, designation,
                            fromDate, toDate, reason, leaveType, numberOfDays,
                            documentReference.getId());

                    String message = String.format(
                            "✅ Leave Application Submitted!\n" +
                                    "Type: %s\n" +
                                    "Days: %d\n" +
                                    "Status: Pending Manager Approval",
                            leaveType, numberOfDays
                    );
                    Toast.makeText(ApplyLeaveActivity.this, message, Toast.LENGTH_LONG).show();

                    // Clear form
                    etFromDate.setText("");
                    etToDate.setText("");
                    etReason.setText("");
                    spLeaveType.setSelection(0);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ApplyLeaveActivity.this,
                            "Failed to apply leave: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void notifyManagerAboutNewLeave(String userId, String employeeName, String department,
                                            String designation, String fromDate, String toDate,
                                            String reason, String leaveType, int numberOfDays,
                                            String leaveId) {
        // Find only ONE manager in the same department (limit to 1)
        db.collection("users")
                .whereEqualTo("department", department)
                .whereEqualTo("role", "manager")
                .limit(1) // Limit to only one manager
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        String managerUserId = document.getId();
                        String managerName = document.getString("name");

                        // Create notification for manager in Firestore
                        Map<String, Object> notification = new HashMap<>();
                        notification.put("userId", managerUserId);
                        notification.put("title", "New Leave Application");
                        notification.put("message", employeeName + " (" + designation + ") " +
                                "has applied for " + numberOfDays + " days " + leaveType + ". " +
                                "Dates: " + fromDate + " to " + toDate);
                        notification.put("type", "new_leave_application");
                        notification.put("leaveId", leaveId);
                        notification.put("fromUserId", userId);
                        notification.put("fromUserName", employeeName);
                        notification.put("timestamp", System.currentTimeMillis());
                        notification.put("read", false);

                        db.collection("notifications")
                                .add(notification)
                                .addOnSuccessListener(documentReference -> {
                                    Log.d("APPLY_LEAVE", "Manager notified in Firestore: " + managerName);

                                    // SHOW SYSTEM NOTIFICATION
                                    NotificationHelper notificationHelper = new NotificationHelper(ApplyLeaveActivity.this);
                                    notificationHelper.showNotification(
                                            "New Leave Application",
                                            employeeName + " has applied for " + numberOfDays + " days " + leaveType,
                                            "new_leave_application"
                                    );

                                    // Also show a Toast for immediate feedback
                                    Toast.makeText(ApplyLeaveActivity.this,
                                            "Manager " + managerName + " notified ✓",
                                            Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("APPLY_LEAVE", "Failed to save notification to Firestore: " + e.getMessage());
                                });
                    } else {
                        Log.d("APPLY_LEAVE", "No manager found in department: " + department);
                        // If no manager found, notify HR instead
                        notifyHRAboutLeave(employeeName, department, designation, fromDate, toDate,
                                leaveType, numberOfDays, leaveId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("APPLY_LEAVE", "Failed to find manager: " + e.getMessage());
                    Toast.makeText(this, "Failed to notify manager", Toast.LENGTH_SHORT).show();
                });
    }

    // Add this new method to notify HR when no manager is found
    private void notifyHRAboutLeave(String employeeName, String department, String designation,
                                    String fromDate, String toDate, String leaveType,
                                    int numberOfDays, String leaveId) {
        // Find one HR user
        db.collection("users")
                .whereEqualTo("role", "hr admin")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        String hrUserId = document.getId();

                        Map<String, Object> notification = new HashMap<>();
                        notification.put("userId", hrUserId);
                        notification.put("title", "New Leave Application (No Manager)");
                        notification.put("message", employeeName + " (" + department + ") " +
                                "applied for " + numberOfDays + " days " + leaveType +
                                ". No manager in department.");
                        notification.put("type", "new_leave_application");
                        notification.put("leaveId", leaveId);
                        notification.put("timestamp", System.currentTimeMillis());
                        notification.put("read", false);

                        db.collection("notifications").add(notification);
                    }
                });
    }

    // NEW METHOD: Check if employee can apply for emergency leave based on leaves taken this month
    private void checkIfCanApplyEmergency(String userId, String fromDate, EmergencyCheckListener listener) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = sdf.parse(fromDate);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            int targetMonth = cal.get(Calendar.MONTH) + 1;
            int targetYear = cal.get(Calendar.YEAR);

            // Check for casual and sick leaves in the same month
            db.collection("leaveRequests")
                    .whereEqualTo("userId", userId)
                    .whereIn("leaveType", Arrays.asList("Casual Leave", "Sick Leave"))
                    .whereIn("status", Arrays.asList("approved", "pending"))
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        int casualCount = 0;
                        int sickCount = 0;

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String leaveType = document.getString("leaveType");
                            String existingFromDate = document.getString("fromDate");

                            if (existingFromDate != null) {
                                try {
                                    Date existingDate = sdf.parse(existingFromDate);
                                    Calendar existingCal = Calendar.getInstance();
                                    existingCal.setTime(existingDate);

                                    int existingMonth = existingCal.get(Calendar.MONTH) + 1;
                                    int existingYear = existingCal.get(Calendar.YEAR);

                                    if (existingMonth == targetMonth && existingYear == targetYear) {
                                        if (leaveType.equals("Casual Leave")) {
                                            casualCount++;
                                        } else if (leaveType.equals("Sick Leave")) {
                                            sickCount++;
                                        }
                                    }
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        listener.onCheckComplete(casualCount, sickCount);
                    });

        } catch (ParseException e) {
            Log.e("ApplyLeave", "Error parsing date: " + e.getMessage());
            listener.onCheckComplete(0, 0);
        }
    }

    // Interfaces for callbacks
    interface MonthlyLimitCheckListener {
        void onLimitChecked(boolean limitExceeded, String monthName, int alreadyTaken);
    }

    interface BalanceCheckListener {
        void onBalanceChecked(boolean sufficient, int availableBalance);
    }

    // NEW INTERFACE for emergency check
    interface EmergencyCheckListener {
        void onCheckComplete(int casualCount, int sickCount);
    }
}