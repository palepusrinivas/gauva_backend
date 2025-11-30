package com.ridefast.ride_fast_backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpSendRequest {
  @NotEmpty(message = "Phone number is required")
  @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
  private String phoneNumber;
}

