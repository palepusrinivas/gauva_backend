package com.ridefast.ride_fast_backend.controller.admin;

import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.DriverKyc;
import com.ridefast.ride_fast_backend.repository.DriverKycRepository;
import com.ridefast.ride_fast_backend.repository.DriverRepository;
import com.ridefast.ride_fast_backend.service.storage.StorageService;
import com.ridefast.ride_fast_backend.service.storage.SignedUrlService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/admin/kyc")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminKycController {

  private final DriverRepository driverRepository;
  private final DriverKycRepository kycRepository;
  @Autowired
  private final StorageService storageService;
  private final SignedUrlService signedUrlService;

  @Value("${app.firebase.documents-gs-path:}")
  private String documentsGsPath;

  @GetMapping("/drivers/{driverId}")
  public ResponseEntity<Map<String, Object>> getDriverKyc(@PathVariable Long driverId) {
    Driver driver = driverRepository.findById(driverId)
        .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
    DriverKyc kyc = kycRepository.findByDriver(driver)
        .orElseThrow(() -> new IllegalArgumentException("KYC not found"));
    
    Map<String, Object> response = new HashMap<>();
    response.put("kyc", kyc);
    response.put("driver", Map.of(
        "id", driver.getId(),
        "name", driver.getName() != null ? driver.getName() : "",
        "email", driver.getEmail() != null ? driver.getEmail() : "",
        "mobile", driver.getMobile() != null ? driver.getMobile() : ""
    ));
    
    // Generate signed URLs for all documents
    Map<String, String> documentUrls = new HashMap<>();
    if (kyc.getPhotoKey() != null && !kyc.getPhotoKey().isBlank()) {
      documentUrls.put("photo", signedUrlService.signDocument(kyc.getPhotoKey(), 60).toString());
    }
    if (kyc.getAadhaarFrontKey() != null && !kyc.getAadhaarFrontKey().isBlank()) {
      documentUrls.put("aadhaarFront", signedUrlService.signDocument(kyc.getAadhaarFrontKey(), 60).toString());
    }
    if (kyc.getAadhaarBackKey() != null && !kyc.getAadhaarBackKey().isBlank()) {
      documentUrls.put("aadhaarBack", signedUrlService.signDocument(kyc.getAadhaarBackKey(), 60).toString());
    }
    if (kyc.getLicenseFrontKey() != null && !kyc.getLicenseFrontKey().isBlank()) {
      documentUrls.put("licenseFront", signedUrlService.signDocument(kyc.getLicenseFrontKey(), 60).toString());
    }
    if (kyc.getLicenseBackKey() != null && !kyc.getLicenseBackKey().isBlank()) {
      documentUrls.put("licenseBack", signedUrlService.signDocument(kyc.getLicenseBackKey(), 60).toString());
    }
    if (kyc.getRcFrontKey() != null && !kyc.getRcFrontKey().isBlank()) {
      documentUrls.put("rcFront", signedUrlService.signDocument(kyc.getRcFrontKey(), 60).toString());
    }
    if (kyc.getRcBackKey() != null && !kyc.getRcBackKey().isBlank()) {
      documentUrls.put("rcBack", signedUrlService.signDocument(kyc.getRcBackKey(), 60).toString());
    }
    response.put("documentUrls", documentUrls);
    
    return ResponseEntity.ok(response);
  }
  
  @GetMapping("/pending")
  public ResponseEntity<?> getPendingKyc(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    org.springframework.data.domain.Pageable pageable = 
        org.springframework.data.domain.PageRequest.of(page, size, 
            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "submittedAt"));
    
    org.springframework.data.domain.Page<DriverKyc> pendingKyc = 
        kycRepository.findByStatus(com.ridefast.ride_fast_backend.enums.KycStatus.PENDING, pageable);
    
    // Enrich with driver information and document URLs
    java.util.List<Map<String, Object>> enriched = pendingKyc.getContent().stream()
        .map(kyc -> {
          Map<String, Object> item = new HashMap<>();
          item.put("kyc", kyc);
          item.put("driver", Map.of(
              "id", kyc.getDriver().getId(),
              "name", kyc.getDriver().getName() != null ? kyc.getDriver().getName() : "",
              "email", kyc.getDriver().getEmail() != null ? kyc.getDriver().getEmail() : "",
              "mobile", kyc.getDriver().getMobile() != null ? kyc.getDriver().getMobile() : ""
          ));
          
          // Generate signed URLs for documents
          Map<String, String> documentUrls = new HashMap<>();
          if (kyc.getPhotoKey() != null && !kyc.getPhotoKey().isBlank()) {
            documentUrls.put("photo", signedUrlService.signDocument(kyc.getPhotoKey(), 60).toString());
          }
          if (kyc.getAadhaarFrontKey() != null && !kyc.getAadhaarFrontKey().isBlank()) {
            documentUrls.put("aadhaarFront", signedUrlService.signDocument(kyc.getAadhaarFrontKey(), 60).toString());
          }
          if (kyc.getAadhaarBackKey() != null && !kyc.getAadhaarBackKey().isBlank()) {
            documentUrls.put("aadhaarBack", signedUrlService.signDocument(kyc.getAadhaarBackKey(), 60).toString());
          }
          if (kyc.getLicenseFrontKey() != null && !kyc.getLicenseFrontKey().isBlank()) {
            documentUrls.put("licenseFront", signedUrlService.signDocument(kyc.getLicenseFrontKey(), 60).toString());
          }
          if (kyc.getLicenseBackKey() != null && !kyc.getLicenseBackKey().isBlank()) {
            documentUrls.put("licenseBack", signedUrlService.signDocument(kyc.getLicenseBackKey(), 60).toString());
          }
          if (kyc.getRcFrontKey() != null && !kyc.getRcFrontKey().isBlank()) {
            documentUrls.put("rcFront", signedUrlService.signDocument(kyc.getRcFrontKey(), 60).toString());
          }
          if (kyc.getRcBackKey() != null && !kyc.getRcBackKey().isBlank()) {
            documentUrls.put("rcBack", signedUrlService.signDocument(kyc.getRcBackKey(), 60).toString());
          }
          item.put("documentUrls", documentUrls);
          return item;
        })
        .collect(java.util.stream.Collectors.toList());
    
    Map<String, Object> response = new HashMap<>();
    response.put("content", enriched);
    response.put("totalElements", pendingKyc.getTotalElements());
    response.put("totalPages", pendingKyc.getTotalPages());
    response.put("currentPage", pendingKyc.getNumber());
    response.put("size", pendingKyc.getSize());
    
    return ResponseEntity.ok(response);
  }
  
  @PutMapping("/drivers/{driverId}/approve")
  public ResponseEntity<?> approveKyc(@PathVariable Long driverId) {
    Driver driver = driverRepository.findById(driverId)
        .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
    DriverKyc kyc = kycRepository.findByDriver(driver)
        .orElseThrow(() -> new IllegalArgumentException("KYC not found"));
    
    kyc.setStatus(com.ridefast.ride_fast_backend.enums.KycStatus.APPROVED);
    kyc.setReviewedAt(java.time.Instant.now());
    kyc.setRejectionReason(null);
    kycRepository.save(kyc);
    
    return ResponseEntity.ok(Map.of("status", "APPROVED", "message", "KYC approved successfully"));
  }
  
  @PutMapping("/drivers/{driverId}/reject")
  public ResponseEntity<?> rejectKyc(@PathVariable Long driverId, @RequestBody Map<String, String> body) {
    String reason = body.get("reason");
    if (reason == null || reason.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Rejection reason is required"));
    }
    
    Driver driver = driverRepository.findById(driverId)
        .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
    DriverKyc kyc = kycRepository.findByDriver(driver)
        .orElseThrow(() -> new IllegalArgumentException("KYC not found"));
    
    kyc.setStatus(com.ridefast.ride_fast_backend.enums.KycStatus.REJECTED);
    kyc.setReviewedAt(java.time.Instant.now());
    kyc.setRejectionReason(reason);
    kycRepository.save(kyc);
    
    return ResponseEntity.ok(Map.of("status", "REJECTED", "message", "KYC rejected", "reason", reason));
  }

  @DeleteMapping("/drivers/{driverId}/files/{name}")
  public ResponseEntity<Void> deleteKycFile(@PathVariable Long driverId, @PathVariable String name) {
    Driver driver = driverRepository.findById(driverId)
        .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
    DriverKyc kyc = kycRepository.findByDriver(driver)
        .orElseThrow(() -> new IllegalArgumentException("KYC not found"));

    String key = resolveKeyByName(kyc, name);
    if (key == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);

    String gs = buildGsPath(key);
    storageService.delete(gs);
    clearKeyByName(kyc, name);
    kycRepository.save(kyc);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @PutMapping(value = "/drivers/{driverId}/files/{name}", consumes = {"multipart/form-data"})
  public ResponseEntity<Map<String, String>> replaceKycFile(@PathVariable Long driverId,
                                                            @PathVariable String name,
                                                            @RequestPart("file") MultipartFile file) throws Exception {
    Driver driver = driverRepository.findById(driverId)
        .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
    DriverKyc kyc = kycRepository.findByDriver(driver)
        .orElseThrow(() -> new IllegalArgumentException("KYC not found"));

    String key = resolveKeyByName(kyc, name);
    if (key == null) {
      // if not present, create under drivers/{id}/kyc/<name>.jpg
      String base = "drivers/" + driverId + "/kyc/";
      key = base + name + ".jpg";
    }

    String gs = storageService.uploadDriverDocument(file.getBytes(), file.getContentType(), key);
    setKeyByName(kyc, name, key);
    kycRepository.save(kyc);

    Map<String, String> resp = new HashMap<>();
    resp.put("key", key);
    resp.put("gsPath", gs);
    return new ResponseEntity<>(resp, HttpStatus.OK);
  }

  @GetMapping("/drivers/{driverId}/files/{name}/download")
  public ResponseEntity<Map<String, String>> getSignedDownloadUrl(@PathVariable Long driverId,
                                                                  @PathVariable String name) {
    Driver driver = driverRepository.findById(driverId)
        .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
    DriverKyc kyc = kycRepository.findByDriver(driver)
        .orElseThrow(() -> new IllegalArgumentException("KYC not found"));

    String key = resolveKeyByName(kyc, name);
    if (key == null || key.isBlank()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    String url = signedUrlService.signDocument(key, 15).toString();
    Map<String, String> resp = new HashMap<>();
    resp.put("key", key);
    resp.put("url", url);
    resp.put("expiresInMinutes", "15");
    return ResponseEntity.ok(resp);
  }

  private String buildGsPath(String objectName) {
    String base = documentsGsPath == null ? "" : documentsGsPath;
    if (!base.endsWith("/")) base = base + "/";
    return base + objectName;
  }

  private String resolveKeyByName(DriverKyc kyc, String name) {
    return switch (name) {
      case "photo" -> kyc.getPhotoKey();
      case "aadhaar_front" -> kyc.getAadhaarFrontKey();
      case "aadhaar_back" -> kyc.getAadhaarBackKey();
      case "license_front" -> kyc.getLicenseFrontKey();
      case "license_back" -> kyc.getLicenseBackKey();
      case "rc_front" -> kyc.getRcFrontKey();
      case "rc_back" -> kyc.getRcBackKey();
      default -> null;
    };
  }

  private void clearKeyByName(DriverKyc kyc, String name) {
    switch (name) {
      case "photo" -> kyc.setPhotoKey(null);
      case "aadhaar_front" -> kyc.setAadhaarFrontKey(null);
      case "aadhaar_back" -> kyc.setAadhaarBackKey(null);
      case "license_front" -> kyc.setLicenseFrontKey(null);
      case "license_back" -> kyc.setLicenseBackKey(null);
      case "rc_front" -> kyc.setRcFrontKey(null);
      case "rc_back" -> kyc.setRcBackKey(null);
      default -> {}
    }
  }

  private void setKeyByName(DriverKyc kyc, String name, String key) {
    switch (name) {
      case "photo" -> kyc.setPhotoKey(key);
      case "aadhaar_front" -> kyc.setAadhaarFrontKey(key);
      case "aadhaar_back" -> kyc.setAadhaarBackKey(key);
      case "license_front" -> kyc.setLicenseFrontKey(key);
      case "license_back" -> kyc.setLicenseBackKey(key);
      case "rc_front" -> kyc.setRcFrontKey(key);
      case "rc_back" -> kyc.setRcBackKey(key);
      default -> {}
    }
  }
}
