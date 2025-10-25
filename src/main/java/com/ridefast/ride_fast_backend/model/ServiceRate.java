package com.ridefast.ride_fast_backend.model;

import com.ridefast.ride_fast_backend.enums.ServiceType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service_rate",
    uniqueConstraints = @UniqueConstraint(columnNames = {"pricing_profile_id", "serviceType"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRate {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private PricingProfile pricingProfile;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private ServiceType serviceType;

  @Column(nullable = false)
  private double baseFare;

  @Column(nullable = false)
  private double perKmRate;

  // Optional override; if 0, engine can fall back to PricingProfile.timeRatePerMin
  @Column(nullable = false)
  private double timeRatePerMin;
}
