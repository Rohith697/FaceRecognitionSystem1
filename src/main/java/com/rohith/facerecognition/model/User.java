package com.rohith.facerecognition.model;

import java.time.LocalDateTime;

public class User {
    private Long id;
    private String name;
    private String email;
    private String department;
    private String faceFeatures; // JSON array string of float values
    private String photoBase64;
    private LocalDateTime createdAt;

    public User() {}

    public User(Long id, String name, String email, String department, String faceFeatures, String photoBase64, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.department = department;
        this.faceFeatures = faceFeatures;
        this.photoBase64 = photoBase64;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getFaceFeatures() { return faceFeatures; }
    public void setFaceFeatures(String faceFeatures) { this.faceFeatures = faceFeatures; }

    public String getPhotoBase64() { return photoBase64; }
    public void setPhotoBase64(String photoBase64) { this.photoBase64 = photoBase64; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
