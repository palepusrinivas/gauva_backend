package com.ridefast.ride_fast_backend.service.impl;

import com.ridefast.ride_fast_backend.enums.KycStatus;
import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.DriverKyc;
import com.ridefast.ride_fast_backend.repository.DriverKycRepository;
import com.ridefast.ride_fast_backend.repository.DriverRepository;
import com.ridefast.ride_fast_backend.service.DriverKycService;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class DriverKycServiceImpl implements DriverKycService {

  private final DriverRepository driverRepository;
  private final DriverKycRepository kycRepository;

  @Value("${app.storage.kyc-prefix:drivers}")
  private String kycRootPrefix;

  @Override
  public Map<String, String> generateUploadKeys(Long driverId) {
    // Only generate object keys (object names under Firebase 'documents' base path).
    String base = kycRootPrefix + "/" + driverId + "/kyc/";
    Map<String, String> keys = new HashMap<>();
    keys.put("photoKey", base + "photo.jpg");
    keys.put("aadhaarFrontKey", base + "aadhaar_front.jpg");
    keys.put("aadhaarBackKey", base + "aadhaar_back.jpg");
    keys.put("licenseFrontKey", base + "license_front.jpg");
    keys.put("licenseBackKey", base + "license_back.jpg");
    keys.put("rcFrontKey", base + "rc_front.jpg");
    keys.put("rcBackKey", base + "rc_back.jpg");
    return keys;
  }

  @Override
  @Transactional
  public DriverKyc submit(Long driverId, String aadhaarNumber, String licenseNumber, String rcNumber,
                          Map<String, String> fileKeys) {
    Driver driver = driverRepository.findById(driverId)
        .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

    if (!DriverKycService.isValidAadhaar(aadhaarNumber)) {
      throw new IllegalArgumentException("Invalid Aadhaar number");
    }
    if (!DriverKycService.isValidLicense(licenseNumber)) {
      throw new IllegalArgumentException("Invalid License number");
    }
    if (!DriverKycService.isValidRc(rcNumber)) {
      throw new IllegalArgumentException("Invalid RC number");
    }

    String base = kycRootPrefix + "/" + driverId + "/kyc/";
    for (String v : fileKeys.values()) {
      if (v == null || !v.startsWith(base)) {
        throw new IllegalArgumentException("File key outside allowed path: " + v);
      }
    }

    DriverKyc kyc = kycRepository.findByDriver(driver).orElse(DriverKyc.builder()
        .driver(driver)
        .build());
    kyc.setAadhaarNumber(aadhaarNumber);
    kyc.setLicenseNumber(licenseNumber);
    kyc.setRcNumber(rcNumber);
    kyc.setPhotoKey(fileKeys.get("photoKey"));
    kyc.setAadhaarFrontKey(fileKeys.get("aadhaarFrontKey"));
    kyc.setAadhaarBackKey(fileKeys.get("aadhaarBackKey"));
    kyc.setLicenseFrontKey(fileKeys.get("licenseFrontKey"));
    kyc.setLicenseBackKey(fileKeys.get("licenseBackKey"));
    kyc.setRcFrontKey(fileKeys.get("rcFrontKey"));
    kyc.setRcBackKey(fileKeys.get("rcBackKey"));
    kyc.setStatus(KycStatus.PENDING);
    kyc.setRejectionReason(null);
    kyc.setSubmittedAt(Instant.now());
    return kycRepository.save(kyc);
  }

  @Override
  public DriverKyc get(Long driverId) {
    Driver driver = driverRepository.findById(driverId)
        .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
    return kycRepository.findByDriver(driver)
        .orElseThrow(() -> new IllegalArgumentException("KYC not found"));
  }

  @Override
  @Transactional
  public DriverKyc approve(Long driverId) {
    DriverKyc kyc = get(driverId);
    kyc.setStatus(KycStatus.APPROVED);
    kyc.setReviewedAt(Instant.now());
    kyc.setRejectionReason(null);
    return kycRepository.save(kyc);
  }

  @Override
  @Transactional
  public DriverKyc reject(Long driverId, String reason) {
    DriverKyc kyc = get(driverId);
    kyc.setStatus(KycStatus.REJECTED);
    kyc.setReviewedAt(Instant.now());
    kyc.setRejectionReason(reason);
    return kycRepository.save(kyc);
  }
}
