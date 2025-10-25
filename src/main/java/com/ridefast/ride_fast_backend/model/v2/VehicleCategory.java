package com.ridefast.ride_fast_backend.model.v2;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleCategory {
  @Id
  @Column(length = 36)
  private String id;

  @Column(nullable = false, unique = true)
  private String name;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false)
  private String image;

  @Column(nullable = false)
  private String type;

  @Column(name = "is_active")
  private Boolean isActive;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}
