package com.ridefast.ride_fast_backend.service.impl;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ridefast.ride_fast_backend.dto.DriverSignUpRequest;
import com.ridefast.ride_fast_backend.dto.UpdateBankDetailsRequest;
import com.ridefast.ride_fast_backend.dto.UpdateDriverProfileRequest;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.enums.RideStatus;
import com.ridefast.ride_fast_backend.enums.UserRole;
import com.ridefast.ride_fast_backend.enums.VerificationStatus;
import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.DriverDetails;
import com.ridefast.ride_fast_backend.model.License;
import com.ridefast.ride_fast_backend.model.Ride;
import com.ridefast.ride_fast_backend.model.Vehicle;
import com.ridefast.ride_fast_backend.repository.DriverDetailsRepository;
import com.ridefast.ride_fast_backend.repository.DriverRepository;
import com.ridefast.ride_fast_backend.repository.LicenseRepository;
import com.ridefast.ride_fast_backend.repository.VehicleRepository;
import com.ridefast.ride_fast_backend.service.CalculatorService;
import com.ridefast.ride_fast_backend.service.DriverService;
import com.ridefast.ride_fast_backend.service.ShortCodeService;
import com.ridefast.ride_fast_backend.service.RealtimeService;
import com.ridefast.ride_fast_backend.util.JwtTokenHelper;
import com.ridefast.ride_fast_backend.model.DriverKyc;
import com.ridefast.ride_fast_backend.repository.DriverKycRepository;
import com.ridefast.ride_fast_backend.enums.KycStatus;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

  private final DriverRepository driverRepository;
  private final DriverDetailsRepository driverDetailsRepository;
  private final LicenseRepository licenseRepository;
  private final VehicleRepository vehicleRepository;
  private final DriverKycRepository driverKycRepository;

  private final CalculatorService calculatorService;
  private final JwtTokenHelper tokenHelper;
  private final RealtimeService realtimeService;

  private final PasswordEncoder passwordEncoder;
  private final ModelMapper modelMapper;
  private final ShortCodeService shortCodeService;

  @Override
  public Driver registerDriver(DriverSignUpRequest request) {
    // Support both nested objects and flat fields from Flutter app
    License license = request.getLicenseOrBuild();
    Vehicle vehicle = request.getVehicleOrBuild();
    
    // Create driver manually to avoid ModelMapper conflicts with *Id fields
    Driver driver = Driver.builder()
        .name(request.getName())
        .email(request.getEmail())
        .mobile(request.getMobile())
        .latitude(request.getLatitude())
        .longitude(request.getLongitude())
        .accountHolderName(request.getAccountHolderName())
        .bankName(request.getBankName())
        .accountNumber(request.getAccountNumber())
        .ifscCode(request.getIfscCode())
        .upiId(request.getUpiId())
        .bankAddress(request.getBankAddress())
        .bankMobile(request.getBankMobile())
        .build();
    
    driver.setPassword(passwordEncoder.encode(request.getPassword()));
    driver.setRole(UserRole.DRIVER);
    
    if (driver.getShortCode() == null || driver.getShortCode().isBlank()) {
      driver.setShortCode(shortCodeService.generateDriverCode());
    }
    
    // Save license and vehicle if provided
    License savedLicense = null;
    Vehicle savedVehicle = null;
    
    if (license != null) {
      savedLicense = licenseRepository.save(license);
      driver.setLicense(savedLicense);
    }
    
    if (vehicle != null) {
      savedVehicle = vehicleRepository.save(vehicle);
      driver.setVehicle(savedVehicle);
    }
    
    Driver savedDriver = driverRepository.save(driver);
    
    // Update back-references
    if (savedLicense != null) {
      savedLicense.setDriver(savedDriver);
      licenseRepository.save(savedLicense);
    }
    if (savedVehicle != null) {
      savedVehicle.setDriver(savedDriver);
      vehicleRepository.save(savedVehicle);
    }
    
    // Create initial KYC record with PENDING status
    DriverKyc kyc = DriverKyc.builder()
        .driver(savedDriver)
        .status(KycStatus.PENDING)
        .submittedAt(null) // Will be set when documents are uploaded
        .build();
    driverKycRepository.save(kyc);
    
    return savedDriver;
  }

  @Override
  public List<Driver> getAvailableDrivers(double pickupLatitude, double pickupLongitude, Ride ride) {
    List<Driver> allDrivers = driverRepository.findAll();
    List<Driver> availableDrivers = new ArrayList<>();
    for (Driver driver : allDrivers) {
      // Check if driver is online (primary check on Driver model)
      if (driver.getIsOnline() == null || !driver.getIsOnline()) {
        continue; // Skip offline drivers
      }
      
      // Check if driver has active ride
      if (driver.getCurrentRide() != null && driver.getCurrentRide().getStatus() != RideStatus.COMPLETED)
        continue;
      
      // Check if driver declined this ride
      if (ride.getDeclinedDrivers().contains(driver.getId()))
        continue;
      
      // double driverLatitude = driver.getLatitude();
      // double driverLongitude = driver.getLongitude();
      // double distance = calculatorService.calculateDistance(driverLatitude,
      // driverLongitude, pickupLatitude,
      // pickupLongitude);
      availableDrivers.add(driver);
    }
    return availableDrivers;
  }

  @Override
  public Driver getNearestDriver(List<Driver> availableDrivers, double pickupLatitude, double pickupLongitude) {
    double min = Double.MAX_VALUE;
    Driver nearestDriver = null;
    for (Driver driver : availableDrivers) {
      double driverLatitude = driver.getLatitude();
      double driverLongitude = driver.getLongitude();
      double distance = calculatorService.calculateDistance(driverLatitude,
          driverLongitude, pickupLatitude,
          pickupLongitude);
      if (min > distance) {
        min = distance;
        nearestDriver = driver;
      }
    }
    return nearestDriver;
  }

  @Override
  public Driver getRequestedDriverProfile(String jwtToken) throws ResourceNotFoundException {
    String email = tokenHelper.getUsernameFromToken(jwtToken);
    Driver driver = driverRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("Driver", "username", email));
    return driver;
  }

  @Override
  public List<Ride> getDriverCurrentRide(Long driverId) throws ResourceNotFoundException {
    // Driver driver = driverRepository.findById(driverId)
    // .orElseThrow(() -> new ResourceNotFoundException("driver", "id", driverId));
    List<Ride> currentRides = driverRepository.getCurrentRides(driverId);
    return currentRides;
  }

  @Override
  public List<Ride> getAllocatedRides(Long driverId) {
    return driverRepository.getAllocatedRides(driverId);
  }

  @Override
  public List<Ride> getCompletedRides(Long driverId) {
    return driverRepository.getCompletedRides(driverId);
  }

  @Override
  public List<Ride> getDriverStartedRide(String jwtToken) throws ResourceNotFoundException {
    String email = tokenHelper.getUsernameFromToken(jwtToken);
    Driver driver = driverRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("Driver", "username", email));
    return driverRepository.getstartedRides(driver.getId());
  }

  @Override
  public Driver updateBankDetails(String jwtToken, UpdateBankDetailsRequest request) throws ResourceNotFoundException {
    String email = tokenHelper.getUsernameFromToken(jwtToken);
    Driver driver = driverRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("Driver", "username", email));
    driver.setAccountHolderName(request.getAccountHolderName());
    driver.setBankName(request.getBankName());
    driver.setAccountNumber(request.getAccountNumber());
    driver.setIfscCode(request.getIfscCode());
    driver.setUpiId(request.getUpiId());
    driver.setBankAddress(request.getBankAddress());
    driver.setBankMobile(request.getBankMobile());

    // Reset verification on any change
    driver.setBankVerificationStatus(VerificationStatus.PENDING);
    driver.setBankVerificationNotes(null);
    driver.setBankVerifiedAt(null);
    return driverRepository.save(driver);
  }

  @Override
  public Driver updateProfile(String jwtToken, UpdateDriverProfileRequest request) throws ResourceNotFoundException {
    String email = tokenHelper.getUsernameFromToken(jwtToken);
    Driver driver = driverRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("Driver", "username", email));
    
    if (request.getName() != null && !request.getName().isBlank()) {
      driver.setName(request.getName());
    }
    if (request.getEmail() != null && !request.getEmail().isBlank()) {
      driver.setEmail(request.getEmail());
    }
    if (request.getMobile() != null && !request.getMobile().isBlank()) {
      driver.setMobile(request.getMobile());
    }
    
    return driverRepository.save(driver);
  }

  @Override
  public void changePassword(String jwtToken, String currentPassword, String newPassword) throws ResourceNotFoundException {
    String email = tokenHelper.getUsernameFromToken(jwtToken);
    Driver driver = driverRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("Driver", "username", email));
    
    // Verify current password
    if (driver.getPassword() == null || driver.getPassword().isBlank()) {
      throw new IllegalArgumentException("Cannot change password - no password set");
    }
    
    if (!passwordEncoder.matches(currentPassword, driver.getPassword())) {
      throw new IllegalArgumentException("Current password is incorrect");
    }
    
    // Validate new password
    if (newPassword.length() < 6) {
      throw new IllegalArgumentException("New password must be at least 6 characters");
    }
    
    // Update password
    driver.setPassword(passwordEncoder.encode(newPassword));
    driverRepository.save(driver);
  }

  @Override
  public void updateOnlineStatus(String jwtToken, boolean isOnline) throws ResourceNotFoundException {
    Driver driver = getRequestedDriverProfile(jwtToken);
    
    // Update online status directly on Driver
    driver.setIsOnline(isOnline);
    Driver savedDriver = driverRepository.save(driver);
    
    // Broadcast driver status update
    try {
      realtimeService.broadcastDriverStatusUpdate(savedDriver);
    } catch (Exception e) {
      log.error("Error broadcasting driver status update: {}", e.getMessage(), e);
    }
    
    // Also update DriverDetails if it exists (for backward compatibility)
    // Use driverId (String) field instead of user.id since MyUser.id is String
    try {
      if (driver.getShortCode() != null) {
        DriverDetails details = driverDetailsRepository.findByDriverId(driver.getShortCode()).orElse(null);
        if (details != null) {
          details.setIsOnline(isOnline ? "true" : "false");
          details.setAvailabilityStatus(isOnline ? "available" : "unavailable");
          if (isOnline) {
            details.setOnline(LocalTime.now());
          } else {
            details.setOffline(LocalTime.now());
          }
          driverDetailsRepository.save(details);
        }
      }
    } catch (Exception e) {
      // Ignore if DriverDetails doesn't exist or can't be updated
      // Driver status is already updated above
      log.warn("Could not update DriverDetails for driver {}: {}", driver.getId(), e.getMessage());
    }
  }

}
