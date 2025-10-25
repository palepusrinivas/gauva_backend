package com.ridefast.ride_fast_backend.service.logs.impl;

import com.ridefast.ride_fast_backend.service.logs.LogUploadService;
import com.ridefast.ride_fast_backend.service.storage.StorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LogUploadServiceImpl implements LogUploadService {
  private static final Logger log = LoggerFactory.getLogger(LogUploadServiceImpl.class);

  private final StorageService storageService;

  @Value("${app.logging.log-path:logs}")
  private String logDir;

  @Value("${app.logging.upload-enabled:false}")
  private boolean uploadEnabled;

  public LogUploadServiceImpl(StorageService storageService) {
    this.storageService = storageService;
  }

  @Override
  public String uploadLatest() {
    if (!uploadEnabled) {
      throw new IllegalStateException("Log upload disabled by configuration");
    }
    try {
      Path dir = Paths.get(logDir);
      if (!Files.exists(dir) || !Files.isDirectory(dir)) {
        throw new IllegalStateException("Logs directory not found: " + dir.toAbsolutePath());
      }
      Optional<Path> latest = Files.list(dir)
          .filter(Files::isRegularFile)
          .max(Comparator.comparingLong(p -> p.toFile().lastModified()));
      if (latest.isEmpty()) {
        throw new IllegalStateException("No log files found in: " + dir.toAbsolutePath());
      }
      Path file = latest.get();
      byte[] data = Files.readAllBytes(file);
      String objectName = "server_" + java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
          .format(java.time.LocalDateTime.now()) + "_" + file.getFileName();
      String contentType = "text/plain";
      return storageService.uploadAppLog(data, contentType, objectName);
    } catch (IOException e) {
      log.error("Failed to upload latest log: {}", e.getMessage());
      throw new RuntimeException("Failed to upload latest log");
    }
  }

  @Override
  public String uploadByName(String fileName) {
    if (!uploadEnabled) {
      throw new IllegalStateException("Log upload disabled by configuration");
    }
    try {
      Path file = Paths.get(logDir, fileName);
      if (!Files.exists(file) || !Files.isRegularFile(file)) {
        throw new IllegalArgumentException("Log file not found: " + file.toAbsolutePath());
      }
      byte[] data = Files.readAllBytes(file);
      String objectName = "named_" + file.getFileName();
      String contentType = "text/plain";
      return storageService.uploadAppLog(data, contentType, objectName);
    } catch (IOException e) {
      log.error("Failed to upload log by name: {}", e.getMessage());
      throw new RuntimeException("Failed to upload log by name");
    }
  }
}
