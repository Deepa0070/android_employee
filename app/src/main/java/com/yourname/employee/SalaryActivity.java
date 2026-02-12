package com.yourname.employee;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class SalaryActivity extends AppCompatActivity {

    // Original TextViews
    private TextView tvBasicSalary, tvNetSalary, tvDeductions, tvMonth,
            tvWorkingDays, tvPresentDays, tvLeaveDays, tvLOPDays, tvDailyRate;

    // Additional views for PDF generation
    private TextView tvEmployeeName, tvDesignation, tvLopDeduction;
    private Spinner spinnerMonth, spinnerYear;
    private Button btnDownloadSalarySlip;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Additional fields for PDF
    private String userId;
    private String employeeName, department, designation;
    private double basicSalaryValue = 0;
    private int totalWorkingDaysValue = 22;
    private int presentDaysValue = 22;
    private int lopDaysValue = 0;
    private int casualLeaveValue = 12;
    private int sickLeaveValue = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_salary);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = mAuth.getCurrentUser().getUid();

        try {
            initializeViews();
            setupMonthYearSpinners();
            loadSalaryData();
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("SalaryActivity", "Error in onCreate: " + e.getMessage());
        }
    }

    private void initializeViews() {
        try {
            // Original views
            tvBasicSalary = findViewById(R.id.tvBasicSalary);
            tvNetSalary = findViewById(R.id.tvNetSalary);
            tvDeductions = findViewById(R.id.tvDeductions);
            tvMonth = findViewById(R.id.tvMonth);
            tvWorkingDays = findViewById(R.id.tvWorkingDays);
            tvPresentDays = findViewById(R.id.tvPresentDays);
            tvLeaveDays = findViewById(R.id.tvLeaveDays);
            tvLOPDays = findViewById(R.id.tvLOPDays);
            tvDailyRate = findViewById(R.id.tvDailyRate);

            // Additional views for PDF (add these to your activity_salary.xml if not present)
            tvEmployeeName = findViewById(R.id.tvEmployeeName);
            tvDesignation = findViewById(R.id.tvDesignation);
            tvLopDeduction = findViewById(R.id.tvLopDeduction);
            spinnerMonth = findViewById(R.id.spinnerMonth);
            spinnerYear = findViewById(R.id.spinnerYear);
            btnDownloadSalarySlip = findViewById(R.id.btnDownloadSalarySlip);
            progressBar = findViewById(R.id.progressBar);

            // Set click listener for download button
            if (btnDownloadSalarySlip != null) {
                btnDownloadSalarySlip.setOnClickListener(v -> generateSalarySlipPdf());
            }

            Log.d("SalaryActivity", "All views initialized successfully");
        } catch (Exception e) {
            Toast.makeText(this, "Layout error: Check XML IDs", Toast.LENGTH_LONG).show();
            Log.e("SalaryActivity", "Error initializing views: " + e.getMessage());
            // Don't throw, continue with available views
        }
    }

    private void setupMonthYearSpinners() {
        if (spinnerMonth == null || spinnerYear == null) {
            Log.d("SalaryActivity", "Month/Year spinners not available in layout");
            return;
        }

        try {
            // Months array
            String[] months = new String[]{"January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"};
            ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, months);
            monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerMonth.setAdapter(monthAdapter);

            // Years: current year and previous 2 years
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            String[] years = new String[]{String.valueOf(currentYear - 2),
                    String.valueOf(currentYear - 1), String.valueOf(currentYear)};
            ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, years);
            yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerYear.setAdapter(yearAdapter);

            // Set default to current month & year
            Calendar cal = Calendar.getInstance();
            spinnerMonth.setSelection(cal.get(Calendar.MONTH));
            spinnerYear.setSelection(years.length - 1);

            // Listener to recalc when selection changes
            AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    loadLopDaysForSelectedMonth();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) { }
            };
            spinnerMonth.setOnItemSelectedListener(listener);
            spinnerYear.setOnItemSelectedListener(listener);
        } catch (Exception e) {
            Log.e("SalaryActivity", "Error setting up spinners: " + e.getMessage());
        }
    }

    private void loadSalaryData() {
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // Get current month and year
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String currentMonth = sdf.format(new Date());

        try {
            if (tvMonth != null) {
                tvMonth.setText("Salary for " + currentMonth);
            }
        } catch (Exception e) {
            Log.e("SalaryActivity", "Error setting month: " + e.getMessage());
        }

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }

                    if (documentSnapshot.exists()) {
                        try {
                            // Load employee details for PDF
                            employeeName = documentSnapshot.getString("name");
                            department = documentSnapshot.getString("department");
                            designation = documentSnapshot.getString("designation");

                            if (tvEmployeeName != null) {
                                tvEmployeeName.setText(employeeName != null ? employeeName : "N/A");
                            }
                            if (tvDesignation != null) {
                                tvDesignation.setText((designation != null ? designation : "N/A") +
                                        (department != null ? " - " + department : ""));
                            }

                            // Get basic salary
                            double basicSalary = 0.0;
                            if (documentSnapshot.contains("basicSalary")) {
                                Object salaryObj = documentSnapshot.get("basicSalary");
                                if (salaryObj instanceof Double) {
                                    basicSalary = (Double) salaryObj;
                                } else if (salaryObj instanceof Long) {
                                    basicSalary = ((Long) salaryObj).doubleValue();
                                } else if (salaryObj instanceof Integer) {
                                    basicSalary = ((Integer) salaryObj).doubleValue();
                                }
                            }

                            // If no basic salary found, set default based on designation
                            if (basicSalary <= 0) {
                                String designation = documentSnapshot.getString("designation");
                                basicSalary = getDefaultSalaryForDesignation(designation);
                            }

                            basicSalaryValue = basicSalary;

                            // Get LOP days for this month
                            int lopDays = 0;
                            if (documentSnapshot.contains("lopDaysThisMonth")) {
                                Object lopObj = documentSnapshot.get("lopDaysThisMonth");
                                if (lopObj instanceof Long) {
                                    lopDays = ((Long) lopObj).intValue();
                                } else if (lopObj instanceof Integer) {
                                    lopDays = (Integer) lopObj;
                                } else if (lopObj instanceof Double) {
                                    lopDays = ((Double) lopObj).intValue();
                                }
                            }

                            // Fallback to leaveBalance.lop if lopDaysThisMonth doesn't exist
                            if (lopDays == 0) {
                                Map<String, Object> leaveBalance = (Map<String, Object>) documentSnapshot.get("leaveBalance");
                                if (leaveBalance != null && leaveBalance.containsKey("lop")) {
                                    Object lopObj = leaveBalance.get("lop");
                                    if (lopObj instanceof Long) {
                                        lopDays = ((Long) lopObj).intValue();
                                    } else if (lopObj instanceof Integer) {
                                        lopDays = (Integer) lopObj;
                                    } else if (lopObj instanceof Double) {
                                        lopDays = ((Double) lopObj).intValue();
                                    }
                                }
                            }

                            lopDaysValue = lopDays;
                            presentDaysValue = totalWorkingDaysValue - lopDaysValue;

                            // Get leave balances for display
                            if (documentSnapshot.contains("leaveBalance")) {
                                Map<String, Object> leaveBalance = (Map<String, Object>) documentSnapshot.get("leaveBalance");
                                if (leaveBalance != null) {
                                    Object casualObj = leaveBalance.get("casual");
                                    Object sickObj = leaveBalance.get("sick");

                                    if (casualObj instanceof Long) {
                                        casualLeaveValue = ((Long) casualObj).intValue();
                                    } else if (casualObj instanceof Integer) {
                                        casualLeaveValue = (Integer) casualObj;
                                    }

                                    if (sickObj instanceof Long) {
                                        sickLeaveValue = ((Long) sickObj).intValue();
                                    } else if (sickObj instanceof Integer) {
                                        sickLeaveValue = (Integer) sickObj;
                                    }
                                }
                            }

                            // Calculate and display salary
                            calculateAndDisplaySalary(basicSalary, lopDays, casualLeaveValue, sickLeaveValue);

                        } catch (Exception e) {
                            Toast.makeText(this, "Error processing data", Toast.LENGTH_SHORT).show();
                            Log.e("SalaryActivity", "Error in data processing: " + e.getMessage());
                            e.printStackTrace();
                            calculateAndDisplaySalary(25000.0, 0, 12, 10);
                        }

                    } else {
                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                        calculateAndDisplaySalary(25000.0, 0, 12, 10);
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    Toast.makeText(this, "Failed to load salary data", Toast.LENGTH_SHORT).show();
                    Log.e("SalaryActivity", "Firestore error: " + e.getMessage());
                    e.printStackTrace();
                    calculateAndDisplaySalary(25000.0, 0, 12, 10);
                });
    }

    private void loadLopDaysForSelectedMonth() {
        if (spinnerMonth == null || spinnerYear == null || userId == null) {
            return;
        }

        int month = spinnerMonth.getSelectedItemPosition() + 1;
        int year = Integer.parseInt(spinnerYear.getSelectedItem().toString());

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Try to get from lopDaysByMonth map first
                    if (documentSnapshot.contains("lopDaysByMonth")) {
                        try {
                            Map<String, Object> lopMap = (Map<String, Object>) documentSnapshot.get("lopDaysByMonth");
                            String key = year + "-" + month;
                            if (lopMap != null && lopMap.containsKey(key)) {
                                Object val = lopMap.get(key);
                                if (val instanceof Long) lopDaysValue = ((Long) val).intValue();
                                else if (val instanceof Integer) lopDaysValue = (Integer) val;
                                else if (val instanceof Double) lopDaysValue = ((Double) val).intValue();
                            } else {
                                lopDaysValue = 0;
                            }
                        } catch (Exception e) {
                            lopDaysValue = 0;
                        }
                    } else {
                        // Fallback to current month's lopDaysThisMonth
                        Calendar cal = Calendar.getInstance();
                        int currentMonth = cal.get(Calendar.MONTH) + 1;
                        int currentYear = cal.get(Calendar.YEAR);
                        if (month == currentMonth && year == currentYear) {
                            Object lopObj = documentSnapshot.get("lopDaysThisMonth");
                            if (lopObj instanceof Long) lopDaysValue = ((Long) lopObj).intValue();
                            else if (lopObj instanceof Integer) lopDaysValue = (Integer) lopObj;
                            else lopDaysValue = 0;
                        } else {
                            lopDaysValue = 0;
                        }
                    }

                    presentDaysValue = totalWorkingDaysValue - lopDaysValue;

                    // Recalculate with current values
                    calculateAndDisplaySalary(basicSalaryValue, lopDaysValue, casualLeaveValue, sickLeaveValue);
                });
    }

    private void calculateAndDisplaySalary(double basicSalary, int lopDays, int casualLeave, int sickLeave) {
        try {
            int totalWorkingDays = 22; // Standard working days in a month
            double dailyRate = basicSalary / totalWorkingDays;
            double lopDeduction = lopDays * dailyRate;
            double netSalary = basicSalary - lopDeduction;

            // Calculate present days
            int presentDays = totalWorkingDays - lopDays;
            int totalLeaves = casualLeave + sickLeave;

            // Update values for PDF
            basicSalaryValue = basicSalary;
            lopDaysValue = lopDays;
            presentDaysValue = presentDays;

            // Display all values with safety checks
            if (tvBasicSalary != null) tvBasicSalary.setText("₹" + String.format("%,.2f", basicSalary));
            if (tvWorkingDays != null) tvWorkingDays.setText(String.valueOf(totalWorkingDays));
            if (tvPresentDays != null) tvPresentDays.setText(String.valueOf(presentDays));
            if (tvLeaveDays != null) tvLeaveDays.setText(String.valueOf(totalLeaves));
            if (tvLOPDays != null) tvLOPDays.setText(String.valueOf(lopDays));
            if (tvDailyRate != null) tvDailyRate.setText("₹" + String.format("%,.2f", dailyRate) + "/day");
            if (tvLopDeduction != null) tvLopDeduction.setText("₹" + String.format("%,.2f", lopDeduction));

            // Display deductions
            if (tvDeductions != null) tvDeductions.setText("₹" + String.format("%,.2f", lopDeduction));
            if (tvNetSalary != null) tvNetSalary.setText("₹" + String.format("%,.2f", netSalary));

            // Show toast notification if there are deductions
            if (lopDeduction > 0) {
                String toastMessage = String.format(
                        "Salary Details:\n" +
                                "Basic: ₹%,.2f\n" +
                                "Daily Rate: ₹%,.2f\n" +
                                "LOP Days: %d\n" +
                                "Deduction: ₹%,.2f\n" +
                                "Net Salary: ₹%,.2f",
                        basicSalary, dailyRate, lopDays, lopDeduction, netSalary
                );
                Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "No salary deductions this month!", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error calculating salary", Toast.LENGTH_SHORT).show();
            Log.e("SalaryActivity", "Error in calculateAndDisplaySalary: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private double getDefaultSalaryForDesignation(String designation) {
        if (designation == null) return 25000.0;

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
                return 25000.0;
        }
    }

    private void generateSalarySlipPdf() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (btnDownloadSalarySlip != null) {
            btnDownloadSalarySlip.setEnabled(false);
        }

        String month = spinnerMonth != null ? spinnerMonth.getSelectedItem().toString() :
                new SimpleDateFormat("MMMM", Locale.getDefault()).format(new Date());
        String year = spinnerYear != null ? spinnerYear.getSelectedItem().toString() :
                new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());

        String fileName = "SalarySlip_" + (employeeName != null ? employeeName.replace(" ", "_") : "Employee") + "_"
                + month + "_" + year + ".pdf";

        // Create PDF document
        PdfDocument pdfDocument = new PdfDocument();

        // Create a page with A4 size (595 x 842 points)
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        Paint headerPaint = new Paint();
        Paint boldPaint = new Paint();

        // Set colors and styles
        titlePaint.setColor(Color.parseColor("#6200EE"));
        titlePaint.setTextSize(24f);
        titlePaint.setFakeBoldText(true);

        headerPaint.setColor(Color.BLACK);
        headerPaint.setTextSize(16f);
        headerPaint.setFakeBoldText(true);

        boldPaint.setColor(Color.BLACK);
        boldPaint.setTextSize(14f);
        boldPaint.setFakeBoldText(true);

        paint.setColor(Color.BLACK);
        paint.setTextSize(14f);

        int x = 50;
        int y = 80;
        int lineHeight = 35;

        // Draw company header
        canvas.drawText("EMPLOYEE SALARY SLIP", x, y, titlePaint);
        y += lineHeight + 10;

        // Draw month and year
        paint.setTextSize(16f);
        paint.setFakeBoldText(true);
        canvas.drawText(month + " " + year, x, y, paint);
        y += lineHeight + 15;

        // Reset paint
        paint.setFakeBoldText(false);
        paint.setTextSize(14f);

        // Draw employee details
        canvas.drawText("EMPLOYEE DETAILS", x, y, headerPaint);
        y += lineHeight;

        // Draw line
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        canvas.drawLine(x, y - 15, 545, y - 15, paint);
        paint.setStyle(Paint.Style.FILL);

        // Employee details in two columns
        canvas.drawText("Name:", x, y, boldPaint);
        canvas.drawText(employeeName != null ? employeeName : "N/A", x + 150, y, paint);
        y += lineHeight;

        canvas.drawText("Department:", x, y, boldPaint);
        canvas.drawText(department != null ? department : "N/A", x + 150, y, paint);
        y += lineHeight;

        canvas.drawText("Designation:", x, y, boldPaint);
        canvas.drawText(designation != null ? designation : "N/A", x + 150, y, paint);
        y += lineHeight + 20;

        // Salary breakdown header
        canvas.drawText("SALARY BREAKDOWN", x, y, headerPaint);
        y += lineHeight;

        // Draw line
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(x, y - 15, 545, y - 15, paint);
        paint.setStyle(Paint.Style.FILL);

        // Salary details
        double dailyRate = totalWorkingDaysValue > 0 ? basicSalaryValue / totalWorkingDaysValue : 0;
        double lopDeduction = dailyRate * lopDaysValue;
        double netSalary = basicSalaryValue - lopDeduction;

        canvas.drawText("Basic Salary:", x, y, boldPaint);
        canvas.drawText(String.format(Locale.US, "₹%.2f", basicSalaryValue), x + 200, y, paint);
        y += lineHeight;

        canvas.drawText("Total Working Days:", x, y, boldPaint);
        canvas.drawText(String.valueOf(totalWorkingDaysValue), x + 200, y, paint);
        y += lineHeight;

        canvas.drawText("Present Days:", x, y, boldPaint);
        canvas.drawText(String.valueOf(presentDaysValue), x + 200, y, paint);
        y += lineHeight;

        canvas.drawText("LOP Days:", x, y, boldPaint);
        canvas.drawText(String.valueOf(lopDaysValue), x + 200, y, paint);
        y += lineHeight;

        canvas.drawText("Daily Rate:", x, y, boldPaint);
        canvas.drawText(String.format(Locale.US, "₹%.2f", dailyRate), x + 200, y, paint);
        y += lineHeight;

        canvas.drawText("LOP Deduction:", x, y, boldPaint);
        canvas.drawText(String.format(Locale.US, "₹%.2f", lopDeduction), x + 200, y, paint);
        y += lineHeight + 10;

        // Draw separator line
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        canvas.drawLine(x, y - 5, 545, y - 5, paint);
        paint.setStyle(Paint.Style.FILL);
        y += lineHeight;

        // Net Salary
        headerPaint.setTextSize(18f);
        headerPaint.setColor(Color.parseColor("#6200EE"));
        canvas.drawText("NET SALARY:", x, y, headerPaint);
        canvas.drawText(String.format(Locale.US, "₹%.2f", netSalary), x + 200, y, headerPaint);
        y += lineHeight + 30;

        // Footer
        paint.setColor(Color.GRAY);
        paint.setTextSize(12f);
        String dateStr = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US)
                .format(new Date());
        canvas.drawText("Generated on: " + dateStr, x, y, paint);
        y += lineHeight;
        canvas.drawText("This is a system generated salary slip.", x, y, paint);

        // Finish the page
        pdfDocument.finishPage(page);

        // Save the PDF
        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }

            File file = new File(downloadsDir, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            pdfDocument.writeTo(fos);
            fos.close();
            pdfDocument.close();

            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (btnDownloadSalarySlip != null) {
                btnDownloadSalarySlip.setEnabled(true);
            }

            Toast.makeText(this, "PDF saved to Downloads folder", Toast.LENGTH_LONG).show();

            // Just show success message with file location
            openPdfFile(file);

        } catch (IOException e) {
            e.printStackTrace();
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (btnDownloadSalarySlip != null) {
                btnDownloadSalarySlip.setEnabled(true);
            }
            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Simplified method to just show a success message with file location
     * instead of trying to open the PDF with external apps
     */
    private void openPdfFile(File file) {
        // Just show a success message with file location
        Toast.makeText(this,
                "PDF saved to: Downloads/" + file.getName(),
                Toast.LENGTH_LONG).show();
    }

    // Keep the original button click handler
    public void onDownloadSlip(View view) {
        generateSalarySlipPdf();
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            loadSalaryData(); // Refresh data when returning to activity
        } catch (Exception e) {
            Log.e("SalaryActivity", "Error in onResume: " + e.getMessage());
        }
    }
}