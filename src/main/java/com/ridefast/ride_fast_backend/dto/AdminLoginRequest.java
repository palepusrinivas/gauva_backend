package com.ridefast.ride_fast_backend.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminLoginRequest {
  @NotEmpty(message = "username is required")
  private String username;

  @NotEmpty(message = "password is required")
  private String password;
}
