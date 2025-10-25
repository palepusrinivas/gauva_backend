package com.ridefast.ride_fast_backend.model.v2;

import com.ridefast.ride_fast_backend.model.MyUser;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleV2 {
  @Id
  @Column(length = 36)
  private String id;

  @Column(name = "ref_id", length = 20, nullable = false)
  private String refId;

  @ManyToOne(optional = false)
  @JoinColumn(name = "brand_id", nullable = false)
  private VehicleBrand brand;

  @ManyToOne(optional = false)
  @JoinColumn(name = "model_id", nullable = false)
  private VehicleModel model;

  @ManyToOne(optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private VehicleCategory category;

  @Column(name = "licence_plate_number", nullable = false)
  private String licencePlateNumber;

  @Column(name = "licence_expire_date", nullable = false)
  private LocalDate licenceExpireDate;

  @Column(name = "vin_number", nullable = false)
  private String vinNumber;

  @Column(nullable = false)
  private String transmission;

  @Column(name = "fuel_type", nullable = false)
  private String fuelType;

  @Column(nullable = false)
  private String ownership;

  @ManyToOne(optional = false)
  @JoinColumn(name = "driver_id", nullable = false)
  private MyUser driver;

  @Column(columnDefinition = "TEXT")
  private String documents;

  @Column(name = "is_active")
  private Boolean isActive;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}
