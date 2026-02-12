package models;

import java.util.Date;

public class LeaveRequest {
    private String id;
    private String userId;
    private String employeeName;
    private String leaveType;
    private String fromDate;
    private String toDate;
    private String reason;
    private String status; // pending, manager_approved, manager_rejected, approved, rejected
    private Date appliedDate;
    private long days;
    private boolean isLOP;
    private String department;
    private String designation;
    private String managerStatus; // pending, approved, rejected
    private String hrStatus; // pending, approved, rejected
    private String managerId;
    private String managerName;

    // Required empty constructor for Firestore
    public LeaveRequest() {}

    // Constructor
    public LeaveRequest(String id, String userId, String employeeName, String leaveType,
                        String fromDate, String toDate, String reason, String status,
                        Date appliedDate, long days, boolean isLOP) {
        this.id = id;
        this.userId = userId;
        this.employeeName = employeeName;
        this.leaveType = leaveType;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.reason = reason;
        this.status = status;
        this.appliedDate = appliedDate;
        this.days = days;
        this.isLOP = isLOP;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public String getFromDate() { return fromDate; }
    public void setFromDate(String fromDate) { this.fromDate = fromDate; }

    public String getToDate() { return toDate; }
    public void setToDate(String toDate) { this.toDate = toDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getAppliedDate() { return appliedDate; }
    public void setAppliedDate(Date appliedDate) { this.appliedDate = appliedDate; }

    public long getDays() { return days; }
    public void setDays(long days) { this.days = days; }

    public boolean isLOP() { return isLOP; }
    public void setLOP(boolean LOP) { isLOP = LOP; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getManagerStatus() { return managerStatus; }
    public void setManagerStatus(String managerStatus) { this.managerStatus = managerStatus; }

    public String getHrStatus() { return hrStatus; }
    public void setHrStatus(String hrStatus) { this.hrStatus = hrStatus; }

    public String getManagerId() { return managerId; }
    public void setManagerId(String managerId) { this.managerId = managerId; }

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    // Helper method to calculate number of days
    public long calculateDaysFromDates() {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
            Date from = sdf.parse(fromDate);
            Date to = sdf.parse(toDate);
            long diff = to.getTime() - from.getTime();
            return (diff / (1000 * 60 * 60 * 24)) + 1;
        } catch (Exception e) {
            return days > 0 ? days : 1;
        }
    }
}