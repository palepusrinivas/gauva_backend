package com.ridefast.ride_fast_backend.controller;

import com.ridefast.ride_fast_backend.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DriverKycUploadController {

  private final StorageService storageService;

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

    return new ResponseEntity<>(keys, HttpStatus.CREATED);
  }
}
