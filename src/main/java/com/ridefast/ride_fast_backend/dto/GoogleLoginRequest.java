package com.ridefast.ride_fast_backend.dto;

import com.ridefast.ride_fast_backend.enums.UserRole;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Google social login
 * Google login is only available for users (NORMAL_USER), not drivers
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoogleLoginRequest {
  @NotEmpty(message = "Firebase ID token is required")
  private String idToken;
  
  // Optional fields that can be provided if not available in Firebase token
  private String name;
  private String email;
  private String phone;
}
