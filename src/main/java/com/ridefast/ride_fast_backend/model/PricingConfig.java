package com.ridefast.ride_fast_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingConfig {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Auto
  private Integer autoBaseFare;           // e.g., 50
  private Integer autoPerKmFare;          // e.g., 15
  private Integer autoNightSurchargePercent; // 15 or 20
  private Integer autoPlatformFeeFlat;    // e.g., 7

  // Bike
  private Integer bikeCommissionPercent;      // 7-9
  private Integer bikeNightSurchargePercent;  // 15

  // Car
  private Integer carCommissionPercent;       // 7-10
  private Integer carNightSurchargePercent;   // 15-20

  // Common
  private Integer gstPercent;                 // 5
  private Integer firstRideCashbackPercent;   // 20/30/50
  private Integer walletUsageCapPercent;      // 10
  private Integer walletCreditValidityDays;   // 7

  // Night window
  private Integer nightStartHour;             // 22
  private Integer nightEndHour;               // 6

  // Payouts
  private Integer minPayoutAmount;            // default 0 if not set
  private Integer payoutFeeFlat;              // default 0
  private Integer payoutFeePercent;           // default 0

  private LocalDateTime updatedAt;

  @PrePersist
  public void onCreate() {
    this.updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  public void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
