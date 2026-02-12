package models;

public class CommonLeavePool {
    private String id;
    private String leaveType; // "casual" or "sick"
    private int totalDays;
    private int donatedDays;
    private int usedDays;

    public CommonLeavePool() {}

    public CommonLeavePool(String leaveType) {
        this.leaveType = leaveType;
        this.totalDays = 0;
        this.donatedDays = 0;
        this.usedDays = 0;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public int getTotalDays() { return totalDays; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }

    public int getDonatedDays() { return donatedDays; }
    public void setDonatedDays(int donatedDays) { this.donatedDays = donatedDays; }

    public int getUsedDays() { return usedDays; }
    public void setUsedDays(int usedDays) { this.usedDays = usedDays; }

    // Helper methods
    public void addDays(int days) {
        this.totalDays += days;
        this.donatedDays += days;
    }

    public boolean useDays(int days) {
        if (totalDays - usedDays >= days) {
            usedDays += days;
            return true;
        }
        return false;
    }

    public int getAvailableDays() {
        return totalDays - usedDays;
    }
}