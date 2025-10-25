package com.ridefast.ride_fast_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "zone")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Zone {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 64)
  private String readableId; // e.g., HYD-AIRPORT

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String polygonWkt; // WKT polygon text; optional for now

  @Column(nullable = false)
  private boolean active;
}
