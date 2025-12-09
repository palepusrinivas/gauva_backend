package com.ridefast.ride_fast_backend.controller;

import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.DriverKyc;
import com.ridefast.ride_fast_backend.repository.DriverKycRepository;
import com.ridefast.ride_fast_backend.repository.DriverRepository;
import com.ridefast.ride_fast_backend.service.storage.StorageService;
import com.ridefast.ride_fast_backend.enums.KycStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class DriverKycUploadController {

  private final StorageService storageService;
  private final DriverRepository driverRepository;
  private final DriverKycRepository driverKycRepository;

  @PostMapping(value = "/drivers/{driverId}/kyc/files", consumes = {"multipart/form-data"})
  public ResponseEntity<Map<String, String>> uploadKycFiles(
      @PathVariable("driverId") Long driverId,
      @RequestPart(value = "photo", required = false) MultipartFile photo,
      @RequestPart(value = "aadhaarFront", required = false) MultipartFile aadhaarFront,
      @RequestPart(value = "aadhaarBack", required = false) MultipartFile aadhaarBack,
      @RequestPart(value = "licenseFront", required = false) MultipartFile licenseFront,
      @RequestPart(value = "licenseBack", required = false) MultipartFile licenseBack,
      @RequestPart(value = "rcFront", required = false) MultipartFile rcFront,
      @RequestPart(value = "rcBack", required = false) MultipartFile rcBack
  ) throws Exception {
    String base = "drivers/" + driverId + "/kyc/";
    Map<String, String> keys = new HashMap<>();

    if (photo != null && !photo.isEmpty()) {
      String object = base + "photo.jpg";
      String gs = storageService.uploadDriverDocument(photo.getBytes(), photo.getContentType(), object);
      keys.put("photoKey", object);
      keys.put("photoGsPath", gs);
    }
    if (aadhaarFront != null && !aadhaarFront.isEmpty()) {
      String object = base + "aadhaar_front.jpg";
      String gs = storageService.uploadDriverDocument(aadhaarFront.getBytes(), aadhaarFront.getContentType(), object);
      keys.put("aadhaarFrontKey", object);
      keys.put("aadhaarFrontGsPath", gs);
    }
    if (aadhaarBack != null && !aadhaarBack.isEmpty()) {
      String object = base + "aadhaar_back.jpg";
      String gs = storageService.uploadDriverDocument(aadhaarBack.getBytes(), aadhaarBack.getContentType(), object);
      keys.put("aadhaarBackKey", object);
      keys.put("aadhaarBackGsPath", gs);
    }
    if (licenseFront != null && !licenseFront.isEmpty()) {
      String object = base + "license_front.jpg";
      String gs = storageService.uploadDriverDocument(licenseFront.getBytes(), licenseFront.getContentType(), object);
      keys.put("licenseFrontKey", object);
      keys.put("licenseFrontGsPath", gs);
    }
    if (licenseBack != null && !licenseBack.isEmpty()) {
      String object = base + "license_back.jpg";
      String gs = storageService.uploadDriverDocument(licenseBack.getBytes(), licenseBack.getContentType(), object);
      keys.put("licenseBackKey", object);
      keys.put("licenseBackGsPath", gs);
    }
    if (rcFront != null && !rcFront.isEmpty()) {
      String object = base + "rc_front.jpg";
      String gs = storageService.uploadDriverDocument(rcFront.getBytes(), rcFront.getContentType(), object);
      keys.put("rcFrontKey", object);
      keys.put("rcFrontGsPath", gs);
    }
    if (rcBack != null && !rcBack.isEmpty()) {
      String object = base + "rc_back.jpg";
      String gs = storageService.uploadDriverDocument(rcBack.getBytes(), rcBack.getContentType(), object);
      keys.put("rcBackKey", object);
      keys.put("rcBackGsPath", gs);
    }

    // Update DriverKyc entity with document keys
    Driver driver = driverRepository.findById(driverId)
        .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
    
    DriverKyc kyc = driverKycRepository.findByDriver(driver)
        .orElse(DriverKyc.builder()
            .driver(driver)
            .status(KycStatus.PENDING)
            .build());
    
    // Update document keys
    if (keys.containsKey("photoKey")) {
      kyc.setPhotoKey(keys.get("photoKey"));
    }
    if (keys.containsKey("aadhaarFrontKey")) {
      kyc.setAadhaarFrontKey(keys.get("aadhaarFrontKey"));
    }
    if (keys.containsKey("aadhaarBackKey")) {
      kyc.setAadhaarBackKey(keys.get("aadhaarBackKey"));
    }
    if (keys.containsKey("licenseFrontKey")) {
      kyc.setLicenseFrontKey(keys.get("licenseFrontKey"));
    }
    if (keys.containsKey("licenseBackKey")) {
      kyc.setLicenseBackKey(keys.get("licenseBackKey"));
    }
    if (keys.containsKey("rcFrontKey")) {
      kyc.setRcFrontKey(keys.get("rcFrontKey"));
    }
    if (keys.containsKey("rcBackKey")) {
      kyc.setRcBackKey(keys.get("rcBackKey"));
    }
    
    // Set submitted timestamp if not already set
    if (kyc.getSubmittedAt() == null) {
      kyc.setSubmittedAt(Instant.now());
    }
    
    // Set status to PENDING if documents are uploaded
    if (kyc.getStatus() == null) {
      kyc.setStatus(KycStatus.PENDING);
    }
    
    driverKycRepository.save(kyc);
    log.info("Updated KYC documents for driver: {}", driverId);

    return new ResponseEntity<>(keys, HttpStatus.CREATED);
  }
}
