package com.ridefast.ride_fast_backend.dto;

import com.ridefast.ride_fast_backend.enums.UserRole;
import com.ridefast.ride_fast_backend.enums.VerificationStatus;
import com.ridefast.ride_fast_backend.model.License;
import com.ridefast.ride_fast_backend.model.Vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DriverResponse {
  private Long id;
  private String name;
  private String email;
  private String mobile;
  private double rating;
  private double latitude;
  private double longitude;
  private License license;
  private UserRole role;
  private Vehicle vehicle;
  private Long totalRevenue;
  private String accountHolderName;
  private String bankName;
  private String accountNumber;
  private String ifscCode;
  private String upiId;
  private String bankAddress;
  private String bankMobile;
  private VerificationStatus bankVerificationStatus;
  private String bankVerificationNotes;
  private LocalDateTime bankVerifiedAt;
}