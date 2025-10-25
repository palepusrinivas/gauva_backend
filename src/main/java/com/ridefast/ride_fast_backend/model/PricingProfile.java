package com.ridefast.ride_fast_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pricing_profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingProfile {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String currency;

  @Column(nullable = false)
  private double baseFare;

  @Column(nullable = false)
  private double perKmRate;

  @Column(nullable = false)
  private double timeRatePerMin;

  @Column(nullable = false)
  private boolean active;
}
