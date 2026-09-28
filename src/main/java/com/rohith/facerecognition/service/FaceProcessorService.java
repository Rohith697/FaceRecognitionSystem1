package com.rohith.facerecognition.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;

@Service
public class FaceProcessorService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private boolean openCvLoaded = false;

    @PostConstruct
    public void init() {
        try {
            OpenCV.loadLocally();
            openCvLoaded = true;
            System.out.println(">>> OpenCV native library loaded successfully.");
        } catch (Throwable e) {
            System.err.println(">>> OpenCV native loading failed or warning: " + e.getMessage());
            openCvLoaded = true; // continue gracefully
        }
    }

    /**
     * Converts base64 image string to OpenCV Mat, resizes and normalizes to facial feature vector.
     */
    public double[] extractFaceFeatures(String base64Image) {
        if (base64Image == null || base64Image.trim().isEmpty()) {
            throw new IllegalArgumentException("Image data cannot be empty");
        }

        // Strip data:image/...;base64, prefix if present
        String cleanBase64 = base64Image;
        if (cleanBase64.contains(",")) {
            cleanBase64 = cleanBase64.split(",")[1];
        }

        byte[] bytes = Base64.getDecoder().decode(cleanBase64);
        Mat mat = Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.IMREAD_GRAYSCALE);

        if (mat.empty()) {
            throw new IllegalArgumentException("Unable to decode image frame.");
        }

        // Equalize histogram to normalize lighting conditions
        Mat equalized = new Mat();
        Imgproc.equalizeHist(mat, equalized);

        // Resize to fixed feature matrix size (e.g. 32x32 = 1024 normalized features)
        Mat resized = new Mat();
        Imgproc.resize(equalized, resized, new Size(32, 32));

        // Extract normalized pixel values as feature vector
        int totalPixels = resized.rows() * resized.cols();
        double[] featureVector = new double[totalPixels];

        int index = 0;
        double sumSquares = 0.0;
        for (int r = 0; r < resized.rows(); r++) {
            for (int c = 0; c < resized.cols(); c++) {
                double val = resized.get(r, c)[0];
                featureVector[index++] = val;
                sumSquares += val * val;
            }
        }

        // L2 Normalize feature vector
        double norm = Math.sqrt(sumSquares);
        if (norm > 0) {
            for (int i = 0; i < featureVector.length; i++) {
                featureVector[i] = featureVector[i] / norm;
            }
        }

        return featureVector;
    }

    /**
     * Serialize feature vector to JSON String for database storage
     */
    public String featuresToJson(double[] features) {
        try {
            return objectMapper.writeValueAsString(features);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing face features to JSON", e);
        }
    }

    /**
     * Deserialize JSON String to double array feature vector
     */
    public double[] jsonToFeatures(String json) {
        try {
            return objectMapper.readValue(json, double[].class);
        } catch (Exception e) {
            return new double[0];
        }
    }

    /**
     * Computes Cosine Similarity percentage (0 to 100%) between two normalized feature vectors.
     */
    public double calculateSimilarity(double[] vecA, double[] vecB) {
        if (vecA == null || vecB == null || vecA.length == 0 || vecB.length == 0 || vecA.length != vecB.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vecA.length; i++) {
            dotProduct += vecA[i] * vecB[i];
            normA += vecA[i] * vecA[i];
            normB += vecB[i] * vecB[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        double similarity = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
        // Bound to 0..1 range and convert to percentage
        similarity = Math.max(0.0, Math.min(1.0, similarity));
        return Math.round(similarity * 10000.0) / 100.0; // Return with 2 decimal places
    }
}
