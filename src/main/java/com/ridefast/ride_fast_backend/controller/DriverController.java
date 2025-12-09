package com.ridefast.ride_fast_backend.controller;

import java.util.List;
import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ridefast.ride_fast_backend.dto.ChangePasswordRequest;
import com.ridefast.ride_fast_backend.dto.DriverResponse;
import com.ridefast.ride_fast_backend.dto.RideDto;
import com.ridefast.ride_fast_backend.exception.ResourceNotFoundException;
import com.ridefast.ride_fast_backend.dto.UpdateBankDetailsRequest;
import com.ridefast.ride_fast_backend.dto.UpdateDriverProfileRequest;
import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.Ride;
import com.ridefast.ride_fast_backend.service.DriverService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/driver")
@RequiredArgsConstructor
public class DriverController {

  private final DriverService driverService;
  private final ModelMapper modelMapper;

  @GetMapping("/profile")
  public ResponseEntity<DriverResponse> getRequestedDriverProfileHandler(
      @RequestHeader("Authorization") String jwtToken) throws ResourceNotFoundException {
    Driver driver = driverService.getRequestedDriverProfile(jwtToken);
    DriverResponse response = modelMapper.map(driver, DriverResponse.class);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @PutMapping("/profile")
  public ResponseEntity<DriverResponse> updateDriverProfile(
      @RequestHeader("Authorization") String jwtToken,
      @RequestBody UpdateDriverProfileRequest request) throws ResourceNotFoundException {
    Driver updatedDriver = driverService.updateProfile(jwtToken, request);
    DriverResponse response = modelMapper.map(updatedDriver, DriverResponse.class);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping("/{driverId}/current_ride")
  public ResponseEntity<List<RideDto>> getDriverCurrentRideHandler(@RequestHeader("Authorization") String jwtToken,
      @PathVariable long driverId)
      throws ResourceNotFoundException {
    Driver driver = driverService.getRequestedDriverProfile(jwtToken);
    List<Ride> driverCurrentRide = driverService.getDriverCurrentRide(driver.getId());
    List<RideDto> list = driverCurrentRide.stream().map((ride -> modelMapper.map(ride, RideDto.class))).toList();

    return new ResponseEntity<>(list, HttpStatus.OK);
  }

  @GetMapping("/rides/started")
  public ResponseEntity<List<RideDto>> getDriverStartedRideHandler(@RequestHeader("Authorization") String jwtToken)
      throws ResourceNotFoundException {
    List<Ride> driverStartedRide = driverService.getDriverStartedRide(jwtToken);
    List<RideDto> list = driverStartedRide.stream().map((ride -> modelMapper.map(ride, RideDto.class))).toList();

    return new ResponseEntity<>(list, HttpStatus.OK);
  }

  @GetMapping("/rides/allocated")
  public ResponseEntity<List<RideDto>> getAllocatedRideHandler(@RequestHeader("Authorization") String jwtToken)
      throws ResourceNotFoundException {
    Driver driver = driverService.getRequestedDriverProfile(jwtToken);
    List<Ride> allocatedRides = driverService.getAllocatedRides(driver.getId());
    List<RideDto> rides = allocatedRides
        .stream()
        .map((ride) -> modelMapper.map(ride, RideDto.class)).toList();
    return new ResponseEntity<>(rides, HttpStatus.OK);
  }

  @GetMapping("/rides/completed")
  public ResponseEntity<List<RideDto>> getCompletedRideHandler(@RequestHeader("Authorization") String jwtToken)
      throws ResourceNotFoundException {
    Driver driver = driverService.getRequestedDriverProfile(jwtToken);
    List<Ride> completedRides = driverService.getCompletedRides(driver.getId());
    List<RideDto> rides = completedRides
        .stream()
        .map((ride) -> modelMapper.map(ride, RideDto.class)).toList();
    return new ResponseEntity<>(rides, HttpStatus.OK);
  }

  @PutMapping("/bank")
  public ResponseEntity<DriverResponse> updateBankDetails(
      @RequestHeader("Authorization") String jwtToken,
      @RequestBody UpdateBankDetailsRequest request) throws ResourceNotFoundException {
    Driver updated = driverService.updateBankDetails(jwtToken, request);
    DriverResponse response = modelMapper.map(updated, DriverResponse.class);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @PostMapping("/change-password")
  public ResponseEntity<Map<String, Object>> changePassword(
      @RequestHeader("Authorization") String jwtToken,
      @Valid @RequestBody ChangePasswordRequest request) throws ResourceNotFoundException {
    
    // Validate passwords match
    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
      return ResponseEntity.badRequest().body(Map.of(
          "success", false,
          "message", "New password and confirm password do not match"
      ));
    }
    
    try {
      driverService.changePassword(jwtToken, request.getCurrentPassword(), request.getNewPassword());
      return ResponseEntity.ok(Map.of(
          "success", true,
          "message", "Password changed successfully"
      ));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of(
          "success", false,
          "message", e.getMessage()
      ));
    }
  }

  @PutMapping("/status/online")
  public ResponseEntity<Map<String, Object>> setOnlineStatus(
      @RequestHeader("Authorization") String jwtToken,
      @RequestBody Map<String, Boolean> request) throws ResourceNotFoundException {
    
    Boolean isOnline = request.get("isOnline");
    if (isOnline == null) {
      return ResponseEntity.badRequest().body(Map.of(
          "success", false,
          "message", "isOnline field is required"
      ));
    }
    
    driverService.updateOnlineStatus(jwtToken, isOnline);
    
    return ResponseEntity.ok(Map.of(
        "success", true,
        "isOnline", isOnline,
        "message", isOnline ? "Driver is now online" : "Driver is now offline"
    ));
  }
}
