package com.rohith.facerecognition.dto;

public class RecognitionResponse 
{
    private boolean recognized;
    private Long userId;
    private String name;
    private String department;
    private double confidenceScore;
    private String message;

    public RecognitionResponse() {}

    public RecognitionResponse(boolean recognized, Long userId, String name, String department, double confidenceScore, String message) {
        this.recognized = recognized;
        this.userId = userId;
        this.name = name;
        this.department = department;
        this.confidenceScore = confidenceScore;
        this.message = message;
    }

    public boolean isRecognized() { return recognized; }
    public void setRecognized(boolean recognized) { this.recognized = recognized; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
