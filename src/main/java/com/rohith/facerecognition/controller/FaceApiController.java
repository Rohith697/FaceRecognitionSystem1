package com.rohith.facerecognition.controller;

import com.rohith.facerecognition.dto.RecognitionRequest;
import com.rohith.facerecognition.dto.RecognitionResponse;
import com.rohith.facerecognition.dto.UserRegistrationRequest;
import com.rohith.facerecognition.model.AttendanceLog;
import com.rohith.facerecognition.model.User;
import com.rohith.facerecognition.repository.UserRepository;
import com.rohith.facerecognition.service.FaceProcessorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class FaceApiController {

    private static final double SIMILARITY_THRESHOLD = 70.0; // Minimum percentage similarity

    private final FaceProcessorService faceProcessor;
    private final UserRepository userRepository;

    public FaceApiController(FaceProcessorService faceProcessor, UserRepository userRepository) {
        this.faceProcessor = faceProcessor;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegistrationRequest request) {
        try {
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Name is required"));
            }
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
            }
            if (request.getImageBase64() == null || request.getImageBase64().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Face photo capture is required"));
            }

            if (userRepository.existsByEmail(request.getEmail().trim())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "A user with email '" + request.getEmail() + "' already exists."));
            }

            // Process face feature vector
            double[] features = faceProcessor.extractFaceFeatures(request.getImageBase64());
            String featuresJson = faceProcessor.featuresToJson(features);

            User user = new User();
            user.setName(request.getName().trim());
            user.setEmail(request.getEmail().trim());
            user.setDepartment(request.getDepartment() != null ? request.getDepartment().trim() : "General");
            user.setFaceFeatures(featuresJson);
            user.setPhotoBase64(request.getImageBase64());

            Long userId = userRepository.saveUser(user);
            user.setId(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User '" + user.getName() + "' registered successfully!");
            response.put("userId", userId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/recognize")
    public ResponseEntity<RecognitionResponse> recognizeFace(@RequestBody RecognitionRequest request) {
        try {
            if (request.getImageBase64() == null || request.getImageBase64().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new RecognitionResponse(false, null, null, null, 0.0, "Empty frame provided"));
            }

            // Extract features from current webcam frame
            double[] inputFeatures = faceProcessor.extractFaceFeatures(request.getImageBase64());

            List<User> enrolledUsers = userRepository.findAllUsersWithFeatures();
            if (enrolledUsers.isEmpty()) {
                return ResponseEntity.ok(new RecognitionResponse(false, null, null, null, 0.0, "No registered users in database. Please enroll first."));
            }

            User bestMatchUser = null;
            double maxSimilarity = 0.0;

            for (User user : enrolledUsers) {
                if (user.getFaceFeatures() != null && !user.getFaceFeatures().isEmpty()) {
                    double[] storedFeatures = faceProcessor.jsonToFeatures(user.getFaceFeatures());
                    double similarity = faceProcessor.calculateSimilarity(inputFeatures, storedFeatures);

                    if (similarity > maxSimilarity) {
                        maxSimilarity = similarity;
                        bestMatchUser = user;
                    }
                }
            }

            if (bestMatchUser != null && maxSimilarity >= SIMILARITY_THRESHOLD) {
                // Log successful recognition / attendance entry
                AttendanceLog log = new AttendanceLog();
                log.setUserId(bestMatchUser.getId());
                log.setUserName(bestMatchUser.getName());
                log.setDepartment(bestMatchUser.getDepartment());
                log.setConfidenceScore(maxSimilarity);
                log.setStatus("SUCCESS");
                userRepository.saveAttendanceLog(log);

                return ResponseEntity.ok(new RecognitionResponse(
                        true,
                        bestMatchUser.getId(),
                        bestMatchUser.getName(),
                        bestMatchUser.getDepartment(),
                        maxSimilarity,
                        "Match found! Welcome " + bestMatchUser.getName()
                ));
            } else {
                return ResponseEntity.ok(new RecognitionResponse(
                        false,
                        null,
                        null,
                        null,
                        maxSimilarity,
                        maxSimilarity > 0.0 ? String.format("Face recognized with insufficient confidence (%.1f%%)", maxSimilarity) : "No matching face found."
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RecognitionResponse(false, null, null, null, 0.0, "Error during recognition: " + e.getMessage()));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAllUsers());
    }

    @GetMapping("/logs")
    public ResponseEntity<List<AttendanceLog>> getAttendanceLogs() {
        return ResponseEntity.ok(userRepository.findAllAttendanceLogs());
    }

    @GetMapping("/health")
    public ResponseEntity<?> getHealth() {
        int totalUsers = userRepository.findAllUsers().size();
        return ResponseEntity.ok(Map.of(
                "status", "ONLINE",
                "registeredUsers", totalUsers,
                "timestamp", System.currentTimeMillis()
        ));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        boolean deleted = userRepository.deleteUser(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of("success", true, "message", "User with ID " + id + " deleted successfully."));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found with ID " + id));
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String email = payload.get("email");
        String department = payload.get("department");

        if (name == null || name.trim().isEmpty() || email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name and email are required."));
        }

        boolean updated = userRepository.updateUser(id, name.trim(), email.trim(), department != null ? department.trim() : "General");
        if (updated) {
            return ResponseEntity.ok(Map.of("success", true, "message", "User updated successfully."));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found with ID " + id));
        }
    }

    @GetMapping("/admin/stats")
    public ResponseEntity<?> getAdminStats() {
        List<User> users = userRepository.findAllUsers();
        List<Map<String, Object>> deptCounts = userRepository.getDepartmentUserCounts();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", users.size());
        stats.put("totalCompanies", deptCounts.size());
        stats.put("companyCounts", deptCounts);
        return ResponseEntity.ok(stats);
    }
}
