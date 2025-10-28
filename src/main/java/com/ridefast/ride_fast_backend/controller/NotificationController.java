package com.ridefast.ride_fast_backend.controller;

import com.ridefast.ride_fast_backend.dto.FcmTokenRequest;
import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.repository.DriverRepository;
import com.ridefast.ride_fast_backend.repository.UserRepository;
import com.ridefast.ride_fast_backend.service.notification.PushNotificationService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final UserRepository userRepository;
  private final DriverRepository driverRepository;
  private final PushNotificationService pushNotificationService;

  @PostMapping("/token")
  public ResponseEntity<?> registerToken(@RequestBody @Valid FcmTokenRequest request) {
    String username = currentUsername();
    if (username == null) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

    // Try user first
    MyUser user = userRepository.findByEmail(username).orElse(null);
    if (user != null) {
      user.setFcmToken(request.getToken());
      userRepository.save(user);
      return ResponseEntity.ok(Map.of("status", "ok"));
    }
    // Then driver
    Driver driver = driverRepository.findByEmail(username).orElse(null);
    if (driver != null) {
      driver.setFcmToken(request.getToken());
      driverRepository.save(driver);
      return ResponseEntity.ok(Map.of("status", "ok"));
    }
    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
  }

  @DeleteMapping("/token")
  public ResponseEntity<?> clearToken() {
    String username = currentUsername();
    if (username == null) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

    MyUser user = userRepository.findByEmail(username).orElse(null);
    if (user != null) {
      user.setFcmToken(null);
      userRepository.save(user);
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    Driver driver = driverRepository.findByEmail(username).orElse(null);
    if (driver != null) {
      driver.setFcmToken(null);
      driverRepository.save(driver);
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
  }

  @PostMapping("/test")
  public ResponseEntity<?> sendTest() {
    String username = currentUsername();
    if (username == null) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

    MyUser user = userRepository.findByEmail(username).orElse(null);
    if (user != null && user.getFcmToken() != null && !user.getFcmToken().isBlank()) {
      pushNotificationService.sendToToken(user.getFcmToken(), "Test", "This is a test notification", Map.of("type","test"));
      return ResponseEntity.ok(Map.of("sent", true));
    }
    Driver driver = driverRepository.findByEmail(username).orElse(null);
    if (driver != null && driver.getFcmToken() != null && !driver.getFcmToken().isBlank()) {
      pushNotificationService.sendToToken(driver.getFcmToken(), "Test", "This is a test notification", Map.of("type","test"));
      return ResponseEntity.ok(Map.of("sent", true));
    }
    return new ResponseEntity<>(Map.of("error","No FCM token registered for current account"), HttpStatus.BAD_REQUEST);
  }

  private String currentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return null;
    return auth.getName();
  }
}
