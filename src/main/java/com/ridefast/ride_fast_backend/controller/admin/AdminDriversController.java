package com.ridefast.ride_fast_backend.controller.admin;

import com.ridefast.ride_fast_backend.enums.KycStatus;
import com.ridefast.ride_fast_backend.enums.UserRole;
import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.DriverKyc;
import com.ridefast.ride_fast_backend.repository.DriverKycRepository;
import com.ridefast.ride_fast_backend.repository.DriverRepository;
import com.ridefast.ride_fast_backend.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/drivers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminDriversController {

  private final DriverRepository driverRepository;
  private final DriverKycRepository driverKycRepository;
  private final StorageService storageService;
  
  @Value("${app.firebase.storage-bucket:}")
  private String storageBucket;

  @GetMapping
  public ResponseEntity<Page<Driver>> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           @RequestParam(required = false) String search) {
    // Sort by ID descending so newest drivers appear first
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    if (search != null && !search.trim().isEmpty()) {
      // Escape special characters for LIKE query
      String searchTerm = search.trim().replace("%", "\\%").replace("_", "\\_");
      return ResponseEntity.ok(driverRepository.searchDrivers(searchTerm, pageable));
    }
    return ResponseEntity.ok(driverRepository.findAll(pageable));
  }

  @GetMapping("/{driverId}")
  public ResponseEntity<Driver> get(@PathVariable Long driverId) {
    return driverRepository.findById(driverId).map(ResponseEntity::ok)
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @PostMapping
  public ResponseEntity<Driver> create(@RequestBody Driver body) {
    body.setRole(UserRole.DRIVER);
    Driver saved = driverRepository.save(body);
    return new ResponseEntity<>(saved, HttpStatus.CREATED);
  }

  @PutMapping("/{driverId}")
  public ResponseEntity<Driver> update(@PathVariable Long driverId, @RequestBody Driver body) {
    return driverRepository.findById(driverId)
        .map(existing -> {
          if (body.getName() != null) existing.setName(body.getName());
          if (body.getEmail() != null) existing.setEmail(body.getEmail());
          if (body.getMobile() != null) existing.setMobile(body.getMobile());
          if (body.getLatitude() != null) existing.setLatitude(body.getLatitude());
          if (body.getLongitude() != null) existing.setLongitude(body.getLongitude());
          if (body.getRating() != null) existing.setRating(body.getRating());
          return new ResponseEntity<>(driverRepository.save(existing), HttpStatus.OK);
        })
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @DeleteMapping("/{driverId}")
  public ResponseEntity<Void> delete(@PathVariable Long driverId) {
    if (!driverRepository.existsById(driverId)) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    driverRepository.deleteById(driverId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  // ========== DRIVER DETAILS WITH DOCUMENTS ==========

  @GetMapping("/{driverId}/details")
  public ResponseEntity<?> getDriverDetails(@PathVariable Long driverId) {
    return driverRepository.findById(driverId)
        .map(driver -> {
          Map<String, Object> response = new HashMap<>();
          response.put("driver", driver);
          
          // Get KYC documents if available
          driverKycRepository.findByDriver(driver).ifPresent(kyc -> {
            Map<String, Object> kycData = new HashMap<>();
            kycData.put("id", kyc.getId());
            kycData.put("status", kyc.getStatus());
            kycData.put("aadhaarNumber", kyc.getAadhaarNumber());
            kycData.put("licenseNumber", kyc.getLicenseNumber());
            kycData.put("rcNumber", kyc.getRcNumber());
            kycData.put("rejectionReason", kyc.getRejectionReason());
            kycData.put("submittedAt", kyc.getSubmittedAt());
            kycData.put("reviewedAt", kyc.getReviewedAt());
            
            // Generate public URLs for documents
            Map<String, String> documents = new HashMap<>();
            if (kyc.getPhotoKey() != null && !kyc.getPhotoKey().isBlank()) {
              documents.put("profilePhoto", getPublicUrl(kyc.getPhotoKey()));
            }
            if (kyc.getLicenseFrontKey() != null && !kyc.getLicenseFrontKey().isBlank()) {
              documents.put("licenseFront", getPublicUrl(kyc.getLicenseFrontKey()));
            }
            if (kyc.getLicenseBackKey() != null && !kyc.getLicenseBackKey().isBlank()) {
              documents.put("licenseBack", getPublicUrl(kyc.getLicenseBackKey()));
            }
            if (kyc.getRcFrontKey() != null && !kyc.getRcFrontKey().isBlank()) {
              documents.put("rcFront", getPublicUrl(kyc.getRcFrontKey()));
            }
            if (kyc.getRcBackKey() != null && !kyc.getRcBackKey().isBlank()) {
              documents.put("rcBack", getPublicUrl(kyc.getRcBackKey()));
            }
            if (kyc.getAadhaarFrontKey() != null && !kyc.getAadhaarFrontKey().isBlank()) {
              documents.put("aadhaarFront", getPublicUrl(kyc.getAadhaarFrontKey()));
            }
            if (kyc.getAadhaarBackKey() != null && !kyc.getAadhaarBackKey().isBlank()) {
              documents.put("aadhaarBack", getPublicUrl(kyc.getAadhaarBackKey()));
            }
            
            kycData.put("documents", documents);
            response.put("kyc", kycData);
          });
          
          return ResponseEntity.ok(response);
        })
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/{driverId}/kyc")
  public ResponseEntity<?> getDriverKyc(@PathVariable Long driverId) {
    return driverRepository.findById(driverId)
        .map(driver -> driverKycRepository.findByDriver(driver)
            .map(kyc -> {
              Map<String, Object> response = new HashMap<>();
              response.put("id", kyc.getId());
              response.put("status", kyc.getStatus());
              response.put("aadhaarNumber", kyc.getAadhaarNumber());
              response.put("licenseNumber", kyc.getLicenseNumber());
              response.put("rcNumber", kyc.getRcNumber());
              response.put("rejectionReason", kyc.getRejectionReason());
              response.put("submittedAt", kyc.getSubmittedAt());
              response.put("reviewedAt", kyc.getReviewedAt());
              
              // Document URLs
              Map<String, String> documents = new HashMap<>();
              if (kyc.getPhotoKey() != null) documents.put("profilePhoto", getPublicUrl(kyc.getPhotoKey()));
              if (kyc.getLicenseFrontKey() != null) documents.put("licenseFront", getPublicUrl(kyc.getLicenseFrontKey()));
              if (kyc.getLicenseBackKey() != null) documents.put("licenseBack", getPublicUrl(kyc.getLicenseBackKey()));
              if (kyc.getRcFrontKey() != null) documents.put("rcFront", getPublicUrl(kyc.getRcFrontKey()));
              if (kyc.getRcBackKey() != null) documents.put("rcBack", getPublicUrl(kyc.getRcBackKey()));
              if (kyc.getAadhaarFrontKey() != null) documents.put("aadhaarFront", getPublicUrl(kyc.getAadhaarFrontKey()));
              if (kyc.getAadhaarBackKey() != null) documents.put("aadhaarBack", getPublicUrl(kyc.getAadhaarBackKey()));
              response.put("documents", documents);
              
              return ResponseEntity.ok(response);
            })
            .orElseGet(() -> ResponseEntity.ok(Map.of("status", "NOT_SUBMITTED", "message", "KYC not submitted"))))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @PutMapping("/{driverId}/kyc/approve")
  public ResponseEntity<?> approveKyc(@PathVariable Long driverId) {
    return driverRepository.findById(driverId)
        .map(driver -> driverKycRepository.findByDriver(driver)
            .map(kyc -> {
              kyc.setStatus(KycStatus.APPROVED);
              kyc.setReviewedAt(Instant.now());
              kyc.setRejectionReason(null);
              driverKycRepository.save(kyc);
              log.info("KYC approved for driver: {}", driverId);
              return ResponseEntity.ok(Map.of("status", "APPROVED", "message", "KYC approved successfully"));
            })
            .orElseGet(() -> ResponseEntity.badRequest().body(Map.of("error", "KYC not found"))))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @PutMapping("/{driverId}/kyc/reject")
  public ResponseEntity<?> rejectKyc(@PathVariable Long driverId, @RequestBody Map<String, String> body) {
    String reason = body.get("reason");
    if (reason == null || reason.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Rejection reason is required"));
    }
    
    return driverRepository.findById(driverId)
        .map(driver -> driverKycRepository.findByDriver(driver)
            .map(kyc -> {
              kyc.setStatus(KycStatus.REJECTED);
              kyc.setReviewedAt(Instant.now());
              kyc.setRejectionReason(reason);
              driverKycRepository.save(kyc);
              log.info("KYC rejected for driver: {} - Reason: {}", driverId, reason);
              return ResponseEntity.ok(Map.of("status", "REJECTED", "message", "KYC rejected", "reason", reason));
            })
            .orElseGet(() -> ResponseEntity.badRequest().body(Map.of("error", "KYC not found"))))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/pending-kyc")
  public ResponseEntity<?> getPendingKycDrivers(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt"));
    Page<DriverKyc> pendingKyc = driverKycRepository.findByStatus(KycStatus.PENDING, pageable);
    return ResponseEntity.ok(pendingKyc);
  }

  private String getPublicUrl(String key) {
    if (key == null || key.isBlank()) {
      log.warn("getPublicUrl called with null or blank key");
      return null;
    }
    // If key is already a full URL, return as is
    if (key.startsWith("http://") || key.startsWith("https://")) {
      log.debug("Key is already a URL: {}", key);
      return key;
    }
    // Construct Firebase Storage public URL
    String bucket = storageBucket != null && !storageBucket.isBlank() ? storageBucket : "gauva-15d9a.firebasestorage.app";
    // URL encode the path component (not the bucket name)
    String encodedKey = java.net.URLEncoder.encode(key, java.nio.charset.StandardCharsets.UTF_8)
        .replace("+", "%20"); // Replace + with %20 for spaces
    String url = String.format("https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
        bucket, encodedKey);
    log.debug("Generated public URL for key '{}': {}", key, url);
    return url;
  }
}
