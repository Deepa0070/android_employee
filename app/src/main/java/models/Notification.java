package models;

public class Notification {
    private String id;
    private String userId;
    private String title;
    private String message;
    private String type;
    private String leaveId;
    private String fromUserId;
    private String fromUserName;
    private long timestamp;
    private boolean read;

    // Empty constructor for Firestore
    public Notification() {}

    // Constructor
    public Notification(String id, String userId, String title, String message, String type,
                        String leaveId, String fromUserId, String fromUserName,
                        long timestamp, boolean read) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.leaveId = leaveId;
        this.fromUserId = fromUserId;
        this.fromUserName = fromUserName;
        this.timestamp = timestamp;
        this.read = read;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLeaveId() {
        return leaveId;
    }

    public void setLeaveId(String leaveId) {
        this.leaveId = leaveId;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    public String getFromUserName() {
        return fromUserName;
    }

    public void setFromUserName(String fromUserName) {
        this.fromUserName = fromUserName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}