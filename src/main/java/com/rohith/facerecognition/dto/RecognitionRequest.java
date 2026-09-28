package com.rohith.facerecognition.dto;

public class RecognitionRequest 
{
    private String imageBase64;

    public RecognitionRequest() 
    {}

    public RecognitionRequest(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
}
