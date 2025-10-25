package com.ridefast.ride_fast_backend.model.v2;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_fares")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripFare {
  @Id
  @Column(length = 36)
  private String id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "zone_id", nullable = false)
  private ZoneV2 zone;

  @ManyToOne(optional = false)
  @JoinColumn(name = "vehicle_category_id", nullable = false)
  private VehicleCategory vehicleCategory;

  @Column(name = "base_fare", nullable = false, precision = 8, scale = 2)
  private BigDecimal baseFare;

  @Column(name = "base_fare_per_km", nullable = false, precision = 8, scale = 2)
  private BigDecimal baseFarePerKm;

  @Column(name = "waiting_fee_per_min", nullable = false, precision = 8, scale = 2)
  private BigDecimal waitingFeePerMin;

  @Column(name = "cancellation_fee_percent", nullable = false, precision = 8, scale = 2)
  private BigDecimal cancellationFeePercent;

  @Column(name = "min_cancellation_fee", nullable = false, precision = 8, scale = 2)
  private BigDecimal minCancellationFee;

  @Column(name = "idle_fee_per_min", nullable = false, precision = 8, scale = 2)
  private BigDecimal idleFeePerMin;

  @Column(name = "trip_delay_fee_per_min", nullable = false, precision = 8, scale = 2)
  private BigDecimal tripDelayFeePerMin;

  @Column(name = "penalty_fee_for_cancel", nullable = false, precision = 8, scale = 2)
  private BigDecimal penaltyFeeForCancel;

  @Column(name = "fee_add_to_next", nullable = false, precision = 8, scale = 2)
  private BigDecimal feeAddToNext;

  @Column(name = "time_rate_per_min_override", precision = 8, scale = 2)
  private BigDecimal timeRatePerMinOverride;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}
