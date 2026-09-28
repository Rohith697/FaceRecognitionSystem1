package com.rohith.facerecognition.dto;

public class UserRegistrationRequest {
    private String name;
    private String email;
    private String department;
    private String imageBase64; // Base64 encoded image string from webcam capture

    public UserRegistrationRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
}
