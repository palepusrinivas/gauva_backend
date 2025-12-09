package com.ridefast.ride_fast_backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "zone")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Zone {
  @Id
  @Column(name = "id", nullable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false, unique = true, length = 255)
  private String name;

  @Column(name = "readable_id", nullable = false, length = 64)
  private String readableId;

  @Column(name = "coordinates", columnDefinition = "TEXT")
  private String coordinates;

  @Column(name = "is_active")
  @Builder.Default
  private Boolean isActive = true;

  @Column(name = "extra_fare_status")
  @Builder.Default
  private String extraFareStatus = "NONE";

  @Column(name = "extra_fare_fee")
  @Builder.Default
  private Double extraFareFee = 0.0;

  @Column(name = "extra_fare_reason")
  private String extraFareReason;

  @Column(name = "deleted_at")
  @UpdateTimestamp
  private LocalDateTime deletedAt;

  @Column(name = "created_at")
  @CreationTimestamp
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  @UpdateTimestamp
  private LocalDateTime updatedAt;

  @Column(name = "active")
  @Builder.Default
  private Boolean active = true;

  @Column(name = "polygon_wkt", columnDefinition = "TEXT")
  private String polygonWkt;
}
