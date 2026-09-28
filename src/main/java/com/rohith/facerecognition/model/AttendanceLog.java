package com.rohith.facerecognition.model;

import java.time.LocalDateTime;

public class AttendanceLog {
    private Long id;
    private Long userId;
    private String userName;
    private String department;
    private Double confidenceScore;
    private String status;
    private LocalDateTime timestamp;

    public AttendanceLog() 
    {
    	
    }
   
    public AttendanceLog(Long id, Long userId, String userName, String department, Double confidenceScore, String status, LocalDateTime timestamp) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.department = department;
        this.confidenceScore = confidenceScore;
        this.status = status;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
