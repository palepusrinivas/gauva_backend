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
  @Column(name = "id",  nullable = false, length = 36)
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(name = "name", nullable = false, unique = true, length = 255)
  private String name;

  @Column(name = "readable_id", nullable = false, length = 64)
  private String readableId;

  @Column(name = "coordinates", columnDefinition = "polygon")
  private String coordinates;

  @Column(name = "is_active")
  private Boolean isActive;

  @Column(name = "extra_fare_status")
  private String extraFareStatus;

  @Column(name = "extra_fare_fee", nullable = false)
  private double extraFareFee;

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

  @Column(name = "active", columnDefinition = "bit(1)")
  private Boolean active;

  @Column(name = "polygon_wkt", columnDefinition = "TEXT")
  private String polygonWkt;
}
