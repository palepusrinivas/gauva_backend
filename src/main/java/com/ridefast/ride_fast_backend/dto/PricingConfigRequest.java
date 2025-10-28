package com.ridefast.ride_fast_backend.dto;

import lombok.Data;

@Data
public class PricingConfigRequest {
  // Auto
  private Integer autoBaseFare;
  private Integer autoPerKmFare;
  private Integer autoNightSurchargePercent;
  private Integer autoPlatformFeeFlat;

  // Bike
  private Integer bikeCommissionPercent;
  private Integer bikeNightSurchargePercent;

  // Car
  private Integer carCommissionPercent;
  private Integer carNightSurchargePercent;

  // Common
  private Integer gstPercent;
  private Integer firstRideCashbackPercent;
  private Integer walletUsageCapPercent;
  private Integer walletCreditValidityDays;

  // Night window
  private Integer nightStartHour;
  private Integer nightEndHour;

  // Payouts
  private Integer minPayoutAmount;
  private Integer payoutFeeFlat;
  private Integer payoutFeePercent;
}
