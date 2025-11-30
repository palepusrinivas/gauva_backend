package com.ridefast.ride_fast_backend.dto;

import com.ridefast.ride_fast_backend.enums.UserRole;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpVerifyRequest {
  @NotEmpty(message = "Firebase ID token is required")
  private String idToken;

  @NotNull(message = "User role is required")
  private UserRole role;
}

