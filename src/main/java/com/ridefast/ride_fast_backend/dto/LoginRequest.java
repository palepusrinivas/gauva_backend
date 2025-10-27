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
public class LoginRequest {

  @NotEmpty(message = "identifier (email or phone) is required")
  private String identifier;

  @NotEmpty(message = "password is required")
  private String password;

  @NotNull(message = "user role is required")
  private UserRole role;
}
