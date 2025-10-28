package com.ridefast.ride_fast_backend.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FcmTokenRequest {
  @NotEmpty(message = "token is required")
  private String token;
}
