package com.yourname.employee;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // If already logged in, check role and redirect
        if (mAuth.getCurrentUser() != null) {
            checkUserRoleAndRedirect();
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        progressBar = findViewById(R.id.progressBar);

        setUnderlineText();

        btnLogin.setOnClickListener(v -> loginUser());

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });
    }

    private void setUnderlineText() {
        SpannableString registerText = new SpannableString("Don't have account? Register");
        registerText.setSpan(new UnderlineSpan(), 0, registerText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvRegister.setText(registerText);

        SpannableString forgotText = new SpannableString("Forgot Password?");
        forgotText.setSpan(new UnderlineSpan(), 0, forgotText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvForgotPassword.setText(forgotText);
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserRoleAndRedirect();
                        }
                    } else {
                        Toast.makeText(this,
                                "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkUserRoleAndRedirect() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No user found", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot document = task.getResult();
                        String role = document.getString("role");
                        String name = document.getString("name");

                        // Show role in toast for debugging
                        Toast.makeText(this, "Welcome " + name + " (" + role + ")", Toast.LENGTH_LONG).show();

                        Intent intent;

                        // Check for all possible HR role values
                        if (role != null && (role.equalsIgnoreCase("hr") ||
                                role.equalsIgnoreCase("hr admin") ||
                                role.equalsIgnoreCase("admin") ||
                                role.equalsIgnoreCase("human resources"))) {
                            // Redirect to HR Dashboard
                            intent = new Intent(LoginActivity.this, HrDashboardActivity.class);
                        }
                        // Check for Manager role
                        else if (role != null && (role.toLowerCase().contains("manager"))) {
                            // Redirect to Manager Dashboard
                            intent = new Intent(LoginActivity.this, ManagerDashboardActivity.class);
                        }
                        // Check for Team Leader role
                        else if (role != null && (role.equalsIgnoreCase("team_leader") ||
                                role.equalsIgnoreCase("team leader") ||
                                role.equalsIgnoreCase("teamleader") ||
                                role.equalsIgnoreCase("Team Lead"))) {
                            // Team Leader goes to Employee Dashboard (view only)
                            intent = new Intent(LoginActivity.this, EmployeeDashboardActivity.class);
                        }
                        else {
                            // Employee goes to Employee Dashboard
                            intent = new Intent(LoginActivity.this, EmployeeDashboardActivity.class);
                        }

                        // Clear back stack and start new activity
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();

                    } else {
                        // Document doesn't exist or error
                        if (task.getException() != null) {
                            Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        // Default to employee dashboard if can't determine role
                        Intent intent = new Intent(LoginActivity.this, EmployeeDashboardActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already logged in
        if (mAuth.getCurrentUser() != null) {
            checkUserRoleAndRedirect();
        }
    }

    // Temporary test method - add a test HR button in your layout
    public void testHrLogin(View view) {
        // Use the HR admin email from your database
        String hrEmail = "kalaiselvam916@gmail.com";
        String hrPassword = "hrpassword123"; // Change to actual password

        etEmail.setText(hrEmail);
        etPassword.setText(hrPassword);
        loginUser();
    }
}