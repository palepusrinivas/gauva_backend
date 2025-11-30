package com.ridefast.ride_fast_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OtpSendResponse {
  private String message;
  private boolean success;
  private String phoneNumber;
}

