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
        return;
      }

      GoogleCredentials credentials = null;
      if (credentialsB64 != null && !credentialsB64.isBlank()) {
        byte[] decoded = java.util.Base64.getDecoder().decode(credentialsB64);
        try (InputStream is = new ByteArrayInputStream(decoded)) {
          credentials = GoogleCredentials.fromStream(is);
        }
      } else if (credentialsPath != null && !credentialsPath.isBlank()) {
        try (InputStream is = new FileInputStream(credentialsPath)) {
          credentials = GoogleCredentials.fromStream(is);
        }
      } else {
        // Use Application Default Credentials if available
        credentials = GoogleCredentials.getApplicationDefault();
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
      log.info("Firebase initialized. projectId={} bucket={}", projectId, storageBucket);
    } catch (Exception e) {
      log.warn("Firebase initialization skipped: {}", e.getMessage());
    }
  }
}
