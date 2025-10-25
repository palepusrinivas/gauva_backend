package com.ridefast.ride_fast_backend.model;

import com.ridefast.ride_fast_backend.enums.ExtraFareStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "zone_fare_rule")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZoneFareRule {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private PricingProfile pricingProfile;

  @ManyToOne(optional = false)
  private Zone zone;

  private Double baseFareOverride;
  private Double perKmRateOverride;
  private Double timeRatePerMinOverride;

  @Enumerated(EnumType.STRING)
  private ExtraFareStatus extraFareStatus;

  private String extraFareReason;

  @Column(nullable = false)
  private boolean active;
}
