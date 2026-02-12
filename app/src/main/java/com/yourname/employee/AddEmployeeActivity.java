package com.yourname.employee;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddEmployeeActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private Spinner spinnerDepartment, spinnerDesignation;
    private Button btnAddEmployee;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String[] departments = {
            "Select Department", "Information Technology", "Human Resources",
            "Sales", "Marketing", "Finance", "Operations", "Production",
            "Research & Development", "Customer Support", "Administration",
            "Quality Assurance", "Supply Chain", "Legal", "Training & Development"
    };

    private String[] designations = {
            "Select Designation",
            // IT Department
            "Software Engineer", "Senior Software Engineer", "Technical Lead",
            "Project Manager", "Systems Analyst", "Database Administrator",
            "Network Engineer", "DevOps Engineer", "QA Engineer", "UI/UX Designer",

            // HR Department
            "HR Executive", "HR Manager", "Recruitment Specialist",
            "Training Coordinator", "Compensation Analyst",

            // Sales & Marketing
            "Sales Executive", "Sales Manager", "Marketing Executive",
            "Marketing Manager", "Business Development Manager",

            // Finance
            "Accountant", "Financial Analyst", "Finance Manager",
            "Audit Executive",

            // Operations
            "Operations Executive", "Operations Manager", "Logistics Coordinator",

            // General
            "Executive Assistant", "Office Administrator", "Team Lead",
            "Manager", "Director", "Vice President", "CEO"
    };

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_employee);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        spinnerDepartment = findViewById(R.id.spinnerDepartment);
        spinnerDesignation = findViewById(R.id.spinnerDesignation);
        btnAddEmployee = findViewById(R.id.btnAddEmployee);
        progressBar = findViewById(R.id.progressBar);

        // Setup department spinner
        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, departments
        );
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(deptAdapter);

        // Setup designation spinner
        ArrayAdapter<String> desigAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, designations
        );
        desigAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDesignation.setAdapter(desigAdapter);

        btnAddEmployee.setOnClickListener(v -> addEmployee());
    }

    private void addEmployee() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String department = spinnerDepartment.getSelectedItem().toString();
        String designation = spinnerDesignation.getSelectedItem().toString();

        // ---------- VALIDATION ----------
        if (TextUtils.isEmpty(name)) {
            etName.setError("Name required");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email required");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Valid email required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password required");
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Minimum 6 characters");
            return;
        }

        if (department.equals("Select Department")) {
            Toast.makeText(this, "Select department", Toast.LENGTH_SHORT).show();
            return;
        }

        if (designation.equals("Select Designation")) {
            Toast.makeText(this, "Select designation", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnAddEmployee.setEnabled(false);

        // ---------- FIREBASE AUTH ----------
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        progressBar.setVisibility(View.GONE);
                        btnAddEmployee.setEnabled(true);
                        Toast.makeText(this,
                                "Failed to add employee: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    String userId = mAuth.getCurrentUser().getUid();

                    Map<String, Object> user = new HashMap<>();
                    user.put("name", name);
                    user.put("email", email);
                    user.put("role", "employee"); // Always employee for HR-added users
                    user.put("department", department);
                    user.put("designation", designation);

                    // Set salary based on designation
                    double basicSalary = getSalaryForDesignation(designation);
                    user.put("basicSalary", basicSalary);

                    // Set default leave balance with LOP initialized to 0
                    Map<String, Object> leaveBalance = new HashMap<>();
                    leaveBalance.put("casual", 12);
                    leaveBalance.put("sick", 10);
                    leaveBalance.put("lop", 0);  // Initialize LOP to 0
                    user.put("leaveBalance", leaveBalance);

                    // Initialize LOP days for current month
                    user.put("lopDaysThisMonth", 0);

                    // ---------- FIRESTORE SAVE ----------
                    db.collection("users").document(userId)
                            .set(user)
                            .addOnCompleteListener(saveTask -> {
                                progressBar.setVisibility(View.GONE);
                                btnAddEmployee.setEnabled(true);

                                if (!saveTask.isSuccessful()) {
                                    Toast.makeText(this,
                                            "Failed saving employee data: " + saveTask.getException().getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                Toast.makeText(this,
                                        "Employee added successfully!",
                                        Toast.LENGTH_LONG).show();

                                // Clear form
                                etName.setText("");
                                etEmail.setText("");
                                etPassword.setText("");
                                spinnerDepartment.setSelection(0);
                                spinnerDesignation.setSelection(0);
                            });
                });
    }

    // Method to get salary based on designation
    private double getSalaryForDesignation(String designation) {
        switch (designation) {
            case "Software Engineer":
            case "HR Executive":
            case "Sales Executive":
            case "Marketing Executive":
            case "Accountant":
            case "Operations Executive":
                return 30000.0;

            case "Senior Software Engineer":
            case "HR Manager":
            case "Sales Manager":
            case "Marketing Manager":
            case "Finance Manager":
            case "Operations Manager":
                return 50000.0;

            case "Technical Lead":
            case "Project Manager":
            case "Business Development Manager":
                return 70000.0;

            case "Team Lead":
            case "Manager":
                return 60000.0;

            case "Director":
                return 100000.0;

            case "Vice President":
                return 150000.0;

            case "CEO":
                return 250000.0;

            default:
                return 25000.0; // Default salary for other designations
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up if needed
    }
}