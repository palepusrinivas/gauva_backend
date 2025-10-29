package com.ridefast.ride_fast_backend.dto.admin;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripFareUpsertRequest {
  private String zoneId;           // optional; else use zoneName
  private String zoneName;         // optional
  private String vehicleCategoryId; // optional; else use categoryType or categoryName
  private String categoryType;     // e.g., MEGA, SEDAN, etc.
  private String categoryName;     // optional

  private BigDecimal baseFare;
  private BigDecimal baseFarePerKm;
  private BigDecimal timeRatePerMinOverride; // optional
  private BigDecimal waitingFeePerMin;       // optional
  private BigDecimal cancellationFeePercent; // optional
  private BigDecimal minCancellationFee;     // optional
  private BigDecimal idleFeePerMin;          // optional
  private BigDecimal tripDelayFeePerMin;     // optional
  private BigDecimal penaltyFeeForCancel;    // optional
  private BigDecimal feeAddToNext;           // optional
}
