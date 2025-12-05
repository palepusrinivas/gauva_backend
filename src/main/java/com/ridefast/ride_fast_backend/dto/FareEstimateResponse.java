package com.ridefast.ride_fast_backend.dto;

import com.ridefast.ride_fast_backend.enums.ExtraFareStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
  
  // Vehicle/Service info
  private VehicleInfo vehicle;
  
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class VehicleInfo {
    private String serviceId;      // e.g., "BIKE", "CAR"
    private String name;           // e.g., "Bike Taxi"
    private String displayName;    // e.g., "Bike Taxi"
    private String icon;           // Emoji icon e.g., "🏍️"
    private String iconUrl;        // URL to vehicle image
    private Integer capacity;      // Passenger capacity
    private String vehicleType;    // "two_wheeler", "four_wheeler"
    private String category;       // "economy", "premium"
    private String estimatedArrival; // "2-5 mins"
    private String description;
  }
}
