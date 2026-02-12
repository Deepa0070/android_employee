package com.yourname.employee;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private RadioGroup rgRole;
    private RadioButton rbEmployee, rbHR, rbTeamLeader, rbManager;
    private Spinner spinnerDepartment, spinnerDesignation;
    private Button btnRegister;
    private TextView tvLogin;
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
            "Team Lead", "Scrum Master",

            // HR Department
            "HR Executive", "HR Manager", "Recruitment Specialist",
            "Training Coordinator", "Compensation Analyst", "HR Generalist",

            // Sales & Marketing
            "Sales Executive", "Sales Manager", "Marketing Executive",
            "Marketing Manager", "Business Development Manager",

            // Finance
            "Accountant", "Financial Analyst", "Finance Manager",
            "Audit Executive", "Chief Financial Officer",

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
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        rgRole = findViewById(R.id.rgRole);
        rbEmployee = findViewById(R.id.rbEmployee);
        rbHR = findViewById(R.id.rbHR);
        rbTeamLeader = findViewById(R.id.rbTeamLeader);
        rbManager = findViewById(R.id.rbManager);

        spinnerDepartment = findViewById(R.id.spinnerDepartment);
        spinnerDesignation = findViewById(R.id.spinnerDesignation);

        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

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

        // Default role
        rbEmployee.setChecked(true);

        btnRegister.setOnClickListener(v -> registerUser());

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        int selectedRoleId = rgRole.getCheckedRadioButtonId();
        RadioButton selectedRole = findViewById(selectedRoleId);

        // Get role text and convert to lowercase
        String roleText = selectedRole.getText().toString().trim().toLowerCase();

        // Determine role based on selection
        String role;
        boolean isTeamLeader;

        if (roleText.contains("hr") || roleText.contains("admin")) {
            isTeamLeader = false;
            role = "hr admin";
        } else if (roleText.contains("manager")) {
            isTeamLeader = false;
            role = "manager";
        } else if (roleText.contains("team")) {
            role = "team_leader";
            isTeamLeader = true;
        } else {
            isTeamLeader = false;
            role = "employee";
        }

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

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Password mismatch");
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

        // Validate role-specific requirements
        if ((role.equals("team_leader") || role.equals("manager")) &&
                department.equals("Select Department")) {
            Toast.makeText(this, "Team leaders and managers must have a department", Toast.LENGTH_LONG).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        // ---------- FIREBASE AUTH ----------
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        progressBar.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        Toast.makeText(this,
                                "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    String userId = mAuth.getCurrentUser().getUid();

                    Map<String, Object> user = new HashMap<>();
                    user.put("name", name);
                    user.put("email", email);
                    user.put("role", role);
                    user.put("department", department);
                    user.put("designation", designation);
                    user.put("isTeamLeader", isTeamLeader);

                    // Set salary based on designation and role
                    double basicSalary = getSalaryForDesignation(designation, role);
                    user.put("basicSalary", basicSalary);

                    // Set default leave balance with LOP initialized to 0
                    Map<String, Object> leaveBalance = new HashMap<>();
                    leaveBalance.put("casual", 12);
                    leaveBalance.put("sick", 12);
                    leaveBalance.put("lop", 0);  // Initialize LOP to 0
                    user.put("leaveBalance", leaveBalance);

                    // Initialize LOP days for current month
                    user.put("lopDaysThisMonth", 0);

                    // Add created timestamp
                    user.put("createdAt", System.currentTimeMillis());

                    // ---------- FIRESTORE SAVE ----------
                    db.collection("users").document(userId)
                            .set(user)
                            .addOnCompleteListener(saveTask -> {
                                progressBar.setVisibility(View.GONE);
                                btnRegister.setEnabled(true);

                                if (!saveTask.isSuccessful()) {
                                    Toast.makeText(this,
                                            "Failed saving user data: " + saveTask.getException().getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                Toast.makeText(this,
                                        "Registration successful! Role: " + role,
                                        Toast.LENGTH_LONG).show();

                                // ---------- REDIRECT BASED ON ROLE ----------
                                Intent intent;

                                if (role.equals("hr admin")) {
                                    // Show confirmation before HR access
                                    Toast.makeText(this,
                                            "HR Admin account created successfully",
                                            Toast.LENGTH_LONG).show();
                                    intent = new Intent(RegisterActivity.this, HrDashboardActivity.class);
                                } else if (role.equals("manager") || role.equals("team_leader")) {
                                    // For managers and team leaders, show special message
                                    Toast.makeText(this,
                                            role + " account created. Please wait for HR approval.",
                                            Toast.LENGTH_LONG).show();
                                    intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                } else {
                                    intent = new Intent(RegisterActivity.this, EmployeeDashboardActivity.class);
                                }

                                // Clear activity stack
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            });
                });
    }

    // Method to get salary based on designation and role
    private double getSalaryForDesignation(String designation, String role) {
        double baseSalary = 25000.0; // Default

        // Adjust based on role
        switch (role) {
            case "team_leader":
                baseSalary = 45000.0;
                break;
            case "manager":
                baseSalary = 60000.0;
                break;
            case "hr admin":
                baseSalary = 50000.0;
                break;
        }

        // Further adjust based on designation
        switch (designation) {
            case "Software Engineer":
            case "HR Executive":
            case "Sales Executive":
            case "Marketing Executive":
            case "Accountant":
            case "Operations Executive":
                baseSalary = Math.max(baseSalary, 30000.0);
                break;

            case "Senior Software Engineer":
            case "HR Manager":
            case "Sales Manager":
            case "Marketing Manager":
            case "Finance Manager":
            case "Operations Manager":
                baseSalary = Math.max(baseSalary, 50000.0);
                break;

            case "Technical Lead":
            case "Project Manager":
            case "Business Development Manager":
                baseSalary = Math.max(baseSalary, 70000.0);
                break;

            case "Team Lead":
            case "Manager":
                baseSalary = Math.max(baseSalary, 60000.0);
                break;

            case "Director":
                baseSalary = Math.max(baseSalary, 100000.0);
                break;

            case "Vice President":
                baseSalary = Math.max(baseSalary, 150000.0);
                break;

            case "CEO":
                baseSalary = Math.max(baseSalary, 250000.0);
                break;
        }

        return baseSalary;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up if needed
    }
}