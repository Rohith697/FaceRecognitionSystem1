package com.rohith.facerecognition;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FaceRecognitionApplication {

    public static void main(String[] args) {
        SpringApplication.run(FaceRecognitionApplication.class, args);
        System.out.println("========================================================================");
        System.out.println(" Face Recognition System is running at: http://localhost:8080");
        System.out.println("========================================================================");
    }
}
