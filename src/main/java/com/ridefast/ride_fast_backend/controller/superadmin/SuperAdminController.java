package com.ridefast.ride_fast_backend.controller.superadmin;

import com.ridefast.ride_fast_backend.model.AdminUser;
import com.ridefast.ride_fast_backend.model.ApiKey;
import com.ridefast.ride_fast_backend.repository.AdminUserRepository;
import com.ridefast.ride_fast_backend.repository.ApiKeyRepository;
import com.ridefast.ride_fast_backend.service.logs.LogUploadService;
import com.ridefast.ride_fast_backend.service.storage.SignedUrlService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.nio.file.*;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/superadmin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

  private final AdminUserRepository adminUserRepository;
  private final ApiKeyRepository apiKeyRepository;
  private final PasswordEncoder passwordEncoder;
  private final LogUploadService logUploadService;
  private final SignedUrlService signedUrlService;

  @Value("${app.logging.log-path:logs}")
  private String logDir;

  // Admin management
  @GetMapping("/admins")
  public ResponseEntity<List<AdminUser>> listAdmins() {
    return ResponseEntity.ok(adminUserRepository.findAll());
  }

  @PostMapping("/admins")
  public ResponseEntity<AdminUser> createAdmin(@RequestBody CreateAdminRequest req) {
    AdminUser admin = AdminUser.builder()
        .username(req.getUsername())
        .password(passwordEncoder.encode(req.getPassword()))
        .role(req.getRole() == null ? "ROLE_ADMIN" : req.getRole())
        .build();
    return new ResponseEntity<>(adminUserRepository.save(admin), HttpStatus.CREATED);
  }

  @DeleteMapping("/admins/{id}")
  public ResponseEntity<Void> deleteAdmin(@PathVariable Long id) {
    if (!adminUserRepository.existsById(id)) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    adminUserRepository.deleteById(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  // API keys management
  @GetMapping("/keys")
  public ResponseEntity<List<ApiKey>> listKeys() {
    return ResponseEntity.ok(apiKeyRepository.findAll());
  }

  @PostMapping("/keys")
  public ResponseEntity<ApiKey> createKey(@RequestBody ApiKey req) {
    ApiKey key = ApiKey.builder()
        .name(req.getName())
        .value(req.getValue())
        .description(req.getDescription())
        .build();
    return new ResponseEntity<>(apiKeyRepository.save(key), HttpStatus.CREATED);
  }

  @DeleteMapping("/keys/{id}")
  public ResponseEntity<Void> deleteKey(@PathVariable Long id) {
    if (!apiKeyRepository.existsById(id)) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    apiKeyRepository.deleteById(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  // Logs access
  @GetMapping("/logs/names")
  public ResponseEntity<List<String>> listLogFiles() throws IOException {
    Path dir = Paths.get(logDir);
    if (!Files.exists(dir) || !Files.isDirectory(dir)) return ResponseEntity.ok(List.of());
    try (var stream = Files.list(dir)) {
      List<String> names = stream
          .filter(Files::isRegularFile)
          .map(p -> p.getFileName().toString())
          .sorted()
          .toList();
      return ResponseEntity.ok(names);
    }
  }

  @GetMapping("/logs/latest")
  public ResponseEntity<String> getLatestLog() throws IOException {
    Path dir = Paths.get(logDir);
    if (!Files.exists(dir) || !Files.isDirectory(dir)) return new ResponseEntity<>("", HttpStatus.OK);
    Path latest = Files.list(dir)
        .filter(Files::isRegularFile)
        .max((a,b) -> Long.compare(a.toFile().lastModified(), b.toFile().lastModified()))
        .orElse(null);
    if (latest == null) return new ResponseEntity<>("", HttpStatus.OK);
    String content = Files.readString(latest);
    return ResponseEntity.ok(content);
  }

  @GetMapping("/logs")
  public ResponseEntity<String> getLogByName(@RequestParam("name") String name) throws IOException {
    Path file = Paths.get(logDir, name);
    if (!Files.exists(file) || !Files.isRegularFile(file)) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    String content = Files.readString(file);
    return ResponseEntity.ok(content);
  }

  @PostMapping("/logs/upload-latest")
  public ResponseEntity<UploadResponse> uploadLatestLog() {
    String gsPath = logUploadService.uploadLatest();
    return new ResponseEntity<>(new UploadResponse(gsPath), HttpStatus.CREATED);
  }

  @GetMapping("/logs/download")
  public ResponseEntity<DownloadResponse> getLogDownloadUrl(@RequestParam("object") String objectName) {
    String url = signedUrlService.signLog(objectName, 15).toString();
    return ResponseEntity.ok(new DownloadResponse(objectName, url, 15));
  }

  @Data
  public static class UploadResponse {
    private final String gsPath;
  }

  @Data
  public static class DownloadResponse {
    private final String object;
    private final String url;
    private final int expiresInMinutes;
  }

  // DTOs
  @Data
  public static class CreateAdminRequest {
    private String username;
    private String password;
    private String role; // defaults to ROLE_ADMIN
  }
}
