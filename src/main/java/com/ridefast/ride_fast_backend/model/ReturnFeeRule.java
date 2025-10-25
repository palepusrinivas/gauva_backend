package com.ridefast.ride_fast_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "return_fee_rule")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnFeeRule {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private PricingProfile pricingProfile;

  @Column(nullable = false)
  private boolean applyOnOutOfZoneDrop;

  private Double returnFeeAmount; // flat fee now; can add perKmBack later
  private String reason;

  @Column(nullable = false)
  private boolean active;
}
