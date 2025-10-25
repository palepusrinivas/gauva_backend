package com.ridefast.ride_fast_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cancellation_fee_rule")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancellationFeeRule {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private PricingProfile pricingProfile;

  private Integer minElapsedMinutes;
  private Integer minDistanceMeters;
  private Double feeAmount;
  private String reason;

  @Column(nullable = false)
  private boolean active;
}
