package com.ridefast.ride_fast_backend.service.logs;

import com.ridefast.ride_fast_backend.service.logs.LogUploadService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogAutoUploader {
  private static final Logger log = LoggerFactory.getLogger(LogAutoUploader.class);

  private final LogUploadService logUploadService;

  @Value("${app.logging.upload-enabled:false}")
  private boolean uploadEnabled;

  // Every 15 minutes
  @Scheduled(cron = "0 0/15 * * * *")
  public void uploadLatestPeriodically() {
    if (!uploadEnabled) return;
    try {
      String path = logUploadService.uploadLatest();
      log.debug("Auto-uploaded latest log to {}", path);
    } catch (Exception e) {
      log.warn("Auto log upload failed: {}", e.getMessage());
    }
  }
}
