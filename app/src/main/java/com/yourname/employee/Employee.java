package com.yourname.employee;

public class Employee {
    private String id;
    private String name;
    private String email;
    private String department;
    private String designation;
    private String role;
    private int casualLeave;
    private int sickLeave;
    private int lopBalance;

    // Empty constructor
    public Employee() {
    }

    // Constructor
    public Employee(String id, String name, String email, String department,
                    String designation, String role, int casualLeave,
                    int sickLeave, int lopBalance) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.department = department;
        this.designation = designation;
        this.role = role;
        this.casualLeave = casualLeave;
        this.sickLeave = sickLeave;
        this.lopBalance = lopBalance;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getCasualLeave() { return casualLeave; }
    public void setCasualLeave(int casualLeave) { this.casualLeave = casualLeave; }

    public int getSickLeave() { return sickLeave; }
    public void setSickLeave(int sickLeave) { this.sickLeave = sickLeave; }

    public int getLopBalance() { return lopBalance; }
    public void setLopBalance(int lopBalance) { this.lopBalance = lopBalance; }
}