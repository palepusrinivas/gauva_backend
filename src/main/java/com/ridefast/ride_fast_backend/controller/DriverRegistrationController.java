package com.ridefast.ride_fast_backend.controller;

import com.ridefast.ride_fast_backend.dto.DriverResponse;
import com.ridefast.ride_fast_backend.enums.KycStatus;
import com.ridefast.ride_fast_backend.enums.UserRole;
import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.DriverKyc;
import com.ridefast.ride_fast_backend.model.License;
import com.ridefast.ride_fast_backend.model.Vehicle;
import com.ridefast.ride_fast_backend.repository.DriverKycRepository;
import com.ridefast.ride_fast_backend.repository.DriverRepository;
import com.ridefast.ride_fast_backend.repository.LicenseRepository;
import com.ridefast.ride_fast_backend.repository.VehicleRepository;
import com.ridefast.ride_fast_backend.service.ShortCodeService;
import com.ridefast.ride_fast_backend.service.storage.StorageService;
import com.ridefast.ride_fast_backend.util.JwtTokenHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class DriverRegistrationController {
    
    private static final String ENDPOINT_PATH = "/register/driver/documents";

    private final DriverRepository driverRepository;
    private final DriverKycRepository driverKycRepository;
    private final LicenseRepository licenseRepository;
    private final VehicleRepository vehicleRepository;
    private final StorageService storageService;
    private final PasswordEncoder passwordEncoder;
    private final ShortCodeService shortCodeService;
    private final JwtTokenHelper jwtTokenHelper;

    @PostMapping(value = "/register/driver/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerDriverWithDocuments(
            // Basic driver info
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("mobile") String mobile,
            @RequestParam(value = "latitude", defaultValue = "0") double latitude,
            @RequestParam(value = "longitude", defaultValue = "0") double longitude,
            
            // Vehicle info
            @RequestParam(value = "vehicleType", required = false) String vehicleType,
            @RequestParam(value = "vehicleNumber", required = false) String vehicleNumber,
            @RequestParam(value = "vehicleColor", required = false) String vehicleColor,
            @RequestParam(value = "vehicleModel", required = false) String vehicleModel,
            
            // License info
            @RequestParam(value = "licenseNumber", required = false) String licenseNumber,
            
            // Aadhaar info
            @RequestParam(value = "aadhaarNumber", required = false) String aadhaarNumber,
            
            // RC info
            @RequestParam(value = "rcNumber", required = false) String rcNumber,
            
            // Bank details (optional)
            @RequestParam(value = "accountHolderName", required = false) String accountHolderName,
            @RequestParam(value = "bankName", required = false) String bankName,
            @RequestParam(value = "accountNumber", required = false) String accountNumber,
            @RequestParam(value = "ifscCode", required = false) String ifscCode,
            @RequestParam(value = "upiId", required = false) String upiId,
            
            // Document files
            @RequestPart(value = "profilePhoto", required = false) MultipartFile profilePhoto,
            @RequestPart(value = "licenseFront", required = false) MultipartFile licenseFront,
            @RequestPart(value = "licenseBack", required = false) MultipartFile licenseBack,
            @RequestPart(value = "rcFront", required = false) MultipartFile rcFront,
            @RequestPart(value = "rcBack", required = false) MultipartFile rcBack,
            @RequestPart(value = "aadhaarFront", required = false) MultipartFile aadhaarFront,
            @RequestPart(value = "aadhaarBack", required = false) MultipartFile aadhaarBack
    ) {
        try {
            log.info("Registering driver with documents: name={}, email={}, mobile={}", name, email, mobile);
            
            // Check if email already exists
            if (driverRepository.findByEmail(email).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
            }
            
            // Check if mobile already exists
            if (driverRepository.findByMobile(mobile).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Mobile number already registered"));
            }
            
            // Create Driver
            Driver driver = Driver.builder()
                    .name(name)
                    .email(email)
                    .mobile(mobile)
                    .password(passwordEncoder.encode(password))
                    .latitude(latitude)
                    .longitude(longitude)
                    .role(UserRole.DRIVER)
                    .shortCode(shortCodeService.generateDriverCode())
                    .accountHolderName(accountHolderName)
                    .bankName(bankName)
                    .accountNumber(accountNumber)
                    .ifscCode(ifscCode)
                    .upiId(upiId)
                    .build();
            
            // Create Vehicle if provided
            if (vehicleNumber != null || vehicleType != null) {
                Vehicle vehicle = Vehicle.builder()
                        .vehicleId(vehicleNumber)
                        .licensePlate(vehicleNumber)
                        .model(vehicleModel != null ? vehicleModel : vehicleType)
                        .color(vehicleColor)
                        .build();
                vehicle = vehicleRepository.save(vehicle);
                driver.setVehicle(vehicle);
            }
            
            // Create License if provided
            if (licenseNumber != null) {
                License license = License.builder()
                        .licenseNumber(licenseNumber)
                        .build();
                license = licenseRepository.save(license);
                driver.setLicense(license);
            }
            
            // Save driver first to get ID
            driver = driverRepository.save(driver);
            final Long driverId = driver.getId();
            
            // Update vehicle and license with driver reference
            if (driver.getVehicle() != null) {
                driver.getVehicle().setDriver(driver);
                vehicleRepository.save(driver.getVehicle());
            }
            if (driver.getLicense() != null) {
                driver.getLicense().setDriver(driver);
                licenseRepository.save(driver.getLicense());
            }
            
            // Upload documents and create KYC record
            DriverKyc kyc = DriverKyc.builder()
                    .driver(driver)
                    .aadhaarNumber(aadhaarNumber)
                    .licenseNumber(licenseNumber)
                    .rcNumber(rcNumber)
                    .status(KycStatus.PENDING)
                    .submittedAt(Instant.now())
                    .build();
            
            // Upload documents to Firebase Storage
            String basePath = "documents/drivers/" + driverId;
            
            if (profilePhoto != null && !profilePhoto.isEmpty()) {
                String photoKey = uploadDocument(profilePhoto, basePath + "/profile");
                kyc.setPhotoKey(photoKey);
                log.info("Uploaded profile photo: {}", photoKey);
            }
            
            if (licenseFront != null && !licenseFront.isEmpty()) {
                String key = uploadDocument(licenseFront, basePath + "/license_front");
                kyc.setLicenseFrontKey(key);
                log.info("Uploaded license front: {}", key);
            }
            
            if (licenseBack != null && !licenseBack.isEmpty()) {
                String key = uploadDocument(licenseBack, basePath + "/license_back");
                kyc.setLicenseBackKey(key);
                log.info("Uploaded license back: {}", key);
            }
            
            if (rcFront != null && !rcFront.isEmpty()) {
                String key = uploadDocument(rcFront, basePath + "/rc_front");
                kyc.setRcFrontKey(key);
                log.info("Uploaded RC front: {}", key);
            }
            
            if (rcBack != null && !rcBack.isEmpty()) {
                String key = uploadDocument(rcBack, basePath + "/rc_back");
                kyc.setRcBackKey(key);
                log.info("Uploaded RC back: {}", key);
            }
            
            if (aadhaarFront != null && !aadhaarFront.isEmpty()) {
                String key = uploadDocument(aadhaarFront, basePath + "/aadhaar_front");
                kyc.setAadhaarFrontKey(key);
                log.info("Uploaded Aadhaar front: {}", key);
            }
            
            if (aadhaarBack != null && !aadhaarBack.isEmpty()) {
                String key = uploadDocument(aadhaarBack, basePath + "/aadhaar_back");
                kyc.setAadhaarBackKey(key);
                log.info("Uploaded Aadhaar back: {}", key);
            }
            
            // Save KYC record
            driverKycRepository.save(kyc);
            
            // Generate JWT token
            String token = jwtTokenHelper.generateToken(driver.getEmail());
            
            // Build comprehensive response
            Map<String, Object> response = new HashMap<>();
            
            // Driver basic info
            response.put("id", driver.getId());
            response.put("name", driver.getName());
            response.put("email", driver.getEmail());
            response.put("mobile", driver.getMobile());
            response.put("role", driver.getRole() != null ? driver.getRole().name() : "DRIVER");
            response.put("shortCode", driver.getShortCode());
            response.put("rating", driver.getRating() != null ? driver.getRating() : 0.0);
            response.put("latitude", driver.getLatitude());
            response.put("longitude", driver.getLongitude());
            
            // Vehicle info
            if (driver.getVehicle() != null) {
                Map<String, Object> vehicleInfo = new HashMap<>();
                vehicleInfo.put("id", driver.getVehicle().getId());
                vehicleInfo.put("model", driver.getVehicle().getModel());
                vehicleInfo.put("licensePlate", driver.getVehicle().getLicensePlate());
                vehicleInfo.put("color", driver.getVehicle().getColor());
                response.put("vehicle", vehicleInfo);
            }
            
            // License info
            if (driver.getLicense() != null) {
                Map<String, Object> licenseInfo = new HashMap<>();
                licenseInfo.put("id", driver.getLicense().getId());
                licenseInfo.put("licenseNumber", driver.getLicense().getLicenseNumber());
                licenseInfo.put("licenseState", driver.getLicense().getLicenseState());
                response.put("license", licenseInfo);
            }
            
            // Bank details
            if (driver.getAccountHolderName() != null || driver.getUpiId() != null) {
                Map<String, Object> bankInfo = new HashMap<>();
                bankInfo.put("accountHolderName", driver.getAccountHolderName());
                bankInfo.put("bankName", driver.getBankName());
                bankInfo.put("accountNumber", driver.getAccountNumber());
                bankInfo.put("ifscCode", driver.getIfscCode());
                bankInfo.put("upiId", driver.getUpiId());
                response.put("bankDetails", bankInfo);
            }
            
            // KYC status
            Map<String, Object> kycInfo = new HashMap<>();
            kycInfo.put("status", kyc.getStatus().name());
            kycInfo.put("submittedAt", kyc.getSubmittedAt());
            kycInfo.put("aadhaarNumber", kyc.getAadhaarNumber());
            kycInfo.put("licenseNumber", kyc.getLicenseNumber());
            kycInfo.put("rcNumber", kyc.getRcNumber());
            response.put("kyc", kycInfo);
            
            // Auth token
            response.put("accessToken", token);
            response.put("tokenType", "Bearer");
            
            // Success message
            response.put("message", "Driver registered successfully. Documents submitted for verification.");
            response.put("success", true);
            
            log.info("Driver registered successfully: id={}, email={}", driver.getId(), driver.getEmail());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("Error registering driver: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }
    
    private String uploadDocument(MultipartFile file, String basePath) {
        try {
            String extension = getFileExtension(file.getOriginalFilename());
            String fileName = basePath + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            return storageService.uploadFile(file, fileName);
        } catch (Exception e) {
            log.error("Failed to upload document: {}", e.getMessage());
            return null;
        }
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}

