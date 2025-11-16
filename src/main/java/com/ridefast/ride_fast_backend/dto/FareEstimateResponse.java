package com.ridefast.ride_fast_backend.dto;

import com.ridefast.ride_fast_backend.enums.ExtraFareStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FareEstimateResponse {
  private String currency;
  private double baseFare;
  private double distanceKm;
  private double perKmRate;
  private double distanceFare;
  private double durationMin;
  private double timeRatePerMin;
  private double timeFare;
  private double cancellationFee;
  private double returnFee;
  private double total;
  private double discount;
  private double finalTotal;
  private String appliedCoupon;
  private ExtraFareStatus extraFareStatus;
  private String extraFareReason;
}
