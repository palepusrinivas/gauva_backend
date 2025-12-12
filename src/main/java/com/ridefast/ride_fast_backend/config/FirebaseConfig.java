package com.ridefast.ride_fast_backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FirebaseConfig {
  private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

  @Value("${app.firebase.project-id:}")
  private String projectId;

  @Value("${app.firebase.storage-bucket:}")
  private String storageBucket;





  // Prefer base64 credentials content. Fallback to file path. If both empty, rely on ADC.
  @Value("${app.firebase.credentials-b64:}")
  private String credentialsB64;

  @Value("${app.firebase.credentials-path:}")
  private String credentialsPath;

  @PostConstruct
  public void init() {
    try {
      if (!FirebaseApp.getApps().isEmpty()) {
        log.info("Firebase already initialized");
        return;
      }

      GoogleCredentials credentials = null;
      if (credentialsB64 != null && !credentialsB64.isBlank()) {
        log.info("Initializing Firebase with base64 credentials");
        byte[] decoded = java.util.Base64.getDecoder().decode(credentialsB64);
        try (InputStream is = new ByteArrayInputStream(decoded)) {
          credentials = GoogleCredentials.fromStream(is);
        }
      } else if (credentialsPath != null && !credentialsPath.isBlank()) {
        log.info("Initializing Firebase with credentials file: {}", credentialsPath);
        try (InputStream is = new FileInputStream(credentialsPath)) {
          credentials = GoogleCredentials.fromStream(is);
        }
      } else {
        log.warn("No Firebase credentials provided (credentials-b64 or credentials-path). Attempting Application Default Credentials...");
        try {
          // Use Application Default Credentials if available
          credentials = GoogleCredentials.getApplicationDefault();
          log.info("Using Application Default Credentials for Firebase");
        } catch (Exception adcException) {
          log.error("Firebase initialization FAILED: No credentials provided and Application Default Credentials not available. " +
              "Please set FIREBASE_CREDENTIALS_B64 environment variable with base64-encoded service account JSON. " +
              "Error: {}", adcException.getMessage());
          // Don't throw exception here - allow app to start but Firebase features will fail
          // This allows the app to start even if Firebase is not configured (for development)
          return;
        }
      }

      FirebaseOptions.Builder builder = FirebaseOptions.builder().setCredentials(credentials);
      if (projectId != null && !projectId.isBlank()) {
        builder.setProjectId(projectId);
      }
      if (storageBucket != null && !storageBucket.isBlank()) {
        builder.setStorageBucket(storageBucket);
      }

      FirebaseOptions options = builder.build();
      FirebaseApp.initializeApp(options);
      log.info("Firebase initialized successfully. projectId={} bucket={}", projectId, storageBucket);
    } catch (Exception e) {
      log.error("Firebase initialization FAILED: {}. Firebase Storage features will not work. " +
          "Please configure FIREBASE_CREDENTIALS_B64 environment variable.", e.getMessage(), e);
      // Don't throw - allow app to start but Firebase features will fail gracefully
    }
  }
}
