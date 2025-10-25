package com.ridefast.ride_fast_backend.controller;

import com.ridefast.ride_fast_backend.service.logs.LogUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogsController {

  private final LogUploadService logUploadService;

  @GetMapping("/upload-latest")
  public ResponseEntity<String> uploadLatest() {
    String gsPath = logUploadService.uploadLatest();
    return ResponseEntity.ok(gsPath);
  }

  @GetMapping("/upload/{fileName}")
  public ResponseEntity<String> uploadByName(@PathVariable String fileName) {
    String gsPath = logUploadService.uploadByName(fileName);
    return ResponseEntity.ok(gsPath);
  }
}
