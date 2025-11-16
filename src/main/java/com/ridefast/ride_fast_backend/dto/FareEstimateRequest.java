package com.ridefast.ride_fast_backend.dto;

import com.ridefast.ride_fast_backend.enums.ServiceType;
import lombok.Data;

@Data
public class FareEstimateRequest {
  private ServiceType serviceType;
  private double distanceKm;
  private double durationMin;
  private Double pickupLat;
  private Double pickupLng;
  private Double dropLat;
  private Double dropLng;
  private String pickupZoneReadableId;
  private String dropZoneReadableId;
  // optional promotions
  private String couponCode;
  private Long userId;
}
