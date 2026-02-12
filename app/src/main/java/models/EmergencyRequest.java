package models;

import java.util.Date;
import java.util.List;

public class EmergencyRequest {
    private String id;
    private String employeeId;
    private String employeeName;
    private String department;
    private String designation;
    private String fromDate;
    private String toDate;
    private String reason;
    private String emergencyDetails;
    private String leaveType;
    private String poolType; // "casual" or "sick"
    private int numberOfDays;
    private String status; // pending, pending_manager, approved, rejected
    private Date requestDate;
    private Date actionDate;
    private String hrId;
    private String hrName;

    // Manager fields - ADD THESE
    private String managerId;
    private String managerName;

    private boolean allocatedFromCommonPool;
    private int allocatedDays;
    private List<String> sourceDonations;

    // Empty constructor for Firestore
    public EmergencyRequest() {}

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getFromDate() { return fromDate; }
    public void setFromDate(String fromDate) { this.fromDate = fromDate; }

    public String getToDate() { return toDate; }
    public void setToDate(String toDate) { this.toDate = toDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getEmergencyDetails() { return emergencyDetails; }
    public void setEmergencyDetails(String emergencyDetails) { this.emergencyDetails = emergencyDetails; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public String getPoolType() { return poolType; }
    public void setPoolType(String poolType) { this.poolType = poolType; }

    public int getNumberOfDays() { return numberOfDays; }
    public void setNumberOfDays(int numberOfDays) { this.numberOfDays = numberOfDays; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getRequestDate() { return requestDate; }
    public void setRequestDate(Date requestDate) { this.requestDate = requestDate; }

    public Date getActionDate() { return actionDate; }
    public void setActionDate(Date actionDate) { this.actionDate = actionDate; }

    public String getHrId() { return hrId; }
    public void setHrId(String hrId) { this.hrId = hrId; }

    public String getHrName() { return hrName; }
    public void setHrName(String hrName) { this.hrName = hrName; }

    // Manager getters and setters - ADD THESE
    public String getManagerId() { return managerId; }
    public void setManagerId(String managerId) { this.managerId = managerId; }

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    // New fields for tracking donations
    public boolean isAllocatedFromCommonPool() { return allocatedFromCommonPool; }
    public void setAllocatedFromCommonPool(boolean allocatedFromCommonPool) {
        this.allocatedFromCommonPool = allocatedFromCommonPool;
    }

    public int getAllocatedDays() { return allocatedDays; }
    public void setAllocatedDays(int allocatedDays) {
        this.allocatedDays = allocatedDays;
    }

    public List<String> getSourceDonations() { return sourceDonations; }
    public void setSourceDonations(List<String> sourceDonations) {
        this.sourceDonations = sourceDonations;
    }

    // Optional: Helper method to check if manager is assigned
    public boolean hasManager() {
        return managerId != null && !managerId.isEmpty();
    }
}