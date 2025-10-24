package com.ridefast.ride_fast_backend.service;

import com.ridefast.ride_fast_backend.enums.KycStatus;
import com.ridefast.ride_fast_backend.model.DriverKyc;
import java.util.Map;

public interface DriverKycService {
  Map<String, String> generateUploadKeys(Long driverId);

  DriverKyc submit(Long driverId, String aadhaarNumber, String licenseNumber, String rcNumber,
                   Map<String, String> fileKeys);

  DriverKyc get(Long driverId);

  DriverKyc approve(Long driverId);

  DriverKyc reject(Long driverId, String reason);

  static String maskAadhaar(String aadhaar) {
    if (aadhaar == null || aadhaar.length() < 4) return aadhaar;
    return "xxxx-xxxx-" + aadhaar.substring(aadhaar.length() - 4);
  }

  static boolean isValidAadhaar(String a) { return a != null && a.matches("^[0-9]{12}$"); }
  static boolean isValidLicense(String l) { return l != null && l.matches("^[A-Z]{2}[0-9]{2}[0-9A-Z]{6,14}$"); }
  static boolean isValidRc(String r) { return r != null && r.matches("^[0-9A-Z-]{6,20}$"); }
}
