package models;

import java.util.Date;

public class DonationRequest {
    private String id;
    private String donorId;
    private String donorName;
    private String recipientId; // null if donated to common pool
    private String recipientName;
    private String leaveType; // "Casual Leave" or "Sick Leave"
    private int numberOfDays;
    private String status; // "pending", "approved", "rejected"
    private String reason;
    private Date appliedDate;
    private Date approvedDate;
    private String hrId;
    private String hrName;

    // NEW FIELDS for emergency usage tracking
    private boolean usedForEmergency;
    private String emergencyRequestId;

    // Required empty constructor for Firestore
    public DonationRequest() {}

    // Constructor
    public DonationRequest(String id, String donorId, String donorName, String recipientId,
                           String recipientName, String leaveType, int numberOfDays,
                           String status, String reason) {
        this.id = id;
        this.donorId = donorId;
        this.donorName = donorName;
        this.recipientId = recipientId;
        this.recipientName = recipientName;
        this.leaveType = leaveType;
        this.numberOfDays = numberOfDays;
        this.status = status;
        this.reason = reason;
        this.appliedDate = new Date();
        this.usedForEmergency = false; // Default value
        this.emergencyRequestId = null; // Default value
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDonorId() { return donorId; }
    public void setDonorId(String donorId) { this.donorId = donorId; }

    public String getDonorName() { return donorName; }
    public void setDonorName(String donorName) { this.donorName = donorName; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public int getNumberOfDays() { return numberOfDays; }
    public void setNumberOfDays(int numberOfDays) { this.numberOfDays = numberOfDays; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Date getAppliedDate() { return appliedDate; }
    public void setAppliedDate(Date appliedDate) { this.appliedDate = appliedDate; }

    public Date getApprovedDate() { return approvedDate; }
    public void setApprovedDate(Date approvedDate) { this.approvedDate = approvedDate; }

    public String getHrId() { return hrId; }
    public void setHrId(String hrId) { this.hrId = hrId; }

    public String getHrName() { return hrName; }
    public void setHrName(String hrName) { this.hrName = hrName; }

    // New fields for tracking emergency usage
    public boolean isUsedForEmergency() {
        return usedForEmergency;
    }

    public void setUsedForEmergency(boolean usedForEmergency) {
        this.usedForEmergency = usedForEmergency;
    }

    public String getEmergencyRequestId() {
        return emergencyRequestId;
    }

    public void setEmergencyRequestId(String emergencyRequestId) {
        this.emergencyRequestId = emergencyRequestId;
    }

    // Helper methods
    public boolean isDonatedToCommonPool() {
        return recipientId == null || recipientId.isEmpty();
    }

    public boolean isApproved() {
        return status != null && status.equals("approved");
    }

    public boolean isAvailableForEmergency() {
        return isDonatedToCommonPool() && isApproved() && !usedForEmergency;
    }
}