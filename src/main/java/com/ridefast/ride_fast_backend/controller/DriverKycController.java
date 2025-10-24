package com.ridefast.ride_fast_backend.controller;

import com.ridefast.ride_fast_backend.enums.KycStatus;
import com.ridefast.ride_fast_backend.model.DriverKyc;
import com.ridefast.ride_fast_backend.service.DriverKycService;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DriverKycController {

  private final DriverKycService kycService;

  @PostMapping("/drivers/{driverId}/kyc/upload-keys")
  public ResponseEntity<Map<String, String>> generateUploadKeys(@PathVariable("driverId") Long driverId) {
    return new ResponseEntity<>(kycService.generateUploadKeys(driverId), HttpStatus.OK);
  }

  @PostMapping("/drivers/{driverId}/kyc/submit")
  public ResponseEntity<DriverKyc> submit(@PathVariable("driverId") Long driverId,
                                          @RequestBody SubmitRequest req) {
    DriverKyc saved = kycService.submit(driverId,
        req.getAadhaarNumber(), req.getLicenseNumber(), req.getRcNumber(), req.getFileKeys());
    return new ResponseEntity<>(saved, HttpStatus.CREATED);
  }

  @GetMapping("/drivers/{driverId}/kyc")
  public ResponseEntity<DriverKycResponse> get(@PathVariable("driverId") Long driverId) {
    DriverKyc kyc = kycService.get(driverId);
    DriverKycResponse resp = new DriverKycResponse();
    resp.setDriverId(driverId);
    resp.setStatus(kyc.getStatus());
    resp.setAadhaarMasked(DriverKycService.maskAadhaar(kyc.getAadhaarNumber()));
    resp.setLicenseNumber(kyc.getLicenseNumber());
    resp.setRcNumber(kyc.getRcNumber());
    resp.setFileKeys(Map.of(
        "photoKey", kyc.getPhotoKey(),
        "aadhaarFrontKey", kyc.getAadhaarFrontKey(),
        "aadhaarBackKey", kyc.getAadhaarBackKey(),
        "licenseFrontKey", kyc.getLicenseFrontKey(),
        "licenseBackKey", kyc.getLicenseBackKey(),
        "rcFrontKey", kyc.getRcFrontKey(),
        "rcBackKey", kyc.getRcBackKey()
    ));
    return new ResponseEntity<>(resp, HttpStatus.OK);
  }

  // Admin endpoints (simple scaffolding)
  @PostMapping("/admin/kyc/{driverId}/approve")
  public ResponseEntity<DriverKyc> approve(@PathVariable("driverId") Long driverId) {
    return new ResponseEntity<>(kycService.approve(driverId), HttpStatus.OK);
  }

  @PostMapping("/admin/kyc/{driverId}/reject")
  public ResponseEntity<DriverKyc> reject(@PathVariable("driverId") Long driverId,
                                          @RequestBody RejectRequest req) {
    return new ResponseEntity<>(kycService.reject(driverId, req.getReason()), HttpStatus.OK);
  }

  @Data
  public static class SubmitRequest {
    private String aadhaarNumber;
    private String licenseNumber;
    private String rcNumber;
    private Map<String, String> fileKeys;
  }

  @Data
  public static class DriverKycResponse {
    private Long driverId;
    private KycStatus status;
    private String aadhaarMasked;
    private String licenseNumber;
    private String rcNumber;
    private Map<String, String> fileKeys;
  }

  @Data
  public static class RejectRequest { private String reason; }
}
