package com.ridefast.ride_fast_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateBankDetailsRequest {
  private String accountHolderName;
  private String bankName;
  private String accountNumber;
  private String ifscCode;
  private String upiId;
  private String bankAddress;
  private String bankMobile;
}
